package com.fishlog.app.reminders

import android.content.Context
import androidx.work.*
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.data.FishLogDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

object ActiveTripReminderScheduler {

    private const val WORK_NAME = "fishlog_active_trip_reminder"

    fun scheduleActiveTripReminder(context: Context, tripId: Long, delayHours: Int) {
        val workRequest = OneTimeWorkRequestBuilder<ActiveTripReminderWorker>()
            .setInitialDelay(delayHours.toLong(), TimeUnit.HOURS)
            .setInputData(workDataOf("tripId" to tripId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelActiveTripReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun rescheduleIfActiveTripExists(context: Context) {
        val prefs = AppPreferences(context)
        if (!prefs.isActiveTripReminderEnabled()) {
            cancelActiveTripReminder(context)
            return
        }

        // We use runBlocking here because this is likely called from app startup 
        // or a context where we need to check the DB quickly.
        runBlocking {
            val database = FishLogDatabase.getDatabase(context)
            val activeTrip = database.fishingTripDao().getActiveTrip().first()
            if (activeTrip != null) {
                val delayHours = prefs.getActiveTripReminderDelayHours()
                val elapsedHours = (System.currentTimeMillis() - activeTrip.startTime) / (1000 * 60 * 60)
                
                if (elapsedHours >= delayHours) {
                    // Trip already exceeded delay, schedule a reminder soon (1 min)
                    val workRequest = OneTimeWorkRequestBuilder<ActiveTripReminderWorker>()
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .setInputData(workDataOf("tripId" to activeTrip.id))
                        .build()

                    WorkManager.getInstance(context).enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.KEEP, // Don't replace if one is already pending
                        workRequest
                    )
                } else {
                    // Schedule for the remaining time
                    val remainingHours = delayHours - elapsedHours
                    scheduleActiveTripReminder(context, activeTrip.id, remainingHours.toInt().coerceAtLeast(1))
                }
            } else {
                cancelActiveTripReminder(context)
            }
        }
    }
}
