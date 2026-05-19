package com.fishlog.app.data

import java.util.*
import kotlin.math.cos
import kotlin.math.floor

data class MoonPhaseData(
    val phaseName: String,
    val illuminationPercent: Double,
    val ageDays: Double,
    val phaseFraction: Double,
    val waxing: Boolean,
    val calculatedAt: Long
)

object MoonPhaseCalculator {
    // Average lunar month in days
    private const val LUNAR_MONTH_DAYS = 29.530588853

    /**
     * Calculates an approximation of the moon phase for a given timestamp.
     * Based on a deterministic cycle from a known New Moon reference.
     */
    fun calculate(timestamp: Long): MoonPhaseData {
        val date = Date(timestamp)
        
        // Known New Moon: January 6, 2000, 18:14 UTC
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(2000, Calendar.JANUARY, 6, 18, 14, 0)
        val referenceNewMoon = calendar.timeInMillis

        val diffMillis = timestamp - referenceNewMoon
        val diffDays = diffMillis.toDouble() / (1000 * 60 * 60 * 24)
        
        // Phase fraction (0.0 to 1.0)
        var phaseFraction = (diffDays / LUNAR_MONTH_DAYS)
        phaseFraction -= floor(phaseFraction)
        if (phaseFraction < 0) phaseFraction += 1.0

        val ageDays = phaseFraction * LUNAR_MONTH_DAYS
        
        // Illumination approximation using cosine
        // phaseFraction 0 = New, 0.5 = Full
        val illuminationPercent = (1 - cos(2 * Math.PI * phaseFraction)) / 2 * 100

        val waxing = phaseFraction < 0.5

        val phaseName = when {
            phaseFraction < 0.03 -> "New Moon"
            phaseFraction < 0.22 -> "Waxing Crescent"
            phaseFraction < 0.28 -> "First Quarter"
            phaseFraction < 0.47 -> "Waxing Gibbous"
            phaseFraction < 0.53 -> "Full Moon"
            phaseFraction < 0.72 -> "Waning Gibbous"
            phaseFraction < 0.78 -> "Last Quarter"
            phaseFraction < 0.97 -> "Waning Crescent"
            else -> "New Moon"
        }

        return MoonPhaseData(
            phaseName = phaseName,
            illuminationPercent = illuminationPercent,
            ageDays = ageDays,
            phaseFraction = phaseFraction,
            waxing = waxing,
            calculatedAt = System.currentTimeMillis()
        )
    }
}
