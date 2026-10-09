//java/com/nagpurpulse/ui/theme/ThemeViewModel.kt

package com.nagpurpulse.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeManager {

    var isLightTheme by mutableStateOf(true)

    /** When true the app follows the device dark/light setting instead of [isLightTheme]. */
    var followSystem by mutableStateOf(false)

    var animateThemeChange by mutableStateOf(false)

    var switchingToLight by mutableStateOf(true)

    fun toggleTheme(enabled: Boolean) {

        switchingToLight = enabled

        animateThemeChange = true

        isLightTheme = enabled
    }

    fun animationFinished() {
        animateThemeChange = false
    }
}