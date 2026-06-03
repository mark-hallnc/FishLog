package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.analytics.TripReview
import com.fishlog.app.analytics.TripReviewEngine
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReviewScreen(
    trips: List<FishingTrip>,
    allLogs: List<CatchLog>,
    onBack: () -> Unit
) {
    // Only completed trips with logs
    val completedTrips = remember(trips, allLogs) {
        trips.filter { trip ->
            trip.endTime != null && allLogs.any { it.tripId == trip.id }
        }.sortedByDescending { it.startTime }
    }

    var selectedTrip by remember { mutableStateOf<FishingTrip?>(null) }
    
    val review = remember(selectedTrip, allLogs, trips) {
        selectedTrip?.let { trip ->
            val tripLogs = allLogs.filter { it.tripId == trip.id }
            TripReviewEngine.generate(trip, tripLogs, allLogs, trips)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Trip Review")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (completedTrips.isEmpty()) {
                EmptyStateView()
            } else {
                TripSelector(
                    trips = completedTrips,
                    selectedTrip = selectedTrip,
                    onTripSelected = { selectedTrip = it }
                )

                if (review != null) {
                    ReviewContent(review)
                } else if (selectedTrip == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a trip to generate a local review.", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripSelector(
    trips: List<FishingTrip>,
    selectedTrip: FishingTrip?,
    onTripSelected: (FishingTrip) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Completed Trip", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box {
            Card(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = selectedTrip?.name ?: "Choose a trip...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        selectedTrip?.let {
                            Text("${it.waterBody} · ${sdf.format(Date(it.startTime))}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                trips.forEach { trip ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(trip.name, fontWeight = FontWeight.Bold)
                                Text("${trip.waterBody} · ${sdf.format(Date(trip.startTime))}", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        onClick = {
                            onTripSelected(trip)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewContent(review: TripReview) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = review.confidenceLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Local Summary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                review.sections.forEach { section ->
                    Text(section.heading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = section.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "This review is generated locally from your logs. No AI or cloud processing was used.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun EmptyStateView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No completed trips with logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Trip reviews are available for completed fishing trips that have at least one catch or no-catch log.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
