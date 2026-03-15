package com.example.studysmart.domain.repository

import com.example.studysmart.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    suspend fun insertSession(session: Session)

    suspend fun deleteSession(session: Session)

    fun getAllSessions(): Flow<List<Session>>

    fun getRecentFiveSessions(): Flow<List<Session>>

    fun getRecentTenSessionsForSubject(subjectId: Int): Flow<List<Session>>

    fun getTotalSessionsDuration(): Flow<Long>

    fun getTotalSessionsDurationBySubject(subjectId: Int): Flow<Long>

    // 🚀 THE FIX: We added this so the Impl and ViewModel can finally see it!
    fun getRecentSessionsForSubject(subjectId: Int): Flow<List<Session>>
}