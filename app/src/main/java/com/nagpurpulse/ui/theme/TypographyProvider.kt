//java/com/nagpurpulse/ui/theme/TypographyProvider.kt

package com.nagpurpulse.ui.theme


import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.nagpurpulse.ui.preferences.PreferenceManager

private fun TextStyle.scale(
    multiplier: Float
): TextStyle {

    return copy(
        fontSize = fontSize * multiplier,
        lineHeight = lineHeight * multiplier
    )
}

@Composable
fun scaledTypography(): Typography {

    val scale = PreferenceManager.textScale

    return Typography.copy(

        displayLarge =
            Typography.displayLarge.scale(scale),

        headlineLarge =
            Typography.headlineLarge.scale(scale),

        headlineMedium =
            Typography.headlineMedium.scale(scale),

        headlineSmall =
            Typography.headlineSmall.scale(scale),

        titleLarge =
            Typography.titleLarge.scale(scale),

        titleMedium =
            Typography.titleMedium.scale(scale),

        titleSmall =
            Typography.titleSmall.scale(scale),

        bodyLarge =
            Typography.bodyLarge.scale(scale),

        bodyMedium =
            Typography.bodyMedium.scale(scale),

        bodySmall =
            Typography.bodySmall.scale(scale),

        labelLarge =
            Typography.labelLarge.scale(scale),

        labelMedium =
            Typography.labelMedium.scale(scale),

        labelSmall =
            Typography.labelSmall.scale(scale)
    )
}