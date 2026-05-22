package com.fishlog.app.data

import com.fishlog.app.TestDataFactory.sampleCatch
import com.fishlog.app.TestDataFactory.sampleTrip
import org.junit.Assert.*
import org.junit.Test

class BackupJsonTest {

    @Test
    fun backupJsonIncludesTripsAndLogs() {
        val trip = sampleTrip(id = 1)
        val log = sampleCatch(id = 10, tripId = 1)
        
        val json = JsonBackupHelper.createBackup(listOf(log), listOf(trip))
        
        assertTrue(json.contains("FishLog"))
        assertTrue(json.contains("Oak Hollow Lake"))
        assertTrue(json.contains("Channel Catfish"))
    }

    @Test
    fun roundTripBackupRestoration() {
        val trip = sampleTrip(id = 500, name = "Roundtrip Trip")
        val log = sampleCatch(id = 999, species = "Roundtrip Fish")
        
        val json = JsonBackupHelper.createBackup(listOf(log), listOf(trip))
        val restored = JsonBackupHelper.parseBackup(json)
        
        assertEquals(1, restored.trips.size)
        assertEquals(1, restored.catchLogs.size)
        assertEquals("Roundtrip Trip", restored.trips[0].name)
        assertEquals("Roundtrip Fish", restored.catchLogs[0].species)
    }

    @Test
    fun handlesMissingFieldsInImport() {
        // Simulating an older version of JSON without some fields
        val minimalJson = """
        {
          "catchLogs": [
            {
              "id": 1,
              "species": "Bass",
              "length": "12",
              "weight": "1",
              "waterTemp": "70",
              "depth": "5",
              "bait": "worm",
              "notes": ""
            }
          ],
          "trips": []
        }
        """.trimIndent()
        
        val restored = JsonBackupHelper.parseBackup(minimalJson)
        assertEquals(1, restored.catchLogs.size)
        assertEquals("Bass", restored.catchLogs[0].species)
        // Verify default UUID or timestamp if they are initialized in constructor
        assertNotNull(restored.catchLogs[0].localUuid)
    }
}
