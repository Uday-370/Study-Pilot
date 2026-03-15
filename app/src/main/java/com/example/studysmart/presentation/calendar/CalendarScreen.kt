package com.example.studysmart.presentation.calendar

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.studysmart.R
import com.example.studysmart.domain.model.Task
import com.example.studysmart.presentation.components.tasksList
import com.example.studysmart.presentation.destinations.TaskScreenRouteDestination
import com.example.studysmart.presentation.task.TaskScreenNavArgs
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Destination
@Composable
fun CalendarScreenRoute(
    navigator: DestinationsNavigator
) {
    val viewModel: CalendarViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()

    CalendarScreen(
        state = state,
        tasks = tasks,
        onEvent = viewModel::onEvent,
        onTaskCardClick = { taskId ->
            // CONVERT THE CALENDAR DATE TO MILLISECONDS
            val dateMillis = state.selectedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            // PASS THE DATE TO THE TASK SCREEN
            val navArg = TaskScreenNavArgs(
                taskId = taskId,
                subjectId = null,
                preSelectedDate = if (taskId == null) dateMillis else null
            )
            navigator.navigate(TaskScreenRouteDestination(navArgs = navArg))
        }
    )
}

@Composable
private fun CalendarScreen(
    state: CalendarState,
    tasks: List<Task>,
    onEvent: (CalendarEvent) -> Unit,
    onTaskCardClick: (Int?) -> Unit
) {
    // Generate the next 14 days for the horizontal strip
    val dates = remember {
        val today = LocalDate.now()
        (0..14).map { today.plusDays(it.toLong()) }
    }

    // THE MASTER SORTING ALGORITHM FOR AGENDA
    val sortedTasks = remember(tasks) {
        tasks.sortedWith(
            compareBy<Task> { it.isComplete } // Incomplete at top
                .thenByDescending { it.priority } // Highest priority next
                .thenBy { it.dueDate } // Earliest due date next
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onTaskCardClick(null) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Task"
                    )
                },
                text = { Text(text = "Add Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Agenda,",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Let's plan ahead.",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // The Horizontal Date Strip
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dates) { date ->
                        DateCard(
                            date = date,
                            isSelected = date == state.selectedDate,
                            onClick = { onEvent(CalendarEvent.OnDateSelected(date)) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // The Tasks List for the selected day
            val dateLabel = if (state.selectedDate == LocalDate.now()) "Today's Tasks"
            else state.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd")) + " Tasks"

            tasksList(
                sectionTitle = dateLabel,
                emptyListText = "Nothing scheduled for this day.\nYou have time to relax!",
                emptyStateImageRes = R.drawable.calendar_screen,
                tasks = sortedTasks,
                onCheckBoxClick = { onEvent(CalendarEvent.OnTaskIsCompleteChange(it)) },
                onTaskCardClick = onTaskCardClick
            )

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun DateCard(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
    val dayName = date.format(DateTimeFormatter.ofPattern("EEE"))
    val dayNumber = date.format(DateTimeFormatter.ofPattern("dd"))

    Box(
        modifier = Modifier
            .width(64.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayName,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayNumber,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        }
    }
}