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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fishlog.app.location.LocationService
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
        allTrips.map { it.waterBody }.distinct()
    }
    
    var name by remember { mutableStateOf("") }
    var hasUserEditedName by remember { mutableStateOf(false) }
    var waterBody by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Generate default name once trips are available and if user hasn't edited
    LaunchedEffect(allTrips) {
        if (!hasUserEditedName) {
            name = generateDefaultTripName(allTrips.map { it.name })
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    var saveLocation by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(locationService.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
        if (hasPermission) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Switch(checked = saveLocation, onCheckedChange = { saveLocation = it })
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
                            viewModel.startTrip(name, waterBody, notes, lat, lon)
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

