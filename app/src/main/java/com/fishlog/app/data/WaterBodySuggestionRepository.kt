package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WaterBodySuggestionRepository(context: Context) {
    private val TAG = "FishLogWaterBodies"
    private val prefs: SharedPreferences = context.getSharedPreferences("water_body_suggestions_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_CACHED_JSON = "cached_water_bodies_json"
        private const val KEY_LAST_LAT = "last_fetch_lat"
        private const val KEY_LAST_LON = "last_fetch_lon"
        private const val KEY_LAST_FETCH_AT = "last_fetch_at"
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    suspend fun fetchNearbyWaterBodies(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 5000
    ): Result<List<NearbyWaterBody>> = withContext(Dispatchers.IO) {
        val query = """
            [out:json][timeout:8];
            (
              way["natural"="water"]["name"](around:$radiusMeters,$latitude,$longitude);
              relation["natural"="water"]["name"](around:$radiusMeters,$latitude,$longitude);
              way["water"~"lake|reservoir|pond"]["name"](around:$radiusMeters,$latitude,$longitude);
              relation["water"~"lake|reservoir|pond"]["name"](around:$radiusMeters,$latitude,$longitude);
              way["waterway"="river"]["name"](around:$radiusMeters,$latitude,$longitude);
              relation["waterway"="river"]["name"](around:$radiusMeters,$latitude,$longitude);
            );
            out center tags 15;
        """.trimIndent()

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://overpass-api.de/api/interpreter?data=$encodedQuery")
            Log.d(TAG, "Fetching nearby water bodies (radius: ${radiusMeters}m) from Overpass at $latitude, $longitude")
            
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val results = parseOverpassJson(responseBody, latitude, longitude)
                cacheNearbyWaterBodies(latitude, longitude, results)
                Result.success(results)
            } else {
                Log.e(TAG, "Overpass API error: ${connection.responseCode}")
                Result.failure(Exception("API returned ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching water bodies (radius: ${radiusMeters}m)", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseOverpassJson(jsonString: String, centerLat: Double, centerLon: Double): List<NearbyWaterBody> {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val elements = root["elements"]?.jsonArray ?: return emptyList()
        
        return elements.mapNotNull { element ->
            val obj = element.jsonObject
            val tags = obj["tags"]?.jsonObject ?: return@mapNotNull null
            val name = tags["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            
            val lat = obj["lat"]?.jsonPrimitive?.doubleOrNull ?: obj["center"]?.jsonObject?.get("lat")?.jsonPrimitive?.doubleOrNull
            val lon = obj["lon"]?.jsonPrimitive?.doubleOrNull ?: obj["center"]?.jsonObject?.get("lon")?.jsonPrimitive?.doubleOrNull
            
            val type = tags["water"]?.jsonPrimitive?.content 
                ?: tags["waterway"]?.jsonPrimitive?.content 
                ?: tags["natural"]?.jsonPrimitive?.content 
                ?: "water"

            NearbyWaterBody(
                name = name,
                latitude = lat,
                longitude = lon,
                type = type,
                distanceMeters = if (lat != null && lon != null) calculateDistance(centerLat, centerLon, lat, lon) else null
            )
        }.distinctBy { it.name.lowercase() }
         .sortedBy { it.distanceMeters ?: Double.MAX_VALUE }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return r * c
    }

    fun getCachedNearbyWaterBodies(): List<NearbyWaterBody> {
        val jsonString = prefs.getString(KEY_CACHED_JSON, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<NearbyWaterBody>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isCacheValid(lat: Double, lon: Double): Boolean {
        val lastLatStr = prefs.getString(KEY_LAST_LAT, null)
        val lastLonStr = prefs.getString(KEY_LAST_LON, null)
        val lastFetchAt = prefs.getLong(KEY_LAST_FETCH_AT, 0L)
        
        if (lastLatStr == null || lastLonStr == null || lastFetchAt == 0L) return false
        
        val now = System.currentTimeMillis()
        if (now - lastFetchAt > CACHE_TTL_MS) return false
        
        val lastLat = lastLatStr.toDoubleOrNull() ?: return false
        val lastLon = lastLonStr.toDoubleOrNull() ?: return false
        
        // Roughly 40km / 25 miles
        val distance = calculateDistance(lat, lon, lastLat, lastLon)
        return distance < 40000
    }

    private fun cacheNearbyWaterBodies(lat: Double, lon: Double, results: List<NearbyWaterBody>) {
        val jsonString = json.encodeToString(results)
        prefs.edit()
            .putString(KEY_CACHED_JSON, jsonString)
            .putString(KEY_LAST_LAT, lat.toString())
            .putString(KEY_LAST_LON, lon.toString())
            .putLong(KEY_LAST_FETCH_AT, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Cached ${results.size} water bodies")
    }
}
