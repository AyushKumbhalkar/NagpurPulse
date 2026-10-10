// notifications/PulseIcons.kt
// Renders the large icon shown beside every notification:
//   - sender avatar (circular) with a small type badge in the corner, or
//   - a gradient tile with the type glyph when there is no avatar.
// All drawing is vector/bitmap based; no emoji glyphs are used anywhere.
package com.nagpurpulse.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

internal object PulseIcons {

    private const val DEFAULT_SIZE = 192
    private const val MAX_IMAGE_BYTES = 3_000_000

    /** Gradient tile with a white type glyph — used when there is no sender avatar. */
    fun glyphTile(ctx: Context, @DrawableRes glyph: Int, accent: Int, size: Int = DEFAULT_SIZE): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                ColorUtils.blendARGB(accent, Color.WHITE, 0.28f), accent,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        drawGlyph(ctx, canvas, glyph, (size * 0.25f).toInt(), (size * 0.25f).toInt(), (size * 0.75f).toInt(), (size * 0.75f).toInt())
        return out
    }

    /** Circular avatar with a type badge anchored bottom-right. */
    fun avatarWithBadge(
        ctx: Context,
        avatar: Bitmap,
        @DrawableRes glyph: Int,
        accent: Int,
        size: Int = DEFAULT_SIZE
    ): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(circleCrop(avatar, size), 0f, 0f, null)

        val badgeR = size * 0.21f
        val cx = size - badgeR - size * 0.01f
        val cy = size - badgeR - size * 0.01f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawCircle(cx, cy, badgeR + size * 0.025f, ring)
        canvas.drawCircle(cx, cy, badgeR, fill)
        val inset = badgeR * 0.42f
        drawGlyph(
            ctx, canvas, glyph,
            (cx - badgeR + inset).toInt(), (cy - badgeR + inset).toInt(),
            (cx + badgeR - inset).toInt(), (cy + badgeR - inset).toInt()
        )
        return out
    }

    /** Circular crop of any bitmap (centre-cropped to a square first). */
    fun circleCrop(src: Bitmap, size: Int = DEFAULT_SIZE): Bitmap {
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val square = Bitmap.createBitmap(src, x, y, side, side)
        val scaled = Bitmap.createScaledBitmap(square, size, size, true)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return out
    }

    private fun drawGlyph(ctx: Context, canvas: Canvas, @DrawableRes glyph: Int, l: Int, t: Int, r: Int, b: Int) {
        val d = ContextCompat.getDrawable(ctx, glyph)?.mutate() ?: return
        d.setTint(Color.WHITE)
        d.setBounds(l, t, r, b)
        d.draw(canvas)
    }

    // ── Network image ─────────────────────────────────────────────────────────

    /**
     * Downloads and down-samples an https image. Safe to call from the FCM worker thread:
     * short timeouts, size cap, never throws. Returns null on any failure so callers
     * simply fall back to the glyph tile.
     */
    fun fetchBitmap(url: String?, maxEdge: Int, timeoutMs: Int = 3_500): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            val parsed = URL(url)
            if (!parsed.protocol.equals("https", ignoreCase = true)) return null
            val conn = (parsed.openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
            }
            try {
                if (conn.responseCode !in 200..299) return null
                val bytes = conn.inputStream.use { readCapped(it) } ?: return null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxEdge && bounds.outHeight / (sample * 2) >= maxEdge) {
                    sample *= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
                // Never hand the system a bitmap larger than needed for a notification.
                val longest = maxOf(decoded.width, decoded.height)
                if (longest <= maxEdge) {
                    decoded
                } else {
                    val ratio = maxEdge.toFloat() / longest
                    Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * ratio).toInt().coerceAtLeast(1),
                        (decoded.height * ratio).toInt().coerceAtLeast(1),
                        true
                    )
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readCapped(input: InputStream): ByteArray? {
        val buffer = ByteArray(8 * 1024)
        val out = ByteArrayOutputStream()
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > MAX_IMAGE_BYTES) return null
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
