package com.fishlog.app.analytics

import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import java.util.*

object AdvancedReportsEngine {

    fun buildReports(
        logs: List<CatchLog>,
        trips: List<FishingTrip>,
        filters: PatternEngineFilters = PatternEngineFilters()
    ): AdvancedReportsResult {
        val tripById = trips.associateBy { it.id }
        val filteredLogs = PatternEngine.filterLogs(logs, trips, filters)

        // 1. Depth
        val depthBuckets = filteredLogs.mapNotNull { log ->
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
        }.map { (label, group) ->
            createBucket(label, group.map { it.first })
        }.sortedBy { 
            when (it.label) {
                "0–5 ft" -> 1
                "6–10 ft" -> 2
                "11–15 ft" -> 3
                "16–25 ft" -> 4
                else -> 5
            }
        }

        // 2. Bait
        val baitBuckets = filteredLogs.filter { it.bait.isNotBlank() }
            .groupBy { it.bait.trim() }
            .map { (label, group) ->
                createBucket(label, group)
            }
            .sortedByDescending { it.observationCount }
            .take(10)

        // 3. Water Temp
        val tempBuckets = filteredLogs.mapNotNull { log ->
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
        }.map { (label, group) ->
            createBucket(label, group.map { it.first })
        }.sortedBy { 
            when (it.label) {
                "<55°F" -> 1
                "55–64°F" -> 2
                "65–74°F" -> 3
                "75–84°F" -> 4
                else -> 5
            }
        }

        // 4. Month
        val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthBuckets = filteredLogs.groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            monthLabels[cal.get(Calendar.MONTH)]
        }.map { (label, group) ->
            createBucket(label, group)
        }.sortedBy { monthLabels.indexOf(it.label) }

        // 5. Moon Phase
        val moonOrder = listOf(
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", 
            "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"
        )
        val moonBuckets = filteredLogs.mapNotNull { log ->
            val trip = tripById[log.tripId]
            val moon = trip?.moonPhaseName
            if (!moon.isNullOrBlank()) log to moon else null
        }.groupBy { it.second }
        .map { (label, group) ->
            createBucket(label, group.map { it.first })
        }.sortedBy { moonOrder.indexOf(it.label) }

        // 6. Time of Day
        val timeLabels = listOf("Morning (5A-11A)", "Midday (11A-4P)", "Evening (4P-9P)", "Night (9P-5A)")
        val timeBuckets = filteredLogs.groupBy { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..10 -> timeLabels[0]
                in 11..15 -> timeLabels[1]
                in 16..20 -> timeLabels[2]
                else -> timeLabels[3]
            }
        }.map { (label, group) ->
            createBucket(label, group)
        }.sortedBy { timeLabels.indexOf(it.label) }

        return AdvancedReportsResult(
            depthBuckets = depthBuckets,
            baitBuckets = baitBuckets,
            waterTempBuckets = tempBuckets,
            monthBuckets = monthBuckets,
            moonPhaseBuckets = moonBuckets,
            timeOfDayBuckets = timeBuckets
        )
    }

    private fun createBucket(label: String, logs: List<CatchLog>): ReportBucket {
        val catches = logs.count { it.logType == "CATCH" }
        val noCatches = logs.count { it.logType == "NO_CATCH" }
        val total = catches + noCatches
        val rate = if (total > 0) catches.toDouble() / total.toDouble() else 0.0
        return ReportBucket(
            label = label,
            catchCount = catches,
            noCatchCount = noCatches,
            observationCount = total,
            catchRate = rate,
            matchingLogIds = logs.map { it.id }
        )
    }
}
