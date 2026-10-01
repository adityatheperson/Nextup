package com.adith.nextup

import org.junit.Assert.*
import org.junit.Test

class AgendaTest {
    private val now = 1_000_000L
    private fun event(id: Long, start: Long = now + 60_000, end: Long = now + 120_000) =
        CalendarEvent(id, "Event $id", start, end)

    @Test fun `ongoing and future events sorted with ended events excluded`() {
        val events = listOf(event(3), event(1, now - 100, now), event(2, now - 100, now + 100))
        assertEquals(listOf(2L, 3L), selectEvents(events, now, 3).map { it.id })
    }
    @Test fun `all day invisible cancelled and declined events are excluded`() {
        val events = listOf(event(1).copy(allDay = true), event(2).copy(visible = false),
            event(3).copy(cancelled = true), event(4).copy(declined = true), event(5))
        assertEquals(listOf(5L), selectEvents(events, now, 5).map { it.id })
    }
    @Test fun `seven day boundary excluded and long ongoing event retained`() {
        val horizon = now + 7 * 24 * 60 * 60 * 1000L
        val events = listOf(event(1, horizon, horizon + 1), event(2, now - 10_000_000, now + 1))
        assertEquals(listOf(2L), selectEvents(events, now, 5).map { it.id })
    }
    @Test fun `recurring instances of same id and overlaps remain separate`() {
        val events = listOf(event(1), event(1, now + 70_000, now + 130_000), event(2))
        assertEquals(3, selectEvents(events, now, 3).size)
        assertEquals(1, selectEvents(events, now, 1).size)
    }
    @Test fun `invalid count clamped and empty titles normalized`() {
        assertEquals(1, selectEvents(List(6) { event(it.toLong()) }, now, 0).size)
        assertEquals(5, selectEvents(List(6) { event(it.toLong()) }, now, 99).size)
        assertEquals("Untitled event", selectEvents(listOf(event(1).copy(title = "  ")), now, 3).single().title)
    }
    @Test fun `dismissal overrides permission notice and survives background refresh`() {
        assertEquals(NotificationMode.HIDDEN, notificationMode(dismissed = true, calendarAllowed = false))
        assertEquals(NotificationMode.HIDDEN, notificationMode(dismissed = true, calendarAllowed = true))
        assertEquals(NotificationMode.PERMISSION_REQUIRED, notificationMode(false, false))
        assertEquals(NotificationMode.AGENDA, notificationMode(false, true))
    }
    @Test fun `next boundary includes ongoing end before future start`() {
        assertEquals(now + 100, nextBoundary(listOf(event(1, now - 100, now + 100), event(2)), now))
        assertNull(nextBoundary(emptyList(), now))
    }
}
