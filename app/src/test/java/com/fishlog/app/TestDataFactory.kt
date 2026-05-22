package com.fishlog.app

import com.fishlog.app.data.CatchLog
import com.fishlog.app.data.FishingTrip

object TestDataFactory {
    fun sampleCatch(
        id: Long = 0,
        species: String = "Channel Catfish",
        bait: String = "Chicken Liver",
        depthFeet: Double? = null,
        waterTempF: Double? = null,
        timestamp: Long = System.currentTimeMillis(),
        tripId: Long? = null
    ) = CatchLog(
        id = id,
        logType = "CATCH",
        species = species,
        length = "",
        weight = "",
        waterTemp = waterTempF?.toString() ?: "",
        depth = depthFeet?.toString() ?: "",
        waterTempF = waterTempF,
        depthFeet = depthFeet,
        timestamp = timestamp,
        bait = bait,
        notes = "",
        tripId = tripId
    )

    fun sampleNoCatch(
        id: Long = 0,
        species: String = "No Catch",
        bait: String = "Chicken Liver",
        depthFeet: Double? = null,
        waterTempF: Double? = null,
        timestamp: Long = System.currentTimeMillis(),
        tripId: Long? = null
    ) = CatchLog(
        id = id,
        logType = "NO_CATCH",
        species = species,
        length = "",
        weight = "",
        waterTemp = waterTempF?.toString() ?: "",
        depth = depthFeet?.toString() ?: "",
        waterTempF = waterTempF,
        depthFeet = depthFeet,
        timestamp = timestamp,
        bait = bait,
        notes = "",
        tripId = tripId
    )

    fun sampleTrip(
        id: Long = 0,
        name: String = "Test Trip",
        waterBody: String = "Oak Hollow Lake",
        startTime: Long = System.currentTimeMillis(),
        moonPhaseName: String = ""
    ) = FishingTrip(
        id = id,
        name = name,
        waterBody = waterBody,
        startTime = startTime,
        notes = "",
        moonPhaseName = moonPhaseName
    )
}
