package com.fishlog.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fishlog.app.data.FishSpecies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesPicker(
    value: String,
    onValueChange: (String) -> Unit,
    existingSpecies: List<String> = emptyList(),
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Combine built-in list with user's existing species, removing duplicates
    val allSuggestions = remember(existingSpecies) {
        (existingSpecies + FishSpecies.commonSpecies).distinct()
    }

    val filteredSuggestions = remember(value, allSuggestions) {
        if (value.isBlank()) {
            emptyList()
        } else {
            allSuggestions.filter { 
                it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true)
            }.take(12)
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text("Species") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            isError = isError,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (expanded && filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onValueChange(suggestion)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

