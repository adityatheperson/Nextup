package com.adith.nextup

import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimeLabels(private val zone: ZoneId, private val locale: Locale, private val use24Hour: Boolean) {
    private val clock = DateTimeFormatter.ofPattern(if (use24Hour) "HH:mm" else "h:mm a", locale)
    fun clock(time: Long): String = Instant.ofEpochMilli(time).atZone(zone).format(clock)

    fun agendaTitle(events: List<CalendarEvent>, now: Long): String =
        if (events.any { it.start <= now && now < it.end }) "Now" else "Upcoming"

    fun notificationTime(event: CalendarEvent, now: Long): String =
        if (event.start <= now && now < event.end) "Now · ends at ${clock(event.end)}"
        else absolute(event.start, now)

    fun absolute(time: Long, now: Long): String {
        val date = Instant.ofEpochMilli(time).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val day = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
        }
        return "$day · ${clock(time)}"
    }

    fun relative(event: CalendarEvent, now: Long): String {
        if (event.start <= now && event.end > now) return "Now · ends at ${clock(event.end)}"
        if (event.end <= now) return "Ended"
        if (Instant.ofEpochMilli(event.start).atZone(zone).toLocalDate() !=
            Instant.ofEpochMilli(now).atZone(zone).toLocalDate()) return absolute(event.start, now)
        val minutes = ((event.start - now + 59_999) / 60_000).coerceAtLeast(1)
        val hours = minutes / 60
        val remainder = minutes % 60
        return when {
            hours == 0L -> "in $minutes min"
            remainder == 0L -> "in $hours hr"
            else -> "in $hours hr $remainder min"
        }
    }
}
