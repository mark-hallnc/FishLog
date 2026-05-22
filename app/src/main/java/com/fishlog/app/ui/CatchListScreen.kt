package com.fishlog.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import com.fishlog.app.data.CatchLog
import com.fishlog.app.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

enum class GpsFilter { ALL, WITH_LOCATION, WITHOUT_LOCATION }
enum class LogTypeFilter { ALL, CATCHES_ONLY, NO_CATCH_ONLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchListScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onCatchClick: (CatchLog) -> Unit,
    onPhotoClick: (String) -> Unit = {}
) {
    val catches by viewModel.allCatches.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSpecies by remember { mutableStateOf("All Species") }
    var selectedBait by remember { mutableStateOf("All Baits") }
    var gpsFilter by remember { mutableStateOf(GpsFilter.ALL) }
    var logTypeFilter by remember { mutableStateOf(LogTypeFilter.ALL) }
    var dateFilter by remember { mutableStateOf<DateRangeFilter>(DateRangeFilter.AllDates) }
    var showFilters by remember { mutableStateOf(false) }

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

    val filteredCatches = remember(catches, searchQuery, selectedSpecies, selectedBait, gpsFilter, logTypeFilter, dateFilter) {
        catches.filter { catch ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val query = searchQuery.lowercase()
                catch.species.lowercase().contains(query) ||
                        catch.bait.lowercase().contains(query) ||
                        catch.notes.lowercase().contains(query) ||
                        catch.depth.lowercase().contains(query) ||
                        catch.waterTemp.lowercase().contains(query) ||
                        catch.length.lowercase().contains(query) ||
                        catch.weight.lowercase().contains(query)
            }

            val matchesSpecies = if (selectedSpecies == "All Species" || catch.logType == "NO_CATCH") true else catch.species == selectedSpecies
            val matchesBait = if (selectedBait == "All Baits") true else catch.bait == selectedBait
            val matchesGps = when (gpsFilter) {
                GpsFilter.ALL -> true
                GpsFilter.WITH_LOCATION -> catch.latitude != null && catch.longitude != null
                GpsFilter.WITHOUT_LOCATION -> catch.latitude == null || catch.longitude == null
            }
            val matchesLogType = when (logTypeFilter) {
                LogTypeFilter.ALL -> true
                LogTypeFilter.CATCHES_ONLY -> catch.logType == "CATCH"
                LogTypeFilter.NO_CATCH_ONLY -> catch.logType == "NO_CATCH"
            }
            val matchesDate = dateFilter.matches(catch.timestamp)

            matchesSearch && matchesSpecies && matchesBait && matchesGps && matchesLogType && matchesDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catch History") },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (showFilters) {
                FilterSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedSpecies = selectedSpecies,
                    speciesList = speciesList,
                    onSpeciesSelected = { selectedSpecies = it },
                    selectedBait = selectedBait,
                    baitList = baitList,
                    onBaitSelected = { selectedBait = it },
                    gpsFilter = gpsFilter,
                    onGpsFilterSelected = { gpsFilter = it },
                    logTypeFilter = logTypeFilter,
                    onLogTypeFilterSelected = { logTypeFilter = it },
                    dateFilter = dateFilter,
                    onDateFilterChange = { dateFilter = it },
                    availableMonths = availableMonths,
                    availableYears = availableYears,
                    onClearFilters = {
                        searchQuery = ""
                        selectedSpecies = "All Species"
                        selectedBait = "All Baits"
                        gpsFilter = GpsFilter.ALL
                        logTypeFilter = LogTypeFilter.ALL
                        dateFilter = DateRangeFilter.AllDates
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Logs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "${filteredCatches.size} of ${catches.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            if (filteredCatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (catches.isEmpty()) "No logs yet" else "No matches found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCatches) { catch ->
                        CatchItem(
                            catch = catch,
                            onClick = { onCatchClick(catch) },
                            onPhotoClick = onPhotoClick,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSpecies: String,
    speciesList: List<String>,
    onSpeciesSelected: (String) -> Unit,
    selectedBait: String,
    baitList: List<String>,
    onBaitSelected: (String) -> Unit,
    gpsFilter: GpsFilter,
    onGpsFilterSelected: (GpsFilter) -> Unit,
    logTypeFilter: LogTypeFilter,
    onLogTypeFilterSelected: (LogTypeFilter) -> Unit,
    dateFilter: DateRangeFilter,
    onDateFilterChange: (DateRangeFilter) -> Unit,
    availableMonths: List<DateRangeFilter.Month>,
    availableYears: List<Int>,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search species, bait, notes...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

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

            DateFilterControls(
                selectedFilter = dateFilter,
                onFilterChange = onDateFilterChange,
                availableMonths = availableMonths,
                availableYears = availableYears,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GPS", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = gpsFilter == GpsFilter.ALL,
                            onClick = { onGpsFilterSelected(GpsFilter.ALL) },
                            label = { Text("All") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = gpsFilter == GpsFilter.WITH_LOCATION,
                            onClick = { onGpsFilterSelected(GpsFilter.WITH_LOCATION) },
                            label = { Text("GPS") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = logTypeFilter == LogTypeFilter.ALL,
                            onClick = { onLogTypeFilterSelected(LogTypeFilter.ALL) },
                            label = { Text("All") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = logTypeFilter == LogTypeFilter.CATCHES_ONLY,
                            onClick = { onLogTypeFilterSelected(LogTypeFilter.CATCHES_ONLY) },
                            label = { Text("Catch") },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = logTypeFilter == LogTypeFilter.NO_CATCH_ONLY,
                            onClick = { onLogTypeFilterSelected(LogTypeFilter.NO_CATCH_ONLY) },
                            label = { Text("No-Catch") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Clear All")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CatchItem(
    catch: CatchLog,
    onClick: () -> Unit,
    onPhotoClick: (String) -> Unit = {},
    viewModel: FishLogViewModel? = null
) {
    val isNoCatch = catch.logType == "NO_CATCH"
    val trips by (viewModel?.allTrips?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val trip = remember(trips, catch.tripId) { trips.find { it.id == catch.tripId } }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNoCatch) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (catch.photoUri != null && !isNoCatch) {
                Image(
                    painter = rememberAsyncImagePainter(catch.photoUri),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPhotoClick(catch.photoUri) },
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isNoCatch) "No Catch" else catch.species,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isNoCatch) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                if (trip != null) {
                    Text(
                        text = "Trip: ${trip.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = formatTimestamp(catch.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isNoCatch) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (catch.waterTemp.isNotBlank() || catch.waterTempF != null) {
                            Icon(Icons.Default.Thermostat, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                            val tempValue = catch.waterTempF ?: catch.waterTemp.toDoubleOrNull()
                            Text(" ${FormatUtils.formatWholeNumber(tempValue)} °F", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (catch.depth.isNotBlank() || catch.depthFeet != null) {
                            Icon(Icons.Default.Water, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                            val depthValue = catch.depthFeet ?: catch.depth.toDoubleOrNull()
                            Text(" ${FormatUtils.formatWholeNumber(depthValue)} ft", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (catch.bait.isNotBlank()) {
                        Text("Bait: ${catch.bait}", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Straighten,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        val lengthValue = catch.lengthInches ?: catch.length.toDoubleOrNull()
                        Text(
                            text = " ${FormatUtils.formatDecimal(lengthValue)} in",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.Scale,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        val weightValue = catch.weightLbs ?: catch.weight.toDoubleOrNull()
                        Text(
                            text = " ${FormatUtils.formatDecimal(weightValue)} lbs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            if (catch.latitude != null && catch.longitude != null) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Has location",
                    tint = if (isNoCatch) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

