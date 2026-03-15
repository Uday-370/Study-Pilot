package com.example.studysmart.presentation.subject

import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.OndemandVideo
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studysmart.R
import com.example.studysmart.domain.model.Resource
import com.example.studysmart.presentation.components.DeleteDialog
import com.example.studysmart.presentation.components.tasksList
import com.example.studysmart.presentation.destinations.SessionScreenRouteDestination
import com.example.studysmart.presentation.destinations.TaskScreenRouteDestination
import com.example.studysmart.presentation.session.StudySessionTimerService
import com.example.studysmart.presentation.session.TimerState
import com.example.studysmart.presentation.task.TaskScreenNavArgs
import com.example.studysmart.util.SnackbarEvent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

data class SubjectScreenNavArgs(val subjectId: Int)

@Destination(navArgsDelegate = SubjectScreenNavArgs::class)
@Composable
fun SubjectScreenRoute(
    navigator: DestinationsNavigator,
    navArgs: SubjectScreenNavArgs,
    timerService: StudySessionTimerService
) {
    val viewModel: SubjectViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val isTimerActiveForSubject = timerService.subjectId.value == navArgs.subjectId &&
            timerService.currentTimerState.value != TimerState.IDLE

    SubjectScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarEvent = viewModel.snackbarEventFlow,
        onBackButtonClick = { navigator.navigateUp() },
        onAddTaskButtonClick = {
            navigator.navigate(TaskScreenRouteDestination(navArgs = TaskScreenNavArgs(taskId = null, subjectId = navArgs.subjectId)))
        },
        onTaskCardClick = { taskId ->
            navigator.navigate(TaskScreenRouteDestination(navArgs = TaskScreenNavArgs(taskId = taskId, subjectId = null)))
        },
        isTimerActiveForSubject = isTimerActiveForSubject,
        onStartSessionClick = {
            navigator.navigate(SessionScreenRouteDestination(subjectId = navArgs.subjectId))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectScreen(
    state: SubjectState,
    onEvent: (SubjectEvent) -> Unit,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onBackButtonClick: () -> Unit,
    onAddTaskButtonClick: () -> Unit,
    onTaskCardClick: (Int?) -> Unit,
    isTimerActiveForSubject: Boolean,
    onStartSessionClick: () -> Unit
) {
    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(key1 = true) {
        snackbarEvent.collectLatest { event ->
            when (event) {
                is SnackbarEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                SnackbarEvent.NavigateUp -> onBackButtonClick()
            }
        }
    }

    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        title = "Delete Class",
        bodyText = "All tasks, sessions, and materials for this class will be permanently erased. Proceed?",
        onDismissRequest = { isDeleteDialogOpen = false },
        onConfirmButtonClick = { onEvent(SubjectEvent.DeleteSubject); isDeleteDialogOpen = false }
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
        topBar = {
            LargeTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackButtonClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                title = {
                    Text(
                        text = state.subjectName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-1).sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = {
                        if (isTimerActiveForSubject) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "⚠️ Cannot delete class while a study session is active. Please end the session first.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } else {
                            isDeleteDialogOpen = true
                        }
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTaskButtonClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Task", fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(8.dp))
                ZenFocusCard(
                    hours = state.studiedHours,
                    goal = state.goalStudyHours,
                    progress = state.progress,
                    colors = state.subjectCardColors
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                QuickStartSessionBar(
                    colors = state.subjectCardColors,
                    isActive = isTimerActiveForSubject,
                    onClick = onStartSessionClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = "Materials",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                val resources = remember(state.resources) { state.resources }

                if (resources.isEmpty()) {
                    Text(
                        text = "No files added. Keep your space clean.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(), // 🚀 THE FIX: Ensures proper horizontal spacing
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(resources, key = { it.resourceId ?: it.hashCode() }) { resource ->
                            ZenResourcePill(resource) {
                                try {
                                    if (resource.uri.startsWith("http")) {
                                        uriHandler.openUri(resource.uri)
                                    } else {
                                        val file = File(resource.uri)
                                        if (file.exists()) {
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            val extension = MimeTypeMap.getFileExtensionFromUrl(file.absolutePath)
                                            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "File missing from Vault!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No app found to open this file.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
            tasksList(
                sectionTitle = "Up Next",
                emptyListText = "Mind empty. No upcoming tasks.",
                emptyStateImageRes = R.drawable.task_list1,
                tasks = state.upcomingTasks,
                onCheckBoxClick = { onEvent(SubjectEvent.OnTaskIsCompleteChange(it)) },
                onTaskCardClick = onTaskCardClick
            )

            item { Spacer(modifier = Modifier.height(32.dp)) }
            tasksList(
                sectionTitle = "Done",
                emptyListText = "Nothing finished yet.",
                emptyStateImageRes = R.drawable.subject_done,
                tasks = state.completedTasks,
                onCheckBoxClick = { onEvent(SubjectEvent.OnTaskIsCompleteChange(it)) },
                onTaskCardClick = onTaskCardClick
            )
        }
    }
}

@Composable
private fun QuickStartSessionBar(
    colors: List<Color>,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val accentColor = colors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val buttonText = if (isActive) "Resume Active Session" else "Start Focus Session"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(64.dp),
        color = if (isActive) accentColor else MaterialTheme.colorScheme.surface,
        border = if (isActive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        // 🚀 THE FIX: Moved clickable inside the surface so the ripple perfectly matches the rounded corners!
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = buttonText,
                tint = if (isActive) Color.White else accentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ZenFocusCard(hours: Float, goal: String, progress: Float, colors: List<Color>) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val accentColor = colors.firstOrNull() ?: MaterialTheme.colorScheme.primary

    val totalMinutes = (hours * 60).toInt()
    val displayHours = totalMinutes / 60
    val displayMins = totalMinutes % 60

    val timeString = when {
        displayHours > 0 -> "${displayHours}h ${displayMins}m"
        else -> "${displayMins}m"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = "Weekly Focus",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / ${goal}h",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                // 🚀 THE FIX: Reduced from two CircularProgressIndicators down to one by using trackColor
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ZenResourcePill(resource: Resource, onClick: () -> Unit) {
    val isVideo = resource.uri.endsWith(".mp4") || resource.uri.endsWith(".mkv")
    val isLink = resource.uri.startsWith("http")

    val icon = when {
        isLink -> Icons.Rounded.Language
        isVideo -> Icons.Rounded.OndemandVideo
        else -> Icons.Rounded.Description
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = resource.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        }
    }
}