package com.fishlog.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.data.NearbyWaterBody
import com.fishlog.app.util.WaterBodyNameUtils

data class WaterBodySuggestion(
    val name: String,
    val sourceLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterBodyPicker(
    value: String,
    onValueChange: (String) -> Unit,
    existingWaterBodies: List<String> = emptyList(),
    nearbyWaterBodies: List<NearbyWaterBody> = emptyList(),
    onRefreshNearby: (() -> Unit)? = null,
    isNearbyLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Combine suggestions
    val combinedSuggestions = remember(existingWaterBodies, nearbyWaterBodies) {
        val list = mutableListOf<WaterBodySuggestion>()
        
        // 1. History
        existingWaterBodies.forEach { 
            list.add(WaterBodySuggestion(it, "From your trips")) 
        }
        
        // 2. Nearby
        nearbyWaterBodies.forEach { nearby ->
            if (existingWaterBodies.none { it.equals(nearby.name, ignoreCase = true) }) {
                list.add(WaterBodySuggestion(nearby.name, "Nearby · ${nearby.type}"))
            }
        }
        
        list
    }

    val filteredSuggestions = remember(value, combinedSuggestions) {
        if (value.isBlank()) {
            combinedSuggestions.take(15)
        } else {
            combinedSuggestions.filter { 
                it.name.contains(value, ignoreCase = true) && !it.name.equals(value, ignoreCase = true)
            }.take(15)
        }
    }

    val bestSuggestion = remember(value, existingWaterBodies) {
        WaterBodyNameUtils.findBestSuggestion(value, existingWaterBodies)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    expanded = true
                },
                label = { Text("Water Body (Lake, River, etc.)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (onRefreshNearby != null) {
                        if (isNearbyLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = onRefreshNearby) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Nearby")
                            }
                        }
                    }
                }
            )

            if (expanded && filteredSuggestions.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(suggestion.name)
                                    Text(suggestion.sourceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            },
                            onClick = {
                                onValueChange(suggestion.name)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        if (value.isBlank() && nearbyWaterBodies.isEmpty() && onRefreshNearby != null && !isNearbyLoading) {
            Text(
                "Nearby suggestions appear when location and internet are available.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        if (bestSuggestion != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Did you mean:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(bestSuggestion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onValueChange(bestSuggestion) },
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use This", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
