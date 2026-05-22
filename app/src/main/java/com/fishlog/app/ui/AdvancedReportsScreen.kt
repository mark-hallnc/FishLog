package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.analytics.*
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.ui.components.ReportBarChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedReportsScreen(
    logs: List<CatchLog>,
    trips: List<FishingTrip>,
    filters: PatternEngineFilters,
    onBack: () -> Unit,
    onBucketClick: (ReportBucket, String) -> Unit
) {
    val reports = remember(logs, trips, filters) {
        AdvancedReportsEngine.buildReports(logs, trips, filters)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Reports") },
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
            // Filter Context
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Active Filters",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = buildString {
                            append(filters.species ?: "All Species")
                            append(" · ")
                            append(filters.waterBody ?: "All Water Bodies")
                            append(" · ")
                            append(filters.dateRange.getLabel())
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ReportBarChart(
                title = "Catch Rate by Bait",
                buckets = reports.baitBuckets,
                emptyMessage = "Not enough bait data yet.",
                onBucketClick = { onBucketClick(it, "Bait") }
            )

            ReportBarChart(
                title = "Catch Rate by Depth",
                buckets = reports.depthBuckets,
                emptyMessage = "Not enough depth data yet.",
                onBucketClick = { onBucketClick(it, "Depth") }
            )

            ReportBarChart(
                title = "Catch Rate by Temperature",
                buckets = reports.waterTempBuckets,
                emptyMessage = "Not enough temperature data yet.",
                onBucketClick = { onBucketClick(it, "Temperature") }
            )

            ReportBarChart(
                title = "Catch Rate by Time of Day",
                buckets = reports.timeOfDayBuckets,
                emptyMessage = "Not enough time data yet.",
                onBucketClick = { onBucketClick(it, "Time of Day") }
            )

            ReportBarChart(
                title = "Catch Rate by Month",
                buckets = reports.monthBuckets,
                emptyMessage = "Not enough monthly data yet.",
                onBucketClick = { onBucketClick(it, "Month") }
            )

            ReportBarChart(
                title = "Catch Rate by Moon Phase",
                buckets = reports.moonPhaseBuckets,
                emptyMessage = "Not enough moon phase data yet.",
                onBucketClick = { onBucketClick(it, "Moon Phase") }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
