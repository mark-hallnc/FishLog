package com.fishlog.app

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
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.sp
import com.fishlog.app.map.MapScreen
import com.fishlog.app.ui.CatchFormScreen
import com.fishlog.app.ui.NoCatchFormScreen
import com.fishlog.app.ui.CatchListScreen
import com.fishlog.app.ui.ExportScreen
import com.fishlog.app.ui.CatchDetailScreen
import com.fishlog.app.ui.InsightsScreen
import com.fishlog.app.ui.StartTripScreen
import com.fishlog.app.ui.TripDetailScreen
import com.fishlog.app.ui.TripHistoryScreen
import com.fishlog.app.ui.TripSummaryScreen
import com.fishlog.app.ui.BackupScreen
import com.fishlog.app.ui.theme.FishLogTheme
import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip
import com.fishlog.app.data.PhotoStorageHelper

import androidx.compose.ui.tooling.preview.Preview

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fishlog.app.data.FishLogDatabase
import com.fishlog.app.ui.FishLogViewModel
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
    var previousScreen by remember { mutableStateOf("Home") }
    var selectedCatch by remember { mutableStateOf<CatchLog?>(null) }
    var selectedTrip by remember { mutableStateOf<FishingTrip?>(null) }
    val scope = rememberCoroutineScope()

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
                onHistoryClick = { 
                    previousScreen = "Home"
                    currentScreen = "History" 
                },
                onMapClick = { 
                    previousScreen = "Home"
                    currentScreen = "Map" 
                },
                onInsightsClick = { currentScreen = "Insights" },
                onBackupClick = { currentScreen = "Backup" },
                onExportClick = { currentScreen = "Export" },
                onTripHistoryClick = { currentScreen = "TripHistory" },
                onStartTripClick = { currentScreen = "StartTrip" },
                onViewTripClick = { trip ->
                    selectedTrip = trip
                    currentScreen = "TripDetail"
                },
                onEndTripClick = { trip ->
                    selectedTrip = trip.copy(endTime = System.currentTimeMillis())
                    currentScreen = "TripSummary"
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
                    },
                    onLogCatch = {
                        selectedCatch = null
                        currentScreen = "Form"
                    },
                    onLogNoCatch = {
                        selectedCatch = null
                        currentScreen = "NoCatchForm"
                    },
                    onTripEnded = { endedTrip ->
                        selectedTrip = endedTrip
                        currentScreen = "TripSummary"
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
                    previousScreen = "History"
                    currentScreen = "Detail"
                }
            )
            "Detail" -> selectedCatch?.let { catch ->
                CatchDetailScreen(
                    catch = catch,
                    onBack = { currentScreen = previousScreen },
                    onEdit = { 
                        currentScreen = if (catch.logType == "NO_CATCH") "NoCatchForm" else "Form"
                    },
                    onDelete = {
                        viewModel.deleteCatch(catch, photoStorageHelper)
                        currentScreen = previousScreen
                    }
                )
            } ?: run { currentScreen = previousScreen }
            "Map" -> MapScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onLogClick = { catch ->
                    selectedCatch = catch
                    previousScreen = "Map"
                    currentScreen = "Detail"
                }
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
            "TripHistory" -> TripHistoryScreen(
                viewModel = viewModel,
                onBack = { currentScreen = "Home" },
                onTripClick = { trip ->
                    selectedTrip = trip
                    currentScreen = "TripDetail"
                },
                onStartTripClick = { currentScreen = "StartTrip" }
            )
            "TripSummary" -> selectedTrip?.let { trip ->
                TripSummaryScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onDone = { currentScreen = "Home" },
                    onViewDetails = { currentScreen = "TripDetail" }
                )
            } ?: run { currentScreen = "Home" }
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
    onTripHistoryClick: () -> Unit,
    onStartTripClick: () -> Unit,
    onViewTripClick: (FishingTrip) -> Unit,
    onEndTripClick: (FishingTrip) -> Unit,
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
            onEndTrip = {
                // TripStatusCard expect a suspend lambda.
                // onEndTripClick is not suspend, but we don't need it to be if it handles its own scope or if we just want it to return immediately.
                // However, TripStatusCard waits for onEndTrip() to complete before resetting isEndingTrip.
                // So let's make onEndTripClick suspend or just call viewModel here.
                activeTrip?.let { trip ->
                    viewModel.endTrip(trip)
                    onEndTripClick(trip)
                }
            },
            onViewTrip = { activeTrip?.let { onViewTripClick(it) } },
            onLogCatch = onLogCatchClick,
            onLogNoCatch = onLogNoCatchClick
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
                    title = "Trip History",
                    icon = Icons.Default.ListAlt,
                    onClick = onTripHistoryClick
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
    onEndTrip: suspend () -> Unit,
    onViewTrip: () -> Unit,
    onLogCatch: () -> Unit = {},
    onLogNoCatch: () -> Unit = {}
) {
    var isEndingTrip by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Reset isEndingTrip when activeTrip changes (e.g. when it becomes null)
    LaunchedEffect(activeTrip == null) {
        if (activeTrip == null) {
            isEndingTrip = false
        }
    }

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
                        enabled = !isEndingTrip,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text("View Details")
                    }
                    Button(
                        onClick = {
                            if (isEndingTrip) return@Button
                            scope.launch {
                                isEndingTrip = true
                                try {
                                    onEndTrip()
                                } catch (e: Exception) {
                                    isEndingTrip = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isEndingTrip,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (isEndingTrip) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End Trip")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onLogCatch,
                        modifier = Modifier.weight(1f),
                        enabled = !isEndingTrip,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Catch")
                    }
                    Button(
                        onClick = onLogNoCatch,
                        modifier = Modifier.weight(1f),
                        enabled = !isEndingTrip,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("No-Catch")
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

