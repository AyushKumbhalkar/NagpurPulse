package com.nagpurpulse.ui.screens.auth

import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
