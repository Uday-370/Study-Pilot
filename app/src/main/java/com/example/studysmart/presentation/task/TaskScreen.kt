package com.example.studysmart.presentation.task

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.* // 🚀 THE FIX: Imported all Rounded icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studysmart.presentation.components.DeleteDialog
import com.example.studysmart.presentation.components.SubjectListBottomSheet
import com.example.studysmart.presentation.components.TaskCheckBox
import com.example.studysmart.presentation.components.TaskDatePicker
import com.example.studysmart.util.Priority
import com.example.studysmart.util.SnackbarEvent
import com.example.studysmart.util.changeMillisToDateString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TaskScreenNavArgs(
    val taskId: Int?,
    val subjectId: Int?,
    val preSelectedDate: Long? = null
)

@Destination(navArgsDelegate = TaskScreenNavArgs::class)
@Composable
fun TaskScreenRoute(
    navigator: DestinationsNavigator,
    navArgs: TaskScreenNavArgs
) {
    val viewModel: TaskViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = navArgs) {
        if (navArgs.taskId == null || navArgs.taskId == -1) {
            navArgs.preSelectedDate?.let {
                viewModel.onEvent(TaskEvent.OnDateChange(it))
            }
        }
    }

    TaskScreen(
        state = state,
        snackbarEvent = viewModel.snackbarEventFlow,
        onEvent = viewModel::onEvent,
        onBackButtonClick = { navigator.navigateUp() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskScreen(
    state: TaskState,
    snackbarEvent: SharedFlow<SnackbarEvent>,
    onEvent: (TaskEvent) -> Unit,
    onBackButtonClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    var isDeleteDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isDatePickerDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isBottomSheetOpen by remember { mutableStateOf(false) }

    // 🚀 THE CRASH FIX: Smart Date Rules
    val initialDate = state.dueDate ?: Instant.now().toEpochMilli()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selectedDate = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.of("UTC")).toLocalDate()
                val today = LocalDate.now()
                // Allow future dates, OR allow it if it is the currently saved due date (prevents crash on overdue tasks)
                return !selectedDate.isBefore(today) || utcTimeMillis == state.dueDate
            }

            override fun isSelectableYear(year: Int): Boolean {
                val initialYear = Instant.ofEpochMilli(initialDate).atZone(ZoneId.of("UTC")).toLocalDate().year
                return year >= LocalDate.now().year || year == initialYear
            }
        }
    )

    LaunchedEffect(key1 = true) {
        snackbarEvent.collectLatest { event ->
            when (event) {
                is SnackbarEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is SnackbarEvent.NavigateUp -> onBackButtonClick()
            }
        }
    }

    DeleteDialog(
        isOpen = isDeleteDialogOpen,
        title = "Delete Task?",
        bodyText = "Are you sure? This cannot be undone.",
        onDismissRequest = { isDeleteDialogOpen = false },
        onConfirmButtonClick = { onEvent(TaskEvent.DeleteTask); isDeleteDialogOpen = false }
    )

    TaskDatePicker(
        state = datePickerState,
        isOpen = isDatePickerDialogOpen,
        onDismissRequest = { isDatePickerDialogOpen = false },
        onConfirmButtonClicked = {
            onEvent(TaskEvent.OnDateChange(datePickerState.selectedDateMillis))
            isDatePickerDialogOpen = false
        }
    )

    SubjectListBottomSheet(
        sheetState = sheetState,
        isOpen = isBottomSheetOpen,
        subjects = state.subjects,
        onDismissRequest = { isBottomSheetOpen = false },
        onSubjectClicked = { subject ->
            scope.launch { sheetState.hide() }.invokeOnCompletion { isBottomSheetOpen = false }
            onEvent(TaskEvent.OnRelatedSubjectSelect(subject))
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TaskScreenTopBar(
                isTaskExist = state.currentTaskId != null,
                isComplete = state.isTaskComplete,
                checkBoxBorderColor = state.priority.color,
                onBackButtonClick = onBackButtonClick,
                onDeleteButtonClick = { isDeleteDialogOpen = true },
                onCheckBoxClick = { onEvent(TaskEvent.OnIsCompleteChange) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = if (state.currentTaskId == null) "New Task" else "Edit Task",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // 1. TITLE & DESCRIPTION CARD
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextField(
                        value = state.title,
                        onValueChange = { onEvent(TaskEvent.OnTitleChange(it)) },
                        placeholder = { Text("Task Title", style = MaterialTheme.typography.titleLarge) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    TextField(
                        value = state.description,
                        onValueChange = { onEvent(TaskEvent.OnDescriptionChange(it)) },
                        placeholder = { Text("Add notes or details...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. SETTINGS GROUP (Date & Subject)
            Text("Settings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            // Due Date (🚀 FIXED: Clearer null handling)
            SettingRow(
                icon = Icons.Rounded.DateRange, // 🚀 Rounded Icon
                label = "Due Date",
                value = state.dueDate?.changeMillisToDateString() ?: "Select a date",
                onClick = { isDatePickerDialogOpen = true }
            )

            SettingRow(
                icon = Icons.Rounded.LibraryBooks, // 🚀 Rounded Icon
                label = "Related Class",
                value = state.relatedToSubject ?: "Select Subject",
                onClick = { isBottomSheetOpen = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. PRIORITY SECTION
            Text("Priority", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { priority ->
                    PriorityChip(
                        modifier = Modifier.weight(1f),
                        priority = priority,
                        isSelected = state.priority == priority,
                        onClick = { onEvent(TaskEvent.OnPriorityChange(priority)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 4. PREMIUM SAVE BUTTON
            val isButtonEnabled = state.title.isNotBlank() && state.relatedToSubject != null

            Button(
                onClick = { onEvent(TaskEvent.SaveTask) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isButtonEnabled,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text("Confirm Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) // 🚀 Rounded Icon
    }
}

@Composable
private fun PriorityChip(modifier: Modifier, priority: Priority, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(if (isSelected) priority.color else Color.Transparent)
    val textColor by animateColorAsState(if (isSelected) Color.White else priority.color)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, priority.color, CircleShape)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(priority.title, color = textColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskScreenTopBar(
    isTaskExist: Boolean,
    isComplete: Boolean,
    checkBoxBorderColor: Color,
    onBackButtonClick: () -> Unit,
    onDeleteButtonClick: () -> Unit,
    onCheckBoxClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackButtonClick) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null) // 🚀 Rounded Icon
            }
        },
        title = { Text("Task Details", fontWeight = FontWeight.Bold) },
        actions = {
            if (isTaskExist) {
                TaskCheckBox(isComplete = isComplete, borderColor = checkBoxBorderColor, onCheckBoxClick = onCheckBoxClick)
                IconButton(onClick = onDeleteButtonClick) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFD48A88)) // 🚀 Rounded Icon
                }
            }
        }
    )
}