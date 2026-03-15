package com.example.studysmart.util // Adjust to your package name!

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Creates the DataStore file in the device's storage
private val Context.dataStore by preferencesDataStore(name = "study_pilot_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // The exact key we use to remember if they are a new user
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("has_completed_onboarding")

    // A flow that actively watches this value
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: false // Default is false (show onboarding)
        }

    // Function to trigger when they click "START FOCUS"
    suspend fun saveOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = true
        }
    }
}