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

        private const val KEY_MAP_CENTER_MODE = "map_center_mode"
        private const val KEY_MAP_LATITUDE = "map_latitude"
        private const val KEY_MAP_LONGITUDE = "map_longitude"
        private const val KEY_MAP_ZOOM = "map_zoom"

        const val MAP_CENTER_CURRENT = "CURRENT_LOCATION"
        const val MAP_CENTER_SAVED = "SAVED_LOCATION"
    }

    enum class DefaultMapCenterMode {
        CURRENT_LOCATION,
        SAVED_LOCATION
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

    fun getMapCenterMode(): String {
        return prefs.getString(KEY_MAP_CENTER_MODE, MAP_CENTER_CURRENT) ?: MAP_CENTER_CURRENT
    }

    fun setMapCenterMode(mode: String) {
        prefs.edit().putString(KEY_MAP_CENTER_MODE, mode).apply()
    }

    fun getMapLatitude(): Double? {
        val lat = prefs.getString(KEY_MAP_LATITUDE, null)
        return lat?.toDoubleOrNull()
    }

    fun getMapLongitude(): Double? {
        val lon = prefs.getString(KEY_MAP_LONGITUDE, null)
        return lon?.toDoubleOrNull()
    }

    fun getMapZoom(): Double {
        return prefs.getFloat(KEY_MAP_ZOOM, 13.0f).toDouble()
    }

    fun setSavedMapLocation(latitude: Double, longitude: Double, zoom: Double) {
        prefs.edit()
            .putString(KEY_MAP_LATITUDE, latitude.toString())
            .putString(KEY_MAP_LONGITUDE, longitude.toString())
            .putFloat(KEY_MAP_ZOOM, zoom.toFloat())
            .apply()
    }

    fun clearSavedMapLocation() {
        prefs.edit()
            .remove(KEY_MAP_LATITUDE)
            .remove(KEY_MAP_LONGITUDE)
            .remove(KEY_MAP_ZOOM)
            .apply()
    }
}
