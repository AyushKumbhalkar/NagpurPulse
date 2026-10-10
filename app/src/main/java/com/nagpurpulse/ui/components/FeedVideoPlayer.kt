// java/com/nagpurpulse/ui/components/FeedVideoPlayer.kt

package com.nagpurpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/** Only one feed video plays at a time; starting another stops the previous one. */
object FeedVideoCoordinator {
    var activeId by mutableStateOf<String?>(null)
}

private val FeedOrange = Color(0xFFFF7A00)

/**
 * Video inside a feed card. Tapping the cover plays it right there (no navigation);
 * the small expand button opens the thread. The player is released as soon as the card scrolls away.
 */
@Composable
fun FeedVideoPlayer(
    postId: String,
    videoUrl: String,
    thumbnailUrl: String?,
    durationMs: Int?,
    onOpenPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = FeedVideoCoordinator.activeId == postId

    Box(modifier.fillMaxWidth()) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AdaptivePostImage(
                imageUrl = thumbnailUrl,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 0.dp,
                showExpandHintWhenCropped = false
            )
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black))
        }

        if (!isActive) {
            Box(
                Modifier
                    .matchParentSize()
                    .clickable { FeedVideoCoordinator.activeId = postId }
            ) {
                VideoBadgeOverlay(durationMs)
            }
            OpenThreadButton(onOpenPost, Modifier.align(Alignment.TopEnd))
        } else {
            ActiveFeedVideo(
                postId = postId,
                videoUrl = videoUrl,
                onOpenPost = onOpenPost,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun OpenThreadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(10.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.OpenInFull, contentDescription = "Open post",
            tint = Color.White, modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ActiveFeedVideo(
    postId: String,
    videoUrl: String,
    onOpenPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var muted by remember { mutableStateOf(false) }
    var wantsPlay by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(muted, player) { player.volume = if (muted) 0f else 1f }

    LaunchedEffect(player) {
        while (true) {
            val d = player.duration
            progress = if (d > 0) (player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(250)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantsPlay = playWhenReady
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                buffering = playbackState == Player.STATE_BUFFERING
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            if (FeedVideoCoordinator.activeId == postId) FeedVideoCoordinator.activeId = null
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // Tap anywhere on the video: pause / resume
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (player.playWhenReady) player.pause() else player.play() }
        )

        if (!wantsPlay) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        } else if (buffering) {
            CircularProgressIndicator(
                color = Color.White, strokeWidth = 3.dp,
                modifier = Modifier.align(Alignment.Center).size(34.dp)
            )
        }

        OpenThreadButton(onOpenPost, Modifier.align(Alignment.TopEnd))

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 10.dp, end = 10.dp, bottom = 12.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { muted = !muted },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (muted) "Unmute" else "Mute",
                tint = Color.White, modifier = Modifier.size(18.dp)
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            color = FeedOrange,
            trackColor = Color.White.copy(alpha = 0.25f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(3.dp)
        )
    }
}
