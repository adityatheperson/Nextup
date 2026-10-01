package com.adith.nextup

import android.Manifest
import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.ZoneId

class AgendaNotifications(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun createChannel() {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Upcoming events", NotificationManager.IMPORTANCE_LOW).apply {
            description = "A silent view of your next calendar events"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        })
    }

    fun allowed(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE

    fun cancel() = manager.cancel(ID)

    fun post(settings: Settings, events: List<CalendarEvent>, calendarAllowed: Boolean, error: String?, now: Long) {
        createChannel()
        if (!allowed() || notificationMode(settings.dismissed, calendarAllowed) == NotificationMode.HIDDEN) {
            cancel()
            return
        }
        try { manager.notify(ID, build(settings, events, calendarAllowed, error, now)) }
        catch (_: SecurityException) { cancel() }
    }

    internal fun build(settings: Settings, events: List<CalendarEvent>, calendarAllowed: Boolean, error: String?, now: Long): Notification {
        val activeEvents = notificationAgenda(events, now, settings.count)
        val first = activeEvents.firstOrNull()
        val labels = TimeLabels(ZoneId.systemDefault(), context.resources.configuration.locales[0], DateFormat.is24HourFormat(context))
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (calendarAllowed && error == null && first != null) {
                putExtra(EXTRA_EVENT_ID, first.id)
                putExtra(EXTRA_EVENT_START, first.start)
                putExtra(EXTRA_EVENT_END, first.end)
            }
        }
        val contentIntent = PendingIntent.getActivity(context, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Different data URIs preserve generation extras in older delete intents.
        val delete = Intent(context, AgendaReceiver::class.java)
            .setAction(AgendaReceiver.ACTION_DISMISS)
            .setData(android.net.Uri.parse("nextup://dismiss/${settings.generation}"))
            .putExtra(AgendaReceiver.EXTRA_GENERATION, settings.generation)
        val deleteIntent = PendingIntent.getBroadcast(context, 1, delete,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF284FB9.toInt())
            .setContentTitle("Upcoming events")
            .setContentText("Unlock to see your agenda")
            .setContentIntent(contentIntent)
            .setSilent(true)
            .build()
        val title = when {
            !calendarAllowed -> "Calendar access required"
            error != null -> "Couldn't refresh calendar"
            activeEvents.isEmpty() -> "No events in the next 7 days"
            else -> labels.agendaTitle(activeEvents, now)
        }
        fun line(event: CalendarEvent): String = "${event.title} — ${labels.notificationTime(event, now)}" +
            (event.walk?.takeIf { it.fresh(now) }?.let { " · ~${it.minutes} min walk (as of ${labels.clock(it.checkedAt)})" } ?: "")
        val text = when {
            !calendarAllowed -> "Tap to allow calendar access"
            error != null -> "Tap NextUp to try again"
            first == null -> "Nothing coming up"
            else -> line(first)
        }
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF284FB9.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
        if (activeEvents.any { it.walk?.fresh(now) == true }) {
            builder.setSubText("Walks: openrouteservice · © OpenStreetMap")
        }
        if (activeEvents.isNotEmpty() && calendarAllowed && error == null) {
            builder.setContentTitle("$title · ${first!!.title}")
                .setContentText(notificationTimeRange(first, labels, now) +
                    (activeEvents.firstOrNull { it.walk != null }?.let {
                        " · ${if (it != first) "${it.title}: " else ""}${notificationWalk(it, labels)}"
                    } ?: ""))
            val progress = eventProgress(first, now)
            // The system owns the outer surface/header. Keep custom content within its height budget.
            val compact = activeEvents.size > 3 || context.resources.configuration.fontScale > 1.2f ||
                activeEvents.any { it.title.length > 30 || it.title.any(Char::isISOControl) } ||
                context.resources.configuration.screenWidthDp < 360
            if (compact) {
                if (progress != null) builder.setProgress(100, progress, false)
                val body = activeEvents.joinToString("\n") { event ->
                    "${labels.notificationTime(event, now)} · ${event.title}" +
                        (notificationWalk(event, labels)?.let { "\n$it" } ?: "")
                } + (if (progress != null) "\nProgress as of ${labels.clock(now)}" else "")
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
            } else {
                builder.setSubText(null)
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomBigContentView(agendaRemoteViews(context, activeEvents, labels, now))
            }
        }
        return builder.build()
    }

    companion object {
        const val CHANNEL = "upcoming_events"
        const val ID = 1
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_START = "event_start"
        const val EXTRA_EVENT_END = "event_end"
    }
}
