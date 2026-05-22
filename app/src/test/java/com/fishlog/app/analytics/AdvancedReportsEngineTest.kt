package com.fishlog.app.analytics

import com.fishlog.app.TestDataFactory.sampleCatch
import com.fishlog.app.TestDataFactory.sampleNoCatch
import com.fishlog.app.TestDataFactory.sampleTrip
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class AdvancedReportsEngineTest {

    @Test
    fun baitReportCalculatesCatchRate() {
        val logs = listOf(
            sampleCatch(id = 1, bait = "Chicken Liver"),
            sampleCatch(id = 2, bait = "Chicken Liver"),
            sampleCatch(id = 3, bait = "Chicken Liver"),
            sampleNoCatch(id = 4, bait = "Chicken Liver")
        )
        
        val result = AdvancedReportsEngine.buildReports(logs, emptyList())
        val baitBucket = result.baitBuckets.find { it.label == "Chicken Liver" }
        
        assertNotNull(baitBucket)
        assertEquals(0.75, baitBucket!!.catchRate, 0.01)
        assertEquals(4, baitBucket.observationCount)
    }

    @Test
    fun monthReportGroupsAcrossYears() {
        val cal = Calendar.getInstance()
        
        cal.set(2025, Calendar.JANUARY, 10)
        val log1 = sampleCatch(id = 1, timestamp = cal.timeInMillis)
        
        cal.set(2026, Calendar.JANUARY, 15)
        val log2 = sampleCatch(id = 2, timestamp = cal.timeInMillis)
        
        val result = AdvancedReportsEngine.buildReports(listOf(log1, log2), emptyList())
        val janBucket = result.monthBuckets.find { it.label == "Jan" }
        
        assertNotNull(janBucket)
        assertEquals(2, janBucket!!.observationCount)
    }

    @Test
    fun moonPhaseReportUsesTripMoonPhase() {
        val trip = sampleTrip(id = 1, moonPhaseName = "Full Moon")
        val log = sampleCatch(id = 1, tripId = 1)
        
        val result = AdvancedReportsEngine.buildReports(listOf(log), listOf(trip))
        val moonBucket = result.moonPhaseBuckets.find { it.label == "Full Moon" }
        
        assertNotNull(moonBucket)
        assertEquals(1, moonBucket!!.observationCount)
    }

    @Test
    fun reportBucketsCarryMatchingLogIds() {
        val logs = listOf(
            sampleCatch(id = 10, bait = "Minnow"),
            sampleNoCatch(id = 20, bait = "Minnow")
        )
        
        val result = AdvancedReportsEngine.buildReports(logs, emptyList())
        val baitBucket = result.baitBuckets.find { it.label == "Minnow" }
        
        assertNotNull(baitBucket)
        assertTrue(baitBucket!!.matchingLogIds.contains(10L))
        assertTrue(baitBucket.matchingLogIds.contains(20L))
    }
}
