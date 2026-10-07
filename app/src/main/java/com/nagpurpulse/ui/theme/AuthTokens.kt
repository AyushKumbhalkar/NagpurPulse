package com.nagpurpulse.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class AuthPalette(
    val background: Color, val ink: Color, val muted: Color,
    val sand: Color, val surface: Color, val outline: Color,
    val error: Color, val success: Color, val accent: Color
)

object AuthTokens {
    val Saffron = Color(0xFFFF941F)
    val Vermilion = Color(0xFFFF3D1F)
    // Ink on saffron has AA contrast; white on this gradient does not.
    val OnGradient = Color(0xFF14171F)
    val HeadlineGradient = Brush.horizontalGradient(listOf(Color(0xFFA64200), Color(0xFFB3220A)))
    val Gradient = Brush.horizontalGradient(listOf(Saffron, Vermilion))
    val Light = AuthPalette(
        Color(0xFFFFF8F0), Color(0xFF14171F), Color(0xFF55505A),
        Color(0xFFF4E8DA), Color.White, Color(0xFF89796D),
        Color(0xFFB42318), Color(0xFF24683D), Color(0xFFB83A0B)
    )
    val Dark = AuthPalette(
        Color(0xFF171411), Color(0xFFFFF8F0), Color(0xFFD1C3B7),
        Color(0xFF302720), Color(0xFF241F1B), Color(0xFFAA9481),
        Color(0xFFFFB4AB), Color(0xFF90D7A6), Color(0xFFFFAD6B)
    )
}

@Composable
fun authPalette(): AuthPalette = if (LocalIsDarkTheme.current) AuthTokens.Dark else AuthTokens.Light
