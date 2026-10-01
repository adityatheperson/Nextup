package com.adith.nextup

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances as I
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val context: Context) {
    suspend fun upcoming(now: Long, count: Int): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val uri = I.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + LOOKAHEAD_MILLIS)
        }.build()
        val projection = arrayOf(I.EVENT_ID, I.TITLE, I.BEGIN, I.END, I.EVENT_LOCATION)
        val selection = "${I.VISIBLE}=1 AND ${I.ALL_DAY}=0 AND ${I.END}>? AND ${I.BEGIN}<? " +
            "AND (${I.STATUS} IS NULL OR ${I.STATUS}!=?) " +
            "AND (${I.SELF_ATTENDEE_STATUS} IS NULL OR ${I.SELF_ATTENDEE_STATUS}!=?)"
        val args = arrayOf(now.toString(), (now + LOOKAHEAD_MILLIS).toString(),
            CalendarContract.Events.STATUS_CANCELED.toString(),
            CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED.toString())
        val result = mutableListOf<CalendarEvent>()
        val cursor = context.contentResolver.query(uri, projection, selection, args,
            "${I.BEGIN} ASC, ${I.END} ASC, ${I.EVENT_ID} ASC")
            ?: throw IllegalStateException("Calendar provider unavailable")
        cursor.use {
            while (it.moveToNext() && result.size < count.coerceIn(1, 5)) {
                result += CalendarEvent(it.getLong(0), it.getString(1).orEmpty(), it.getLong(2), it.getLong(3),
                    location = it.getString(4).orEmpty().trim())
            }
        }
        selectEvents(result, now, count)
    }
}
