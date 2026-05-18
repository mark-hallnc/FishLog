package com.fishlog.app.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.PhotoStorageHelper
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.ui.RecentValueChips
import com.fishlog.app.location.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchFormScreen(
    viewModel: FishLogViewModel,
    unitSystem: String = AppPreferences.UNITS_US,
    onBack: () -> Unit,
    editingCatch: CatchLog? = null
) {
    val isMetric = unitSystem == AppPreferences.UNITS_METRIC
    val lengthLabel = if (isMetric) "Length (cm)" else "Length (in)"
    val weightLabel = if (isMetric) "Weight (kg)" else "Weight (lbs)"
    val tempLabel = if (isMetric) "Water Temp (°C)" else "Water Temp (°F)"
    val depthLabel = if (isMetric) "Depth (m)" else "Depth (ft)"
    val tempSuffix = if (isMetric) "°C" else "°F"
    val depthSuffix = if (isMetric) " m" else " ft"

    var species by remember { mutableStateOf(editingCatch?.species ?: "") }
    
    val catches by viewModel.allCatches.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val existingSpecies = remember(catches) {
        catches.map { it.species }.distinct().sorted()
    }

    val recentSpecies = remember(catches) {
        catches.filter { it.logType == "CATCH" && it.species.isNotBlank() }
            .map { it.species }
            .distinct()
            .take(6)
    }

    val recentBaits = remember(catches) {
        catches.filter { it.bait.isNotBlank() }
            .map { it.bait.trim() }
            .distinctBy { it.lowercase() }
            .take(6)
    }

    val recentDepths = remember(catches) {
        catches.mapNotNull { it.depth.trim().ifBlank { it.depthFeet?.toString() } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(5)
    }

    val recentTemps = remember(catches) {
        catches.mapNotNull { it.waterTemp.trim().ifBlank { it.waterTempF?.toString() } }
            .filter { it.isNotBlank() }
            .distinct()
            .take(5)
    }

    var length by remember { mutableStateOf(editingCatch?.length ?: "") }
    var weight by remember { mutableStateOf(editingCatch?.weight ?: "") }
    var waterTemp by remember { mutableStateOf(editingCatch?.waterTemp ?: "") }
    var depth by remember { mutableStateOf(editingCatch?.depth ?: "") }
    var bait by remember { mutableStateOf(editingCatch?.bait ?: "") }
    var notes by remember { mutableStateOf(editingCatch?.notes ?: "") }
    var photoUri by remember { mutableStateOf(editingCatch?.photoUri) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val photoStorageHelper = remember { PhotoStorageHelper(context) }
    var saveLocation by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(locationService.hasLocationPermission()) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            photoUri = tempCameraUri.toString()
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoUri = photoStorageHelper.savePhoto(uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
        if (hasPermission && editingCatch == null) {
            saveLocation = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && editingCatch == null) {
            saveLocation = true
        }
    }
    
    var showConfirmation by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingCatch != null) "Edit Catch" else "Log Catch") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeTrip != null && editingCatch == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                text = "Logging to active trip",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (activeTrip!!.waterBody.isNotBlank()) 
                                    "${activeTrip!!.name} · ${activeTrip!!.waterBody}" 
                                    else activeTrip!!.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else if (editingCatch == null) {
                Text(
                    text = "Not attached to a trip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (showConfirmation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Catch saved successfully!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (editingCatch == null) {
                            Button(
                                onClick = {
                                    showConfirmation = false
                                    species = ""
                                    length = ""
                                    weight = ""
                                    waterTemp = ""
                                    depth = ""
                                    bait = ""
                                    notes = ""
                                    photoUri = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Log Another Catch")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Return")
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Catch Info", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        SpeciesPicker(
                            value = species,
                            onValueChange = { species = it },
                            existingSpecies = existingSpecies,
                            isError = errorMessage != null && species.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (errorMessage != null && species.isBlank()) {
                            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        RecentValueChips(
                            values = recentSpecies,
                            onValueSelected = { species = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = length,
                                onValueChange = { length = it },
                                label = { Text(lengthLabel) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text(weightLabel) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Photos", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        
                        if (photoUri != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(photoUri),
                                    contentDescription = "Catch Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { 
                                            // Optional: If we want to delete immediately, but better on Save or just clear reference
                                            // photoStorageHelper.deletePhoto(photoUri)
                                            photoUri = null 
                                        },
                                        modifier = Modifier
                                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    tempCameraUri = photoStorageHelper.getNewPhotoUri()
                                    takePictureLauncher.launch(tempCameraUri!!)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera")
                            }
                            OutlinedButton(
                                onClick = {
                                    pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gallery")
                            }
                        }
                        
                        if (photoUri != null) {
                            TextButton(
                                onClick = { photoUri = null },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove Photo")
                            }
                        }
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
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = waterTemp,
                                    onValueChange = { waterTemp = it },
                                    label = { Text(tempLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                RecentValueChips(
                                    values = recentTemps.map { "$it$tempSuffix" },
                                    onValueSelected = { waterTemp = it.removeSuffix(tempSuffix) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = depth,
                                    onValueChange = { depth = it },
                                    label = { Text(depthLabel) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                RecentValueChips(
                                    values = recentDepths.map { "$it$depthSuffix" },
                                    onValueSelected = { depth = it.removeSuffix(depthSuffix) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        Column {
                            OutlinedTextField(
                                value = bait,
                                onValueChange = { bait = it },
                                label = { Text("Bait or Lure") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            RecentValueChips(
                                values = recentBaits,
                                onValueSelected = { bait = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (editingCatch != null) "Refresh GPS location" else "Save GPS location",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (hasPermission) "Use device coordinates" else "Permissions needed",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (hasPermission) {
                            Switch(
                                checked = saveLocation,
                                onCheckedChange = { saveLocation = it }
                            )
                        } else {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enable")
                            }
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        if (species.isBlank()) {
                            errorMessage = "Species is required"
                        } else {
                            errorMessage = null
                            isSaving = true
                            scope.launch {
                                try {
                                    var lat: Double? = editingCatch?.latitude
                                    var lon: Double? = editingCatch?.longitude
                                    
                                    if (saveLocation && hasPermission) {
                                        val location = locationService.getCurrentLocation()
                                        if (location != null) {
                                            lat = location.latitude
                                            lon = location.longitude
                                        }
                                    }

                                    if (editingCatch != null) {
                                        // Delete old photo if it was removed or replaced
                                        if (editingCatch.photoUri != photoUri) {
                                            photoStorageHelper.deletePhoto(editingCatch.photoUri)
                                        }

                                        val updatedCatch = editingCatch.copy(
                                            species = species,
                                            length = length,
                                            weight = weight,
                                            waterTemp = waterTemp,
                                            depth = depth,
                                            bait = bait,
                                            notes = notes,
                                            latitude = lat,
                                            longitude = lon,
                                            lengthInches = length.toDoubleOrNull(),
                                            weightLbs = weight.toDoubleOrNull(),
                                            waterTempF = waterTemp.toDoubleOrNull(),
                                            depthFeet = depth.toDoubleOrNull(),
                                            photoUri = photoUri,
                                            updatedAt = System.currentTimeMillis(),
                                            backupStatus = com.fishlog.app.data.BackupStatus.PENDING_BACKUP
                                        )
                                        viewModel.updateCatch(updatedCatch)
                                    } else {
                                        viewModel.saveCatch(
                                            species = species,
                                            length = length,
                                            weight = weight,
                                            waterTemp = waterTemp,
                                            depth = depth,
                                            bait = bait,
                                            notes = notes,
                                            latitude = lat,
                                            longitude = lon,
                                            lengthInches = length.toDoubleOrNull(),
                                            weightLbs = weight.toDoubleOrNull(),
                                            waterTempF = waterTemp.toDoubleOrNull(),
                                            depthFeet = depth.toDoubleOrNull(),
                                            photoUri = photoUri
                                        )
                                    }
                                    showConfirmation = true
                                } catch (e: Exception) {
                                    errorMessage = "Error saving catch: ${e.message}"
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
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
                        Text(
                            if (editingCatch != null) "Update Catch" else "Save Catch",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

