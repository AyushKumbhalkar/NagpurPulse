//java/com/nagpurpulse/ui/theme/AnimatedColorScheme.kt

package com.nagpurpulse.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
fun animateColorScheme(
    target: ColorScheme
): ColorScheme {

    return target.copy(

        primary = animateColorAsState(
            target.primary,
            tween(700)
        ).value,

        background = animateColorAsState(
            target.background,
            tween(700)
        ).value,

        surface = animateColorAsState(
            target.surface,
            tween(700)
        ).value,

        onBackground = animateColorAsState(
            target.onBackground,
            tween(700)
        ).value,

        onSurface = animateColorAsState(
            target.onSurface,
            tween(700)
        ).value,

        outline = animateColorAsState(
            target.outline,
            tween(700)
        ).value
    )
}