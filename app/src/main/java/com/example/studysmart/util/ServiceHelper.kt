package com.example.studysmart.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.studysmart.MainActivity
import com.example.studysmart.presentation.session.StudySessionTimerService

object ServiceHelper {

    fun clickPendingIntent(context: Context): PendingIntent {
        val clickIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("study_smart://dashboard/session"),
            context,
            MainActivity::class.java
        )

        return PendingIntent.getActivity(
            context,
            Constants.CLICK_REQUEST_CODE,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // 🚀 NEW: Intent to pause the timer from the notification
    fun stopIntent(context: Context): PendingIntent {
        val stopIntent = Intent(context, StudySessionTimerService::class.java).apply {
            action = Constants.ACTION_SERVICE_STOP
        }
        return PendingIntent.getService(
            context,
            101, // Unique request code
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // 🚀 NEW: Intent to resume the timer from the notification
    fun resumeIntent(context: Context): PendingIntent {
        val resumeIntent = Intent(context, StudySessionTimerService::class.java).apply {
            action = Constants.ACTION_SERVICE_START
        }
        return PendingIntent.getService(
            context,
            102,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // 🚀 NEW: Intent to completely cancel/finish the timer from the notification
    fun cancelIntent(context: Context): PendingIntent {
        val cancelIntent = Intent(context, StudySessionTimerService::class.java).apply {
            action = Constants.ACTION_SERVICE_CANCEL
        }
        return PendingIntent.getService(
            context,
            103,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun triggerForegroundService(
        context: Context,
        action: String,
        mode: TimerMode = TimerMode.STOPWATCH,
        durationSeconds: Int = 1500 // Default 25 mins
    ) {
        Intent(context, StudySessionTimerService::class.java).apply {
            this.action = action
            putExtra(Constants.EXTRA_TIMER_MODE, mode.name)
            putExtra(Constants.EXTRA_POMODORO_DURATION, durationSeconds)
            context.startService(this)
        }
    }
}