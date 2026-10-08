// this is the ThreadDetailScreen.kt file
//java/com/nagpurpulse/ui/screens/thread/ThreadDetailScreen.kt

package com.nagpurpulse.ui.screens.thread

import com.nagpurpulse.data.repository.AdminRepository
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.nagpurpulse.R
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nagpurpulse.data.model.Comment
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.SavedPostsRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import com.nagpurpulse.ui.preferences.DensityManager
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

enum class CommentSort(val value: String) { TOP("top"), NEW("new"), OLD("old") }

/** A comment written locally that is still being sent (or failed). */
data class PendingComment(
    val localId: String,
    val body: String,
    val isAnonymous: Boolean,
    val parentId: String?,
    val failed: Boolean = false
)

/** One-shot messages for the snackbar; the screen maps them to string resources. */
enum class CommentMessage {
    REPORT_SENT, REPORT_FAILED, POST_FAILED, TOO_FAST, TOO_LONG,
    EDIT_FAILED, DELETE_FAILED, DELETED, LIKE_FAILED, LOAD_FAILED
}

private const val COMMENT_PAGE_SIZE = 20
private const val MAX_RELOAD_ROOTS = 100

data class ThreadDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val commentsLoading: Boolean = true,
    val commentSort: CommentSort = CommentSort.TOP,
    val totalRoots: Int = 0,
    val isLoadingMore: Boolean = false,
    val replyTarget: Comment? = null,
    val pending: List<PendingComment> = emptyList(),
    val scrollToCommentId: String? = null,
    val pinnedCommentId: String? = null,
    val newCommentCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val userVote: String? = null,
    val isSaved: Boolean = false,
    val isLoggedIn: Boolean = false,

    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val voteLoaded: Boolean = false
)

