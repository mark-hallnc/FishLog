package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.AppPreferences
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSummaryScreen(
    trip: FishingTrip,
    viewModel: FishLogViewModel,
    unitSystem: String = AppPreferences.UNITS_US,
    onDone: () -> Unit,
    onViewDetails: () -> Unit
) {
    val catches by viewModel.allCatches.collectAsState()
    val tripLogs = remember(catches, trip.id) {
        catches.filter { it.tripId == trip.id }
    }

    val isMetric = unitSystem == AppPreferences.UNITS_METRIC
    val tempSuffix = if (isMetric) "°C" else "°F"

    val catchCount = tripLogs.count { it.logType == "CATCH" }
    val noCatchCount = tripLogs.count { it.logType == "NO_CATCH" }
    
    val speciesCaught = remember(tripLogs) {
        tripLogs.filter { it.logType == "CATCH" && it.species.isNotBlank() }
            .map { it.species }
            .distinct()
            .sorted()
    }

    val topBait = remember(tripLogs) {
        tripLogs.filter { it.logType == "CATCH" && it.bait.isNotBlank() }
            .map { it.bait }
            .groupBy { it }
            .maxByOrNull { it.value.size }?.key
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Summary") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Great trip!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(trip.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (trip.waterBody.isNotBlank()) {
                        Text(trip.waterBody, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    trip.endTime?.let { end ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duration", style = MaterialTheme.typography.labelSmall)
                                Text(formatDuration(trip.startTime, end), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Logs", style = MaterialTheme.typography.labelSmall)
                                Text(tripLogs.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            InsightCard(title = "Results") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Catches", catchCount.toString(), Modifier.weight(1f))
                    StatItem("No-Catches", noCatchCount.toString(), Modifier.weight(1f))
                }
                
                if (catchCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Species Caught:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = speciesCaught.joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No catches logged this trip.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            InsightCard(title = "Top Pattern") {
                if (topBait != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Most Effective Bait", style = MaterialTheme.typography.labelSmall)
                            Text(topBait, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("No bait/lure recorded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            val hasConditions = trip.skyCondition.isNotBlank() || trip.windCondition.isNotBlank() || 
                trip.airTempF != null || trip.waterClarity.isNotBlank() || trip.pressureTrend.isNotBlank() ||
                trip.weatherAutoFilled || trip.weatherSummary.isNotBlank()

            if (hasConditions) {
                InsightCard(title = "Conditions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val row1 = listOfNotNull(
                            trip.weatherSummary.ifBlank { trip.skyCondition.ifBlank { null } },
                            trip.airTempF?.let { "${it.toInt()}$tempSuffix" }
                        )
                        if (row1.isNotEmpty()) {
                            Text(row1.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        val row2 = listOfNotNull(
                            if (trip.windSpeedMph != null) "Wind ${trip.windSpeedMph.toInt()} mph" else trip.windCondition.ifBlank { null },
                            if (trip.waterClarity.isNotBlank()) "Water Clarity: ${trip.waterClarity}" else null
                        )
                        if (row2.isNotEmpty()) {
                            Text(row2.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        }
                        
                        if (trip.pressureTrend.isNotBlank()) {
                            Text("Pressure: ${trip.pressureTrend}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                Text(
                    "Conditions not recorded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            if (trip.moonPhaseName.isNotBlank()) {
                InsightCard(title = "Moon") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MoonPhaseIcon(
                            illuminationPercent = trip.moonIlluminationPercent,
                            waxing = trip.moonWaxing,
                            phaseName = trip.moonPhaseName,
                            size = 32.dp
                        )
                        Text(
                            text = "${trip.moonPhaseName} · ${trip.moonIlluminationPercent?.toInt() ?: 0}% illuminated",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (trip.notes.isNotBlank()) {
                InsightCard(title = "Notes") {
                    Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Trip Details")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatDuration(start: Long, end: Long): String {
    val diff = end - start
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff / (1000 * 60)) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
