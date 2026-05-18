package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.ui.DateRangeFilter
import com.fishlog.app.ui.DateFilterControls
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: FishLogViewModel,
    unitSystem: String = AppPreferences.UNITS_US,
    onBack: () -> Unit
) {
    val logs by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()

    var dateFilter by remember { mutableStateOf<DateRangeFilter>(DateRangeFilter.AllDates) }

    val filteredLogs = remember(logs, dateFilter) {
        logs.filter { dateFilter.matches(it.timestamp) }
    }

    val filteredTrips = remember(trips, dateFilter) {
        trips.filter { dateFilter.matches(it.startTime) }
    }

    val availableMonths = remember(logs) {
        logs.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            DateRangeFilter.Month(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        }.distinct().sortedByDescending { it.year * 12 + it.month }
    }

    val availableYears = remember(logs) {
        logs.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR)
        }.distinct().sortedDescending()
    }

    val isMetric = unitSystem == AppPreferences.UNITS_METRIC
    val tempSuffix = if (isMetric) "°C" else "°F"
    val depthSuffix = if (isMetric) "m" else "ft"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
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
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No fishing logs yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Add a catch to start seeing insights.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        } else {
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    DateFilterControls(
                        selectedFilter = dateFilter,
                        onFilterChange = { dateFilter = it },
                        availableMonths = availableMonths,
                        availableYears = availableYears,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (filteredLogs.isEmpty() && filteredTrips.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No logs match this date range.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    SummarySection(filteredLogs)
                    TopSpeciesSection(filteredLogs)
                    TopBaitsSection(filteredLogs)
                    MonthlyActivitySection(filteredLogs)
                    EnvironmentalSection(filteredLogs, tempSuffix, depthSuffix)
                    LocationCoverageSection(filteredLogs)
                    TopWaterBodiesSection(filteredTrips)
                    TripInsightsSection(filteredLogs, filteredTrips)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TopWaterBodiesSection(trips: List<FishingTrip>) {
    val topWaterBodies = trips
        .filter { it.waterBody.isNotBlank() }
        .map { it.waterBody.trim() }
        .groupBy { it.lowercase() }
        .map { group ->
            // Use the most frequent original casing for display
            val originalCasing = group.value.groupBy { it }.maxBy { it.value.size }.key
            originalCasing to group.value.size
        }
        .sortedByDescending { it.second }
        .take(5)

    InsightCard(title = "Top Water Bodies") {
        if (topWaterBodies.isEmpty()) {
            Text("Not enough data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            topWaterBodies.forEach { (waterBody, count) ->
                RankingRow(waterBody, count, trips.size)
            }
        }
    }
}

@Composable
fun TripInsightsSection(logs: List<CatchLog>, trips: List<FishingTrip>) {
    val totalTrips = trips.size
    val completedTrips = trips.filter { it.endTime != null }
    val avgCatches = if (completedTrips.isNotEmpty()) {
        val totalCatches = logs.count { log -> log.logType == "CATCH" && completedTrips.any { it.id == log.tripId } }
        totalCatches.toFloat() / completedTrips.size
    } else 0f

    InsightCard(title = "Trip Insights") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem("Total Trips", totalTrips.toString(), Modifier.weight(1f))
            StatItem("Avg Catch/Trip", String.format("%.1f", avgCatches), Modifier.weight(1f))
        }
        
        val tripsWithConditions = trips.count { 
            it.skyCondition.isNotBlank() || it.windCondition.isNotBlank() || it.airTempF != null 
        }
        if (tripsWithConditions > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "$tripsWithConditions trips have conditions recorded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun InsightCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SummarySection(logs: List<CatchLog>) {
    val totalCatches = logs.count { it.logType == "CATCH" }
    val totalNoCatches = logs.count { it.logType == "NO_CATCH" }
    val totalLogs = logs.size
    val catchRate = if (totalLogs > 0) (totalCatches.toFloat() / totalLogs * 100).toInt() else 0

    InsightCard(title = "Total Logs") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem("Catches", totalCatches.toString(), Modifier.weight(1f))
            StatItem("No-Catch", totalNoCatches.toString(), Modifier.weight(1f))
            StatItem("Success Rate", "$catchRate%", Modifier.weight(1f))
        }
    }
}

@Composable
fun TopSpeciesSection(logs: List<CatchLog>) {
    val topSpecies = logs
        .filter { it.logType == "CATCH" && it.species.isNotBlank() }
        .groupBy { it.species }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    InsightCard(title = "Top Species") {
        if (topSpecies.isEmpty()) {
            Text("Not enough data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            topSpecies.forEach { (species, count) ->
                RankingRow(species, count, logs.count { it.logType == "CATCH" })
            }
        }
    }
}

@Composable
fun TopBaitsSection(logs: List<CatchLog>) {
    val topBaits = logs
        .filter { it.logType == "CATCH" && it.bait.isNotBlank() }
        .groupBy { it.bait }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    InsightCard(title = "Top Baits & Lures") {
        if (topBaits.isEmpty()) {
            Text("Not enough data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            topBaits.forEach { (bait, count) ->
                RankingRow(bait, count, logs.count { it.logType == "CATCH" })
            }
        }
    }
}

@Composable
fun MonthlyActivitySection(logs: List<CatchLog>) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthlyData = logs
        .filter { it.logType == "CATCH" }
        .groupBy { 
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.timestamp
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.first }
        .take(6)

    InsightCard(title = "Catches by Month") {
        if (monthlyData.isEmpty()) {
            Text("Not enough data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            monthlyData.forEach { (timestamp, count) ->
                val monthName = monthFormat.format(Date(timestamp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(monthName, style = MaterialTheme.typography.bodyLarge)
                    Text(count.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun EnvironmentalSection(logs: List<CatchLog>, tempSuffix: String, depthSuffix: String) {
    val catches = logs.filter { it.logType == "CATCH" }
    
    val temps = catches.mapNotNull { it.waterTempF }
    val depths = catches.mapNotNull { it.depthFeet }

    InsightCard(title = "Conditions Range") {
        if (temps.isEmpty() && depths.isEmpty()) {
            Text("Not enough numeric data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            if (temps.isNotEmpty()) {
                Text("Water Temp Range", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Lowest", "${temps.minOrNull()}$tempSuffix", Modifier.weight(1f))
                    StatItem("Highest", "${temps.maxOrNull()}$tempSuffix", Modifier.weight(1f))
                    StatItem("Average", "${String.format("%.1f", temps.average())}$tempSuffix", Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (depths.isNotEmpty()) {
                Text("Depth Range", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Shallowest", "${depths.minOrNull()}$depthSuffix", Modifier.weight(1f))
                    StatItem("Deepest", "${depths.maxOrNull()}$depthSuffix", Modifier.weight(1f))
                    StatItem("Average", "${String.format("%.1f", depths.average())}$depthSuffix", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LocationCoverageSection(logs: List<CatchLog>) {
    val withLocation = logs.count { it.latitude != null && it.longitude != null }
    val total = logs.size
    val percentage = if (total > 0) (withLocation.toFloat() / total * 100).toInt() else 0

    InsightCard(title = "Location Coverage") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem("With GPS", withLocation.toString(), Modifier.weight(1f))
            StatItem("No GPS", (total - withLocation).toString(), Modifier.weight(1f))
            StatItem("Coverage", "$percentage%", Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RankingRow(label: String, count: Int, total: Int) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(count.toString(), fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.tertiary,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

