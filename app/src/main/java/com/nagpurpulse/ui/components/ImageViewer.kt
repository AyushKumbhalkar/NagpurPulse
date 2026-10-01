package com.nagpurpulse.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.ui.theme.*

/**
 * Full-screen image viewer with:
 * - Pinch to zoom
 * - Pan when zoomed
 * - Swipe gallery (multiple images)
 * - Blurred loading placeholder
 * - Close + share actions
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { images.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress   = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.97f))
        ) {
            // Pager
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(
                    url      = images[page],
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f))
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.weight(1f))
                if (images.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(0.6f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("${pagerState.currentPage + 1} / ${images.size}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f))
                        .pressScale(onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // Page dots
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(images.size) { i ->
                        val isActive = i == pagerState.currentPage
                        val dotWidth by animateDpAsState(if (isActive) 20.dp else 6.dp, spring(Spring.DampingRatioMediumBouncy), label = "dot")
                        Box(
                            modifier = Modifier
                                .width(dotWidth)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isActive) OrangePrimary else Color.White.copy(0.4f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomableImage(url: String, modifier: Modifier = Modifier) {
    var scale       by remember { mutableFloatStateOf(1f) }
    var offset      by remember { mutableStateOf(Offset.Zero) }
    val animScale   by animateFloatAsState(scale, spring(Spring.DampingRatioMediumBouncy), label = "zoom_scale")
    val animOffsetX by animateFloatAsState(offset.x, spring(Spring.DampingRatioMediumBouncy), label = "offset_x")
    val animOffsetY by animateFloatAsState(offset.y, spring(Spring.DampingRatioMediumBouncy), label = "offset_y")

    SubcomposeAsyncImage(
        model   = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        loading = {
            Box(Modifier.fillMaxSize().shimmerEffect(RoundedCornerShape(0.dp)))
        },
        contentDescription = null,
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.8f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                        offset = Offset.Zero
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero }
                        else scale = 2.5f
                    }
                )
            }
            .graphicsLayer {
                scaleX       = animScale
                scaleY       = animScale
                translationX = animOffsetX
                translationY = animOffsetY
            },
        contentScale = ContentScale.Fit
    )
}

// ── Thumbnail grid for multiple images ───────────────────────────────────────
@Composable
fun ImageThumbnailGrid(
    images: List<String>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (images.size) {
        1 -> SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(images[0]).crossfade(true).build(),
            loading = { Box(Modifier.fillMaxWidth().height(200.dp).shimmerEffect(RoundedCornerShape(14.dp))) },
            contentDescription = null,
            modifier = modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)).pressScale { onImageClick(0) },
            contentScale = ContentScale.Crop
        )
        2 -> Row(modifier = modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            images.forEachIndexed { i, url ->
                SubcomposeAsyncImage(
                    model = url, contentDescription = null,
                    modifier = Modifier.weight(1f).fillMaxHeight().clip(if (i == 0) RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp) else RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp)).pressScale { onImageClick(i) },
                    contentScale = ContentScale.Crop
                )
            }
        }
        else -> {
            Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    SubcomposeAsyncImage(model = images[0], contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 14.dp)).pressScale { onImageClick(0) },
                        contentScale = ContentScale.Crop)
                    SubcomposeAsyncImage(model = images[1], contentDescription = null,
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 14.dp)).pressScale { onImageClick(1) },
                        contentScale = ContentScale.Crop)
                }
                if (images.size >= 3) {
                    Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        images.drop(2).take(2).forEachIndexed { i, url ->
                            Box(Modifier.weight(1f).fillMaxHeight().clip(if (i == 0) RoundedCornerShape(bottomStart = 14.dp) else RoundedCornerShape(bottomEnd = 14.dp)).pressScale { onImageClick(i + 2) }) {
                                SubcomposeAsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (i == 1 && images.size > 4) {
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.6f)), Alignment.Center) {
                                        Text("+${images.size - 4}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
