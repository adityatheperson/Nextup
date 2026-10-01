package com.adith.nextup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

class AgendaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DISMISS) {
            val pending = goAsync()
            val app = context.applicationContext as NextUpApp
            app.scope.launch {
                try { app.coordinator.dismiss(intent.getLongExtra(EXTRA_GENERATION, -1)) }
                finally { pending.finish() }
            }
        } else {
            // Work survives the short receiver lifetime and runs only after unlock.
            RefreshScheduler(context).enqueueRefresh()
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.adith.nextup.DISMISS"
        const val EXTRA_GENERATION = "generation"
    }
}
