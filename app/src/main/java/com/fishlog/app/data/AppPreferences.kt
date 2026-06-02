package com.fishlog.app.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fishlog_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APPEARANCE_MODE = "appearance_mode"
        private const val KEY_UNIT_SYSTEM = "unit_system"
        private const val KEY_HAS_SEEN_FIRST_RUN = "has_seen_first_run"

        const val MODE_FOLLOW_SYSTEM = "FOLLOW_SYSTEM"
        const val MODE_LIGHT = "LIGHT"
        const val MODE_DARK = "DARK"

        const val UNITS_US = "US"
        const val UNITS_METRIC = "METRIC"

        private const val KEY_MAP_CENTER_MODE = "map_center_mode"
        private const val KEY_MAP_LATITUDE = "map_latitude"
        private const val KEY_MAP_LONGITUDE = "map_longitude"
        private const val KEY_MAP_ZOOM = "map_zoom"
        private const val KEY_MAP_STYLE = "map_style"
        private const val KEY_CLOUD_BACKUP_MODE = "cloud_backup_mode"
        private const val KEY_CLOUD_BACKUP_PENDING = "cloud_backup_pending"
        private const val KEY_LAST_CLOUD_BACKUP_AT = "last_cloud_backup_at"
        private const val KEY_LAST_CLOUD_BACKUP_ATTEMPT_AT = "last_cloud_backup_attempt_at"
        private const val KEY_LAST_CLOUD_BACKUP_FAILED_AT = "last_cloud_backup_failed_at"
        private const val KEY_LAST_CLOUD_BACKUP_ERROR_MESSAGE = "last_cloud_backup_error_message"
        private const val KEY_LAST_LOCAL_DATA_CHANGED_AT = "last_local_data_changed_at"
        private const val KEY_LAST_AUTO_BACKUP_SCHEDULED_AT = "last_auto_backup_scheduled_at"
        private const val KEY_LAST_AUTO_BACKUP_STARTED_AT = "last_auto_backup_started_at"
        private const val KEY_LAST_AUTO_BACKUP_COMPLETED_AT = "last_auto_backup_completed_at"
        private const val KEY_AUTO_BACKUP_IN_PROGRESS = "auto_backup_in_progress"
        private const val KEY_AUTO_BACKUP_LAST_WORKER_MESSAGE = "auto_backup_last_worker_message"
        private const val KEY_REMINDER_ENABLED = "active_trip_reminder_enabled"
        private const val KEY_REMINDER_DELAY = "active_trip_reminder_delay_hours"
        private const val KEY_HOME_PHOTO_SLIDESHOW_ENABLED = "home_photo_slideshow_enabled"
        private const val KEY_CLOUD_BACKUP_FREQUENCY = "cloud_backup_frequency"
        private const val KEY_DEVELOPER_TOOLS_ENABLED = "developer_tools_enabled"

        const val MAP_CENTER_CURRENT = "CURRENT_LOCATION"
        const val MAP_CENTER_SAVED = "SAVED_LOCATION"

        const val MAP_STYLE_STANDARD = "STANDARD"
        const val MAP_STYLE_SATELLITE = "SATELLITE"
        const val MAP_STYLE_TOPOGRAPHIC = "TOPOGRAPHIC"

        const val CLOUD_BACKUP_MODE_MANUAL = "manual"
        const val CLOUD_BACKUP_MODE_AUTOMATIC = "automatic"

        const val CLOUD_BACKUP_FREQUENCY_HOURLY = "hourly"
        const val CLOUD_BACKUP_FREQUENCY_EVERY_6_HOURS = "every_6_hours"
        const val CLOUD_BACKUP_FREQUENCY_TWICE_DAILY = "twice_daily"
        const val CLOUD_BACKUP_FREQUENCY_DAILY = "daily"
    }

    enum class DefaultMapCenterMode {
        CURRENT_LOCATION,
        SAVED_LOCATION
    }

    fun getCloudBackupMode(): String {
        return prefs.getString(KEY_CLOUD_BACKUP_MODE, CLOUD_BACKUP_MODE_MANUAL) ?: CLOUD_BACKUP_MODE_MANUAL
    }

    fun setCloudBackupMode(mode: String) {
        prefs.edit().putString(KEY_CLOUD_BACKUP_MODE, mode).apply()
    }

    fun isAutomaticCloudBackupEnabled(): Boolean {
        return getCloudBackupMode() == CLOUD_BACKUP_MODE_AUTOMATIC
    }

    fun markCloudBackupPending() {
        prefs.edit().putBoolean(KEY_CLOUD_BACKUP_PENDING, true).apply()
    }

    fun clearCloudBackupPending() {
        prefs.edit().putBoolean(KEY_CLOUD_BACKUP_PENDING, false).apply()
    }

    fun getCloudBackupPending(): Boolean {
        return prefs.getBoolean(KEY_CLOUD_BACKUP_PENDING, false)
    }

    fun setLastCloudBackupSuccess(timestamp: Long) {
        prefs.edit()
            .putLong(KEY_LAST_CLOUD_BACKUP_AT, timestamp)
            .putLong(KEY_LAST_CLOUD_BACKUP_ATTEMPT_AT, timestamp)
            .remove(KEY_LAST_CLOUD_BACKUP_FAILED_AT)
            .remove(KEY_LAST_CLOUD_BACKUP_ERROR_MESSAGE)
            .apply()
    }

    fun setLastCloudBackupFailure(timestamp: Long, friendlyMessage: String) {
        prefs.edit()
            .putLong(KEY_LAST_CLOUD_BACKUP_ATTEMPT_AT, timestamp)
            .putLong(KEY_LAST_CLOUD_BACKUP_FAILED_AT, timestamp)
            .putString(KEY_LAST_CLOUD_BACKUP_ERROR_MESSAGE, friendlyMessage)
            .apply()
    }

    fun setLastCloudBackupAttempt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_CLOUD_BACKUP_ATTEMPT_AT, timestamp).apply()
    }

    fun getLastCloudBackupAt(): Long? {
        val at = prefs.getLong(KEY_LAST_CLOUD_BACKUP_AT, 0L)
        return if (at == 0L) null else at
    }

    fun getLastCloudBackupAttemptAt(): Long? {
        val at = prefs.getLong(KEY_LAST_CLOUD_BACKUP_ATTEMPT_AT, 0L)
        return if (at == 0L) null else at
    }

    fun getLastCloudBackupFailedAt(): Long? {
        val at = prefs.getLong(KEY_LAST_CLOUD_BACKUP_FAILED_AT, 0L)
        return if (at == 0L) null else at
    }

    fun getLastCloudBackupErrorMessage(): String? {
        return prefs.getString(KEY_LAST_CLOUD_BACKUP_ERROR_MESSAGE, null)
    }

    fun setLastLocalDataChangedAt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_LOCAL_DATA_CHANGED_AT, timestamp).apply()
    }

    fun getLastLocalDataChangedAt(): Long? {
        val at = prefs.getLong(KEY_LAST_LOCAL_DATA_CHANGED_AT, 0L)
        return if (at == 0L) null else at
    }

    fun setAutoBackupScheduled(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_AUTO_BACKUP_SCHEDULED_AT, timestamp).apply()
    }

    fun getAutoBackupScheduledAt(): Long? {
        val at = prefs.getLong(KEY_LAST_AUTO_BACKUP_SCHEDULED_AT, 0L)
        return if (at == 0L) null else at
    }

    fun setAutoBackupStarted(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_AUTO_BACKUP_STARTED_AT, timestamp).apply()
    }

    fun getAutoBackupStartedAt(): Long? {
        val at = prefs.getLong(KEY_LAST_AUTO_BACKUP_STARTED_AT, 0L)
        return if (at == 0L) null else at
    }

    fun setAutoBackupCompleted(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_AUTO_BACKUP_COMPLETED_AT, timestamp).apply()
    }

    fun getAutoBackupCompletedAt(): Long? {
        val at = prefs.getLong(KEY_LAST_AUTO_BACKUP_COMPLETED_AT, 0L)
        return if (at == 0L) null else at
    }

    fun setAutoBackupInProgress(inProgress: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_IN_PROGRESS, inProgress).apply()
    }

    fun getAutoBackupInProgress(): Boolean {
        return prefs.getBoolean(KEY_AUTO_BACKUP_IN_PROGRESS, false)
    }

    fun setAutoBackupWorkerMessage(message: String?) {
        prefs.edit().putString(KEY_AUTO_BACKUP_LAST_WORKER_MESSAGE, message).apply()
    }

    fun getAutoBackupWorkerMessage(): String? {
        return prefs.getString(KEY_AUTO_BACKUP_LAST_WORKER_MESSAGE, null)
    }

    fun isActiveTripReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, false)
    }

    fun setActiveTripReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
    }

    fun getActiveTripReminderDelayHours(): Int {
        return prefs.getInt(KEY_REMINDER_DELAY, 6)
    }

    fun setActiveTripReminderDelayHours(hours: Int) {
        prefs.edit().putInt(KEY_REMINDER_DELAY, hours).apply()
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

    fun hasSeenFirstRun(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_FIRST_RUN, false)
    }

    fun setHasSeenFirstRun(value: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_FIRST_RUN, value).apply()
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

    fun getMapStyle(): String {
        return prefs.getString(KEY_MAP_STYLE, MAP_STYLE_STANDARD) ?: MAP_STYLE_STANDARD
    }

    fun setMapStyle(style: String) {
        prefs.edit().putString(KEY_MAP_STYLE, style).apply()
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

    fun isHomePhotoSlideshowEnabled(): Boolean {
        return prefs.getBoolean(KEY_HOME_PHOTO_SLIDESHOW_ENABLED, true)
    }

    fun setHomePhotoSlideshowEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HOME_PHOTO_SLIDESHOW_ENABLED, enabled).apply()
    }

    fun getCloudBackupFrequency(): String {
        return prefs.getString(KEY_CLOUD_BACKUP_FREQUENCY, CLOUD_BACKUP_FREQUENCY_EVERY_6_HOURS) ?: CLOUD_BACKUP_FREQUENCY_EVERY_6_HOURS
    }

    fun setCloudBackupFrequency(frequency: String) {
        prefs.edit().putString(KEY_CLOUD_BACKUP_FREQUENCY, frequency).apply()
    }

    fun getCloudBackupFrequencyHours(): Int {
        return when (getCloudBackupFrequency()) {
            CLOUD_BACKUP_FREQUENCY_HOURLY -> 1
            CLOUD_BACKUP_FREQUENCY_EVERY_6_HOURS -> 6
            CLOUD_BACKUP_FREQUENCY_TWICE_DAILY -> 12
            CLOUD_BACKUP_FREQUENCY_DAILY -> 24
            else -> 6
        }
    }

    fun getCloudBackupFrequencyLabel(): String {
        return when (getCloudBackupFrequency()) {
            CLOUD_BACKUP_FREQUENCY_HOURLY -> "Every hour"
            CLOUD_BACKUP_FREQUENCY_EVERY_6_HOURS -> "Every 6 hours"
            CLOUD_BACKUP_FREQUENCY_TWICE_DAILY -> "Twice daily"
            CLOUD_BACKUP_FREQUENCY_DAILY -> "Daily"
            else -> "Every 6 hours"
        }
    }

    fun isDeveloperToolsEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEVELOPER_TOOLS_ENABLED, false)
    }

    fun setDeveloperToolsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVELOPER_TOOLS_ENABLED, enabled).apply()
    }
}
