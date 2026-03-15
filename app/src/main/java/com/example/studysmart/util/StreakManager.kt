package com.example.studysmart.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey // 🚀 NEW IMPORT
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Context.dataStore by preferencesDataStore(name = "focus_flow_prefs")

class StreakManager(private val context: Context) {

    private val STREAK_COUNT_KEY = intPreferencesKey("streak_count")
    private val LAST_ACTIVE_DATE_KEY = longPreferencesKey("last_active_date")

    // 🚀 NEW: The flag to prevent spamming the user with review requests
    private val HAS_SEEN_REVIEW_KEY = booleanPreferencesKey("has_seen_review")

    // 🚀 NEW: Exposes the flag to your UI so it knows whether to show the popup
    val hasSeenReviewPrompt: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_SEEN_REVIEW_KEY] ?: false
    }

    // 🚀 NEW: Call this when they interact with the review dialog so it never shows again
    suspend fun markReviewPromptSeen() {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_REVIEW_KEY] = true
        }
    }

    // The UI instantly knows if the streak was broken while they were away
    val currentStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        val savedStreak = preferences[STREAK_COUNT_KEY] ?: 0
        val lastActiveMillis = preferences[LAST_ACTIVE_DATE_KEY] ?: 0L

        if (lastActiveMillis == 0L) {
            0
        } else {
            val lastActiveDate = Instant.ofEpochMilli(lastActiveMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()

            // If the last time they studied was BEFORE yesterday, the streak is dead.
            // Visually show 0 until they complete a new session today.
            if (lastActiveDate.isBefore(today.minusDays(1))) {
                0
            } else {
                savedStreak
            }
        }
    }

    suspend fun recordActivity() {
        val today = LocalDate.now()
        val todayMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        context.dataStore.edit { preferences ->
            val lastActiveMillis = preferences[LAST_ACTIVE_DATE_KEY] ?: 0L
            val currentStreak = preferences[STREAK_COUNT_KEY] ?: 0

            if (lastActiveMillis == 0L) {
                preferences[STREAK_COUNT_KEY] = 1
                preferences[LAST_ACTIVE_DATE_KEY] = todayMillis
                return@edit
            }

            val lastActiveDate = Instant.ofEpochMilli(lastActiveMillis).atZone(ZoneId.systemDefault()).toLocalDate()

            when {
                lastActiveDate == today -> {
                    // Already studied today, do nothing
                }
                lastActiveDate == today.minusDays(1) -> {
                    // Studied yesterday! Increment the streak.
                    preferences[STREAK_COUNT_KEY] = currentStreak + 1
                    preferences[LAST_ACTIVE_DATE_KEY] = todayMillis
                }
                else -> {
                    // Missed a day. Reset the flow to 1 for today's new session.
                    preferences[STREAK_COUNT_KEY] = 1
                    preferences[LAST_ACTIVE_DATE_KEY] = todayMillis
                }
            }
        }
    }
}