package com.fishlog.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fishlog.app.data.CatchLog
import com.fishlog.app.location.LocationService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoCatchFormScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit,
    editingLog: CatchLog? = null
) {
    var waterTemp by remember { mutableStateOf(editingLog?.waterTemp ?: "") }
    var depth by remember { mutableStateOf(editingLog?.depth ?: "") }
    var bait by remember { mutableStateOf(editingLog?.bait ?: "") }
    var notes by remember { mutableStateOf(editingLog?.notes ?: "") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    var saveLocation by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(locationService.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.any { it }
        if (hasPermission && editingLog == null) {
            saveLocation = true
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && editingLog == null) {
            saveLocation = true
        }
    }
    
    var showConfirmation by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingLog != null) "Edit No-Catch Log" else "Log No-Catch") },
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
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No-Catch log saved!",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (editingLog == null) {
                            Button(
                                onClick = {
                                    showConfirmation = false
                                    waterTemp = ""
                                    depth = ""
                                    bait = ""
                                    notes = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Log Another No-Catch")
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
                                text = if (editingLog != null) "Refresh GPS location" else "Save GPS location",
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
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            try {
                                var lat: Double? = editingLog?.latitude
                                var lon: Double? = editingLog?.longitude
                                
                                if (saveLocation && hasPermission) {
                                    val location = locationService.getCurrentLocation()
                                    if (location != null) {
                                        lat = location.latitude
                                        lon = location.longitude
                                    }
                                }

                                if (editingLog != null) {
                                    val updatedLog = editingLog.copy(
                                        waterTemp = waterTemp,
                                        depth = depth,
                                        bait = bait,
                                        notes = notes,
                                        latitude = lat,
                                        longitude = lon,
                                        waterTempF = waterTemp.toDoubleOrNull(),
                                        depthFeet = depth.toDoubleOrNull(),
                                        updatedAt = System.currentTimeMillis(),
                                        backupStatus = com.fishlog.app.data.BackupStatus.PENDING_BACKUP
                                    )
                                    viewModel.updateCatch(updatedLog)
                                } else {
                                    viewModel.saveNoCatch(
                                        waterTemp = waterTemp,
                                        depth = depth,
                                        bait = bait,
                                        notes = notes,
                                        latitude = lat,
                                        longitude = lon,
                                        waterTempF = waterTemp.toDoubleOrNull(),
                                        depthFeet = depth.toDoubleOrNull()
                                    )
                                }
                                showConfirmation = true
                            } catch (e: Exception) {
                                errorMessage = "Error saving log: ${e.message}"
                            } finally {
                                isSaving = false
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
                            if (editingLog != null) "Update No-Catch Log" else "Save No-Catch Log",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

