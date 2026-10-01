// java/com/nagpurpulse/ui/theme/ThemeTransitionOverlay.kt

package com.nagpurpulse.ui.theme

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun ThemeTransitionOverlay() {

    if (!ThemeManager.animateThemeChange) return

    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {

        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 250,
                easing = FastOutSlowInEasing
            )
        )

        ThemeManager.animationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (ThemeManager.switchingToLight)
                    Color.White
                else
                    Color.Black
            )
            .graphicsLayer {
                this.alpha = alpha.value
            }
    )
}