package com.example.studysmart.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creates a separate DataStore specifically for user settings
private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    private val USER_NAME_KEY = stringPreferencesKey("user_name")

    // Reads the name. If it doesn't exist yet, it returns an empty string ("")
    val userName: Flow<String> = context.userPrefsDataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: ""
    }

    // Saves the name
    suspend fun saveName(name: String) {
        context.userPrefsDataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }

    // Add this right below USER_NAME_KEY
    private val PROFILE_PIC_KEY = stringPreferencesKey("profile_pic_uri")

    // Add this to expose the picture
    val profilePicUri: Flow<String?> = context.userPrefsDataStore.data.map { preferences ->
        preferences[PROFILE_PIC_KEY]
    }

    // Add this to save the picture
    suspend fun saveProfilePicUri(uri: String) {
        context.userPrefsDataStore.edit { preferences ->
            preferences[PROFILE_PIC_KEY] = uri
        }
    }
}