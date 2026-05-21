package com.fishlog.app.analytics

import com.fishlog.app.ui.DateRangeFilter

data class PatternEngineResult(
    val totalCatchLogs: Int,
    val totalNoCatchLogs: Int,
    val totalObservations: Int,
    val topPattern: PatternInsight?,
    val bestBaitsBySpecies: List<PatternInsight>,
    val bestDepthRanges: List<PatternInsight>,
    val bestWaterTempRanges: List<PatternInsight>,
    val bestTimesOfDay: List<PatternInsight>,
    val bestWaterBodies: List<PatternInsight>,
    val moonPhasePatterns: List<PatternInsight>
)

data class PatternInsight(
    val title: String,
    val subtitle: String,
    val category: String,
    val catchCount: Int,
    val noCatchCount: Int,
    val observationCount: Int,
    val catchRate: Double,
    val confidenceLabel: String,
    val matchingLogIds: List<Long> = emptyList(),
    val patternType: PatternType
)

enum class PatternType {
    BAIT_BY_SPECIES,
    DEPTH_RANGE,
    WATER_TEMP_RANGE,
    TIME_OF_DAY,
    WATER_BODY,
    MOON_PHASE,
    TOP_PATTERN
}

data class PatternEngineFilters(
    val species: String? = null,
    val waterBody: String? = null,
    val dateRange: DateRangeFilter = DateRangeFilter.AllDates
)
