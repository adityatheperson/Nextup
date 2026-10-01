package com.adith.nextup

/** Preserve event slots; only the earliest not-yet-started instance may show a walk. */
fun notificationAgenda(events: List<CalendarEvent>, now: Long, count: Int): List<CalendarEvent> {
    val selected = selectEvents(events, now, count)
    val target = selected.indexOfFirst { it.start > now }
    return selected.mapIndexed { index, event ->
        event.copy(walk = event.walk?.takeIf {
            index == target && isPhysicalLocation(event.location) && it.fresh(now)
        })
    }
}

/** A snapshot at refresh time, not an independently ticking background timer. */
fun eventProgress(event: CalendarEvent, now: Long): Int? =
    if (event.start <= now && now < event.end && event.end > event.start)
        (((now.toDouble() - event.start) / (event.end.toDouble() - event.start)) * 100).toInt().coerceIn(0, 99)
    else null
