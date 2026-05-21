package com.fishlog.app.analytics

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
    val confidenceLabel: String
)
