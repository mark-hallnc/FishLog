package com.fishlog.app.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.fishlog.app.R
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.util.MapUtils
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun StaticMapPreview(
    latitude: Double,
    longitude: Double,
    isNoCatch: Boolean,
    modifier: Modifier = Modifier,
    zoomLevel: Double = 15.0,
    mapStyle: String = AppPreferences.MAP_STYLE_STANDARD
) {
    val context = LocalContext.current
    
    val mapView = remember(mapStyle) {
        MapView(context).apply {
            setTileSource(MapUtils.getTileSourceForStyle(mapStyle))
            setMultiTouchControls(false)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
            // Disable all interactions to make it "static"
            isClickable = false
            isFocusable = false
            // This prevents the map from consuming touch events that should go to the parent Card
            setOnTouchListener { _, _ -> true }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            val point = GeoPoint(latitude, longitude)
            view.controller.setZoom(zoomLevel)
            view.controller.setCenter(point)
            
            view.overlays.clear()
            val marker = Marker(view)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Use same icons as MapScreen
            if (isNoCatch) {
                marker.icon = context.getDrawable(R.drawable.ic_map_marker_no_catch)
            } else {
                marker.icon = context.getDrawable(R.drawable.ic_map_marker_catch)
            }
            
            // Ensure marker info window doesn't pop up on a static preview
            marker.setOnMarkerClickListener { _, _ -> true }
            
            view.overlays.add(marker)
            view.invalidate()
        }
    )
}
