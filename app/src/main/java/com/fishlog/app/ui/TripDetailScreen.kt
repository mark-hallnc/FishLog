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
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.CatchLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: FishingTrip,
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onLogClick: (CatchLog) -> Unit
) {
    val catches by viewModel.allCatches.collectAsState()
    val tripLogs = remember(catches, trip.id) {
        catches.filter { it.tripId == trip.id }
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
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(trip.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (trip.waterBody.isNotBlank()) {
                        Text(trip.waterBody, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Started: ${formatTimestamp(trip.startTime)}", style = MaterialTheme.typography.bodyMedium)
                    trip.endTime?.let {
                        Text("Ended: ${formatTimestamp(it)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Duration: ${formatDuration(trip.startTime, it)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (trip.endTime == null) {
                var isEndingTrip by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Button(
                    onClick = {
                        if (isEndingTrip) return@Button
                        isEndingTrip = true
                        scope.launch {
                            try {
                                viewModel.endTrip(trip)
                                onBack() // Navigate back after success
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
                                if (trip.airTempF != null) StatItem("Air Temp", "${trip.airTempF}°F", Modifier.weight(1f))
                                if (trip.waterClarity.isNotBlank()) StatItem("Clarity", trip.waterClarity, Modifier.weight(1f))
                            }
                        }
                        if (trip.pressureTrend.isNotBlank()) {
                            StatItem("Pressure", trip.pressureTrend, Modifier.fillMaxWidth())
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

