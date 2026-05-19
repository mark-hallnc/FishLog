package com.fishlog.app.data

import java.util.*

object InsightsCalculator {

    fun bucketDepth(depthFeet: Double?): String? {
        val d = depthFeet ?: return null
        return when {
            d <= 5 -> "0–5 ft"
            d <= 10 -> "6–10 ft"
            d <= 20 -> "11–20 ft"
            d <= 30 -> "21–30 ft"
            else -> "31+ ft"
        }
    }

    fun bucketWaterTemp(tempF: Double?): String? {
        val t = tempF ?: return null
        return when {
            t < 50 -> "Under 50°F"
            t < 60 -> "50–59°F"
            t < 70 -> "60–69°F"
            t < 80 -> "70–79°F"
            else -> "80°F+"
        }
    }

    fun bucketHour(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..7 -> "Early Morning (4A-8A)"
            in 8..11 -> "Morning (8A-12P)"
            in 12..16 -> "Afternoon (12P-5P)"
            in 17..20 -> "Evening (5P-9P)"
            else -> "Night (9P-4A)"
        }
    }
    
    fun getSortOrderForTime(bucket: String): Int {
        return when {
            bucket.startsWith("Early") -> 0
            bucket.startsWith("Morning") -> 1
            bucket.startsWith("Afternoon") -> 2
            bucket.startsWith("Evening") -> 3
            else -> 4
        }
    }

    fun calculateCatchRate(catches: Int, noCatches: Int): Int {
        val total = catches + noCatches
        return if (total > 0) (catches.toFloat() / total * 100).toInt() else 0
    }

    fun normalizeWaterBody(name: String): String {
        return name.trim().lowercase(Locale.getDefault())
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