@Composable
private fun ReportPostDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reasons = listOf(
        "Spam" to "spam",
        "Harassment" to "harassment",
        "Misinformation" to "misinformation",
        "Inappropriate" to "inappropriate",
        "Other" to "other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171318),
        titleContentColor = Color(0xFFF7F3F5),
        textContentColor = Color(0xFFC7C0CA),
        shape = RoundedCornerShape(28.dp),
        title = { Text("Report Post") },
        text = {
            Column {
                reasons.forEach { (displayName, dbValue) ->
                    TextButton(
                        onClick = {
                            onReport(dbValue)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(displayName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@HiltViewModel
class ThreadDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val savedPostsRepository: SavedPostsRepository,
    private val adminRepository: AdminRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val postId: String = savedStateHandle["postId"] ?: ""
    private val _uiState = MutableStateFlow(ThreadDetailUiState())
    val uiState: StateFlow<ThreadDetailUiState> = _uiState

    private val _events = MutableSharedFlow<CommentMessage>(extraBufferCapacity = 8)
    val events: SharedFlow<CommentMessage> = _events.asSharedFlow()

    /** Ignores out-of-order responses when several reloads overlap. */
    private var loadSeq = 0
    private val likesInFlight = mutableSetOf<String>()

    init {
        _uiState.value = _uiState.value.copy(
            isLoggedIn = authRepository.isLoggedIn(),
            currentUserId = authRepository.currentUserId
        )

        viewModelScope.launch {
            val adminStatus = adminRepository.isAdmin()
            _uiState.value = _uiState.value.copy(
                isAdmin = adminStatus
            )
        }

        loadPost()
        reloadComments(silent = true)
        loadUserVoteAndSaved()

        // Keep this thread in sync with other people's comments. A burst of events
        // collapses into one reload (collectLatest cancels the waiting one).
        viewModelScope.launch {
            runCatching {
                postRepository.subscribeToComments(postId).collectLatest {
                    delay(300)
                    loadPost()
                    reloadComments(silent = true, fromRealtime = true)
                }
            }.onFailure { error ->
                android.util.Log.e("THREAD_COMMENTS_RT", "Comment realtime subscription stopped", error)
            }
        }
    }

    fun refreshThread() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val seq = ++loadSeq
                val postResult = postRepository.getPostById(postId)
                val commentsResult = postRepository.getComments(
                    postId, _uiState.value.commentSort.value, COMMENT_PAGE_SIZE, 0
                )
                postResult.onSuccess { post -> _uiState.update { it.copy(post = post) } }
                commentsResult.onSuccess { page ->
                    if (seq == loadSeq) {
                        _uiState.update {
                            it.copy(
                                comments = page.comments, totalRoots = page.totalRoots,
                                commentsLoading = false, newCommentCount = 0, pinnedCommentId = null
                            )
                        }
                    }
                }
                if (postResult.isFailure && commentsResult.isFailure) {
                    _uiState.update { it.copy(error = postResult.exceptionOrNull()?.message) }
                    _events.tryEmit(CommentMessage.LOAD_FAILED)
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun loadPost() {
        viewModelScope.launch {
            val shouldShowInitialLoading = _uiState.value.post == null
            if (shouldShowInitialLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }
            postRepository.getPostById(postId).fold(
                onSuccess = { post ->
                    _uiState.value = _uiState.value.copy(post = post, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            )
        }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    private fun mergeById(old: List<Comment>, incoming: List<Comment>): List<Comment> =
        (old.associateBy { it.id } + incoming.associateBy { it.id }).values.toList()

    private fun mapComment(id: String, transform: (Comment) -> Comment) {
        _uiState.update { st -> st.copy(comments = st.comments.map { if (it.id == id) transform(it) else it }) }
    }

    private fun postErrorMessage(e: Throwable, fallback: CommentMessage): CommentMessage {
        val text = e.message.orEmpty()
        return when {
            text.contains("too fast", ignoreCase = true) -> CommentMessage.TOO_FAST
            text.contains("too long", ignoreCase = true) -> CommentMessage.TOO_LONG
            else -> fallback
        }
    }

    /** Reloads what we already have (>= one page, <= [MAX_RELOAD_ROOTS] roots) in one request. */
    private fun reloadComments(silent: Boolean = true, fromRealtime: Boolean = false) {
        viewModelScope.launch {
            val seq = ++loadSeq
            val current = _uiState.value
            val loadedRoots = current.comments.count { it.parentId == null }
            val limit = loadedRoots.coerceIn(COMMENT_PAGE_SIZE, MAX_RELOAD_ROOTS)
            postRepository.getComments(postId, current.commentSort.value, limit, 0).fold(
                onSuccess = { page ->
                    if (seq != loadSeq) return@fold
                    _uiState.update { st ->
                        val knownIds = st.comments.mapTo(HashSet()) { it.id }
                        val fresh = if (fromRealtime) page.comments.count { it.id !in knownIds && !it.isMine } else 0
                        st.copy(
                            comments = page.comments, totalRoots = page.totalRoots,
                            commentsLoading = false, newCommentCount = st.newCommentCount + fresh
                        )
                    }
                },
                onFailure = {
                    if (seq != loadSeq) return@fold
                    _uiState.update { it.copy(commentsLoading = false) }
                    if (!silent) _events.tryEmit(CommentMessage.LOAD_FAILED)
                }
            )
        }
    }

    fun loadMoreComments() {
        val st = _uiState.value
        if (st.isLoadingMore) return
        val loadedRoots = st.comments.count { it.parentId == null }
        if (loadedRoots >= st.totalRoots) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            postRepository.getComments(postId, st.commentSort.value, COMMENT_PAGE_SIZE, loadedRoots).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(comments = mergeById(it.comments, page.comments),
                            totalRoots = page.totalRoots, isLoadingMore = false)
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    _events.tryEmit(CommentMessage.LOAD_FAILED)
                }
            )
        }
    }

    fun setCommentSort(sort: CommentSort) {
        if (sort == _uiState.value.commentSort) return
        _uiState.update {
            it.copy(commentSort = sort, comments = emptyList(), commentsLoading = true,
                totalRoots = 0, newCommentCount = 0, pinnedCommentId = null)
        }
        reloadComments(silent = false)
    }

    fun setReplyTarget(comment: Comment?) { _uiState.update { it.copy(replyTarget = comment) } }
    fun consumeScrollTarget() { _uiState.update { it.copy(scrollToCommentId = null) } }
    fun clearNewComments() { _uiState.update { it.copy(newCommentCount = 0) } }

    /**
     * Sends a comment (or a reply when a reply target is set). It shows up immediately as
     * "Sending…" and can be retried on failure. The server assigns the anonymous alias.
     */
    fun sendComment(body: String, isAnonymous: Boolean) {
        val text = body.trim()
        if (text.isEmpty()) return
        if (authRepository.currentUserId == null) return
        if (text.length > MAX_COMMENT_LENGTH) { _events.tryEmit(CommentMessage.TOO_LONG); return }
        val pending = PendingComment(
            localId = UUID.randomUUID().toString(), body = text,
            isAnonymous = isAnonymous, parentId = _uiState.value.replyTarget?.id
        )
        _uiState.update { it.copy(pending = it.pending + pending, replyTarget = null) }
        dispatchPending(pending)
    }

    private fun dispatchPending(pending: PendingComment) {
        viewModelScope.launch {
            postRepository.addComment(postId, pending.body, pending.isAnonymous, pending.parentId).fold(
                onSuccess = { created ->
                    _uiState.update { st ->
                        st.copy(
                            pending = st.pending.filterNot { it.localId == pending.localId },
                            comments = mergeById(st.comments, listOf(created)),
                            post = st.post?.let { p -> p.copy(commentCount = p.commentCount + 1) },
                            scrollToCommentId = created.id,
                            pinnedCommentId = if (created.parentId == null) created.id else st.pinnedCommentId
                        )
                    }
                },
                onFailure = { e ->
                    android.util.Log.e("ThreadDetailVM", "Posting comment failed", e)
                    _uiState.update { st ->
                        st.copy(pending = st.pending.map { if (it.localId == pending.localId) it.copy(failed = true) else it })
                    }
                    _events.tryEmit(postErrorMessage(e, CommentMessage.POST_FAILED))
                }
            )
        }
    }

    fun retryPending(localId: String) {
        val item = _uiState.value.pending.firstOrNull { it.localId == localId } ?: return
        val retrying = item.copy(failed = false)
        _uiState.update { st -> st.copy(pending = st.pending.map { if (it.localId == localId) retrying else it }) }
        dispatchPending(retrying)
    }

    fun discardPending(localId: String) {
        _uiState.update { st -> st.copy(pending = st.pending.filterNot { it.localId == localId }) }
    }

    /** Optimistic like with rollback; the server returns the authoritative count. */
    fun toggleCommentLike(commentId: String) {
        if (authRepository.currentUserId == null) return
        if (!likesInFlight.add(commentId)) return
        val before = _uiState.value.comments.firstOrNull { it.id == commentId }
        if (before == null) { likesInFlight.remove(commentId); return }
        val wantLiked = !before.likedByCurrentUser
        mapComment(commentId) {
            it.copy(likedByCurrentUser = wantLiked,
                upvotes = if (wantLiked) it.upvotes + 1 else (it.upvotes - 1).coerceAtLeast(0))
        }
        viewModelScope.launch {
            postRepository.toggleCommentLike(commentId).fold(
                onSuccess = { r -> mapComment(commentId) { it.copy(likedByCurrentUser = r.isLiked, upvotes = r.likeCount) } },
                onFailure = {
                    mapComment(commentId) { it.copy(likedByCurrentUser = before.likedByCurrentUser, upvotes = before.upvotes) }
                    _events.tryEmit(CommentMessage.LIKE_FAILED)
                }
            )
            likesInFlight.remove(commentId)
        }
    }

    fun editComment(commentId: String, newBody: String) {
        viewModelScope.launch {
            postRepository.updateComment(commentId, newBody).fold(
                onSuccess = { updated -> mapComment(commentId) { it.copy(body = updated.body, editedAt = updated.editedAt) } },
                onFailure = { e -> _events.tryEmit(postErrorMessage(e, CommentMessage.EDIT_FAILED)) }
            )
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            postRepository.deleteComment(commentId).fold(
                onSuccess = {
                    mapComment(commentId) { it.copy(isDeleted = true, deletedByAuthor = true, body = "") }
                    _uiState.update { st ->
                        st.copy(post = st.post?.let { p -> p.copy(commentCount = (p.commentCount - 1).coerceAtLeast(0)) })
                    }
                    _events.tryEmit(CommentMessage.DELETED)
                },
                onFailure = { _events.tryEmit(CommentMessage.DELETE_FAILED) }
            )
        }
    }

    fun reportComment(commentId: String, reason: String) {
        viewModelScope.launch {
            postRepository.reportComment(commentId, reason).fold(
                onSuccess = { _events.tryEmit(CommentMessage.REPORT_SENT) },
                onFailure = { _events.tryEmit(CommentMessage.REPORT_FAILED) }
            )
        }
    }

    private fun loadUserVoteAndSaved() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            postRepository.getUserVote(userId, postId).fold(
                onSuccess = { vote ->
                    android.util.Log.d(
                        "VOTE_DEBUG",
                        "Loaded vote = $vote for post = $postId"
                    )

                    _uiState.value = _uiState.value.copy(
                        userVote = vote,
                        voteLoaded = true
                    )
                },
                onFailure = {}
            )
            savedPostsRepository.isPostSaved(userId, postId).fold(
                onSuccess = { saved -> _uiState.value = _uiState.value.copy(isSaved = saved) },
                onFailure = {}
            )
        }
    }

    fun reportPost(reason: String) {
        val userId = authRepository.currentUserId ?: return

        viewModelScope.launch {
            postRepository.reportPost(
                postId = postId,
                reportedBy = userId,
                reason = reason
            )
        }
    }



    fun vote(voteType: String) {

        android.util.Log.d(
            "VOTE_DEBUG",
            "Clicked $voteType currentVote=${_uiState.value.userVote}"
        )

        val userId = authRepository.currentUserId ?: return
        val post = _uiState.value.post ?: return

        val currentVote = _uiState.value.userVote

        var upvotes = post.upvotes
        var downvotes = post.downvotes
        var newVote: String? = currentVote

        when {

            // Remove existing vote
            currentVote == voteType -> {

                if (voteType == "up") {
                    upvotes = (upvotes - 1).coerceAtLeast(0)
                } else {
                    downvotes = (downvotes - 1).coerceAtLeast(0)
                }

                newVote = null
            }

            // Change Down -> Up
            currentVote == "down" && voteType == "up" -> {

                downvotes = (downvotes - 1).coerceAtLeast(0)
                upvotes += 1

                newVote = "up"
            }

            // Change Up -> Down
            currentVote == "up" && voteType == "down" -> {

                upvotes = (upvotes - 1).coerceAtLeast(0)
                downvotes += 1

                newVote = "down"
            }

            // New Upvote
            voteType == "up" -> {

                upvotes += 1
                newVote = "up"
            }

            // New Downvote
            voteType == "down" -> {

                downvotes += 1
                newVote = "down"
            }
        }

        _uiState.value = _uiState.value.copy(
            post = post.copy(
                upvotes = upvotes,
                downvotes = downvotes
            ),
            userVote = newVote
        )

        viewModelScope.launch {

            val result = postRepository.votePost(
                userId,
                postId,
                voteType
            )

            viewModelScope.launch {

                postRepository.votePost(
                    userId,
                    postId,
                    voteType
                )
            }
        }
    }

    fun toggleSave() {
        val userId  = authRepository.currentUserId ?: return
        val isSaved = _uiState.value.isSaved
        _uiState.value = _uiState.value.copy(isSaved = !isSaved)
        viewModelScope.launch {
            if (isSaved) savedPostsRepository.unsavePost(userId, postId)
            else         savedPostsRepository.savePost(userId, postId)
        }
    }
}

@Composable
fun ThreadDetailScreen(
    navController: NavController,
    postId: String,
    commentId: String? = null,
    onBack: () -> Unit,
    viewModel: ThreadDetailViewModel = hiltViewModel()
) {
    val context   = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val uiState   by viewModel.uiState.collectAsState()
    var commentText by remember { mutableStateOf(CommentDrafts.get(postId)) }
    var isCommentAnonymous by remember { mutableStateOf(false) }
    LaunchedEffect(commentText) { CommentDrafts.set(postId, commentText) }

    val snackbarHostState = remember { SnackbarHostState() }
    val composerFocus = remember { FocusRequester() }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(60_000); nowMillis = System.currentTimeMillis() }
    }
    val expandedThreads = remember { mutableStateMapOf<String, Boolean>() }
    val threads = remember(uiState.comments, uiState.commentSort, uiState.pinnedCommentId) {
        buildCommentThreads(uiState.comments, uiState.commentSort.value, uiState.pinnedCommentId)
    }
    val highlightId = uiState.scrollToCommentId ?: commentId

    // Snackbar messages from the ViewModel (errors, confirmations)
    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            val res = when (msg) {
                CommentMessage.REPORT_SENT -> R.string.comment_report_sent
                CommentMessage.REPORT_FAILED -> R.string.comment_report_failed
                CommentMessage.POST_FAILED -> R.string.comment_post_failed
                CommentMessage.TOO_FAST -> R.string.comment_too_fast
                CommentMessage.TOO_LONG -> R.string.comment_too_long
                CommentMessage.EDIT_FAILED -> R.string.comment_edit_failed
                CommentMessage.DELETE_FAILED -> R.string.comment_delete_failed
                CommentMessage.DELETED -> R.string.comment_deleted_snack
                CommentMessage.LIKE_FAILED -> R.string.comment_like_failed
                CommentMessage.LOAD_FAILED -> R.string.comment_load_failed
            }
            snackbarHostState.showSnackbar(context.getString(res))
        }
    }

    // Replying: focus the shared composer
    LaunchedEffect(uiState.replyTarget?.id) {
        if (uiState.replyTarget != null) runCatching { composerFocus.requestFocus() }
    }
    val glowAnim = remember {
        Animatable(0f)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(highlightId, threads) {
        val target = highlightId
        if (target.isNullOrBlank()) return@LaunchedEffect
        val idx = threads.indexOfFirst { t -> t.root.id == target || t.replies.any { it.comment.id == target } }
        if (idx >= 0) {
            if (threads[idx].root.id != target) expandedThreads[threads[idx].root.id] = true
            val headerItems = (if (uiState.post != null) 1 else 0) + (if (!uiState.isLoggedIn) 1 else 0) +
                uiState.pending.size
            delay(250)
            listState.animateScrollToItem(headerItems + idx)
            if (uiState.scrollToCommentId != null) {
                delay(2600)
                viewModel.consumeScrollTarget()
            }
        }
    }

    LaunchedEffect(isCommentAnonymous) {

        if (isCommentAnonymous) {

            glowAnim.snapTo(0f)

            glowAnim.animateTo(
                1f,
                tween(350)
            )

            glowAnim.animateTo(
                0.3f,
                tween(250)
            )
        }
    }
    val maskScale by animateFloatAsState(
        targetValue = if (isCommentAnonymous) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mask_scale"
    )

    val maskRotation by animateFloatAsState(
        targetValue = if (isCommentAnonymous) 12f else 0f,
        animationSpec = tween(250),
        label = "mask_rotation"
    )

    val maskColor by animateColorAsState(
        targetValue =
            if (isCommentAnonymous)
                OrangePrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        label = "mask_color"
    )
    var showImagePreview by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    fun sharePost() {
        val post = uiState.post ?: return
        val i = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "${post.title}\n\n${post.body ?: ""}"); type = "text/plain"
        }
        context.startActivity(Intent.createChooser(i, null))
    }

    val saveScale by animateFloatAsState(
        if (uiState.isSaved) 1.3f else 1f,
        spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
        label = "save_scale"
    )
    var shareBurst by remember { mutableStateOf(false) }
    val shareScale by animateFloatAsState(if (shareBurst) 1.3f else 1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh), label = "share_scale", finishedListener = { shareBurst = false })
    val sendEnabled = commentText.isNotBlank() && commentText.length <= MAX_COMMENT_LENGTH
    val sendBg by animateColorAsState(if (sendEnabled) OrangePrimary
    else MaterialTheme.colorScheme.surfaceVariant, tween(200), label = "send_bg")

    // Full-screen image preview
    if (showImagePreview && !uiState.post?.imageUrl.isNullOrBlank()) {
        Dialog(onDismissRequest = { showImagePreview = false }) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.95f))) {
                AsyncImage(
                    model = uiState.post!!.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showImagePreview = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(
                        DensityManager.cardPadding.dp
                    )
                ) {
                    Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
    if (showReportDialog) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false },
            onReport = { reason ->
                viewModel.reportPost(reason)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.weight(1f))
                Text("Thread", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                if (uiState.isLoggedIn) {
                    IconButton(onClick = { viewModel.toggleSave() }) {
                        Icon(
                            if (uiState.isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            null,
                            tint =
                                if (uiState.isSaved)
                                    OrangePrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.scale(saveScale)
                        )
                    }
                }
                IconButton(onClick = { shareBurst = true; sharePost() }) {
                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.scale(shareScale))
                }
            }
        },
        bottomBar = {
            if (uiState.isLoggedIn) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {

                    AnimatedVisibility(
                        visible = uiState.replyTarget != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val target = uiState.replyTarget
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OrangeSubtle)
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(
                                    R.string.comment_composer_replying,
                                    if (target != null) commentDisplayName(target) else ""
                                ),
                                color = OrangePrimary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.setReplyTarget(null) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.comment_composer_cancel_reply),
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isCommentAnonymous)
                                        listOf(
                                            OrangePrimary.copy(
                                                0.10f + glowAnim.value * 0.15f
                                            ),
                                            OrangeSubtle.copy(0.20f),
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    else
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            )
                            .clickable {
                                isCommentAnonymous = !isCommentAnonymous
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        AnimatedContent(
                            targetState = isCommentAnonymous,
                            transitionSpec = {
                                fadeIn(
                                    animationSpec = tween(250)
                                ) +
                                        scaleIn(
                                            initialScale = 0.7f
                                        ) togetherWith
                                        fadeOut() +
                                        scaleOut(
                                            targetScale = 1.3f
                                        )
                            },
                            label = "mask_icon"
                        ) { anonymous ->

                            Icon(
                                imageVector =
                                    if (anonymous)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Person,
                                contentDescription = null,
                                tint = maskColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(maskScale)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        AnimatedContent(
                            targetState = isCommentAnonymous,
                            label = "anon_text"
                        ) { anonymous ->

                            if (anonymous) {

                                Text(
                                    stringResource(R.string.comment_anon_mode_on),
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )

                            } else {

                                Text(
                                    stringResource(R.string.comment_public),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isCommentAnonymous,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    OrangeSubtle.copy(0.15f)
                                )
                                .border(
                                    1.dp,
                                    OrangePrimary.copy(0.25f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    stringResource(R.string.comment_anon_identity),
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                stringResource(R.string.comment_anon_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it.take(MAX_COMMENT_LENGTH + 100) },
                            modifier = Modifier.weight(1f).focusRequester(composerFocus),
                            placeholder = {
                                Crossfade(targetState = isCommentAnonymous, label = "placeholder") { anonymous ->
                                    Text(
                                        stringResource(
                                            if (anonymous) R.string.comment_composer_hint_anon
                                            else R.string.comment_composer_hint
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = DividerColor,
                                cursorColor = OrangePrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            minLines = 1,
                            maxLines = 5,
                            supportingText = if (commentText.length > MAX_COMMENT_LENGTH - 300) {
                                {
                                    Text(
                                        stringResource(R.string.comment_char_counter, commentText.length, MAX_COMMENT_LENGTH),
                                        color = if (commentText.length > MAX_COMMENT_LENGTH)
                                            MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else null
                        )

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(sendBg)
                                .pressScale {
                                    if (sendEnabled) {
                                        viewModel.sendComment(commentText, isCommentAnonymous)
                                        commentText = ""
                                        isCommentAnonymous = false
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.comment_send),
                                tint = if (sendEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) { items(4) { ShimmerPostCard() } }
            return@Scaffold
        }
        Box(Modifier.padding(paddingValues).fillMaxSize()) {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing),
            onRefresh = { viewModel.refreshThread() },
            modifier = Modifier.fillMaxSize()
        ) {
        CompositionLocalProvider(LocalNowMillis provides nowMillis) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ── Post content ──────────────────────────────────────────────
            uiState.post?.let { post ->
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.background), 0f, 400f)
                            )
                            // Only vertical padding here on purpose: the post
                            // image below is meant to bleed edge-to-edge to
                            // the screen, exactly like Reddit's own thread
                            // page does. Every other row re-applies the
                            // horizontal inset itself.
                            .padding(vertical = DensityManager.cardPadding.dp)
                    ) {
                        // Category + time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DensityManager.cardPadding.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryBadge(post.category)
                            Spacer(Modifier.weight(1f))
                            Text(
                                post.timeAgo(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Title
                        Text(
                            post.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineMedium,
                            lineHeight = 27.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = DensityManager.cardPadding.dp)
                        )

                        Spacer(Modifier.height(8.dp))

                        // Meta
                        Row(
                            modifier = Modifier.padding(horizontal = DensityManager.cardPadding.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (post.isAnonymous) "u/Anonymous" else "u/${post.username ?: "unknown"}",
                                color = OrangePrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    if (!post.isAnonymous) navController.navigate("user_profile/${post.userId}")
                                }
                            )
                            if (post.areaTag != null) {
                                Text("  ·  ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Text(" ${post.areaTag}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        // Image — edge-to-edge, real aspect ratio, Reddit-style.
                        // No horizontal padding on purpose: this is the one
                        // element on the whole screen meant to touch both
                        // edges, exactly like Reddit's thread detail page.
                        if (!post.imageUrl.isNullOrBlank()) {

                            Spacer(Modifier.height(14.dp))

                            AdaptivePostImage(
                                imageUrl = post.imageUrl,
                                modifier = Modifier.fillMaxWidth(),
                                minRatio = 0.6f,   // the detail page allows a bit more height than the feed before cropping
                                maxRatio = 1.91f,
                                cornerRadius = 0.dp,
                                onClick = { showImagePreview = true }
                            )
                        }

                        // Body
                        if (!post.body.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                post.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = DensityManager.cardPadding.dp),
                                lineHeight = 22.sp
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Stats bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DensityManager.cardPadding.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Upvote
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (uiState.voteLoaded && uiState.userVote == "up")
                                            OrangeSubtle
                                        else
                                            MaterialTheme.colorScheme.surface
                                    )
                                    .pressScale {

                                        if (uiState.voteLoaded) {
                                            viewModel.vote("up")
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, null, tint = if (uiState.voteLoaded && uiState.userVote == "up") OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                AnimatedContent(
                                    post.upvotes,
                                    transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
                                    label = "upvotes"
                                ) { count ->
                                    Text(formatCount(count), color = if (uiState.voteLoaded && uiState.userVote == "up") OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Comments count
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                AnimatedContent(
                                    uiState.comments.size,
                                    transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
                                    label = "ccount"
                                ) { count -> Text("$count", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                            }

                            Spacer(Modifier.weight(1f))

                            var menuExpanded by remember { mutableStateOf(false) }

                            Box {

                                IconButton(
                                    onClick = { menuExpanded = true }
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF26191A), Color(0xFF111116), Color(0xFF21171B))
                                            ),
                                            RoundedCornerShape(18.dp)
                                        )
                                        .border(1.dp, Color(0xFFFF6848).copy(alpha = 0.78f), RoundedCornerShape(18.dp))
                                ) {

                                    DropdownMenuItem(
                                        text = { Text("Report Post", color = PrimaryText) },
                                        onClick = {
                                            menuExpanded = false
                                            showReportDialog = true
                                        }
                                    )
                                }
                            }

                            /*
                            // View count hidden from the thread UI for now.
                            // Keep this code commented out so it can be restored later if needed.
                            if (post.viewCount > 0) {
                                Text(
                                    "${formatCount(post.viewCount)} views",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            */
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = DensityManager.cardPadding.dp),
                            color = DividerColor,
                            thickness = 0.5.dp
                        )
                        Spacer(Modifier.height(14.dp))

                        // Comments header
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = DensityManager.cardPadding.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.comments_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.weight(1f))
                            val total = post.commentCount
                            Text(
                                if (total == 1) stringResource(R.string.comments_count_one)
                                else stringResource(R.string.comments_count, total),
                                color = OrangePrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = DensityManager.cardPadding.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                CommentSort.TOP to R.string.comments_sort_top,
                                CommentSort.NEW to R.string.comments_sort_new,
                                CommentSort.OLD to R.string.comments_sort_old
                            ).forEach { (sort, label) ->
                                FilterChip(
                                    selected = uiState.commentSort == sort,
                                    onClick = { viewModel.setCommentSort(sort) },
                                    label = { Text(stringResource(label)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangeSubtle,
                                        selectedLabelColor = OrangePrimary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }

            // ── Login prompt ──────────────────────────────────────────────
            if (!uiState.isLoggedIn) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(OrangeSubtle)
                            .border(1.dp, OrangePrimary.copy(0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(
                                stringResource(R.string.comment_signin_prompt),
                                color = OrangePrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── My comments that are still sending (or failed) ──────────────
            items(uiState.pending, key = { "pending_" + it.localId }) { pending ->
                PendingCommentRow(
                    body = pending.body,
                    isAnonymous = pending.isAnonymous,
                    failed = pending.failed,
                    onRetry = { viewModel.retryPending(pending.localId) },
                    onDiscard = { viewModel.discardPending(pending.localId) },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            if (uiState.commentsLoading && uiState.comments.isEmpty()) {
                // ── Loading skeletons ─────────────────────────────────────────
                items(3) { ShimmerPostCard(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) }
            } else if (threads.isEmpty() && uiState.pending.isEmpty()) {
                // ── Empty comments ────────────────────────────────────────────
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                stringResource(R.string.comments_empty_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            if (uiState.isLoggedIn) {
                                TextButton(onClick = { runCatching { composerFocus.requestFocus() } }) {
                                    Text(stringResource(R.string.comments_empty_cta), color = OrangePrimary)
                                }
                            } else {
                                Text(
                                    stringResource(R.string.comments_empty_login),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Conversations: top-level comment + all replies under it ───
                items(threads, key = { it.root.id }) { thread ->
                    val root = thread.root
                    val repliesOpen = expandedThreads[root.id] == true
                    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                        CommentCard(
                            navController = navController,
                            comment = root,
                            isLoggedIn = uiState.isLoggedIn,
                            isAdmin = uiState.isAdmin,
                            isHighlighted = root.id == highlightId,
                            onUpvote = viewModel::toggleCommentLike,
                            onReply = { viewModel.setReplyTarget(it) },
                            onReport = viewModel::reportComment,
                            onEdit = viewModel::editComment,
                            onDelete = viewModel::deleteComment
                        )

                        if (thread.replies.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { expandedThreads[root.id] = !repliesOpen }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chevron by animateFloatAsState(
                                    if (repliesOpen) 180f else 0f, label = "replies_chevron"
                                )
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(18.dp).rotate(chevron)
                                )
                                Spacer(Modifier.width(4.dp))
                                val n = thread.replies.size
                                Text(
                                    when {
                                        repliesOpen -> stringResource(R.string.comments_hide_replies)
                                        n == 1 -> stringResource(R.string.comments_view_one_reply)
                                        else -> stringResource(R.string.comments_view_replies, n)
                                    },
                                    color = OrangePrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            AnimatedVisibility(
                                visible = repliesOpen,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 14.dp, top = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    thread.replies.forEach { reply ->
                                        key(reply.comment.id) {
                                            CommentCard(
                                                navController = navController,
                                                comment = reply.comment,
                                                isLoggedIn = uiState.isLoggedIn,
                                                isAdmin = uiState.isAdmin,
                                                isHighlighted = reply.comment.id == highlightId,
                                                isReply = true,
                                                replyToName = reply.replyToName,
                                                onUpvote = viewModel::toggleCommentLike,
                                                onReply = { viewModel.setReplyTarget(it) },
                                                onReport = viewModel::reportComment,
                                                onEdit = viewModel::editComment,
                                                onDelete = viewModel::deleteComment
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = DividerColor.copy(0.5f), thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                val loadedRoots = uiState.comments.count { it.parentId == null }
                if (loadedRoots < uiState.totalRoots) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), Alignment.Center) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = OrangePrimary
                                )
                            } else {
                                TextButton(onClick = { viewModel.loadMoreComments() }) {
                                    Text(stringResource(R.string.comments_load_more), color = OrangePrimary)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
        }
        }

        // "N new comments" pill instead of the list jumping under the reader
        AnimatedVisibility(
            visible = uiState.newCommentCount > 0,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrangePrimary)
                    .clickable {
                        viewModel.clearNewComments()
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (uiState.newCommentCount == 1) stringResource(R.string.comments_new_one)
                    else stringResource(R.string.comments_new_many, uiState.newCommentCount),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        }
    }
}
