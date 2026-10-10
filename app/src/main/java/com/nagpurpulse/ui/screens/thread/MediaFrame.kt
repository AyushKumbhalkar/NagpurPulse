// java/com/nagpurpulse/ui/screens/thread/MediaFrame.kt

package com.nagpurpulse.ui.screens.thread

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Shapes the author can pick for how the media appears in the feed. */
enum class FrameRatio(val label: String, val ratio: Float?, val caption: String) {
    ORIGINAL("Original", null, "Full photo"),
    SQUARE("1:1", 1f, "Square"),
    PORTRAIT("4:5", 0.8f, "Tall"),
    LANDSCAPE("16:9", 16f / 9f, "Wide")
}

const val MAX_FRAME_ZOOM = 3f

/**
 * What part of the photo / video cover people will see.
 * [zoom] 1 = widest possible window for the ratio, 3 = a third of it.
 * [focusX]/[focusY] (0..1) slide the window across the room that is left over.
 */
data class MediaFrame(
    val ratio: FrameRatio = FrameRatio.ORIGINAL,
    val zoom: Float = 1f,
    val focusX: Float = 0.5f,
    val focusY: Float = 0.5f
) {
    val isDefault: Boolean get() = ratio == FrameRatio.ORIGINAL && zoom <= 1.001f
}

data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** A gallery item for the "recent" row. */
data class RecentMedia(val uri: Uri, val isVideo: Boolean, val durationMs: Long = 0L)

/** Size of the crop window in source pixels. */
fun cropWindowSize(srcW: Int, srcH: Int, frame: MediaFrame): Pair<Float, Float> {
    val srcRatio = srcW.toFloat() / srcH.toFloat()
    val target = frame.ratio.ratio ?: srcRatio
    var cw: Float
    var ch: Float
    if (srcRatio > target) {
        ch = srcH.toFloat(); cw = ch * target
    } else {
        cw = srcW.toFloat(); ch = cw / target
    }
    val z = frame.zoom.coerceIn(1f, MAX_FRAME_ZOOM)
    return (cw / z) to (ch / z)
}

fun computeCrop(srcW: Int, srcH: Int, frame: MediaFrame): CropRect {
    val (cw, ch) = cropWindowSize(srcW, srcH, frame)
    val left = ((srcW - cw) * frame.focusX.coerceIn(0f, 1f)).roundToInt().coerceIn(0, srcW - 1)
    val top = ((srcH - ch) * frame.focusY.coerceIn(0f, 1f)).roundToInt().coerceIn(0, srcH - 1)
    val w = min(cw.roundToInt().coerceAtLeast(1), srcW - left)
    val h = min(ch.roundToInt().coerceAtLeast(1), srcH - top)
    return CropRect(left, top, w, h)
}

fun cropBitmap(src: Bitmap, frame: MediaFrame): Bitmap {
    if (frame.isDefault) return src
    val r = computeCrop(src.width, src.height, frame)
    return Bitmap.createBitmap(src, r.left, r.top, r.width, r.height)
}

fun scaleDownTo(src: Bitmap, maxSide: Int): Bitmap {
    val longest = max(src.width, src.height)
    if (longest <= maxSide) return src
    val f = maxSide.toFloat() / longest
    return Bitmap.createScaledBitmap(
        src, (src.width * f).roundToInt().coerceAtLeast(1), (src.height * f).roundToInt().coerceAtLeast(1), true
    )
}

object MediaFrameUtils {

    const val MAX_VIDEO_BYTES = 30L * 1024 * 1024
    const val MAX_VIDEO_MS = 60_000L

    fun isVideo(context: Context, uri: Uri): Boolean =
        context.contentResolver.getType(uri)?.startsWith("video/") == true

    fun sizeOf(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)

    fun videoDurationMs(context: Context, uri: Uri): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { r.release() }
        }
    }

    fun videoExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        val ext = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        return ext?.lowercase()?.takeIf { it in setOf("mp4", "webm", "3gp", "mov") } ?: "mp4"
    }

    /** A video frame at [timeMs], scaled so its longest side is at most [maxSide]. */
    fun videoFrame(context: Context, uri: Uri, timeMs: Long, maxSide: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            val frame = r.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: r.getFrameAtTime(-1)
            frame?.let { scaleDownTo(it, maxSide) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    /** Decodes a photo (EXIF rotation applied) with its longest side at most [maxSide]. */
    fun decodeImage(context: Context, uri: Uri, maxSide: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val orientation = context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val rotated = if (raw != null && degrees != 0f) {
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(degrees) }, true)
        } else raw
        rotated?.let { scaleDownTo(it, maxSide) }
    } catch (_: Exception) {
        null
    }

    /**
     * JPEG bytes of exactly what the author framed. A photo left on "Original" is returned untouched,
     * so posting without touching the editor behaves exactly as before.
     */
    suspend fun renderCoverJpeg(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        coverTimeMs: Long,
        frame: MediaFrame
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (!isVideo && frame.isDefault) {
            return@withContext runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
        }
        val src = (if (isVideo) videoFrame(context, uri, coverTimeMs, 2048) else decodeImage(context, uri, 2560))
            ?: return@withContext null
        val out = cropBitmap(src, frame)
        ByteArrayOutputStream().use { bos ->
            out.compress(Bitmap.CompressFormat.JPEG, 92, bos)
            bos.toByteArray()
        }
    }

    /** Newest photos and (when [includeVideos]) videos on the device. */
    suspend fun loadRecentMedia(context: Context, includeVideos: Boolean, limit: Int = 14): List<RecentMedia> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<RecentMedia>()
            try {
                // The duration column is only guaranteed on the shared files table from Android 10.
                val projection = buildList {
                    add(MediaStore.Files.FileColumns._ID)
                    add(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Video.VideoColumns.DURATION)
                }.toTypedArray()
                val types = if (includeVideos)
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE},${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
                else "${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
                val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN ($types)"
                val sort = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
                val collection = MediaStore.Files.getContentUri("external")
                context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val durCol = c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                    while (c.moveToNext() && out.size < limit) {
                        val video = c.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        val dur = if (video && durCol >= 0 && !c.isNull(durCol)) c.getLong(durCol) else 0L
                        // Skip clips the app would reject anyway.
                        if (video && dur > MAX_VIDEO_MS + 500) continue
                        out += RecentMedia(
                            uri = ContentUris.withAppendedId(
                                if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                c.getLong(idCol)
                            ),
                            isVideo = video,
                            durationMs = dur
                        )
                    }
                }
            } catch (_: Exception) {
                // Permission revoked or provider unavailable: show an empty row.
            }
            out
        }
}
