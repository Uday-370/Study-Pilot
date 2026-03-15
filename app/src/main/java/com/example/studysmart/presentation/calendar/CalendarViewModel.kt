package com.example.studysmart.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studysmart.domain.model.Task
import com.example.studysmart.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state = _state.asStateFlow()

    // Grab all tasks and filter them based on the currently selected date in the state
    val filteredTasks: StateFlow<List<Task>> = combine(
        taskRepository.getAllTasks(),
        _state
    ) { tasks, state ->
        tasks.filter { task ->
            // Convert task due date (millis) to LocalDate for comparison
            val taskDate = Instant.ofEpochMilli(task.dueDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            taskDate == state.selectedDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onEvent(event: CalendarEvent) {
        when (event) {
            is CalendarEvent.OnDateSelected -> {
                _state.update { it.copy(selectedDate = event.date) }
            }
            is CalendarEvent.OnTaskIsCompleteChange -> {
                viewModelScope.launch {
                    taskRepository.upsertTask(
                        event.task.copy(isComplete = !event.task.isComplete)
                    )
                }
            }
        }
    }
}