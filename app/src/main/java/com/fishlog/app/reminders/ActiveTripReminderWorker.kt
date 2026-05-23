package com.fishlog.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fishlog.app.MainActivity
import com.fishlog.app.R
import com.fishlog.app.data.AppPreferences
import com.fishlog.app.data.FishLogDatabase
import kotlinx.coroutines.flow.first

class ActiveTripReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TripReminder"
        private const val CHANNEL_ID = "fishlog_trip_reminders"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val prefs = AppPreferences(applicationContext)
        if (!prefs.isActiveTripReminderEnabled()) {
            return Result.success()
        }

        val database = FishLogDatabase.getDatabase(applicationContext)
        val activeTrip = database.fishingTripDao().getActiveTrip().first()

        if (activeTrip == null) {
            Log.d(TAG, "No active trip found. Skipping reminder.")
            return Result.success()
        }

        val startTime = activeTrip.startTime
        val now = System.currentTimeMillis()
        val delayHours = prefs.getActiveTripReminderDelayHours()
        val elapsedMillis = now - startTime

        // Confirm active trip has been active at least the configured delay
        // (Minus a small buffer for timing variations)
        val thresholdMillis = (delayHours.toLong() * 60 * 60 * 1000) - 60000
        if (elapsedMillis < thresholdMillis) {
            Log.d(TAG, "Trip started too recently ($elapsedMillis ms). Threshold is $thresholdMillis ms. Skipping.")
            return Result.success()
        }

        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        createNotificationChannel()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Use launcher icon as default
            .setContentTitle("FishLog trip still active")
            .setContentText("Still fishing? End your trip when you’re done so your log stays accurate.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission missing", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Trip Reminders"
            val descriptionText = "Reminders for active fishing trips"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
