package com.fishlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.fishlog.app.map.MapScreen
import com.fishlog.app.map.MapLocationPickerScreen
import com.fishlog.app.ui.CatchFormScreen
import com.fishlog.app.ui.NoCatchFormScreen
import com.fishlog.app.ui.CatchListScreen
import com.fishlog.app.ui.CatchDetailScreen
import com.fishlog.app.ui.InsightsScreen
import com.fishlog.app.ui.StartTripScreen
import com.fishlog.app.ui.TripDetailScreen
import com.fishlog.app.ui.TripHistoryScreen
import com.fishlog.app.ui.TripSummaryScreen
import com.fishlog.app.ui.EditTripScreen
import com.fishlog.app.ui.SettingsScreen
import com.fishlog.app.ui.AdvancedAnalyticsScreen
import com.fishlog.app.ui.AdvancedReportsScreen
import com.fishlog.app.ui.TripReviewScreen
import com.fishlog.app.ui.FirstRunScreen
import com.fishlog.app.ui.PatternDetailScreen
import com.fishlog.app.ui.PhotoViewerScreen
import com.fishlog.app.ui.theme.FishLogTheme
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.PhotoStorageHelper
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.data.AccountStatus
import com.fishlog.app.data.BackupUiState
import com.fishlog.app.analytics.PatternInsight
import com.fishlog.app.analytics.PatternEngineFilters
import com.fishlog.app.analytics.PatternType
import com.fishlog.app.analytics.PatternEngine
import com.fishlog.app.ui.DateRangeFilter
import com.fishlog.app.ui.LogTypeFilter
import com.fishlog.app.ui.MapReturnState
import com.fishlog.app.billing.PaidFeature
import com.fishlog.app.billing.FeatureGate

import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fishlog.app.data.FishLogDatabase
import com.fishlog.app.data.CloudBackupRepository
import com.fishlog.app.data.WeatherRepository
import com.fishlog.app.data.WaterBodySuggestionRepository
import com.fishlog.app.ui.FishLogViewModel
import com.fishlog.app.util.DurationUtils
import com.fishlog.app.util.rememberCurrentMinuteMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavOrigin {
    HOME,
    HISTORY,
    TRIP_HISTORY,
    MAP,
    DETAIL,
    TRIP_DETAIL,
    ADVANCED_ANALYTICS,
    PATTERN_DETAIL,
    SETTINGS
}

