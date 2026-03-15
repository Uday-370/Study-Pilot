package com.example.studysmart.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studysmart.R
import com.example.studysmart.domain.model.Subject
import com.example.studysmart.domain.model.Task
import com.example.studysmart.presentation.components.TaskCard
import com.example.studysmart.presentation.destinations.SessionScreenRouteDestination
import com.example.studysmart.presentation.destinations.SubjectScreenRouteDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.util.Calendar

@Destination
@Composable
fun DashboardScreenRoute(
    navigator: DestinationsNavigator
) {
    val viewModel: DashboardViewModel = hiltViewModel()

    val state by viewModel.state.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    val subjectProgressMap by viewModel.subjectProgressMap.collectAsStateWithLifecycle()

    var isAddSubjectDialogOpen by rememberSaveable { mutableStateOf(false) }

    if (isAddSubjectDialogOpen) {
        AddSubjectDialog(
            state = state,
            onEvent = viewModel::onEvent,
            onDismiss = { isAddSubjectDialogOpen = false }
        )
    }

    PremiumDashboardScreen(
        studentName = userName ?: "Student",
        streakDays = currentStreak,
        subjects = state.subjects,
        tasks = tasks,
        subjectProgressMap = subjectProgressMap,
        onTaskToggle = { task -> viewModel.onEvent(DashboardEvent.OnTaskIsCompleteChange(task)) },
        onStartSessionClick = { subjectId ->
            navigator.navigate(SessionScreenRouteDestination(subjectId = subjectId ?: -1))
        },
        onSubjectClick = { subjectId ->
            navigator.navigate(SubjectScreenRouteDestination(subjectId = subjectId))
        },
        onAddSubjectClick = { isAddSubjectDialogOpen = true }
    )
}

// --- MAIN SCREEN UI ---
@Composable
fun PremiumDashboardScreen(
    studentName: String,
    streakDays: Int,
    subjects: List<Subject>,
    tasks: List<Task>,
    subjectProgressMap: Map<Int, Float>,
    onTaskToggle: (Task) -> Unit,
    onStartSessionClick: (Int?) -> Unit,
    onSubjectClick: (Int) -> Unit,
    onAddSubjectClick: () -> Unit
) {
    // 🚀 FIXED: Greeting logic runs ONCE securely
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else -> "Good evening,"
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. GREETING & STREAK SECTION
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = greeting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                        Text(text = studentName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔥", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "$streakDays Days", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // 2. THE HERO CARD (Action-First UX)
            item {
                val upNextTask = tasks.firstOrNull { !it.isComplete }
                val featuredSubjectId = upNextTask?.taskSubjectId

                val heroTitle = if (subjects.isEmpty()) {
                    "Set up your first class!"
                } else if (upNextTask != null) {
                    upNextTask.title
                } else {
                    "Open Study Session"
                }

                HeroActionCard(
                    suggestedTaskTitle = heroTitle,
                    onStartSessionClick = {
                        if (subjects.isEmpty()) {
                            onAddSubjectClick()
                        } else {
                            onStartSessionClick(featuredSubjectId)
                        }
                    }
                )
            }

            // 3. HORIZONTAL SUBJECT PROGRESS TRACKERS
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Your Classes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = onAddSubjectClick) { Text("+ Add") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 🚀 FIXED: The Empty Subject Vault with Mascot
            if (subjects.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onAddSubjectClick() } // 🚀 The whole box is clickable!
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your vault is empty.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap to track your first class.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        // 🚀 SMALL STICKMAN CALL TO ACTION
                        Icon(
                            painter = painterResource(id = R.drawable.dashboard_vault),
                            contentDescription = "Add Class",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        items(subjects) { subject ->
                            val studiedHours = subject.subjectId?.let { subjectProgressMap[it] } ?: 0f

                            val progress = if (subject.goalHours > 0f) {
                                (studiedHours / subject.goalHours).coerceIn(0f, 1f)
                            } else 0f

                            SubjectProgressCard(
                                subject = subject,
                                progress = progress,
                                onClick = { subject.subjectId?.let { onSubjectClick(it) } }
                            )
                        }
                    }
                }
            }

            // 4. CLEAN TASK LIST
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Up Next", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 🚀 FIXED: The Giant Zen Mascot for Empty Tasks
            if (tasks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 🚀 MASSIVE ZEN STICKMAN BACKGROUND
                        Icon(
                            painter = painterResource(id = R.drawable.up_next),
                            contentDescription = "Zen State",
                            modifier = Modifier.size(190.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) // Soft watermark opacity
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (subjects.isEmpty()) "Awaiting Your Orders." else "Absolute Flow Achieved.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (subjects.isEmpty()) "Add a class above to start scheduling tasks." else "No upcoming tasks. Enjoy your peace.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(tasks) { task ->
                    TaskCard(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                        task = task,
                        subjectName = subjects.find { it.subjectId == task.taskSubjectId }?.name ?: "General",
                        onCheckBoxClick = { onTaskToggle(task) },
                        onClick = { onStartSessionClick(task.taskSubjectId) }
                    )
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun HeroActionCard(suggestedTaskTitle: String, onStartSessionClick: () -> Unit) {
    val heroGradient = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(160.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(heroGradient).padding(20.dp)) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text("Suggested Focus", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = suggestedTaskTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "Tap play to start session", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
            Surface(
                onClick = onStartSessionClick,
                modifier = Modifier.align(Alignment.BottomEnd).size(48.dp),
                shape = CircleShape, color = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Session", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectProgressCard(subject: Subject, progress: Float, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.size(width = 140.dp, height = 140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(48.dp), strokeWidth = 6.dp, color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(text = subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${subject.goalHours} hrs Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun AddSubjectDialog(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.subjectName,
                    onValueChange = { onEvent(DashboardEvent.OnSubjectNameChange(it)) },
                    label = { Text("Class Name (e.g. Physics)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.goalStudyHours,
                    onValueChange = { onEvent(DashboardEvent.OnGoalStudyHoursChange(it)) },
                    label = { Text("Weekly Goal (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onEvent(DashboardEvent.SaveSubject)
                    onDismiss()
                },
                enabled = state.subjectName.isNotBlank() && state.goalStudyHours.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}