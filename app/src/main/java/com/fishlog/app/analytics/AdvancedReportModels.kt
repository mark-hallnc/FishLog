package com.fishlog.app.analytics

data class ReportBucket(
    val label: String,
    val catchCount: Int,
    val noCatchCount: Int,
    val observationCount: Int,
    val catchRate: Double,
    val matchingLogIds: List<Long> = emptyList()
)

data class AdvancedReportsResult(
    val depthBuckets: List<ReportBucket>,
    val baitBuckets: List<ReportBucket>,
    val waterTempBuckets: List<ReportBucket>,
    val monthBuckets: List<ReportBucket>,
    val moonPhaseBuckets: List<ReportBucket>,
    val timeOfDayBuckets: List<ReportBucket>
)
