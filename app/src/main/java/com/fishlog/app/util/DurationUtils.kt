package com.fishlog.app.util

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

object DurationUtils {

    /**
     * Formats trip duration into a human-readable string.
     * Examples: "Just started", "42m", "2h 15m", "1d 3h 12m"
     */
    fun formatTripDuration(
        startTime: Long,
        endTime: Long? = null,
        now: Long = System.currentTimeMillis()
    ): String {
        val durationMs = (endTime ?: now) - startTime
        
        if (durationMs < 60_000L) {
            return "Just started"
        }

        val totalMinutes = durationMs / 60_000L
        val minutes = totalMinutes % 60
        val totalHours = totalMinutes / 60
        val hours = totalHours % 24
        val days = totalHours / 24

        return buildString {
            if (days > 0) {
                append("${days}d ")
            }
            if (hours > 0) {
                append("${hours}h ")
            }
            if (minutes > 0 || (days == 0L && hours == 0L)) {
                append("${minutes}m")
            }
        }.trim()
    }
}

/**
 * A helper composable that provides a timestamp that updates every minute.
 */
@Composable
fun rememberCurrentMinuteMillis(): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            val delayToNextMinute = 60_000L - (now % 60_000L)
            delay(delayToNextMinute.coerceAtLeast(1_000L))
        }
    }

    return now
}
