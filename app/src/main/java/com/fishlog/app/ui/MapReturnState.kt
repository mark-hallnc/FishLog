package com.fishlog.app.ui

data class MapReturnState(
    val centerLat: Double,
    val centerLon: Double,
    val zoom: Double,
    val openedFromMap: Boolean = false
)