/**
 * Main entry point for FishLog.
 * 
 * Run unit tests:
 * ./gradlew testDebugUnitTest
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = FishLogDatabase.getDatabase(applicationContext)
        val catchDao = database.catchLogDao()
        val tripDao = database.fishingTripDao()
        
        enableEdgeToEdge()
        val cloudBackupRepository = CloudBackupRepository(applicationContext)
        val weatherRepository = WeatherRepository()
        val waterBodySuggestionRepository = WaterBodySuggestionRepository(applicationContext)
        val appPreferences = AppPreferences(applicationContext)
        
        setContent {
            val viewModel: FishLogViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FishLogViewModel(
                            applicationContext,
                            catchDao, 
                            tripDao, 
                            cloudBackupRepository, 
                            weatherRepository,
                            appPreferences,
                            waterBodySuggestionRepository
                        ) as T
                    }
                }
            )

            var appearanceMode by remember { mutableStateOf(appPreferences.getAppearanceMode()) }
            var unitSystem by remember { mutableStateOf(appPreferences.getUnitSystem()) }
            
            var mapCenterMode by remember { mutableStateOf(appPreferences.getMapCenterMode()) }
            var mapDefaultLat by remember { mutableStateOf(appPreferences.getMapLatitude()) }
            var mapDefaultLon by remember { mutableStateOf(appPreferences.getMapLongitude()) }
            var mapDefaultZoom by remember { mutableStateOf(appPreferences.getMapZoom()) }
            var mapStyle by remember { mutableStateOf(appPreferences.getMapStyle()) }
            var homePhotoSlideshowEnabled by remember { mutableStateOf(appPreferences.isHomePhotoSlideshowEnabled()) }

            var activeTripReminderEnabled by remember { mutableStateOf(appPreferences.isActiveTripReminderEnabled()) }
            var activeTripReminderDelay by remember { mutableStateOf(appPreferences.getActiveTripReminderDelayHours()) }

            val darkTheme = when (appearanceMode) {
                AppPreferences.MODE_LIGHT -> false
                AppPreferences.MODE_DARK -> true
                else -> isSystemInDarkTheme()
            }

            FishLogTheme(darkTheme = darkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    appearanceMode = appearanceMode,
                    unitSystem = unitSystem,
                    mapCenterMode = mapCenterMode,
                    mapDefaultLat = mapDefaultLat,
                    mapDefaultLon = mapDefaultLon,
                    mapDefaultZoom = mapDefaultZoom,
                    mapStyle = mapStyle,
                    homePhotoSlideshowEnabled = homePhotoSlideshowEnabled,
                    activeTripReminderEnabled = activeTripReminderEnabled,
                    activeTripReminderDelay = activeTripReminderDelay,
                    onAppearanceModeChange = { mode ->
                        appearanceMode = mode
                        appPreferences.setAppearanceMode(mode)
                    },
                    onUnitSystemChange = { system ->
                        unitSystem = system
                        appPreferences.setUnitSystem(system)
                    },
                    onMapCenterModeChange = { mode ->
                        mapCenterMode = mode
                        appPreferences.setMapCenterMode(mode)
                    },
                    onSetDefaultMapLocation = { lat, lon, zoom ->
                        mapDefaultLat = lat
                        mapDefaultLon = lon
                        mapDefaultZoom = zoom
                        appPreferences.setSavedMapLocation(lat, lon, zoom)
                    },
                    onClearDefaultMapLocation = {
                        mapDefaultLat = null
                        mapDefaultLon = null
                        appPreferences.clearSavedMapLocation()
                    },
                    onMapStyleChange = { style ->
                        mapStyle = style
                        appPreferences.setMapStyle(style)
                    },
                    onHomePhotoSlideshowEnabledChange = { enabled ->
                        homePhotoSlideshowEnabled = enabled
                        appPreferences.setHomePhotoSlideshowEnabled(enabled)
                    },
                    onActiveTripReminderChange = { enabled, delay ->
                        android.util.Log.d("FishLogReminder", "Setting changed: enabled=$enabled, delay=$delay")
                        activeTripReminderEnabled = enabled
                        activeTripReminderDelay = delay
                        viewModel.updateActiveTripReminder(enabled, delay)
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: FishLogViewModel,
    appearanceMode: String,
    unitSystem: String,
    mapCenterMode: String,
    mapDefaultLat: Double?,
    mapDefaultLon: Double?,
    mapDefaultZoom: Double,
    mapStyle: String,
    homePhotoSlideshowEnabled: Boolean,
    activeTripReminderEnabled: Boolean,
    activeTripReminderDelay: Int,
    onAppearanceModeChange: (String) -> Unit,
    onUnitSystemChange: (String) -> Unit,
    onMapCenterModeChange: (String) -> Unit,
    onSetDefaultMapLocation: (Double, Double, Double) -> Unit,
    onClearDefaultMapLocation: () -> Unit,
    onMapStyleChange: (String) -> Unit,
    onHomePhotoSlideshowEnabledChange: (Boolean) -> Unit,
    onActiveTripReminderChange: (Boolean, Int) -> Unit
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val photoStorageHelper = remember { PhotoStorageHelper(context) }
    var currentScreen by remember { 
        mutableStateOf(if (appPreferences.hasSeenFirstRun()) "Home" else "FirstRun") 
    }
    var isFirstRunViewAgain by remember { mutableStateOf(false) }
    var previousScreen by remember { mutableStateOf("Home") }
    var previousTripScreen by remember { mutableStateOf("Home") }
    
    var detailOrigin by remember { mutableStateOf(NavOrigin.HOME) }
    var tripDetailOrigin by remember { mutableStateOf(NavOrigin.HOME) }
    var mapOrigin by remember { mutableStateOf(NavOrigin.HOME) }

    var selectedCatch by remember { mutableStateOf<CatchLog?>(null) }
    var focusedLogOnMap by remember { mutableStateOf<CatchLog?>(null) }
    var selectedTrip by remember { mutableStateOf<FishingTrip?>(null) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var savedMapReturnState by remember { mutableStateOf<MapReturnState?>(null) }
    var selectedPatternInsight by remember { mutableStateOf<PatternInsight?>(null) }
    var selectedReportFilters by remember { mutableStateOf(PatternEngineFilters()) }

    // Map filters state to persist across detail views
    var mapSelectedSpecies by remember { mutableStateOf("All Species") }
    var mapSelectedBait by remember { mutableStateOf("All Baits") }
    var mapSelectedTripId by remember { mutableStateOf<Long?>(null) }
    var mapSelectedWaterBody by remember { mutableStateOf("All Water Bodies") }
    var mapDateFilter by remember { mutableStateOf<DateRangeFilter>(DateRangeFilter.AllDates) }
    var mapLogTypeFilter by remember { mutableStateOf(LogTypeFilter.ALL) }
    var mapShowFilters by remember { mutableStateOf(false) }
    var mapSelectedLogForOverlay by remember { mutableStateOf<CatchLog?>(null) }

    val scope = rememberCoroutineScope()

    // PART 1 — Stop unnecessary backup polling
    // Only poll while backup is pending or in progress
    LaunchedEffect(viewModel.cloudBackupPending, viewModel.backupUiState) {
        val isPending = viewModel.cloudBackupPending
        val isInProgress = viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS
        
        if (isPending || isInProgress) {
            android.util.Log.d("FishLogCloud", "Polling STARTED (pending=$isPending, progress=$isInProgress)")
            while (viewModel.cloudBackupPending || viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS) {
                viewModel.refreshCloudBackupStatusFromPrefs()
                delay(5000)
            }
            android.util.Log.d("FishLogCloud", "Polling STOPPED")
        }
    }

    // Refresh when entering Home or Settings
    LaunchedEffect(currentScreen) {
        if (currentScreen == "Home" || currentScreen == "Settings") {
            android.util.Log.d("FishLogCloud", "Refreshing backup status on screen entry: $currentScreen")
            viewModel.refreshCloudBackupStatusFromPrefs()
        }
    }

    fun handleBack() {
        android.util.Log.d("FishLogNav", "Back pressed on screen: $currentScreen")
        when (currentScreen) {
            "History", "Map", "Insights", "AdvancedAnalytics", "TripHistory", "Settings", "StartTrip" -> {
                if (currentScreen == "Map" && mapOrigin == NavOrigin.DETAIL) {
                    currentScreen = "Detail"
                    mapOrigin = NavOrigin.HOME
                } else if (currentScreen == "Map" && mapOrigin == NavOrigin.TRIP_DETAIL) {
                    currentScreen = "TripDetail"
                    mapOrigin = NavOrigin.HOME
                } else {
                    currentScreen = "Home"
                }
            }
            "Detail" -> {
                currentScreen = when(detailOrigin) {
                    NavOrigin.HISTORY -> "History"
                    NavOrigin.TRIP_DETAIL -> "TripDetail"
                    NavOrigin.MAP -> "Map"
                    NavOrigin.PATTERN_DETAIL -> "PatternDetail"
                    else -> "Home"
                }
            }
            "TripDetail" -> {
                currentScreen = when(tripDetailOrigin) {
                    NavOrigin.TRIP_HISTORY -> "TripHistory"
                    NavOrigin.MAP -> "Map"
                    NavOrigin.PATTERN_DETAIL -> "PatternDetail"
                    else -> "Home"
                }
            }
            "PhotoViewer" -> currentScreen = "Detail"
            "PatternDetail" -> currentScreen = "AdvancedAnalytics"
            "AdvancedReports", "TripReview" -> currentScreen = "AdvancedAnalytics"
            "Form" -> currentScreen = if (selectedCatch != null) "Detail" else "Home"
            "NoCatchForm" -> currentScreen = if (selectedCatch != null) "Detail" else "Home"
            "EditTrip" -> currentScreen = "TripDetail"
            "TripSummary" -> currentScreen = "Home"
            "MapLocationPicker" -> currentScreen = "Settings"
            else -> { /* Home or FirstRun, let system handle */ }
        }
    }

    BackHandler(enabled = currentScreen != "Home" && currentScreen != "FirstRun") {
        handleBack()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentScreen) {
            "FirstRun" -> FirstRunScreen(
                isViewAgain = isFirstRunViewAgain,
                modifier = modifier,
                onGetStarted = {
                    if (!isFirstRunViewAgain) {
                        appPreferences.setHasSeenFirstRun(true)
                        currentScreen = "Home"
                    } else {
                        isFirstRunViewAgain = false
                        currentScreen = "Settings"
                    }
                }
            )
            "Home" -> HomeScreen(
                fishLogViewModel = viewModel,
                homePhotoSlideshowEnabled = homePhotoSlideshowEnabled,
                onLogCatchClick = { 
                    selectedCatch = null
                    currentScreen = "Form" 
                },
                onLogNoCatchClick = {
                    selectedCatch = null
                    currentScreen = "NoCatchForm"
                },
                onHistoryClick = { 
                    detailOrigin = NavOrigin.HOME
                    currentScreen = "History" 
                },
                onMapClick = { 
                    mapOrigin = NavOrigin.HOME
                    focusedLogOnMap = null
                    savedMapReturnState = null
                    mapSelectedLogForOverlay = null
                    currentScreen = "Map" 
                },
                onInsightsClick = { currentScreen = "Insights" },
                onAdvancedAnalyticsClick = { currentScreen = "AdvancedAnalytics" },
                onTripHistoryClick = { 
                    tripDetailOrigin = NavOrigin.HOME
                    currentScreen = "TripHistory" 
                },
                onSettingsClick = { currentScreen = "Settings" },
                onStartTripClick = { currentScreen = "StartTrip" },
                onViewTripClick = { trip ->
                    selectedTrip = trip
                    tripDetailOrigin = NavOrigin.HOME
                    currentScreen = "TripDetail"
                },
                onEndTripClick = { trip ->
                    selectedTrip = trip.copy(endTime = System.currentTimeMillis())
                    currentScreen = "TripSummary"
                },
                onBackupStatusClick = { currentScreen = "Settings" },
                modifier = modifier
            )
            "StartTrip" -> StartTripScreen(
                viewModel = viewModel,
                onBack = { handleBack() },
                onTripStarted = { currentScreen = "Home" }
            )
            "TripDetail" -> selectedTrip?.let { trip ->
                TripDetailScreen(
                    trip = trip,
                    viewModel = viewModel,
                    unitSystem = unitSystem,
                    onBack = { handleBack() },
                    onLogClick = { catch ->
                        selectedCatch = catch
                        detailOrigin = NavOrigin.TRIP_DETAIL
                        currentScreen = "Detail"
                    },
                    onLogCatch = {
                        selectedCatch = null
                        currentScreen = "Form"
                    },
                    onLogNoCatch = {
                        selectedCatch = null
                        currentScreen = "NoCatchForm"
                    },
                    onTripEnded = { endedTrip ->
                        selectedTrip = endedTrip
                        currentScreen = "TripSummary"
                    },
                    onEditTrip = { trip ->
                        selectedTrip = trip
                        currentScreen = "EditTrip"
                    },
                    onTripDeleted = {
                        currentScreen = "Home"
                    }
                )
            } ?: run { currentScreen = "Home" }
            "EditTrip" -> selectedTrip?.let { trip ->
                EditTripScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onBack = { handleBack() },
                    onSave = { updatedTrip ->
                        selectedTrip = updatedTrip
                        currentScreen = "TripDetail"
                    }
                )
            } ?: run { currentScreen = "Home" }
            "Form" -> CatchFormScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { handleBack() },
                editingCatch = selectedCatch
            )
            "NoCatchForm" -> NoCatchFormScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { handleBack() },
                editingLog = selectedCatch
            )
            "History" -> CatchListScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { handleBack() },
                onCatchClick = { catch ->
                    selectedCatch = catch
                    detailOrigin = NavOrigin.HISTORY
                    currentScreen = "Detail"
                },
                onPhotoClick = { uri ->
                    selectedPhotoUri = uri
                    detailOrigin = NavOrigin.HISTORY
                    currentScreen = "PhotoViewer"
                }
            )
            "Detail" -> selectedCatch?.let { catch ->
                CatchDetailScreen(
                    catch = catch,
                    unitSystem = unitSystem,
                    mapStyle = mapStyle,
                    onBack = { handleBack() },
                    onEdit = { 
                        currentScreen = if (catch.logType == "NO_CATCH") "NoCatchForm" else "Form"
                    },
                    onDelete = {
                        viewModel.deleteCatch(catch, photoStorageHelper)
                        handleBack()
                    },
                    onViewOnMap = { log ->
                        selectedCatch = log
                        focusedLogOnMap = log
                        mapOrigin = NavOrigin.DETAIL
                        savedMapReturnState = null
                        mapSelectedLogForOverlay = log
                        currentScreen = "Map"
                    },
                    onPhotoClick = { uri ->
                        selectedPhotoUri = uri
                        currentScreen = "PhotoViewer"
                    }
                )
            } ?: run { currentScreen = "Home" }
            "PhotoViewer" -> selectedPhotoUri?.let { uri ->
                PhotoViewerScreen(
                    photoUri = uri,
                    onBack = { handleBack() }
                )
            } ?: run { currentScreen = "Home" }
            "Map" -> MapScreen(
                viewModel = viewModel,
                onBack = { handleBack() },
                onLogClick = { catch, returnState ->
                    selectedCatch = catch
                    savedMapReturnState = returnState
                    detailOrigin = NavOrigin.MAP
                    currentScreen = "Detail"
                },
                onTripClick = { trip, returnState ->
                    selectedTrip = trip
                    savedMapReturnState = returnState
                    tripDetailOrigin = NavOrigin.MAP
                    currentScreen = "TripDetail"
                },
                focusLog = focusedLogOnMap,
                initialReturnState = savedMapReturnState,
                selectedSpecies = mapSelectedSpecies,
                onSpeciesChange = { mapSelectedSpecies = it },
                selectedBait = mapSelectedBait,
                onBaitChange = { mapSelectedBait = it },
                selectedTripId = mapSelectedTripId,
                onTripIdChange = { mapSelectedTripId = it },
                selectedWaterBody = mapSelectedWaterBody,
                onWaterBodyChange = { mapSelectedWaterBody = it },
                dateFilter = mapDateFilter,
                onDateFilterChange = { mapDateFilter = it },
                logTypeFilter = mapLogTypeFilter,
                onLogTypeFilterChange = { mapLogTypeFilter = it },
                showFilters = mapShowFilters,
                onShowFiltersChange = { mapShowFilters = it },
                selectedLogForOverlay = mapSelectedLogForOverlay,
                onLogForOverlayChange = { mapSelectedLogForOverlay = it },
                mapCenterMode = mapCenterMode,
                mapDefaultLat = mapDefaultLat,
                mapDefaultLon = mapDefaultLon,
                mapDefaultZoom = mapDefaultZoom,
                mapStyle = mapStyle,
                onMapStyleChange = onMapStyleChange
            )
            "MapLocationPicker" -> MapLocationPickerScreen(
                initialLat = mapDefaultLat,
                initialLon = mapDefaultLon,
                initialZoom = mapDefaultZoom,
                mapStyle = mapStyle,
                onMapStyleChange = onMapStyleChange,
                viewModel = viewModel,
                onLocationPicked = { lat, lon, zoom ->
                    onSetDefaultMapLocation(lat, lon, zoom)
                    currentScreen = "Settings"
                },
                onBack = { handleBack() }
            )
            "Insights" -> InsightsScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { handleBack() }
            )
            "AdvancedAnalytics" -> AdvancedAnalyticsScreen(
                viewModel = viewModel,
                onBack = { handleBack() },
                onInsightClick = { insight ->
                    selectedPatternInsight = insight
                    currentScreen = "PatternDetail"
                },
                onViewReports = { filters ->
                    selectedReportFilters = filters
                    currentScreen = "AdvancedReports"
                },
                onViewTripReview = {
                    currentScreen = "TripReview"
                }
            )
            "AdvancedReports" -> {
                val catches by viewModel.allCatches.collectAsState()
                val trips by viewModel.allTrips.collectAsState()
                AdvancedReportsScreen(
                    logs = catches,
                    trips = trips,
                    filters = selectedReportFilters,
                    onBack = { handleBack() },
                    onBucketClick = { bucket, category ->
                        selectedPatternInsight = PatternInsight(
                            title = bucket.label,
                            subtitle = category,
                            category = category,
                            catchCount = bucket.catchCount,
                            noCatchCount = bucket.noCatchCount,
                            observationCount = bucket.observationCount,
                            catchRate = bucket.catchRate,
                            confidenceLabel = PatternEngine.calculateConfidence(bucket.observationCount),
                            matchingLogIds = bucket.matchingLogIds,
                            patternType = PatternType.TOP_PATTERN
                        )
                        currentScreen = "PatternDetail"
                    }
                )
            }
            "TripReview" -> {
                val catches by viewModel.allCatches.collectAsState()
                val trips by viewModel.allTrips.collectAsState()
                TripReviewScreen(
                    trips = trips,
                    allLogs = catches,
                    onBack = { handleBack() }
                )
            }
            "PatternDetail" -> selectedPatternInsight?.let { insight ->
                val catches by viewModel.allCatches.collectAsState()
                val trips by viewModel.allTrips.collectAsState()
                PatternDetailScreen(
                    insight = insight,
                    allLogs = catches,
                    allTrips = trips,
                    onBack = { handleBack() },
                    onLogClick = { log ->
                        selectedCatch = log
                        detailOrigin = NavOrigin.PATTERN_DETAIL
                        currentScreen = "Detail"
                    },
                    onTripClick = { trip ->
                        selectedTrip = trip
                        tripDetailOrigin = NavOrigin.PATTERN_DETAIL
                        currentScreen = "TripDetail"
                    }
                )
            } ?: run { currentScreen = "AdvancedAnalytics" }
            "TripHistory" -> TripHistoryScreen(
                viewModel = viewModel,
                onBack = { handleBack() },
                onTripClick = { trip ->
                    selectedTrip = trip
                    tripDetailOrigin = NavOrigin.TRIP_HISTORY
                    currentScreen = "TripDetail"
                },
                onStartTripClick = { currentScreen = "StartTrip" }
            )
            "TripSummary" -> selectedTrip?.let { trip ->
                TripSummaryScreen(
                    trip = trip,
                    viewModel = viewModel,
                    unitSystem = unitSystem,
                    onDone = { currentScreen = "Home" },
                    onViewDetails = { 
                        tripDetailOrigin = NavOrigin.HOME
                        currentScreen = "TripDetail" 
                    }
                )
            } ?: run { currentScreen = "Home" }
            "Settings" -> SettingsScreen(
                viewModel = viewModel,
                appearanceMode = appearanceMode,
                unitSystem = unitSystem,
                mapCenterMode = mapCenterMode,
                mapDefaultLat = mapDefaultLat,
                mapDefaultLon = mapDefaultLon,
                mapStyle = mapStyle,
                homePhotoSlideshowEnabled = homePhotoSlideshowEnabled,
                activeTripReminderEnabled = activeTripReminderEnabled,
                activeTripReminderDelay = activeTripReminderDelay,
                onAppearanceModeChange = onAppearanceModeChange,
                onUnitSystemChange = onUnitSystemChange,
                onMapCenterModeChange = onMapCenterModeChange,
                onClearDefaultMapLocation = onClearDefaultMapLocation,
                onChooseDefaultMapLocation = { currentScreen = "MapLocationPicker" },
                onViewWelcomeGuide = {
                    isFirstRunViewAgain = true
                    currentScreen = "FirstRun"
                },
                onResetWelcomeScreen = {
                    appPreferences.setHasSeenFirstRun(false)
                },
                onMapStyleChange = onMapStyleChange,
                onHomePhotoSlideshowEnabledChange = onHomePhotoSlideshowEnabledChange,
                onActiveTripReminderChange = onActiveTripReminderChange,
                onBack = { handleBack() }
            )
        }
    }
}

