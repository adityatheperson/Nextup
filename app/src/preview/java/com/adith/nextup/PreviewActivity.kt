package com.adith.nextup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Synthetic, isolated visual fixtures. This source is never included in the installable debug APK. */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scenario = intent.getStringExtra("scenario") ?: "light"
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (scenario.startsWith("notification")) {
            showNotifications(scenario)
            return
        }
        val now = System.currentTimeMillis()
        val events = listOf(
            CalendarEvent(1, if (scenario == "large") "Design review and planning for the next semester" else "ICS 33 Lecture", now + 34 * 60_000, now + 94 * 60_000),
            CalendarEvent(2, "Math Discussion", now + 120 * 60_000, now + 180 * 60_000),
            CalendarEvent(3, "Gym", now + 330 * 60_000, now + 390 * 60_000),
        )
        setContent {
            var state by remember { mutableStateOf(AgendaState(
                calendarAllowed = scenario != "denied", notificationsAllowed = scenario != "denied",
                loading = false, lastUpdated = now,
                events = if (scenario in listOf("empty", "denied")) emptyList() else events.mapIndexed { index, event ->
                    if (scenario.startsWith("walking") && index < 2) event.copy(location = "Engineering Hall",
                        walk = WalkEstimate(12 + index * 8, now, "Engineering Hall")) else event
                }, walking = if (scenario.startsWith("walking")) WalkingStatus(true, true, true,
                    "Movement updates on · only walks of 40 minutes or less.") else WalkingStatus())) }
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, if (scenario == "large") 1.3f else 1f)) {
                NextUpTheme(darkTheme = scenario == "dark" || scenario == "walking-dark") {
                    NextUpScreen(state, { state = state.copy(settings = state.settings.copy(count = it)) },
                        { state = state.copy(lastUpdated = System.currentTimeMillis()) }, {}, {})
                }
            }
        }
    }

    private fun showNotifications(scenario: String) {
        val dark = scenario.contains("dark")
        val large = scenario.contains("large")
        val config = android.content.res.Configuration(resources.configuration).apply {
            uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO
            fontScale = if (large) 1.3f else 1f
        }
        val ctx = android.view.ContextThemeWrapper(createConfigurationContext(config),
            if (dark) android.R.style.Theme_Material else android.R.style.Theme_Material_Light)
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val root = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(32))
            setBackgroundColor(android.graphics.Color.parseColor(if (dark) "#111724" else "#F7F9FE"))
        }
        val now = System.currentTimeMillis()
        for (ongoing in listOf(false, true)) {
            root.addView(android.widget.TextView(ctx).apply {
                text = if (ongoing) "DURING CLASS · synthetic preview" else "BEFORE CLASS · synthetic preview"
                setPadding(0, dp(16), 0, dp(8))
            })
            val events = listOf(
                CalendarEvent(1, if (large) "Calculus lecture and extended problem-solving workshop" else "Calculus lecture",
                    now + (if (ongoing) -30 else 30) * 60_000, now + (if (ongoing) 30 else 90) * 60_000,
                    location = "Campus", walk = WalkEstimate(12, now - 5 * 60_000, "Campus")),
                CalendarEvent(2, "Physics lab", now + 120 * 60_000, now + 180 * 60_000,
                    location = "Campus", walk = WalkEstimate(8, now - 5 * 60_000, "Campus")),
                CalendarEvent(3, "Study group", now + 240 * 60_000, now + 300 * 60_000)) +
                if (large) listOf(CalendarEvent(4, "Office hours", now + 360 * 60_000, now + 390 * 60_000),
                    CalendarEvent(5, "Evening seminar", now + 420 * 60_000, now + 480 * 60_000)) else emptyList()
            val notification = AgendaNotifications(ctx).build(Settings(count = if (large) 5 else 3), events, true, null, now)
            val views = android.app.Notification.Builder.recoverBuilder(ctx, notification).createBigContentView()
            root.addView(views.apply(ctx, root))
        }
        setContentView(android.widget.ScrollView(ctx).apply { addView(root) })
    }
}
