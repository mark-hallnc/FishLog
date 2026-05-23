package com.fishlog.app.analytics

import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.ui.DateRangeFilter
import java.util.Calendar

object TripReviewEngine {

    fun generate(
        trip: FishingTrip,
        tripLogs: List<CatchLog>,
        allLogs: List<CatchLog>,
        allTrips: List<FishingTrip>
    ): TripReview? {
        if (trip.endTime == null || tripLogs.isEmpty()) return null

        val sections = mutableListOf<TripReviewSection>()

        // 1. What happened
        val catches = tripLogs.count { it.logType == "CATCH" }
        val noCatches = tripLogs.count { it.logType == "NO_CATCH" }
        val speciesSummary = tripLogs.filter { it.logType == "CATCH" && it.species.isNotBlank() }
            .groupBy { it.species }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        val speciesText = if (speciesSummary.isNotEmpty()) {
            val main = speciesSummary.first()
            if (speciesSummary.size > 1) {
                "${main.first} made up most of the action, with ${speciesSummary.drop(1).joinToString(", ") { it.first }} also mixed in."
            } else {
                "${main.first} was the primary focus of this trip."
            }
        } else ""

        sections.add(TripReviewSection(
            heading = "What happened",
            text = "You logged $catches catches and $noCatches no-catches on this trip. $speciesText".trim(),
            type = TripReviewSectionType.WHAT_HAPPENED
        ))

        // 2. What worked
        val successfulLogs = tripLogs.filter { it.logType == "CATCH" }
        if (successfulLogs.isNotEmpty()) {
            val topBait = successfulLogs.filter { it.bait.isNotBlank() }
                .groupingBy { it.bait }.eachCount().maxByOrNull { it.value }?.key
            
            val topDepth = successfulLogs.mapNotNull { it.depthFeet ?: it.depth.toDoubleOrNull() }
                .averageOrNull()?.let { bucketDepth(it) }

            val topTime = getTimeOfDay(successfulLogs.first().timestamp)

            val workedText = buildString {
                append("Most catches came ")
                if (topBait != null) append("on $topBait ")
                if (topDepth != null) append("in $topDepth of water ")
                append("during the $topTime.")
            }

            sections.add(TripReviewSection(
                heading = "What worked",
                text = workedText,
                type = TripReviewSectionType.WHAT_WORKED
            ))
        }

        // 3. What did not work
        val failedLogs = tripLogs.filter { it.logType == "NO_CATCH" }
        if (failedLogs.isNotEmpty()) {
            val failedBait = failedLogs.filter { it.bait.isNotBlank() }
                .groupingBy { it.bait }.eachCount().maxByOrNull { it.value }?.key
            
            val failedDepth = failedLogs.mapNotNull { it.depthFeet ?: it.depth.toDoubleOrNull() }
                .averageOrNull()?.let { bucketDepth(it) }

            val failedText = buildString {
                append("No-catch logs were ")
                if (failedBait != null) append("tied to $failedBait ")
                if (failedDepth != null) append("in $failedDepth of water ")
                append(". This suggests the pattern wasn't quite right for those spots.")
            }

            sections.add(TripReviewSection(
                heading = "What did not work",
                text = failedText,
                type = TripReviewSectionType.WHAT_DID_NOT_WORK
            ))
        }

        // 4. Conditions
        val conditionsText = buildString {
            val sky = trip.weatherSummary.ifBlank { trip.skyCondition }
            if (sky.isNotBlank()) append("$sky conditions ")
            if (trip.windSpeedMph != null) append("with ${trip.windSpeedMph.toInt()} mph winds ")
            if (trip.airTempF != null) append("and ${trip.airTempF.toInt()}°F air temp. ")
            if (trip.waterClarity.isNotBlank()) append("Water clarity was ${trip.waterClarity}. ")
            if (trip.moonPhaseName.isNotBlank()) append("The moon was ${trip.moonPhaseName} at ${trip.moonIlluminationPercent?.toInt() ?: 0}% illumination.")
        }
        if (conditionsText.isNotBlank()) {
            sections.add(TripReviewSection(
                heading = "Conditions",
                text = conditionsText.trim(),
                type = TripReviewSectionType.CONDITIONS
            ))
        }

        // 5. Compared to history
        val historicalLogs = allLogs.filter { it.tripId != trip.id }
        if (historicalLogs.size >= 5) {
            val primarySpecies = speciesSummary.firstOrNull()?.first
            val histFilters = PatternEngineFilters(
                species = primarySpecies,
                waterBody = trip.waterBody.ifBlank { null },
                dateRange = DateRangeFilter.AllDates
            )
            val histResult = PatternEngine.analyze(historicalLogs, allTrips.filter { it.id != trip.id }, histFilters)
            
            val historyText = if (histResult.totalObservations >= 3 && histResult.topPattern != null) {
                "Compared with your history, this trip lines up with your ${histResult.topPattern.title} pattern: it has performed well for you before."
            } else {
                "There is not enough specific past data to compare this trip against your history yet."
            }

            sections.add(TripReviewSection(
                heading = "Compared to your history",
                text = historyText,
                type = TripReviewSectionType.COMPARED_TO_HISTORY
            ))
        }

        return TripReview(
            tripId = trip.id,
            tripName = trip.name,
            waterBody = trip.waterBody,
            sections = sections,
            confidenceLabel = calculateConfidence(tripLogs.size),
            observationCount = tripLogs.size
        )
    }

    private fun calculateConfidence(count: Int): String = when {
        count <= 2 -> "Very early"
        count <= 5 -> "Light review"
        count <= 10 -> "Useful review"
        else -> "Stronger review"
    }

    private fun bucketDepth(d: Double): String = when {
        d <= 5 -> "0–5 ft"
        d <= 10 -> "6–10 ft"
        d <= 15 -> "11–15 ft"
        d <= 25 -> "16–25 ft"
        else -> "26+ ft"
    }

    private fun getTimeOfDay(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "morning"
            in 11..15 -> "midday"
            in 16..20 -> "evening"
            else -> "night"
        }
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
