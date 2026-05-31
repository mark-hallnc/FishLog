package com.fishlog.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.analytics.*
import com.fishlog.app.data.FishingTrip
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictiveSuggestionsScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onViewMatchingLogs: (List<Long>, String) -> Unit
) {
    val catches by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()

    var species by remember { mutableStateOf<String?>(null) }
    var waterBody by remember { mutableStateOf<String?>(null) }
    var timeOfDay by remember { mutableStateOf(SuggestionTimeOfDay.ANY) }

    val speciesOptions = remember(catches) {
        listOf("All Species") + catches.filter { it.logType == "CATCH" && it.species.isNotBlank() }
            .map { it.species }.distinct().sorted()
    }

    val waterBodyOptions = remember(trips) {
        listOf("All Water Bodies") + trips.map { it.waterBody.trim() }.filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }.sorted()
    }

    val inputs = remember(species, waterBody, timeOfDay) {
        PredictiveSuggestionInputs(
            species = if (species == "All Species") null else species,
            waterBody = if (waterBody == "All Water Bodies") null else waterBody,
            timeOfDay = timeOfDay
        )
    }

    val result = remember(catches, trips, inputs) {
        PredictiveSuggestionEngine.generate(catches, trips, inputs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suggested Setups") },
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
            Text(
                text = "Pattern-based suggestions from your saved history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Inputs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownFilter(
                        label = "Target Species",
                        selectedOption = species ?: "All Species",
                        options = speciesOptions,
                        onOptionSelected = { species = it }
                    )
                    DropdownFilter(
                        label = "Water Body",
                        selectedOption = waterBody ?: "All Water Bodies",
                        options = waterBodyOptions,
                        onOptionSelected = { waterBody = it }
                    )
                    DropdownFilter(
                        label = "Time of Day",
                        selectedOption = timeOfDay.name.lowercase().replaceFirstChar { it.uppercase() },
                        options = SuggestionTimeOfDay.entries.map { it.name.lowercase().replaceFirstChar { it.uppercase() } },
                        onOptionSelected = { timeOfDay = SuggestionTimeOfDay.valueOf(it.uppercase()) }
                    )
                }
            }

            if (result.notEnoughData) {
                NotEnoughDataView()
            } else {
                result.suggestions.forEach { suggestion ->
                    SuggestionCard(suggestion, onViewMatchingLogs)
                }
                
                Text(
                    text = "Suggestions are based only on your saved FishLog history. This is a starting point, not a guarantee.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: PredictiveSuggestion,
    onViewMatchingLogs: (List<Long>, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = suggestion.summary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SuggestionConfidenceChip(suggestion.confidenceLabel)
                Text(
                    text = "${(suggestion.catchRate * 100).toInt()}% Success · ${suggestion.observationCount} logs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "WHY:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            suggestion.why.forEach { reason ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("• ", style = MaterialTheme.typography.bodySmall)
                    Text(reason, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (suggestion.matchingLogIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { onViewMatchingLogs(suggestion.matchingLogIds, suggestion.title) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View historically productive logs")
                }
            }
        }
    }
}

@Composable
fun SuggestionConfidenceChip(label: String) {
    val color = when (label) {
        "Stronger pattern" -> Color(0xFF4CAF50)
        "Developing pattern" -> Color(0xFF2196F3)
        "Early pattern" -> Color(0xFFFFA000)
        else -> Color.Gray
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun NotEnoughDataView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QueryStats,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Not enough data yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Log more catches and no-catches for this species and water body to unlock Suggested Setups.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
