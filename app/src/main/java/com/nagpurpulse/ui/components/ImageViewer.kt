package com.nagpurpulse.ui.components

import android.content.Intent
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
import kotlin.math.abs

/**
 * Full-screen image viewer:
 * - Pinch to zoom (1x–5x), pan clamped to the image, double-tap zoom
 * - Swipe gallery (disabled while zoomed so panning never fights the pager)
 * - Drag up/down to dismiss — backdrop fades as you drag
 * - Single tap hides / shows the controls
 * - Share sends the image link
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageViewer(
    images: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { images.size }
    val context = LocalContext.current
    val haptic = rememberHaptic()

    var zoomed by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var chromeVisible by remember { mutableStateOf(true) }

    val chromeAlpha by animateFloatAsState(
        if (chromeVisible && dragProgress < 0.02f) 1f else 0f,
        tween(180), label = "chrome_alpha"
    )
    val backdropAlpha = 0.97f * (1f - (dragProgress * 0.85f).coerceIn(0f, 0.85f))

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
                .background(Color.Black.copy(backdropAlpha))
        ) {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !zoomed,
                key = { images[it] }
            ) { page ->
                ZoomableImage(
                    url      = images[page],
                    modifier = Modifier.fillMaxSize(),
                    onZoomChanged = { zoomed = it },
                    onDragProgress = { dragProgress = it },
                    onDismissGesture = onDismiss,
                    onTap = { chromeVisible = !chromeVisible },
                    onZoomHaptic = { haptic.tap() }
                )
            }

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .graphicsLayer { alpha = chromeAlpha }
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(0.6f))
                        .pressScale(onClick = { if (chromeAlpha > 0.5f) onDismiss() }),
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
                        .pressScale(onClick = {
                            if (chromeAlpha <= 0.5f) return@pressScale
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, images[pagerState.currentPage])
                            }
                            runCatching {
                                context.startActivity(
                                    Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }),
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
                        .graphicsLayer { alpha = chromeAlpha }
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
fun ZoomableImage(
    url: String,
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit = {},
    onDragProgress: (Float) -> Unit = {},
    onDismissGesture: () -> Unit = {},
    onTap: () -> Unit = {},
    onZoomHaptic: () -> Unit = {}
) {
    var scale     by remember { mutableFloatStateOf(1f) }
    var offset    by remember { mutableStateOf(Offset.Zero) }
    var dragY     by remember { mutableFloatStateOf(0f) }
    var gesturing by remember { mutableStateOf(false) }

    // While a finger is down we follow it exactly; on release we spring.
    val spec: AnimationSpec<Float> = if (gesturing) snap() else spring(Spring.DampingRatioLowBouncy)
    val animScale   by animateFloatAsState(scale, spec, label = "zoom_scale")
    val animOffsetX by animateFloatAsState(offset.x, spec, label = "offset_x")
    val animOffsetY by animateFloatAsState(offset.y + dragY, spec, label = "offset_y")

    val isZoomed = scale > 1.01f
    LaunchedEffect(isZoomed) { onZoomChanged(isZoomed) }

    SubcomposeAsyncImage(
        model   = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        loading = {
            Box(Modifier.fillMaxSize().shimmerEffect(RoundedCornerShape(0.dp)))
        },
        contentDescription = null,
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    gesturing = true
                    // 0 = undecided, 1 = zoom/pan, 2 = drag to dismiss, 3 = leave to the pager
                    var mode = 0
                    var accumulated = Offset.Zero
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.isConsumed }) mode = 3
                        if (mode != 3) {
                            val zoom = event.calculateZoom()
                            val pan  = event.calculatePan()
                            if (mode == 0) {
                                accumulated += pan
                                val multiTouch = event.changes.count { it.pressed } >= 2
                                if (multiTouch) {
                                    mode = 1
                                } else if (accumulated.getDistance() > viewConfiguration.touchSlop) {
                                    mode = when {
                                        scale > 1.01f -> 1
                                        abs(accumulated.y) > abs(accumulated.x) -> 2
                                        else -> 3
                                    }
                                }
                            }
                            when (mode) {
                                1 -> {
                                    val before = scale
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                                    scale = newScale
                                    if (before <= 1.01f && newScale > 1.01f) onZoomHaptic()
                                    offset = if (newScale > 1f) {
                                        val maxX = size.width * (newScale - 1f) / 2f
                                        val maxY = size.height * (newScale - 1f) / 2f
                                        Offset(
                                            (offset.x + pan.x).coerceIn(-maxX, maxX),
                                            (offset.y + pan.y).coerceIn(-maxY, maxY)
                                        )
                                    } else Offset.Zero
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                                2 -> {
                                    dragY += pan.y
                                    onDragProgress((abs(dragY) / size.height).coerceIn(0f, 1f) * 3f)
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    gesturing = false
                    if (mode == 2) {
                        if (abs(dragY) > size.height * 0.18f) {
                            onDismissGesture()
                        } else {
                            dragY = 0f
                            onDragProgress(0f)
                        }
                    }
                    if (scale < 1.02f) { scale = 1f; offset = Offset.Zero }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) { scale = 1f; offset = Offset.Zero }
                        else scale = 2.5f
                        onZoomHaptic()
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
