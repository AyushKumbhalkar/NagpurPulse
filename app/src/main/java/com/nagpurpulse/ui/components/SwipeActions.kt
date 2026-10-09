// java/com/nagpurpulse/ui/components/SwipeActions.kt
package com.nagpurpulse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.OrangePrimary

/**
 * Swipe a comment to the right to reply to it — the same action as the Reply button, just
 * closer to the thumb. The row always springs back, so the list never changes shape. A
 * light tick fires the moment the swipe is far enough to count, and a confirmation buzz
 * when it is released.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToReply(
    enabled: Boolean,
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }
    val haptic = rememberHaptic()
    val currentOnReply by rememberUpdatedState(onReply)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            if (target == SwipeToDismissBoxValue.StartToEnd) {
                haptic.success()
                currentOnReply()
            }
            false // always snap back
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.28f }
    )

    val progress = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) state.progress else 0f
    val pastThreshold = progress >= 0.28f
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(pastThreshold) {
        if (pastThreshold && !armed) haptic.threshold()
        armed = pastThreshold
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().padding(start = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val p = progress.coerceIn(0f, 1f)
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    contentDescription = stringResource(R.string.comment_reply),
                    tint = OrangePrimary,
                    modifier = Modifier.size(22.dp).graphicsLayer {
                        alpha = p
                        scaleX = 0.6f + 0.6f * p
                        scaleY = 0.6f + 0.6f * p
                    }
                )
            }
        },
        content = { content() }
    )
}
