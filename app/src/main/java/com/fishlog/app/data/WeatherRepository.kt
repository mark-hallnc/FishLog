package com.fishlog.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class WeatherData(
    val airTempF: Double?,
    val feelsLikeF: Double?,
    val humidityPercent: Double?,
    val windSpeedMph: Double?,
    val windDirectionDegrees: Double?,
    val windGustMph: Double?,
    val barometricPressureHpa: Double?,
    val cloudCoverPercent: Double?,
    val precipitationIn: Double?,
    val weatherCode: Int?,
    val weatherSummary: String,
    val fetchedAt: Long,
    val source: String
)

class WeatherRepository {
    private val TAG = "FishLogWeather"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchWeatherForLocation(
        latitude: Double,
        longitude: Double
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        val urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timezone=auto"
        
        Log.d(TAG, "Requesting weather: $urlString")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")

            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "Response body (first 500): ${responseBody.take(500)}")

                val root = json.parseToJsonElement(responseBody).jsonObject
                val current = root["current"]?.jsonObject ?: throw Exception("Weather response could not be read: 'current' object missing.")

                Log.d(TAG, "'current' object found in JSON")

                val code = current["weather_code"]?.jsonPrimitive?.intOrNull
                val pressure = current["pressure_msl"]?.jsonPrimitive?.doubleOrNull 
                    ?: current["surface_pressure"]?.jsonPrimitive?.doubleOrNull

                val data = WeatherData(
                    airTempF = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull,
                    feelsLikeF = current["apparent_temperature"]?.jsonPrimitive?.doubleOrNull,
                    humidityPercent = current["relative_humidity_2m"]?.jsonPrimitive?.doubleOrNull,
                    windSpeedMph = current["wind_speed_10m"]?.jsonPrimitive?.doubleOrNull,
                    windDirectionDegrees = current["wind_direction_10m"]?.jsonPrimitive?.doubleOrNull,
                    windGustMph = current["wind_gusts_10m"]?.jsonPrimitive?.doubleOrNull,
                    barometricPressureHpa = pressure,
                    cloudCoverPercent = current["cloud_cover"]?.jsonPrimitive?.doubleOrNull,
                    precipitationIn = current["precipitation"]?.jsonPrimitive?.doubleOrNull,
                    weatherCode = code,
                    weatherSummary = mapWeatherCodeToSummary(code),
                    fetchedAt = System.currentTimeMillis(),
                    source = "Open-Meteo"
                )

                Log.d(TAG, "Parsed WeatherData: $data")

                // Success criteria: at least one useful field must be present
                if (data.airTempF != null || data.feelsLikeF != null || data.humidityPercent != null || 
                    data.weatherCode != null || data.windSpeedMph != null || data.barometricPressureHpa != null || 
                    data.cloudCoverPercent != null) {
                    Result.success(data)
                } else {
                    Log.w(TAG, "No usable weather fields parsed from response")
                    Result.failure(Exception("Weather response did not include usable current conditions."))
                }
            } else {
                Log.e(TAG, "API error: $responseCode")
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "Error body: $errorBody")
                Result.failure(Exception("Weather request failed with error code $responseCode."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather", e)
            Result.failure(Exception("Weather request failed. You can enter conditions manually."))
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Maps WMO weather codes used by Open-Meteo to readable summaries.
     */
    fun mapWeatherCodeToSummary(code: Int?): String {
        return when (code) {
            0 -> "Clear"
            1, 2 -> "Partly Cloudy"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 65, 66, 67 -> "Rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Showers"
            95, 96, 99 -> "Storms"
            else -> "Unknown"
        }
    }

    /**
     * Maps wind speed in MPH to a qualitative descriptor for app fields.
     */
    fun mapWindSpeedToCondition(speedMph: Double?): String {
        val speed = speedMph ?: return ""
        return when {
            speed <= 3 -> "Calm"
            speed <= 10 -> "Light"
            speed <= 18 -> "Moderate"
            speed <= 30 -> "Strong"
            else -> "Gusty"
        }
    }

    /**
     * Internal test example for verify parsing of the sample JSON provided in the bug report.
     * 
     * val sampleJson = """
     * {
     *   "latitude": 35.969963,
     *   "longitude": -80.01754,
     *   "current": {
     *     "time": "2026-05-19T18:45",
     *     "interval": 900,
     *     "temperature_2m": 89.4,
     *     "relative_humidity_2m": 31,
     *     "apparent_temperature": 87.2,
     *     "precipitation": 0.000,
     *     "weather_code": 0,
     *     "cloud_cover": 0,
     *     "pressure_msl": 1018.6,
     *     "surface_pressure": 986.4,
     *     "wind_speed_10m": 8.8,
     *     "wind_direction_10m": 187,
     *     "wind_gusts_10m": 12.1
     *   }
     * }
     * """
     * 
     * Result:
     * airTempF: 89.4
     * humidityPercent: 31.0
     * weatherCode: 0
     * weatherSummary: "Clear"
     * cloudCoverPercent: 0.0
     * precipitationIn: 0.0
     * ...
     */
}


