package com.fishlog.app.util

import java.text.DecimalFormat
import kotlin.math.roundToInt

data class PoundsOunces(
    val pounds: Int,
    val ounces: Int
)

object WeightUtils {
    private val poundsFormat = DecimalFormat("#")
    private val ouncesFormat = DecimalFormat("#")

    fun poundsOuncesToDecimalPounds(pounds: Int, ounces: Int): Double {
        return pounds.toDouble() + (ounces.toDouble() / 16.0)
    }

    fun decimalPoundsToPoundsOunces(decimalPounds: Double?): PoundsOunces? {
        if (decimalPounds == null) return null
        
        val totalOunces = (decimalPounds * 16.0).roundToInt()
        val pounds = totalOunces / 16
        val ounces = totalOunces % 16
        
        return PoundsOunces(pounds, ounces)
    }

    fun formatWeightPoundsOunces(decimalPounds: Double?): String {
        val po = decimalPoundsToPoundsOunces(decimalPounds) ?: return "—"
        
        return when {
            po.pounds > 0 && po.ounces > 0 -> "${po.pounds} lb ${po.ounces} oz"
            po.pounds > 0 -> "${po.pounds} lb"
            po.ounces > 0 -> "${po.ounces} oz"
            else -> "0 lb"
        }
    }
}
