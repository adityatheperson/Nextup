package com.adith.nextup

import kotlin.math.ceil

const val WALK_TTL = 15 * 60_000L
const val WALK_COOLDOWN = 2 * 60_000L
data class WalkEstimate(val minutes: Int, val checkedAt: Long, val destination: String) {
    fun fresh(now: Long) = now >= checkedAt && now - checkedAt < WALK_TTL
}
fun walkingMinutes(seconds: Double): Int? = if (seconds.isFinite() && seconds >= 0 && seconds <= 2400)
    ceil(seconds / 60).toInt().coerceAtLeast(1) else null
fun usableFix(ageMillis: Long, accuracy: Float) = ageMillis in 0..120_000 && accuracy.isFinite() && accuracy in 0f..100f
fun isPhysicalLocation(text: String): Boolean {
    val value = text.trim().lowercase()
    return value.isNotEmpty() && !Regex("https?://|www\\.|zoom|meet\\.google|teams\\.microsoft|webex|online|virtual").containsMatchIn(value)
}
fun confidentMatch(first: Double, second: Double?) = first.isFinite() && first >= .8 &&
    (second == null || (second.isFinite() && first - second >= .15))
fun routingDelay(now: Long, last: Long): Long = if (last == 0L) 0 else
    (WALK_COOLDOWN - (now - last)).coerceIn(0, WALK_COOLDOWN)
