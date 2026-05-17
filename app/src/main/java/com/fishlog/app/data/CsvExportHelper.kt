package com.fishlog.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {

    fun convertToCsv(catches: List<CatchLog>, trips: List<FishingTrip> = emptyList()): String {
        val header = "id,logType,tripId,tripName,species,length,lengthInches,weight,weightLbs,waterTemp,waterTempF,depth,depthFeet,bait,notes,latitude,longitude,photoUri,timestamp,dateTimeReadable"
        val csvBuilder = StringBuilder()
        csvBuilder.append(header).append("\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (catch in catches) {
            val trip = trips.find { it.id == catch.tripId }
            val row = listOf(
                catch.id.toString(),
                catch.logType,
                catch.tripId?.toString() ?: "",
                escapeCsv(trip?.name ?: ""),
                escapeCsv(catch.species),
                escapeCsv(catch.length),
                catch.lengthInches?.toString() ?: "",
                escapeCsv(catch.weight),
                catch.weightLbs?.toString() ?: "",
                escapeCsv(catch.waterTemp),
                catch.waterTempF?.toString() ?: "",
                escapeCsv(catch.depth),
                catch.depthFeet?.toString() ?: "",
                escapeCsv(catch.bait),
                escapeCsv(catch.notes),
                catch.latitude?.toString() ?: "",
                catch.longitude?.toString() ?: "",
                escapeCsv(catch.photoUri ?: ""),
                catch.timestamp.toString(),
                sdf.format(Date(catch.timestamp))
            )
            csvBuilder.append(row.joinToString(",")).append("\n")
        }

        return csvBuilder.toString()
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            val escapedValue = value.replace("\"", "\"\"")
            return "\"$escapedValue\""
        }
        return value
    }
}

