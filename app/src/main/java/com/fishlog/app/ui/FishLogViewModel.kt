package com.fishlog.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fishlog.app.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FishLogViewModel(
    private val catchLogDao: CatchLogDao,
    private val fishingTripDao: FishingTripDao,
    private val cloudBackupRepository: CloudBackupRepository
) : ViewModel() {

    // Account & Backup States
    var accountStatus by mutableStateOf(if (cloudBackupRepository.isSignedIn()) AccountStatus.SIGNED_IN else AccountStatus.SIGNED_OUT)
        private set
    
    var accountEmail by mutableStateOf(cloudBackupRepository.getCurrentAccountEmail())
        private set

    var backupUiState by mutableStateOf(BackupUiState.IDLE)
        private set

    var backupStatusMessage by mutableStateOf<String?>(null)
        private set

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

    fun createAccount(email: String) {
        viewModelScope.launch {
            backupUiState = BackupUiState.BACKUP_IN_PROGRESS
            val result = cloudBackupRepository.createAccount(email)
            if (result.isSuccess) {
                accountStatus = AccountStatus.SIGNED_IN
                accountEmail = email
                backupUiState = BackupUiState.SUCCESS
                backupStatusMessage = "Account created successfully (Placeholder)"
            } else {
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = "Failed to create account"
            }
        }
    }

    fun signIn(email: String) {
        viewModelScope.launch {
            backupUiState = BackupUiState.BACKUP_IN_PROGRESS
            val result = cloudBackupRepository.signIn(email)
            if (result.isSuccess) {
                accountStatus = AccountStatus.SIGNED_IN
                accountEmail = email
                backupUiState = BackupUiState.SUCCESS
                backupStatusMessage = "Signed in successfully (Placeholder)"
            } else {
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = "Failed to sign in"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            cloudBackupRepository.signOut()
            accountStatus = AccountStatus.SIGNED_OUT
            accountEmail = null
            backupUiState = BackupUiState.IDLE
            backupStatusMessage = null
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            backupUiState = BackupUiState.BACKUP_IN_PROGRESS
            backupStatusMessage = "Backing up..."
            val result = cloudBackupRepository.backupNow()
            if (result.isSuccess) {
                backupUiState = BackupUiState.SUCCESS
                backupStatusMessage = "Backup completed (Placeholder)"
            } else {
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = "Backup failed"
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            backupUiState = BackupUiState.RESTORE_IN_PROGRESS
            backupStatusMessage = "Restoring..."
            val result = cloudBackupRepository.restoreFromCloud()
            if (result.isSuccess) {
                backupUiState = BackupUiState.SUCCESS
                backupStatusMessage = "Restore placeholder completed. Supabase integration coming next."
            } else {
                backupUiState = BackupUiState.ERROR
                backupStatusMessage = "Restore failed"
            }
        }
    }

    fun clearBackupMessage() {
        backupStatusMessage = null
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
        pressureTrend: String = ""
    ) {
        viewModelScope.launch {
            val trip = FishingTrip(
                name = name,
                waterBody = waterBody,
                startTime = System.currentTimeMillis(),
                notes = notes,
                latitude = latitude,
                longitude = longitude,
                skyCondition = skyCondition,
                windCondition = windCondition,
                airTempF = airTempF,
                waterClarity = waterClarity,
                pressureTrend = pressureTrend,
                updatedAt = System.currentTimeMillis(),
                backupStatus = BackupStatus.PENDING_BACKUP
            )
            fishingTripDao.insertTrip(trip)
        }
    }

    suspend fun endTrip(trip: FishingTrip) {
        fishingTripDao.updateTrip(trip.copy(
            endTime = System.currentTimeMillis(), 
            updatedAt = System.currentTimeMillis(),
            backupStatus = BackupStatus.PENDING_BACKUP
        ))
    }

    fun updateTrip(trip: FishingTrip) {
        viewModelScope.launch {
            fishingTripDao.updateTrip(trip.copy(
                updatedAt = System.currentTimeMillis(),
                backupStatus = BackupStatus.PENDING_BACKUP
            ))
        }
    }


    fun deleteTrip(trip: FishingTrip) {
        viewModelScope.launch {
            catchLogDao.clearTripIdForLogs(trip.id)
            fishingTripDao.deleteTrip(trip)
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
        }
    }

    fun updateCatch(catchLog: CatchLog) {
        viewModelScope.launch {
            catchLogDao.updateCatch(catchLog)
        }
    }

    fun deleteCatch(catchLog: CatchLog, photoStorageHelper: com.fishlog.app.data.PhotoStorageHelper? = null) {
        viewModelScope.launch {
            photoStorageHelper?.deletePhoto(catchLog.photoUri)
            catchLogDao.deleteCatch(catchLog)
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
        }
    }

    fun seedSampleData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = com.fishlog.app.data.SampleDataSeeder.seed(catchLogDao, fishingTripDao)
            onResult(result)
        }
    }

    fun removeSampleData(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = com.fishlog.app.data.SampleDataSeeder.clear(catchLogDao, fishingTripDao)
            onResult(result)
        }
    }
}

