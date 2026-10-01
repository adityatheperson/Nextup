package com.adith.nextup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

data class AgendaState(
    val settings: Settings = Settings(),
    val events: List<CalendarEvent> = emptyList(),
    val calendarAllowed: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val loading: Boolean = true,
    val lastUpdated: Long? = null,
    val error: String? = null,
    val walking: WalkingStatus = WalkingStatus(),
)

class RefreshCoordinator(context: Context) {
    private val context = context.applicationContext
    private val store = SettingsStore(this.context)
    private val repository = CalendarRepository(this.context)
    private val notifications = AgendaNotifications(this.context)
    private val scheduler = RefreshScheduler(this.context)
    private val mutex = Mutex()
    private val walking = WalkingService(this.context)
    private val revision = AtomicLong()
    private val mutableState = MutableStateFlow(AgendaState())
    val state = mutableState.asStateFlow()

    fun calendarAllowed() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    suspend fun markPermissionRequested() = mutex.withLock { store.markPermissionRequested() }

    suspend fun dismiss(generation: Long) {
      revision.incrementAndGet()
      mutex.withLock {
        store.dismiss(generation)
        val settings = store.read()
        if (settings.dismissed) {
            notifications.cancel()
            scheduler.stop()
            walking.stop()
            walking.invalidate()
        }
        mutableState.value = mutableState.value.copy(settings = settings,
            events = mutableState.value.events.map { it.copy(walk = null) })
      }
    }

    suspend fun configureWalking(key: String?): Boolean {
        revision.incrementAndGet()
        walking.stop()
        return try {
            walking.configure(key)
            mutex.withLock {
                val state = mutableState.value
                val clean = state.events.map { it.copy(walk = null) }
                mutableState.value = state.copy(events = clean, walking = walking.status())
                notifications.post(state.settings, clean, calendarAllowed(), state.error, System.currentTimeMillis())
            }
            scheduler.enqueueRefresh()
            true
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(walking = walking.status("Couldn't save the key. Please try again."))
            false
        }
    }

    suspend fun moved() {
        revision.incrementAndGet()
        walking.invalidate()
        mutex.withLock {
            val settings = store.read()
            if (settings.dismissed || !settings.activated) { walking.stop(); return@withLock }
            val clean = mutableState.value.events.map { it.copy(walk = null) }
            mutableState.value = mutableState.value.copy(events = clean)
            // Remove old-origin estimates immediately, even if the app process was recreated.
            if (clean.isEmpty()) notifications.cancel()
            else notifications.post(settings, clean, calendarAllowed(), mutableState.value.error, System.currentTimeMillis())
            scheduler.enqueueWalking(walking.retryDelay(System.currentTimeMillis()))
        }
    }

    suspend fun refresh(opened: Boolean = false, count: Int? = null): Boolean {
        val ticket = revision.incrementAndGet()
        val success = refreshAgenda(opened, count)
        val base = mutableState.value
        if (!success || !base.calendarAllowed || base.settings.dismissed || !base.settings.activated || !base.notificationsAllowed) {
            walking.stop(); walking.invalidate(); return success
        }
        val enriched = walking.enrich(base.events) { revision.get() == ticket && calendarAllowed() }
        // Workers have no UI observer: validate calendar contents again after network work.
        val unchanged = try { repository.upcoming(System.currentTimeMillis(), base.settings.count) == base.events }
            catch (e: CancellationException) { throw e } catch (_: Exception) { false }
        if (!unchanged) {
            if (revision.get() == ticket) scheduler.enqueueWalking(0)
            return success
        }
        mutex.withLock {
            if (revision.get() != ticket) return@withLock
            val settings = store.read()
            if (settings.dismissed || !calendarAllowed()) { walking.stop(); return@withLock }
            mutableState.value = mutableState.value.copy(events = enriched.events, walking = enriched.status)
            notifications.post(settings, enriched.events, true, base.error, System.currentTimeMillis())
            val expiry = enriched.events.mapNotNull { it.walk?.let { e -> e.checkedAt + WALK_TTL } }.minOrNull()
            val boundary = nextBoundary(enriched.events, System.currentTimeMillis())
            scheduler.scheduleBoundary(listOfNotNull(expiry, boundary).minOrNull())
            val delay = walking.retryDelay(System.currentTimeMillis())
            if (enriched.events.none { it.walk != null } && delay > 0) scheduler.enqueueWalking(delay)
        }
        return success
    }

    private suspend fun refreshAgenda(opened: Boolean, count: Int?): Boolean = mutex.withLock {
        mutableState.value = mutableState.value.copy(loading = true)
        try {
            if (opened) store.open()
            if (count != null) store.setCount(count)
            val settings = store.read()
            notifications.createChannel()
            if (!settings.activated) {
                notifications.cancel()
                scheduler.scheduleBoundary(null)
                mutableState.value = AgendaState(settings = settings, loading = false,
                    calendarAllowed = calendarAllowed(), notificationsAllowed = notifications.allowed())
                return@withLock true
            }
            if (!settings.dismissed) scheduler.ensurePeriodic()
            var allowed = calendarAllowed()
            var error: String? = null
            val now = System.currentTimeMillis()
            val events = if (allowed) {
                try { repository.upcoming(now, settings.count) }
                catch (_: SecurityException) { allowed = false; emptyList() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) {
                    error = "Couldn't read your calendars. Check that Calendar is synced, then try Refresh."
                    emptyList()
                }
            } else emptyList()
            // Re-check after the provider query: permission can change while the query is running.
            allowed = allowed && calendarAllowed()
            val safeEvents = if (allowed) events else emptyList()
            notifications.post(settings, safeEvents, allowed, error, now)
            scheduler.scheduleBoundary(if (settings.dismissed) null else nextBoundary(safeEvents, now))
            mutableState.value = AgendaState(settings, safeEvents, allowed, notifications.allowed(),
                loading = false, lastUpdated = if (error == null && allowed) now else null, error = error,
                walking = walking.status())
            error == null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            notifications.cancel()
            mutableState.value = mutableState.value.copy(events = emptyList(), loading = false,
                lastUpdated = null, error = "Couldn't update NextUp. Please try Refresh.")
            false
        }
    }
}
