package com.example.studysmart.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class NotificationAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDailyReminders() {
        // Schedule Streak Warning for 8:00 PM (20:00)
        scheduleAlarm(hour = 20, minute = 0, type = "STREAK_WARNING", requestCode = 100)

        // Schedule Task Reminder for 9:00 AM (09:00)
        scheduleAlarm(hour = 9, minute = 0, type = "TASK_REMINDER", requestCode = 101)
    }

    private fun scheduleAlarm(hour: Int, minute: Int, type: String, requestCode: Int) {
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode, // Unique ID for each alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed today, schedule it for tomorrow instead!
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // setInexactRepeating is battery-safe and doesn't require scary Android 14 permissions
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // Repeats exactly every 24 hours
            pendingIntent
        )
    }

    // 🚀 TEMPORARY DEVELOPER TEST: Fires 10 seconds from right now!
    // Notice how it is INSIDE the class now!
    fun testAlarmNow() {
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", "STREAK_WARNING") // Tests the Anime quote!
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Forces Android to fire this in exactly 10,000 milliseconds (10 seconds)
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 10000,
            pendingIntent
        )
    }
}