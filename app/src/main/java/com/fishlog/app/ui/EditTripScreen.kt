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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.WeatherData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    trip: FishingTrip,
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onSave: (FishingTrip) -> Unit
) {
    val allTrips by viewModel.allTrips.collectAsState()
    val existingWaterBodies = remember(allTrips) {
        allTrips.map { it.waterBody }.distinct()
    }
    
    var name by remember { mutableStateOf(trip.name) }
    var waterBody by remember { mutableStateOf(trip.waterBody) }
    var notes by remember { mutableStateOf(trip.notes) }
    var isSaving by remember { mutableStateOf(false) }
    var fetchedWeatherData by remember { mutableStateOf<WeatherData?>(null) }

    var skyCondition by remember { mutableStateOf(trip.skyCondition) }
    var windCondition by remember { mutableStateOf(trip.windCondition) }
    var airTemp by remember { mutableStateOf(trip.airTempF?.toString() ?: "") }
    var waterClarity by remember { mutableStateOf(trip.waterClarity) }
    var pressureTrend by remember { mutableStateOf(trip.pressureTrend) }

    val skyOptions = listOf("Clear", "Partly Cloudy", "Cloudy", "Rain", "Storms", "Fog", "Other")
    val windOptions = listOf("Calm", "Light", "Moderate", "Strong", "Gusty")
    val clarityOptions = listOf("Clear", "Stained", "Muddy", "Murky", "Other")
    val pressureOptions = listOf("Rising", "Falling", "Steady", "Unknown")

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Trip") },
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Trip Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    WaterBodyPicker(
                        value = waterBody,
                        onValueChange = { waterBody = it },
                        existingWaterBodies = existingWaterBodies,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Conditions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownFilter(
                            label = "Sky",
                            selectedOption = skyCondition.ifBlank { "Select Sky" },
                            options = skyOptions,
                            onOptionSelected = { skyCondition = it },
                            modifier = Modifier.weight(1f)
                        )
                        DropdownFilter(
                            label = "Wind",
                            selectedOption = windCondition.ifBlank { "Select Wind" },
                            options = windOptions,
                            onOptionSelected = { windCondition = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = airTemp,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) airTemp = it },
                            label = { Text("Air Temp (°F)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        DropdownFilter(
                            label = "Clarity",
                            selectedOption = waterClarity.ifBlank { "Select Clarity" },
                            options = clarityOptions,
                            onOptionSelected = { waterClarity = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DropdownFilter(
                        label = "Pressure Trend",
                        selectedOption = pressureTrend.ifBlank { "Select Pressure Trend" },
                        options = pressureOptions,
                        onOptionSelected = { pressureTrend = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    var weatherMessage by remember { mutableStateOf<String?>(null) }
                    var isWeatherLoading by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            scope.launch {
                                isWeatherLoading = true
                                weatherMessage = "Fetching weather..."
                                if (trip.latitude != null && trip.longitude != null) {
                                    val result = viewModel.fetchWeather(trip.latitude, trip.longitude, trip.startTime)
                                    if (result.isSuccess) {
                                        val data = result.getOrNull()!!
                                        fetchedWeatherData = data
                                        airTemp = data.airTempF?.toString() ?: airTemp
                                        weatherMessage = "Weather auto-filled."
                                    } else {
                                        weatherMessage = result.exceptionOrNull()?.message ?: "Weather unavailable."
                                    }
                                } else {
                                    weatherMessage = "Trip location not saved."
                                }
                                isWeatherLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isWeatherLoading
                    ) {
                        if (isWeatherLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Auto-fill Weather")
                        }
                    }

                    weatherMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            val updatedTrip = trip.copy(
                                name = name,
                                waterBody = waterBody,
                                notes = notes,
                                skyCondition = skyCondition,
                                windCondition = windCondition,
                                airTempF = airTemp.toDoubleOrNull(),
                                waterClarity = waterClarity,
                                pressureTrend = pressureTrend,
                                updatedAt = System.currentTimeMillis(),
                                backupStatus = com.fishlog.app.data.BackupStatus.PENDING_BACKUP
                            )
                            viewModel.updateTrip(updatedTrip, fetchedWeatherData)
                            onSave(updatedTrip)
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saving...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
