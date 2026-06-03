package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.analytics.PatternEngine
import com.fishlog.app.analytics.PatternEngineFilters
import com.fishlog.app.analytics.PatternEngineResult
import com.fishlog.app.analytics.PatternInsight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAnalyticsScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onInsightClick: (PatternInsight) -> Unit,
    onViewReports: (PatternEngineFilters) -> Unit,
    onViewTripReview: () -> Unit,
    onViewPredictiveSuggestions: () -> Unit
) {
    val catches by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()

    var filters by remember { mutableStateOf(PatternEngineFilters()) }

    val speciesOptions = remember(catches) {
        listOf("All Species") + catches.filter { it.logType == "CATCH" && it.species.isNotBlank() }
            .map { it.species }.distinct().sorted()
    }

    val waterBodyOptions = remember(trips) {
        listOf("All Water Bodies") + trips.map { it.waterBody.trim() }.filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }.sorted()
    }

    val availableMonths = remember(catches) {
        catches.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            DateRangeFilter.Month(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
        }.distinct().sortedByDescending { it.year * 12 + it.month }
    }

    val availableYears = remember(catches) {
        catches.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR)
        }.distinct().sortedDescending()
    }

    val patternResult = remember(catches, trips, filters) {
        PatternEngine.analyze(catches, trips, filters)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Advanced Analytics")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filters Card
            FilterCard(
                filters = filters,
                onFiltersChange = { filters = it },
                speciesOptions = speciesOptions,
                waterBodyOptions = waterBodyOptions,
                availableMonths = availableMonths,
                availableYears = availableYears
            )

            // Pattern Engine Section
            PatternEngineSection(
                result = patternResult,
                filters = filters,
                onReset = { filters = PatternEngineFilters() },
                onInsightClick = onInsightClick
            )

            // Roadmap Section
            Text(
                text = "Planned Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            AnalyticsActionCard(
                title = "Advanced Reports",
                description = "Visual charts for catch rate by bait, depth, temperature, time, month, and moon.",
                icon = Icons.Default.Assessment,
                onClick = { onViewReports(filters) }
            )

            AnalyticsActionCard(
                title = "Trip Review",
                description = "Local summaries of completed trips based on your catches, no-catches, weather, moon, and patterns.",
                icon = Icons.Default.AutoAwesome,
                onClick = { onViewTripReview() }
            )

            AnalyticsActionCard(
                title = "Suggested Setups",
                description = "Pattern-based suggestions from your saved catches and no-catches.",
                icon = Icons.Default.Lightbulb,
                onClick = onViewPredictiveSuggestions
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FilterCard(
    filters: PatternEngineFilters,
    onFiltersChange: (PatternEngineFilters) -> Unit,
    speciesOptions: List<String>,
    waterBodyOptions: List<String>,
    availableMonths: List<DateRangeFilter.Month>,
    availableYears: List<Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = { onFiltersChange(PatternEngineFilters()) }) {
                    Text("Reset")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownFilter(
                    label = "Species",
                    selectedOption = filters.species ?: "All Species",
                    options = speciesOptions,
                    onOptionSelected = { onFiltersChange(filters.copy(species = if (it == "All Species") null else it)) },
                    modifier = Modifier.weight(1f)
                )
                DropdownFilter(
                    label = "Water Body",
                    selectedOption = filters.waterBody ?: "All Water Bodies",
                    options = waterBodyOptions,
                    onOptionSelected = { onFiltersChange(filters.copy(waterBody = if (it == "All Water Bodies") null else it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            DateFilterControls(
                selectedFilter = filters.dateRange,
                onFilterChange = { onFiltersChange(filters.copy(dateRange = it)) },
                availableMonths = availableMonths,
                availableYears = availableYears,
                modifier = Modifier.fillMaxWidth()
            )

            if (filters.species != null) {
                Text(
                    text = "Species filters use matching catch logs. No-catch logs without species may still be included as trip context.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PatternEngineSection(
    result: PatternEngineResult,
    filters: PatternEngineFilters,
    onReset: () -> Unit = {},
    onInsightClick: (PatternInsight) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pattern Engine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            
            val filterContext = buildString {
                append(filters.species ?: "All Species")
                append(" · ")
                append(filters.waterBody ?: "All Water Bodies")
                append(" · ")
                append(filters.dateRange.getLabel())
            }

            Text(
                text = filterContext,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Local analysis of your catch and no-catch observations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (result.totalObservations == 0) {
                NoMatchingLogsView(onReset)
            } else if (result.totalObservations < 5) {
                NotEnoughDataView(result)
            } else {
                PatternResultsView(result, onInsightClick)
            }
        }
    }
}

@Composable
fun NoMatchingLogsView(onReset: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No matching logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "Try changing species, water body, or date range filters.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReset) {
            Text("Clear Filters")
        }
    }
}

@Composable
fun NotEnoughDataView(result: PatternEngineResult) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Not enough data yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Log at least 5 observations to unlock useful patterns. No-catch logs help refine your catch rates!",
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmallStat("Catches", result.totalCatchLogs.toString())
                SmallStat("No-Catches", result.totalNoCatchLogs.toString())
            }
        }
    }
}

@Composable
fun PatternResultsView(result: PatternEngineResult, onInsightClick: (PatternInsight) -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stats Overview
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallStat("Observations", result.totalObservations.toString())
            SmallStat("Total Catches", result.totalCatchLogs.toString())
            SmallStat("No-Catches", result.totalNoCatchLogs.toString())
        }

        // Top Pattern Highlight
        result.topPattern?.let { top ->
            Surface(
                onClick = { onInsightClick(top) },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Active Pattern", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(top.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${top.subtitle} · ${top.confidenceLabel}", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LinearProgressIndicator(
                        progress = { top.catchRate.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        "${(top.catchRate * 100).toInt()}% Catch Rate (${top.catchCount} fish / ${top.observationCount} logs)",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Category Breakdowns
        InsightList("Best Baits", result.bestBaitsBySpecies, onInsightClick)
        InsightList("Best Times", result.bestTimesOfDay, onInsightClick)
        InsightList("Best Depths", result.bestDepthRanges, onInsightClick)
        InsightList("Water Temp", result.bestWaterTempRanges, onInsightClick)
        InsightList("Water Bodies", result.bestWaterBodies, onInsightClick)
        InsightList("Moon Phase", result.moonPhasePatterns, onInsightClick)
    }
}

@Composable
fun InsightList(title: String, insights: List<PatternInsight>, onInsightClick: (PatternInsight) -> Unit = {}) {
    if (insights.isEmpty()) return
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        insights.forEach { insight ->
            PatternInsightRow(insight, onClick = { onInsightClick(insight) })
        }
    }
}

@Composable
fun PatternInsightRow(insight: PatternInsight, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(insight.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(insight.catchRate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { insight.catchRate.toFloat() },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            "${insight.catchCount} catches / ${insight.observationCount} logs · ${insight.confidenceLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun SmallStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AnalyticsActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "View Reports",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun AnalyticsPlaceholderCard(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Coming later",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
