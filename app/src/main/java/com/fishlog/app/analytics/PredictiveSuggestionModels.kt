package com.fishlog.app.analytics

data class PredictiveSuggestionInputs(
    val species: String? = null,
    val waterBody: String? = null,
    val timeOfDay: SuggestionTimeOfDay? = SuggestionTimeOfDay.ANY,
    val waterTempF: Double? = null,
    val depthFeet: Double? = null,
    val moonPhase: String? = null
)

enum class SuggestionTimeOfDay {
    ANY,
    MORNING,
    MIDDAY,
    EVENING,
    NIGHT
}

data class PredictiveSuggestionResult(
    val totalObservations: Int,
    val suggestions: List<PredictiveSuggestion>,
    val summary: String,
    val notEnoughData: Boolean
)

data class PredictiveSuggestion(
    val title: String,
    val summary: String,
    val why: List<String>,
    val confidenceLabel: String,
    val score: Double,
    val catchRate: Double,
    val catchCount: Int,
    val noCatchCount: Int,
    val observationCount: Int,
    val matchingLogIds: List<Long>,
    val components: List<SuggestionComponent> = emptyList()
)

data class SuggestionComponent(
    val label: String,
    val value: String,
    val catchCount: Int,
    val noCatchCount: Int,
    val observationCount: Int,
    val catchRate: Double,
    val confidenceLabel: String,
    val matchingLogIds: List<Long>
)
