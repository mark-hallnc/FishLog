package com.fishlog.app.data

import org.junit.Assert.*
import org.junit.Test

class WeatherParserTest {

    private val sampleJson = """
    {
      "latitude":35.969963,
      "longitude":-80.01754,
      "current":{
        "time":"2026-05-19T18:45",
        "interval":900,
        "temperature_2m":89.4,
        "relative_humidity_2m":31,
        "apparent_temperature":87.2,
        "precipitation":0.000,
        "weather_code":0,
        "cloud_cover":0,
        "pressure_msl":1018.6,
        "surface_pressure":986.4,
        "wind_speed_10m":8.8,
        "wind_direction_10m":187,
        "wind_gusts_10m":12.1
      }
    }
    """.trimIndent()

    @Test
    fun parsesZeroWeatherCodeAsClear() {
        val result = WeatherParser.parseCurrentWeatherJson(sampleJson)
        assertTrue(result.isSuccess)
        val data = result.getOrNull()!!
        assertEquals(0, data.weatherCode)
        assertEquals("Clear", data.weatherSummary)
    }

    @Test
    fun parsesZeroCloudCoverAndPrecipitation() {
        val result = WeatherParser.parseCurrentWeatherJson(sampleJson)
        val data = result.getOrNull()!!
        assertEquals(0.0, data.cloudCoverPercent!!, 0.01)
        assertEquals(0.0, data.precipitationIn!!, 0.01)
    }

    @Test
    fun parsesPressureWindHumidity() {
        val result = WeatherParser.parseCurrentWeatherJson(sampleJson)
        val data = result.getOrNull()!!
        assertEquals(1018.6, data.barometricPressureHpa!!, 0.01)
        assertEquals(8.8, data.windSpeedMph!!, 0.01)
        assertEquals(187.0, data.windDirectionDegrees!!, 0.01)
        assertEquals(31.0, data.humidityPercent!!, 0.01)
    }

    @Test
    fun missingCurrentObjectFailsGracefully() {
        val badJson = """{"latitude": 0}"""
        val result = WeatherParser.parseCurrentWeatherJson(badJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun pressureFallbackUsesSurfacePressure() {
        val fallbackJson = """
        {
          "current": {
            "temperature_2m": 70,
            "surface_pressure": 950.5
          }
        }
        """.trimIndent()
        val result = WeatherParser.parseCurrentWeatherJson(fallbackJson)
        val data = result.getOrNull()!!
        assertEquals(950.5, data.barometricPressureHpa!!, 0.01)
    }
}
