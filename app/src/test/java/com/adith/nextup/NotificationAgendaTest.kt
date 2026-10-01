package com.adith.nextup

import org.junit.Assert.*
import org.junit.Test

class NotificationAgendaTest {
    private val now = 1_000_000L
    private fun event(id: Long, start: Long, end: Long, location: String = "Campus") =
        CalendarEvent(id, "Class $id", start, end, location = location,
            walk = WalkEstimate(8, now, location))

    @Test fun `only earliest future instance gets a walk even with shared ids and locations`() {
        val ongoing = event(1, now - 60_000, now + 60_000)
        val next = event(1, now + 120_000, now + 180_000)
        val later = event(2, now + 240_000, now + 300_000)
        assertEquals(listOf(null, 8, null), notificationAgenda(listOf(ongoing, next, later), now, 3).map { it.walk?.minutes })
        assertEquals(8, notificationAgenda(listOf(next, later), now, 3).first().walk?.minutes)
    }

    @Test fun `missing online or stale next walk does not borrow a later estimate`() {
        val next = event(1, now + 60_000, now + 120_000)
        val later = event(2, now + 180_000, now + 240_000)
        for (unavailable in listOf(next.copy(walk = null), next.copy(location = "Online"),
            next.copy(walk = WalkEstimate(8, now - WALK_TTL, "Campus")))) {
            assertTrue(notificationAgenda(listOf(unavailable, later), now, 3).all { it.walk == null })
        }
    }

    @Test fun `start transfers walk to next class and end removes current class`() {
        val first = event(1, now, now + 60_000)
        val next = event(2, now + 120_000, now + 180_000)
        assertEquals(listOf(null, 8), notificationAgenda(listOf(first, next), now, 3).map { it.walk?.minutes })
        assertEquals(listOf(2L), notificationAgenda(listOf(first, next), first.end, 3).map { it.id })
        assertEquals(1, notificationAgenda(listOf(first, next), now, 1).size)
    }

    @Test fun `progress covers only a positive duration ongoing event`() {
        val event = event(1, now, now + 100_000)
        assertNull(eventProgress(event, now - 1))
        assertEquals(0, eventProgress(event, now))
        assertEquals(50, eventProgress(event, now + 50_000))
        assertEquals(99, eventProgress(event, now + 99_999))
        assertNull(eventProgress(event, event.end))
        assertNull(eventProgress(event.copy(end = now), now))
    }
}
