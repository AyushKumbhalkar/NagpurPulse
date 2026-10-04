package com.nagpurpulse.ui.preferences

import com.nagpurpulse.data.repository.UserPreferencesRepository
import javax.inject.Inject

class PreferencesBootstrap @Inject constructor(
    private val repository: UserPreferencesRepository
) {

    suspend fun load() {
        val preferences = repository.getPreferences().getOrNull() ?: return

        preferences.textSize
            .takeIf { it in setOf("small", "medium", "large", "extra_large") }
            ?.let(PreferenceManager::updateTextSize)

        preferences.displayDensity
            .takeIf { it in setOf("compact", "comfortable", "spacious") }
            ?.let { DensityManager.density = it }

        preferences.feedStyle
            .takeIf { it == "compact" || it == "expanded" }
            ?.let { FeedLayoutManager.feedStyle = it }
    }
}
