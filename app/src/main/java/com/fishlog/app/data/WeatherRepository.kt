package com.fishlog.app.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    val pressureTrend: String?,
    val fetchedAt: Long,
    val source: String
)

@Serializable
data class DailyForecastData(
    val condition: String,
    val weatherCode: Int?,
    val highTempF: Double?,
    val lowTempF: Double?,
    val windSpeedMph: Double?,
    val windGustMph: Double?,
    val windDirectionDegrees: Double?,
    val fetchedAt: Long
)

class WeatherRepository {
    private val TAG = "FishLogWeather"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchTodayForecast(
        latitude: Double,
        longitude: Double
    ): Result<DailyForecastData> = withContext(Dispatchers.IO) {
        val urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$latitude&longitude=$longitude" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant" +
                "&temperature_unit=fahrenheit&wind_speed_unit=mph&timezone=auto&forecast_days=1"

        Log.d(TAG, "Requesting forecast: $urlString")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                WeatherParser.parseForecastJson(responseBody)
            } else {
                Result.failure(Exception("Forecast API error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching forecast", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun fetchWeatherForLocation(
        latitude: Double,
        longitude: Double
    ): Result<WeatherData> = withContext(Dispatchers.IO) {
        val urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
                "&hourly=pressure_msl" +
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
                WeatherParser.parseCurrentWeatherJson(responseBody)
            } else {
                Log.e(TAG, "API error: $responseCode")
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "Error body: $errorBody")
                Result.failure(Exception("Weather is unavailable right now. You can enter conditions manually."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather", e)
            Result.failure(Exception("Weather request failed. You can enter conditions manually."))
        } finally {
            connection?.disconnect()
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
}
