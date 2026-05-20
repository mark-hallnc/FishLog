package com.fishlog.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fishlog.app.data.*
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FishLogViewModel,
    appearanceMode: String,
    unitSystem: String,
    onAppearanceModeChange: (String) -> Unit,
    onUnitSystemChange: (String) -> Unit,
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
                SettingRow(
                    icon = Icons.Default.MyLocation,
                    title = "Default Map Center",
                    subtitle = "Current location",
                    helperText = "Map opens near your GPS location when permission is available."
                )
            }

            // Data & Backup
            SettingsSection(title = "Data & Backup") {
                // 1. About Backups
                Text(
                    text = "FishLog stores data locally on this device. You can export backups manually. Cloud backup is optional and coming later.",
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
                Text("JSON backup for restoring or moving data.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                // 5. Cloud Backup
                Text("Cloud Backup (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("Sign in to sync your data to the cloud.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.accountStatus == AccountStatus.SIGNED_OUT) {
                    var email by remember { mutableStateOf("") }
                    val isEmailValid = email.contains("@") && email.isNotBlank()

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

                    Spacer(modifier = Modifier.height(12.dp))

                    val isOperationInProgress = viewModel.backupUiState == BackupUiState.AUTH_IN_PROGRESS || 
                                              viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS || 
                                              viewModel.backupUiState == BackupUiState.RESTORE_IN_PROGRESS

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.createAccount(email) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isEmailValid && !isOperationInProgress
                        ) {
                            Text("Create Account", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.signIn(email) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = isEmailValid && !isOperationInProgress
                        ) {
                            Text("Sign In", fontSize = 12.sp)
                        }
                    }
                } else if (viewModel.accountStatus == AccountStatus.WAITING_FOR_CODE) {
                    var code by remember { mutableStateOf("") }
                    val isCodeValid = code.length >= 6

                    Text(
                        text = "Check your email for a sign-in code.",
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
                        label = { Text("6-digit Code") },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    val isOperationInProgress = viewModel.backupUiState == BackupUiState.BACKUP_IN_PROGRESS || 
                                              viewModel.backupUiState == BackupUiState.RESTORE_IN_PROGRESS

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
                            onClick = { viewModel.restoreFromCloud() },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isOperationInProgress
                    ) {
                        Text("Sign Out", color = MaterialTheme.colorScheme.error)
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
                    text = "For testing only. Adds sample trips, catches, and no-catch logs.",
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
                        Text("Add Sample Data")
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
                        Text("Remove Sample Data")
                    }
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
