package com.adith.nextup

const val LOOKAHEAD_MILLIS = 7 * 24 * 60 * 60 * 1000L

data class CalendarEvent(
    val id: Long,
    val title: String,
    val start: Long,
    val end: Long,
    val allDay: Boolean = false,
    val visible: Boolean = true,
    val cancelled: Boolean = false,
    val declined: Boolean = false,
    val location: String = "",
    val walk: WalkEstimate? = null,
)

fun selectEvents(events: List<CalendarEvent>, now: Long, count: Int): List<CalendarEvent> = events
    .asSequence()
    .filter { it.end > now && it.start < now + LOOKAHEAD_MILLIS && it.end >= it.start }
    .filter { it.visible && !it.allDay && !it.cancelled && !it.declined }
    .sortedWith(compareBy<CalendarEvent> { it.start }.thenBy { it.end }.thenBy { it.id })
    .take(count.coerceIn(1, 5))
    .map { it.copy(title = it.title.trim().ifEmpty { "Untitled event" }) }
    .toList()

fun nextBoundary(events: List<CalendarEvent>, now: Long): Long? = events
    .flatMap { listOf(it.start, it.end) }.filter { it > now }.minOrNull()

enum class NotificationMode { HIDDEN, PERMISSION_REQUIRED, AGENDA }

fun notificationMode(dismissed: Boolean, calendarAllowed: Boolean): NotificationMode = when {
    dismissed -> NotificationMode.HIDDEN
    !calendarAllowed -> NotificationMode.PERMISSION_REQUIRED
    else -> NotificationMode.AGENDA
}
