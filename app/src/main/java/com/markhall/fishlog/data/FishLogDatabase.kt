package com.markhall.fishlog.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CatchLog::class, FishingTrip::class], version = 6, exportSchema = false)
abstract class FishLogDatabase : RoomDatabase() {
    abstract fun catchLogDao(): CatchLogDao
    abstract fun fishingTripDao(): FishingTripDao

    companion object {
        @Volatile
        private var Instance: FishLogDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN logType TEXT NOT NULL DEFAULT 'CATCH'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN lengthInches REAL")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN weightLbs REAL")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN waterTempF REAL")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN depthFeet REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create fishing_trips table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fishing_trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        name TEXT NOT NULL, 
                        waterBody TEXT NOT NULL, 
                        startTime INTEGER NOT NULL, 
                        endTime INTEGER, 
                        notes TEXT NOT NULL, 
                        latitude REAL, 
                        longitude REAL, 
                        createdAt INTEGER NOT NULL, 
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                // Add tripId column to catch_logs
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN tripId INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN photoUri TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // CatchLog metadata
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN localUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE catch_logs ADD COLUMN backupStatus TEXT NOT NULL DEFAULT 'PENDING_BACKUP'")
                
                // FishingTrip metadata
                db.execSQL("ALTER TABLE fishing_trips ADD COLUMN localUuid TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE fishing_trips ADD COLUMN deletedAt INTEGER")
                db.execSQL("ALTER TABLE fishing_trips ADD COLUMN backupStatus TEXT NOT NULL DEFAULT 'PENDING_BACKUP'")

                // Patch existing records: localUuid, createdAt, updatedAt
                // Room doesn't support generating UUIDs in SQL easily, so we use empty string and patch in ViewModel/App startup or here with a random string if sqlite had one.
                // For now, setting createdAt/updatedAt to timestamp or current time.
                db.execSQL("UPDATE catch_logs SET localUuid = hex(randomblob(16)) WHERE localUuid = ''")
                db.execSQL("UPDATE catch_logs SET createdAt = timestamp, updatedAt = timestamp WHERE createdAt = 0")
                
                db.execSQL("UPDATE fishing_trips SET localUuid = hex(randomblob(16)) WHERE localUuid = ''")
            }
        }

        fun getDatabase(context: Context): FishLogDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, FishLogDatabase::class.java, "fishlog_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
