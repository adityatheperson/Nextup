package com.adith.nextup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class RefreshScheduler(private val context: Context) {
    private val work get() = WorkManager.getInstance(context)
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private fun boundaryIntent() = PendingIntent.getBroadcast(context, 20,
        Intent(context, AgendaReceiver::class.java).setAction(ACTION_BOUNDARY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun walkingIntent() = PendingIntent.getBroadcast(context, 21,
        Intent(context, AgendaReceiver::class.java).setAction("com.adith.nextup.WALKING_RETRY"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun ensurePeriodic() {
        work.enqueueUniquePeriodicWork("agenda-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build())
    }

    fun enqueueRefresh() {
        work.enqueueUniqueWork("agenda-refresh", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }

    fun enqueueWalking(delay: Long) {
        // A separate inexact alarm avoids replacing/cancelling the worker scheduling its own retry.
        alarms.set(AlarmManager.RTC, System.currentTimeMillis() + delay.coerceAtLeast(1_000), walkingIntent())
    }

    fun scheduleBoundary(time: Long?) {
        alarms.cancel(boundaryIntent())
        if (time != null) {
            // Inexact, non-wakeup alarm: low battery cost; Android may defer this until awake.
            alarms.set(AlarmManager.RTC, time.coerceAtLeast(System.currentTimeMillis() + 1_000), boundaryIntent())
        }
    }

    fun stop() {
        alarms.cancel(boundaryIntent())
        alarms.cancel(walkingIntent())
        work.cancelUniqueWork("agenda-periodic")
        work.cancelUniqueWork("agenda-refresh")
        work.cancelUniqueWork("walking-refresh")
    }

    companion object { const val ACTION_BOUNDARY = "com.adith.nextup.BOUNDARY" }
}
