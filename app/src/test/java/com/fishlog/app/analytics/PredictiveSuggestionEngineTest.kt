package com.fishlog.app.analytics

import com.fishlog.app.TestDataFactory.sampleCatch
import com.fishlog.app.TestDataFactory.sampleNoCatch
import com.fishlog.app.TestDataFactory.sampleTrip
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class PredictiveSuggestionEngineTest {

    @Test
    fun noDataReturnsNotEnoughData() {
        val result = PredictiveSuggestionEngine.generate(emptyList(), emptyList())
        assertTrue(result.notEnoughData)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun underFiveObservationsReturnsNotEnoughData() {
        val logs = listOf(
            sampleCatch(id = 1),
            sampleCatch(id = 2),
            sampleCatch(id = 3),
            sampleCatch(id = 4)
        )
        val result = PredictiveSuggestionEngine.generate(logs, emptyList())
        assertTrue(result.notEnoughData)
    }

    @Test
    fun generatesSuggestionsWhenEnoughDataExists() {
        val species = "Channel Catfish"
        val bait = "Chicken Liver"
        val logs = mutableListOf<com.fishlog.app.data.CatchLog>()
        
        // Add 10 logs to ensure enough data and component counts
        for (i in 1..8) {
            logs.add(sampleCatch(id = i.toLong(), species = species, bait = bait, depthFeet = 3.0))
        }
        for (i in 9..10) {
            logs.add(sampleNoCatch(id = i.toLong(), species = species, bait = bait, depthFeet = 3.0))
        }
        
        val result = PredictiveSuggestionEngine.generate(logs, emptyList())
        assertFalse(result.notEnoughData)
        assertTrue(result.suggestions.isNotEmpty())
        
        val combo = result.suggestions.find { it.title == "Best starting setup" }
        assertNotNull(combo)
        assertTrue(combo!!.summary.contains(bait))
        assertTrue(combo.summary.contains("0–5 ft"))
    }

    @Test
    fun filtersBySpecies() {
        val catLogs = (1..5).map { sampleCatch(id = it.toLong(), species = "Catfish") }
        val bassLogs = (6..10).map { sampleCatch(id = it.toLong(), species = "Bass") }
        val allLogs = catLogs + bassLogs
        
        val inputs = PredictiveSuggestionInputs(species = "Catfish")
        val result = PredictiveSuggestionEngine.generate(allLogs, emptyList(), inputs)
        
        assertEquals(5, result.totalObservations)
    }

    @Test
    fun filtersByWaterBody() {
        val trip1 = sampleTrip(id = 1, waterBody = "Lake A")
        val trip2 = sampleTrip(id = 2, waterBody = "Lake B")
        
        val logs1 = (1..5).map { sampleCatch(id = it.toLong(), tripId = 1) }
        val logs2 = (6..10).map { sampleCatch(id = it.toLong(), tripId = 2) }
        
        val inputs = PredictiveSuggestionInputs(waterBody = "Lake A")
        val result = PredictiveSuggestionEngine.generate(logs1 + logs2, listOf(trip1, trip2), inputs)
        
        assertEquals(5, result.totalObservations)
    }

    @Test
    fun timeOfDayInputBoostsMatchingSuggestion() {
        val cal = Calendar.getInstance()
        val morningLogs = mutableListOf<com.fishlog.app.data.CatchLog>()
        val eveningLogs = mutableListOf<com.fishlog.app.data.CatchLog>()
        
        cal.set(Calendar.HOUR_OF_DAY, 8)
        // Ensure bait/depth are different so they don't form a generic combo that overrides time intent
        for (i in 1..10) morningLogs.add(sampleCatch(id = i.toLong(), timestamp = cal.timeInMillis, bait = "Bait Morning", depthFeet = 5.0))
        
        cal.set(Calendar.HOUR_OF_DAY, 18)
        for (i in 11..20) eveningLogs.add(sampleCatch(id = i.toLong(), timestamp = cal.timeInMillis, bait = "Bait Evening", depthFeet = 15.0))
        
        val allLogs = morningLogs + eveningLogs
        
        // Scenario 1: Ask for Morning
        val morningInputs = PredictiveSuggestionInputs(timeOfDay = SuggestionTimeOfDay.MORNING)
        val morningResult = PredictiveSuggestionEngine.generate(allLogs, emptyList(), morningInputs)
        
        // Check if any suggestion summary contains Morning
        val hasMorningMatch = morningResult.suggestions.any { 
            it.summary.contains("Morning", ignoreCase = true)
        }
        assertTrue("Suggestions should contain Morning since it matches input", hasMorningMatch)
    }

    @Test
    fun handlesMissingFieldsWithoutCrashing() {
        val logs = (1..10).map { sampleCatch(id = it.toLong(), bait = "", depthFeet = null) }
        val result = PredictiveSuggestionEngine.generate(logs, emptyList())
        assertNotNull(result)
    }
}
