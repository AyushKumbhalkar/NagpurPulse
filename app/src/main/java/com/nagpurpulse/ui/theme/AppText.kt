// ui/theme/AppText.kt

package com.nagpurpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

object AppText {

    val DisplayLarge
        @Composable get() = MaterialTheme.typography.displayLarge

    val HeadlineLarge
        @Composable get() = MaterialTheme.typography.headlineLarge

    val HeadlineMedium
        @Composable get() = MaterialTheme.typography.headlineMedium

    val TitleLarge
        @Composable get() = MaterialTheme.typography.titleLarge

    val TitleMedium
        @Composable get() = MaterialTheme.typography.titleMedium

    val BodyLarge
        @Composable get() = MaterialTheme.typography.bodyLarge

    val BodyMedium
        @Composable get() = MaterialTheme.typography.bodyMedium

    val BodySmall
        @Composable get() = MaterialTheme.typography.bodySmall

    val LabelLarge
        @Composable get() = MaterialTheme.typography.labelLarge

    val LabelMedium
        @Composable get() = MaterialTheme.typography.labelMedium

    val LabelSmall
        @Composable get() = MaterialTheme.typography.labelSmall
}