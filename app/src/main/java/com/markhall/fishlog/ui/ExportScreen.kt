package com.markhall.fishlog.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markhall.fishlog.data.CsvExportHelper
import com.markhall.fishlog.data.JsonBackupHelper
import java.io.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: FishLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val catches by viewModel.allCatches.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export & Backup") },
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
            ExportSection(
                title = "CSV Export",
                description = "CSV is for spreadsheets and data analysis in tools like Excel or Google Sheets.",
                buttonText = "Export Catch Logs CSV",
                icon = Icons.Default.FileDownload,
                onClick = { csvExportLauncher.launch("fishlog-catches.csv") }
            )

            ExportSection(
                title = "Full Backup",
                description = "JSON backup is for restoring or moving your FishLog data to another device.",
                secondaryDescription = "Note: Photo files are not included in this backup yet.",
                buttonText = "Export Full Backup",
                icon = Icons.Default.Backup,
                onClick = { jsonExportLauncher.launch("fishlog-backup.json") },
                showImport = true,
                onImportClick = { importLauncher.launch(arrayOf("application/json")) }
            )

            statusMessage?.let {
                Surface(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ExportSection(
    title: String,
    description: String,
    secondaryDescription: String? = null,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    showImport: Boolean = false,
    onImportClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
            secondaryDescription?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(icon, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText)
            }
            if (showImport && onImportClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Backup")
                }
            }
        }
    }
}
