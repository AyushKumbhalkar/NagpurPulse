// java/com/nagpurpulse/ui/components/ShareCardRenderer.kt

package com.nagpurpulse.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.categoryDisplay
import com.nagpurpulse.data.model.categoryEmoji
import com.nagpurpulse.data.model.isVideo
import com.nagpurpulse.data.model.sharePreview
import com.nagpurpulse.data.model.timeAgo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Draws the branded 4:5 card people actually receive when they share a post
 * (WhatsApp chat / status, Instagram, Telegram...). Pure Canvas so it never depends on the UI being on screen.
 */
object ShareCardRenderer {

    private const val W = 1080
    private const val H = 1350

    private fun px(v: Int) = v.toFloat()

    suspend fun render(context: Context, post: Post): Bitmap = withContext(Dispatchers.Default) {
        val photo = post.imageUrl?.takeIf { it.isNotBlank() }?.let { loadBitmap(context, it) }
        draw(post, photo)
    }

    /** Text people paste next to the card. */
    fun caption(post: Post, url: String?): String = buildString {
        if (post.isAlert) append(alertEmoji(post.category)).append(' ') else append(post.categoryEmoji()).append(' ')
        append(post.title)
        sharePreview(post.body, 160)?.let { append("\n\n").append(it) }
        append("\n\n")
        if (url != null) append("👉 Read & reply: ").append(url)
        else append("👉 See what Nagpur is saying on Nagpur Pulse 🍊")
    }

    suspend fun saveToCache(context: Context, bitmap: Bitmap, postId: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            // Keep the cache from growing: one card per post, older ones are overwritten.
            dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 24L * 3600 * 1000 }?.forEach { it.delete() }
            val file = File(dir, "nagpurpulse_${postId.take(8).ifBlank { "post" }}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = try {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(1000).build()
        val result = context.imageLoader.execute(request)
        ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
    } catch (_: Exception) {
        null
    }

