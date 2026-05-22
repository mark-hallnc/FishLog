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
import com.fishlog.app.util.FormatUtils
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

    val isActive = trip.endTime == null
    val forecast = viewModel.activeTripForecast

    LaunchedEffect(trip.id, isActive, trip.latitude, trip.longitude) {
        if (isActive) {
            viewModel.loadActiveTripForecastIfNeeded(trip)
        }
    }

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

            if (isActive && forecast != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Today's Forecast",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = buildString {
                                append(forecast.condition)
                                if (forecast.highTempF != null && forecast.lowTempF != null) {
                                    append(" · ${FormatUtils.formatWholeNumber(forecast.highTempF)}°/${FormatUtils.formatWholeNumber(forecast.lowTempF)}°")
                                }
                                if (forecast.windSpeedMph != null) {
                                    append(" · Wind ${FormatUtils.formatWholeNumber(forecast.windSpeedMph)} mph")
                                    val dir = getWindDirection(forecast.windDirectionDegrees)
                                    if (dir.isNotBlank()) append(" $dir")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
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

            val hasConditions = trip.skyCondition.isNotBlank() || trip.windCondition.isNotBlank() || 
                    trip.airTempF != null || trip.waterClarity.isNotBlank() || trip.pressureTrend.isNotBlank() ||
                    trip.weatherAutoFilled || trip.weatherSummary.isNotBlank()

            if (hasConditions) {
                InsightCard(title = "Conditions") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 1. Condition
                        val conditionValue = buildString {
                            if (trip.weatherSummary.isNotBlank()) {
                                append(trip.weatherSummary)
                                if (trip.skyCondition.isNotBlank() && !trip.skyCondition.equals(trip.weatherSummary, ignoreCase = true)) {
                                    append(" · noted: ${trip.skyCondition}")
                                }
                            } else if (trip.skyCondition.isNotBlank()) {
                                append(trip.skyCondition)
                            }
                        }
                        if (conditionValue.isNotBlank()) {
                            ConditionDetailItem("Condition", conditionValue)
                        }

                        // 2. Air Temp
                        val tempValue = buildString {
                            if (trip.airTempF != null) {
                                append("${FormatUtils.formatWholeNumber(trip.airTempF)}°$tempSuffix")
                                if (trip.feelsLikeF != null) {
                                    append(" · feels like ${FormatUtils.formatWholeNumber(trip.feelsLikeF)}°$tempSuffix")
                                }
                            } else if (trip.feelsLikeF != null) {
                                append("Feels like ${FormatUtils.formatWholeNumber(trip.feelsLikeF)}°$tempSuffix")
                            }
                        }
                        if (tempValue.isNotBlank()) {
                            ConditionDetailItem("Air Temp", tempValue)
                        }

                        // 3. Wind
                        val windValue = buildString {
                            if (trip.windCondition.isNotBlank()) {
                                append(trip.windCondition)
                            }
                            if (trip.windSpeedMph != null) {
                                if (this.isNotEmpty()) append(" · ")
                                append("${FormatUtils.formatWholeNumber(trip.windSpeedMph)} mph")
                                val dir = getWindDirection(trip.windDirectionDegrees)
                                if (dir.isNotBlank()) append(" $dir")
                                if (trip.windGustMph != null && trip.windGustMph > (trip.windSpeedMph ?: 0.0) + 2) {
                                    append(" · gusts ${FormatUtils.formatWholeNumber(trip.windGustMph)} mph")
                                }
                            }
                        }
                        if (windValue.isNotBlank()) {
                            ConditionDetailItem("Wind", windValue)
                        }

                        // 4. Pressure
                        val pressureValue = buildString {
                            if (trip.barometricPressureHpa != null) {
                                append("${FormatUtils.formatWholeNumber(trip.barometricPressureHpa)} hPa")
                            }
                            if (trip.pressureTrend.isNotBlank()) {
                                if (this.isNotEmpty()) append(" ")
                                append(getPressureTrendIcon(trip.pressureTrend))
                                append(" ${trip.pressureTrend}")
                            }
                        }
                        if (pressureValue.isNotBlank()) {
                            ConditionDetailItem("Pressure", pressureValue)
                        }

                        // 5. Humidity, Cloud Cover, Precipitation
                        if (trip.humidityPercent != null) {
                            ConditionDetailItem("Humidity", "${FormatUtils.formatWholeNumber(trip.humidityPercent)}%")
                        }
                        if (trip.cloudCoverPercent != null) {
                            ConditionDetailItem("Cloud Cover", "${FormatUtils.formatWholeNumber(trip.cloudCoverPercent)}%")
                        }
                        if (trip.precipitationIn != null && trip.precipitationIn > 0) {
                            ConditionDetailItem("Precipitation", "${FormatUtils.formatDecimal(trip.precipitationIn)} in")
                        }

                        // 6. Water Clarity
                        if (trip.waterClarity.isNotBlank()) {
                            ConditionDetailItem("Water Clarity", trip.waterClarity)
                        }

                        if (trip.weatherAutoFilled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Auto-filled weather",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (trip.moonPhaseName.isNotBlank()) {
                InsightCard(title = "Moon") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MoonPhaseIcon(
                            illuminationPercent = trip.moonIlluminationPercent,
                            waxing = trip.moonWaxing,
                            phaseName = trip.moonPhaseName,
                            size = 56.dp
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem("Phase", trip.moonPhaseName, Modifier.weight(1f))
                                StatItem("Illumination", "${FormatUtils.formatWholeNumber(trip.moonIlluminationPercent)}%", Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem("Moon Age", "${FormatUtils.formatDecimal(trip.moonAgeDays)} days", Modifier.weight(1f))
                                StatItem("Trend", if (trip.moonWaxing == true) "Waxing" else "Waning", Modifier.weight(1f))
                            }
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
                    CatchItem(catch = log, onClick = { onLogClick(log) }, unitSystem = unitSystem)
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

private fun getWindDirection(degrees: Double?): String {
    if (degrees == null) return ""
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
    return directions[((degrees % 360) / 45).toInt()]
}

@Composable
private fun ConditionDetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private fun getPressureTrendIcon(trend: String): String {
    return when {
        trend.contains("Rising", ignoreCase = true) || trend.contains("Increasing", ignoreCase = true) || trend.contains("Up", ignoreCase = true) -> "↑"
        trend.contains("Falling", ignoreCase = true) || trend.contains("Decreasing", ignoreCase = true) || trend.contains("Down", ignoreCase = true) -> "↓"
        trend.contains("Steady", ignoreCase = true) || trend.contains("Stable", ignoreCase = true) -> "→"
        else -> ""
    }
}

