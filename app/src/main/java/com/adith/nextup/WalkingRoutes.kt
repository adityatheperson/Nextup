package com.adith.nextup

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class WalkPoint(val latitude: Double, val longitude: Double, val label: String)
class RoutingFailure(val status: Int) : Exception("Routing unavailable")

class WalkingRoutes {
    suspend fun destination(text: String, lat: Double, lon: Double, key: String): WalkPoint? {
        val url = Uri.parse("https://api.openrouteservice.org/geocode/search").buildUpon()
            .appendQueryParameter("text", text).appendQueryParameter("size", "2")
            .appendQueryParameter("focus.point.lat", lat.toString()).appendQueryParameter("focus.point.lon", lon.toString()).build().toString()
        return parseDestination(request(url, key), text)
    }
    internal fun parseDestination(response: JSONObject, text: String): WalkPoint? {
        val features = response.optJSONArray("features") ?: return null
        if (features.length() == 0) return null
        val first = features.getJSONObject(0)
        val score = first.getJSONObject("properties").optDouble("confidence", Double.NaN)
        val second = if (features.length() > 1) features.getJSONObject(1).getJSONObject("properties").optDouble("confidence", Double.NaN) else null
        if (!confidentMatch(score, second)) return null
        val coordinates = first.getJSONObject("geometry").getJSONArray("coordinates")
        val longitude = coordinates.getDouble(0)
        val latitude = coordinates.getDouble(1)
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return WalkPoint(latitude, longitude, first.getJSONObject("properties").optString("label", text))
    }
    suspend fun seconds(lat: Double, lon: Double, destination: WalkPoint, key: String): Double? {
        val body = JSONObject().put("coordinates", JSONArray().put(JSONArray().put(lon).put(lat))
            .put(JSONArray().put(destination.longitude).put(destination.latitude))).put("instructions", false)
        return request("https://api.openrouteservice.org/v2/directions/foot-walking", key, body.toString())
            .optJSONArray("routes")?.optJSONObject(0)?.optJSONObject("summary")?.optDouble("duration", Double.NaN)
    }
    private suspend fun request(url: String, key: String, body: String? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", key)
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw RoutingFailure(status)
            val bytes = connection.inputStream.use { it.readNBytes(1_048_577) }
            check(bytes.size <= 1_048_576)
            JSONObject(String(bytes, Charsets.UTF_8))
        } finally { connection.disconnect() }
    }
}
