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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import com.fishlog.app.ui.PatternDetailScreen
import com.fishlog.app.ui.PhotoViewerScreen
import com.fishlog.app.ui.theme.FishLogTheme
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.PhotoStorageHelper
import com.fishlog.app.data.AppPreferences
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        setContent {
            val viewModel: FishLogViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FishLogViewModel(
                            catchDao, 
                            tripDao, 
                            cloudBackupRepository, 
                            weatherRepository,
                            waterBodySuggestionRepository
                        ) as T
                    }
                }
            )

            val appPreferences = remember { AppPreferences(applicationContext) }
            var appearanceMode by remember { mutableStateOf(appPreferences.getAppearanceMode()) }
            var unitSystem by remember { mutableStateOf(appPreferences.getUnitSystem()) }
            
            var mapCenterMode by remember { mutableStateOf(appPreferences.getMapCenterMode()) }
            var mapDefaultLat by remember { mutableStateOf(appPreferences.getMapLatitude()) }
            var mapDefaultLon by remember { mutableStateOf(appPreferences.getMapLongitude()) }
            var mapDefaultZoom by remember { mutableStateOf(appPreferences.getMapZoom()) }

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
    onAppearanceModeChange: (String) -> Unit,
    onUnitSystemChange: (String) -> Unit,
    onMapCenterModeChange: (String) -> Unit,
    onSetDefaultMapLocation: (Double, Double, Double) -> Unit,
    onClearDefaultMapLocation: () -> Unit
) {
    val context = LocalContext.current
    val photoStorageHelper = remember { PhotoStorageHelper(context) }
    var currentScreen by remember { mutableStateOf("Home") }
    var previousScreen by remember { mutableStateOf("Home") }
    var previousTripScreen by remember { mutableStateOf("Home") }
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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentScreen) {
            "Home" -> HomeScreen(
                viewModel = viewModel,
                onLogCatchClick = { 
                    selectedCatch = null
                    currentScreen = "Form" 
                },
                onLogNoCatchClick = {
                    selectedCatch = null
                    currentScreen = "NoCatchForm"
                },
                onHistoryClick = { 
                    previousScreen = "Home"
                    currentScreen = "History" 
                },
                onMapClick = { 
                    previousScreen = "Home"
                    focusedLogOnMap = null
                    savedMapReturnState = null
                    mapSelectedLogForOverlay = null
                    currentScreen = "Map" 
                },
                onInsightsClick = { currentScreen = "Insights" },
                onAdvancedAnalyticsClick = { currentScreen = "AdvancedAnalytics" },
                onTripHistoryClick = { currentScreen = "TripHistory" },
                onSettingsClick = { currentScreen = "Settings" },
                onStartTripClick = { currentScreen = "StartTrip" },
                onViewTripClick = { trip ->
                    selectedTrip = trip
                    previousTripScreen = "Home"
                    currentScreen = "TripDetail"
                },
                onEndTripClick = { trip ->
                    selectedTrip = trip.copy(endTime = System.currentTimeMillis())
                    currentScreen = "TripSummary"
                },
                modifier = modifier
            )
            "StartTrip" -> StartTripScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onTripStarted = { currentScreen = "Home" }
            )
            "TripDetail" -> selectedTrip?.let { trip ->
                TripDetailScreen(
                    trip = trip,
                    viewModel = viewModel,
                    unitSystem = unitSystem,
                    onBack = { currentScreen = previousTripScreen },
                    onLogClick = { catch ->
                        selectedCatch = catch
                        previousScreen = "TripDetail"
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
                    onBack = { currentScreen = "TripDetail" },
                    onSave = { updatedTrip ->
                        selectedTrip = updatedTrip
                        currentScreen = "TripDetail"
                    }
                )
            } ?: run { currentScreen = "Home" }
            "Form" -> CatchFormScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { currentScreen = if (selectedCatch != null) "Detail" else "Home" },
                editingCatch = selectedCatch
            )
            "NoCatchForm" -> NoCatchFormScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { currentScreen = if (selectedCatch != null) "Detail" else "Home" },
                editingLog = selectedCatch
            )
            "History" -> CatchListScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onCatchClick = { catch ->
                    selectedCatch = catch
                    previousScreen = "History"
                    currentScreen = "Detail"
                },
                onPhotoClick = { uri ->
                    selectedPhotoUri = uri
                    previousScreen = "History"
                    currentScreen = "PhotoViewer"
                }
            )
            "Detail" -> selectedCatch?.let { catch ->
                CatchDetailScreen(
                    catch = catch,
                    unitSystem = unitSystem,
                    onBack = { currentScreen = previousScreen },
                    onEdit = { 
                        currentScreen = if (catch.logType == "NO_CATCH") "NoCatchForm" else "Form"
                    },
                    onDelete = {
                        viewModel.deleteCatch(catch, photoStorageHelper)
                        currentScreen = previousScreen
                    },
                    onViewOnMap = { log ->
                        selectedCatch = log
                        focusedLogOnMap = log
                        previousScreen = "Detail"
                        savedMapReturnState = null
                        mapSelectedLogForOverlay = log
                        currentScreen = "Map"
                    },
                    onPhotoClick = { uri ->
                        selectedPhotoUri = uri
                        previousScreen = "Detail"
                        currentScreen = "PhotoViewer"
                    }
                )
            } ?: run { currentScreen = previousScreen }
            "PhotoViewer" -> selectedPhotoUri?.let { uri ->
                PhotoViewerScreen(
                    photoUri = uri,
                    onBack = { currentScreen = previousScreen }
                )
            } ?: run { currentScreen = previousScreen }
            "Map" -> MapScreen(
                viewModel = viewModel,
                onBack = { 
                    currentScreen = previousScreen
                    savedMapReturnState = null 
                },
                onLogClick = { catch, returnState ->
                    selectedCatch = catch
                    savedMapReturnState = returnState
                    previousScreen = "Map"
                    currentScreen = "Detail"
                },
                onTripClick = { trip, returnState ->
                    selectedTrip = trip
                    savedMapReturnState = returnState
                    previousTripScreen = "Map"
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
                mapDefaultZoom = mapDefaultZoom
            )
            "MapLocationPicker" -> MapLocationPickerScreen(
                initialLat = mapDefaultLat,
                initialLon = mapDefaultLon,
                initialZoom = mapDefaultZoom,
                viewModel = viewModel,
                onLocationPicked = { lat, lon, zoom ->
                    onSetDefaultMapLocation(lat, lon, zoom)
                    currentScreen = "Settings"
                },
                onBack = { currentScreen = "Settings" }
            )
            "Insights" -> InsightsScreen(
                viewModel = viewModel,
                unitSystem = unitSystem,
                onBack = { currentScreen = "Home" }
            )
            "AdvancedAnalytics" -> AdvancedAnalyticsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onInsightClick = { insight ->
                    selectedPatternInsight = insight
                    previousScreen = "AdvancedAnalytics"
                    currentScreen = "PatternDetail"
                },
                onViewReports = { filters ->
                    selectedReportFilters = filters
                    currentScreen = "AdvancedReports"
                }
            )
            "AdvancedReports" -> {
                val catches by viewModel.allCatches.collectAsState()
                val trips by viewModel.allTrips.collectAsState()
                AdvancedReportsScreen(
                    logs = catches,
                    trips = trips,
                    filters = selectedReportFilters,
                    onBack = { currentScreen = "AdvancedAnalytics" },
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
                        previousScreen = "AdvancedReports"
                        currentScreen = "PatternDetail"
                    }
                )
            }
            "PatternDetail" -> selectedPatternInsight?.let { insight ->
                val catches by viewModel.allCatches.collectAsState()
                val trips by viewModel.allTrips.collectAsState()
                PatternDetailScreen(
                    insight = insight,
                    allLogs = catches,
                    allTrips = trips,
                    onBack = { currentScreen = "AdvancedAnalytics" },
                    onLogClick = { log ->
                        selectedCatch = log
                        previousScreen = "PatternDetail"
                        currentScreen = "Detail"
                    },
                    onTripClick = { trip ->
                        selectedTrip = trip
                        previousTripScreen = "PatternDetail"
                        currentScreen = "TripDetail"
                    }
                )
            } ?: run { currentScreen = "AdvancedAnalytics" }
            "TripHistory" -> TripHistoryScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onTripClick = { trip ->
                    selectedTrip = trip
                    previousTripScreen = "TripHistory"
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
                        previousTripScreen = "Home"
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
                onAppearanceModeChange = onAppearanceModeChange,
                onUnitSystemChange = onUnitSystemChange,
                onMapCenterModeChange = onMapCenterModeChange,
                onClearDefaultMapLocation = onClearDefaultMapLocation,
                onChooseDefaultMapLocation = { currentScreen = "MapLocationPicker" },
                onBack = { currentScreen = "Home" }
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: FishLogViewModel,
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
    modifier: Modifier = Modifier
) {
    val activeTrip by viewModel.activeTrip.collectAsState()
    val catches by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()

    val catchPhotos = remember(catches) {
        catches.filter { it.logType == "CATCH" && !it.photoUri.isNullOrBlank() }.map { it.photoUri!! }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Area
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
                Crossfade(
                    targetState = catchPhotos.getOrNull(currentPhotoIndex % catchPhotos.size),
                    animationSpec = tween(1000),
                    label = "PhotoSlideshow"
                ) { photoUri ->
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                
                // Readability overlay (dark gradient)
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
                // Decorative background element (subtle wave-like circle) - Only if no photos
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

            // Content inside the Hero Area
            Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 12.dp, y = (-12).dp)
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
                    
                    // Quick stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QuickStat(
                            label = "Trips",
                            value = trips.size.toString(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        QuickStat(
                            label = "Catches",
                            value = catches.count { it.logType == "CATCH" }.toString(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (trips.isNotEmpty()) {
                            val lastTripDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(trips.maxOf { it.startTime }))
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
                        viewModel.endTrip(trip)
                        onEndTripClick(trip)
                    }
                },
                onViewTrip = { activeTrip?.let { onViewTripClick(it) } },
                onLogCatch = onLogCatchClick,
                onLogNoCatch = onLogNoCatchClick
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
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
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HomeCard(
                    title = "Log Catch",
                    subtitle = "Got one!",
                    icon = Icons.Default.AddCircle,
                    onClick = onLogCatchClick
                )
            }
            item {
                HomeCard(
                    title = "No Catch",
                    subtitle = "Still fishing",
                    icon = Icons.Default.Block,
                    onClick = onLogNoCatchClick
                )
            }
            item {
                HomeCard(
                    title = "History",
                    subtitle = "Past logs",
                    icon = Icons.Default.History,
                    onClick = onHistoryClick
                )
            }
            item {
                HomeCard(
                    title = "Map",
                    subtitle = "Hotspots",
                    icon = Icons.Default.Map,
                    onClick = onMapClick
                )
            }
            item {
                HomeCard(
                    title = "Trip History",
                    subtitle = "Outings",
                    icon = Icons.Default.ListAlt,
                    onClick = onTripHistoryClick
                )
            }
            item {
                HomeCard(
                    title = "Insights",
                    subtitle = "Stats & patterns",
                    icon = Icons.Default.Analytics,
                    onClick = onInsightsClick
                )
            }
            item {
                HomeCard(
                    title = "Advanced Analytics",
                    subtitle = "AI & deeper patterns",
                    icon = Icons.Default.AutoAwesome,
                    badge = FeatureGate.paidLabel(PaidFeature.ADVANCED_ANALYTICS),
                    onClick = onAdvancedAnalyticsClick
                )
            }
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
    onLogNoCatch: () -> Unit = {}
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
                    modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.weight(1f),
                        enabled = !isEndingTrip,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Catch")
                    }
                    Button(
                        onClick = onLogNoCatch,
                        modifier = Modifier.weight(1f),
                        enabled = !isEndingTrip,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No-Catch")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        if (isEndingTrip) return@OutlinedButton
                        scope.launch {
                            isEndingTrip = true
                            try {
                                onEndTrip()
                            } catch (e: Exception) {
                                isEndingTrip = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEndingTrip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isEndingTrip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error,
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
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
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

