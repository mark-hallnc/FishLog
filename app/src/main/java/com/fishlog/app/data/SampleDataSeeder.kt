package com.fishlog.app.data

import kotlinx.coroutines.flow.first
import java.util.*

object SampleDataSeeder {
    private const val SAMPLE_MARKER = "SAMPLE_DATA"
    private const val UUID_PREFIX = "sample-"

    suspend fun seed(catchLogDao: CatchLogDao, fishingTripDao: FishingTripDao): String {
        val existingTrips = fishingTripDao.getAllTrips().first()
        if (existingTrips.any { it.localUuid.startsWith(UUID_PREFIX) }) {
            return "Sample data already exists"
        }

        val calendar = Calendar.getInstance()
        
        // 1. Oak Hollow Lake - Spring Morning (March 2026)
        calendar.set(2026, Calendar.MARCH, 15, 7, 0)
        val startTime1 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 4)
        val endTime1 = calendar.timeInMillis
        val tripId1 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-1",
            name = "Oak Hollow Morning",
            waterBody = "Oak Hollow Lake",
            startTime = startTime1,
            endTime = endTime1,
            notes = "Spring morning bite. $SAMPLE_MARKER",
            latitude = 36.007,
            longitude = -79.997,
            skyCondition = "Clear",
            windCondition = "Calm",
            airTempF = 58.0,
            waterClarity = "Clear",
            pressureTrend = "Steady"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-1",
            logType = "CATCH",
            species = "Largemouth Bass",
            length = "18",
            weight = "3.2",
            waterTemp = "58",
            depth = "5",
            lengthInches = 18.0,
            weightLbs = 3.2,
            waterTempF = 58.0,
            depthFeet = 5.0,
            tripId = tripId1,
            bait = "spinnerbait",
            notes = "Caught near the dam. $SAMPLE_MARKER",
            latitude = 36.008,
            longitude = -79.998,
            timestamp = startTime1 + 1800000 // +30 mins
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-2",
            logType = "CATCH",
            species = "Largemouth Bass",
            length = "14",
            weight = "1.5",
            waterTemp = "59",
            depth = "3",
            lengthInches = 14.0,
            weightLbs = 1.5,
            waterTempF = 59.0,
            depthFeet = 3.0,
            tripId = tripId1,
            bait = "spinnerbait",
            notes = SAMPLE_MARKER,
            latitude = 36.009,
            longitude = -79.999,
            timestamp = startTime1 + 3600000 // +1 hour
        ))

        // 2. High Rock Lake - Summer Evening (July 2026)
        calendar.set(2026, Calendar.JULY, 10, 18, 0)
        val startTime2 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 3)
        val endTime2 = calendar.timeInMillis
        val tripId2 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-2",
            name = "High Rock Evening",
            waterBody = "High Rock Lake",
            startTime = startTime2,
            endTime = endTime2,
            notes = "Hot summer evening. $SAMPLE_MARKER",
            latitude = 35.650,
            longitude = -80.250,
            skyCondition = "Partly Cloudy",
            windCondition = "Light",
            airTempF = 88.0,
            waterClarity = "Stained",
            pressureTrend = "Falling"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-3",
            logType = "CATCH",
            species = "Channel Catfish",
            length = "22",
            weight = "4.5",
            waterTemp = "82",
            depth = "12",
            lengthInches = 22.0,
            weightLbs = 4.5,
            waterTempF = 82.0,
            depthFeet = 12.0,
            tripId = tripId2,
            bait = "chicken",
            notes = "Deep water hole. $SAMPLE_MARKER",
            latitude = 35.651,
            longitude = -80.251,
            timestamp = startTime2 + 7200000 // +2 hours
        ))

        // 3. Farm Pond - Quick Bank Trip (May 2026)
        calendar.set(2026, Calendar.MAY, 5, 14, 0)
        val startTime3 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 1)
        val endTime3 = calendar.timeInMillis
        val tripId3 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-3",
            name = "Pond Session",
            waterBody = "Farm Pond",
            startTime = startTime3,
            endTime = endTime3,
            notes = "Testing monthly grouping. $SAMPLE_MARKER",
            latitude = 35.9557,
            longitude = -80.0053,
            skyCondition = "Cloudy",
            airTempF = 72.0
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-4",
            logType = "CATCH",
            species = "Bluegill",
            length = "8",
            weight = "0.5",
            waterTemp = "72",
            depth = "2",
            lengthInches = 8.0,
            weightLbs = 0.5,
            waterTempF = 72.0,
            depthFeet = 2.0,
            tripId = tripId3,
            bait = "worm",
            notes = SAMPLE_MARKER,
            latitude = 35.9558,
            longitude = -80.0054,
            timestamp = startTime3 + 900000 // +15 mins
        ))

        // 4. Coastal Inshore - Saltwater (October 2025 - Older trip)
        calendar.set(2025, Calendar.OCTOBER, 20, 10, 0)
        val startTime4 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 6)
        val endTime4 = calendar.timeInMillis
        val tripId4 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-4",
            name = "Inshore Redfish",
            waterBody = "Bogue Sound",
            startTime = startTime4,
            endTime = endTime4,
            notes = "Autumn migration. $SAMPLE_MARKER",
            latitude = 34.720,
            longitude = -76.670,
            skyCondition = "Clear",
            windCondition = "Moderate",
            airTempF = 65.0,
            waterClarity = "Clear",
            pressureTrend = "Rising"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-5",
            logType = "CATCH",
            species = "Red Drum",
            length = "24",
            weight = "6.0",
            waterTemp = "68",
            depth = "3",
            lengthInches = 24.0,
            weightLbs = 6.0,
            waterTempF = 68.0,
            depthFeet = 3.0,
            tripId = tripId4,
            bait = "shrimp",
            notes = "Near the grass marsh. $SAMPLE_MARKER",
            latitude = 34.721,
            longitude = -76.671,
            timestamp = startTime4 + 3600000
        ))

        // 5. One trip with no catches, only no-catch logs (May 2026 - same month as pond)
        calendar.set(2026, Calendar.MAY, 20, 16, 0)
        val startTime5 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 2)
        val endTime5 = calendar.timeInMillis
        val tripId5 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-5",
            name = "Skunked Session",
            waterBody = "High Rock Lake",
            startTime = startTime5,
            endTime = endTime5,
            notes = "No luck today. $SAMPLE_MARKER",
            latitude = 35.655,
            longitude = -80.255,
            skyCondition = "Rain",
            windCondition = "Strong",
            airTempF = 68.0,
            waterClarity = "Muddy",
            pressureTrend = "Falling"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}nocatch-1",
            logType = "NO_CATCH",
            species = "No Catch",
            length = "",
            weight = "",
            waterTemp = "74",
            depth = "5",
            waterTempF = 74.0,
            depthFeet = 5.0,
            tripId = tripId5,
            bait = "spinnerbait",
            notes = "Tried everything. $SAMPLE_MARKER",
            latitude = 35.656,
            longitude = -80.256,
            timestamp = startTime5 + 1800000
        ))

        // 6. Standalone catch (Recent - May 2026)
        calendar.set(2026, Calendar.MAY, 17, 12, 0)
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-standalone",
            logType = "CATCH",
            species = "Crappie",
            length = "12",
            weight = "1.0",
            waterTemp = "70",
            depth = "6",
            lengthInches = 12.0,
            weightLbs = 1.0,
            waterTempF = 70.0,
            depthFeet = 6.0,
            bait = "minnow",
            notes = "Standalone catch at the dock. $SAMPLE_MARKER",
            latitude = 35.956,
            longitude = -80.006,
            timestamp = calendar.timeInMillis
        ))

        return "Sample data added"
    }

    suspend fun clear(catchLogDao: CatchLogDao, fishingTripDao: FishingTripDao): String {
        val allCatches = catchLogDao.getAllCatches().first()
        val allTrips = fishingTripDao.getAllTrips().first()

        var count = 0
        allCatches.filter { it.localUuid.startsWith(UUID_PREFIX) }.forEach {
            catchLogDao.deleteCatch(it)
            count++
        }

        allTrips.filter { it.localUuid.startsWith(UUID_PREFIX) }.forEach {
            fishingTripDao.deleteTrip(it)
            count++
        }

        return if (count > 0) "Sample data removed ($count items)" else "No sample data found"
    }
}
