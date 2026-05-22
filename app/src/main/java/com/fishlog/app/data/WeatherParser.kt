package com.fishlog.app.data

import kotlinx.serialization.json.*

object WeatherParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseCurrentWeatherJson(jsonString: String): Result<WeatherData> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val current = root["current"]?.jsonObject ?: throw Exception("Weather response could not be read: 'current' object missing.")
            val hourly = root["hourly"]?.jsonObject

            val code = current["weather_code"]?.jsonPrimitive?.intOrNull
            val pressure = current["pressure_msl"]?.jsonPrimitive?.doubleOrNull 
                ?: current["surface_pressure"]?.jsonPrimitive?.doubleOrNull

            // Calculate pressure trend
            var trend: String? = null
            if (hourly != null && pressure != null) {
                try {
                    val times = hourly["time"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val pressures = hourly["pressure_msl"]?.jsonArray?.map { it.jsonPrimitive.doubleOrNull } ?: emptyList()
                    
                    val nowEpoch = System.currentTimeMillis() / 1000
                    val currentIndex = times.indexOfFirst { timeStr ->
                        val timeEpoch = java.time.LocalDateTime.parse(timeStr).atZone(java.time.ZoneId.systemDefault()).toEpochSecond()
                        timeEpoch > nowEpoch - 1800 
                    }.let { if (it == -1) pressures.size - 1 else it }

                    if (currentIndex >= 3) {
                        val prevPressure = pressures[currentIndex - 3]
                        if (prevPressure != null) {
                            val delta = pressure - prevPressure
                            trend = when {
                                delta >= 1.0 -> "Rising"
                                delta <= -1.0 -> "Falling"
                                else -> "Steady"
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silently fail trend calculation in parser
                }
            }

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
                pressureTrend = trend,
                fetchedAt = System.currentTimeMillis(),
                source = "Open-Meteo"
            )

            if (data.airTempF != null || data.feelsLikeF != null || data.humidityPercent != null || 
                data.weatherCode != null || data.windSpeedMph != null || data.barometricPressureHpa != null || 
                data.cloudCoverPercent != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("Weather response did not include usable current conditions."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseForecastJson(jsonString: String): Result<DailyForecastData> {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val daily = root["daily"]?.jsonObject ?: throw Exception("Forecast response missing 'daily' object")

            val code = daily["weather_code"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.intOrNull
            val high = daily["temperature_2m_max"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val low = daily["temperature_2m_min"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val wind = daily["wind_speed_10m_max"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val gust = daily["wind_gusts_10m_max"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull
            val dir = daily["wind_direction_10m_dominant"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.doubleOrNull

            val data = DailyForecastData(
                condition = mapWeatherCodeToSummary(code),
                weatherCode = code,
                highTempF = high,
                lowTempF = low,
                windSpeedMph = wind,
                windGustMph = gust,
                windDirectionDegrees = dir,
                fetchedAt = System.currentTimeMillis()
            )

            if (data.highTempF != null || data.lowTempF != null || data.weatherCode != null) {
                Result.success(data)
            } else {
                Result.failure(Exception("No usable forecast fields found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
}
