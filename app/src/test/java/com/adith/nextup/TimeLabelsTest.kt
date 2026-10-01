package com.adith.nextup

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class TimeLabelsTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val now = Instant.parse("2026-09-24T17:00:00Z").toEpochMilli()
    private val labels = TimeLabels(zone, Locale.US, false)
    @Test fun `notification changes at start and ended event gives way to next`() {
        val event = CalendarEvent(1, "Meeting", now, now + 3_600_000)
        val next = CalendarEvent(2, "Next", event.end, event.end + 3_600_000)
        assertEquals("Upcoming", labels.agendaTitle(listOf(event), now - 1))
        assertEquals("Now", labels.agendaTitle(listOf(event), now))
        assertEquals("Now · ends at 11:00 AM", labels.notificationTime(event, now))
        assertEquals("Today · 10:00 AM", labels.notificationTime(event, now - 1))
        assertEquals(listOf(next), selectEvents(listOf(event, next), event.end, 3))
        assertEquals("Upcoming", labels.agendaTitle(listOf(event), event.end))
        assertEquals(emptyList<CalendarEvent>(), selectEvents(listOf(event), event.end, 3))
    }

    @Test fun `overlapping ongoing events keep now heading until both end`() {
        val first = CalendarEvent(1, "First", now - 1, now + 60_000)
        val second = CalendarEvent(2, "Second", now, now + 120_000)
        assertEquals("Now", labels.agendaTitle(listOf(first, second), first.end))
        assertEquals(listOf(second), selectEvents(listOf(first, second), first.end, 3))
        assertEquals("Upcoming", labels.agendaTitle(listOf(first, second), second.end))
    }
    @Test fun `notification uses dates and absolute time`() {
        assertEquals("Today · 10:34 AM", labels.absolute(now + 34 * 60_000, now))
        assertEquals("Tomorrow · 10:00 AM", labels.absolute(now + 86_400_000, now))
    }
    @Test fun `relative minutes hours and ongoing end`() {
        assertEquals("in 34 min", labels.relative(CalendarEvent(1, "A", now + 34 * 60_000, now + 4_000_000), now))
        assertEquals("in 2 hr 15 min", labels.relative(CalendarEvent(1, "A", now + 135 * 60_000, now + 9_000_000), now))
        assertEquals("Now · ends at 11:00 AM", labels.relative(CalendarEvent(1, "A", now - 1, now + 3_600_000), now))
    }
    @Test fun `tomorrow is calendar date across DST and 24 hour preference respected`() {
        val beforeDst = Instant.parse("2026-03-08T07:30:00Z").toEpochMilli()
        val afterDst = Instant.parse("2026-03-08T16:00:00Z").toEpochMilli()
        assertEquals("Tomorrow · 9:00 AM", labels.absolute(afterDst, beforeDst))
        assertEquals("Today · 10:00", TimeLabels(zone, Locale.US, true).absolute(now, now))
    }
}
