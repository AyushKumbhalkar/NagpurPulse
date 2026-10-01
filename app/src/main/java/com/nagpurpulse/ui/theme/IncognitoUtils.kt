package com.nagpurpulse.ui.theme

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.ui.graphics.Color

private val avatarColors = listOf(
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFFEAB308),
    Color(0xFF22C55E),
    Color(0xFF06B6D4),
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899)
)

private val avatarIcons = listOf(
    Icons.Filled.AccountCircle,
    Icons.Filled.Face,
    Icons.Filled.Person,
    Icons.Filled.PersonOutline
)

fun incognitoColor(seed: String): Color {
    return avatarColors[
        kotlin.math.abs(seed.hashCode()) % avatarColors.size
    ]
}

fun incognitoIcon(seed: String): ImageVector {
    return avatarIcons[
        kotlin.math.abs(seed.hashCode()) % avatarIcons.size
    ]
}