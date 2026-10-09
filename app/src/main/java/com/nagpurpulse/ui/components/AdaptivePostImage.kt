// AdaptivePostImage.kt file
// java/com/nagpurpulse/ui/components/AdaptivePostImage.kt

package com.nagpurpulse.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

/**
 * Remembers the true (width / height) ratio of every remote image we've ever
 * measured, keyed by URL. Without this, an image box would "pop" from a
 * placeholder shape to its real shape every single time it loads — including
 * every time you scroll back up to a post you already saw. With it, the box
 * is sized correctly on frame one, anywhere in the app, the moment an image
 * has been measured once.
 */
private object ImageAspectRatioCache {
    private val cache = HashMap<String, Float>()
    fun get(url: String): Float? = cache[url]
    fun put(url: String, ratio: Float) { cache[url] = ratio }
}

/** Used only before the real ratio is known (first ever load of that URL). */
private const val FALLBACK_RATIO = 0.8f // 4:5 portrait — a calm default, not a jarring square or sliver

/**
 * Reddit-style adaptive post image.
 *
 * The core idea: every post image keeps its *real* shape instead of being
 * forced into one fixed box.
 *
 * - While `minRatio <= naturalRatio <= maxRatio`, the photo is shown at its
 *   exact natural aspect ratio — nothing is cropped, nothing is stretched.
 * - Outside that range (an ultra-tall screenshot, a wide panorama) the box is
 *   clamped to the nearer bound and the image is centre-cropped to fill it,
 *   so one oddly-shaped photo can never take over the whole feed.
 * - When a crop did happen, a small "expand" hint appears in the corner so
 *   people know there's more of the image to see on tap.
 *
 * @param minRatio narrowest box (tallest shape) allowed before cropping kicks in.
 * @param maxRatio widest box allowed before cropping kicks in.
 * @param cornerRadius pass 0.dp for an edge-to-edge / full-bleed placement.
 * @param onClick pass null to let taps fall through to a clickable parent
 *   (e.g. the whole feed card already navigates on tap); pass a lambda only
 *   when this image itself should react independently (e.g. opening a
 *   full-screen viewer from the thread detail screen).
 */
@Composable
fun AdaptivePostImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    minRatio: Float = 0.75f,
    maxRatio: Float = 1.78f,
    cornerRadius: Dp = 16.dp,
    showExpandHintWhenCropped: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    var naturalRatio by remember(imageUrl) { mutableStateOf(ImageAspectRatioCache.get(imageUrl)) }
    var isError by remember(imageUrl) { mutableStateOf(false) }
    val isLoading = naturalRatio == null && !isError

    val targetBoxRatio = (naturalRatio ?: FALLBACK_RATIO).coerceIn(minRatio, maxRatio)
    // Animate the box settling into its real shape instead of an abrupt jump-cut
    // the first time a given image is decoded.
    val animatedBoxRatio by animateFloatAsState(
        targetValue = targetBoxRatio,
        animationSpec = tween(220),
        label = "image_ratio"
    )
    val wasCropped = naturalRatio != null && (naturalRatio!! < minRatio || naturalRatio!! > maxRatio)

    // Only runs while the image is loading. A permanent infinite transition per image keeps
    // requesting frames for every photo in the feed, even after it has loaded.
    val pulse = rememberLoadingPulse(enabled = isLoading)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(animatedBoxRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
    ) {
        if (!isError) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .let { if (isLoading) it.alpha(pulse) else it },
                contentScale = ContentScale.Crop,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> {
                            val drawable = state.result.drawable
                            val w = drawable.intrinsicWidth
                            val h = drawable.intrinsicHeight
                            if (w > 0 && h > 0) {
                                val ratio = w.toFloat() / h.toFloat()
                                ImageAspectRatioCache.put(imageUrl, ratio)
                                naturalRatio = ratio
                            }
                            isError = false
                        }
                        is AsyncImagePainter.State.Error -> {
                            isError = true
                        }
                        else -> Unit
                    }
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        if (wasCropped && showExpandHintWhenCropped && !isError) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Icon(
                    Icons.Filled.OpenInFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberLoadingPulse(enabled: Boolean): Float {
    if (!enabled) return 1f
    val pulse by rememberInfiniteTransition(label = "img_pulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "img_pulse_alpha"
    )
    return pulse
}
