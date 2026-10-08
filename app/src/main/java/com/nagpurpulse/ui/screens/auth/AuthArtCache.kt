package com.nagpurpulse.ui.screens.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Big illustrations (the sign-up header / footer are 1.3 - 1.7 MB PNGs) used to be decoded on the
 * main thread every time they re-entered the screen, which is exactly what happens when the keyboard
 * is opened and closed quickly. Here they are decoded ONCE, off the main thread, scaled down to the
 * screen width, kept for the life of the process and handed out as ready-to-draw bitmaps.
 */
internal object AuthArtCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun key(resId: Int, targetWidthPx: Int, fadeTop: Boolean) = "$resId:$targetWidthPx:$fadeTop"

    fun peek(key: String): ImageBitmap? = cache[key]

    suspend fun load(context: Context, resId: Int, targetWidthPx: Int, fadeTop: Boolean): ImageBitmap? {
        val key = key(resId, targetWidthPx, fadeTop)
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { decode(context, resId, targetWidthPx, fadeTop) }.getOrNull()
                ?.also { cache[key] = it }
        }
    }

    private fun decode(context: Context, resId: Int, targetWidthPx: Int, fadeTop: Boolean): ImageBitmap {
        val res = context.resources
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(res, resId, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidthPx) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inScaled = false            // never density-scale, even if the file sits in plain drawable/
            inMutable = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bmp = BitmapFactory.decodeResource(res, resId, opts) ?: error("decode failed: $resId")
        if (bmp.width > targetWidthPx * 1.15f) {
            val h = (bmp.height.toLong() * targetWidthPx / bmp.width).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(bmp, targetWidthPx, h, true)
            if (scaled !== bmp) bmp.recycle()
            bmp = scaled
        }
        if (!bmp.isMutable) bmp = bmp.copy(Bitmap.Config.ARGB_8888, true)
        if (fadeTop) {
            // Bake the "fade into the page" mask once, instead of an offscreen blend on every frame.
            val paint = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                shader = LinearGradient(
                    0f, 0f, 0f, bmp.height.toFloat(),
                    intArrayOf(0x00000000, 0xFF000000.toInt(), 0xFF000000.toInt()),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
            }
            Canvas(bmp).drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), paint)
        }
        return bmp.asImageBitmap()
    }
}

/** Returns the cached art (or null for a moment on the very first frame, then it fades in). */
@Composable
internal fun rememberAuthArt(resId: Int, fadeTop: Boolean = false): ImageBitmap? {
    if (resId == 0) return null
    val context = LocalContext.current
    if (LocalInspectionMode.current) return ImageBitmap.imageResource(context.resources, resId)
    val density = LocalDensity.current.density
    val target = (LocalConfiguration.current.screenWidthDp * density).toInt().coerceIn(720, 1440)
    val key = AuthArtCache.key(resId, target, fadeTop)
    var bmp by remember(key) { mutableStateOf(AuthArtCache.peek(key)) }
    LaunchedEffect(key) {
        if (bmp == null) bmp = AuthArtCache.load(context.applicationContext, resId, target, fadeTop)
    }
    return bmp
}
