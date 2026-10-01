package com.adith.nextup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val coordinator = (applicationContext as NextUpApp).coordinator
        return if (coordinator.refresh()) Result.success() else Result.retry()
    }
}
