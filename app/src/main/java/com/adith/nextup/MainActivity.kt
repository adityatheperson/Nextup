package com.adith.nextup

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings as AndroidSettings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val coordinator get() = (application as NextUpApp).coordinator
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        lifecycleScope.launch { coordinator.refresh() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextUpTheme {
                val state by coordinator.state.collectAsStateWithLifecycle()
                var showWalking by remember { mutableStateOf(false) }
                var saving by remember { mutableStateOf(false) }
                NextUpScreen(state,
                    onCount = { value -> lifecycleScope.launch { coordinator.refresh(count = value) } },
                    onRefresh = { lifecycleScope.launch { coordinator.refresh() } },
                    onCalendarPermission = { requestCalendar() },
                    onNotificationPermission = { requestNotifications() },
                    onWalkingSettings = { showWalking = true })
                if (showWalking) WalkingSetup(state.walking, { showWalking = false },
                    onSave = { key -> lifecycleScope.launch {
                        saving = true
                        try { if (coordinator.configureWalking(key)) showWalking = false } finally { saving = false }
                    } },
                    onRemove = { lifecycleScope.launch {
                        saving = true
                        try { if (coordinator.configureWalking(null)) showWalking = false } finally { saving = false }
                    } },
                    onPrecise = { requestPreciseLocation() },
                    onBackground = { openSettings(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:$packageName"))) }, busy = saving)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { observeWhileVisible() }
                coordinator.refresh(opened = true)
                if (!coordinator.state.value.settings.permissionRequested && coordinator.state.value.error == null) {
                    coordinator.markPermissionRequested()
                    permissions.launch(buildList {
                        add(Manifest.permission.READ_CALENDAR)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }.toTypedArray())
                }
                awaitCancellation()
            }
        }
        openEventIfRequested(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openEventIfRequested(intent)
    }

    private suspend fun observeWhileVisible(): Unit = coroutineScope {
        var refreshJob: kotlinx.coroutines.Job? = null
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refreshJob?.cancel()
                refreshJob = launch { delay(500); coordinator.refresh() }
            }
        }
        var registered = false
        try {
            if (coordinator.calendarAllowed()) {
                try {
                    contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
                    registered = true
                } catch (_: SecurityException) { coordinator.refresh() }
            }
            launch {
                var checkedAt = System.currentTimeMillis()
                var refreshedAt = checkedAt
                while (true) {
                    delay(1_000)
                    val now = System.currentTimeMillis()
                    val boundary = nextBoundary(coordinator.state.value.events, checkedAt)
                    val refreshDue = (boundary != null && boundary <= now) ||
                        now - refreshedAt >= 60_000 || now < checkedAt
                    checkedAt = now
                    if (refreshDue) {
                        refreshedAt = now
                        coordinator.refresh()
                    }
                }
            }
            awaitCancellation()
        } finally {
            if (registered) contentResolver.unregisterContentObserver(observer)
        }
    }

    private fun requestCalendar() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR) ||
            !coordinator.state.value.settings.permissionRequested) {
            permissions.launch(arrayOf(Manifest.permission.READ_CALENDAR))
        } else openSettings(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:$packageName")))
    }

    private fun requestPreciseLocation() {
        val prefs = getPreferences(MODE_PRIVATE)
        if (prefs.getBoolean("location_requested", false) &&
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            openSettings(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
        } else {
            prefs.edit().putBoolean("location_requested", true).apply()
            permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            permissions.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        } else openSettings(Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun openSettings(intent: Intent) {
        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
            startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:$packageName")))
        }
    }

    private fun openEventIfRequested(source: Intent) {
        val id = source.getLongExtra(AgendaNotifications.EXTRA_EVENT_ID, -1)
        if (id < 0) return
        val start = source.getLongExtra(AgendaNotifications.EXTRA_EVENT_START, 0)
        val end = source.getLongExtra(AgendaNotifications.EXTRA_EVENT_END, 0)
        source.removeExtra(AgendaNotifications.EXTRA_EVENT_ID)
        if (!coordinator.calendarAllowed()) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end))
        } catch (_: ActivityNotFoundException) {
            // NextUp remains visible if there is no calendar application to handle the event.
        } catch (_: SecurityException) {
            // A missing external handler permission must not crash the agenda.
        }
    }
}
