package com.markhall.fishlog.ui

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
import com.markhall.fishlog.data.CatchLog
import com.markhall.fishlog.data.PhotoStorageHelper
import com.markhall.fishlog.location.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchFormScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    editingCatch: CatchLog? = null
) {
    var species by remember { mutableStateOf(editingCatch?.species ?: "") }
    
    val catches by viewModel.allCatches.collectAsState()
    val existingSpecies = remember(catches) {
        catches.map { it.species }.distinct().sorted()
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

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = length,
                                onValueChange = { length = it },
                                label = { Text("Length (in)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Weight (lbs)") },
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
                                IconButton(
                                    onClick = { 
                                        photoStorageHelper.deletePhoto(photoUri)
                                        photoUri = null 
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White)
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
                            OutlinedTextField(
                                value = waterTemp,
                                onValueChange = { waterTemp = it },
                                label = { Text("Water Temp (°F)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = depth,
                                onValueChange = { depth = it },
                                label = { Text("Depth (ft)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        OutlinedTextField(
                            value = bait,
                            onValueChange = { bait = it },
                            label = { Text("Bait or Lure") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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
                                            backupStatus = com.markhall.fishlog.data.BackupStatus.PENDING_BACKUP
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
