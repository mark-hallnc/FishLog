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
import com.fishlog.app.ui.DropdownFilter
import com.fishlog.app.ui.FishLogViewModel
import com.fishlog.app.ui.LogTypeFilter
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onLogClick: (CatchLog) -> Unit
) {
    val context = LocalContext.current
    val catches by viewModel.allCatches.collectAsState()

    var selectedSpecies by remember { mutableStateOf("All Species") }
    var selectedBait by remember { mutableStateOf("All Baits") }
    var logTypeFilter by remember { mutableStateOf(LogTypeFilter.ALL) }
    var showFilters by remember { mutableStateOf(false) }

    val speciesList = remember(catches) {
        listOf("All Species") + catches.filter { it.logType == "CATCH" }.map { it.species }.distinct().sorted()
    }

    val baitList = remember(catches) {
        listOf("All Baits") + catches.map { it.bait }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val logsWithLocation = remember(catches) {
        catches.filter { it.latitude != null && it.longitude != null }
    }

    val filteredLogs = remember(logsWithLocation, selectedSpecies, selectedBait, logTypeFilter) {
        logsWithLocation.filter { log ->
            val matchesSpecies = if (selectedSpecies == "All Species" || log.logType == "NO_CATCH") true else log.species == selectedSpecies
            val matchesBait = if (selectedBait == "All Baits") true else log.bait == selectedBait
            val matchesLogType = when (logTypeFilter) {
                LogTypeFilter.ALL -> true
                LogTypeFilter.CATCHES_ONLY -> log.logType == "CATCH"
                LogTypeFilter.NO_CATCH_ONLY -> log.logType == "NO_CATCH"
            }
            matchesSpecies && matchesBait && matchesLogType
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

    // Track initial load and filter changes for recentering
    var initialCenterApplied by remember { mutableStateOf(false) }
    val filterKey = remember(selectedSpecies, selectedBait, logTypeFilter) {
        "$selectedSpecies-$selectedBait-$logTypeFilter"
    }

    LaunchedEffect(filterKey, filteredLogs.isEmpty()) {
        if (filteredLogs.isNotEmpty()) {
            val mostRecentLog = filteredLogs.maxBy { it.timestamp }
            mapView.controller.setCenter(GeoPoint(mostRecentLog.latitude!!, mostRecentLog.longitude!!))
            if (!initialCenterApplied || mapView.zoomLevelDouble < 10.0) {
                mapView.controller.setZoom(12.0)
                initialCenterApplied = true
            }
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
                    logTypeFilter = logTypeFilter,
                    onLogTypeFilterSelected = { logTypeFilter = it },
                    onClearFilters = {
                        selectedSpecies = "All Species"
                        selectedBait = "All Baits"
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
                        view.overlays.clear()
                        
                        filteredLogs.forEach { log ->
                            val marker = Marker(view)
                            marker.position = GeoPoint(log.latitude!!, log.longitude!!)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            
                            val isNoCatch = log.logType == "NO_CATCH"
                            marker.title = if (isNoCatch) "No Catch" else log.species
                            
                            val dateStr = formatTimestamp(log.timestamp)
                            marker.snippet = if (isNoCatch) {
                                "Temp: ${log.waterTempF ?: log.waterTemp}°F, Depth: ${log.depthFeet ?: log.depth}ft\nBait: ${log.bait}\nDate: $dateStr"
                            } else {
                                "${log.lengthInches ?: log.length} in, ${log.weightLbs ?: log.weight} lbs\nBait: ${log.bait}\nDate: $dateStr"
                            }
                            
                            // Visual distinction for no-catch
                            if (isNoCatch) {
                                marker.icon = context.getDrawable(org.osmdroid.library.R.drawable.marker_default_focused_base)
                            }
                            
                            marker.setOnMarkerClickListener { _, _ ->
                                onLogClick(log)
                                true
                            }
                            
                            view.overlays.add(marker)
                        }

                        view.invalidate()
                    }
                )

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
                            text = "${filteredLogs.size} of ${logsWithLocation.size} mapped",
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
                                text = "No matches found",
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
                TextButton(onClick = onClearFilters, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Clear", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