@Composable
fun HomeScreen(
    fishLogViewModel: FishLogViewModel,
    homePhotoSlideshowEnabled: Boolean = true,
    onLogCatchClick: () -> Unit,
    onLogNoCatchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMapClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onAdvancedAnalyticsClick: () -> Unit,
    onTripHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStartTripClick: () -> Unit,
    onViewTripClick: (FishingTrip) -> Unit,
    onEndTripClick: (FishingTrip) -> Unit,
    onBackupStatusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SideEffect {
        android.util.Log.d("FishLogPerf", "HomeScreen composed (slideshow=$homePhotoSlideshowEnabled)")
    }
    val activeTrip by fishLogViewModel.activeTrip.collectAsState()
    val catches by fishLogViewModel.allCatches.collectAsState()
    val trips by fishLogViewModel.allTrips.collectAsState()

    val catchPhotos = remember(catches, homePhotoSlideshowEnabled) {
        if (homePhotoSlideshowEnabled && catches.isNotEmpty()) {
            val photos = catches.filter { it.logType == "CATCH" && !it.photoUri.isNullOrBlank() }.map { it.photoUri!! }
            android.util.Log.d("FishLogPerf", "HomeScreen: Photo count = ${photos.size} (Slideshow Enabled)")
            photos
        } else {
            if (!homePhotoSlideshowEnabled) {
                android.util.Log.d("FishLogPerf", "HomeScreen: Slideshow Disabled")
            }
            emptyList()
        }
    }

    var currentPhotoIndex by remember { mutableIntStateOf(0) }

    if (catchPhotos.size > 1) {
        LaunchedEffect(catchPhotos) {
            while (true) {
                delay(10000) // 10 seconds
                currentPhotoIndex = (currentPhotoIndex + 1) % catchPhotos.size
            }
        }
    }

    val tripsCount = remember(trips) { trips.size }
    val catchesCount = remember(catches) { catches.count { it.logType == "CATCH" } }
    val lastTripStartTime = remember(trips) { trips.maxOfOrNull { it.startTime } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Area
        HomeHeader(
            catchPhotos = catchPhotos,
            currentPhotoIndex = currentPhotoIndex,
            onSettingsClick = onSettingsClick,
            fishLogViewModel = fishLogViewModel,
            onBackupStatusClick = onBackupStatusClick,
            tripsCount = tripsCount,
            catchesCount = catchesCount,
            lastTripStartTime = lastTripStartTime
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Active Trip section
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            TripStatusCard(
                activeTrip = activeTrip,
                onStartTrip = onStartTripClick,
                onEndTrip = {
                    activeTrip?.let { trip ->
                        fishLogViewModel.endTrip(trip)
                        onEndTripClick(trip)
                    }
                },
                onViewTrip = { activeTrip?.let { onViewTripClick(it) } },
                onLogCatch = onLogCatchClick,
                onLogNoCatch = onLogNoCatchClick,
                fishLogViewModel = fishLogViewModel
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        DashboardGrid(
            onLogCatchClick = onLogCatchClick,
            onLogNoCatchClick = onLogNoCatchClick,
            onHistoryClick = onHistoryClick,
            onMapClick = onMapClick,
            onTripHistoryClick = onTripHistoryClick,
            onInsightsClick = onInsightsClick,
            onAdvancedAnalyticsClick = onAdvancedAnalyticsClick
        )
    }
}

@Composable
fun HomeHeader(
    catchPhotos: List<String>,
    currentPhotoIndex: Int,
    onSettingsClick: () -> Unit,
    fishLogViewModel: FishLogViewModel,
    onBackupStatusClick: () -> Unit,
    tripsCount: Int,
    catchesCount: Int,
    lastTripStartTime: Long?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        if (catchPhotos.isNotEmpty()) {
            val context = LocalContext.current
            Crossfade(
                targetState = catchPhotos.getOrNull(currentPhotoIndex % catchPhotos.size),
                animationSpec = tween(1000),
                label = "PhotoSlideshow"
            ) { photoUri ->
                if (photoUri != null) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(photoUri)
                            .size(800, 400) // Optimized size for header
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .offset(x = 100.dp, y = (-50).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )

            Icon(
                imageVector = Icons.Default.Water,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp),
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-12).dp)
                    .testTag("home_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                CloudBackupStatusChip(
                    fishLogViewModel = fishLogViewModel,
                    onClick = onBackupStatusClick
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "FishLog",
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                )
                Text(
                    text = "Track catches. Spot patterns.",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickStat(
                        label = "Trips",
                        value = tripsCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    QuickStat(
                        label = "Catches",
                        value = catchesCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    if (lastTripStartTime != null) {
                        val lastTripDate = remember(lastTripStartTime) {
                            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(lastTripStartTime))
                        }
                        QuickStat(
                            label = "Last Outing",
                            value = lastTripDate,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardGrid(
    onLogCatchClick: () -> Unit,
    onLogNoCatchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMapClick: () -> Unit,
    onTripHistoryClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onAdvancedAnalyticsClick: () -> Unit
) {
    Column {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            textAlign = TextAlign.Start
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HomeCard(
                    title = "Log Catch",
                    subtitle = "Fish on!",
                    icon = Icons.Default.AddCircle,
                    onClick = onLogCatchClick,
                    modifier = Modifier.testTag("home_log_catch_card")
                )
            }
            item {
                HomeCard(
                    title = "No Catch",
                    subtitle = "Log those too!",
                    icon = Icons.Default.Block,
                    onClick = onLogNoCatchClick,
                    modifier = Modifier.testTag("home_log_no_catch_card")
                )
            }
            item {
                HomeCard(
                    title = "History",
                    subtitle = "Past logs",
                    icon = Icons.Default.History,
                    onClick = onHistoryClick,
                    modifier = Modifier.testTag("home_history_card")
                )
            }
            item {
                HomeCard(
                    title = "Map",
                    subtitle = "See logs on the map",
                    icon = Icons.Default.Map,
                    onClick = onMapClick,
                    modifier = Modifier.testTag("home_map_card")
                )
            }
            item {
                HomeCard(
                    title = "Trip History",
                    subtitle = "See past trips",
                    icon = Icons.Default.ListAlt,
                    onClick = onTripHistoryClick,
                    modifier = Modifier.testTag("home_trip_history_card")
                )
            }
            item {
                HomeCard(
                    title = "Insights",
                    subtitle = "Stats & patterns",
                    icon = Icons.Default.Analytics,
                    onClick = onInsightsClick,
                    modifier = Modifier.testTag("home_insights_card")
                )
            }
            item {
                HomeCard(
                    title = "Advanced Analytics",
                    subtitle = "AI & deeper patterns",
                    icon = Icons.Default.AutoAwesome,
                    badge = FeatureGate.paidLabel(PaidFeature.ADVANCED_ANALYTICS),
                    onClick = onAdvancedAnalyticsClick,
                    modifier = Modifier.testTag("home_advanced_analytics_card")
                )
            }
        }
    }
}

@Composable
fun CloudBackupStatusChip(
    fishLogViewModel: FishLogViewModel,
    onClick: () -> Unit
) {
    val status = fishLogViewModel.cloudBackupStatus ?: return

    val (label, color, icon) = when {
        !status.isSignedIn -> {
            Triple("Cloud Off", Color.Gray, Icons.Default.CloudOff)
        }
        status.isBackingUp -> {
            Triple("Backing Up...", MaterialTheme.colorScheme.primary, Icons.Default.CloudUpload)
        }
        status.lastErrorMessage != null -> {
            Triple("Backup Failed", MaterialTheme.colorScheme.error, Icons.Default.CloudOff)
        }
        !status.isAutomatic -> {
            Triple("Manual Backup", Color.Gray, Icons.Default.Cloud)
        }
        status.isPending -> {
            val workerMsg = status.autoBackupWorkerMessage
            val labelText = when {
                workerMsg?.contains("scheduled soon", ignoreCase = true) == true -> "Backing up soon"
                workerMsg?.contains("scheduled", ignoreCase = true) == true -> "Backup Scheduled"
                else -> "Backup Pending"
            }
            Triple(labelText, Color(0xFFFFA000), Icons.Default.CloudSync)
        }
        else -> {
            Triple("Auto Backup On", Color(0xFF4CAF50), Icons.Default.CloudDone)
        }
    }

    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QuickStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
fun TripStatusCard(
    activeTrip: FishingTrip?,
    onStartTrip: () -> Unit,
    onEndTrip: suspend () -> Unit,
    onViewTrip: () -> Unit,
    onLogCatch: () -> Unit = {},
    onLogNoCatch: () -> Unit = {},
    fishLogViewModel: FishLogViewModel
) {
    var isEndingTrip by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(activeTrip == null) {
        if (activeTrip == null) {
            isEndingTrip = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeTrip != null) 
                MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (activeTrip != null) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = if (activeTrip == null) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (activeTrip == null) {
                Icon(
                    imageVector = Icons.Default.Anchor,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No active fishing trip",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Start a trip to track catches and conditions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartTrip,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_start_trip_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start New Trip")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Green, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE TRIP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Text(
                            text = activeTrip.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (activeTrip.waterBody.isNotBlank()) {
                            Text(
                                text = activeTrip.waterBody,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "Started at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activeTrip.startTime))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val currentMinute = rememberCurrentMinuteMillis()
                        val liveDuration = DurationUtils.formatTripDuration(activeTrip.startTime, now = currentMinute)
                        Text(
                            text = "Duration: $liveDuration",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        val isReminderEnabled = remember(fishLogViewModel.appPreferences) {
                            fishLogViewModel.appPreferences.isActiveTripReminderEnabled()
                        }
                        if (isReminderEnabled) {
                            val reminderHours = remember(fishLogViewModel.appPreferences) {
                                fishLogViewModel.appPreferences.getActiveTripReminderDelayHours()
                            }
                            Text(
                                text = "Reminder after ${reminderHours}h",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onViewTrip,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "View Details",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLogCatch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_active_trip_log_catch_button"),
                        enabled = !isEndingTrip,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Catch")
                    }
                    Button(
                        onClick = onLogNoCatch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_active_trip_log_no_catch_button"),
                        enabled = !isEndingTrip,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No-Catch")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (isEndingTrip) return@Button
                        scope.launch {
                            isEndingTrip = true
                            try {
                                onEndTrip()
                            } catch (e: Exception) {
                                isEndingTrip = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_active_trip_end_trip_button"),
                    enabled = !isEndingTrip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    if (isEndingTrip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End Fishing Trip")
                    }
                }
            }
        }
    }
}

@Composable
fun HomeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (containerColor == MaterialTheme.colorScheme.primaryContainer) 
                        contentColor else MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        lineHeight = 20.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (containerColor == MaterialTheme.colorScheme.primaryContainer) 
                                contentColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            
            if (badge != null) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FishLogTheme {
        // Mock data or empty ViewModel would be needed here for a full preview
        // For now, keeping it simple as requested
    }
}

