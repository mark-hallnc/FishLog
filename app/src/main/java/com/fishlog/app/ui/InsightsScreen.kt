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
import com.fishlog.app.data.*
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
                    BestWaterBodiesSection(filteredLogs, trips)
                    BestSpeciesByWaterBodySection(filteredLogs, trips)
                    BestBaitBySpeciesSection(filteredLogs)
                    BestBaitByTempSection(filteredLogs)
                    CatchRateByTripSection(filteredLogs, filteredTrips)
                    CatchRateByDepthSection(filteredLogs)
                    CatchRateByTimeSection(filteredLogs)
                    BestMonthsSection(filteredLogs)
                    MostProductiveTripsSection(filteredLogs, filteredTrips)
                    EnvironmentalSection(filteredLogs, tempSuffix, depthSuffix)
                    LocationCoverageSection(filteredLogs)
                    TripInsightsSection(filteredLogs, filteredTrips)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SummarySection(logs: List<CatchLog>) {
    val totalCatches = logs.count { it.logType == "CATCH" }
    val totalNoCatches = logs.count { it.logType == "NO_CATCH" }
    val totalLogs = logs.size
    val catchRate = InsightsCalculator.calculateCatchRate(totalCatches, totalNoCatches)

    InsightCard(title = "Total Logs") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatItem("Catches", totalCatches.toString(), Modifier.weight(1f))
            StatItem("No-Catch", totalNoCatches.toString(), Modifier.weight(1f))
            StatItem("Success Rate", "$catchRate%", Modifier.weight(1f))
        }
    }
}

