package com.fishlog.app.data

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
    /**
     * Placeholder for fetching weather data for a given location and time.
     * TODO: Integrate with Open-Meteo or another weather API.
     */
    suspend fun fetchWeatherForLocation(
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ): Result<WeatherData> {
        // For now, return a placeholder failure since API is not connected.
        return Result.failure(Exception("Weather auto-fill is not connected yet."))
    }
}
