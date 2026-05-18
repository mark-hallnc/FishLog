package com.fishlog.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.location.LocationService
import com.fishlog.app.ui.DropdownFilter
import com.fishlog.app.ui.FishLogViewModel
import com.fishlog.app.ui.LogTypeFilter
import com.fishlog.app.ui.DateRangeFilter
import com.fishlog.app.ui.DateFilterControls
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onLogClick: (CatchLog) -> Unit,
    onTripClick: (FishingTrip) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val catches by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()

    var selectedSpecies by remember { mutableStateOf("All Species") }
    var selectedBait by remember { mutableStateOf("All Baits") }
    var selectedTripId by remember { mutableStateOf<Long?>(null) } // null = All, -1 = Standalone
    var selectedWaterBody by remember { mutableStateOf("All Water Bodies") }
    var dateFilter by remember { mutableStateOf<DateRangeFilter>(DateRangeFilter.AllDates) }
    var logTypeFilter by remember { mutableStateOf(LogTypeFilter.ALL) }
    var showFilters by remember { mutableStateOf(false) }
    
    var selectedLogForOverlay by remember { mutableStateOf<CatchLog?>(null) }

    val speciesList = remember(catches) {
        listOf("All Species") + catches.filter { it.logType == "CATCH" }.map { it.species }.distinct().sorted()
    }

    val baitList = remember(catches) {
        listOf("All Baits") + catches.map { it.bait }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val availableMonths = remember(catches) {
        catches.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            DateRangeFilter.Month(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        }.distinct().sortedByDescending { it.year * 12 + it.month }
    }

    val availableYears = remember(catches) {
        catches.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR)
        }.distinct().sortedDescending()
    }

    val tripOptions = remember(trips) {
        listOf("All Logs", "Not attached to trip") + trips.map { "${it.name}${if (it.waterBody.isNotBlank()) " · ${it.waterBody}" else ""}" }
    }
    
    val selectedTripLabel = remember(selectedTripId, trips) {
        when (selectedTripId) {
            null -> "All Logs"
            -1L -> "Not attached to trip"
            else -> trips.find { it.id == selectedTripId }?.let { "${it.name}${if (it.waterBody.isNotBlank()) " · ${it.waterBody}" else ""}" } ?: "All Logs"
        }
    }

    val waterBodyList = remember(trips) {
        listOf("All Water Bodies") + trips.map { it.waterBody.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.sorted()
    }

    val logsWithLocation = remember(catches) {
        catches.filter { it.latitude != null && it.longitude != null }
    }

    val filteredLogs = remember(logsWithLocation, selectedSpecies, selectedBait, selectedTripId, selectedWaterBody, dateFilter, logTypeFilter, trips) {
        logsWithLocation.filter { log ->
            val matchesSpecies = if (selectedSpecies == "All Species" || log.logType == "NO_CATCH") true else log.species == selectedSpecies
            val matchesBait = if (selectedBait == "All Baits") true else log.bait == selectedBait
            
            val matchesTrip = when (selectedTripId) {
                null -> true
                -1L -> log.tripId == null
                else -> log.tripId == selectedTripId
            }
            
            val matchesWaterBody = if (selectedWaterBody == "All Water Bodies") {
                true
            } else {
                val trip = trips.find { it.id == log.tripId }
                trip?.waterBody?.trim()?.equals(selectedWaterBody, ignoreCase = true) == true
            }
            
            val matchesDate = dateFilter.matches(log.timestamp)

            val matchesLogType = when (logTypeFilter) {
                LogTypeFilter.ALL -> true
                LogTypeFilter.CATCHES_ONLY -> log.logType == "CATCH"
                LogTypeFilter.NO_CATCH_ONLY -> log.logType == "NO_CATCH"
            }
            
            matchesSpecies && matchesBait && matchesTrip && matchesWaterBody && matchesDate && matchesLogType
        }
    }

    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }
    
    val userLocationMarker = remember {
        Marker(mapView).apply {
            title = "You are here"
            icon = context.getDrawable(org.osmdroid.library.R.drawable.person)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    // Track initial load and filter changes for recentering
    var initialCenterApplied by remember { mutableStateOf(false) }

    fun centerOnUser() {
        scope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(15.0)
                userLocationMarker.position = geoPoint
                if (!mapView.overlays.contains(userLocationMarker)) {
                    mapView.overlays.add(userLocationMarker)
                }
            } else {
                // Fallback to most recent log if available
                if (filteredLogs.isNotEmpty()) {
                    val mostRecentLog = filteredLogs.maxBy { it.timestamp }
                    mapView.controller.setCenter(GeoPoint(mostRecentLog.latitude!!, mostRecentLog.longitude!!))
                    mapView.controller.setZoom(12.0)
                } else {
                    // Fallback to High Point, NC
                    mapView.controller.setCenter(GeoPoint(35.9557, -80.0053))
                    mapView.controller.setZoom(12.0)
                }
            }
            initialCenterApplied = true
            mapView.invalidate()
        }
    }

    LaunchedEffect(Unit) {
        if (!initialCenterApplied) {
            centerOnUser()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { centerOnUser() }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center on Me")
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Toggle Filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .clipToBounds()
        ) {
            if (showFilters) {
                MapFilterSection(
                    selectedSpecies = selectedSpecies,
                    speciesList = speciesList,
                    onSpeciesSelected = { selectedSpecies = it },
                    selectedBait = selectedBait,
                    baitList = baitList,
                    onBaitSelected = { selectedBait = it },
                    selectedTripLabel = selectedTripLabel,
                    tripOptions = tripOptions,
                    onTripSelected = { label ->
                        selectedTripId = when (label) {
                            "All Logs" -> null
                            "Not attached to trip" -> -1L
                            else -> {
                                val tripName = label.split(" · ").first()
                                trips.find { it.name == tripName }?.id
                            }
                        }
                    },
                    selectedWaterBody = selectedWaterBody,
                    waterBodyList = waterBodyList,
                    onWaterBodySelected = { selectedWaterBody = it },
                    dateFilter = dateFilter,
                    onDateFilterChange = { dateFilter = it },
                    availableMonths = availableMonths,
                    availableYears = availableYears,
                    logTypeFilter = logTypeFilter,
                    onLogTypeFilterSelected = { logTypeFilter = it },
                    onClearFilters = {
                        selectedSpecies = "All Species"
                        selectedBait = "All Baits"
                        selectedTripId = null
                        selectedWaterBody = "All Water Bodies"
                        dateFilter = DateRangeFilter.AllDates
                        logTypeFilter = LogTypeFilter.ALL
                    },
                    modifier = Modifier.zIndex(2f)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
                    .zIndex(0f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView },
                    update = { view ->
                        // Clear markers except user location
                        val overlaysToKeep = if (view.overlays.contains(userLocationMarker)) listOf(userLocationMarker) else emptyList()
                        view.overlays.clear()
                        view.overlays.addAll(overlaysToKeep)
                        
                        filteredLogs.forEach { log ->
                            val marker = Marker(view)
                            marker.position = GeoPoint(log.latitude!!, log.longitude!!)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            val isNoCatch = log.logType == "NO_CATCH"
                            marker.title = if (isNoCatch) "No Catch" else log.species
                            
                            val dateStr = formatTimestamp(log.timestamp)
                            val trip = trips.find { it.id == log.tripId }
                            val tripLine = if (trip != null) "\nTrip: ${trip.name}" else ""
                            
                            marker.snippet = if (isNoCatch) {
                                "Date: $dateStr\nBait: ${log.bait}\nTemp: ${log.waterTempF ?: log.waterTemp}°F, Depth: ${log.depthFeet ?: log.depth}ft$tripLine"
                            } else {
                                "Date: $dateStr\nBait: ${log.bait}\nSize: ${log.lengthInches ?: log.length} in, ${log.weightLbs ?: log.weight} lbs$tripLine"
                            }
                            
                            // Visual distinction for no-catch
                            if (isNoCatch) {
                                marker.icon = context.getDrawable(org.osmdroid.library.R.drawable.marker_default_focused_base)
                            } else {
                                marker.icon = context.getDrawable(org.osmdroid.library.R.drawable.marker_default)
                            }
                            
                            marker.setOnMarkerClickListener { _, _ ->
                                selectedLogForOverlay = log
                                // Also animate to marker to make it clear what was selected
                                mapView.controller.animateTo(marker.position)
                                true
                            }
                            
                            view.overlays.add(marker)
                        }

                        view.invalidate()
                    }
                )

                // Marker detail overlay
                selectedLogForOverlay?.let { log ->
                    val trip = trips.find { it.id == log.tripId }
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text(
                                        text = if (log.logType == "NO_CATCH") "No Catch" else log.species,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = formatTimestamp(log.timestamp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                IconButton(onClick = { selectedLogForOverlay = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            
                            if (trip != null) {
                                Text(
                                    text = "Trip: ${trip.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onLogClick(log) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("View Log")
                                }
                                if (trip != null) {
                                    OutlinedButton(
                                        onClick = { onTripClick(trip) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("View Trip")
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay count and messages
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "Showing ${filteredLogs.size} of ${logsWithLocation.size} mapped logs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    
                    if (filteredLogs.isEmpty() && logsWithLocation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "No mapped logs match these filters.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (logsWithLocation.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "No log locations saved yet.",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                FloatingActionButton(
                    onClick = { centerOnUser() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on Me")
                }
            }
        }
    }
}

@Composable
fun MapFilterSection(
    selectedSpecies: String,
    speciesList: List<String>,
    onSpeciesSelected: (String) -> Unit,
    selectedBait: String,
    baitList: List<String>,
    onBaitSelected: (String) -> Unit,
    selectedTripLabel: String,
    tripOptions: List<String>,
    onTripSelected: (String) -> Unit,
    selectedWaterBody: String,
    waterBodyList: List<String>,
    onWaterBodySelected: (String) -> Unit,
    dateFilter: DateRangeFilter,
    onDateFilterChange: (DateRangeFilter) -> Unit,
    availableMonths: List<DateRangeFilter.Month>,
    availableYears: List<Int>,
    logTypeFilter: LogTypeFilter,
    onLogTypeFilterSelected: (LogTypeFilter) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row 1: Species & Bait
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownFilter(
                    label = "Species",
                    selectedOption = selectedSpecies,
                    options = speciesList,
                    onOptionSelected = onSpeciesSelected,
                    modifier = Modifier.weight(1f)
                )
                DropdownFilter(
                    label = "Bait",
                    selectedOption = selectedBait,
                    options = baitList,
                    onOptionSelected = onBaitSelected,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Trip & Water Body
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownFilter(
                    label = "Trip",
                    selectedOption = selectedTripLabel,
                    options = tripOptions,
                    onOptionSelected = onTripSelected,
                    modifier = Modifier.weight(1f)
                )
                DropdownFilter(
                    label = "Water Body",
                    selectedOption = selectedWaterBody,
                    options = waterBodyList,
                    onOptionSelected = onWaterBodySelected,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Date Filter
            DateFilterControls(
                selectedFilter = dateFilter,
                onFilterChange = onDateFilterChange,
                availableMonths = availableMonths,
                availableYears = availableYears,
                modifier = Modifier.fillMaxWidth()
            )

            // Row 4: Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = logTypeFilter == LogTypeFilter.ALL,
                        onClick = { onLogTypeFilterSelected(LogTypeFilter.ALL) },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = logTypeFilter == LogTypeFilter.CATCHES_ONLY,
                        onClick = { onLogTypeFilterSelected(LogTypeFilter.CATCHES_ONLY) },
                        label = { Text("Catch", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = logTypeFilter == LogTypeFilter.NO_CATCH_ONLY,
                        onClick = { onLogTypeFilterSelected(LogTypeFilter.NO_CATCH_ONLY) },
                        label = { Text("No-Catch", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                
                TextButton(
                    onClick = onClearFilters, 
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Clear All", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
