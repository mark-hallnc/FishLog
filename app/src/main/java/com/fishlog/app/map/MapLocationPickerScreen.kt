package com.fishlog.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fishlog.app.location.LocationService
import com.fishlog.app.ui.FishLogViewModel
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerScreen(
    initialLat: Double?,
    initialLon: Double?,
    initialZoom: Double,
    viewModel: FishLogViewModel,
    onLocationPicked: (Double, Double, Double) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val catches by viewModel.allCatches.collectAsState()

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }

    var initialCenterApplied by remember { mutableStateOf(false) }

    fun centerOnUser() {
        scope.launch {
            val location = locationService.getCurrentLocation()
            if (location != null) {
                mapView.controller.animateTo(GeoPoint(location.latitude, location.longitude))
                mapView.controller.setZoom(15.0)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!initialCenterApplied) {
            initialCenterApplied = true
            
            val targetLat: Double
            val targetLon: Double
            val targetZoom: Double
            
            if (initialLat != null && initialLon != null) {
                targetLat = initialLat
                targetLon = initialLon
                targetZoom = initialZoom
            } else {
                val location = locationService.getCurrentLocation()
                if (location != null) {
                    targetLat = location.latitude
                    targetLon = location.longitude
                    targetZoom = 15.0
                } else {
                    // Fallback: most recent log
                    val mostRecent = catches.filter { it.latitude != null }.maxByOrNull { it.timestamp }
                    if (mostRecent != null) {
                        targetLat = mostRecent.latitude!!
                        targetLon = mostRecent.longitude!!
                        targetZoom = 13.0
                    } else {
                        // High Point, NC
                        targetLat = 35.9557
                        targetLon = -80.0053
                        targetZoom = 12.0
                    }
                }
            }
            
            mapView.post {
                mapView.controller.setZoom(targetZoom)
                mapView.controller.setCenter(GeoPoint(targetLat, targetLon))
                mapView.invalidate()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Default Map Location") },
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
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView }
            )

            // Center Crosshair
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp) // Align point of icon to center
            )
            
            // Helper text overlay
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Text(
                    "Move the map to the spot you want FishLog to open.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { centerOnUser() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on Me")
                }

                Button(
                    onClick = {
                        val center = mapView.mapCenter
                        onLocationPicked(center.latitude, center.longitude, mapView.zoomLevelDouble)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Use This Location")
                }
            }
        }
    }
}
