package com.example.studysmart.presentation.subject

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studysmart.domain.model.Subject
import com.example.studysmart.domain.model.Task
import com.example.studysmart.domain.repository.ResourceRepository
import com.example.studysmart.domain.repository.SessionRepository
import com.example.studysmart.domain.repository.SubjectRepository
import com.example.studysmart.domain.repository.TaskRepository
import com.example.studysmart.presentation.navArgs
import com.example.studysmart.util.SnackbarEvent
import com.example.studysmart.util.toHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    private val resourceRepository: ResourceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navArgs: SubjectScreenNavArgs = savedStateHandle.navArgs()

    private val _state = MutableStateFlow(SubjectState())

    private val tasksFlow = combine(
        taskRepository.getUpcomingTasksForSubject(navArgs.subjectId),
        taskRepository.getCompletedTasksForSubject(navArgs.subjectId)
    ) { upcoming, completed ->

        val sortedUpcoming = upcoming.sortedWith(
            compareBy<Task> { it.isComplete }
                .thenBy { it.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { it.priority }
        )

        val sortedCompleted = completed.sortedWith(
            compareBy<Task> { it.isComplete }
                .thenBy { it.dueDate ?: Long.MAX_VALUE }
                .thenByDescending { it.priority }
        )

        Pair(sortedUpcoming, sortedCompleted)
    }

    // 🚀 FIX 2: We use 'getRecentTenSessionsForSubject' from your original repository!
    val state = combine(
        _state,
        tasksFlow,
        sessionRepository.getRecentTenSessionsForSubject(navArgs.subjectId),
        sessionRepository.getTotalSessionsDurationBySubject(navArgs.subjectId),
        resourceRepository.getResourcesForSubject(navArgs.subjectId)
    ) { state, tasks, recentSessions, totalDuration, resources ->

        val safeTotalDuration = totalDuration ?: 0L
        val studiedHours = safeTotalDuration.toHours()

        val goalStudyHours = state.goalStudyHours.toFloatOrNull() ?: 1f
        val progress = if (goalStudyHours > 0f) {
            (studiedHours / goalStudyHours).coerceIn(0f, 1f)
        } else 0f

        state.copy(
            upcomingTasks = tasks.first,
            completedTasks = tasks.second,
            recentSessions = recentSessions,
            studiedHours = studiedHours,
            progress = progress,
            resources = resources
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = SubjectState()
    )

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    init {
        fetchSubject()
    }

    fun onEvent(event: SubjectEvent) {
        when (event) {
            is SubjectEvent.OnSubjectCardColorChange -> _state.update { it.copy(subjectCardColors = event.color) }
            is SubjectEvent.OnSubjectNameChange -> _state.update { it.copy(subjectName = event.name) }
            is SubjectEvent.OnGoalStudyHoursChange -> _state.update { it.copy(goalStudyHours = event.hours) }
            is SubjectEvent.OnDeleteSessionButtonClick -> _state.update { it.copy(session = event.session) }
            is SubjectEvent.OnTaskIsCompleteChange -> updateTask(event.task)
            SubjectEvent.UpdateSubject -> updateSubject()
            SubjectEvent.DeleteSubject -> deleteSubject()
            SubjectEvent.DeleteSession -> deleteSession()
            else -> {}
        }
    }

    private fun updateSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    subject = Subject(
                        subjectId = state.value.currentSubjectId,
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() }
                    )
                )
                _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Subject updated successfully."))
            } catch (e: Exception) {
                _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Couldn't update subject. ${e.message}", duration = SnackbarDuration.Long))
            }
        }
    }

    private fun fetchSubject() {
        viewModelScope.launch {
            subjectRepository.getSubjectById(navArgs.subjectId)?.let { subject ->
                _state.update {
                    it.copy(
                        subjectName = subject.name,
                        goalStudyHours = subject.goalHours.toString(),
                        subjectCardColors = subject.colors.map { colors -> Color(colors) },
                        currentSubjectId = subject.subjectId
                    )
                }
            }
        }
    }

    private fun deleteSubject() {
        viewModelScope.launch {
            try {
                val currentSubjectId = state.value.currentSubjectId
                if (currentSubjectId != null) {
                    withContext(Dispatchers.IO) {
                        subjectRepository.deleteSubject(subjectId = currentSubjectId)
                    }
                    _snackbarEventFlow.emit(SnackbarEvent.NavigateUp)
                } else {
                    _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "No Subject to delete"))
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Couldn't delete subject. ${e.message}", duration = SnackbarDuration.Long))
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                // 🚀 THE FIX: Calculate the NEW state first
                val isNowComplete = !task.isComplete

                taskRepository.upsertTask(
                    task = task.copy(isComplete = isNowComplete)
                )

                // Now the message matches the new state perfectly
                val message = if (isNowComplete) "Task marked as done." else "Moved back to upcoming."

                _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = message))
            } catch (e: Exception) {
                _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Couldn't update task. ${e.message}", duration = SnackbarDuration.Long))
            }
        }
    }

    private fun deleteSession() {
        viewModelScope.launch {
            try {
                state.value.session?.let {
                    sessionRepository.deleteSession(it)
                    _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Session deleted successfully"))
                }
            } catch (e: Exception) {
                _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Couldn't delete session. ${e.message}", duration = SnackbarDuration.Long))
            }
        }
    }
}