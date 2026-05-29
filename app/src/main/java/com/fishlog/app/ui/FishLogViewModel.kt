package com.fishlog.app.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fishlog.app.data.*
import com.fishlog.app.backup.AutoBackupScheduler
import com.fishlog.app.reminders.ActiveTripReminderScheduler
import com.fishlog.app.util.WaterBodyNameUtils
import androidx.work.WorkManager
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FishLogViewModel(
    private val applicationContext: android.content.Context,
    private val catchLogDao: CatchLogDao,
    private val fishingTripDao: FishingTripDao,
    private val cloudBackupRepository: CloudBackupRepository,
    private val weatherRepository: WeatherRepository,
    val appPreferences: AppPreferences,
    private val waterBodySuggestionRepository: WaterBodySuggestionRepository? = null
) : ViewModel() {

    // Account & Backup States
    var accountStatus by mutableStateOf(if (cloudBackupRepository.isSignedIn()) AccountStatus.SIGNED_IN else AccountStatus.SIGNED_OUT)
        private set
    
    var accountEmail by mutableStateOf(cloudBackupRepository.getCurrentAccountEmail())
        private set

    var cloudBackupMode by mutableStateOf(appPreferences.getCloudBackupMode())
        private set

    var cloudBackupPending by mutableStateOf(appPreferences.getCloudBackupPending())
        private set

    var backupUiState by mutableStateOf(BackupUiState.IDLE)
        private set

    var backupStatusMessage by mutableStateOf<String?>(null)
        private set

    var pendingAuthEmail by mutableStateOf<String?>(null)
        private set

    var lastCloudBackupAt by mutableStateOf(appPreferences.getLastCloudBackupAt())
        private set
    
    var lastCloudBackupErrorMessage by mutableStateOf(appPreferences.getLastCloudBackupErrorMessage())
        private set

    var cloudBackupStatus by mutableStateOf<CloudBackupStatus?>(null)
        private set

    // Nearby Water Bodies
    var nearbyWaterBodies by mutableStateOf<List<NearbyWaterBody>>(emptyList())
        private set
    var nearbyWaterBodiesLoading by mutableStateOf(false)
        private set
    private var nearbyWaterBodiesLoadedForSession = false

    // Active Trip Forecast State (In-memory only)
    var activeTripForecast by mutableStateOf<DailyForecastData?>(null)
        private set
    private var activeTripForecastForTripId: Long? = null
    private var activeTripForecastFetchedAt: Long? = null
    private var activeTripForecastLoading = false

    val allCatches: StateFlow<List<CatchLog>> = catchLogDao.getAllCatches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allTrips: StateFlow<List<FishingTrip>> = fishingTripDao.getAllTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeTrip: StateFlow<FishingTrip?> = fishingTripDao.getActiveTrip()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            cloudBackupRepository.refreshAccountState()
            refreshCloudBackupStatusFromPrefs()
            
            if (appPreferences.isAutomaticCloudBackupEnabled() && 
                appPreferences.getCloudBackupPending() && 
                cloudBackupRepository.isSignedIn()) {
                AutoBackupScheduler.scheduleAutoBackup(applicationContext)
            }

            // Active Trip Reminder initial check
            if (appPreferences.isActiveTripReminderEnabled()) {
                ActiveTripReminderScheduler.rescheduleIfActiveTripExists(applicationContext)
            }
        }
    }

    fun refreshCloudBackupStatusFromPrefs() {
        android.util.Log.d("FishLogCloud", "Refreshing backup status from prefs")
        
        val isSignedIn = cloudBackupRepository.isSignedIn()
        if (isSignedIn) {
            if (accountStatus != AccountStatus.SIGNED_IN) {
                Log.d("FishLogAuth", "Status changed to SIGNED_IN")
            }
            accountStatus = AccountStatus.SIGNED_IN
            accountEmail = cloudBackupRepository.getCurrentAccountEmail()
            pendingAuthEmail = null
        } else {
            // Preserve WAITING_FOR_CODE state during background refreshes
            if (accountStatus == AccountStatus.WAITING_FOR_CODE && pendingAuthEmail != null) {
                Log.d("FishLogAuth", "Preserving WAITING_FOR_CODE state for $pendingAuthEmail")
            } else {
                if (accountStatus != AccountStatus.SIGNED_OUT) {
                    Log.d("FishLogAuth", "Status changed to SIGNED_OUT")
                }
                accountStatus = AccountStatus.SIGNED_OUT
                accountEmail = cloudBackupRepository.getCurrentAccountEmail()
            }
        }

        lastCloudBackupAt = appPreferences.getLastCloudBackupAt()
        lastCloudBackupErrorMessage = appPreferences.getLastCloudBackupErrorMessage()
        cloudBackupMode = appPreferences.getCloudBackupMode()
        cloudBackupPending = appPreferences.getCloudBackupPending()
        refreshCloudBackupStatus()
    }

    fun refreshCloudBackupStatus() {
        cloudBackupStatus = calculateBackupStatus()
    }

    private fun calculateBackupStatus(): CloudBackupStatus {
        val user = if (SupabaseClientProvider.isConfigured()) {
            SupabaseClientProvider.client.auth.currentUserOrNull()
        } else null
        
        val isBackingUp = backupUiState == BackupUiState.BACKUP_IN_PROGRESS || appPreferences.getAutoBackupInProgress()
        
        return CloudBackupStatus(
            isSignedIn = user != null,
            email = user?.email ?: accountEmail,
            userId = user?.id,
            mode = cloudBackupMode,
            isAutomatic = appPreferences.isAutomaticCloudBackupEnabled(),
            isPending = cloudBackupPending,
            isBackingUp = isBackingUp,
            lastBackupAt = lastCloudBackupAt,
            lastAttemptAt = appPreferences.getLastCloudBackupAttemptAt(),
            lastFailedAt = appPreferences.getLastCloudBackupFailedAt(),
            lastErrorMessage = lastCloudBackupErrorMessage,
            photosIncluded = true,
            backupPath = user?.let { "${it.id}/fishlog-backup.json" },
            autoBackupWorkerMessage = appPreferences.getAutoBackupWorkerMessage()
        )
    }

    fun sendSignInCode(email: String) {
        viewModelScope.launch {
            Log.d("FishLogAuth", "Sending sign-in code to: $email")
            backupUiState = BackupUiState.AUTH_IN_PROGRESS
            val result = cloudBackupRepository.sendSignInCode(email)
            if (result.isSuccess) {
                Log.d("FishLogAuth", "Code sent successfully. Switching to WAITING_FOR_CODE")
                pendingAuthEmail = email
                accountStatus = AccountStatus.WAITING_FOR_CODE
                backupUiState = BackupUiState.WAITING_FOR_CODE
                backupStatusMessage = "Check your email for the sign-in code."
            } else {
                val error = result.exceptionOrNull()?.message ?: "Could not send code."
                Log.e("FishLogAuth", "Failed to send code: $error")
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = error
            }
        }
    }

    fun createAccount(email: String) = sendSignInCode(email)
    fun signIn(email: String) = sendSignInCode(email)

    fun verifyEmailCode(code: String) {
        val email = pendingAuthEmail ?: return
        viewModelScope.launch {
            Log.d("FishLogAuth", "Verifying email code for: $email")
            backupUiState = BackupUiState.AUTH_IN_PROGRESS
            val result = cloudBackupRepository.verifyEmailOtp(email, code)
            if (result.isSuccess) {
                Log.d("FishLogAuth", "Code verified successfully. Signed in.")
                accountStatus = AccountStatus.SIGNED_IN
                accountEmail = cloudBackupRepository.getCurrentAccountEmail()
                pendingAuthEmail = null
                backupUiState = BackupUiState.SUCCESS
                backupStatusMessage = "Code verified. You're signed in."
                
                refreshCloudBackupStatus()

                if (appPreferences.isAutomaticCloudBackupEnabled() && appPreferences.getCloudBackupPending()) {
                    AutoBackupScheduler.runAutoBackupSoon(applicationContext)
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Could not verify code. Try again."
                Log.e("FishLogAuth", "Verification failed: $error")
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = error
                // Explicitly keep status as WAITING_FOR_CODE so user can try again
                accountStatus = AccountStatus.WAITING_FOR_CODE
            }
        }
    }

    fun updateCloudBackupMode(mode: String) {
        appPreferences.setCloudBackupMode(mode)
        cloudBackupMode = mode
        refreshCloudBackupStatus()
        if (mode == AppPreferences.CLOUD_BACKUP_MODE_AUTOMATIC) {
            if (appPreferences.getCloudBackupPending() && cloudBackupRepository.isSignedIn()) {
                AutoBackupScheduler.runAutoBackupSoon(applicationContext)
            }
        } else {
            AutoBackupScheduler.cancelAutoBackup(applicationContext)
        }
    }

    fun markCloudBackupNeeded() {
        markCloudBackupNeeded("manual")
    }

    private fun markCloudBackupNeeded(reason: String) {
        Log.d("FishLogCloud", "Backup marked pending: $reason")
        appPreferences.markCloudBackupPending()
        cloudBackupPending = true
        appPreferences.setLastLocalDataChangedAt(System.currentTimeMillis())
        refreshCloudBackupStatus()
        if (appPreferences.isAutomaticCloudBackupEnabled() && cloudBackupRepository.isSignedIn()) {
            AutoBackupScheduler.scheduleAutoBackup(applicationContext)
        }
    }

    fun resendCode() {
        pendingAuthEmail?.let { 
            Log.d("FishLogAuth", "Resending code to: $it")
            signIn(it) 
        }
    }

    fun changeEmail() {
        Log.d("FishLogAuth", "Changing email. Resetting to SIGNED_OUT.")
        pendingAuthEmail = null
        accountStatus = AccountStatus.SIGNED_OUT
        backupUiState = BackupUiState.IDLE
        backupStatusMessage = null
    }

    fun signOut() {
        viewModelScope.launch {
            Log.d("FishLogAuth", "Signing out.")
            cloudBackupRepository.signOut()
            accountStatus = AccountStatus.SIGNED_OUT
            accountEmail = null
            pendingAuthEmail = null
            backupUiState = BackupUiState.IDLE
            backupStatusMessage = null
            refreshCloudBackupStatus()
            AutoBackupScheduler.cancelAutoBackup(applicationContext)
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            backupUiState = BackupUiState.BACKUP_IN_PROGRESS
            backupStatusMessage = "Backing up trips, logs, and photos..."
            
            try {
                val catches = allCatches.value
                val trips = allTrips.value
                val photoHelper = PhotoStorageHelper(applicationContext)
                
                val result = cloudBackupRepository.backupNow(catches, trips, photoHelper)
                
                if (result.isSuccess) {
                    val backupResult = result.getOrNull()!!
                    backupUiState = BackupUiState.SUCCESS
                    
                    val msg = if (backupResult.photosFailed > 0) {
                        "Backup complete. ${backupResult.photosFailed} photos could not be backed up."
                    } else {
                        "Cloud backup complete."
                    }
                    backupStatusMessage = msg
                    
                    val now = System.currentTimeMillis()
                    appPreferences.setLastCloudBackupSuccess(now)
                    appPreferences.clearCloudBackupPending()
                    lastCloudBackupAt = now
                    cloudBackupPending = false
                    lastCloudBackupErrorMessage = null
                    refreshCloudBackupStatus()
                } else {
                    backupUiState = BackupUiState.ERROR
                    val errorMsg = result.exceptionOrNull()?.message ?: "Backup failed"
                    backupStatusMessage = errorMsg
                    appPreferences.setLastCloudBackupFailure(System.currentTimeMillis(), errorMsg)
                    lastCloudBackupErrorMessage = errorMsg
                    refreshCloudBackupStatus()
                }
            } catch (e: Exception) {
                Log.e("FishLogCloud", "BackupNow failed with exception", e)
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = "Could not generate backup file."
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            backupUiState = BackupUiState.RESTORE_IN_PROGRESS
            backupStatusMessage = "Downloading from cloud..."
            
            val photoHelper = PhotoStorageHelper(applicationContext)
            val result = cloudBackupRepository.restoreFromCloud(photoHelper) { backup ->
                // This is called when JSON is parsed but before photos are restored
                importBackup(backup)
            }

            if (result.isSuccess) {
                val restoreResult = result.getOrNull()!!
                
                // Update local logs with the new photo URIs
                restoreResult.restoredPhotoUris.forEach { (uuid, newUri) ->
                    catchLogDao.updatePhotoUriByUuid(uuid, newUri)
                }

                backupUiState = BackupUiState.SUCCESS
                
                val msg = if (restoreResult.failedCount > 0) {
                    "Restore complete. ${restoreResult.failedCount} photos could not be restored."
                } else {
                    "Cloud restore complete. Data and photos merged."
                }
                backupStatusMessage = msg
                refreshCloudBackupStatus()
            } else {
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = result.exceptionOrNull()?.message ?: "Restore failed"
            }
        }
    }

    fun clearBackupMessage() {
        backupStatusMessage = null
    }

    fun testCloudBackupSetup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            backupUiState = BackupUiState.BACKUP_IN_PROGRESS
            backupStatusMessage = "Testing cloud connection..."
            
            val result = cloudBackupRepository.testCloudBackupSetup()
            if (result.isSuccess) {
                val diag = result.getOrNull()!!
                onResult(diag.message)
                backupUiState = BackupUiState.IDLE
                backupStatusMessage = null
            } else {
                val error = result.exceptionOrNull()?.message ?: "Test failed"
                onResult(error)
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = error
            }
        }
    }

    fun loadNearbyWaterBodiesForTripForm(
        locationService: com.fishlog.app.location.LocationService,
        forceRefresh: Boolean = false
    ) {
        val repo = waterBodySuggestionRepository ?: return
        
        if (nearbyWaterBodiesLoadedForSession && !forceRefresh) return
        
        // Initial load from cache
        nearbyWaterBodies = repo.getCachedNearbyWaterBodies()
        
        if (!locationService.hasLocationPermission()) return

        viewModelScope.launch {
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                // If cache is valid for this location, don't block for network unless forced
                if (repo.isCacheValid(loc.latitude, loc.longitude) && !forceRefresh) {
                    nearbyWaterBodiesLoadedForSession = true
                    return@launch
                }

                nearbyWaterBodiesLoading = true
                val result = repo.fetchNearbyWaterBodies(loc.latitude, loc.longitude)
                if (result.isSuccess) {
                    nearbyWaterBodies = result.getOrNull() ?: emptyList()
                    nearbyWaterBodiesLoadedForSession = true
                } else {
                    // Fail silently, keep cache
                    Log.d("FishLogWaterBodies", "Nearby fetch failed or timed out. Using cache if available.")
                }
                nearbyWaterBodiesLoading = false
            }
        }
    }

    fun resetNearbyWaterBodiesSession() {
        nearbyWaterBodiesLoadedForSession = false
    }

    fun loadActiveTripForecastIfNeeded(trip: FishingTrip) {
        val lat = trip.latitude ?: return
        val lon = trip.longitude ?: return
        if (trip.endTime != null) return // Only for active trips

        val now = System.currentTimeMillis()
        val oneHour = 60 * 60 * 1000L

        val alreadyLoadedRecently = activeTripForecastForTripId == trip.id &&
                activeTripForecast != null &&
                (activeTripForecastFetchedAt ?: 0L) > (now - oneHour)

        if (alreadyLoadedRecently || activeTripForecastLoading) return

        activeTripForecastLoading = true
        viewModelScope.launch {
            val result = weatherRepository.fetchTodayForecast(lat, lon)
            if (result.isSuccess) {
                activeTripForecast = result.getOrNull()
                activeTripForecastForTripId = trip.id
                activeTripForecastFetchedAt = System.currentTimeMillis()
            } else {
                // On failure, we just leave it null or clear it if it was for a different trip
                if (activeTripForecastForTripId != trip.id) {
                    activeTripForecast = null
                }
            }
            activeTripForecastLoading = false
        }
    }

    suspend fun fetchWeather(lat: Double, lon: Double): Result<WeatherData> {
        return weatherRepository.fetchWeatherForLocation(lat, lon)
    }

    fun mapWindSpeedToCondition(speedMph: Double?): String {
        return weatherRepository.mapWindSpeedToCondition(speedMph)
    }

    fun startTrip(
        name: String,
        waterBody: String,
        notes: String,
        latitude: Double?,
        longitude: Double?,
        skyCondition: String = "",
        windCondition: String = "",
        airTempF: Double? = null,
        waterClarity: String = "",
        pressureTrend: String = "",
        weatherData: WeatherData? = null
    ) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val moonData = MoonPhaseCalculator.calculate(startTime)

            val trip = FishingTrip(
                name = name,
                waterBody = waterBody,
                startTime = startTime,
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                skyCondition = skyCondition,
                windCondition = windCondition,
                airTempF = airTempF,
                waterClarity = waterClarity,
                pressureTrend = pressureTrend,
                updatedAt = System.currentTimeMillis(),
                backupStatus = BackupStatus.PENDING_BACKUP,
                
                // Moon phase auto-fill
                moonAutoFilled = true,
                moonPhaseName = moonData.phaseName,
                moonIlluminationPercent = moonData.illuminationPercent,
                moonAgeDays = moonData.ageDays,
                moonPhaseFraction = moonData.phaseFraction,
                moonWaxing = moonData.waxing,
                moonCalculatedAt = moonData.calculatedAt,

                // Weather auto-fill fields from repository data if available
                weatherAutoFilled = weatherData != null,
                weatherSource = weatherData?.source ?: "",
                weatherFetchedAt = weatherData?.fetchedAt,
                feelsLikeF = weatherData?.feelsLikeF,
                humidityPercent = weatherData?.humidityPercent,
                windSpeedMph = weatherData?.windSpeedMph,
                windDirectionDegrees = weatherData?.windDirectionDegrees,
                windGustMph = weatherData?.windGustMph,
                barometricPressureHpa = weatherData?.barometricPressureHpa,
                cloudCoverPercent = weatherData?.cloudCoverPercent,
                precipitationIn = weatherData?.precipitationIn,
                weatherCode = weatherData?.weatherCode,
                weatherSummary = weatherData?.weatherSummary ?: ""
            )
            val tripId = fishingTripDao.insertTrip(trip)
            markCloudBackupNeeded("start_trip")

            if (appPreferences.isActiveTripReminderEnabled()) {
                ActiveTripReminderScheduler.scheduleActiveTripReminder(
                    applicationContext, 
                    tripId, 
                    appPreferences.getActiveTripReminderDelayHours()
                )
            }
        }
    }

    suspend fun endTrip(trip: FishingTrip) {
        fishingTripDao.updateTrip(trip.copy(
            endTime = System.currentTimeMillis(), 
            updatedAt = System.currentTimeMillis(),
            backupStatus = BackupStatus.PENDING_BACKUP
        ))
        markCloudBackupNeeded("end_trip")
        ActiveTripReminderScheduler.cancelActiveTripReminder(applicationContext)
    }

    fun updateTrip(trip: FishingTrip, weatherData: WeatherData? = null) {
        viewModelScope.launch {
            var updatedTrip = trip.copy(
                updatedAt = System.currentTimeMillis(),
                backupStatus = BackupStatus.PENDING_BACKUP
            )

            // Apply weather data if provided (manually triggered auto-fill)
            if (weatherData != null) {
                updatedTrip = updatedTrip.copy(
                    weatherAutoFilled = true,
                    weatherSource = weatherData.source,
                    weatherFetchedAt = weatherData.fetchedAt,
                    feelsLikeF = weatherData.feelsLikeF,
                    humidityPercent = weatherData.humidityPercent,
                    windSpeedMph = weatherData.windSpeedMph,
                    windDirectionDegrees = weatherData.windDirectionDegrees,
                    windGustMph = weatherData.windGustMph,
                    barometricPressureHpa = weatherData.barometricPressureHpa,
                    cloudCoverPercent = weatherData.cloudCoverPercent,
                    precipitationIn = weatherData.precipitationIn,
                    weatherCode = weatherData.weatherCode,
                    weatherSummary = weatherData.weatherSummary
                )
            }

            // Auto-fill moon phase if missing
            if (!updatedTrip.moonAutoFilled || updatedTrip.moonPhaseName.isBlank()) {
                val moonData = MoonPhaseCalculator.calculate(updatedTrip.startTime)
                updatedTrip = updatedTrip.copy(
                    moonAutoFilled = true,
                    moonPhaseName = moonData.phaseName,
                    moonIlluminationPercent = moonData.illuminationPercent,
                    moonAgeDays = moonData.ageDays,
                    moonPhaseFraction = moonData.phaseFraction,
                    moonWaxing = moonData.waxing,
                    moonCalculatedAt = moonData.calculatedAt
                )
            }

            fishingTripDao.updateTrip(updatedTrip)
            markCloudBackupNeeded("update_trip")
        }
    }


    fun deleteTrip(trip: FishingTrip) {
        viewModelScope.launch {
            catchLogDao.clearTripIdForLogs(trip.id)
            fishingTripDao.deleteTrip(trip)
            markCloudBackupNeeded("delete_trip")
            
            // If it was the active trip, cancel the reminder
            if (trip.endTime == null) {
                ActiveTripReminderScheduler.cancelActiveTripReminder(applicationContext)
            }
        }
    }

    fun updateTripWaterBodyForMatchingOldName(oldName: String, newName: String) {
        viewModelScope.launch {
            val normOld = WaterBodyNameUtils.normalize(oldName)
            if (normOld.isBlank()) return@launch

            val trips = allTrips.value
            trips.filter { WaterBodyNameUtils.normalize(it.waterBody) == normOld }
                .forEach { trip ->
                    if (trip.waterBody != newName) {
                        fishingTripDao.updateTrip(trip.copy(
                            waterBody = newName,
                            updatedAt = System.currentTimeMillis(),
                            backupStatus = BackupStatus.PENDING_BACKUP
                        ))
                    }
                }
            markCloudBackupNeeded()
        }
    }

    fun saveCatch(
        species: String,
        length: String,
        weight: String,
        waterTemp: String,
        depth: String,
        bait: String,
        notes: String,
        latitude: Double? = null,
        longitude: Double? = null,
        lengthInches: Double? = null,
        weightLbs: Double? = null,
        waterTempF: Double? = null,
        depthFeet: Double? = null,
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val currentActiveTrip = activeTrip.value
            val catchLog = CatchLog(
                species = species,
                length = length,
                weight = weight,
                waterTemp = waterTemp,
                depth = depth,
                lengthInches = lengthInches,
                weightLbs = weightLbs,
                waterTempF = waterTempF,
                depthFeet = depthFeet,
                tripId = currentActiveTrip?.id,
                photoUri = photoUri,
                bait = bait,
                notes = notes,
                latitude = latitude,
                longitude = longitude
            )
            catchLogDao.insertCatch(catchLog)
            markCloudBackupNeeded("save_catch")
        }
    }

    fun saveCatchRun(
        species: String,
        length: String,
        weight: String,
        waterTemp: String,
        depth: String,
        bait: String,
        notes: String,
        latitude: Double? = null,
        longitude: Double? = null,
        lengthInches: Double? = null,
        weightLbs: Double? = null,
        waterTempF: Double? = null,
        depthFeet: Double? = null,
        photoUri: String? = null,
        quantity: Int
    ) {
        viewModelScope.launch {
            val currentActiveTrip = activeTrip.value
            val baseTime = System.currentTimeMillis()
            val logs = mutableListOf<CatchLog>()

            for (i in 0 until quantity) {
                val runNote = "Logged as part of a $quantity-fish run."
                val combinedNotes = if (notes.isBlank()) runNote else "$notes\n$runNote"
                
                logs.add(CatchLog(
                    species = species,
                    length = length,
                    weight = weight,
                    waterTemp = waterTemp,
                    depth = depth,
                    lengthInches = lengthInches,
                    weightLbs = weightLbs,
                    waterTempF = waterTempF,
                    depthFeet = depthFeet,
                    tripId = currentActiveTrip?.id,
                    // Attach photo only to the first catch in the run
                    photoUri = if (i == 0) photoUri else null,
                    bait = bait,
                    notes = combinedNotes,
                    latitude = latitude,
                    longitude = longitude,
                    // Offset timestamps by 1 second each
                    timestamp = baseTime + (i * 1000L)
                ))
            }
            catchLogDao.insertAll(logs)
            markCloudBackupNeeded("save_catch_run")
        }
    }

    fun saveNoCatch(
        waterTemp: String,
        depth: String,
        bait: String,
        notes: String,
        latitude: Double? = null,
        longitude: Double? = null,
        waterTempF: Double? = null,
        depthFeet: Double? = null
    ) {
        viewModelScope.launch {
            val currentActiveTrip = activeTrip.value
            val noCatchLog = CatchLog(
                logType = "NO_CATCH",
                species = "No Catch",
                length = "",
                weight = "",
                waterTemp = waterTemp,
                depth = depth,
                waterTempF = waterTempF,
                depthFeet = depthFeet,
                tripId = currentActiveTrip?.id,
                bait = bait,
                notes = notes,
                latitude = latitude,
                longitude = longitude
            )
            catchLogDao.insertCatch(noCatchLog)
            markCloudBackupNeeded("save_no_catch")
        }
    }

    fun updateCatch(catchLog: CatchLog) {
        viewModelScope.launch {
            catchLogDao.updateCatch(catchLog)
            markCloudBackupNeeded("update_catch")
        }
    }

    fun deleteCatch(catchLog: CatchLog, photoStorageHelper: com.fishlog.app.data.PhotoStorageHelper? = null) {
        viewModelScope.launch {
            photoStorageHelper?.deletePhoto(catchLog.photoUri)
            catchLogDao.deleteCatch(catchLog)
            markCloudBackupNeeded("delete_catch")
        }
    }

    fun updateActiveTripReminder(enabled: Boolean, delayHours: Int) {
        android.util.Log.d("FishLogReminder", "Updating prefs: enabled=$enabled, delay=$delayHours")
        appPreferences.setActiveTripReminderEnabled(enabled)
        appPreferences.setActiveTripReminderDelayHours(delayHours)
        
        if (enabled) {
            android.util.Log.d("FishLogReminder", "Rescheduling reminder")
            ActiveTripReminderScheduler.rescheduleIfActiveTripExists(applicationContext)
        } else {
            android.util.Log.d("FishLogReminder", "Canceling reminder")
            ActiveTripReminderScheduler.cancelActiveTripReminder(applicationContext)
        }
    }

    fun importBackup(backup: com.fishlog.app.data.FishLogBackup) {
        viewModelScope.launch {
            val currentTrips = allTrips.value
            val tripIdMap = mutableMapOf<Long, Long>()

            for (trip in backup.trips) {
                val existingTrip = currentTrips.find { 
                    it.startTime == trip.startTime && 
                    it.name == trip.name && 
                    it.waterBody == trip.waterBody 
                }
                
                if (existingTrip == null) {
                    val newTrip = trip.copy(
                        id = 0,
                        backupStatus = com.fishlog.app.data.BackupStatus.PENDING_BACKUP,
                        localUuid = trip.localUuid.ifBlank { java.util.UUID.randomUUID().toString() }
                    )
                    val newId = fishingTripDao.insertTrip(newTrip)
                    tripIdMap[trip.id] = newId
                } else {
                    tripIdMap[trip.id] = existingTrip.id
                }
            }

            val currentCatches = allCatches.value
            for (catch in backup.catchLogs) {
                val isDuplicate = currentCatches.any {
                    it.timestamp == catch.timestamp &&
                    it.species == catch.species &&
                    it.logType == catch.logType &&
                    it.latitude == catch.latitude &&
                    it.longitude == catch.longitude &&
                    it.bait == catch.bait
                }
                if (!isDuplicate) {
                    val newTripId = catch.tripId?.let { tripIdMap[it] }
                    val importedCatch = catch.copy(
                        id = 0, 
                        tripId = newTripId,
                        backupStatus = com.fishlog.app.data.BackupStatus.PENDING_BACKUP,
                        localUuid = catch.localUuid.ifBlank { java.util.UUID.randomUUID().toString() }
                    )
                    catchLogDao.insertCatch(importedCatch)
                }
            }
            markCloudBackupNeeded("import_backup")
        }
    }

    fun seedSampleData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = com.fishlog.app.data.SampleDataSeeder.seed(catchLogDao, fishingTripDao)
            onResult(result)
            markCloudBackupNeeded("seed_sample_data")
        }
    }

    fun removeSampleData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = com.fishlog.app.data.SampleDataSeeder.clear(catchLogDao, fishingTripDao)
            onResult(result)
            markCloudBackupNeeded("clear_sample_data")
        }
    }

    fun deleteAllTripsAndLogs(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val photoHelper = PhotoStorageHelper(applicationContext)
                // Fetch fresh list from DAO to ensure all files are identified
                val catches = catchLogDao.getAllCatches().first()
                
                // 1. Delete local photo files
                catches.forEach { catch ->
                    if (!catch.photoUri.isNullOrBlank()) {
                        try {
                            photoHelper.deletePhoto(catch.photoUri)
                        } catch (e: Exception) {
                            Log.w("FishLogDanger", "Failed to delete photo file: ${catch.photoUri}", e)
                        }
                    }
                }

                // 2. Delete logs
                catchLogDao.deleteAllLogs()

                // 3. Delete trips
                fishingTripDao.deleteAllTrips()

                // 4. Cancel active trip reminder
                ActiveTripReminderScheduler.cancelActiveTripReminder(applicationContext)

                // 5. Clear active trip forecast
                activeTripForecast = null
                activeTripForecastForTripId = null
                activeTripForecastFetchedAt = null

                // 6. Mark cloud backup pending
                markCloudBackupNeeded("clear_all_trips_and_logs")

                onResult("All trips and logs were deleted.")
            } catch (e: Exception) {
                Log.e("FishLogDanger", "Error deleting all data", e)
                onResult("Could not delete all trips and logs.")
            }
        }
    }
}