    private fun layout(text: String, paint: TextPaint, width: Int, maxLines: Int, spacing: Float): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, spacing)
            .setIncludePad(false)
            .build()

    private fun draw(post: Post, photo: Bitmap?): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val isAlert = post.isAlert

        // ── Background ────────────────────────────────────────────────
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.shader = if (isAlert)
            LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), intArrayOf(0xFFFF5A4D.toInt(), 0xFFD7263D.toInt(), 0xFF8E1230.toInt()), null, Shader.TileMode.CLAMP)
        else
            LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), intArrayOf(0xFFFFA726.toInt(), 0xFFFF7A00.toInt(), 0xFFE8480C.toInt()), null, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(34, 255, 255, 255) }
        c.drawCircle(W + 40f, -20f, 380f, glow)
        c.drawCircle(-80f, H - 120f, 330f, glow)

        // ── Header: brand + area ──────────────────────────────────────
        val brand = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 46f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText("🍊 Nagpur Pulse", 72f, 112f, brand)
        val area = post.areaTag?.takeIf { it.isNotBlank() && !it.equals("Nagpur", true) }
        if (area != null) {
            val ap = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val label = "📍 $area"
            val tw = ap.measureText(label)
            val pill = RectF(W - 72f - tw - 48f, 62f, W - 72f, 128f)
            c.drawRoundRect(pill, 33f, 33f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) })
            c.drawText(label, pill.left + 24f, pill.top + 45f, ap)
        }

        // ── White card ────────────────────────────────────────────────
        val card = RectF(56f, 176f, W - 56f, 1168f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(48f, 0f, 18f, Color.argb(70, 0, 0, 0))
        }
        c.drawRoundRect(card, 56f, 56f, cardPaint)

        val pad = 60f
        val innerW = (card.width() - pad * 2).toInt()
        var y = card.top + pad

        // Category pill
        val pillColor = if (isAlert) 0xFFD7263D.toInt() else 0xFFE06A00.toInt()
        val pillBg = if (isAlert) 0xFFFFE9EA.toInt() else 0xFFFFF1E3.toInt()
        val pillText = if (isAlert) {
            val sev = when (post.alertSeverity) { "critical" -> " · CRITICAL"; "high" -> " · URGENT"; else -> "" }
            "${alertEmoji(post.category)} ${alertLabel(post.category).uppercase()} ALERT$sev"
        } else "${post.categoryEmoji()} ${post.categoryDisplay()}"
        val pp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pillColor; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pillRect = RectF(card.left + pad, y, card.left + pad + pp.measureText(pillText) + 52f, y + 68f)
        c.drawRoundRect(pillRect, 34f, 34f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBg })
        c.drawText(pillText, pillRect.left + 26f, pillRect.top + 46f, pp)
        y = pillRect.bottom + 34f

        // Photo / video cover
        val hasPhoto = photo != null
        if (photo != null) {
            val box = RectF(card.left + pad, y, card.right - pad, y + 400f)
            drawCenterCrop(c, photo, box, 40f)
            if (post.isVideo) drawPlayBadge(c, box)
            y = box.bottom + 34f
        }

        // Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF17171A.toInt(); textSize = if (hasPhoto) 58f else 72f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = layout(post.title, titlePaint, innerW, if (hasPhoto) 3 else 5, 1.12f)
        c.save(); c.translate(card.left + pad, y); titleLayout.draw(c); c.restore()
        y += titleLayout.height + 24f

        // Body: as many lines as still fit above the stats row
        val statsTop = card.bottom - 128f
        val bodyText = sharePreview(post.body, 220)
        if (bodyText != null) {
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF55555C.toInt(); textSize = 40f }
            val lineH = bodyPaint.fontSpacing * 1.15f
            val lines = ((statsTop - 20f - y) / lineH).toInt().coerceIn(0, if (hasPhoto) 3 else 7)
            if (lines > 0) {
                val bl = layout(bodyText, bodyPaint, innerW, lines, 1.15f)
                c.save(); c.translate(card.left + pad, y); bl.draw(c); c.restore()
            }
        }

        // Stats row
        val divider = Paint().apply { color = 0xFFEDEDF0.toInt(); strokeWidth = 3f }
        c.drawLine(card.left + pad, statsTop, card.right - pad, statsTop, divider)
        val stat = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3A3A40.toInt(); textSize = 42f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val baseline = statsTop + 78f
        c.drawText("▲ ${post.upvotes}", card.left + pad, baseline, stat)
        c.drawText("💬 ${post.commentCount}", card.left + pad + 220f, baseline, stat)
        val time = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8E8E93.toInt(); textSize = 36f }
        val ago = post.timeAgo()
        c.drawText(ago, card.right - pad - time.measureText(ago), baseline, time)

        // ── Call to action ────────────────────────────────────────────
        val ctaText = "Join the conversation →"
        val cta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isAlert) 0xFFD7263D.toInt() else 0xFFE06A00.toInt()
            textSize = 42f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val ctaW = cta.measureText(ctaText) + 96f
        val ctaRect = RectF((W - ctaW) / 2f, 1208f, (W + ctaW) / 2f, 1208f + 86f)
        c.drawRoundRect(ctaRect, 43f, 43f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        c.drawText(ctaText, ctaRect.left + 48f, ctaRect.top + 58f, cta)
        val tag = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); textSize = 30f }
        val tagText = "Nagpur's neighbourhood community app"
        c.drawText(tagText, (W - tag.measureText(tagText)) / 2f, 1326f, tag)

        return bmp
    }

    private fun drawCenterCrop(c: Canvas, bmp: Bitmap, dst: RectF, radius: Float) {
        val scale = max(dst.width() / bmp.width, dst.height() / bmp.height)
        val sw = dst.width() / scale
        val sh = dst.height() / scale
        val sl = (bmp.width - sw) / 2f
        val st = (bmp.height - sh) / 2f
        val src = android.graphics.Rect(sl.toInt(), st.toInt(), (sl + sw).toInt(), (st + sh).toInt())
        c.save()
        c.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
        c.drawBitmap(bmp, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        c.restore()
    }

    private fun drawPlayBadge(c: Canvas, box: RectF) {
        val cx = box.centerX()
        val cy = box.centerY()
        c.drawCircle(cx, cy, 70f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 0, 0, 0) })
        val tri = Path().apply {
            moveTo(cx - 20f, cy - 34f); lineTo(cx - 20f, cy + 34f); lineTo(cx + 38f, cy); close()
        }
        c.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    }
}
