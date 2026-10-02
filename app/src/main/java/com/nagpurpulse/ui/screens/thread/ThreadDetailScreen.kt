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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.nagpurpulse.ui.preferences.DensityManager
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

data class ThreadDetailUiState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSubmittingComment: Boolean = false,
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
        loadComments()
        loadUserVoteAndSaved()

        // Keep this thread synchronized with comments/replies created by other users.
        viewModelScope.launch {
            runCatching {
                postRepository.subscribeToComments(postId).collect {
                    delay(250)
                    loadPost()
                    loadComments()
                }
            }.onFailure { error ->
                android.util.Log.e("THREAD_COMMENTS_RT", "Comment realtime subscription stopped", error)
            }
        }
    }

    fun refreshThread() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            try {
                val postResult = postRepository.getPostById(postId)
                val commentsResult = postRepository.getComments(postId)
                postResult.onSuccess { post ->
                    _uiState.value = _uiState.value.copy(post = post)
                }
                commentsResult.onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(comments = comments)
                }
                if (postResult.isFailure && commentsResult.isFailure) {
                    _uiState.value = _uiState.value.copy(error = postResult.exceptionOrNull()?.message)
                }
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    private fun loadPost() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            postRepository.getPostById(postId).fold(
                onSuccess = { post -> _uiState.value = _uiState.value.copy(post = post, isLoading = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(error = e.message, isLoading = false) }
            )
        }
    }

    private fun loadComments() {
        viewModelScope.launch {
            postRepository.getComments(postId).fold(
                onSuccess = { comments -> _uiState.value = _uiState.value.copy(comments = comments) },
                onFailure = {}
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

    fun submitComment(body: String, isAnonymous: Boolean = false) {

        android.util.Log.d(
            "ANON_TEST",
            "ViewModel Received = $isAnonymous"
        )
        val userId = authRepository.currentUserId ?: return
        if (body.isBlank()) return
        // If anonymous, generate persistent alias for this user in this post
        val anonAlias = if (isAnonymous) {
            com.nagpurpulse.data.model.generateAnonAlias(postId, userId)
        } else null
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingComment = true)
            postRepository.addComment(postId, userId, body, isAnonymous, anonAlias = anonAlias).fold(
                onSuccess = { comment ->
                    _uiState.value = _uiState.value.copy(
                        comments = listOf(comment) + _uiState.value.comments,
                        post     = _uiState.value.post?.copy(commentCount = _uiState.value.post!!.commentCount + 1),
                        isSubmittingComment = false
                    )
                },
                onFailure = { _uiState.value = _uiState.value.copy(isSubmittingComment = false) }
            )
        }
    }

    fun submitReply(parentCommentId: String, body: String, isAnonymous: Boolean = false) {
        val userId = authRepository.currentUserId ?: return
        if (body.isBlank()) return
        val anonAlias = if (isAnonymous) com.nagpurpulse.data.model.generateAnonAlias(postId, userId) else null
        viewModelScope.launch {
            postRepository.addComment(postId, userId, body, isAnonymous, parentCommentId, anonAlias = anonAlias).fold(
                onSuccess = { newReply -> _uiState.value = _uiState.value.copy(comments = _uiState.value.comments + newReply) },
                onFailure = {}
            )
        }
    }

    fun upvoteComment(commentId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            postRepository.upvoteComment(commentId, userId).fold(
                onSuccess = { isLiked ->
                    _uiState.value = _uiState.value.copy(
                        comments = _uiState.value.comments.map { c ->
                            if (c.id == commentId) c.copy(
                                upvotes = (c.upvotes + if (isLiked) 1 else -1).coerceAtLeast(0),
                                likedByCurrentUser = isLiked
                            ) else c
                        }
                    )
                },
                onFailure = { error ->
                    android.util.Log.e("COMMENT_LIKE", "Failed to toggle comment like", error)
                }
            )
        }
    }

    fun updateComment(commentId: String, newBody: String) {
        viewModelScope.launch {
            postRepository.updateComment(commentId, newBody).fold(
                onSuccess = { updated ->
                    _uiState.value = _uiState.value.copy(
                        comments = _uiState.value.comments.map { comment ->
                            if (comment.id == updated.id) updated else comment
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }

    fun reportComment(
        commentId: String,
        reason: String
    ) {
        val userId = authRepository.currentUserId ?: return

        viewModelScope.launch {

            postRepository.reportComment(
                commentId = commentId,
                reportedBy = userId,
                reason = reason
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
    var commentText by remember { mutableStateOf("") }
    var isCommentAnonymous by remember { mutableStateOf(false) }
    val glowAnim = remember {
        Animatable(0f)
    }

    val listState = rememberLazyListState()

    LaunchedEffect(commentId, uiState.comments) {

        if (commentId.isNullOrBlank()) return@LaunchedEffect

        val parentComments = uiState.comments.filter { it.parentId == null }

        var index = parentComments.indexOfFirst { it.id == commentId }

        if (index == -1) {
            val reply = uiState.comments.firstOrNull { it.id == commentId }
            val parentId = reply?.parentId
            index = parentComments.indexOfFirst { it.id == parentId }
        }

        if (index >= 0) {
            delay(250)
            listState.animateScrollToItem(index + 1)
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
    val collapsedThreads = remember { mutableStateMapOf<String, Boolean>() }
    val visibleReplies = remember {
        mutableStateMapOf<String, Int>()
    }

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
    val sendEnabled = commentText.isNotBlank()
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
                                    " Anonymous Mode Enabled",
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )

                            } else {

                                Text(
                                    " Public Comment",
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
                                    "Anonymous Identity",
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                "Your identity will be hidden from other users",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = {

                                Crossfade(
                                    targetState = isCommentAnonymous,
                                    label = "placeholder"
                                ) { anonymous ->

                                    Text(
                                        if (anonymous)
                                            "Speak freely. Nobody will know it's you"
                                        else
                                            "Share your thoughts…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            },
                            shape = RoundedCornerShape(26.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = DividerColor,
                                cursorColor = OrangePrimary,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            singleLine = true
                        )

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(sendBg)
                                .pressScale {
                                    if (sendEnabled) {

                                        android.util.Log.d(
                                            "ANON_TEST",
                                            "Checkbox Value = $isCommentAnonymous"
                                        )

                                        viewModel.submitComment(
                                            commentText,
                                            isCommentAnonymous

                                        )

                                        commentText = ""
                                        isCommentAnonymous = false

                                        focusManager.clearFocus()
                                        keyboardController?.hide()

                                        focusManager.clearFocus()

                                        keyboardController?.hide()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isSubmittingComment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint =
                                        if (sendEnabled)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing),
            onRefresh = { viewModel.refreshThread() },
            modifier = Modifier.padding(paddingValues)
        ) {
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
                                    onDismissRequest = { menuExpanded = false }
                                ) {

                                    DropdownMenuItem(
                                        text = { Text("Report Post") },
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
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DensityManager.cardPadding.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Text("🔥 ", style = MaterialTheme.typography.titleMedium)

                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )

                            Text("Top Comments", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${uiState.comments.size} replies",
                                color = OrangePrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
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
                                "Sign in to vote, comment & save",
                                color = OrangePrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Empty comments ────────────────────────────────────────────
            if (uiState.comments.isEmpty()) {
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
                            Text("No comments yet", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (uiState.isLoggedIn) "Be the first to comment!" else "Login to join the conversation",
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            } else {
                // ── Threaded comments ─────────────────────────────────────
                val parentComments = uiState.comments.filter { it.parentId == null }

                itemsIndexed(parentComments) { index, parentComment ->
                    StaggeredItem(index) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                            // Parent comment
                            CommentCard(
                                navController = navController,
                                comment       = parentComment,
                                isLoggedIn    = uiState.isLoggedIn,

                                currentUserId = uiState.currentUserId,
                                isAdmin = uiState.isAdmin,
                                onEdit = { commentId, newBody ->
                                    viewModel.updateComment(commentId, newBody)
                                },


                                isHighlighted = parentComment.id == commentId,
                                onUpvote      = { commentId -> viewModel.upvoteComment(commentId) },
                                onReplySubmit = { pid, body, anonymous ->
                                    viewModel.submitReply(
                                        pid,
                                        body,
                                        anonymous
                                    )

                                },
                                onReport = { commentId, reason ->
                                    viewModel.reportComment(commentId, reason)
                                }
                            )

                            // Replies toggle
                            val replies = uiState.comments.filter { it.parentId == parentComment.id }
                            val visibleCount =
                                visibleReplies[parentComment.id] ?: 0
                            val totalReplies = replies.size + replies.sumOf { r -> uiState.comments.count { it.parentId == r.id } }

                            if (replies.isNotEmpty()) {



                                Text(
                                    text = when {

                                        visibleCount == 0 ->
                                            "▶ View ${replies.size} replies"

                                        visibleCount < replies.size ->
                                            "▶ View ${replies.size - visibleCount} more replies"

                                        else ->
                                            "▼ Hide replies"
                                    },
                                    color = OrangePrimary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(start = 40.dp, top = 4.dp, bottom = 6.dp)
                                        .pressScale {

                                            if (visibleCount == 0) {

                                                visibleReplies[parentComment.id] = 1

                                            } else if (visibleCount < replies.size) {

                                                visibleReplies[parentComment.id] =
                                                    visibleCount + 1

                                            } else {

                                                visibleReplies[parentComment.id] = 0
                                            }
                                        }
                                )
                            }

                            if (visibleCount > 0){
                                replies.take(visibleCount).forEach { reply ->
                                    Spacer(Modifier.height(6.dp))
                                    CommentCard(
                                        navController = navController,
                                        comment       = reply,
                                        isLoggedIn    = uiState.isLoggedIn,
                                        currentUserId = uiState.currentUserId,
                                        isAdmin       = uiState.isAdmin,
                                        onEdit        = { editedCommentId, newBody ->
                                            viewModel.updateComment(editedCommentId, newBody)
                                        },
                                        onUpvote      = { commentId -> viewModel.upvoteComment(commentId) },
                                        onReport = { commentId, reason ->
                                            viewModel.reportComment(commentId, reason)
                                        },
                                        onReplySubmit = { pid, body, anonymous ->
                                            viewModel.submitReply(
                                                pid,
                                                body,
                                                anonymous
                                            )
                                        },
                                        depth         = 1,
                                        modifier      = Modifier.padding(start = 24.dp)
                                    )
                                    // Nested replies
                                    uiState.comments.filter { it.parentId == reply.id }.forEach { nested ->
                                        Spacer(Modifier.height(6.dp))
                                        CommentCard(
                                            navController = navController,
                                            comment       = nested,
                                            isLoggedIn    = uiState.isLoggedIn,
                                            currentUserId = uiState.currentUserId,
                                            isAdmin       = uiState.isAdmin,
                                            onEdit        = { editedCommentId, newBody ->
                                                viewModel.updateComment(editedCommentId, newBody)
                                            },
                                            onReport = { commentId, reason ->
                                                viewModel.reportComment(commentId, reason)
                                            },
                                            onUpvote      = { commentId -> viewModel.upvoteComment(commentId) },
                                            onReplySubmit = { pid, body, anonymous ->
                                                viewModel.submitReply(
                                                    pid,
                                                    body,
                                                    anonymous
                                                )
                                            },
                                            depth         = 2,
                                            modifier      = Modifier.padding(start = 48.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = DividerColor.copy(0.5f), thickness = 0.5.dp)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
        }
    }
}