@Composable
fun BestWaterBodiesSection(logs: List<CatchLog>, allTrips: List<FishingTrip>) {
    val data = remember(logs, allTrips) {
        logs.filter { it.tripId != null }
            .groupBy { log ->
                val trip = allTrips.find { it.id == log.tripId }
                trip?.waterBody?.let { InsightsCalculator.normalizeWaterBody(it) } ?: ""
            }
            .filter { it.key.isNotBlank() }
            .map { (waterBody, logList) ->
                val catches = logList.count { it.logType == "CATCH" }
                val noCatches = logList.count { it.logType == "NO_CATCH" }
                Triple(waterBody, catches, noCatches)
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    InsightCard(
        title = "Best Water Bodies",
        caption = "Ranked by successful catches from logs attached to trips."
    ) {
        if (data.isEmpty()) {
            Text("Not enough water body data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (name, catches, noCatches) ->
                val rate = InsightsCalculator.calculateCatchRate(catches, noCatches)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("$catches catches", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$noCatches no-catch · $rate% catch rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun BestSpeciesByWaterBodySection(logs: List<CatchLog>, allTrips: List<FishingTrip>) {
    val data = remember(logs, allTrips) {
        logs.filter { it.logType == "CATCH" && it.tripId != null }
            .groupBy { log ->
                val trip = allTrips.find { it.id == log.tripId }
                trip?.waterBody?.let { InsightsCalculator.normalizeWaterBody(it) } ?: ""
            }
            .filter { it.key.isNotBlank() }
            .mapValues { (_, logList) ->
                logList.groupBy { it.species }
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }
            }
            .mapNotNull { if (it.value != null) it.key to it.value!! else null }
            .sortedByDescending { it.second.second }
            .take(5)
    }

    InsightCard(
        title = "Best Species by Water Body",
        caption = "The most frequent successful catch per location."
    ) {
        if (data.isEmpty()) {
            Text("Not enough species and water body data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (waterBody, topSpeciesData) ->
                val (species, count) = topSpeciesData
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(waterBody, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "Top species: $species — $count catches",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun BestBaitBySpeciesSection(logs: List<CatchLog>) {
    val data = remember(logs) {
        logs.filter { it.logType == "CATCH" && it.species.isNotBlank() && it.bait.isNotBlank() }
            .groupBy { it.species }
            .mapValues { (_, logList) ->
                logList.groupBy { it.bait }
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }
            }
            .mapNotNull { if (it.value != null) it.key to it.value!! else null }
            .sortedByDescending { it.second.second }
            .take(5)
    }

    InsightCard(
        title = "Best Bait by Species",
        caption = "Only successful catches are counted."
    ) {
        if (data.isEmpty()) {
            Text("Not enough bait and species data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (species, topBaitData) ->
                val (bait, count) = topBaitData
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(species, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "Best bait: $bait — $count catches",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun BestBaitByTempSection(logs: List<CatchLog>) {
    val data = remember(logs) {
        logs.filter { it.logType == "CATCH" && it.waterTempF != null && it.bait.isNotBlank() }
            .groupBy { InsightsCalculator.bucketWaterTemp(it.waterTempF)!! }
            .mapValues { (_, logList) ->
                logList.groupBy { it.bait }
                    .maxByOrNull { it.value.size }
                    ?.let { it.key to it.value.size }
            }
            .mapNotNull { if (it.value != null) it.key to it.value!! else null }
            .sortedBy { it.first } // Temperature ascending
    }

    InsightCard(
        title = "Best Bait by Water Temp",
        caption = "Most successful lure for each temperature range."
    ) {
        if (data.isEmpty()) {
            Text("Not enough water temperature and bait data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (tempRange, topBaitData) ->
                val (bait, count) = topBaitData
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(tempRange, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "Best bait: $bait — $count catches",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun CatchRateByTripSection(logs: List<CatchLog>, trips: List<FishingTrip>) {
    val data = remember(logs, trips) {
        trips.mapNotNull { trip ->
            val tripLogs = logs.filter { it.tripId == trip.id }
            if (tripLogs.isEmpty()) return@mapNotNull null
            
            val catches = tripLogs.count { it.logType == "CATCH" }
            val noCatches = tripLogs.count { it.logType == "NO_CATCH" }
            val rate = InsightsCalculator.calculateCatchRate(catches, noCatches)
            
            val subtitle = "${trip.name}${if (trip.waterBody.isNotBlank()) " · ${trip.waterBody}" else ""}"
            Quadruple(subtitle, catches, noCatches, rate)
        }
        .sortedWith(compareByDescending<Quadruple<String, Int, Int, Int>> { it.fourth }.thenByDescending { it.second })
        .take(5)
    }

    InsightCard(
        title = "Catch Rate by Trip",
        caption = "Percentage of logs that were successful catches for each trip."
    ) {
        if (data.isEmpty()) {
            Text("Not enough trip data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (name, catches, noCatches, rate) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("$rate%", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$catches catches · $noCatches no-catch",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun CatchRateByDepthSection(logs: List<CatchLog>) {
    val data = remember(logs) {
        logs.mapNotNull { log ->
            InsightsCalculator.bucketDepth(log.depthFeet)?.let { it to log.logType }
        }
        .groupBy { it.first }
        .map { (bucket, types) ->
            val catches = types.count { it.second == "CATCH" }
            val total = types.size
            val rate = if (total > 0) (catches.toFloat() / total * 100).toInt() else 0
            Triple(bucket, catches, rate)
        }
        .sortedBy { it.first }
    }

    InsightCard(
        title = "Catch Rate by Depth",
        caption = "Comparison of success across depth ranges."
    ) {
        if (data.isEmpty()) {
            Text("Not enough depth data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (bucket, catches, rate) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(bucket, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("$rate%", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$catches successful catches in this range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun CatchRateByTimeSection(logs: List<CatchLog>) {
    val data = remember(logs) {
        logs.groupBy { InsightsCalculator.bucketHour(it.timestamp) }
            .map { (bucket, logList) ->
                val catches = logList.count { it.logType == "CATCH" }
                val total = logList.size
                val rate = if (total > 0) (catches.toFloat() / total * 100).toInt() else 0
                Triple(bucket, catches, rate)
            }
            .sortedBy { InsightsCalculator.getSortOrderForTime(it.first) }
    }

    InsightCard(
        title = "Catch Rate by Time of Day",
        caption = "Identifying peak feeding times from your local logs."
    ) {
        if (data.isEmpty()) {
            Text("Not enough time-of-day data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (bucket, catches, rate) ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(bucket, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("$rate%", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$catches catches recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun BestMonthsSection(logs: List<CatchLog>) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthOnlyFormat = SimpleDateFormat("MMMM", Locale.getDefault())
    
    val successfulLogs = remember(logs) { logs.filter { it.logType == "CATCH" } }
    
    val monthYearData = remember(successfulLogs) {
        successfulLogs
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
            .map { (timestamp, logList) ->
                monthFormat.format(Date(timestamp)) to logList.size
            }
            .sortedByDescending { it.second }
            .take(6)
    }

    val bestMonthOverall = remember(successfulLogs) {
        successfulLogs
            .groupBy { 
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.MONTH)
            }
            .maxByOrNull { it.value.size }
            ?.let { (monthIndex, logList) ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, monthIndex)
                monthOnlyFormat.format(cal.time) to logList.size
            }
    }

    InsightCard(
        title = "Best Months",
        caption = "Ranked by successful catch volume."
    ) {
        if (monthYearData.isEmpty()) {
            Text("Not enough monthly data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            bestMonthOverall?.let { (name, count) ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Best month overall: $name — $count catches",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            monthYearData.forEach { (monthName, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(monthName, style = MaterialTheme.typography.bodyLarge)
                    Text("$count catches", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun MostProductiveTripsSection(logs: List<CatchLog>, trips: List<FishingTrip>) {
    val data = remember(logs, trips) {
        trips.mapNotNull { trip ->
            val tripLogs = logs.filter { it.tripId == trip.id }
            val catches = tripLogs.count { it.logType == "CATCH" }
            if (catches == 0) return@mapNotNull null
            
            val noCatches = tripLogs.count { it.logType == "NO_CATCH" }
            
            var catchPerHour: String? = null
            if (trip.endTime != null) {
                val durationHours = (trip.endTime - trip.startTime).toFloat() / (1000 * 60 * 60)
                if (durationHours > 0.1f) {
                    catchPerHour = String.format("%.1f", catches / durationHours)
                }
            }
            
            val info = "${trip.name}${if (trip.waterBody.isNotBlank()) " · ${trip.waterBody}" else ""}"
            Triple(info, catches, Pair(noCatches, catchPerHour))
        }
        .sortedByDescending { it.second }
        .take(5)
    }

    InsightCard(
        title = "Most Productive Trips",
        caption = "Sessions with the highest volume of successful catches."
    ) {
        if (data.isEmpty()) {
            Text("Not enough trip catch data yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            data.forEach { (info, catches, extras) ->
                val (noCatches, perHour) = extras
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(info, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("$catches catches", fontWeight = FontWeight.Bold)
                    }
                    val details = listOfNotNull(
                        "$noCatches no-catch",
                        perHour?.let { "$it catches/hour" }
                    ).joinToString(" · ")
                    Text(
                        details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
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
fun InsightCard(
    title: String, 
    caption: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
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
                color = MaterialTheme.colorScheme.primary
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
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
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.tertiary,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
