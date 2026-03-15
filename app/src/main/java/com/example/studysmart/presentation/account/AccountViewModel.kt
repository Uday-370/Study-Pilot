package com.example.studysmart.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studysmart.domain.repository.SessionRepository
import com.example.studysmart.util.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val userName = userPreferences.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // Moved the Weekly Chart logic here from Dashboard
    val weeklyStudyData: StateFlow<List<Float>> = sessionRepository.getAllSessions()
        .map { sessions ->
            val today = LocalDate.now()
            val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
            last7Days.map { date ->
                val totalSeconds = sessions.filter {
                    Instant.ofEpochMilli(it.date).atZone(ZoneId.systemDefault()).toLocalDate() == date
                }.sumOf { it.duration }
                totalSeconds.toFloat() / 3600f
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0f })

    val weeklyStudyLabels: List<String> = (0..6).map {
        LocalDate.now().minusDays(it.toLong()).format(DateTimeFormatter.ofPattern("EEE"))
    }.reversed()

    fun onNewNameChange(newName: String) {
        viewModelScope.launch {
            userPreferences.saveName(newName)
        }
    }

    val totalStudiedHours: StateFlow<Float> = sessionRepository.getTotalSessionsDuration()
        .map { it.toFloat() / 3600f }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0f
        )

    // 1. Get the Profile Picture
    val profilePicUri: StateFlow<String?> = userPreferences.profilePicUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // 2. Get the Session History (Sorted Newest to Oldest)
    val sessions: StateFlow<List<com.example.studysmart.domain.model.Session>> = sessionRepository.getAllSessions()
        .map { list -> list.sortedByDescending { it.date } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 3. Save the new picture
    fun onProfilePicChange(uri: String) {
        viewModelScope.launch {
            userPreferences.saveProfilePicUri(uri)
        }
    }

    // 4. Delete a session from history
    fun deleteSession(session: com.example.studysmart.domain.model.Session) {
        viewModelScope.launch {
            sessionRepository.deleteSession(session)
        }
    }
}