// java/com/nagpurpulse/ui/screens/thread/MediaFrameEditor.kt

package com.nagpurpulse.ui.screens.thread

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTransformGestures
import com.nagpurpulse.data.model.formatMediaDuration
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val EditorBg = Color(0xFF0E0E10)
private val EditorPanel = Color(0xFF1A1A1D)

/** The feed shows photos between 3:4 and 16:9 uncropped; anything outside gets centre-cropped. */
private const val FEED_MIN_RATIO = 0.75f
private const val FEED_MAX_RATIO = 1.78f

/**
 * Full-screen "frame your post" editor.
 * Pick a shape, pinch to zoom, drag to choose what shows. For videos you also pick the cover frame.
 * The preview is drawn with the very same crop maths used when the post is uploaded (WYSIWYG).
 */
@Composable
internal fun MediaFrameEditor(
    uri: Uri,
    isVideo: Boolean,
    durationMs: Long,
    initialFrame: MediaFrame,
    initialCoverMs: Long,
    onDismiss: () -> Unit,
    onApply: (frame: MediaFrame, coverMs: Long, preview: Bitmap) -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()

    var frame by remember { mutableStateOf(initialFrame) }
    var coverMs by remember { mutableLongStateOf(initialCoverMs) }
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    // Photos load once; video covers reload (debounced) while the slider moves.
    LaunchedEffect(uri, isVideo, if (isVideo) coverMs else 0L) {
        if (isVideo && source != null) delay(150)
        val bmp = withContext(Dispatchers.IO) {
            if (isVideo) MediaFrameUtils.videoFrame(context, uri, coverMs, 1600)
            else MediaFrameUtils.decodeImage(context, uri, 1600)
        }
        if (bmp != null) { source = bmp; failed = false } else if (source == null) failed = true
    }

    val src = source
    val srcRatio = src?.let { it.width.toFloat() / it.height } ?: 1f
    val outRatio = frame.ratio.ratio ?: srcRatio
    val feedWillCrop = outRatio < FEED_MIN_RATIO - 0.01f || outRatio > FEED_MAX_RATIO + 0.01f

    val frameState = rememberUpdatedState(frame)
    val sizeState = rememberUpdatedState(boxSize)
    val srcState = rememberUpdatedState(src)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorBg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top bar ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close without framing", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Frame your post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        if (isVideo) "Choose the cover people see in the feed" else "Pinch to zoom · drag to choose what shows",
                        color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (src != null) OrangeMain else Color.White.copy(alpha = 0.12f))
                        .pressScale {
                            val s = srcState.value ?: return@pressScale
                            haptic.tap()
                            val framed = scaleDownTo(cropBitmap(s, frameState.value), 1080)
                            onApply(frameState.value, coverMs, framed)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── Live preview ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (src == null) {
                    if (failed) {
                        Text("Couldn't open this file. Try another one.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    } else {
                        CircularProgressIndicator(color = OrangeMain, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
                    }
                } else {
                    val image = remember(src) { src.asImageBitmap() }
                    Canvas(
                        modifier = Modifier
                            .aspectRatio(outRatio.coerceIn(0.4f, 2.5f))
                            .clip(RoundedCornerShape(18.dp))
                            .border(2.dp, OrangeMain.copy(alpha = 0.9f), RoundedCornerShape(18.dp))
                            .background(Color.Black)
                            .onSizeChanged { boxSize = it }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoomChange, _ ->
                                    val s = srcState.value ?: return@detectTransformGestures
                                    val box = sizeState.value
                                    if (box.width == 0) return@detectTransformGestures
                                    val f = frameState.value
                                    val newZoom = (f.zoom * zoomChange).coerceIn(1f, MAX_FRAME_ZOOM)
                                    val (cw, ch) = cropWindowSize(s.width, s.height, f.copy(zoom = newZoom))
                                    val travelX = s.width - cw
                                    val travelY = s.height - ch
                                    // Dragging the picture right slides the window left.
                                    val dxSrc = pan.x * cw / box.width
                                    val dySrc = pan.y * ch / box.height
                                    val fx = if (travelX > 1f) (f.focusX - dxSrc / travelX).coerceIn(0f, 1f) else 0.5f
                                    val fy = if (travelY > 1f) (f.focusY - dySrc / travelY).coerceIn(0f, 1f) else 0.5f
                                    frame = f.copy(zoom = newZoom, focusX = fx, focusY = fy)
                                }
                            }
                    ) {
                        val rect = computeCrop(image.width, image.height, frame)
                        drawImage(
                            image = image,
                            srcOffset = IntOffset(rect.left, rect.top),
                            srcSize = IntSize(rect.width, rect.height),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            filterQuality = FilterQuality.Medium
                        )
                        // Rule-of-thirds guide
                        val line = Color.White.copy(alpha = 0.28f)
                        for (i in 1..2) {
                            val x = size.width * i / 3f
                            val y = size.height * i / 3f
                            drawLine(line, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                            drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                        }
                    }
                }
            }

            // ── Controls ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(EditorPanel)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (feedWillCrop) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OrangeMain.copy(alpha = 0.14f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = OrangeMain, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "This shape is very tall or wide, so the feed trims the edges. Pick a frame to choose what stays.",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Text("Shape in feed", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FrameRatio.values().forEach { option ->
                        val selected = frame.ratio == option
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) OrangeMain.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f))
                                .border(1.5.dp, if (selected) OrangeMain else Color.Transparent, RoundedCornerShape(14.dp))
                                .pressScale {
                                    haptic.tap()
                                    frame = frame.copy(ratio = option, focusX = 0.5f, focusY = 0.5f)
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Little outline of the shape itself
                            val r = (option.ratio ?: srcRatio).coerceIn(0.5f, 2f)
                            Box(
                                Modifier
                                    .height(22.dp)
                                    .aspectRatio(r)
                                    .border(1.5.dp, if (selected) OrangeMain else Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(option.label, color = if (selected) OrangeMain else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(option.caption, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Zoom", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("%.1f×".format(frame.zoom), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Reset",
                        color = OrangeMain, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .pressScale { haptic.tap(); frame = MediaFrame() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Slider(
                    value = frame.zoom,
                    onValueChange = { frame = frame.copy(zoom = it) },
                    valueRange = 1f..MAX_FRAME_ZOOM,
                    colors = SliderDefaults.colors(
                        thumbColor = OrangeMain, activeTrackColor = OrangeMain,
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                    )
                )

                if (isVideo && durationMs > 0L) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cover frame", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${formatMediaDuration(coverMs)} / ${formatMediaDuration(durationMs)}",
                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = coverMs.toFloat(),
                        onValueChange = { coverMs = it.toLong() },
                        valueRange = 0f..durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = OrangeMain, activeTrackColor = OrangeMain,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                        )
                    )
                }
            }
        }
    }
}
