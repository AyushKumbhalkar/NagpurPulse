// java/com/nagpurpulse/ui/screens/thread/VideoCompressor.kt

package com.nagpurpulse.ui.screens.thread

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shrinks videos before upload so a 5 minute clip does not eat storage:
 * H.264 + AAC, short side capped at 720 px, ~850 kbps video (roughly 30 MB for 5 minutes).
 */
@OptIn(UnstableApi::class)
object VideoCompressor {

    private const val TARGET_VIDEO_BITRATE = 850_000
    private const val MAX_SHORT_SIDE = 720

    private data class Size(val width: Int, val height: Int)

    private fun probe(context: Context, uri: Uri): Size? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
            val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) Size(h, w) else Size(w, h)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    /** Already small and low-bitrate clips are uploaded as they are. */
    suspend fun needsCompression(context: Context, uri: Uri, durationMs: Long, sourceBytes: Long): Boolean =
        withContext(Dispatchers.IO) {
            if (durationMs <= 0L || sourceBytes <= 0L) return@withContext true
            val bitrate = sourceBytes * 8000L / durationMs
            val size = probe(context, uri)
            val shortSide = size?.let { min(it.width, it.height) } ?: Int.MAX_VALUE
            bitrate > 1_300_000L || shortSide > MAX_SHORT_SIDE
        }

    private fun even(v: Float): Int = (v / 2f).roundToInt().coerceAtLeast(1) * 2

    /**
     * Returns the compressed file, or null if the device could not transcode it.
     * Cancelling the calling coroutine cancels the transcode and deletes the partial file.
     */
    suspend fun compress(context: Context, uri: Uri, onProgress: (Int) -> Unit): File? {
        val size = withContext(Dispatchers.IO) { probe(context, uri) }
        val dir = File(context.cacheDir, "compressed").apply { mkdirs() }
        dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 6L * 3600 * 1000 }?.forEach { it.delete() }
        val out = File(dir, "post_${System.currentTimeMillis()}.mp4")

        // Transformer must be created and driven from a thread with a Looper.
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<File?> { cont ->
                val handler = Handler(Looper.getMainLooper())

                val videoEffects = mutableListOf<Effect>()
                if (size != null && min(size.width, size.height) > MAX_SHORT_SIDE) {
                    val scale = MAX_SHORT_SIDE.toFloat() / min(size.width, size.height)
                    videoEffects += Presentation.createForWidthAndHeight(
                        even(size.width * scale), even(size.height * scale), Presentation.LAYOUT_SCALE_TO_FIT
                    )
                }

                val encoderFactory = DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder().setBitrate(TARGET_VIDEO_BITRATE).build()
                    )
                    .build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(out)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            out.delete()
                            if (cont.isActive) cont.resume(null)
                        }
                    })
                    .build()

                val item = EditedMediaItem.Builder(MediaItem.fromUri(uri))
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val holder = ProgressHolder()
                val poll = object : Runnable {
                    override fun run() {
                        if (!cont.isActive) return
                        if (transformer.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                            onProgress(holder.progress.coerceIn(0, 100))
                        }
                        handler.postDelayed(this, 400)
                    }
                }

                cont.invokeOnCancellation {
                    handler.post {
                        handler.removeCallbacks(poll)
                        runCatching { transformer.cancel() }
                        out.delete()
                    }
                }

                try {
                    transformer.start(item, out.absolutePath)
                    handler.post(poll)
                } catch (_: Exception) {
                    out.delete()
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }
}
