package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.AppPreferences
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: FishingTrip,
    viewModel: FishLogViewModel,
    unitSystem: String = AppPreferences.UNITS_US,
    onBack: () -> Unit,
    onLogClick: (CatchLog) -> Unit,
    onLogCatch: () -> Unit = {},
    onLogNoCatch: () -> Unit = {},
    onTripEnded: (FishingTrip) -> Unit = {},
    onEditTrip: (FishingTrip) -> Unit = {},
    onTripDeleted: () -> Unit = {}
) {
    val catches by viewModel.allCatches.collectAsState()
    val tripLogs = remember(catches, trip.id) {
        catches.filter { it.tripId == trip.id }
    }

    val isMetric = unitSystem == AppPreferences.UNITS_METRIC
    val tempSuffix = if (isMetric) "°C" else "°F"

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Trip?") },
            text = { Text("This will delete the trip session but keep the catch and no-catch logs. Those logs will no longer be attached to this trip.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTrip(trip)
                        onTripDeleted()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Trip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Details") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (trip.waterBody.isNotBlank()) {
                        Text(
                            text = trip.waterBody,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Started: ${formatTimestamp(trip.startTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    trip.endTime?.let {
                        Text(
                            text = "Ended: ${formatTimestamp(it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Duration: ${formatDuration(trip.startTime, it)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Edit and Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onEditTrip(trip) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Trip")
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Trip")
                }
            }

            if (trip.endTime == null) {
                var isEndingTrip by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLogCatch,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Catch")
                    }
                    Button(
                        onClick = onLogNoCatch,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No-Catch")
                    }
                }

                Button(
                    onClick = {
                        if (isEndingTrip) return@Button
                        isEndingTrip = true
                        scope.launch {
                            try {
                                viewModel.endTrip(trip)
                                onTripEnded(trip.copy(endTime = System.currentTimeMillis()))
                            } catch (e: Exception) {
                                isEndingTrip = false
                                // Optional: Show error
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isEndingTrip
                ) {
                    if (isEndingTrip) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ending Trip...", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("End Trip", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            InsightCard(title = "Trip Summary") {
                val catchCount = tripLogs.count { it.logType == "CATCH" }
                val noCatchCount = tripLogs.count { it.logType == "NO_CATCH" }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Catches", catchCount.toString(), Modifier.weight(1f))
                    StatItem("No-Catches", noCatchCount.toString(), Modifier.weight(1f))
                }
            }

            if (trip.skyCondition.isNotBlank() || trip.windCondition.isNotBlank() || 
                trip.airTempF != null || trip.waterClarity.isNotBlank() || trip.pressureTrend.isNotBlank()) {
                InsightCard(title = "Conditions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (trip.skyCondition.isNotBlank() || trip.windCondition.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (trip.skyCondition.isNotBlank()) StatItem("Sky", trip.skyCondition, Modifier.weight(1f))
                                if (trip.windCondition.isNotBlank()) StatItem("Wind", trip.windCondition, Modifier.weight(1f))
                            }
                        }
                        if (trip.airTempF != null || trip.waterClarity.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                if (trip.airTempF != null) StatItem("Air Temp", "${trip.airTempF}$tempSuffix", Modifier.weight(1f))
                                if (trip.waterClarity.isNotBlank()) StatItem("Clarity", trip.waterClarity, Modifier.weight(1f))
                            }
                        }
                        if (trip.pressureTrend.isNotBlank()) {
                            StatItem("Pressure", trip.pressureTrend, Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (trip.moonPhaseName.isNotBlank()) {
                InsightCard(title = "Moon") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("Phase", trip.moonPhaseName, Modifier.weight(1f))
                            StatItem("Illumination", "${trip.moonIlluminationPercent?.toInt() ?: 0}%", Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem("Moon Age", "${String.format("%.1f", trip.moonAgeDays ?: 0.0)} days", Modifier.weight(1f))
                            StatItem("Trend", if (trip.moonWaxing == true) "Waxing" else "Waning", Modifier.weight(1f))
                        }
                    }
                }
            }

            if (trip.weatherAutoFilled || trip.weatherSummary.isNotBlank()) {
                InsightCard(title = "Weather Details") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (trip.weatherAutoFilled) {
                            Text("Auto-filled conditions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        
                        if (trip.weatherSummary.isNotBlank()) {
                            Text(trip.weatherSummary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (trip.feelsLikeF != null) StatItem("Feels Like", "${trip.feelsLikeF.toInt()}°F", Modifier.weight(1f))
                            if (trip.humidityPercent != null) StatItem("Humidity", "${trip.humidityPercent.toInt()}%", Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (trip.windSpeedMph != null) StatItem("Wind Speed", "${trip.windSpeedMph.toInt()} mph", Modifier.weight(1f))
                            if (trip.windDirectionDegrees != null) StatItem("Wind Dir", "${trip.windDirectionDegrees.toInt()}°", Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (trip.windGustMph != null) StatItem("Wind Gusts", "${trip.windGustMph.toInt()} mph", Modifier.weight(1f))
                            if (trip.barometricPressureHpa != null) StatItem("Pressure", "${trip.barometricPressureHpa.toInt()} hPa", Modifier.weight(1f))
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (trip.cloudCoverPercent != null) StatItem("Cloud Cover", "${trip.cloudCoverPercent.toInt()}%", Modifier.weight(1f))
                            if (trip.precipitationIn != null) StatItem("Rain/Snow", "${trip.precipitationIn} in", Modifier.weight(1f))
                        }
                    }
                }
            }

            if (trip.notes.isNotBlank()) {
                InsightCard(title = "Notes") {
                    Text(trip.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (tripLogs.isNotEmpty()) {
                Text("Logs in this Trip", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                tripLogs.forEach { log ->
                    CatchItem(catch = log, onClick = { onLogClick(log) })
                }
            } else {
                Text("No logs for this trip yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatDuration(start: Long, end: Long): String {
    val diff = end - start
    val hours = diff / (1000 * 60 * 60)
    val minutes = (diff / (1000 * 60)) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

