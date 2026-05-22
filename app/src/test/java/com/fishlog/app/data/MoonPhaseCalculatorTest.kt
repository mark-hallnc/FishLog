package com.fishlog.app.data

import org.junit.Assert.*
import org.junit.Test

class MoonPhaseCalculatorTest {

    @Test
    fun illuminationIsWithinValidRange() {
        val now = System.currentTimeMillis()
        val moonData = MoonPhaseCalculator.calculate(now)
        assertTrue(moonData.illuminationPercent in 0.0..100.0)
    }

    @Test
    fun phaseFractionIsWithinValidRange() {
        val now = System.currentTimeMillis()
        val moonData = MoonPhaseCalculator.calculate(now)
        assertTrue(moonData.phaseFraction in 0.0..1.0)
    }

    @Test
    fun phaseNameIsNotBlank() {
        val now = System.currentTimeMillis()
        val moonData = MoonPhaseCalculator.calculate(now)
        assertTrue(moonData.phaseName.isNotBlank())
    }

    @Test
    fun knownFullMoon() {
        // Known Full Moon approx: Feb 24, 2024
        val cal = java.util.Calendar.getInstance()
        cal.set(2024, java.util.Calendar.FEBRUARY, 24, 12, 0)
        val moonData = MoonPhaseCalculator.calculate(cal.timeInMillis)
        
        // Should be around 100% illumination
        assertTrue(moonData.illuminationPercent > 95.0)
        assertEquals("Full Moon", moonData.phaseName)
    }

    @Test
    fun knownNewMoon() {
        // Known New Moon approx: Feb 9, 2024
        val cal = java.util.Calendar.getInstance()
        cal.set(2024, java.util.Calendar.FEBRUARY, 9, 12, 0)
        val moonData = MoonPhaseCalculator.calculate(cal.timeInMillis)
        
        // Should be around 0% illumination
        assertTrue(moonData.illuminationPercent < 5.0)
        assertEquals("New Moon", moonData.phaseName)
    }
}
