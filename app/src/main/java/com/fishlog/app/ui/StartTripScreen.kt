package com.fishlog.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.location.LocationService
import com.fishlog.app.data.WeatherData
import com.fishlog.app.util.WaterBodyNameUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTripScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    onTripStarted: () -> Unit
) {
    val allTrips by viewModel.allTrips.collectAsState()
    val existingWaterBodies = remember(allTrips) {
        WaterBodyNameUtils.getUniqueWaterBodies(allTrips)
    }
    
    var name by remember { mutableStateOf("") }
    var hasUserEditedName by remember { mutableStateOf(false) }
    var waterBody by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var fetchedWeatherData by remember { mutableStateOf<WeatherData?>(null) }

    var skyCondition by remember { mutableStateOf("") }
    var windCondition by remember { mutableStateOf("") }
    var airTemp by remember { mutableStateOf("") }
    var waterClarity by remember { mutableStateOf("") }
    var pressureTrend by remember { mutableStateOf("") }

    val skyOptions = listOf("Clear", "Partly Cloudy", "Cloudy", "Rain", "Storms", "Fog", "Other")
    val windOptions = listOf("Calm", "Light", "Moderate", "Strong", "Gusty")
    val clarityOptions = listOf("Clear", "Stained", "Muddy", "Murky", "Other")
    val pressureOptions = listOf("Rising", "Falling", "Steady", "Unknown")

    // Generate default name once trips are available and if user hasn't edited
    LaunchedEffect(allTrips) {
        if (!hasUserEditedName) {
            name = generateDefaultTripName(allTrips.map { it.name })
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    
    var saveLocation by remember { mutableStateOf(locationService.hasLocationPermission()) }
    var hasUserChangedSaveLocation by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(locationService.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
        if (hasPermission && !hasUserChangedSaveLocation) {
            saveLocation = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start New Trip") },
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
            // 1. Trip Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Trip Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            hasUserEditedName = true
                        },
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
                }
            }

            // 2. Weather
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Weather", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    var weatherMessage by remember { mutableStateOf<String?>(null) }
                    var isWeatherLoading by remember { mutableStateOf(false) }

                    Button(
                        onClick = {
                            scope.launch {
                                isWeatherLoading = true
                                weatherMessage = "Fetching current weather..."
                                
                                val loc = locationService.getCurrentLocation()
                                if (loc != null) {
                                    val result = viewModel.fetchWeather(loc.latitude, loc.longitude)
                                    if (result.isSuccess) {
                                        val data = result.getOrNull()!!
                                        fetchedWeatherData = data
                                        airTemp = data.airTempF?.let { "%.1f".format(it) } ?: airTemp
                                        
                                        val summary = data.weatherSummary
                                        if (skyOptions.contains(summary)) {
                                            skyCondition = summary
                                        } else if (summary.contains("Cloudy")) {
                                            skyCondition = "Cloudy"
                                        } else if (summary.contains("Rain") || summary.contains("Showers")) {
                                            skyCondition = "Rain"
                                        } else if (summary.contains("Storm")) {
                                            skyCondition = "Storms"
                                        } else {
                                            skyCondition = "Other"
                                        }

                                        val windDesc = viewModel.mapWindSpeedToCondition(data.windSpeedMph)
                                        if (windOptions.contains(windDesc)) {
                                            windCondition = windDesc
                                        }

                                        data.pressureTrend?.let {
                                            if (pressureOptions.contains(it)) {
                                                pressureTrend = it
                                            }
                                        }

                                        weatherMessage = "Weather filled."
                                    } else {
                                        weatherMessage = result.exceptionOrNull()?.message ?: "Weather response could not be read."
                                    }
                                } else {
                                    weatherMessage = "Enable GPS and wait for location to auto-fill weather."
                                }
                                isWeatherLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isWeatherLoading
                    ) {
                        if (isWeatherLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching...")
                        } else {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-fill Weather")
                        }
                    }

                    weatherMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    
                    Text(
                        "Auto-fill uses your current location to fetch current conditions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

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
                            label = "Pressure Trend",
                            selectedOption = pressureTrend.ifBlank { "Select Pressure Trend" },
                            options = pressureOptions,
                            onOptionSelected = { pressureTrend = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Water Conditions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Water Conditions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    
                    DropdownFilter(
                        label = "Water Clarity",
                        selectedOption = waterClarity.ifBlank { "Select Clarity" },
                        options = clarityOptions,
                        onOptionSelected = { waterClarity = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Notes
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save Start Location", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(if (hasPermission) "Use current coordinates" else "Permissions needed", style = MaterialTheme.typography.bodySmall)
                    }
                    if (hasPermission) {
                        Switch(
                            checked = saveLocation,
                            onCheckedChange = { 
                                saveLocation = it
                                hasUserChangedSaveLocation = true
                            }
                        )
                    } else {
                        Button(onClick = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }, shape = RoundedCornerShape(8.dp)) {
                            Text("Enable")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            var lat: Double? = null
                            var lon: Double? = null
                            if (saveLocation && hasPermission) {
                                val loc = locationService.getCurrentLocation()
                                lat = loc?.latitude
                                lon = loc?.longitude
                            }
                            viewModel.startTrip(
                                name = name,
                                waterBody = waterBody,
                                notes = notes,
                                latitude = lat,
                                longitude = lon,
                                skyCondition = skyCondition,
                                windCondition = windCondition,
                                airTempF = airTemp.toDoubleOrNull(),
                                waterClarity = waterClarity,
                                pressureTrend = pressureTrend,
                                weatherData = fetchedWeatherData
                            )
                            onTripStarted()
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
                    Text("Starting...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Start Trip", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

fun generateDefaultTripName(existingNames: List<String>): String {
    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    val baseName = dateFormat.format(Date())
    
    if (!existingNames.contains(baseName)) {
        return baseName
    }
    
    var counter = 1
    while (existingNames.contains("$baseName-$counter")) {
        counter++
    }
    return "$baseName-$counter"
}

