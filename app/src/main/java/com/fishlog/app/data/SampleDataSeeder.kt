package com.fishlog.app.data

import kotlinx.coroutines.flow.first
import java.util.*
import kotlin.random.Random

object SampleDataSeeder {
    private const val UUID_PREFIX = "sample-"

    private val LAKES = listOf(
        LakeInfo("Oak Hollow Lake", 36.005, -80.000),
        LakeInfo("High Rock Lake", 35.620, -80.240),
        LakeInfo("Lake Moultrie", 33.320, -80.070)
    )

    private val CATFISH_SPECIES = listOf("Channel Catfish", "Blue Catfish", "Flathead Catfish")
    private val BYCATCH_SPECIES = listOf("White Perch", "Bluegill", "Largemouth Bass", "Bowfin", "Common Carp", "Striped Bass")
    
    private val CATFISH_BAITS = listOf("Cut Shad", "Chicken Liver", "Nightcrawler", "Shrimp", "Hot Dog", "Stink Bait", "Cut Bluegill", "Dip Bait")
    private val BYCATCH_BAITS = listOf("Small Jig", "Minnow", "Crankbait", "Worm", "Spinnerbait")

    data class LakeInfo(val name: String, val lat: Double, val lon: Double)

    suspend fun seed(catchLogDao: CatchLogDao, fishingTripDao: FishingTripDao): String {
        val existingTrips = fishingTripDao.getAllTrips().first()
        if (existingTrips.any { it.localUuid.startsWith(UUID_PREFIX) }) {
            return "Sample data already exists. Clear or replace it before seeding again."
        }

        val random = Random(42) // Fixed seed for reproducibility
        var totalTrips = 0
        var totalCatches = 0
        var totalNoCatches = 0

        // Create ~36 trips to cover 100+ logs
        for (i in 1..36) {
            val lake = LAKES[random.nextInt(LAKES.size)]
            val startTime = generateRandomTimestamp(random)
            val durationMillis = (random.nextInt(2, 7) * 3600000).toLong()
            val endTime = startTime + durationMillis
            
            val moonData = MoonPhaseCalculator.calculate(startTime)
            val season = getSeason(startTime)
            val timeOfDay = getTimeOfDay(startTime)
            
            val tripId = fishingTripDao.insertTrip(FishingTrip(
                localUuid = "${UUID_PREFIX}trip-$i",
                name = "${lake.name} ${season} ${timeOfDay}",
                waterBody = lake.name,
                startTime = startTime,
                endTime = endTime,
                notes = "Sample trip for analytics testing.",
                latitude = lake.lat + randomJitter(random),
                longitude = lake.lon + randomJitter(random),
                skyCondition = listOf("Clear", "Partly Cloudy", "Cloudy", "Rain").random(random),
                windCondition = listOf("Calm", "Light", "Moderate", "Strong").random(random),
                airTempF = Math.round(getSeasonalTemp(season, timeOfDay, random)).toDouble(),
                waterClarity = listOf("Clear", "Slightly Stained", "Stained", "Muddy").random(random),
                pressureTrend = listOf("Rising", "Steady", "Falling").random(random),
                weatherAutoFilled = true,
                weatherSource = "Sample",
                weatherFetchedAt = startTime,
                humidityPercent = Math.round(random.nextDouble(30.0, 90.0)).toDouble(),
                windSpeedMph = Math.round(random.nextDouble(0.0, 20.0)).toDouble(),
                windDirectionDegrees = Math.round(random.nextDouble(0.0, 360.0)).toDouble(),
                barometricPressureHpa = (Math.round(random.nextDouble(1000.0, 1025.0) * 10.0) / 10.0),
                cloudCoverPercent = Math.round(random.nextDouble(0.0, 100.0)).toDouble(),
                moonAutoFilled = true,
                moonPhaseName = moonData.phaseName,
                moonIlluminationPercent = moonData.illuminationPercent,
                moonAgeDays = moonData.ageDays,
                moonPhaseFraction = moonData.phaseFraction,
                moonWaxing = moonData.waxing,
                moonCalculatedAt = startTime
            ))
            totalTrips++

            // Generate 1-6 logs per trip
            val numLogs = random.nextInt(1, 7)
            for (j in 1..numLogs) {
                val isCatch = random.nextDouble() < 0.75 // 75% catch rate for sample richness
                val logTime = startTime + (random.nextLong(0, durationMillis))
                
                if (isCatch) {
                    val isCatfish = random.nextDouble() < 0.70 // 70% catfish
                    val species = if (isCatfish) CATFISH_SPECIES.random(random) else BYCATCH_SPECIES.random(random)
                    val bait = if (isCatfish) CATFISH_BAITS.random(random) else (CATFISH_BAITS + BYCATCH_BAITS).random(random)
                    
                    val length = getRealisticLength(species, random)
                    val weight = getRealisticWeight(species, length, random)
                    val depth = getRealisticDepth(species, timeOfDay, random)
                    val temp = getSeasonalWaterTemp(season, random)

                    catchLogDao.insertCatch(CatchLog(
                        localUuid = "${UUID_PREFIX}catch-$i-$j",
                        logType = "CATCH",
                        species = species,
                        length = length.toInt().toString(),
                        weight = "%.1f".format(weight),
                        waterTemp = temp.toInt().toString(),
                        depth = depth.toInt().toString(),
                        lengthInches = (Math.round(length * 100.0) / 100.0),
                        weightLbs = (Math.round(weight * 100.0) / 100.0),
                        waterTempF = Math.round(temp).toDouble(),
                        depthFeet = Math.round(depth).toDouble(),
                        tripId = tripId,
                        bait = bait,
                        notes = "Caught during $timeOfDay session.",
                        latitude = lake.lat + randomJitter(random),
                        longitude = lake.lon + randomJitter(random),
                        timestamp = logTime,
                        updatedAt = logTime,
                        backupStatus = BackupStatus.PENDING_BACKUP
                    ))
                    totalCatches++
                } else {
                    val bait = CATFISH_BAITS.random(random)
                    val depth = random.nextDouble(5.0, 30.0)
                    val temp = getSeasonalWaterTemp(season, random)

                    catchLogDao.insertCatch(CatchLog(
                        localUuid = "${UUID_PREFIX}nocatch-$i-$j",
                        logType = "NO_CATCH",
                        species = "No Catch",
                        length = "",
                        weight = "",
                        waterTemp = temp.toInt().toString(),
                        depth = depth.toInt().toString(),
                        waterTempF = Math.round(temp).toDouble(),
                        depthFeet = Math.round(depth).toDouble(),
                        tripId = tripId,
                        bait = bait,
                        notes = "Marked fish but no bite.",
                        latitude = lake.lat + randomJitter(random),
                        longitude = lake.lon + randomJitter(random),
                        timestamp = logTime,
                        updatedAt = logTime,
                        backupStatus = BackupStatus.PENDING_BACKUP
                    ))
                    totalNoCatches++
                }
            }
        }

        return "Successfully added $totalTrips trips, $totalCatches catches, and $totalNoCatches no-catches."
    }

