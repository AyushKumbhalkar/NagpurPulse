package com.nagpurpulse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nagpur_pulse_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_USER_ID           = stringPreferencesKey("user_id")
        private val KEY_USERNAME          = stringPreferencesKey("username")
        private val KEY_ONBOARDING_DONE   = booleanPreferencesKey("onboarding_done")
        private val KEY_AREA_SELECTED     = booleanPreferencesKey("area_selected")
        private val KEY_FCM_TOKEN_SAVED   = booleanPreferencesKey("fcm_token_saved")
        private val KEY_NOTIFICATION_PERM = booleanPreferencesKey("notification_permission_asked")
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val username: Flow<String?> = context.dataStore.data.map { it[KEY_USERNAME] }
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val isAreaSelected: Flow<Boolean> = context.dataStore.data.map { it[KEY_AREA_SELECTED] ?: false }
    val isFcmTokenSaved: Flow<Boolean> = context.dataStore.data.map { it[KEY_FCM_TOKEN_SAVED] ?: false }
    val wasNotificationPermissionAsked: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATION_PERM] ?: false }

    suspend fun saveSession(userId: String, username: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID]  = userId
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setAreaSelected(selected: Boolean) {
        context.dataStore.edit { it[KEY_AREA_SELECTED] = selected }
    }

    suspend fun setFcmTokenSaved(saved: Boolean) {
        context.dataStore.edit { it[KEY_FCM_TOKEN_SAVED] = saved }
    }

    suspend fun setNotificationPermissionAsked(asked: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATION_PERM] = asked }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
