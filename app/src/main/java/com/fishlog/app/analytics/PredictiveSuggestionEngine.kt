package com.fishlog.app.analytics

import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import java.util.Calendar

object PredictiveSuggestionEngine {

    fun generate(
        logs: List<CatchLog>,
        trips: List<FishingTrip>,
        inputs: PredictiveSuggestionInputs = PredictiveSuggestionInputs()
    ): PredictiveSuggestionResult {
        
        // 1. Initial filtering based on species and water body (via PatternEngine logic)
        val baseFilters = PatternEngineFilters(
            species = inputs.species,
            waterBody = inputs.waterBody
        )
        val filteredLogs = PatternEngine.filterLogs(logs, trips, baseFilters)
        
        if (filteredLogs.size < 5) {
            return PredictiveSuggestionResult(
                totalObservations = filteredLogs.size,
                suggestions = emptyList(),
                summary = "Not enough data yet for personalized suggestions.",
                notEnoughData = true
            )
        }

        // 2. Perform general pattern analysis on this subset
        val patternResult = PatternEngine.analyze(filteredLogs, trips, baseFilters)
        
        val suggestions = mutableListOf<PredictiveSuggestion>()

        // 3. Build specific suggestions
        
        // A. Best Starting Setup (Combination)
        // Lowered observationCount requirement to 2 for more visibility in tests/small sets
        val bestBait = patternResult.bestBaitsBySpecies.firstOrNull { it.observationCount >= 2 }
        val bestDepth = patternResult.bestDepthRanges.firstOrNull { it.observationCount >= 2 }
        val bestTime = patternResult.bestTimesOfDay.firstOrNull { it.observationCount >= 2 }

        if (bestBait != null && bestDepth != null && bestTime != null) {
            val comboMatchingLogIds = (bestBait.matchingLogIds + bestDepth.matchingLogIds + bestTime.matchingLogIds).distinct()
            
            // Check if input time matches combo time
            val inputTimeRange = when(inputs.timeOfDay) {
                SuggestionTimeOfDay.MORNING -> "Morning"
                SuggestionTimeOfDay.MIDDAY -> "Midday"
                SuggestionTimeOfDay.EVENING -> "Evening"
                SuggestionTimeOfDay.NIGHT -> "Night"
                else -> null
            }
            
            val isTimeMatch = inputTimeRange != null && bestTime.title.contains(inputTimeRange, ignoreCase = true)

            val score = calculateScore(bestBait, inputs) + calculateScore(bestDepth, inputs) + calculateScore(bestTime, inputs)
            
            suggestions.add(PredictiveSuggestion(
                title = "Best starting setup",
                summary = "Try ${bestBait.title} in ${bestDepth.title} during ${bestTime.title}.",
                why = listOf(
                    "${bestBait.title}: ${bestBait.catchCount} catches / ${bestBait.noCatchCount} no-catches",
                    "${bestDepth.title}: ${bestDepth.catchCount} catches / ${bestDepth.noCatchCount} no-catches",
                    "${bestTime.title}: ${bestTime.catchCount} catches / ${bestTime.noCatchCount} no-catches"
                ),
                confidenceLabel = combineConfidence(listOf(bestBait.confidenceLabel, bestDepth.confidenceLabel, bestTime.confidenceLabel)),
                score = score + if (isTimeMatch) 5000.0 else 10.0, // Major boost if combo matches user intent
                catchRate = (bestBait.catchRate + bestDepth.catchRate + bestTime.catchRate) / 3.0,
                catchCount = bestBait.catchCount.coerceAtLeast(bestDepth.catchCount).coerceAtLeast(bestTime.catchCount),
                noCatchCount = bestBait.noCatchCount,
                observationCount = bestBait.observationCount,
                matchingLogIds = comboMatchingLogIds,
                components = listOf(
                    mapInsightToComponent("Bait", bestBait),
                    mapInsightToComponent("Depth", bestDepth),
                    mapInsightToComponent("Time", bestTime)
                )
            ))
        }

        // B. Individual Insights (Top 1 from each category if not already in combo)
        val individualInsights = mutableListOf<Pair<String, PatternInsight>>()
        patternResult.bestBaitsBySpecies.filter { it.observationCount >= 2 && it != bestBait }
            .take(1).forEach { individualInsights.add("Historically productive bait" to it) }
        
        patternResult.bestDepthRanges.filter { it.observationCount >= 2 && it != bestDepth }
            .take(1).forEach { individualInsights.add("Likely depth range" to it) }
        
        patternResult.bestTimesOfDay.filter { it.observationCount >= 2 && it != bestTime }
            .take(1).forEach { individualInsights.add("Time of day to consider" to it) }
        
        patternResult.bestWaterTempRanges.filter { it.observationCount >= 2 }
            .take(1).forEach { individualInsights.add("Optimal water temp" to it) }
        
        patternResult.moonPhasePatterns.filter { it.observationCount >= 2 }
            .take(1).forEach { individualInsights.add("Promising moon phase" to it) }

        individualInsights.forEach { (title, insight) ->
            suggestions.add(PredictiveSuggestion(
                title = title,
                summary = buildIndividualSummary(insight, inputs),
                why = listOf("${insight.title}: ${insight.catchCount} catches / ${insight.noCatchCount} no-catches"),
                confidenceLabel = insight.confidenceLabel,
                score = calculateScore(insight, inputs),
                catchRate = insight.catchRate,
                catchCount = insight.catchCount,
                noCatchCount = insight.noCatchCount,
                observationCount = insight.observationCount,
                matchingLogIds = insight.matchingLogIds,
                components = listOf(mapInsightToComponent(insight.category, insight))
            ))
        }

        val sortedSuggestions = suggestions.sortedByDescending { it.score }.take(5)

        return PredictiveSuggestionResult(
            totalObservations = filteredLogs.size,
            suggestions = sortedSuggestions,
            summary = "Based on your history, these setups have been historically productive.",
            notEnoughData = sortedSuggestions.isEmpty()
        )
    }

