package com.adith.nextup

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract as C
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidIntegrationTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_CALENDAR, Manifest.permission.POST_NOTIFICATIONS)
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val resolver get() = context.contentResolver
    private var calendarId = -1L
    private val now get() = System.currentTimeMillis()
    private val account = "nextup-instrumentation-only"

    private fun syncUri(uri: android.net.Uri) = uri.buildUpon()
        .appendQueryParameter(C.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(C.Calendars.ACCOUNT_NAME, account)
        .appendQueryParameter(C.Calendars.ACCOUNT_TYPE, C.ACCOUNT_TYPE_LOCAL).build()

    @Before fun setUp() {
        instrumentation.uiAutomation.adoptShellPermissionIdentity()
        calendarId = ContentUris.parseId(resolver.insert(syncUri(C.Calendars.CONTENT_URI), ContentValues().apply {
            put(C.Calendars.ACCOUNT_NAME, account)
            put(C.Calendars.ACCOUNT_TYPE, C.ACCOUNT_TYPE_LOCAL)
            put(C.Calendars.NAME, account)
            put(C.Calendars.CALENDAR_DISPLAY_NAME, "NextUp test calendar")
            put(C.Calendars.CALENDAR_COLOR, 0xff336655.toInt())
            put(C.Calendars.CALENDAR_ACCESS_LEVEL, C.Calendars.CAL_ACCESS_OWNER)
            put(C.Calendars.OWNER_ACCOUNT, account)
            put(C.Calendars.VISIBLE, 1)
            put(C.Calendars.SYNC_EVENTS, 1)
            put(C.Calendars.CALENDAR_TIME_ZONE, "UTC")
        })!!)
    }

    @After fun tearDown() {
        if (calendarId >= 0) resolver.delete(syncUri(ContentUris.withAppendedId(C.Calendars.CONTENT_URI, calendarId)), null, null)
        instrumentation.uiAutomation.dropShellPermissionIdentity()
        context.getSystemService(NotificationManager::class.java).cancelAll()
        RefreshScheduler(context).stop()
    }

    private fun event(title: String, start: Long, end: Long, extra: ContentValues = ContentValues()): Long {
        val values = ContentValues().apply {
            put(C.Events.CALENDAR_ID, calendarId)
            put(C.Events.TITLE, title)
            put(C.Events.DTSTART, start)
            put(C.Events.DTEND, end)
            put(C.Events.EVENT_TIMEZONE, "UTC")
            putAll(extra)
        }
        if (values.containsKey(C.Events.RRULE)) values.remove(C.Events.DTEND)
        return ContentUris.parseId(resolver.insert(syncUri(C.Events.CONTENT_URI), values)!!)
    }

    @Test fun providerExpandsRecurrenceAndExcludesDeclinedCancelledAndAllDay() = runBlocking {
        val instant = now
        event("Ongoing", instant - 60_000, instant + 60_000)
        val recurring = event("Recurring", instant + 120_000, instant + 180_000, ContentValues().apply {
            put(C.Events.RRULE, "FREQ=DAILY;COUNT=2")
            put(C.Events.DURATION, "PT1H")
        })
        event("Declined", instant + 60_000, instant + 120_000, ContentValues().apply { put(C.Events.SELF_ATTENDEE_STATUS, C.Attendees.ATTENDEE_STATUS_DECLINED) })
        event("Cancelled", instant + 60_000, instant + 120_000, ContentValues().apply { put(C.Events.STATUS, C.Events.STATUS_CANCELED) })
        val day = instant / 86_400_000 * 86_400_000
        event("All day", day, day + 86_400_000, ContentValues().apply { put(C.Events.ALL_DAY, 1) })
        val result = CalendarRepository(context).upcoming(instant, 5)
        assertEquals(listOf("Ongoing", "Recurring", "Recurring"), result.map { it.title })
        assertEquals(2, result.count { it.id == recurring })
        resolver.update(syncUri(ContentUris.withAppendedId(C.Calendars.CONTENT_URI, calendarId)), ContentValues().apply { put(C.Calendars.VISIBLE, 0) }, null, null)
        assertTrue(CalendarRepository(context).upcoming(instant, 5).isEmpty())
    }

    @Test fun dismissedNotificationDoesNotClearForegroundAgenda() = runBlocking {
        val instant = now
        event("Still in preview", instant + 60_000, instant + 3_600_000)
        val coordinator = (context.applicationContext as NextUpApp).coordinator
        assertTrue(coordinator.refresh(opened = true))
        coordinator.dismiss(coordinator.state.value.settings.generation)
        assertTrue(coordinator.refresh(count = 5))
        assertEquals(listOf("Still in preview"), coordinator.state.value.events.map { it.title })
        assertTrue(coordinator.state.value.settings.dismissed)
        assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
    }

    @Test fun settingsPersistDismissalAndCountAndIgnoreOldDeleteIntents() = runBlocking {
        val store = SettingsStore(context)
        store.open()
        store.setCount(5)
        val oldGeneration = store.read().generation
        store.dismiss(oldGeneration)
        val reloaded = SettingsStore(context).read()
        assertEquals(5, reloaded.count)
        assertTrue(reloaded.dismissed)
        store.open()
        store.dismiss(oldGeneration)
        assertFalse(store.read().dismissed)
        assertEquals(5, store.read().count)
    }

    @Test fun notificationIsSilentPrivateAndPermissionNoticeRemovesTitles() {
        val publisher = AgendaNotifications(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        publisher.post(Settings(activated = true), listOf(CalendarEvent(1, "Private appointment", now + 60_000, now + 120_000)), true, null, now)
        val notification = awaitNotificationTitle("Upcoming")
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertFalse(notification.publicVersion.extras.toString().contains("Private appointment"))
        assertNull(manager.getNotificationChannel(AgendaNotifications.CHANNEL).sound)
        assertFalse(manager.getNotificationChannel(AgendaNotifications.CHANNEL).shouldVibrate())
        publisher.post(Settings(activated = true), emptyList(), false, null, now)
        val notice = awaitNotificationTitle("Calendar access required")
        assertEquals("Calendar access required", notice.extras.getString(Notification.EXTRA_TITLE))
        assertFalse(notice.extras.toString().contains("Private appointment"))
    }

    private fun awaitNotificationTitle(title: String): Notification {
        // NotificationManager enqueues updates asynchronously in system_server.
        val manager = context.getSystemService(NotificationManager::class.java)
        repeat(100) {
            manager.activeNotifications.firstOrNull {
                it.notification.extras.getString(Notification.EXTRA_TITLE) == title
            }?.let { return it.notification }
            Thread.sleep(20)
        }
        error("Notification did not reach title: $title")
    }
}
