package com.adith.nextup

import android.content.Context
import android.location.Location
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class WalkingStatus(val configured: Boolean = false, val precise: Boolean = false,
    val background: Boolean = false, val message: String = "Add your openrouteservice key to enable walking times.")
data class WalkingResult(val events: List<CalendarEvent>, val status: WalkingStatus)

class WalkingService(context: Context) {
    private val settings = WalkingSettings(context)
    private val location = WalkingLocation(context)
    private val routes = WalkingRoutes()
    private val mutex = Mutex()
    private var origin: Location? = null
    private var cache = emptyMap<String, WalkEstimate?>()
    private var cacheAt = 0L
    @Volatile private var invalidated = false
    private val destinations = mutableMapOf<String, Pair<Long, WalkPoint>>()
    fun status(message: String? = null) = WalkingStatus(settings.key() != null, location.precise(), location.background(),
        message ?: when {
            settings.key() == null -> "Add your openrouteservice key to enable walking times."
            !location.precise() -> "Allow precise location to estimate walking times."
            !location.background() -> "Allow location all the time for movement updates."
            !location.enabled() -> "Turn on device location to update walking times."
            else -> "Updates after about 200 m of movement; Android may delay updates."
        })
    fun stop() { location.stop() }
    fun invalidate() { invalidated = true }
    suspend fun clearCache() = mutex.withLock { cache = emptyMap(); cacheAt = 0; origin = null }
    suspend fun configure(key: String?) = mutex.withLock {
        withContext(Dispatchers.IO) { if (key == null) settings.clear() else settings.save(key) }
        cache = emptyMap(); destinations.clear(); cacheAt = 0; origin = null
        location.stop()
    }
    fun retryDelay(now: Long) = maxOf(routingDelay(now, settings.lastAttempt),
        (settings.retryAfter - now).coerceIn(0, 60 * 60_000L))

    suspend fun enrich(events: List<CalendarEvent>, current: () -> Boolean): WalkingResult = mutex.withLock {
        if (invalidated) { cache = emptyMap(); cacheAt = 0; origin = null; invalidated = false }
        fun result(message: String? = null, estimates: Map<String, WalkEstimate?> = emptyMap()) = WalkingResult(
            events.map { it.copy(walk = estimates[it.location]?.takeIf { e -> e.fresh(System.currentTimeMillis()) }) }, status(message))
        if (!current()) return@withLock result()
        val key = settings.key() ?: return@withLock result()
        // A deliberate all-the-time opt-in is required for this background feature.
        if (!location.precise() || !location.background() || !location.enabled()) {
            location.stop(); cache = emptyMap(); return@withLock result()
        }
        val targets = events.map { it.location }.filter(::isPhysicalLocation).distinct()
        if (targets.isEmpty()) { location.stop(); return@withLock result("No physical event locations to route.") }
        try {
            val fix = location.current() ?: return@withLock result("Waiting for a recent, precise location.")
            if (!current()) return@withLock result()
            val now = System.currentTimeMillis()
            val moved = origin?.distanceTo(fix)?.let { it >= 100f } ?: true
            if (moved) cache = emptyMap()
            if (!moved && now >= cacheAt && now - cacheAt < WALK_TTL && targets.all { cache.containsKey(it) }) {
                location.monitor(origin ?: fix)
                return@withLock result(estimates = cache)
            }
            val monitor = location.monitor(fix)
            if (retryDelay(now) > 0) return@withLock result("Waiting briefly before refreshing walking times.", cache)
            settings.lastAttempt = now
            val next = mutableMapOf<String, WalkEstimate?>()
            for (target in targets) {
                if (!current() || !location.precise() || !location.background()) return@withLock result()
                val cached = destinations[target]?.takeIf { now >= it.first && now - it.first < 86_400_000 }
                val destination = cached?.second ?: routes.destination(target, fix.latitude, fix.longitude, key)
                if (destination == null) { next[target] = null; continue }
                destinations[target] = now to destination
                if (!current()) return@withLock result()
                val seconds = try { routes.seconds(fix.latitude, fix.longitude, destination, key) }
                    catch (e: RoutingFailure) { if (e.status == 404) null else throw e }
                next[target] = seconds?.let(::walkingMinutes)?.let { WalkEstimate(it, now, destination.label) }
            }
            if (!current() || !location.precise() || !location.background()) return@withLock result()
            cache = next; cacheAt = now; origin = fix
            result(if (!monitor) "Movement monitoring unavailable; using periodic refresh."
                else if (next.values.all { it == null }) "No clear walking route within 40 minutes."
                else "Movement updates on · only walks of 40 minutes or less.", next)
        } catch (e: CancellationException) { throw e }
        catch (e: RoutingFailure) {
            settings.retryAfter = System.currentTimeMillis() + when (e.status) {
                401, 403 -> 60 * 60_000L
                429 -> 15 * 60_000L
                else -> WALK_COOLDOWN
            }
            result(if (e.status in listOf(401, 403)) "Key rejected. Check your openrouteservice key."
                else if (e.status == 429) "Routing limit reached. Will retry later." else "Walking routes unavailable. Will retry later.")
        } catch (_: Exception) { result("Walking times unavailable. Check location and connection.") }
    }
}
