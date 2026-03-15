package com.example.studysmart.presentation.session

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.PowerManager // 🚀 NEW IMPORT
import android.os.SystemClock
import android.os.VibratorManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.example.studysmart.R
import com.example.studysmart.domain.model.Session
import com.example.studysmart.domain.repository.SessionRepository
import com.example.studysmart.util.Constants.ACTION_SERVICE_CANCEL
import com.example.studysmart.util.Constants.ACTION_SERVICE_START
import com.example.studysmart.util.Constants.ACTION_SERVICE_STOP
import com.example.studysmart.util.Constants.EXTRA_POMODORO_DURATION
import com.example.studysmart.util.Constants.EXTRA_TIMER_MODE
import com.example.studysmart.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.studysmart.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.studysmart.util.Constants.NOTIFICATION_ID
import com.example.studysmart.util.ServiceHelper
import com.example.studysmart.util.StreakManager
import com.example.studysmart.util.TimerMode
import com.example.studysmart.util.pad
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class StudySessionTimerService : Service() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var streakManager: StreakManager

    private val binder = StudySessionTimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null

    // 🚀 THE WAKELOCK: Ensures the timer rings even if the screen is off
    private var wakeLock: PowerManager.WakeLock? = null

    private var timeMillisWhenStarted: Long = 0L
    private var accumulatedTimeMillis: Long = 0L
    private var initialPomodoroDurationMillis: Long = 0L

    var duration: Duration = Duration.ZERO
        private set

    val seconds = mutableStateOf("00")
    val minutes = mutableStateOf("00")
    val hours = mutableStateOf("00")
    val currentTimerState = mutableStateOf(TimerState.IDLE)
    val subjectId = mutableStateOf<Int?>(null)
    val currentTimerMode = mutableStateOf(TimerMode.STOPWATCH)
    val isTargetReached = mutableStateOf(false)
    val currentSubjectName = mutableStateOf("Session")

    var studiedSeconds = 0L
        private set

    private val animeQuotes = listOf(
        "\"A dropout will beat a genius through hard work.\" - Rock Lee",
        "\"If you don't take risks, you can't create a future.\" - Monkey D. Luffy",
        "\"I have to work harder than anyone else to make it!\" - Izuku Midoriya",
        "\"If you don't like your destiny, don't accept it.\" - Naruto Uzumaki",
        "\"A goal without a plan is just a wish.\" - Erwin Smith",
        "\"You can die anytime, but living takes true courage.\" - Kenshin Himura",
        "\"Push past your limits. PLUS ULTRA!\" - Yami Sukehiro"
    )

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modeString = intent?.getStringExtra(EXTRA_TIMER_MODE)

        if (modeString != null) {
            if (modeString == TimerMode.POMODORO.name) {
                currentTimerMode.value = TimerMode.POMODORO
                val initialSeconds = intent.getIntExtra(EXTRA_POMODORO_DURATION, 1500)

                if (currentTimerState.value == TimerState.IDLE) {
                    initialPomodoroDurationMillis = initialSeconds * 1000L
                    duration = initialSeconds.seconds
                    updateTimeUnits()
                    isTargetReached.value = false
                }
            } else {
                currentTimerMode.value = TimerMode.STOPWATCH
            }
        }

        intent?.action.let {
            when (it) {
                ACTION_SERVICE_START -> {
                    if (currentTimerState.value != TimerState.STARTED) {
                        startForegroundService()
                        startTimer { h, m, s -> updateNotification(h, m, s) }
                    }
                }
                ACTION_SERVICE_STOP -> {
                    stopTimer()
                    updateNotification(hours.value, minutes.value, seconds.value)
                }
                ACTION_SERVICE_CANCEL -> saveSessionAndCancel()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // 🚀 NEW: Safely handles acquiring CPU wake lock
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StudySmart:TimerWakeLock")
        }
        if (wakeLock?.isHeld == false) {
            // Adds a 4-hour safety timeout to guarantee it never drains battery forever if app crashes
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
        }
    }

    // 🚀 NEW: Releases lock to save battery
    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            val alarmChannel = NotificationChannel(
                "ALARM_CHANNEL",
                "Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    private fun startForegroundService() {
        createNotificationChannel()

        notificationBuilder
            .setContentTitle("StudyPilot")
            .setContentText("Focus Session Active")
            .setSmallIcon(R.drawable.study_pilot)
            .setColor(android.graphics.Color.parseColor("#FFFFFF"))
            .setContentIntent(ServiceHelper.clickPendingIntent(this))
            .setOnlyAlertOnce(true)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun updateNotification(hours: String, minutes: String, seconds: String) {
        val isRunning = currentTimerState.value == TimerState.STARTED
        notificationBuilder.clearActions()

        if (isRunning) {
            notificationBuilder.addAction(0, "Pause", ServiceHelper.stopIntent(this))
        } else {
            notificationBuilder.addAction(0, "Resume", ServiceHelper.resumeIntent(this))
        }

        notificationBuilder.addAction(0, "End Session", ServiceHelper.cancelIntent(this))

        notificationBuilder
            .setContentTitle("Flow State")
            .setContentText("$hours:$minutes:$seconds")
            .setStyle(null)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun startTimer(onTick: (h: String, m: String, s: String) -> Unit) {
        currentTimerState.value = TimerState.STARTED
        timeMillisWhenStarted = SystemClock.elapsedRealtime()

        // 🚀 THE FIX: Keep CPU awake during active ticking!
        acquireWakeLock()
        timerJob?.cancel()

        timerJob = serviceScope.launch {
            while (isActive) {
                val currentTime = SystemClock.elapsedRealtime()
                val timeSinceStart = currentTime - timeMillisWhenStarted
                val totalElapsedMillis = accumulatedTimeMillis + timeSinceStart

                studiedSeconds = (totalElapsedMillis / 1000)

                if (currentTimerMode.value == TimerMode.STOPWATCH) {
                    if (isTargetReached.value) {
                        duration = (totalElapsedMillis - initialPomodoroDurationMillis).milliseconds
                    } else {
                        duration = totalElapsedMillis.milliseconds
                    }
                } else {
                    val remainingMillis = initialPomodoroDurationMillis - totalElapsedMillis

                    if (remainingMillis <= 0L && !isTargetReached.value) {
                        isTargetReached.value = true
                        triggerTimerComplete()

                        currentTimerMode.value = TimerMode.STOPWATCH
                        duration = Duration.ZERO
                    } else {
                        duration = remainingMillis.milliseconds
                    }
                }

                updateTimeUnits()
                onTick(hours.value, minutes.value, seconds.value)

                delay(1000L)
            }
        }
    }

    private fun triggerTimerComplete() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(1000, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.zen_bell1)
                mediaPlayer?.setOnCompletionListener { it.release() }
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val randomQuote = animeQuotes.random()

        val quoteNotification = NotificationCompat.Builder(this, "ALARM_CHANNEL")
            .setSmallIcon(R.drawable.study_pilot)
            .setColor(android.graphics.Color.parseColor("#FFFFFF"))
            .setContentTitle("Target Reached! 🎯")
            .setContentText("Overtime started.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Overtime started.\n\n$randomQuote"))
            .setContentIntent(ServiceHelper.clickPendingIntent(this))
            .setAutoCancel(true)
            .build()

        notificationManager.notify(9999, quoteNotification)
    }

    private fun saveSessionAndCancel() {
        val durationSeconds = studiedSeconds

        if (durationSeconds >= 300) {
            serviceScope.launch {
                try {
                    sessionRepository.insertSession(
                        Session(
                            sessionSubjectId = subjectId.value ?: -1,
                            relatedToSubject = currentSubjectName.value,
                            date = Instant.now().toEpochMilli(),
                            duration = durationSeconds
                        )
                    )

                    val startOfTodayMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val allSessions = sessionRepository.getAllSessions().first()
                    val totalSecondsToday = allSessions
                        .filter { it.date >= startOfTodayMillis }
                        .sumOf { it.duration }

                    if (totalSecondsToday >= 600) {
                        streakManager.recordActivity()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) { stopAndResetTimer() }
                }
            }
        } else {
            stopAndResetTimer()
        }
    }

    private fun stopAndResetTimer() {
        stopTimer()
        cancelTimer()
        stopForegroundService()
    }

    private fun stopTimer() {
        timerJob?.cancel()

        // 🚀 THE FIX: Let the CPU sleep to save battery while paused!
        releaseWakeLock()

        if (currentTimerState.value == TimerState.STARTED) {
            accumulatedTimeMillis += (SystemClock.elapsedRealtime() - timeMillisWhenStarted)
        }

        currentTimerState.value = TimerState.STOPPED
    }

    private fun cancelTimer() {
        duration = Duration.ZERO
        studiedSeconds = 0L
        accumulatedTimeMillis = 0L
        timeMillisWhenStarted = 0L
        initialPomodoroDurationMillis = 0L

        notificationManager.cancel(9999)

        updateTimeUnits()
        currentTimerState.value = TimerState.IDLE
        isTargetReached.value = false
    }

    private fun stopForegroundService() {
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(9999)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun updateTimeUnits() {
        duration.toComponents { hours, minutes, seconds, _ ->
            this@StudySessionTimerService.hours.value = hours.toInt().pad()
            this@StudySessionTimerService.minutes.value = minutes.pad()
            this@StudySessionTimerService.seconds.value = seconds.pad()
        }
    }

    inner class StudySessionTimerBinder : Binder() {
        fun getService(): StudySessionTimerService = this@StudySessionTimerService
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock() // Absolute safety net
    }
}

enum class TimerState { IDLE, STARTED, STOPPED }