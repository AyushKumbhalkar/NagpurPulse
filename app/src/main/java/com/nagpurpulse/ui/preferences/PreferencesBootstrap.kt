package com.nagpurpulse.ui.preferences

import com.nagpurpulse.data.repository.UserPreferencesRepository
import javax.inject.Inject

class PreferencesBootstrap @Inject constructor(
    private val repository: UserPreferencesRepository
) {

    suspend fun load() {

        repository.getPreferences()
            .getOrNull()
            ?.let {

                PreferenceManager.updateTextSize(
                    it.textSize
                )
            }
    }
}