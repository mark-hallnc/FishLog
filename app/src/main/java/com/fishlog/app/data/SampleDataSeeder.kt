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

        // --- NEW REALISTIC SAMPLES ---

        // 7. Lake Moultrie, SC - Catfish & Bass (June 2026)
        calendar.set(2026, Calendar.JUNE, 12, 8, 30)
        val start7 = calendar.timeInMillis
        val tripId7 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-7",
            name = "Moultrie Monsters",
            waterBody = "Lake Moultrie, SC",
            startTime = start7,
            endTime = start7 + 14400000, // 4 hours
            notes = "Deep drop-offs. $SAMPLE_MARKER",
            latitude = 33.25,
            longitude = -80.05,
            skyCondition = "Partly Cloudy",
            windCondition = "Light",
            airTempF = 78.0,
            waterClarity = "Stained"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-7-1",
            species = "Blue Catfish",
            length = "38", weight = "28.5",
            waterTemp = "72", depth = "25",
            lengthInches = 38.0, weightLbs = 28.5,
            waterTempF = 72.0, depthFeet = 25.0,
            tripId = tripId7, bait = "cut shad",
            notes = "Big fight! $SAMPLE_MARKER",
            latitude = 33.255, longitude = -80.055,
            timestamp = start7 + 3600000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-7-2",
            species = "Channel Catfish",
            length = "20", weight = "5.0",
            waterTemp = "74", depth = "15",
            lengthInches = 20.0, weightLbs = 5.0,
            waterTempF = 74.0, depthFeet = 15.0,
            tripId = tripId7, bait = "chicken",
            notes = SAMPLE_MARKER,
            latitude = 33.256, longitude = -80.056,
            timestamp = start7 + 7200000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-7-3",
            species = "Largemouth Bass",
            length = "21", weight = "5.8",
            waterTemp = "72", depth = "8",
            lengthInches = 21.0, weightLbs = 5.8,
            waterTempF = 72.0, depthFeet = 8.0,
            tripId = tripId7, bait = "jig",
            notes = SAMPLE_MARKER,
            latitude = 33.257, longitude = -80.057,
            timestamp = start7 + 9000000
        ))

        // 8. Lake Okeechobee, FL - Bass Heaven (April 2026)
        calendar.set(2026, Calendar.APRIL, 22, 6, 0)
        val start8 = calendar.timeInMillis
        val tripId8 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-8",
            name = "The Big O",
            waterBody = "Lake Okeechobee, FL",
            startTime = start8,
            endTime = start8 + 18000000,
            notes = "Grass mats and shiners. $SAMPLE_MARKER",
            latitude = 26.95,
            longitude = -80.80,
            skyCondition = "Clear",
            windCondition = "Light",
            airTempF = 74.0,
            waterClarity = "Clear"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-8-1",
            species = "Largemouth Bass",
            length = "24", weight = "8.2",
            waterTemp = "76", depth = "3",
            lengthInches = 24.0, weightLbs = 8.2,
            waterTempF = 76.0, depthFeet = 3.0,
            tripId = tripId8, bait = "shiner",
            notes = "Personal best! $SAMPLE_MARKER",
            latitude = 26.951, longitude = -80.801,
            timestamp = start8 + 1800000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-8-2",
            species = "Largemouth Bass",
            length = "19", weight = "4.1",
            waterTemp = "76", depth = "2",
            lengthInches = 19.0, weightLbs = 4.1,
            waterTempF = 76.0, depthFeet = 2.0,
            tripId = tripId8, bait = "shiner",
            notes = SAMPLE_MARKER,
            latitude = 26.952, longitude = -80.802,
            timestamp = start8 + 3600000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-8-3",
            species = "Largemouth Bass",
            length = "22", weight = "6.5",
            waterTemp = "77", depth = "4",
            lengthInches = 22.0, weightLbs = 6.5,
            waterTempF = 77.0, depthFeet = 4.0,
            tripId = tripId8, bait = "soft plastic",
            notes = SAMPLE_MARKER,
            latitude = 26.953, longitude = -80.803,
            timestamp = start8 + 7200000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-8-4",
            species = "Bluegill",
            length = "9", weight = "0.6",
            waterTemp = "78", depth = "2",
            lengthInches = 9.0, weightLbs = 0.6,
            waterTempF = 78.0, depthFeet = 2.0,
            tripId = tripId8, bait = "worm",
            notes = SAMPLE_MARKER,
            latitude = 26.954, longitude = -80.804,
            timestamp = start8 + 9000000
        ))

        // 9. Lake Marion, SC - Santee Striper (November 2025)
        calendar.set(2025, Calendar.NOVEMBER, 12, 7, 0)
        val start9 = calendar.timeInMillis
        val tripId9 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-9",
            name = "Marion Stripers",
            waterBody = "Lake Marion, SC",
            startTime = start9,
            endTime = start9 + 14400000,
            notes = "Chasing gulls. $SAMPLE_MARKER",
            latitude = 33.50,
            longitude = -80.45,
            skyCondition = "Cloudy",
            windCondition = "Moderate",
            airTempF = 52.0,
            waterClarity = "Stained"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-9-1",
            species = "Striped Bass",
            length = "28", weight = "10.5",
            waterTemp = "54", depth = "18",
            lengthInches = 28.0, weightLbs = 10.5,
            waterTempF = 54.0, depthFeet = 18.0,
            tripId = tripId9, bait = "cut herring",
            notes = SAMPLE_MARKER,
            latitude = 33.501, longitude = -80.451,
            timestamp = start9 + 3600000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-9-2",
            species = "Striped Bass",
            length = "22", weight = "5.0",
            waterTemp = "54", depth = "22",
            lengthInches = 22.0, weightLbs = 5.0,
            waterTempF = 54.0, depthFeet = 22.0,
            tripId = tripId9, bait = "cut herring",
            notes = SAMPLE_MARKER,
            latitude = 33.502, longitude = -80.452,
            timestamp = start9 + 5400000
        ))

        // 10. Lake Norman, NC - Spotted Bass (February 2026)
        calendar.set(2026, Calendar.FEBRUARY, 18, 10, 0)
        val start10 = calendar.timeInMillis
        val tripId10 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-10",
            name = "Norman Spots",
            waterBody = "Lake Norman, NC",
            startTime = start10,
            endTime = start10 + 10800000,
            notes = "Winter deep bite. $SAMPLE_MARKER",
            latitude = 35.48,
            longitude = -80.93,
            skyCondition = "Clear",
            windCondition = "Calm",
            airTempF = 42.0,
            waterClarity = "Clear"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-10-1",
            species = "Spotted Bass",
            length = "16", weight = "2.4",
            waterTemp = "44", depth = "35",
            lengthInches = 16.0, weightLbs = 2.4,
            waterTempF = 44.0, depthFeet = 35.0,
            tripId = tripId10, bait = "dropshot worm",
            notes = SAMPLE_MARKER,
            latitude = 35.481, longitude = -80.931,
            timestamp = start10 + 3600000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}nocatch-10-1",
            logType = "NO_CATCH", species = "No Catch",
            length = "", weight = "",
            waterTemp = "44", depth = "40",
            waterTempF = 44.0, depthFeet = 40.0,
            tripId = tripId10, bait = "dropshot worm",
            notes = "Marked fish but wouldn't bite. $SAMPLE_MARKER",
            latitude = 35.482, longitude = -80.932,
            timestamp = start10 + 7200000
        ))

        // 11. Toledo Bend / Sam Rayburn - Texas Giants (May 2026)
        calendar.set(2026, Calendar.MAY, 25, 6, 30)
        val start11 = calendar.timeInMillis
        val tripId11 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-11",
            name = "Texas Lunkers",
            waterBody = "Toledo Bend, TX/LA",
            startTime = start11,
            endTime = start11 + 21600000, // 6 hours
            notes = "Post-spawn timber. $SAMPLE_MARKER",
            latitude = 31.20,
            longitude = -93.58,
            skyCondition = "Cloudy",
            windCondition = "Light",
            airTempF = 82.0,
            waterClarity = "Muddy"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-11-1",
            species = "Largemouth Bass",
            length = "23", weight = "7.4",
            waterTemp = "79", depth = "6",
            lengthInches = 23.0, weightLbs = 7.4,
            waterTempF = 79.0, depthFeet = 6.0,
            tripId = tripId11, bait = "Texas rig worm",
            notes = SAMPLE_MARKER,
            latitude = 31.201, longitude = -93.581,
            timestamp = start11 + 3600000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-11-2",
            species = "Largemouth Bass",
            length = "20", weight = "5.2",
            waterTemp = "79", depth = "10",
            lengthInches = 20.0, weightLbs = 5.2,
            waterTempF = 79.0, depthFeet = 10.0,
            tripId = tripId11, bait = "jig",
            notes = SAMPLE_MARKER,
            latitude = 31.202, longitude = -93.582,
            timestamp = start11 + 7200000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-11-3",
            species = "Crappie",
            length = "14", weight = "1.8",
            waterTemp = "79", depth = "12",
            lengthInches = 14.0, weightLbs = 1.8,
            waterTempF = 79.0, depthFeet = 12.0,
            tripId = tripId11, bait = "brush pile minnow",
            notes = SAMPLE_MARKER,
            latitude = 31.203, longitude = -93.583,
            timestamp = start11 + 10800000
        ))

        // 12. Kerr Lake, NC/VA - Spring Stripers (April 2026)
        calendar.set(2026, Calendar.APRIL, 10, 8, 0)
        val start12 = calendar.timeInMillis
        val tripId12 = fishingTripDao.insertTrip(FishingTrip(
            localUuid = "${UUID_PREFIX}trip-12",
            name = "Kerr Stripers",
            waterBody = "Kerr Lake, NC/VA",
            startTime = start12,
            endTime = start12 + 14400000,
            notes = "River run. $SAMPLE_MARKER",
            latitude = 36.52,
            longitude = -78.30,
            skyCondition = "Partly Cloudy",
            windCondition = "Moderate",
            airTempF = 62.0,
            waterClarity = "Stained"
        ))

        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-12-1",
            species = "Striped Bass",
            length = "26", weight = "8.5",
            waterTemp = "58", depth = "5",
            lengthInches = 26.0, weightLbs = 8.5,
            waterTempF = 58.0, depthFeet = 5.0,
            tripId = tripId12, bait = "live shad",
            notes = SAMPLE_MARKER,
            latitude = 36.521, longitude = -78.301,
            timestamp = start12 + 1800000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-12-2",
            species = "Largemouth Bass",
            length = "17", weight = "2.8",
            waterTemp = "60", depth = "2",
            lengthInches = 17.0, weightLbs = 2.8,
            waterTempF = 60.0, depthFeet = 2.0,
            tripId = tripId12, bait = "crankbait",
            notes = SAMPLE_MARKER,
            latitude = 36.522, longitude = -78.302,
            timestamp = start12 + 7200000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}catch-12-3",
            species = "Bluegill",
            length = "8", weight = "0.5",
            waterTemp = "62", depth = "2",
            lengthInches = 8.0, weightLbs = 0.5,
            waterTempF = 62.0, depthFeet = 2.0,
            tripId = tripId12, bait = "worm",
            notes = SAMPLE_MARKER,
            latitude = 36.523, longitude = -78.303,
            timestamp = start12 + 10800000
        ))
        catchLogDao.insertCatch(CatchLog(
            localUuid = "${UUID_PREFIX}nocatch-12-1",
            logType = "NO_CATCH", species = "No Catch",
            length = "", weight = "",
            waterTemp = "62", depth = "10",
            waterTempF = 62.0, depthFeet = 10.0,
            tripId = tripId12, bait = "live shad",
            notes = "Slow bite afternoon. $SAMPLE_MARKER",
            latitude = 36.524, longitude = -78.304,
            timestamp = start12 + 12600000
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
