// java/com/nagpurpulse/ui/components/PostVideoPlayer.kt

package com.nagpurpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.nagpurpulse.data.model.formatMediaDuration

/** Small "this is a video" marker for feed thumbnails: centred play button + duration chip. */
@Composable
fun VideoBadgeOverlay(durationMs: Int?, modifier: Modifier = Modifier, compact: Boolean = false) {
    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(if (compact) 26.dp else 52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow, contentDescription = "Video",
                tint = Color.White, modifier = Modifier.size(if (compact) 16.dp else 32.dp)
            )
        }
        if (!compact && durationMs != null && durationMs > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(formatMediaDuration(durationMs.toLong()), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Tap-to-play video for the thread screen. Nothing is downloaded or decoded until the user taps,
 * so opening a post stays as fast as it was for photos. Playback pauses when the app is backgrounded
 * and the player is always released when the post leaves the screen.
 */
@Composable
fun PostVideoPlayer(
    videoUrl: String,
    thumbnailUrl: String?,
    durationMs: Int?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var started by remember(videoUrl) { mutableStateOf(false) }
    var ratio by remember(videoUrl) { mutableFloatStateOf(16f / 9f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio.coerceIn(0.6f, 1.91f))
            .background(Color.Black)
    ) {
        if (!started) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { started = true }
            ) {
                VideoBadgeOverlay(durationMs)
            }
        } else {
            val player = remember(videoUrl) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(videoUrl))
                    repeatMode = Player.REPEAT_MODE_OFF
                    prepare()
                    playWhenReady = true
                }
            }
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            ratio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                        }
                    }
                }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                    player.release()
                }
            }
            DisposableEffect(lifecycleOwner, player) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) player.pause()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            AndroidView(
                factory = { ctx -> PlayerView(ctx).apply { this.player = player } },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
