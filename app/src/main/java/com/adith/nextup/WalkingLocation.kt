package com.adith.nextup

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class WalkingLocation(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    fun precise() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun background() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun enabled() = manager.isLocationEnabled
    private fun pending() = PendingIntent.getBroadcast(context, 61,
        Intent(context, WalkingReceiver::class.java).setAction("${context.packageName}.MOVEMENT"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    @SuppressLint("MissingPermission")
    suspend fun current(): Location? {
        if (!precise() || !enabled()) return null
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { manager.hasProvider(it) && manager.isProviderEnabled(it) }
        val cached = providers.mapNotNull { manager.getLastKnownLocation(it) }.filter(::usable)
            .maxByOrNull { it.elapsedRealtimeNanos }
        if (cached != null) return cached
        for (provider in providers) {
            val fix = withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    manager.getCurrentLocation(provider, signal, context.mainExecutor) { if (cont.isActive) cont.resume(it) }
                }
            }
            if (fix != null && usable(fix)) return fix
        }
        return null
    }
    private fun usable(location: Location) = location.hasAccuracy() &&
        usableFix((SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000, location.accuracy)
    @SuppressLint("MissingPermission")
    fun monitor(origin: Location): Boolean {
        if (!precise() || !background()) return false
        return try {
            manager.addProximityAlert(origin.latitude, origin.longitude, 200f, -1, pending())
            true
        } catch (_: Exception) { false }
    }
    fun stop() { try { manager.removeProximityAlert(pending()) } catch (_: Exception) { } }
}