    private fun generateRandomTimestamp(random: Random): Long {
        val calendar = Calendar.getInstance()
        // Dates between March 2025 and May 20, 2026
        val startYear = 2025
        val endYear = 2026
        
        val is2026 = random.nextBoolean()
        val year = if (is2026) 2026 else 2025
        val month = if (year == 2026) random.nextInt(0, 5) else random.nextInt(2, 12)
        val day = if (year == 2026 && month == 4) random.nextInt(1, 21) else random.nextInt(1, 28)
        val hour = random.nextInt(0, 24)
        val minute = random.nextInt(0, 60)
        
        calendar.set(year, month, day, hour, minute)
        return calendar.timeInMillis
    }

    private fun randomJitter(random: Random): Double {
        return (random.nextDouble() - 0.5) * 0.05 // +/- ~3 miles
    }

    private fun getSeason(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (cal.get(Calendar.MONTH)) {
            in 2..4 -> "Spring"
            in 5..7 -> "Summer"
            in 8..10 -> "Fall"
            else -> "Winter"
        }
    }

    private fun getTimeOfDay(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> "Morning"
            in 11..15 -> "Midday"
            in 16..20 -> "Evening"
            else -> "Night"
        }
    }

    private fun getSeasonalTemp(season: String, timeOfDay: String, random: Random): Double {
        val base = when (season) {
            "Spring" -> 60.0
            "Summer" -> 85.0
            "Fall" -> 70.0
            else -> 45.0
        }
        val todAdjustment = when (timeOfDay) {
            "Morning" -> -10.0
            "Midday" -> 5.0
            "Evening" -> 0.0
            else -> -15.0
        }
        return base + todAdjustment + random.nextDouble(-5.0, 5.0)
    }

    private fun getSeasonalWaterTemp(season: String, random: Random): Double {
        return when (season) {
            "Spring" -> random.nextDouble(55.0, 68.0)
            "Summer" -> random.nextDouble(75.0, 88.0)
            "Fall" -> random.nextDouble(60.0, 75.0)
            else -> random.nextDouble(42.0, 55.0)
        }
    }

    private fun getRealisticLength(species: String, random: Random): Double {
        return when (species) {
            "Channel Catfish" -> random.nextDouble(12.0, 28.0)
            "Blue Catfish" -> random.nextDouble(18.0, 45.0)
            "Flathead Catfish" -> random.nextDouble(20.0, 42.0)
            "Largemouth Bass" -> random.nextDouble(12.0, 22.0)
            "Striped Bass" -> random.nextDouble(18.0, 32.0)
            "Common Carp" -> random.nextDouble(18.0, 34.0)
            "Bluegill" -> random.nextDouble(6.0, 10.0)
            "White Perch" -> random.nextDouble(7.0, 13.0)
            else -> random.nextDouble(10.0, 25.0)
        }
    }

    private fun getRealisticWeight(species: String, length: Double, random: Random): Double {
        // Very rough length-to-weight approximations
        val factor = when (species) {
            "Blue Catfish", "Flathead Catfish" -> 0.0006
            "Channel Catfish" -> 0.0005
            "Largemouth Bass" -> 0.0004
            else -> 0.0003
        }
        val exponent = if (species.contains("Catfish")) 3.1 else 3.0
        return (factor * Math.pow(length, exponent)) * random.nextDouble(0.8, 1.2)
    }

    private fun getRealisticDepth(species: String, timeOfDay: String, random: Random): Double {
        val baseDepth = if (species.contains("Catfish")) {
            if (species == "Blue Catfish") 22.0 else 10.0
        } else 5.0
        
        val todAdj = when (timeOfDay) {
            "Night", "Evening" -> -5.0
            "Midday" -> 10.0
            else -> 0.0
        }
        return (baseDepth + todAdj + random.nextDouble(-3.0, 5.0)).coerceAtLeast(2.0)
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
