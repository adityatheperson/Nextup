package com.adith.nextup

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NextUpApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val coordinator by lazy { RefreshCoordinator(this) }
}
