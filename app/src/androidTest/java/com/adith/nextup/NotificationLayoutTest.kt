package com.adith.nextup

import android.app.Notification
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

/** Synthetic events only. No calendar, location or notification permissions needed. */
class NotificationLayoutTest {
    @Test fun ongoingLayoutRendersProgressAndOnlyNextWalk() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val now = System.currentTimeMillis()
        val events = listOf(
            CalendarEvent(1, "Current class", now - 30 * 60_000, now + 30 * 60_000,
                location = "Campus", walk = WalkEstimate(12, now, "Campus")),
            CalendarEvent(2, "Next class", now + 60 * 60_000, now + 120 * 60_000,
                location = "Campus", walk = WalkEstimate(8, now, "Campus")),
            CalendarEvent(3, "Later class", now + 180 * 60_000, now + 240 * 60_000,
                location = "Campus", walk = WalkEstimate(20, now, "Campus")))
        val notification = AgendaNotifications(context).build(Settings(), events, true, null, now)
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertFalse(notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("class"))
        assertNotNull(notification.deleteIntent)
        instrumentation.runOnMainSync {
            val view = notification.bigContentView.apply(context, LinearLayout(context))
            val width = (280 * context.resources.displayMetrics.density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            assertTrue("Expanded custom content must fit 252dp", view.measuredHeight <= 252 * context.resources.displayMetrics.density)
            assertEquals(50, view.findViewById<ProgressBar>(R.id.event_progress).progress)
            assertEquals(View.GONE, view.findViewById<View>(R.id.featured_walk).visibility)
            val rows = view.findViewById<LinearLayout>(R.id.later_events)
            assertTrue(rows.getChildAt(0).findViewById<TextView>(R.id.row_walk).text.contains("8 min walk"))
            assertEquals(View.GONE, rows.getChildAt(1).findViewById<View>(R.id.row_walk).visibility)
        }
    }

    @Test fun upcomingLayoutHidesProgressAndShowsFeaturedWalk() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val now = System.currentTimeMillis()
        val event = CalendarEvent(1, "Next class", now + 60_000, now + 120_000,
            location = "Campus", walk = WalkEstimate(8, now, "Campus"))
        val notification = AgendaNotifications(context).build(Settings(), listOf(event), true, null, now)
        instrumentation.runOnMainSync {
            val view = notification.bigContentView.apply(context, LinearLayout(context))
            assertEquals(View.GONE, view.findViewById<View>(R.id.progress_group).visibility)
            assertTrue(view.findViewById<TextView>(R.id.featured_walk).text.contains("8 min walk"))
        }
    }

    @Test fun multilineTitleUsesNativeFallbackWithoutLosingWalkingSelection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val now = System.currentTimeMillis()
        val events = listOf(CalendarEvent(1, "One\nTwo\nThree\nFour", now - 60_000, now + 60_000),
            CalendarEvent(2, "Next", now + 120_000, now + 180_000, location = "Campus",
                walk = WalkEstimate(8, now, "Campus")))
        val notification = AgendaNotifications(context).build(Settings(), events, true, null, now)
        val body = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertTrue(body.contains("One\nTwo\nThree\nFour"))
        assertTrue(body.contains("8 min walk"))
        assertEquals(50, notification.extras.getInt(Notification.EXTRA_PROGRESS))
    }
}
