//This is Theme.kt file

//java/com/nagpurpulse/ui/theme/Theme.kt

package com.nagpurpulse.ui.theme

import com.nagpurpulse.ui.preferences.PreferenceManager
import com.nagpurpulse.ui.theme.scaledTypography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Composition local for theme mode access anywhere ──────────────────────────
val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary          = OrangePrimary,
    onPrimary        = Color.White,
    primaryContainer = OrangeDim,
    onPrimaryContainer = OrangeLight,
    secondary        = OrangeLight,
    onSecondary      = Color.White,
    background       = BackgroundDark,
    onBackground     = TextPrimary,
    surface          = SurfaceOne,
    onSurface        = TextPrimary,
    surfaceVariant   = SurfaceTwo,
    onSurfaceVariant = TextSecondary,
    outline          = DividerColor,
    error            = RedAlert,
    onError          = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary          = OrangePrimary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFE5CC),
    onPrimaryContainer = OrangeDim,
    secondary        = OrangeLight,
    onSecondary      = Color.White,
    background       = BackgroundLight,
    onBackground     = TextPrimaryLight,
    surface          = SurfaceLight,
    onSurface        = TextPrimaryLight,
    surfaceVariant   = SurfaceLightTwo,
    onSurfaceVariant = TextSecondaryLight,
    outline          = DividerLight,
    error            = RedAlert,
    onError          = Color.White,
)

@Composable
fun NagpurPulseTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {

    android.util.Log.d(
        "TEXT_SCALE_TEST",
        "Scale = ${PreferenceManager.textScale}"
    )



    val targetScheme =
        if (darkTheme)
            DarkColorScheme
        else
            LightColorScheme

    val colorScheme = targetScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        android.util.Log.d(
            "THEME_COMPOSE",
            "darkTheme = $darkTheme"
        )

        MaterialTheme(
            colorScheme = colorScheme,
            typography  = scaledTypography(),
            content     = content
        )
    }
}

