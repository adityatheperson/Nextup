package com.adith.nextup

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.ZoneId

fun notificationWalk(event: CalendarEvent, labels: TimeLabels): String? = event.walk?.let {
    "~${it.minutes} min walk · as of ${labels.clock(it.checkedAt)}"
}

fun notificationTimeRange(event: CalendarEvent, labels: TimeLabels, now: Long): String {
    val zone = ZoneId.systemDefault()
    val sameDay = Instant.ofEpochMilli(event.start).atZone(zone).toLocalDate() ==
        Instant.ofEpochMilli(event.end).atZone(zone).toLocalDate()
    return "${labels.absolute(event.start, now)} – ${if (sameDay) labels.clock(event.end) else labels.absolute(event.end, now)}"
}

fun agendaRemoteViews(context: Context, events: List<CalendarEvent>, labels: TimeLabels, now: Long): RemoteViews {
    val first = events.first()
    return RemoteViews(context.packageName, R.layout.notification_agenda).apply {
        setTextViewText(R.id.event_status, labels.agendaTitle(events, now).uppercase())
        setTextViewText(R.id.featured_title, first.title)
        setTextViewText(R.id.featured_time, notificationTimeRange(first, labels, now))
        setTextViewText(R.id.featured_walk, notificationWalk(first, labels))
        setViewVisibility(R.id.featured_walk, if (first.walk != null) View.VISIBLE else View.GONE)
        val progress = eventProgress(first, now)
        setViewVisibility(R.id.progress_group, if (progress != null) View.VISIBLE else View.GONE)
        setProgressBar(R.id.event_progress, 100, progress ?: 0, false)
        setContentDescription(R.id.event_progress, "Event ${progress ?: 0}% complete as of ${labels.clock(now)}")
        setTextViewText(R.id.progress_time, "Started ${labels.clock(first.start)} · Ends ${labels.clock(first.end)} · As of ${labels.clock(now)}")
        removeAllViews(R.id.later_events)
        events.drop(1).forEach { event ->
            addView(R.id.later_events, RemoteViews(context.packageName, R.layout.notification_event_row).apply {
                setTextViewText(R.id.row_title, "${labels.notificationTime(event, now)}  ·  ${event.title}")
                setTextViewText(R.id.row_walk, notificationWalk(event, labels))
                setViewVisibility(R.id.row_walk, if (event.walk != null) View.VISIBLE else View.GONE)
            })
        }
        setViewVisibility(R.id.later_events, if (events.size > 1) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.event_divider, if (events.size > 1) View.VISIBLE else View.GONE)
        setViewVisibility(R.id.walk_credit, if (events.any { it.walk != null }) View.VISIBLE else View.GONE)
    }
}
