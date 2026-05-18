package com.fishlog.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilterControls(
    selectedFilter: DateRangeFilter,
    onFilterChange: (DateRangeFilter) -> Unit,
    availableMonths: List<DateRangeFilter.Month>,
    availableYears: List<Int>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pickingStartDate by remember { mutableStateOf(true) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedFilter.getLabel(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date Range") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text("All Dates") }, onClick = { onFilterChange(DateRangeFilter.AllDates); expanded = false })
                    DropdownMenuItem(text = { Text("Today") }, onClick = { onFilterChange(DateRangeFilter.Today); expanded = false })
                    DropdownMenuItem(text = { Text("Last 7 Days") }, onClick = { onFilterChange(DateRangeFilter.Last7Days); expanded = false })
                    DropdownMenuItem(text = { Text("Last 30 Days") }, onClick = { onFilterChange(DateRangeFilter.Last30Days); expanded = false })
                    DropdownMenuItem(text = { Text("This Year") }, onClick = { onFilterChange(DateRangeFilter.ThisYear); expanded = false })
                    DropdownMenuItem(text = { Text("By Month") }, onClick = { 
                        val lastMonth = availableMonths.firstOrNull() ?: DateRangeFilter.Month(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR))
                        onFilterChange(lastMonth)
                        expanded = false 
                    })
                    DropdownMenuItem(text = { Text("By Season") }, onClick = { 
                        onFilterChange(DateRangeFilter.Season(DateRangeFilter.SUMMER, Calendar.getInstance().get(Calendar.YEAR)))
                        expanded = false 
                    })
                    DropdownMenuItem(text = { Text("Custom Range") }, onClick = { 
                        onFilterChange(DateRangeFilter.Custom(null, null))
                        expanded = false 
                    })
                }
            }
            
            if (selectedFilter !is DateRangeFilter.AllDates) {
                IconButton(onClick = { onFilterChange(DateRangeFilter.AllDates) }) {
                    Icon(Icons.Default.Clear, contentDescription = "Reset Date Filter")
                }
            }
        }

        // Additional controls for specific filters
        when (selectedFilter) {
            is DateRangeFilter.Month -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownFilter(
                        label = "Select Month",
                        selectedOption = selectedFilter.getLabel(),
                        options = availableMonths.map { it.getLabel() },
                        onOptionSelected = { label ->
                            availableMonths.find { it.getLabel() == label }?.let { onFilterChange(it) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            is DateRangeFilter.Season -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownFilter(
                        label = "Season",
                        selectedOption = selectedFilter.season,
                        options = listOf(DateRangeFilter.SPRING, DateRangeFilter.SUMMER, DateRangeFilter.FALL, DateRangeFilter.WINTER),
                        onOptionSelected = { onFilterChange(selectedFilter.copy(season = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    DropdownFilter(
                        label = "Year",
                        selectedOption = selectedFilter.year?.toString() ?: "All Years",
                        options = listOf("All Years") + availableYears.map { it.toString() },
                        onOptionSelected = { 
                            onFilterChange(selectedFilter.copy(year = it.toIntOrNull()))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            is DateRangeFilter.Custom -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    OutlinedButton(
                        onClick = { pickingStartDate = true; showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedFilter.startDate?.let { sdf.format(Date(it)) } ?: "Start Date")
                    }
                    OutlinedButton(
                        onClick = { pickingStartDate = false; showDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(selectedFilter.endDate?.let { sdf.format(Date(it)) } ?: "End Date")
                    }
                }
            }
            else -> {}
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                    if (selectedFilter is DateRangeFilter.Custom) {
                        if (pickingStartDate) {
                            onFilterChange(selectedFilter.copy(startDate = selectedDate))
                        } else {
                            onFilterChange(selectedFilter.copy(endDate = selectedDate))
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (selectedFilter is DateRangeFilter.Custom && selectedFilter.startDate != null && selectedFilter.endDate != null) {
        if (selectedFilter.startDate > selectedFilter.endDate) {
            Text(
                text = "Start date must be before end date",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
