package com.nagpurpulse.ui.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DensityManager {

    var density by mutableStateOf("comfortable")

    val cardPadding: Int
        get() = when (density) {
            "compact" -> 10
            "comfortable" -> 16
            "spacious" -> 22
            else -> 16
        }

    val itemSpacing: Int
        get() = when (density) {
            "compact" -> 6
            "comfortable" -> 12
            "spacious" -> 18
            else -> 12
        }
}