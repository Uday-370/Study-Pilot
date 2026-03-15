package com.example.studysmart.presentation.session

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studysmart.R
import com.example.studysmart.presentation.components.SubjectListBottomSheet
import com.example.studysmart.util.Constants.ACTION_SERVICE_CANCEL
import com.example.studysmart.util.Constants.ACTION_SERVICE_START
import com.example.studysmart.util.Constants.ACTION_SERVICE_STOP
import com.example.studysmart.util.ServiceHelper
import com.example.studysmart.util.SnackbarEvent
import com.example.studysmart.util.TimerMode
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Destination(
    deepLinks = [DeepLink(action = Intent.ACTION_VIEW, uriPattern = "study_smart://dashboard/session")]
)
@Composable
fun SessionScreenRoute(
    subjectId: Int = -1,
    navigator: DestinationsNavigator,
    timerService: StudySessionTimerService
) {
    val viewModel: SessionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = subjectId, key2 = state.subjects) {
        if (subjectId != -1 && timerService.currentTimerState.value == TimerState.IDLE) {
            state.subjects.find { it.subjectId == subjectId }?.let { targetSubject ->
                timerService.subjectId.value = targetSubject.subjectId
                viewModel.onEvent(SessionEvent.OnRelatedSubjectChange(targetSubject))
            }
        }
    }

    SessionScreen(
        state = state,
        snackbarEvent = viewModel.snackbarEventFlow,
        onEvent = viewModel::onEvent,
        onBackButtonClick = { navigator.navigateUp() },
        timerService = timerService
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreen(
    state: SessionState,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onEvent: (SessionEvent) -> Unit,
    onBackButtonClick: () -> Unit,
    timerService: StudySessionTimerService
) {
    val hours by timerService.hours
    val minutes by timerService.minutes
    val seconds by timerService.seconds
    val currentTimerState by timerService.currentTimerState
    val isTargetReached by timerService.isTargetReached

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var isBottomSheetOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentMode by rememberSaveable { mutableStateOf(timerService.currentTimerMode.value) }

    // Ã°Å¸Å¡â‚¬ NEW: Custom Pomodoro Time State
    var isTimePickerOpen by rememberSaveable { mutableStateOf(false) }
    var pomodoroMinutes by rememberSaveable { mutableIntStateOf(25) }

    LaunchedEffect(timerService.currentTimerMode.value, currentTimerState) {
        if (currentTimerState != TimerState.IDLE) {
            currentMode = timerService.currentTimerMode.value
        }
    }

    LaunchedEffect(isTargetReached) {
        if (isTargetReached) {
            snackbarHostState.showSnackbar(
                message = "Ã°Å¸Å½Â¯ Target reached! You're in the flow, keep going!",
                duration = SnackbarDuration.Long
            )
        }
    }

    LaunchedEffect(key1 = true) {
        snackbarEvent.collectLatest { event ->
            when (event) {
                is SnackbarEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message, duration = event.duration)
                }
                SnackbarEvent.NavigateUp -> {}
            }
        }
    }

    LaunchedEffect(key1 = state.subjects) {
        val subjectId = timerService.subjectId.value
        onEvent(
            SessionEvent.UpdateSubjectIdAndRelatedSubject(
                subjectId = subjectId,
                relatedToSubject = state.subjects.find { it.subjectId == subjectId }?.name
            )
        )
    }

    // Ã°Å¸Å¡â‚¬ NEW: The Time Picker Dialog
    if (isTimePickerOpen) {
        var tempMinutes by remember { mutableStateOf(pomodoroMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { isTimePickerOpen = false },
            title = { Text("Set Focus Time", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Duration in minutes (Min: 5 mins)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempMinutes,
                        onValueChange = { tempMinutes = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = tempMinutes.toIntOrNull() ?: 25
                    pomodoroMinutes = parsed.coerceAtLeast(5) // Forces minimum of 5 minutes
                    isTimePickerOpen = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isTimePickerOpen = false }) { Text("Cancel") }
            }
        )
    }

    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottomSheetOpen,
        subjects = state.subjects,
        onDismissRequest = { isBottomSheetOpen = false },
        onSubjectClicked = { subject ->
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) isBottomSheetOpen = false
            }
            onEvent(SessionEvent.OnRelatedSubjectChange(subject))
        }
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(16.dp),
                    snackbarData = data
                )
            }
        },
        topBar = { SessionScreenTopBar(onBackButtonClick = onBackButtonClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                ModeSelector(
                    currentMode = currentMode,
                    onModeChange = { currentMode = it },
                    isEnabled = currentTimerState == TimerState.IDLE
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Ã°Å¸Å¡â‚¬ FIX: Display the custom picked time when idle!
                val displayHours = if (currentTimerState == TimerState.IDLE && currentMode == TimerMode.POMODORO) (pomodoroMinutes / 60).toString().padStart(2, '0') else hours
                val displayMinutes = if (currentTimerState == TimerState.IDLE && currentMode == TimerMode.POMODORO) (pomodoroMinutes % 60).toString().padStart(2, '0') else minutes
                val displaySeconds = if (currentTimerState == TimerState.IDLE && currentMode == TimerMode.POMODORO) "00" else seconds

                TimerSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    hours = displayHours,
                    minutes = displayMinutes,
                    seconds = displaySeconds,
                    isClickable = currentTimerState == TimerState.IDLE && currentMode == TimerMode.POMODORO,
                    onClick = { isTimePickerOpen = true }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            item {
                RelatedToSubjectSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    relatedToSubject = state.relatedToSubject ?: "Select Subject",
                    selectSubjectButtonClick = { isBottomSheetOpen = true },
                    isEnabled = seconds == "00" && currentTimerState != TimerState.STARTED
                )
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }

            item {
                ButtonsSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    startButtonClick = {
                        if (state.subjectId != null && state.relatedToSubject != null) {
                            ServiceHelper.triggerForegroundService(
                                context = context,
                                action = if (currentTimerState == TimerState.STARTED) ACTION_SERVICE_STOP else ACTION_SERVICE_START,
                                mode = currentMode,
                                durationSeconds = pomodoroMinutes * 60
                            )
                            timerService.subjectId.value = state.subjectId
                            timerService.currentSubjectName.value = state.relatedToSubject!!
                        } else {
                            onEvent(SessionEvent.NotifyToUpdateSubject)
                        }
                    },
                    cancelButtonClick = {
                        // The 'X' button just cancels and resets the timer without saving
                        ServiceHelper.triggerForegroundService(context = context, action = ACTION_SERVICE_CANCEL)
                    },
                    finishButtonClick = {
                        // Ã°Å¸Å¡â‚¬ THE FIX: Check if they have studied for at least 5 mins (300 seconds)
                        if (timerService.studiedSeconds < 300) {
                            // If under 5 mins, block the save and show the warning
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Ã¢Å¡Â Ã¯Â¸Â Session too short! Focus for at least 5 minutes to save.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            // If 5 mins or more, tell the service to save, and go back to Dashboard
                            ServiceHelper.triggerForegroundService(context = context, action = ACTION_SERVICE_CANCEL)
                            onBackButtonClick()
                        }
                    },
                    timerState = currentTimerState
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(
    currentMode: TimerMode,
    onModeChange: (TimerMode) -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(if (currentMode == TimerMode.STOPWATCH) MaterialTheme.colorScheme.surface else Color.Transparent)
                .clickable(enabled = isEnabled) { onModeChange(TimerMode.STOPWATCH) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Stopwatch",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (currentMode == TimerMode.STOPWATCH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(if (currentMode == TimerMode.POMODORO) MaterialTheme.colorScheme.surface else Color.Transparent)
                .clickable(enabled = isEnabled) { onModeChange(TimerMode.POMODORO) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Countdown",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (currentMode == TimerMode.POMODORO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionScreenTopBar(onBackButtonClick: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        title = {
            Text(
                text = "Focus Session",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun TimerSection(
    modifier: Modifier,
    hours: String,
    minutes: String,
    seconds: String,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f), shape = CircleShape)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
                .border(width = 8.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), shape = CircleShape)
                .clip(CircleShape) // Ã°Å¸Å¡â‚¬ FIX: Keeps the click ripple beautifully round
                .clickable(enabled = isClickable) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                AnimatedTimerText(time = hours)
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                AnimatedTimerText(time = minutes)
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                AnimatedTimerText(time = seconds)
            }
        }
    }
}

@Composable
private fun AnimatedTimerText(time: String) {
    AnimatedContent(
        targetState = time,
        label = "timerText",
        transitionSpec = { timerTextAnimation() }
    ) { digits ->
        Text(
            text = digits,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 50.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RelatedToSubjectSection(
    modifier: Modifier,
    relatedToSubject: String,
    selectSubjectButtonClick: () -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(enabled = isEnabled) { selectSubjectButtonClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Currently Studying",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = relatedToSubject,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (isEnabled) {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Subject",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ButtonsSection(
    modifier: Modifier,
    startButtonClick: () -> Unit,
    cancelButtonClick: () -> Unit,
    finishButtonClick: () -> Unit,
    timerState: TimerState
) {
    val isRunning = timerState == TimerState.STARTED
    val isPaused = timerState == TimerState.STOPPED

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = isPaused) {
            IconButton(
                onClick = cancelButtonClick,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(56.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
            }
        }

        val gradientColors = if (isRunning) {
            listOf(Color(0xFFD48A88), Color(0xFFE4A9A8))
        } else {
            listOf(Color(0xFF6B8A7A), Color(0xFF8DAA9D))
        }

        Box(
            modifier = Modifier
                .height(72.dp)
                .weight(1f)
                .clip(CircleShape)
                .background(brush = Brush.horizontalGradient(gradientColors))
                .clickable { startButtonClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (timerState) {
                        TimerState.IDLE -> "Start Session"
                        TimerState.STARTED -> "Pause"
                        TimerState.STOPPED -> "Resume"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        AnimatedVisibility(visible = isPaused) {
            IconButton(
                onClick = finishButtonClick,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(56.dp)
                    .background(color = MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Finish", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun timerTextAnimation(duration: Int = 400): ContentTransform {
    return slideInVertically(animationSpec = tween(duration)) { fullHeight -> fullHeight } +
            fadeIn(animationSpec = tween(duration)) togetherWith
            slideOutVertically(animationSpec = tween(duration)) { fullHeight -> -fullHeight } +
            fadeOut(animationSpec = tween(duration))
}