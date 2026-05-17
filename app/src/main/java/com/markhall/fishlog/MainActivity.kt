package com.markhall.fishlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markhall.fishlog.map.MapScreen
import com.markhall.fishlog.ui.CatchFormScreen
import com.markhall.fishlog.ui.NoCatchFormScreen
import com.markhall.fishlog.ui.CatchListScreen
import com.markhall.fishlog.ui.ExportScreen
import com.markhall.fishlog.ui.CatchDetailScreen
import com.markhall.fishlog.ui.InsightsScreen
import com.markhall.fishlog.ui.StartTripScreen
import com.markhall.fishlog.ui.TripDetailScreen
import com.markhall.fishlog.ui.BackupScreen
import com.markhall.fishlog.ui.theme.FishLogTheme
import com.markhall.fishlog.data.CatchLog
import com.markhall.fishlog.data.FishingTrip
import com.markhall.fishlog.data.PhotoStorageHelper

import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.markhall.fishlog.data.FishLogDatabase
import com.markhall.fishlog.ui.FishLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = FishLogDatabase.getDatabase(applicationContext)
        val catchDao = database.catchLogDao()
        val tripDao = database.fishingTripDao()
        
        enableEdgeToEdge()
        setContent {
            val viewModel: FishLogViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return FishLogViewModel(catchDao, tripDao) as T
                    }
                }
            )
            FishLogTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: FishLogViewModel) {
    val context = LocalContext.current
    val photoStorageHelper = remember { PhotoStorageHelper(context) }
    var currentScreen by remember { mutableStateOf("Home") }
    var selectedCatch by remember { mutableStateOf<CatchLog?>(null) }
    var selectedTrip by remember { mutableStateOf<FishingTrip?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (currentScreen) {
            "Home" -> HomeScreen(
                viewModel = viewModel,
                onLogCatchClick = { 
                    selectedCatch = null
                    currentScreen = "Form" 
                },
                onLogNoCatchClick = {
                    selectedCatch = null
                    currentScreen = "NoCatchForm"
                },
                onHistoryClick = { currentScreen = "History" },
                onMapClick = { currentScreen = "Map" },
                onInsightsClick = { currentScreen = "Insights" },
                onBackupClick = { currentScreen = "Backup" },
                onExportClick = { currentScreen = "Export" },
                onStartTripClick = { currentScreen = "StartTrip" },
                onViewTripClick = { trip ->
                    selectedTrip = trip
                    currentScreen = "TripDetail"
                },
                modifier = modifier
            )
            "StartTrip" -> StartTripScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onTripStarted = { currentScreen = "Home" }
            )
            "TripDetail" -> selectedTrip?.let { trip ->
                TripDetailScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onBack = { currentScreen = "Home" },
                    onLogClick = { catch ->
                        selectedCatch = catch
                        currentScreen = "Detail"
                    }
                )
            } ?: run { currentScreen = "Home" }
            "Form" -> CatchFormScreen(
                viewModel = viewModel,
                onBack = { currentScreen = if (selectedCatch != null) "Detail" else "Home" },
                editingCatch = selectedCatch
            )
            "NoCatchForm" -> NoCatchFormScreen(
                viewModel = viewModel,
                onBack = { currentScreen = if (selectedCatch != null) "Detail" else "Home" },
                editingLog = selectedCatch
            )
            "History" -> CatchListScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onCatchClick = { catch ->
                    selectedCatch = catch
                    currentScreen = "Detail"
                }
            )
            "Detail" -> selectedCatch?.let { catch ->
                CatchDetailScreen(
                    catch = catch,
                    onBack = { currentScreen = "History" },
                    onEdit = { 
                        currentScreen = if (catch.logType == "NO_CATCH") "NoCatchForm" else "Form"
                    },
                    onDelete = {
                        viewModel.deleteCatch(catch, photoStorageHelper)
                        currentScreen = "History"
                    }
                )
            } ?: run { currentScreen = "History" }
            "Map" -> MapScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" }
            )
            "Insights" -> InsightsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" }
            )
            "Backup" -> BackupScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onExportBackup = { currentScreen = "Export" },
                onImportBackup = { currentScreen = "Export" }
            )
            "Export" -> ExportScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" }
            )
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: FishLogViewModel,
    onLogCatchClick: () -> Unit,
    onLogNoCatchClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMapClick: () -> Unit,
    onInsightsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onExportClick: () -> Unit,
    onStartTripClick: () -> Unit,
    onViewTripClick: (FishingTrip) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTrip by viewModel.activeTrip.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "FishLog",
            style = MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Offline fishing log and map",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.2.sp
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        TripStatusCard(
            activeTrip = activeTrip,
            onStartTrip = onStartTripClick,
            onEndTrip = { viewModel.endTrip(activeTrip!!) },
            onViewTrip = { onViewTripClick(activeTrip!!) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HomeCard(
                    title = "Log Catch",
                    icon = Icons.Default.AddCircle,
                    onClick = onLogCatchClick
                )
            }
            item {
                HomeCard(
                    title = "No Catch",
                    icon = Icons.Default.Block,
                    onClick = onLogNoCatchClick
                )
            }
            item {
                HomeCard(
                    title = "History",
                    icon = Icons.Default.History,
                    onClick = onHistoryClick
                )
            }
            item {
                HomeCard(
                    title = "Map",
                    icon = Icons.Default.Map,
                    onClick = onMapClick
                )
            }
            item {
                HomeCard(
                    title = "Insights",
                    icon = Icons.Default.Analytics,
                    onClick = onInsightsClick
                )
            }
            item {
                HomeCard(
                    title = "Backup",
                    icon = Icons.Default.CloudQueue,
                    onClick = onBackupClick
                )
            }
            item {
                HomeCard(
                    title = "Export",
                    icon = Icons.Default.FileDownload,
                    onClick = onExportClick
                )
            }
        }
    }
}

@Composable
fun TripStatusCard(
    activeTrip: FishingTrip?,
    onStartTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onViewTrip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (activeTrip != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeTrip == null) {
                Text("No active fishing trip", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onStartTrip, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Trip")
                }
            } else {
                Text("Active Trip: ${activeTrip.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (activeTrip.waterBody.isNotBlank()) {
                    Text(activeTrip.waterBody, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
                Text("Started at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activeTrip.startTime))}", style = MaterialTheme.typography.bodySmall)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewTrip,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text("View Details")
                    }
                    Button(
                        onClick = onEndTrip,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("End Trip")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FishLogTheme {
        // Mock data or empty ViewModel would be needed here for a full preview
        // For now, keeping it simple as requested
    }
}
