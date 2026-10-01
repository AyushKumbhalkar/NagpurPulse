// This is the CreateThreadViewModel.kt file.

//java/com/nagpurpulse/ui/screens/thread/CreateThreadViewModel.kt

package com.nagpurpulse.ui.screens.thread

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


// ── ViewModel ─────────────────────────────────────────────────────────────────
data class CreateThreadUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreateThreadViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateThreadUiState())
    val uiState: StateFlow<CreateThreadUiState> = _uiState

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

    fun createPost(
        context: Context,
        title: String,
        body: String,
        category: String?,
        areaTag: String?,
        isAnonymous: Boolean,
        imageUri: Uri?,
        postType: String = "normal",
        editingPostId: String? = null,
        onSuccess: () -> Unit
    ) {
        val userId = authRepository.currentUserId ?: run {
            _uiState.value = CreateThreadUiState(
                error = "You must be logged in to post"
            )
            return
        }

        if (title.isBlank()) {
            _uiState.value = CreateThreadUiState(
                error = "Please enter a title"
            )
            return
        }

        viewModelScope.launch {

            _uiState.value = CreateThreadUiState(isLoading = true)

            var imageUrl: String? = null

            // Upload only when a new image was selected
            if (imageUri != null) {

                val imageBytes = context.contentResolver
                    .openInputStream(imageUri)
                    ?.readBytes()

                if (imageBytes != null) {

                    val uploadResult =
                        postRepository.uploadPostImage(imageBytes)

                    uploadResult.fold(
                        onSuccess = {
                            imageUrl = it
                        },
                        onFailure = {
                            _uiState.value = CreateThreadUiState(
                                error = "Image upload failed"
                            )
                            return@launch
                        }
                    )
                }
            }

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
                    imageUrl = imageUrl
                ).fold(
                    onSuccess = {
                        _uiState.value = CreateThreadUiState(
                            isSuccess = true
                        )
                        onSuccess()
                    },
                    onFailure = { e ->
                        _uiState.value = CreateThreadUiState(
                            error = e.message ?: "Failed to update post"
                        )
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
                    imageUrl = imageUrl
                ).fold(
                    onSuccess = {
                        _uiState.value = CreateThreadUiState(
                            isSuccess = true
                        )
                        onSuccess()
                    },
                    onFailure = { e ->
                        _uiState.value = CreateThreadUiState(
                            error = e.message ?: "Failed to create post"
                        )
                    }
                )
            }
        }
    }
}