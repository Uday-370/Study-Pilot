package com.example.studysmart.presentation.dashboard

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import androidx.lifecycle.viewModelScope
import com.example.studysmart.domain.model.Subject
import com.example.studysmart.domain.model.Task
import com.example.studysmart.domain.repository.SessionRepository
import com.example.studysmart.domain.repository.SubjectRepository
import com.example.studysmart.domain.repository.TaskRepository
import com.example.studysmart.util.SnackbarEvent
import com.example.studysmart.util.StreakManager
import com.example.studysmart.util.UserPreferences
import com.example.studysmart.util.toHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository,
    private val streakManager: StreakManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = combine(
        _state,
        subjectRepository.getTotalSubjectCount(),
        subjectRepository.getTotalGoalHours(),
        subjectRepository.getAllSubjects(),
        sessionRepository.getTotalSessionsDuration()
    ) { state, subjectCount, goalHours, subjects, totalSessionDuration ->
        state.copy(
            totalSubjectCount = subjectCount,
            totalGoalStudyHours = goalHours,
            subjects = subjects,
            totalStudiedHours = totalSessionDuration.toHours()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = DashboardState()
    )

    val tasks: StateFlow<List<Task>> = taskRepository.getAllUpcomingTasks()
        .map { tasksList ->
            tasksList.sortedWith(
                compareBy<Task> { it.isComplete } // 1st: Incomplete always at top
                    .thenBy { it.dueDate ?: Long.MAX_VALUE } // 2nd: Earliest deadlines first (no dates go to bottom)
                    .thenByDescending { it.priority } // 3rd: Priority acts as the tie-breaker for tasks on the same day
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = emptyList()
        )

    val subjectProgressMap: StateFlow<Map<Int, Float>> = sessionRepository.getAllSessions()
        .map { sessions ->
            sessions.groupBy { it.sessionSubjectId }
                .mapValues { (_, sessionsForSubject) ->
                    // Add .toFloat() here just in case!
                    sessionsForSubject.sumOf { it.duration }.toHours().toFloat()
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val currentStreak = streakManager.currentStreak.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val userName: StateFlow<String?> = userPreferences.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSubjectNameChange -> {
                _state.update { it.copy(subjectName = event.name) }
            }

            is DashboardEvent.OnGoalStudyHoursChange -> {
                _state.update { it.copy(goalStudyHours = event.hours) }
            }

            is DashboardEvent.OnSubjectCardColorChange -> {
                _state.update { it.copy(subjectCardColors = event.colors) }
            }

            DashboardEvent.SaveSubject -> saveSubject()

            is DashboardEvent.OnTaskIsCompleteChange -> {
                updateTask(event.task)
            }

            is DashboardEvent.SaveUserName -> {
                viewModelScope.launch {
                    userPreferences.saveName(event.name)
                }
            }
            // Note: DeleteSession events are now handled in the Account/Session screens
            else -> {}
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                val updatedTask = task.copy(isComplete = !task.isComplete)
                taskRepository.upsertTask(task = updatedTask)

                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(message = "Task updated.")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        "Couldn't update task. ${e.message}",
                        SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun saveSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    subject = Subject(
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() }
                    )
                )
                _state.update {
                    it.copy(
                        subjectName = "",
                        goalStudyHours = "",
                        subjectCardColors = Subject.subjectCardColors.random()
                    )
                }
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(message = "Subject saved successfully")
                )
            } catch (e: Exception) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't save subject. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }
}