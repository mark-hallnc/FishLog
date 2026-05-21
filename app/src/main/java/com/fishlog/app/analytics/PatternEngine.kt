package com.fishlog.app.analytics

import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.ui.DateRangeFilter
import java.util.Calendar

object PatternEngine {

    fun analyze(
        logs: List<CatchLog>, 
        trips: List<FishingTrip>,
        filters: PatternEngineFilters = PatternEngineFilters()
    ): PatternEngineResult {
        val tripById = trips.associateBy { it.id }
        
        // 1. Filter the base log set
        val filteredLogs = logs.filter { log ->
            // Date Filter
            if (!filters.dateRange.matches(log.timestamp)) return@filter false

            // Water Body Filter
            if (filters.waterBody != null && filters.waterBody != "All Water Bodies") {
                val trip = tripById[log.tripId]
                val wb = trip?.waterBody?.trim()
                if (wb?.equals(filters.waterBody, ignoreCase = true) != true) return@filter false
            }

            // Species Filter
            if (filters.species != null && filters.species != "All Species") {
                // If it's a catch, must match species
                if (log.logType == "CATCH") {
                    if (!log.species.equals(filters.species, ignoreCase = true)) return@filter false
                }
                // If it's a no-catch, we include it if it's "general" (blank species) 
                // because it represents a trip/observation where the target might have been that species.
                // This preserves catch rate context.
                else if (log.logType == "NO_CATCH") {
                    if (log.species.isNotBlank() && !log.species.equals(filters.species, ignoreCase = true)) return@filter false
                }
            }

            true
        }

        val catchLogs = filteredLogs.filter { it.logType == "CATCH" }
        val noCatchLogs = filteredLogs.filter { it.logType == "NO_CATCH" }
        
        val totalCatchLogs = catchLogs.size
        val totalNoCatchLogs = noCatchLogs.size
        val totalObservations = totalCatchLogs + totalNoCatchLogs

        // 2. Compute Insights from filtered set
        // Best Baits by Species
        val baitInsights = filteredLogs.filter { it.bait.isNotBlank() }
            .groupBy { "${it.species}|${it.bait}" }
            .map { (key, group) ->
                val parts = key.split("|")
                val species = parts[0]
                val bait = parts[1]
                createInsight(
                    title = bait,
                    subtitle = if (species.isNotBlank() && species != "No Catch") species else "General",
                    category = "Bait",
                    group = group,
                    type = PatternType.BAIT_BY_SPECIES
                )
            }
            .filter { it.observationCount >= 2 }
            .sortedWith(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount })
            .take(5)

        // Depth Ranges
        val depthInsights = filteredLogs.mapNotNull { log ->
            val depth = log.depthFeet ?: log.depth.toDoubleOrNull()
            depth?.let { log to it }
        }.groupBy { (_, d) ->
            when {
                d <= 5 -> "0–5 ft"
                d <= 10 -> "6–10 ft"
                d <= 15 -> "11–15 ft"
                d <= 25 -> "16–25 ft"
                else -> "26+ ft"
            }
        }.map { (range, group) ->
            createInsight(range, "Depth", "Depth", group.map { it.first }, PatternType.DEPTH_RANGE)
        }.sortedWith(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount })
        .take(5)

        // Water Temp Ranges
        val tempInsights = filteredLogs.mapNotNull { log ->
            val temp = log.waterTempF ?: log.waterTemp.toDoubleOrNull()
            temp?.let { log to it }
        }.groupBy { (_, t) ->
            when {
                t < 55 -> "<55°F"
                t < 65 -> "55–64°F"
                t < 75 -> "65–74°F"
                t < 85 -> "75–84°F"
                else -> "85°F+"
            }
        }.map { (range, group) ->
            createInsight(range, "Water Temp", "Temp", group.map { it.first }, PatternType.WATER_TEMP_RANGE)
        }.sortedWith(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount })
        .take(5)

        // Time of Day
        val timeInsights = filteredLogs.groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..10 -> "Morning (5A-11A)"
                in 11..15 -> "Midday (11A-4P)"
                in 16..20 -> "Evening (4P-9P)"
                else -> "Night (9P-5A)"
            }
        }.map { (time, group) ->
            createInsight(time, "Time of Day", "Time", group, PatternType.TIME_OF_DAY)
        }.sortedWith(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount })
        .take(5)

        // Water Bodies
        val waterBodyInsights = filteredLogs.mapNotNull { log ->
            val trip = tripById[log.tripId]
            val wb = trip?.waterBody?.trim()
            if (!wb.isNullOrBlank()) log to wb else null
        }.groupBy { it.second }
        .map { (wb, group) ->
            createInsight(wb, "Water Body", "Location", group.map { it.first }, PatternType.WATER_BODY)
        }.sortedWith(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount })
        .take(5)

        // Moon Phase
        val moonInsights = filteredLogs.mapNotNull { log ->
            val trip = tripById[log.tripId]
            val moon = trip?.moonPhaseName
            if (!moon.isNullOrBlank()) log to moon else null
        }.groupBy { it.second }
        .map { (moon, group) ->
            createInsight(moon, "Moon Phase", "Moon", group.map { it.first }, PatternType.MOON_PHASE)
        }.sortedWith(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount })
        .take(5)

        // Top Pattern
        val allInsights = baitInsights + depthInsights + tempInsights + timeInsights + waterBodyInsights + moonInsights
        val topPattern = allInsights.filter { it.observationCount >= 3 }
            .maxWithOrNull(compareByDescending<PatternInsight> { it.catchRate }.thenByDescending { it.catchCount }.thenByDescending { it.observationCount })

        return PatternEngineResult(
            totalCatchLogs = totalCatchLogs,
            totalNoCatchLogs = totalNoCatchLogs,
            totalObservations = totalObservations,
            topPattern = topPattern,
            bestBaitsBySpecies = baitInsights,
            bestDepthRanges = depthInsights,
            bestWaterTempRanges = tempInsights,
            bestTimesOfDay = timeInsights,
            bestWaterBodies = waterBodyInsights,
            moonPhasePatterns = moonInsights
        )
    }

    private fun createInsight(
        title: String, 
        subtitle: String, 
        category: String, 
        group: List<CatchLog>,
        type: PatternType
    ): PatternInsight {
        val catches = group.count { it.logType == "CATCH" }
        val noCatches = group.count { it.logType == "NO_CATCH" }
        val total = catches + noCatches
        val rate = if (total > 0) catches.toDouble() / total.toDouble() else 0.0
        
        val confidence = when {
            total < 3 -> "Very early"
            total <= 5 -> "Early pattern"
            total <= 10 -> "Developing pattern"
            else -> "Stronger pattern"
        }

        return PatternInsight(
            title = title,
            subtitle = subtitle,
            category = category,
            catchCount = catches,
            noCatchCount = noCatches,
            observationCount = total,
            catchRate = rate,
            confidenceLabel = confidence,
            matchingLogIds = group.map { it.id },
            patternType = type
        )
    }
}
