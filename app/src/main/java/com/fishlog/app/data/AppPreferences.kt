package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fishlog_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APPEARANCE_MODE = "appearance_mode"
        private const val KEY_UNIT_SYSTEM = "unit_system"

        const val MODE_FOLLOW_SYSTEM = "FOLLOW_SYSTEM"
        const val MODE_LIGHT = "LIGHT"
        const val MODE_DARK = "DARK"

        const val UNITS_US = "US"
        const val UNITS_METRIC = "METRIC"
    }

    fun getAppearanceMode(): String {
        return prefs.getString(KEY_APPEARANCE_MODE, MODE_FOLLOW_SYSTEM) ?: MODE_FOLLOW_SYSTEM
    }

    fun setAppearanceMode(mode: String) {
        prefs.edit().putString(KEY_APPEARANCE_MODE, mode).apply()
    }

    fun getUnitSystem(): String {
        return prefs.getString(KEY_UNIT_SYSTEM, UNITS_US) ?: UNITS_US
    }

    fun setUnitSystem(system: String) {
        prefs.edit().putString(KEY_UNIT_SYSTEM, system).apply()
    }
}
