package com.fishlog.app.data

import kotlinx.serialization.Serializable

@Serializable
data class NearbyWaterBody(
    val name: String,
    val latitude: Double?,
    val longitude: Double?,
    val type: String,
    val distanceMeters: Double? = null,
    val source: String = "OpenStreetMap"
)
