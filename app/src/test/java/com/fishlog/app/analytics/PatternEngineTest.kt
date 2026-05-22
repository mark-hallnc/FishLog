package com.fishlog.app.analytics

import com.fishlog.app.TestDataFactory.sampleCatch
import com.fishlog.app.TestDataFactory.sampleNoCatch
import com.fishlog.app.TestDataFactory.sampleTrip
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class PatternEngineTest {

    @Test
    fun catchRateIncludesNoCatchLogs() {
        val species = "Channel Catfish"
        val bait = "Chicken Liver"
        val logs = listOf(
            sampleCatch(id = 1, species = species, bait = bait),
            sampleCatch(id = 2, species = species, bait = bait),
            sampleNoCatch(id = 3, species = species, bait = bait),
            sampleNoCatch(id = 4, species = species, bait = bait)
        )
        val trips = emptyList<com.fishlog.app.data.FishingTrip>()

        val result = PatternEngine.analyze(logs, trips)
        
        val baitInsight = result.bestBaitsBySpecies.find { it.title == bait && it.subtitle == species }
        assertNotNull(baitInsight)
        assertEquals(0.5, baitInsight!!.catchRate, 0.01)
        assertEquals(4, baitInsight.observationCount)
    }

    @Test
    fun bestBaitUsesCatchAndNoCatchObservations() {
        val species = "Channel Catfish"
        val logs = listOf(
            // Chicken Liver: 3 catches, 1 no-catch -> 0.75
            sampleCatch(id = 1, species = species, bait = "Chicken Liver"),
            sampleCatch(id = 2, species = species, bait = "Chicken Liver"),
            sampleCatch(id = 3, species = species, bait = "Chicken Liver"),
            sampleNoCatch(id = 4, species = species, bait = "Chicken Liver"),
            // Hot Dog: 1 catch, 3 no-catches -> 0.25
            sampleCatch(id = 5, species = species, bait = "Hot Dog"),
            sampleNoCatch(id = 6, species = species, bait = "Hot Dog"),
            sampleNoCatch(id = 7, species = species, bait = "Hot Dog"),
            sampleNoCatch(id = 8, species = species, bait = "Hot Dog")
        )
        
        val result = PatternEngine.analyze(logs, emptyList())
        val baits = result.bestBaitsBySpecies
        
        val liverIndex = baits.indexOfFirst { it.title == "Chicken Liver" }
        val dogIndex = baits.indexOfFirst { it.title == "Hot Dog" }
        
        assertTrue("Chicken Liver index ($liverIndex) should be before Hot Dog index ($dogIndex)", liverIndex < dogIndex)
    }

    @Test
    fun depthBucketsAreAssignedCorrectly() {
        val logs = listOf(
            sampleCatch(id = 1, depthFeet = 3.0),
            sampleCatch(id = 2, depthFeet = 8.0),
            sampleCatch(id = 3, depthFeet = 14.0),
            sampleCatch(id = 4, depthFeet = 20.0),
            sampleCatch(id = 5, depthFeet = 30.0)
        )
        
        val result = PatternEngine.analyze(logs, emptyList())
        val depths = result.bestDepthRanges
        
        assertNotNull(depths.find { it.title == "0–5 ft" })
        assertNotNull(depths.find { it.title == "6–10 ft" })
        assertNotNull(depths.find { it.title == "11–15 ft" })
        assertNotNull(depths.find { it.title == "16–25 ft" })
        assertNotNull(depths.find { it.title == "26+ ft" })
    }

    @Test
    fun waterTempBucketsAreAssignedCorrectly() {
        val logs = listOf(
            sampleCatch(id = 1, waterTempF = 50.0),
            sampleCatch(id = 2, waterTempF = 60.0),
            sampleCatch(id = 3, waterTempF = 70.0),
            sampleCatch(id = 4, waterTempF = 80.0),
            sampleCatch(id = 5, waterTempF = 86.0)
        )
        
        val result = PatternEngine.analyze(logs, emptyList())
        val temps = result.bestWaterTempRanges
        
        assertNotNull(temps.find { it.title == "<55°F" })
        assertNotNull(temps.find { it.title == "55–64°F" })
        assertNotNull(temps.find { it.title == "65–74°F" })
        assertNotNull(temps.find { it.title == "75–84°F" })
        assertNotNull(temps.find { it.title == "85°F+" })
    }

    @Test
    fun timeOfDayBucketsAreAssignedCorrectly() {
        val cal = Calendar.getInstance()
        
        cal.set(Calendar.HOUR_OF_DAY, 8)
        val morningLog = sampleCatch(id = 1, timestamp = cal.timeInMillis)
        
        cal.set(Calendar.HOUR_OF_DAY, 13)
        val middayLog = sampleCatch(id = 2, timestamp = cal.timeInMillis)
        
        cal.set(Calendar.HOUR_OF_DAY, 18)
        val eveningLog = sampleCatch(id = 3, timestamp = cal.timeInMillis)
        
        cal.set(Calendar.HOUR_OF_DAY, 23)
        val nightLog = sampleCatch(id = 4, timestamp = cal.timeInMillis)
        
        val result = PatternEngine.analyze(listOf(morningLog, middayLog, eveningLog, nightLog), emptyList())
        val times = result.bestTimesOfDay
        
        assertNotNull(times.find { it.title.contains("Morning") })
        assertNotNull(times.find { it.title.contains("Midday") })
        assertNotNull(times.find { it.title.contains("Evening") })
        assertNotNull(times.find { it.title.contains("Night") })
    }

    @Test
    fun filtersByWaterBody() {
        val trip1 = sampleTrip(id = 1, waterBody = "Oak Hollow Lake")
        val trip2 = sampleTrip(id = 2, waterBody = "High Rock Lake")
        
        val logs = listOf(
            sampleCatch(id = 1, tripId = 1),
            sampleCatch(id = 2, tripId = 2)
        )
        
        val filters = PatternEngineFilters(waterBody = "Oak Hollow Lake")
        val result = PatternEngine.analyze(logs, listOf(trip1, trip2), filters)
        
        assertEquals(1, result.totalObservations)
        assertEquals(1, result.totalCatchLogs)
    }

    @Test
    fun handlesMissingFieldsWithoutCrashing() {
        val logs = listOf(
            sampleCatch(id = 1, species = "", bait = "", depthFeet = null, waterTempF = null)
        )
        
        val result = PatternEngine.analyze(logs, emptyList())
        assertEquals(1, result.totalCatchLogs)
    }
}
