package com.fishlog.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.data.*
import com.fishlog.app.billing.FeatureGate
import com.fishlog.app.billing.PaidFeature
import com.fishlog.app.util.FormatUtils
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FishLogViewModel,
    appearanceMode: String,
    unitSystem: String,
    mapCenterMode: String,
    mapDefaultLat: Double?,
    mapDefaultLon: Double?,
    mapStyle: String,
    homePhotoSlideshowEnabled: Boolean,
    activeTripReminderEnabled: Boolean,
    activeTripReminderDelay: Int,
    onAppearanceModeChange: (String) -> Unit,
    onUnitSystemChange: (String) -> Unit,
    onMapCenterModeChange: (String) -> Unit,
    onClearDefaultMapLocation: () -> Unit,
    onChooseDefaultMapLocation: () -> Unit,
    onViewWelcomeGuide: () -> Unit,
    onResetWelcomeScreen: () -> Unit,
    onMapStyleChange: (String) -> Unit,
    onHomePhotoSlideshowEnabledChange: (Boolean) -> Unit,
    onActiveTripReminderChange: (Boolean, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val catches by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val pendingBackupCount = remember(catches) {
        catches.count { it.backupStatus == BackupStatus.PENDING_BACKUP }
    }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }

    var showDeleteAllStep1 by remember { mutableStateOf(false) }
    var showDeleteAllStep2 by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    if (showDeleteAllStep1) {
        AlertDialog(
            onDismissRequest = { showDeleteAllStep1 = false },
            title = { Text("Delete all trips and logs?") },
            text = { Text("This will permanently delete all trips, catches, and no-catch logs from this device. Export or cloud backup your data first if you may want it later. Your settings and account will stay the same.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllStep1 = false
                    showDeleteAllStep2 = true
                    deleteConfirmText = ""
                }) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllStep1 = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAllStep2) {
        AlertDialog(
            onDismissRequest = { showDeleteAllStep2 = false },
            title = { Text("Are you absolutely sure?") },
            text = {
                Column {
                    Text("This cannot be undone unless you restore from a backup. To continue, type DELETE.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        placeholder = { Text("Type DELETE") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllStep2 = false
                        viewModel.deleteAllTripsAndLogs { result ->
                            statusMessage = result
                            isError = result.contains("Could not", ignoreCase = true)
                        }
                    },
                    enabled = deleteConfirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllStep2 = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from Cloud?") },
            text = { Text("FishLog will download your cloud backup and restore it on this device. Your current local data will be merged with the cloud data. Duplicates will be skipped. Consider exporting a local backup first if you want a safety copy.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    viewModel.restoreFromCloud()
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val csvContent = CsvExportHelper.convertToCsv(catches, trips)
                context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(csvContent)
                    }
                }
                statusMessage = "CSV exported successfully!"
                isError = false
            } catch (e: Exception) {
                statusMessage = "CSV export failed: ${e.message}"
                isError = true
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonContent = JsonBackupHelper.createBackup(catches, trips)
                context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(jsonContent)
                    }
                }
                statusMessage = "Backup exported successfully!"
                isError = false
            } catch (e: Exception) {
                statusMessage = "Backup export failed: ${e.message}"
                isError = true
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            showImportConfirm = uri
        }
    }

    if (showImportConfirm != null) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = null },
            title = { Text("Import Backup") },
            text = { Text("Importing a backup will add records from the backup file to this device. Duplicates will be skipped.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = showImportConfirm!!
                    showImportConfirm = null
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val jsonString = inputStream.bufferedReader().use { it.readText() }
                            val backup = JsonBackupHelper.parseBackup(jsonString)
                            viewModel.importBackup(backup)
                            statusMessage = "Backup imported successfully!"
                            isError = false
                        }
                    } catch (e: Exception) {
                        statusMessage = "Import failed: ${e.message}"
                        isError = true
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showMapCenterDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showMapStyleDialog by remember { mutableStateOf(false) }

    if (showMapStyleDialog) {
        AlertDialog(
            onDismissRequest = { showMapStyleDialog = false },
            title = { Text("Default Map Style") },
            text = {
                Column {
                    AppearanceOption(
                        label = "Standard",
                        selected = mapStyle == AppPreferences.MAP_STYLE_STANDARD,
                        onClick = {
                            onMapStyleChange(AppPreferences.MAP_STYLE_STANDARD)
                            showMapStyleDialog = false
                        }
                    )
                    AppearanceOption(
                        label = "Topographic",
                        selected = mapStyle == AppPreferences.MAP_STYLE_TOPOGRAPHIC,
                        onClick = {
                            onMapStyleChange(AppPreferences.MAP_STYLE_TOPOGRAPHIC)
                            showMapStyleDialog = false
                        }
                    )
                    AppearanceOption(
                        label = "Satellite",
                        selected = mapStyle == AppPreferences.MAP_STYLE_SATELLITE,
                        onClick = {
                            onMapStyleChange(AppPreferences.MAP_STYLE_SATELLITE)
                            showMapStyleDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapStyleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showReminderDialog) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar("Notification permission denied. Reminders will not be shown.")
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Active Trip Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "FishLog can remind you if a trip is still active after a while. This helps keep trip duration and analytics accurate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val options = listOf(
                        "Off" to 0,
                        "After 4 hours" to 4,
                        "After 6 hours" to 6,
                        "After 8 hours" to 8,
                        "After 12 hours" to 12
                    )

                    options.forEach { (label, hours) ->
                        val selected = if (hours == 0) !activeTripReminderEnabled else activeTripReminderEnabled && activeTripReminderDelay == hours
                        AppearanceOption(
                            label = label,
                            selected = selected,
                            onClick = {
                                if (hours == 0) {
                                    onActiveTripReminderChange(false, activeTripReminderDelay)
                                } else {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onActiveTripReminderChange(true, hours)
                                }
                                showReminderDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showMapCenterDialog) {
        AlertDialog(
            onDismissRequest = { showMapCenterDialog = false },
            title = { Text("Default Map Center") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose where the map opens by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    AppearanceOption(
                        label = "Current Location",
                        selected = mapCenterMode == AppPreferences.MAP_CENTER_CURRENT,
                        onClick = {
                            onMapCenterModeChange(AppPreferences.MAP_CENTER_CURRENT)
                        }
                    )
                    Text(
                        "Open the map near me when GPS is available.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AppearanceOption(
                        label = "Saved Location",
                        selected = mapCenterMode == AppPreferences.MAP_CENTER_SAVED,
                        onClick = {
                            if (mapDefaultLat != null && mapDefaultLon != null) {
                                onMapCenterModeChange(AppPreferences.MAP_CENTER_SAVED)
                            } else {
                                onChooseDefaultMapLocation()
                                showMapCenterDialog = false
                            }
                        }
                    )
                    Text(
                        "Open the map to a spot I choose.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 36.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onChooseDefaultMapLocation()
                            showMapCenterDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose on Map")
                    }

                    if (mapDefaultLat != null) {
                        OutlinedButton(
                            onClick = {
                                onClearDefaultMapLocation()
                                onMapCenterModeChange(AppPreferences.MAP_CENTER_CURRENT)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Saved Location")
                        }
                    }

                    Text(
                        "Saved locations work offline, but map detail depends on cached tiles.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapCenterDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showAppearanceDialog) {
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            title = { Text("Select Appearance") },
            text = {
                Column {
                    AppearanceOption(
                        label = "Follow system",
                        selected = appearanceMode == AppPreferences.MODE_FOLLOW_SYSTEM,
                        onClick = {
                            onAppearanceModeChange(AppPreferences.MODE_FOLLOW_SYSTEM)
                            showAppearanceDialog = false
                        }
                    )
                    AppearanceOption(
                        label = "Light",
                        selected = appearanceMode == AppPreferences.MODE_LIGHT,
                        onClick = {
                            onAppearanceModeChange(AppPreferences.MODE_LIGHT)
                            showAppearanceDialog = false
                        }
                    )
                    AppearanceOption(
                        label = "Dark",
                        selected = appearanceMode == AppPreferences.MODE_DARK,
                        onClick = {
                            onAppearanceModeChange(AppPreferences.MODE_DARK)
                            showAppearanceDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUnitDialog) {
        AlertDialog(
            onDismissRequest = { showUnitDialog = false },
            title = { Text("Select Units") },
            text = {
                Column {
                    AppearanceOption(
                        label = "US units",
                        selected = unitSystem == AppPreferences.UNITS_US,
                        onClick = {
                            onUnitSystemChange(AppPreferences.UNITS_US)
                            showUnitDialog = false
                        }
                    )
                    AppearanceOption(
                        label = "Metric",
                        selected = unitSystem == AppPreferences.UNITS_METRIC,
                        onClick = {
                            onUnitSystemChange(AppPreferences.UNITS_METRIC)
                            showUnitDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showUnitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            // App Preferences
            SettingsSection(title = "App Preferences") {
                val appearanceLabel = when (appearanceMode) {
                    AppPreferences.MODE_LIGHT -> "Light"
                    AppPreferences.MODE_DARK -> "Dark"
                    else -> "Follow system"
                }
                val appearanceHelper = when (appearanceMode) {
                    AppPreferences.MODE_LIGHT -> "FishLog always uses light mode."
                    AppPreferences.MODE_DARK -> "FishLog always uses dark mode."
                    else -> "Light/dark mode follows your Android setting."
                }
                SettingRow(
                    icon = Icons.Default.BrightnessMedium,
                    title = "Appearance",
                    subtitle = appearanceLabel,
                    helperText = appearanceHelper,
                    onClick = { showAppearanceDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                val unitLabel = if (unitSystem == AppPreferences.UNITS_METRIC) "Metric" else "US units"
                val unitHelper = if (unitSystem == AppPreferences.UNITS_METRIC) 
                    "°C, meters, centimeters, kilograms" 
                    else "°F, feet, inches, pounds"
                
                SettingRow(
                    icon = Icons.Default.Straighten,
                    title = "Units",
                    subtitle = unitLabel,
                    helperText = unitHelper,
                    onClick = { showUnitDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                val mapCenterLabel = if (mapCenterMode == AppPreferences.MAP_CENTER_SAVED && mapDefaultLat != null) {
                    "Saved location"
                } else {
                    "Current location"
                }
                
                val mapCenterHelper = if (mapCenterMode == AppPreferences.MAP_CENTER_SAVED && mapDefaultLat != null) {
                    "${String.format(java.util.Locale.US, "%.4f", mapDefaultLat)}, ${String.format(java.util.Locale.US, "%.4f", mapDefaultLon)}"
                } else if (mapCenterMode == AppPreferences.MAP_CENTER_SAVED) {
                    "Saved location not set"
                } else {
                    "Map opens near your GPS location when available."
                }

                SettingRow(
                    icon = Icons.Default.MyLocation,
                    title = "Default Map Center",
                    subtitle = mapCenterLabel,
                    helperText = mapCenterHelper,
                    onClick = { showMapCenterDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                val mapStyleLabel = when (mapStyle) {
                    AppPreferences.MAP_STYLE_TOPOGRAPHIC -> "Topographic"
                    AppPreferences.MAP_STYLE_SATELLITE -> "Satellite"
                    else -> "Standard"
                }
                SettingRow(
                    icon = Icons.Default.Layers,
                    title = "Map Style",
                    subtitle = mapStyleLabel,
                    helperText = "Choose your preferred map view.",
                    onClick = { showMapStyleDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                SettingRow(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Home Photo Slideshow",
                    subtitle = if (homePhotoSlideshowEnabled) "On" else "Off",
                    helperText = "Show your logged catch photos in the Home screen header.",
                    onClick = { onHomePhotoSlideshowEnabledChange(!homePhotoSlideshowEnabled) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                val reminderSubtitle = if (activeTripReminderEnabled) "After $activeTripReminderDelay hours" else "Off"
                SettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Active Trip Reminder",
                    subtitle = reminderSubtitle,
                    helperText = "Avoid leaving trips active accidentally.",
                    onClick = { showReminderDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                SettingRow(
                    icon = Icons.Default.Info,
                    title = "Welcome Guide",
                    subtitle = "View again",
                    helperText = "Review how FishLog works and privacy details.",
                    onClick = onViewWelcomeGuide
                )
            }

            // Data & Backup
            SettingsSection(title = "Data & Backup") {
                // 1. About Backups
                Text(
                    text = "FishLog stores data locally on this device. You can export backups manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // 2. Local Status
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Local Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Logs", style = MaterialTheme.typography.bodySmall)
                            Text(catches.size.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Trips", style = MaterialTheme.typography.bodySmall)
                            Text(trips.size.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending Cloud Backup", style = MaterialTheme.typography.bodySmall)
                            Text(pendingBackupCount.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. CSV Export
                Text("CSV Export", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("For spreadsheets and external analysis.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { csvExportLauncher.launch("fishlog-catches.csv") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Catch Logs CSV")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Full Backup
                Text("Full Backup", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Full backup for restoring or moving data.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { jsonExportLauncher.launch("fishlog-backup.json") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Full Backup", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Full Backup", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // 5. Cloud Backup (Future Premium)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cloud Backup", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            SuggestionChip(
                                onClick = { },
                                label = { Text(FeatureGate.paidLabel(PaidFeature.CLOUD_BACKUP), fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        
                        Text(
                            text = "Cloud backup and restore will protect your logs and catch photos in the event of a lost or damaged device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        // --- NEW STATUS AREA ---
                        val status = viewModel.cloudBackupStatus
                        if (status != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val workerMsg = status.autoBackupWorkerMessage
                            
                            val statusLabel = when {
                                !status.isSignedIn -> "Signed Out"
                                status.isBackingUp -> "Backing up..."
                                status.lastErrorMessage != null -> "Backup Failed"
                                status.isPending -> {
                                    if (workerMsg?.contains("scheduled", ignoreCase = true) == true) "Backup Scheduled"
                                    else "Backup Pending"
                                }
                                else -> "Up to date"
                            }
                            
                            val statusColor = when {
                                !status.isSignedIn -> Color.Gray
                                status.isBackingUp -> MaterialTheme.colorScheme.primary
                                status.lastErrorMessage != null -> MaterialTheme.colorScheme.error
                                status.isPending -> Color(0xFFFFA000)
                                else -> Color(0xFF4CAF50)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            if (status.isSignedIn) {
                                Text(
                                    text = "Signed in as: ${status.email}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                status.lastBackupAt?.let { lastAt ->
                                    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastAt))
                                    Text(
                                        text = "Last successful backup: $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } ?: run {
                                    if (status.lastErrorMessage == null) {
                                        Text("No cloud backup yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                
                                if (workerMsg != null && status.isPending) {
                                    Text(
                                        text = "Status: $workerMsg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                if (status.lastErrorMessage != null) {
                                    Text(
                                        text = "Last error: ${status.lastErrorMessage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            
                            if (status.isSignedIn) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.testCloudBackupSetup { result ->
                                            scope.launch { snackbarHostState.showSnackbar(result) }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = viewModel.backupUiState == BackupUiState.IDLE
                                ) {
                                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Test Cloud Connection")
                                }
                            }
                        }
                        // --- END STATUS AREA ---

                        // Backup Mode Selection
                        Text(
                            text = "Backup Mode",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = viewModel.cloudBackupMode == AppPreferences.CLOUD_BACKUP_MODE_MANUAL,
                                onClick = { viewModel.updateCloudBackupMode(AppPreferences.CLOUD_BACKUP_MODE_MANUAL) },
                                label = { Text("Manual") },
                                leadingIcon = { if (viewModel.cloudBackupMode == AppPreferences.CLOUD_BACKUP_MODE_MANUAL) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = viewModel.cloudBackupMode == AppPreferences.CLOUD_BACKUP_MODE_AUTOMATIC,
                                onClick = { viewModel.updateCloudBackupMode(AppPreferences.CLOUD_BACKUP_MODE_AUTOMATIC) },
                                label = { Text("Automatic") },
                                leadingIcon = { if (viewModel.cloudBackupMode == AppPreferences.CLOUD_BACKUP_MODE_AUTOMATIC) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = if (viewModel.cloudBackupMode == AppPreferences.CLOUD_BACKUP_MODE_AUTOMATIC)
                                "Automatic backups run shortly after changes when internet is available. It does not sync across devices automatically."
                                else "Back up only when you tap Backup Now.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (viewModel.accountStatus == AccountStatus.SIGNED_OUT) {
                            var email by remember { mutableStateOf("") }
                            val isEmailValid = email.contains("@") && email.isNotBlank()

                            Text(
                                text = "Enter your email and FishLog will send a one-time code. If you do not have an account yet, one will be created.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                                ),
                                isError = email.isNotBlank() && !isEmailValid
                            )
                            
                            if (email.isNotBlank() && !isEmailValid) {
                                Text(
                                    text = "Enter a valid email address.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val isOperationInProgress = viewModel.backupUiState == BackupUiState.AUTH_IN_PROGRESS || 
                                                      viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS || 
                                                      viewModel.backupUiState == BackupUiState.RESTORE_IN_PROGRESS

                            Button(
                                onClick = { viewModel.sendSignInCode(email) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = isEmailValid && !isOperationInProgress
                            ) {
                                if (isOperationInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Email Me a Sign-In Code")
                                }
                            }
                        } else if (viewModel.accountStatus == AccountStatus.WAITING_FOR_CODE) {
                            var code by remember { mutableStateOf("") }
                            val isCodeValid = code.length >= 6

                            Text(
                                text = "Check your email for the sign-in code.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Verifying: ${viewModel.pendingAuthEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("8-digit Code") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val isOperationInProgress = viewModel.backupUiState == BackupUiState.AUTH_IN_PROGRESS

                            Button(
                                onClick = { viewModel.verifyEmailCode(code) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = isCodeValid && !isOperationInProgress
                            ) {
                                if (isOperationInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Verify Code")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { viewModel.resendCode() }, enabled = !isOperationInProgress) {
                                    Text("Resend Code")
                                }
                                TextButton(onClick = { viewModel.changeEmail() }, enabled = !isOperationInProgress) {
                                    Text("Change Email")
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Signed in as:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = viewModel.accountEmail ?: "Unknown",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                TextButton(onClick = { viewModel.signOut() }) {
                                    Text("Sign Out", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }
                            }

                            if (viewModel.cloudBackupPending) {
                                Surface(
                                    color = Color(0xFFFFA000).copy(alpha = 0.1f),
                                    contentColor = Color(0xFFFFA000),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudSync, null, Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (viewModel.cloudBackupMode == AppPreferences.CLOUD_BACKUP_MODE_AUTOMATIC)
                                                "Backup pending. Uploading when internet is available."
                                                else "You have unsaved local changes.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            viewModel.lastCloudBackupAt?.let { lastAt ->
                                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastAt))
                                Text(
                                    text = "Last successful backup: $dateStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } ?: run {
                                Text(
                                    text = "No cloud backup yet.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            viewModel.lastCloudBackupErrorMessage?.let { error ->
                                Text(
                                    text = "Last attempt failed: $error",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val isOperationInProgress = viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS || 
                                                      viewModel.backupUiState == BackupUiState.RESTORE_IN_PROGRESS

                            /**
                             * TODO: Gate these buttons behind real premium entitlement check.
                             * For now, they remain placeholders for development.
                             */
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.backupNow() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isOperationInProgress
                                ) {
                                    if (viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Backing up...", fontSize = 12.sp)
                                    } else {
                                        Text("Backup Now", fontSize = 12.sp)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showRestoreConfirm = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isOperationInProgress
                                ) {
                                    if (viewModel.backupUiState == BackupUiState.RESTORE_IN_PROGRESS) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restoring...", fontSize = 12.sp)
                                    } else {
                                        Text("Restore", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                viewModel.backupStatusMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.backupUiState == BackupUiState.ERROR) 
                                MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearBackupMessage() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                statusMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { statusMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Developer Tools
            SettingsSection(title = "Developer Tools") {
                Text(
                    text = "Adds about 100 realistic test logs across 30+ trips for analytics testing. All dates are before today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.seedSampleData { result ->
                                scope.launch { snackbarHostState.showSnackbar(result) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Seed Rich Sample Data", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.removeSampleData { result ->
                                scope.launch { snackbarHostState.showSnackbar(result) }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear Sample Data", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onResetWelcomeScreen()
                        scope.launch { snackbarHostState.showSnackbar("Welcome screen reset. It will appear on next launch.") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Welcome Screen", fontSize = 12.sp)
                }
            }

            // About
            SettingsSection(title = "About FishLog") {
                SettingRow(
                    title = "App Name",
                    subtitle = "FishLog"
                )
                SettingRow(
                    title = "Version",
                    subtitle = "Development build"
                )
                SettingRow(
                    title = "Data model",
                    subtitle = "Local-first"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Privacy note:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "FishLog stores your data on this device unless you choose to export or back it up.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Danger Zone
            SettingsSection(title = "Danger Zone") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delete all trips, catches, and no-catch logs from this device. This does not change your settings or account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showDeleteAllStep1 = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Trips & Logs")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    helperText: String? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(clickableModifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (helperText != null) {
                Text(text = helperText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun AppearanceOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}
