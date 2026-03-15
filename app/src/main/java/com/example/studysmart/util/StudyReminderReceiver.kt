package com.example.studysmart.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.studysmart.MainActivity
import com.example.studysmart.R
import com.example.studysmart.domain.repository.SessionRepository
import com.example.studysmart.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@AndroidEntryPoint
class StudyReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("REMINDER_TYPE") ?: return

        // 🚀 THE PREMIUM FIX: Foreground Suppression.
        // If the user is inside the app, DO NOT fire background reminders!
        if (MainActivity.isAppInForeground) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (type) {
                    "STREAK_WARNING" -> checkAndShowSmartReminder(context)
                    "TASK_REMINDER" -> checkAndShowTaskReminder(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun checkAndShowSmartReminder(context: Context) {
        val now = LocalDate.now()
        val startOfTodayMillis = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val allSessions = sessionRepository.getAllSessions().first()

        val studiedSecondsToday = allSessions
            .filter { it.date >= startOfTodayMillis }
            .sumOf { it.duration }

        if (studiedSecondsToday >= 600) return

        val lastSession = allSessions.maxByOrNull { it.date }

        if (lastSession == null) {
            sendNotification(
                context = context,
                title = "Ready to start? 🚀",
                message = "Your vault is waiting. Let's build your first focus streak today!",
                notificationId = 1001
            )
            return
        }

        val lastStudyDate = Instant.ofEpochMilli(lastSession.date).atZone(ZoneId.systemDefault()).toLocalDate()
        val daysAbsent = ChronoUnit.DAYS.between(lastStudyDate, now)

        when (daysAbsent) {
            1L -> {
                val quotes = listOf(
                    "\"One more session!\" - Don't let your streak sink today!",
                    "\"Discipline beats motivation every time.\" - Keep your streak alive!",
                    "\"Push past your limits. PLUS ULTRA!\" - You have 4 hours left to study!"
                )
                sendNotification(context, "⚠️ Streak in Danger!", quotes.random(), 1001)
            }
            3L -> {
                sendNotification(
                    context = context,
                    title = "Even heroes need training ⚔️",
                    message = "\"If you don't take risks, you can't create a future.\" Your vault has been quiet... time to get back to work!",
                    notificationId = 1003
                )
            }
            7L -> {
                sendNotification(
                    context = context,
                    title = "Where have you been? 🌍",
                    message = "\"A goal without a plan is just a wish.\" Let's plan your return today.",
                    notificationId = 1004
                )
            }
        }
    }

    private suspend fun checkAndShowTaskReminder(context: Context) {
        val now = LocalDate.now()
        val startOfTodayMillis = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfTomorrowMillis = now.plusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        val allTasks = taskRepository.getAllTasks().first()

        val pendingTasks = allTasks.filter { task ->
            !task.isComplete && task.dueDate != null && task.dueDate in startOfTodayMillis..endOfTomorrowMillis
        }

        if (pendingTasks.isEmpty()) return

        val taskCount = pendingTasks.size
        val dynamicMessage = if (taskCount == 1) {
            "\"A goal without a plan is just a wish.\" You have 1 task due soon. Knock it out!"
        } else {
            "\"A goal without a plan is just a wish.\" You have $taskCount tasks due soon. Time to focus!"
        }

        sendNotification(
            context = context,
            title = "📝 Tasks Due Soon",
            message = dynamicMessage,
            notificationId = 1002
        )
    }

    private fun sendNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "REMINDER_CHANNEL",
                "Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 🚀 THE AESTHETIC FIX: Matches the exact minimalist look of your Session Timer!
        val notification = NotificationCompat.Builder(context, "REMINDER_CHANNEL")
            .setSmallIcon(R.drawable.study_pilot)
            .setColor(android.graphics.Color.parseColor("#FFFFFF")) // Pure White
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}