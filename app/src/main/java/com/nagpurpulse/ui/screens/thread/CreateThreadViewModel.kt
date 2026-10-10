// This is the CreateThreadViewModel.kt file.

//java/com/nagpurpulse/ui/screens/thread/CreateThreadViewModel.kt

package com.nagpurpulse.ui.screens.thread

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.model.ComposerInsights
import com.nagpurpulse.data.model.PostPrompt
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


// ── ViewModel ─────────────────────────────────────────────────────────────────
data class CreateThreadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Incremented on every error so the same message can be shown twice in a row. */
    val errorNonce: Int = 0,
    val isSuccess: Boolean = false,
    /** What the upload is doing right now ("Uploading video…"); only meaningful while isLoading. */
    val statusLabel: String? = null,
    /** 0..100 while a picked video is being compressed in the background, otherwise null. */
    val videoPrepProgress: Int? = null,
    // Composer header
    val displayName: String = "",
    val avatarUrl: String? = null,
    // Best-effort live numbers (null / empty = hide)
    val insights: ComposerInsights = ComposerInsights(),
    // "Not sure what to post?" ideas (empty = hide)
    val prompts: List<PostPrompt> = emptyList()
)

@HiltViewModel
class CreateThreadViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateThreadUiState())
    val uiState: StateFlow<CreateThreadUiState> = _uiState

    private var insightsJob: Job? = null

    val currentUserId: String? get() = authRepository.currentUserId

    init {
        loadProfile()
        loadPrompts()
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            val prompts = postRepository.getPostPrompts(limit = 5)
            _uiState.update { it.copy(prompts = prompts) }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getCurrentProfile().onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        displayName = profile.displayName?.takeIf { n -> n.isNotBlank() }
                            ?: profile.username,
                        avatarUrl = profile.avatarUrl
                    )
                }
            }
        }
    }

    /** Refreshes social proof / reach / trending numbers for the chosen area. */
    fun loadInsights(areaTag: String?) {
        insightsJob?.cancel()
        insightsJob = viewModelScope.launch {
            postRepository.getComposerInsights(areaTag).onSuccess { result ->
                _uiState.update { it.copy(insights = result) }
            }
        }
    }

    fun getPostById(
        postId: String,
        onLoaded: (com.nagpurpulse.data.model.Post) -> Unit
    ) {
        viewModelScope.launch {

            postRepository.getPostById(postId)
                .onSuccess {
                    onLoaded(it)
                }
        }
    }

    // ── Video compression (starts as soon as a video is picked) ─────────────
    private var prepJob: Deferred<File?>? = null
    private var prepUri: Uri? = null

    fun prepareVideo(context: Context, uri: Uri, durationMs: Long, sourceBytes: Long) {
        cancelVideoPrep()
        prepUri = uri
        prepJob = viewModelScope.async {
            if (!VideoCompressor.needsCompression(context, uri, durationMs, sourceBytes)) return@async null
            _uiState.update { it.copy(videoPrepProgress = 0) }
            try {
                VideoCompressor.compress(context, uri) { p ->
                    _uiState.update { it.copy(videoPrepProgress = p) }
                }
            } finally {
                _uiState.update { it.copy(videoPrepProgress = null) }
            }
        }
    }

    fun cancelVideoPrep() {
        prepJob?.cancel()
        prepJob = null
        prepUri = null
        _uiState.update { it.copy(videoPrepProgress = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showError(message: String) = setError(message)

    private fun setError(message: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                error = message,
                errorNonce = it.errorNonce + 1
            )
        }
    }

    private fun friendlyError(e: Throwable, fallback: String): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                    raw.contains("timeout", ignoreCase = true) ||
                    raw.contains("timed out", ignoreCase = true) ->
                "No internet connection. Your draft is saved."
            raw.isBlank() -> fallback
            else -> raw
        }
    }

    fun createPost(
        context: Context,
        title: String,
        body: String,
        category: String?,
        areaTag: String?,
        isAnonymous: Boolean,
        imageUri: Uri?,
        postType: String = "normal",
        alertSeverity: String? = null,
        editingPostId: String? = null,
        clearImage: Boolean = false,
        isVideo: Boolean = false,
        mediaFrame: MediaFrame? = null,
        coverTimeMs: Long = 0L,
        videoDurationMs: Long = 0L,
        clearVideo: Boolean = false,
        onSuccess: () -> Unit
    ) {
        if (_uiState.value.isLoading) return

        val userId = authRepository.currentUserId ?: run {
            setError("You must be logged in to post")
            return
        }

        if (title.isBlank()) {
            setError("Please enter a title")
            return
        }

        viewModelScope.launch {

            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }

            var imageUrl: String? = null
            var videoUrl: String? = null

            // Upload only when new media was selected
            if (imageUri != null) {

                if (isVideo) {
                    _uiState.update { it.copy(statusLabel = "Compressing video…") }

                    // Compression normally finished while the author was still writing the post.
                    val compressed: File? = if (prepUri == imageUri) {
                        try { prepJob?.await() } catch (_: Exception) { null }
                    } else null

                    val sourceSize = withContext(Dispatchers.IO) { MediaFrameUtils.sizeOf(context, imageUri) }
                    if (compressed == null && sourceSize > MediaFrameUtils.MAX_VIDEO_BYTES) {
                        setError("Couldn't compress this video on your phone. Try a shorter clip.")
                        return@launch
                    }

                    _uiState.update { it.copy(statusLabel = "Uploading video…") }

                    val videoBytes = withContext(Dispatchers.IO) {
                        runCatching {
                            if (compressed != null) compressed.readBytes()
                            else context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                        }.getOrNull()
                    }
                    if (videoBytes == null) {
                        setError("Couldn't read that video. Try another one.")
                        return@launch
                    }
                    if (videoBytes.size > MediaFrameUtils.MAX_VIDEO_BYTES) {
                        compressed?.delete()
                        setError("That video is still too large after compression. Try a shorter clip.")
                        return@launch
                    }

                    val ext = if (compressed != null) "mp4"
                    else withContext(Dispatchers.IO) { MediaFrameUtils.videoExtension(context, imageUri) }

                    val uploaded = postRepository.uploadPostVideo(videoBytes, ext)
                    compressed?.delete()
                    uploaded.fold(
                        onSuccess = { videoUrl = it },
                        onFailure = { e ->
                            setError(friendlyError(e, "Video upload failed"))
                            return@launch
                        }
                    )
                }

                _uiState.update { it.copy(statusLabel = if (isVideo) "Adding your cover…" else "Optimising photo…") }

                // Cropping to the chosen frame (and decoding a video cover) is CPU/disk work.
                val imageBytes = MediaFrameUtils.renderCoverJpeg(
                    context = context,
                    uri = imageUri,
                    isVideo = isVideo,
                    coverTimeMs = coverTimeMs,
                    frame = mediaFrame ?: MediaFrame()
                )

                if (imageBytes == null) {
                    setError(if (isVideo) "Couldn't read that video. Try another one." else "Couldn't read that photo. Try another one.")
                    return@launch
                }

                val uploadResult = postRepository.uploadPostImage(imageBytes)

                uploadResult.fold(
                    onSuccess = { imageUrl = it },
                    onFailure = { e ->
                        setError(friendlyError(e, "Image upload failed"))
                        return@launch
                    }
                )
            }

            _uiState.update { it.copy(statusLabel = "Posting…") }

            // ─────────────────────────────────────────────────────────────
            // EDIT EXISTING POST
            // ─────────────────────────────────────────────────────────────
            if (editingPostId != null) {

                postRepository.updatePost(
                    postId = editingPostId,
                    title = title.trim(),
                    body = body.trim().ifBlank { null },
                    category = category?.ifBlank { "" } ?: "",
                    areaTag = areaTag,
                    isAnonymous = isAnonymous,
                    postType = postType,
                    isAlert = postType == "alert",
                    alertSeverity = alertSeverity,
                    imageUrl = imageUrl,
                    clearImage = clearImage && imageUrl == null,
                    videoUrl = videoUrl,
                    videoDurationMs = if (videoUrl != null) videoDurationMs.toInt() else null,
                    clearVideo = clearVideo && videoUrl == null
                ).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        onSuccess()
                    },
                    onFailure = { e ->
                        setError(friendlyError(e, "Failed to update post"))
                    }
                )

            } else {

                // ─────────────────────────────────────────────────────────
                // CREATE NEW POST
                // ─────────────────────────────────────────────────────────
                postRepository.createPost(
                    userId = userId,
                    title = title.trim(),
                    body = body.trim().ifBlank { null },
                    category = category?.ifBlank { "" } ?: "",
                    areaTag = areaTag,
                    isAnonymous = isAnonymous,
                    postType = postType,
                    isAlert = postType == "alert",
                    alertSeverity = alertSeverity,
                    imageUrl = imageUrl,
                    videoUrl = videoUrl,
                    videoDurationMs = if (videoUrl != null) videoDurationMs.toInt() else null
                ).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        onSuccess()
                    },
                    onFailure = { e ->
                        setError(friendlyError(e, "Failed to create post"))
                    }
                )
            }
        }
    }
}
