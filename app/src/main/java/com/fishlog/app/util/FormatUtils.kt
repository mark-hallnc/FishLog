package com.fishlog.app.util

import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

object FormatUtils {
    private val wholeNumberFormat = DecimalFormat("#").apply {
        roundingMode = RoundingMode.HALF_UP
    }
    private val decimalFormat = DecimalFormat("#.##").apply {
        roundingMode = RoundingMode.HALF_UP
    }

    fun formatWholeNumber(value: Double?): String {
        return value?.let { wholeNumberFormat.format(it) } ?: "—"
    }

    fun formatDecimal(value: Double?): String {
        return value?.let { decimalFormat.format(it) } ?: "—"
    }

    fun formatLength(value: Double?, unit: String = "in"): String {
        if (value == null) return "—"
        val formatted = decimalFormat.format(value)
        return "$formatted $unit"
    }

    fun formatWeight(value: Double?, unit: String = "lb"): String {
        if (value == null) return "—"
        return if (unit.lowercase() == "lb" || unit.lowercase() == "lbs") {
            WeightUtils.formatWeightPoundsOunces(value)
        } else {
            val formatted = decimalFormat.format(value)
            "$formatted $unit"
        }
    }

    fun formatWaterTemp(value: Double?, unit: String = "°F"): String {
        if (value == null) return "—"
        val formatted = wholeNumberFormat.format(value)
        return "$formatted$unit"
    }

    fun formatDepth(value: Double?, unit: String = "ft"): String {
        if (value == null) return "—"
        val formatted = wholeNumberFormat.format(value)
        return "$formatted $unit"
    }
}
