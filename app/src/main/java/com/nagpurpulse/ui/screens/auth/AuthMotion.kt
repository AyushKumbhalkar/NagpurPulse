package com.nagpurpulse.ui.screens.auth

import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    var reduced by remember { mutableStateOf(preview || !android.animation.ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(context, preview) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = !android.animation.ValueAnimator.areAnimatorsEnabled()
            }
        }
        if (!preview) context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer
        )
        onDispose { if (!preview) context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}

@Composable
internal fun AuthEntrance(index: Int, skip: Boolean = false, content: @Composable () -> Unit) {
    val reduced = rememberReduceMotion()
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (skip || reduced || played) 1f else 0f) }
    LaunchedEffect(reduced, skip) {
        if (reduced || skip || played) progress.snapTo(1f) else {
            delay(80L + index * 90L)
            progress.animateTo(1f, tween(380))
            played = true
        }
    }
    Box(Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 16.dp.toPx()
    }) { content() }
}

/** Very slow zoom-in/zoom-out so illustrations feel alive. Static when animations are off. */
internal fun Modifier.slowBreathing(max: Float = 1.03f, originY: Float = 1f): Modifier = composed {
    val reduce = rememberReduceMotion()
    val scale by rememberInfiniteTransition(label = "breathing").animateFloat(
        1f, max, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "breathe"
    )
    graphicsLayer {
        val sc = if (reduce) 1f else scale
        scaleX = sc; scaleY = sc
        transformOrigin = TransformOrigin(0.5f, originY)
    }
}
