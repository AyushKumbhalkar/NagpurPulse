//java/com/nagpurpulse/ui/preferences/PreferenceManager.kt

package com.nagpurpulse.ui.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object PreferenceManager {

    var textSize by mutableStateOf("medium")

    val textScale: Float
        get() = when (textSize) {

            "small" -> 0.90f

            "large" -> 1.10f

            "extra_large" -> 1.20f

            else -> 1.00f
        }

    fun updateTextSize(size: String) {
        textSize = size
    }
}