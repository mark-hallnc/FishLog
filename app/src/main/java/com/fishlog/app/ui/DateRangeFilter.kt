package com.fishlog.app.ui

import java.text.SimpleDateFormat
import java.util.*

sealed class DateRangeFilter {
    object AllDates : DateRangeFilter()
    object Today : DateRangeFilter()
    object Last7Days : DateRangeFilter()
    object Last30Days : DateRangeFilter()
    object ThisYear : DateRangeFilter()
    data class Month(val month: Int, val year: Int) : DateRangeFilter()
    data class Season(val season: String, val year: Int? = null) : DateRangeFilter()
    data class Custom(val startDate: Long?, val endDate: Long?) : DateRangeFilter()

    companion object {
        const val SPRING = "Spring"
        const val SUMMER = "Summer"
        const val FALL = "Fall"
        const val WINTER = "Winter"
    }

    fun getLabel(): String {
        return when (this) {
            AllDates -> "All Dates"
            Today -> "Today"
            Last7Days -> "Last 7 Days"
            Last30Days -> "Last 30 Days"
            ThisYear -> "This Year"
            is Month -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, month)
                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                "$monthName $year"
            }
            is Season -> if (year != null) "$season $year" else season
            is Custom -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                when {
                    startDate != null && endDate != null -> "${sdf.format(Date(startDate))} - ${sdf.format(Date(endDate))}"
                    startDate != null -> "From ${sdf.format(Date(startDate))}"
                    endDate != null -> "Until ${sdf.format(Date(endDate))}"
                    else -> "Custom Range"
                }
            }
        }
    }

    fun matches(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        
        return when (this) {
            AllDates -> true
            Today -> isSameDay(timestamp, now)
            Last7Days -> timestamp >= getStartOfDay(now - 6L * 24 * 60 * 60 * 1000)
            Last30Days -> timestamp >= getStartOfDay(now - 29L * 24 * 60 * 60 * 1000)
            ThisYear -> isSameYear(timestamp, now)
            is Month -> {
                cal.timeInMillis = timestamp
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            }
            is Season -> {
                cal.timeInMillis = timestamp
                val m = cal.get(Calendar.MONTH)
                val y = cal.get(Calendar.YEAR)
                
                val seasonMatches = when (season) {
                    SPRING -> m in Calendar.MARCH..Calendar.MAY
                    SUMMER -> m in Calendar.JUNE..Calendar.AUGUST
                    FALL -> m in Calendar.SEPTEMBER..Calendar.NOVEMBER
                    WINTER -> m == Calendar.DECEMBER || m == Calendar.JANUARY || m == Calendar.FEBRUARY
                    else -> false
                }
                
                if (!seasonMatches) return false
                
                if (year != null) {
                    if (season == WINTER) {
                        // Winter spans across years. Dec is year-1, Jan/Feb are year.
                        // Wait, simple season/year mapping:
                        // Winter 2025-2026 means Dec 2025, Jan 2026, Feb 2026.
                        // If we pass year 2026 as the target year:
                        if (m == Calendar.DECEMBER) y == year - 1 else y == year
                    } else {
                        y == year
                    }
                } else true
            }
            is Custom -> {
                val start = startDate?.let { getStartOfDay(it) } ?: Long.MIN_VALUE
                val end = endDate?.let { getEndOfDay(it) } ?: Long.MAX_VALUE
                timestamp in start..end
            }
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameYear(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
