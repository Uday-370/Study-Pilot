package com.example.studysmart.presentation.session

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studysmart.domain.model.Session
import com.example.studysmart.domain.repository.SessionRepository
import com.example.studysmart.domain.repository.SubjectRepository
import com.example.studysmart.util.SnackbarEvent
import com.example.studysmart.util.StreakManager
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
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository,
    private val streakManager: StreakManager
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state = combine(
        _state,
        subjectRepository.getAllSubjects(),
        sessionRepository.getAllSessions()
    ) { state, subjects, sessions ->
        state.copy(
            subjects = subjects,
            sessions = sessions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = SessionState()
    )

    private val _snackbarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackbarEventFlow = _snackbarEventFlow.asSharedFlow()

    fun onEvent(event: SessionEvent) {
        when (event) {
            SessionEvent.NotifyToUpdateSubject -> notifyToUpdateSubject()
            SessionEvent.DeleteSession -> deleteSession()
            is SessionEvent.OnDeleteSessionButtonClick -> {
                _state.update { it.copy(session = event.session) }
            }
            is SessionEvent.OnRelatedSubjectChange -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.subject.name,
                        subjectId = event.subject.subjectId
                    )
                }
            }
            is SessionEvent.SaveSession -> insertSession(event.duration)
            is SessionEvent.UpdateSubjectIdAndRelatedSubject -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.relatedToSubject,
                        subjectId = event.subjectId
                    )
                }
            }
        }
    }

    private fun notifyToUpdateSubject() {
        viewModelScope.launch {
            if (state.value.subjectId == null || state.value.relatedToSubject == null) {
                _snackbarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Please select a subject before starting the session."
                    )
                )
            }
        }
    }

    private fun deleteSession() {
        // 🚀 THE FIX: Moved database operation to the IO thread to prevent UI freezing
        viewModelScope.launch(Dispatchers.IO) {
            try {
                state.value.session?.let {
                    sessionRepository.deleteSession(it)
                    withContext(Dispatchers.Main) {
                        _snackbarEventFlow.emit(SnackbarEvent.ShowSnackbar(message = "Session deleted."))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Couldn't delete session. ${e.message}",
                            duration = SnackbarDuration.Long
                        )
                    )
                }
            }
        }
    }

    private fun insertSession(duration: Long) {
        // 🚀 THE FIX: Moved database insertion to the IO thread
        viewModelScope.launch(Dispatchers.IO) {
            if (duration < 36) {
                withContext(Dispatchers.Main) {
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Session too short. Minimum 36s required to save."
                        )
                    )
                }
                return@launch
            }
            try {
                sessionRepository.insertSession(
                    session = Session(
                        sessionSubjectId = state.value.subjectId ?: -1,
                        relatedToSubject = state.value.relatedToSubject ?: "General",
                        date = Instant.now().toEpochMilli(),
                        duration = duration
                    )
                )

                // THE REWARD TRIGGER:
                // Only grow the flow if the user focused for at least 10 minutes (600 seconds)
                if (duration >= 600) {
                    streakManager.recordActivity()
                }

                withContext(Dispatchers.Main) {
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(message = "Session logged successfully!")
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _snackbarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Couldn't save session. ${e.message}",
                            duration = SnackbarDuration.Long
                        )
                    )
                }
            }
        }
    }
}