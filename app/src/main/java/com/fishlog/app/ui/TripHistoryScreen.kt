package com.fishlog.app.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.saveable.rememberSaveable
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.CatchLog
import com.fishlog.app.ui.DateRangeFilter
import com.fishlog.app.ui.DateFilterControls
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onTripClick: (FishingTrip) -> Unit,
    onStartTripClick: () -> Unit
) {
    val trips by viewModel.allTrips.collectAsState()
    val catches by viewModel.allCatches.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All Trips") }
    var waterBodyFilter by remember { mutableStateOf("All Water Bodies") }
    var dateFilter by remember { mutableStateOf<DateRangeFilter>(DateRangeFilter.AllDates) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val waterBodies = remember(trips) {
        listOf("All Water Bodies") + trips.map { it.waterBody }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val availableMonths = remember(trips) {
        trips.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTime
            DateRangeFilter.Month(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        }.distinct().sortedByDescending { it.year * 12 + it.month }
    }

    val availableYears = remember(trips) {
        trips.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.startTime
            cal.get(Calendar.YEAR)
        }.distinct().sortedDescending()
    }

    val filteredTrips = remember(trips, searchQuery, statusFilter, waterBodyFilter, dateFilter) {
        trips.filter { trip ->
            val matchesSearch = trip.name.contains(searchQuery, ignoreCase = true) ||
                    trip.waterBody.contains(searchQuery, ignoreCase = true) ||
                    trip.notes.contains(searchQuery, ignoreCase = true)
            
            val matchesStatus = when (statusFilter) {
                "Active" -> trip.endTime == null
                "Completed" -> trip.endTime != null
                else -> true
            }
            
            val matchesWaterBody = if (waterBodyFilter == "All Water Bodies") true else trip.waterBody == waterBodyFilter
            
            val matchesDate = dateFilter.matches(trip.startTime)

            matchesSearch && matchesStatus && matchesWaterBody && matchesDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier.testTag("trip_history_filter_button")
                    ) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                            contentDescription = "Toggle Filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            // Filters section
            if (showFilters) {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search trips...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = null
                                        )
                                    }
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DropdownFilter(
                                label = "Status",
                                selectedOption = statusFilter,
                                options = listOf("All Trips", "Active", "Completed"),
                                onOptionSelected = { statusFilter = it },
                                modifier = Modifier.weight(1f)
                            )
                            DropdownFilter(
                                label = "Water Body",
                                selectedOption = waterBodyFilter,
                                options = waterBodies,
                                onOptionSelected = { waterBodyFilter = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        DateFilterControls(
                            selectedFilter = dateFilter,
                            onFilterChange = { dateFilter = it },
                            availableMonths = availableMonths,
                            availableYears = availableYears,
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextButton(
                            onClick = {
                                searchQuery = ""
                                statusFilter = "All Trips"
                                waterBodyFilter = "All Water Bodies"
                                dateFilter = DateRangeFilter.AllDates
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Clear Filters")
                        }
                    }
                }
            }

            // Results summary shown only when filters are active or panel is open
            if (showFilters || searchQuery.isNotBlank() || statusFilter != "All Trips" || waterBodyFilter != "All Water Bodies" || dateFilter != DateRangeFilter.AllDates) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${filteredTrips.size} of ${trips.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (trips.isEmpty()) {
                EmptyTripsState(onStartTripClick)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrips) { trip ->
                        val tripCatches = catches.filter { it.tripId == trip.id }
                        TripHistoryCard(
                            trip = trip,
                            catches = tripCatches,
                            onClick = { onTripClick(trip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TripHistoryCard(
    trip: FishingTrip,
    catches: List<CatchLog>,
    onClick: () -> Unit
) {
    val isActive = trip.endTime == null
    val catchCount = catches.count { it.logType == "CATCH" }
    val noCatchCount = catches.count { it.logType == "NO_CATCH" }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (trip.waterBody.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trip.waterBody,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                if (isActive) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text("ACTIVE", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                } else {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text("COMPLETED", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            val conditions = remember(trip) {
                listOfNotNull(
                    trip.skyCondition.ifBlank { null },
                    trip.windCondition.ifBlank { null },
                    trip.airTempF?.let { "${it.toInt()}°F" }
                ).joinToString(" · ")
            }
            
            if (conditions.isNotBlank()) {
                Text(
                    text = conditions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Started", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(formatTime(trip.startTime), style = MaterialTheme.typography.bodySmall)
                }
                
                if (!isActive) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(formatDuration(trip.startTime, trip.endTime!!), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TripStat(label = "Catches", value = catchCount.toString(), icon = Icons.Default.AddCircle)
                TripStat(label = "No-Catch", value = noCatchCount.toString(), icon = Icons.Default.Block)
                TripStat(label = "Total Logs", value = catches.size.toString(), icon = Icons.Default.History)
            }
        }
    }
}

@Composable
fun TripStat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun EmptyTripsState(onStartTripClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.ListAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No trips logged yet.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Start a trip to group catches and no-catch logs into a fishing session.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onStartTripClick, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Your First Trip")
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(start: Long, end: Long): String {
    val diff = end - start
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff / (1000 * 60)) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
