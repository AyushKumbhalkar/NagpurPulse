//java/com/nagpurpulse/ui/theme/TypographyProvider.kt

package com.nagpurpulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import com.nagpurpulse.R
import com.nagpurpulse.ui.preferences.PreferenceManager

/** Nunito for Latin text. Devanagari glyphs fall back to the system font automatically. */
private val NunitoFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

private fun TextStyle.scale(
    fontScale: Float,
    lineBoost: Float,
    family: FontFamily
): TextStyle {
    return copy(
        fontFamily = family,
        fontSize = fontSize * fontScale,
        lineHeight = if (lineHeight.isSpecified) lineHeight * fontScale * lineBoost else lineHeight
    )
}

@Composable
fun scaledTypography(): Typography {

    val scale = PreferenceManager.textScale
    val language = LocalConfiguration.current.locales.get(0)?.language
    // Devanagari (Hindi / Marathi) has tall matras — give it a little more line height.
    val lineBoost = remember(language) {
        if (language == "hi" || language == "mr") 1.14f else 1f
    }
    val f = NunitoFamily

    return Typography.copy(
        displayLarge = Typography.displayLarge.scale(scale, lineBoost, f),
        headlineLarge = Typography.headlineLarge.scale(scale, lineBoost, f),
        headlineMedium = Typography.headlineMedium.scale(scale, lineBoost, f),
        headlineSmall = Typography.headlineSmall.scale(scale, lineBoost, f),
        titleLarge = Typography.titleLarge.scale(scale, lineBoost, f),
        titleMedium = Typography.titleMedium.scale(scale, lineBoost, f),
        titleSmall = Typography.titleSmall.scale(scale, lineBoost, f),
        bodyLarge = Typography.bodyLarge.scale(scale, lineBoost, f),
        bodyMedium = Typography.bodyMedium.scale(scale, lineBoost, f),
        bodySmall = Typography.bodySmall.scale(scale, lineBoost, f),
        labelLarge = Typography.labelLarge.scale(scale, lineBoost, f),
        labelMedium = Typography.labelMedium.scale(scale, lineBoost, f),
        labelSmall = Typography.labelSmall.scale(scale, lineBoost, f)
    )
}
