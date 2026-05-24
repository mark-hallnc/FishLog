package com.fishlog.app.util

import com.fishlog.app.data.AppPreferences
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

object MapUtils {
    fun getTileSourceForStyle(style: String): ITileSource {
        return when (style) {
            AppPreferences.MAP_STYLE_TOPOGRAPHIC -> TileSourceFactory.OpenTopo
            AppPreferences.MAP_STYLE_SATELLITE -> TileSourceFactory.USGS_SAT
            else -> TileSourceFactory.MAPNIK
        }
    }
}
