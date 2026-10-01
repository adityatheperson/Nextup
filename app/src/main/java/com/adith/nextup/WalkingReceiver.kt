package com.adith.nextup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import kotlinx.coroutines.launch

class WalkingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.hasExtra(LocationManager.KEY_PROXIMITY_ENTERING) ||
            intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, true)) return
        val pending = goAsync()
        val app = context.applicationContext as NextUpApp
        app.scope.launch {
            try { app.coordinator.moved() } finally { pending.finish() }
        }
    }
}