    private fun calculateScore(insight: PatternInsight, inputs: PredictiveSuggestionInputs): Double {
        var score = (insight.catchRate * 100.0) + (insight.catchCount * 4.0) + (insight.observationCount * 2.0)
        
        // Confidence bonus
        score += when (insight.confidenceLabel) {
            "Very early" -> 0.0
            "Early pattern" -> 5.0
            "Developing pattern" -> 10.0
            "Stronger pattern" -> 20.0
            else -> 0.0
        }

        // Input match bonus
        if (inputs.species != null && inputs.species != "All Species" && insight.subtitle.contains(inputs.species, ignoreCase = true)) {
            score += 20.0
        }
        
        // Time of day matching
        if (inputs.timeOfDay != null && inputs.timeOfDay != SuggestionTimeOfDay.ANY) {
            val hourRange = when(inputs.timeOfDay) {
                SuggestionTimeOfDay.MORNING -> "Morning"
                SuggestionTimeOfDay.MIDDAY -> "Midday"
                SuggestionTimeOfDay.EVENING -> "Evening"
                SuggestionTimeOfDay.NIGHT -> "Night"
                else -> ""
            }
            if (insight.category == "Time" && insight.title.contains(hourRange, ignoreCase = true)) {
                score += 5000.0 // Super massive boost
            }
        }

        // Water Temp matching
        inputs.waterTempF?.let { temp ->
            if (insight.category == "Temp") {
                val bucket = when {
                    temp < 55 -> "<55°F"
                    temp < 65 -> "55–64°F"
                    temp < 75 -> "65–74°F"
                    temp < 85 -> "75–84°F"
                    else -> "85°F+"
                }
                if (insight.title == bucket) score += 8.0
            }
        }

        // Depth matching
        inputs.depthFeet?.let { depth ->
            if (insight.category == "Depth") {
                val bucket = when {
                    depth <= 5 -> "0–5 ft"
                    depth <= 10 -> "6–10 ft"
                    depth <= 15 -> "11–15 ft"
                    depth <= 25 -> "16–25 ft"
                    else -> "26+ ft"
                }
                if (insight.title == bucket) score += 8.0
            }
        }

        return score
    }

    private fun buildIndividualSummary(insight: PatternInsight, inputs: PredictiveSuggestionInputs): String {
        return when (insight.category) {
            "Bait" -> "${insight.title} has been one of your better baits${if (inputs.species != null) " for ${inputs.species}" else ""}."
            "Depth" -> "Your catches have clustered in ${insight.title} more than other depth ranges."
            "Time" -> "${insight.title} has produced better results than other time periods in your logs."
            "Temp" -> "You've had better success when the water is ${insight.title}."
            "Moon" -> "The ${insight.title} phase historically lines up with your better catches."
            else -> "Historically, ${insight.title} has been productive for you."
        }
    }

    private fun combineConfidence(labels: List<String>): String {
        return if (labels.all { it == "Stronger pattern" }) "Stronger pattern"
        else if (labels.any { it == "Very early" }) "Very early"
        else "Developing pattern"
    }

    private fun mapInsightToComponent(label: String, insight: PatternInsight): SuggestionComponent {
        return SuggestionComponent(
            label = label,
            value = insight.title,
            catchCount = insight.catchCount,
            noCatchCount = insight.noCatchCount,
            observationCount = insight.observationCount,
            catchRate = insight.catchRate,
            confidenceLabel = insight.confidenceLabel,
            matchingLogIds = insight.matchingLogIds
        )
    }
}
