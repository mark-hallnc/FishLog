package com.fishlog.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.analytics.PatternInsight
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternDetailScreen(
    insight: PatternInsight,
    allLogs: List<CatchLog>,
    allTrips: List<FishingTrip>,
    onBack: () -> Unit,
    onLogClick: (CatchLog) -> Unit,
    onTripClick: (FishingTrip) -> Unit
) {
    val tripById = remember(allTrips) { allTrips.associateBy { it.id } }
    val matchingLogs = remember(insight, allLogs) {
        val idSet = insight.matchingLogIds.toSet()
        allLogs.filter { it.id in idSet }.sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pattern Details") },
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeaderCard(insight)
            }

            item {
                Text(
                    text = "Logs in this Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (matchingLogs.isEmpty()) {
                item {
                    Text("No matching logs found.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(matchingLogs) { log ->
                    val trip = tripById[log.tripId]
                    EvidenceRow(
                        log = log,
                        trip = trip,
                        onLogClick = { onLogClick(log) },
                        onTripClick = { trip?.let { onTripClick(it) } }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeaderCard(insight: PatternInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(insight.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${insight.subtitle} · ${insight.category}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InsightSmallStat("Catch Rate", "${(insight.catchRate * 100).toInt()}%")
                InsightSmallStat("Catches", insight.catchCount.toString())
                InsightSmallStat("No-Catches", insight.noCatchCount.toString())
                InsightSmallStat("Total", insight.observationCount.toString())
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = insight.confidenceLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "These are the local logs and trips that contributed to this pattern calculation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun EvidenceRow(
    log: CatchLog,
    trip: FishingTrip?,
    onLogClick: () -> Unit,
    onTripClick: () -> Unit
) {
    val isCatch = log.logType == "CATCH"
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(log.timestamp))

    Card(
        onClick = onLogClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isCatch) Color(0xFF4CAF50).copy(alpha = 0.1f) else Color(0xFFFF5722).copy(alpha = 0.1f),
                        contentColor = if (isCatch) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isCatch) "CATCH" else "NO-CATCH",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (isCatch) log.species else "No Catch Attempt",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                if (log.bait.isNotBlank()) {
                    val lengthValue = log.lengthInches ?: log.length.toDoubleOrNull()
                    val weightValue = log.weightLbs ?: log.weight.toDoubleOrNull()
                    val measurements = listOfNotNull(
                        if (log.bait.isNotBlank()) "Bait: ${log.bait}" else null,
                        if (isCatch && lengthValue != null) "Len: ${FormatUtils.formatLength(lengthValue)}" else null,
                        if (isCatch && weightValue != null) "Wt: ${FormatUtils.formatWeight(weightValue)}" else null
                    ).joinToString(" · ")
                    
                    Text(
                        text = measurements,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (trip != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable(onClick = onTripClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = trip.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun InsightSmallStat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
