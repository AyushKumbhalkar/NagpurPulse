//This is the HomeSCreen.kt file and below is the path of the file
// java/com/nagpurpulse/ui/screens/home/HomeScreen.kt
//
// ViewModel + screen wiring for Home. Reusable visual pieces live in HomeComponents.kt.

package com.nagpurpulse.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.location.LocationHelper
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.remote.weather.WeatherRepository
import com.nagpurpulse.data.repository.AdminRepository
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.data.repository.SavedPostsRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.ui.components.PostCard
import com.nagpurpulse.ui.components.ShimmerPostCard
import com.nagpurpulse.ui.components.StaggeredItem
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.theme.OrangePrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

// ── ViewModel ─────────────────────────────────────────────────────────────────
data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val userVotes: Map<String, String?> = emptyMap(),

    // First load with nothing to show yet → full skeleton.
    val isLoading: Boolean = false,
    // Pull-to-refresh in progress.
    val isRefreshing: Boolean = false,
    // Sort / category changed while old posts are still on screen (stale-while-revalidate).
    val isSwitching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,

    val error: String? = null,
    val sortBy: String = "top",
    val category: String? = null,

    val unreadNotifCount: Int = 0,
    val hasLoadedUnreadNotifications: Boolean = false,
    val unreadMsgCount: Int = 0,
    val hasLoadedUnreadMessages: Boolean = false,

    val temperature: Int? = null,
    // Raw PM2.5 (µg/m³) as returned by WeatherRepository; HomeComponents converts it to an AQI estimate.
    val aqi: Int? = null,
    val weatherAttempted: Boolean = false,
    val currentArea: String = "Near You",

    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val userKarma: Int? = null,

    val onlineCount: Int = 0,
    val newPostsCount: Int = 0,
    val snackbarMessage: String? = null
)

private const val FEED_PAGE_SIZE = 20
private const val BADGE_REFRESH_MS = 60_000L
private const val NEW_POSTS_PROBE_MS = 90_000L

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val savedPostsRepository: SavedPostsRepository,
    private val weatherRepository: WeatherRepository,
    private val locationHelper: LocationHelper,
    private val adminRepository: AdminRepository,
    private val messageRepository: MessageRepository,
    private val notificationRepository: NotificationRepository,
    private val presenceRepository: PresenceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    var isAdmin by mutableStateOf(false)
        private set

    // Page index of the last page merged into the feed (0 = first page).
    private var currentPage = 0

    // Every post id the user has been shown; used to detect genuinely new posts.
    private val seenPostIds = mutableSetOf<String>()

    init {
        loadPosts()
        loadWeather()
        loadLocation()
        loadProfile()
        checkAdminStatus()
        loadUnreadMessages()
        subscribeToUnreadMessages()
        loadUnreadNotifications()
        subscribeToUnreadNotifications()
        startPresenceWhenAuthenticated()
        observePresence()
        startNewPostsProbe()
    }

    // ── Profile (name / avatar / karma for greeting + composer) ──────────────
    private fun loadProfile() {
        viewModelScope.launch {
            repeat(10) {
                if (authRepository.currentUserId != null) {
                    authRepository.getCurrentProfile().onSuccess { profile ->
                        val firstName = (profile.displayName?.takeIf { it.isNotBlank() } ?: profile.username)
                            .trim()
                            .substringBefore(' ')
                        _uiState.update {
                            it.copy(
                                userName = firstName.ifBlank { null },
                                userAvatarUrl = profile.avatarUrl,
                                userKarma = profile.karma
                            )
                        }
                    }
                    return@launch
                }
                delay(1_000)
            }
        }
    }

    private fun observePresence() {
        viewModelScope.launch {
            presenceRepository.onlineUserIds.collect { ids ->
                _uiState.update { it.copy(onlineCount = ids.size) }
            }
        }
    }

    // ── Unread counters ──────────────────────────────────────────────────────
    private fun loadUnreadMessages() {
        viewModelScope.launch {
            messageRepository.getConversations().onSuccess { conversations ->
                _uiState.update {
                    it.copy(
                        unreadMsgCount = conversations.sumOf { c -> c.myUnreadCount },
                        hasLoadedUnreadMessages = true
                    )
                }
            }
        }
    }

    private fun subscribeToUnreadMessages() {
        viewModelScope.launch {
            messageRepository.subscribeToConversations().collect {
                loadUnreadMessages()
            }
        }
        // Realtime is the fast path; a slow periodic refresh recovers missed events and
        // keeps the Home badge consistent across devices.
        viewModelScope.launch {
            while (true) {
                delay(BADGE_REFRESH_MS)
                loadUnreadMessages()
            }
        }
    }

    private fun loadUnreadNotifications() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            notificationRepository.getUnreadCount(userId).onSuccess { count ->
                _uiState.update {
                    it.copy(unreadNotifCount = count, hasLoadedUnreadNotifications = true)
                }
            }
        }
    }

    private fun subscribeToUnreadNotifications() {
        viewModelScope.launch {
            var userId = authRepository.currentUserId
            repeat(30) {
                if (userId == null) {
                    delay(1_000)
                    userId = authRepository.currentUserId
                }
            }
            val resolvedUserId = userId ?: return@launch
            notificationRepository.subscribeToNotifications(resolvedUserId).collect {
                loadUnreadNotifications()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(BADGE_REFRESH_MS)
                loadUnreadNotifications()
            }
        }
    }

    private fun startPresenceWhenAuthenticated() {
        viewModelScope.launch {
            repeat(30) {
                if (authRepository.currentUserId != null) {
                    presenceRepository.start()
                    return@launch
                }
                delay(1_000)
            }
        }
    }

    private fun checkAdminStatus() {
        viewModelScope.launch {
            isAdmin = adminRepository.isAdmin()
        }
    }

    // ── Weather / location ───────────────────────────────────────────────────
    private fun loadWeather() {
        viewModelScope.launch {
            try {
                val coords = locationHelper.getCoordinates()
                if (coords != null) {
                    val result = weatherRepository.getWeatherAndAqi(coords.first, coords.second)
                    _uiState.update { it.copy(temperature = result.first, aqi = result.second) }
                }
            } catch (e: Exception) {
                Log.w("HomeWeather", "Weather unavailable", e)
            } finally {
                _uiState.update { it.copy(weatherAttempted = true) }
            }
        }
    }

    private fun loadLocation() {
        viewModelScope.launch {
            try {
                val area = locationHelper.getSubLocality()
                _uiState.update { it.copy(currentArea = area) }
            } catch (e: Exception) {
                Log.w("HomeLocation", "Location unavailable", e)
            }
        }
    }

    /** Re-reads location + weather, e.g. after the user grants permission or taps the area chip. */
    fun refreshLocation() {
        _uiState.update { it.copy(weatherAttempted = false) }
        loadLocation()
        loadWeather()
    }

    // ── Feed ─────────────────────────────────────────────────────────────────
    // Only the most recently requested feed load may update the visible posts.
    // This prevents a slower response for an old tab from replacing a newer tab's feed.
    private var latestPostsLoadRequestId = 0L

    fun loadPosts(refresh: Boolean = false) {
        val requestId = ++latestPostsLoadRequestId
        val requestedSort = _uiState.value.sortBy
        val requestedCategory = _uiState.value.category

        viewModelScope.launch {
            // Keep whatever is on screen while the new feed loads (stale-while-revalidate);
            // only show the full skeleton when there is nothing to show yet.
            _uiState.update { s ->
                s.copy(
                    isLoading = !refresh && s.posts.isEmpty(),
                    isRefreshing = refresh,
                    isSwitching = !refresh && s.posts.isNotEmpty()
                )
            }

            postRepository.getPosts(
                category = requestedCategory,
                sortBy = requestedSort,
                page = 0,
                pageSize = FEED_PAGE_SIZE
            ).fold(
                onSuccess = { posts ->
                    val current = _uiState.value
                    val isLatestRequest = requestId == latestPostsLoadRequestId
                    val matchesCurrentFeed = requestedSort == current.sortBy &&
                        requestedCategory == current.category

                    if (!isLatestRequest || !matchesCurrentFeed) return@fold

                    currentPage = 0
                    seenPostIds.addAll(posts.map { it.id })

                    _uiState.update {
                        it.copy(
                            posts = posts,
                            isLoading = false,
                            isRefreshing = false,
                            isSwitching = false,
                            isLoadingMore = false,
                            endReached = posts.isEmpty(),
                            newPostsCount = 0,
                            error = null
                        )
                    }
                    loadSavedPostIds()
                    loadUserVotes()
                },
                onFailure = { e ->
                    Log.e("HomeFeed", "loadPosts failed (sort=$requestedSort)", e)
                    val isLatestRequest = requestId == latestPostsLoadRequestId
                    val matchesCurrentFeed = requestedSort == _uiState.value.sortBy &&
                        requestedCategory == _uiState.value.category

                    if (isLatestRequest && matchesCurrentFeed) {
                        _uiState.update { s ->
                            val hasPosts = s.posts.isNotEmpty()
                            s.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isSwitching = false,
                                isLoadingMore = false,
                                // Real error screen only when there is nothing else to show;
                                // otherwise keep the feed and tell the user via snackbar.
                                error = if (hasPosts) null else (e.message ?: "Couldn't load posts"),
                                snackbarMessage = if (hasPosts) {
                                    "Couldn't refresh. Check your connection."
                                } else {
                                    s.snackbarMessage
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    /** Appends the next page of the current feed. Safe to call repeatedly. */
    fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isRefreshing || s.isSwitching || s.isLoadingMore ||
            s.endReached || s.posts.isEmpty()
        ) return

        val requestId = latestPostsLoadRequestId
        val requestedSort = s.sortBy
        val requestedCategory = s.category
        val nextPage = currentPage + 1

        _uiState.update { it.copy(isLoadingMore = true) }

        viewModelScope.launch {
            postRepository.getPosts(
                category = requestedCategory,
                sortBy = requestedSort,
                page = nextPage,
                pageSize = FEED_PAGE_SIZE
            ).fold(
                onSuccess = { more ->
                    val current = _uiState.value
                    val isStale = requestId != latestPostsLoadRequestId ||
                        requestedSort != current.sortBy ||
                        requestedCategory != current.category
                    if (isStale) {
                        _uiState.update { it.copy(isLoadingMore = false) }
                        return@fold
                    }

                    currentPage = nextPage
                    val existingIds = current.posts.map { it.id }.toSet()
                    val fresh = more.filter { it.id !in existingIds }
                    seenPostIds.addAll(fresh.map { it.id })

                    _uiState.update {
                        it.copy(
                            posts = it.posts + fresh,
                            isLoadingMore = false,
                            endReached = more.isEmpty()
                        )
                    }
                    if (fresh.isNotEmpty()) {
                        loadSavedPostIds()
                        loadUserVotes()
                    }
                },
                onFailure = { e ->
                    Log.w("HomeFeed", "loadMore failed (page=$nextPage)", e)
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            )
        }
    }

    // ── "N new posts" pill ───────────────────────────────────────────────────
    private fun startNewPostsProbe() {
        viewModelScope.launch {
            while (true) {
                delay(NEW_POSTS_PROBE_MS)
                val s = _uiState.value
                if (s.isLoading || s.isRefreshing || s.posts.isEmpty()) continue

                val myId = authRepository.currentUserId
                postRepository.getPosts(
                    category = null,
                    sortBy = "new",
                    page = 0,
                    pageSize = 10
                ).onSuccess { latest ->
                    val unseen = latest.count { it.id !in seenPostIds && it.userId != myId }
                    if (unseen > 0) {
                        _uiState.update { it.copy(newPostsCount = unseen) }
                    }
                }
            }
        }
    }

    /** Jumps to the newest posts across all categories (used by the "N new posts" pill). */
    fun showLatest() {
        _uiState.update { it.copy(sortBy = "new", category = null, newPostsCount = 0) }
        loadPosts()
    }

    private suspend fun loadSavedPostIds() {
        val userId = authRepository.currentUserId ?: return
        savedPostsRepository.getSavedPostIds(userId).fold(
            onSuccess = { ids -> _uiState.update { it.copy(savedPostIds = ids.toSet()) } },
            onFailure = {}
        )
    }

    // One batched query for every visible post (was: one request per post).
    private suspend fun loadUserVotes() {
        val userId = authRepository.currentUserId ?: return
        val ids = _uiState.value.posts.map { it.id }
        if (ids.isEmpty()) return

        postRepository.getUserVotesForPosts(userId, ids).fold(
            onSuccess = { voteMap ->
                _uiState.update { s ->
                    val merged = s.userVotes.toMutableMap()
                    ids.forEach { id -> merged[id] = voteMap[id] }
                    s.copy(userVotes = merged)
                }
            },
            // Preserve the last known votes if Supabase temporarily fails to read them.
            onFailure = { error -> Log.w("HomeVotes", "Couldn't refresh votes", error) }
        )
    }

    fun toggleSave(postId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            val isSaved = postId in _uiState.value.savedPostIds
            if (isSaved) savedPostsRepository.unsavePost(userId, postId)
            else savedPostsRepository.savePost(userId, postId)
            loadSavedPostIds()
        }
    }

    fun vote(postId: String, voteType: String) {

        val currentVote = _uiState.value.userVotes[postId]
        val userId = authRepository.currentUserId ?: return

        _uiState.value = _uiState.value.copy(
            posts = _uiState.value.posts.map { post ->

                if (post.id != postId) return@map post

                when {

                    // Upvote clicked again -> remove vote
                    voteType == "up" && currentVote == "up" ->
                        post.copy(upvotes = (post.upvotes - 1).coerceAtLeast(0))

                    // Downvote clicked again -> remove vote
                    voteType == "down" && currentVote == "down" ->
                        post.copy(downvotes = (post.downvotes - 1).coerceAtLeast(0))

                    // Switch down -> up
                    voteType == "up" && currentVote == "down" ->
                        post.copy(
                            upvotes = post.upvotes + 1,
                            downvotes = (post.downvotes - 1).coerceAtLeast(0)
                        )

                    // Switch up -> down
                    voteType == "down" && currentVote == "up" ->
                        post.copy(
                            upvotes = (post.upvotes - 1).coerceAtLeast(0),
                            downvotes = post.downvotes + 1
                        )

                    // Fresh upvote
                    voteType == "up" ->
                        post.copy(upvotes = post.upvotes + 1)

                    // Fresh downvote
                    voteType == "down" ->
                        post.copy(downvotes = post.downvotes + 1)

                    else -> post
                }
            },

            userVotes = _uiState.value.userVotes.toMutableMap().apply {

                when {
                    voteType == "up" && currentVote == "up" -> put(postId, null)
                    voteType == "down" && currentVote == "down" -> put(postId, null)
                    else -> put(postId, voteType)
                }
            }
        )

        // Save to Supabase in background
        viewModelScope.launch {
            postRepository.votePost(userId, postId, voteType).onFailure { error ->
                Log.e("HomeVotes", "Saving vote failed for post $postId", error)
                // Refresh authoritative state after a failed save; do not assume it toggled off.
                loadUserVotes()
                loadPosts()
            }
        }
    }

    fun setSortBy(sort: String) {
        val normalized = sort.trim().lowercase()
        if (normalized == _uiState.value.sortBy) return
        _uiState.update { it.copy(sortBy = normalized) }
        loadPosts()
    }

    fun setCategory(cat: String?) {
        if (cat == _uiState.value.category) return
        _uiState.update { it.copy(category = cat) }
        loadPosts()
    }

    fun getCurrentUserId(): String? {
        return authRepository.currentUserId
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
            loadPosts(refresh = true)
        }
    }


    fun reportPost(
        postId: String,
        reason: String
    ) {
        val userId = authRepository.currentUserId ?: return

        viewModelScope.launch {

            postRepository.reportPost(
                postId = postId,
                reportedBy = userId,
                reason = reason
            )
                .onSuccess {
                    _uiState.update { it.copy(snackbarMessage = "Report submitted successfully.") }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(snackbarMessage = error.message ?: "Failed to submit report.")
                    }
                }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}

// Index of the sticky Top/New/Hot header inside the feed LazyColumn:
// 0 location card · 1 greeting · 2 composer · 3 "right now" strip · 4 category chips · 5 sort header
private const val STICKY_HEADER_INDEX = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onProfileClick: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val compactWidth = LocalConfiguration.current.screenWidthDp < 360

    // ── Location: explain first, then ask ────────────────────────────────────
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(hasLocationPermission()) }
    var showLocationCard by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        showLocationCard = false
        val granted = results.values.any { it }
        locationGranted = granted
        if (granted) viewModel.refreshLocation()
    }

    LaunchedEffect(Unit) {
        if (!locationGranted && !HomeStreak.isLocationPromptSnoozed(context)) {
            delay(1_500)
            showLocationCard = true
        }
    }

    // ── Gentle daily streak (device-only) ────────────────────────────────────
    var streak by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { streak = HomeStreak.touch(context) }

    // ── Header entrance ──────────────────────────────────────────────────────
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); headerVisible = true }

    var dragPreviewTab by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbarMessage()
        }
    }

    // ── Badge animations ─────────────────────────────────────────────────────
    var previousUnreadNotifCount by remember { mutableIntStateOf(uiState.unreadNotifCount) }
    var previousUnreadMsgCount by remember { mutableIntStateOf(uiState.unreadMsgCount) }
    var notificationCountInitialized by remember { mutableStateOf(false) }
    var messageCountInitialized by remember { mutableStateOf(false) }
    val bellRotation = remember { Animatable(0f) }
    val messageScale = remember { Animatable(1f) }

    LaunchedEffect(uiState.unreadNotifCount, uiState.hasLoadedUnreadNotifications) {
        val current = uiState.unreadNotifCount
        if (uiState.hasLoadedUnreadNotifications && notificationCountInitialized && current > previousUnreadNotifCount) {
            haptic.alert()
            bellRotation.snapTo(0f)
            bellRotation.animateTo(0f, animationSpec = keyframes {
                durationMillis = 900
                16f at 100
                -16f at 200
                12f at 300
                -12f at 400
                7f at 500
                -5f at 600
                0f at 700
            })
        }
        if (uiState.hasLoadedUnreadNotifications) {
            previousUnreadNotifCount = current
            notificationCountInitialized = true
        }
    }

    // Messages get a friendly "pop" instead of a shake.
    LaunchedEffect(uiState.unreadMsgCount, uiState.hasLoadedUnreadMessages) {
        val current = uiState.unreadMsgCount
        if (uiState.hasLoadedUnreadMessages && messageCountInitialized && current > previousUnreadMsgCount) {
            haptic.tap()
            messageScale.snapTo(1f)
            messageScale.animateTo(1f, animationSpec = keyframes {
                durationMillis = 520
                1.3f at 130
                0.9f at 270
                1.1f at 400
                1f at 520
            })
        }
        if (uiState.hasLoadedUnreadMessages) {
            previousUnreadMsgCount = current
            messageCountInitialized = true
        }
    }

    // ── Scroll behaviour ─────────────────────────────────────────────────────
    // When Top/New/Hot or a category changes while the user is deep in the feed,
    // bring the first post of the new feed back into view.
    var firstFeedKey by remember { mutableStateOf(true) }
    LaunchedEffect(uiState.sortBy, uiState.category) {
        if (firstFeedKey) {
            firstFeedKey = false
        } else if (listState.firstVisibleItemIndex > STICKY_HEADER_INDEX) {
            listState.animateScrollToItem(STICKY_HEADER_INDEX)
        }
    }

    // Infinite scroll: ask for the next page when the user is within 4 items of the end.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(nearEnd, uiState.posts.size) {
        if (nearEnd) viewModel.loadMore()
    }

    // ── Derived feed data ────────────────────────────────────────────────────
    val alertPost = remember(uiState.posts) { uiState.posts.firstOrNull { it.isAlert } }
    val trendingPost = remember(uiState.posts, alertPost) {
        if (uiState.posts.size < 3) null
        else uiState.posts
            .maxByOrNull { it.upvotes + it.commentCount * 2 }
            ?.takeIf { it.upvotes + it.commentCount * 2 >= 10 && it.id != alertPost?.id }
    }

    val itemSpacing = DensityManager.itemSpacing.dp
    val feedAlpha by animateFloatAsState(
        targetValue = if (uiState.isSwitching) 0.5f else 1f,
        label = "feed_alpha"
    )
    val currentUserId = viewModel.getCurrentUserId()
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)

    Scaffold(
        modifier = Modifier.pointerInput(uiState.sortBy) {
            val tabs = listOf("top", "new", "hot")
            var horizontalDistance = 0f
            var sortAtDragStart = uiState.sortBy.trim().lowercase()
            fun targetForSwipe(distance: Float): String? {
                val index = tabs.indexOf(sortAtDragStart)
                if (index < 0 || distance == 0f) return null
                // Finger right advances Top -> New -> Hot; finger left reverses.
                val nextIndex = (index + if (distance > 0f) 1 else -1)
                    .coerceIn(0, tabs.lastIndex)
                return tabs[nextIndex].takeIf { it != sortAtDragStart }
            }
            detectHorizontalDragGestures(
                onDragStart = {
                    horizontalDistance = 0f
                    sortAtDragStart = uiState.sortBy.trim().lowercase()
                    dragPreviewTab = null
                },
                onHorizontalDrag = { _, dragAmount ->
                    horizontalDistance += dragAmount
                    dragPreviewTab = if (abs(horizontalDistance) > 12f) {
                        targetForSwipe(horizontalDistance)
                    } else null
                },
                onDragEnd = {
                    if (abs(horizontalDistance) > 80f) {
                        targetForSwipe(horizontalDistance)?.let { target ->
                            haptic.tap()
                            viewModel.setSortBy(target)
                        }
                    }
                    horizontalDistance = 0f
                    dragPreviewTab = null
                },
                onDragCancel = {
                    horizontalDistance = 0f
                    dragPreviewTab = null
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,

        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },

        // Slim, fixed top bar: brand + three actions. Greeting, weather and filters now
        // live inside the feed so they scroll away and posts get the full screen.
        topBar = {
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(tween(380)) + slideInVertically { -28 }
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.background
                                ),
                                startY = 0f, endY = 220f
                            )
                        )
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PulseBrand()

                        Spacer(Modifier.weight(1f))

                        val buttonSize = if (compactWidth) 40.dp else 44.dp
                        val buttonGap = if (compactWidth) 6.dp else 8.dp

                        HeaderIconButton(
                            icon = Icons.Filled.Search,
                            contentDescription = "Search",
                            size = buttonSize,
                            onClick = { navController.navigate(Screen.Explore.route) }
                        )

                        Spacer(Modifier.width(buttonGap))

                        HeaderIconButton(
                            icon = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            size = buttonSize,
                            badgeCount = uiState.unreadNotifCount,
                            iconModifier = Modifier.rotate(bellRotation.value),
                            onClick = onNotifications
                        )

                        Spacer(Modifier.width(buttonGap))

                        HeaderIconButton(
                            icon = Icons.Filled.Forum,
                            contentDescription = "Messages",
                            size = buttonSize,
                            badgeCount = uiState.unreadMsgCount,
                            iconModifier = Modifier.scale(messageScale.value),
                            onClick = { navController.navigate(Screen.Messages.route) }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onCreatePost = onCreatePost,
                onProfileClick = onProfileClick,
                hasAlertBadge = false,
                messageCount = uiState.unreadMsgCount
            )
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.loadPosts(refresh = true) },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        contentColor = OrangePrimary,
                        backgroundColor = MaterialTheme.colorScheme.surface
                    )
                }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                ) {
                    // 0 ── Location explainer (only while permission is missing)
                    item(key = "location_card") {
                        if (showLocationCard && !locationGranted) {
                            LocationRationaleCard(
                                onAllow = {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                onDismiss = {
                                    showLocationCard = false
                                    HomeStreak.snoozeLocationPrompt(context)
                                },
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 12.dp)
                            )
                        }
                    }

                    // 1 ── Greeting + location / weather / AQI / streak / level
                    item(key = "greeting") {
                        GreetingBlock(
                            userName = uiState.userName,
                            area = uiState.currentArea,
                            temperature = uiState.temperature,
                            pm25 = uiState.aqi,
                            weatherAttempted = uiState.weatherAttempted,
                            streak = streak,
                            karma = uiState.userKarma,
                            onLocationClick = { viewModel.refreshLocation() },
                            onLevelClick = onProfileClick,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp, bottom = 12.dp)
                        )
                    }

                    // 2 ── "What's happening in <area>?" composer
                    item(key = "composer") {
                        ComposerBar(
                            userName = uiState.userName,
                            avatarUrl = uiState.userAvatarUrl,
                            area = uiState.currentArea,
                            onClick = {
                                haptic.tap()
                                onCreatePost()
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        )
                    }

                    // 3 ── Live local signals (renders nothing when there is nothing real to show)
                    item(key = "right_now") {
                        RightNowStrip(
                            onlineCount = uiState.onlineCount,
                            alertPost = alertPost,
                            trendingPost = trendingPost,
                            onPostClick = onPostClick,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 14.dp)
                        )
                    }

                    // 4 ── Category filter chips
                    item(key = "categories") {
                        CategoryChipsRow(
                            selected = uiState.category,
                            onSelected = { viewModel.setCategory(it) },
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .padding(bottom = 10.dp)
                        )
                    }

                    // 5 ── Sticky Top / New / Hot
                    stickyHeader(key = "sort_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp)
                                .padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            SortTabs(
                                selected = uiState.sortBy,
                                onSelected = { viewModel.setSortBy(it) }
                            )
                            // Reserve the 2dp so the layout never jumps when the bar appears.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .height(2.dp)
                            ) {
                                if (uiState.isSwitching) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = OrangePrimary,
                                        trackColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                }
                            }
                        }
                    }

                    // ── Feed ────────────────────────────────────────────────
                    when {
                        uiState.isLoading && uiState.posts.isEmpty() -> {
                            items(5, key = { "shimmer_$it" }) {
                                ShimmerPostCard(
                                    Modifier
                                        .padding(horizontal = 12.dp)
                                        .padding(bottom = itemSpacing)
                                )
                            }
                        }

                        uiState.posts.isEmpty() && uiState.error != null -> {
                            item(key = "feed_error") {
                                FeedErrorState(onRetry = { viewModel.loadPosts() })
                            }
                        }

                        uiState.posts.isEmpty() -> {
                            item(key = "feed_empty") {
                                FeedEmptyState(
                                    category = uiState.category,
                                    onCreatePost = {
                                        haptic.tap()
                                        onCreatePost()
                                    }
                                )
                            }
                        }

                        else -> {
                            itemsIndexed(
                                items = uiState.posts,
                                key = { _, post -> post.id }
                            ) { index, post ->
                                StaggeredItem(index = index) {
                                    PostCard(
                                        post = post,
                                        modifier = Modifier
                                            .animateItem()
                                            .alpha(feedAlpha)
                                            .padding(start = 12.dp, end = 12.dp, bottom = itemSpacing),
                                        currentVote = uiState.userVotes[post.id],
                                        isSaved = post.id in uiState.savedPostIds,
                                        isOwnPost = post.userId == currentUserId,
                                        isAdmin = viewModel.isAdmin,
                                        onClick = { onPostClick(post.id) },
                                        onUserClick = { userId ->
                                            navController.navigate("user_profile/$userId")
                                        },
                                        onUpvote = { viewModel.vote(post.id, "up") },
                                        onDownvote = { viewModel.vote(post.id, "down") },
                                        onToggleSave = { viewModel.toggleSave(post.id) },
                                        onReport = {
                                            viewModel.reportPost(post.id, "other")
                                        },
                                        onEdit = {
                                            navController.navigate(
                                                Screen.CreatePost.createEditRoute(post.id)
                                            )
                                        },
                                        onDelete = {
                                            viewModel.deletePost(post.id)
                                        }
                                    )
                                }
                            }

                            if (uiState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = OrangePrimary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            if (uiState.endReached && !uiState.isLoadingMore) {
                                item(key = "end_of_feed") {
                                    FeedEndCard(
                                        onExplore = { navController.navigate(Screen.Explore.route) }
                                    )
                                }
                            }
                        }
                    }

                    item(key = "bottom_space") { Spacer(Modifier.height(8.dp)) }
                }
            }

            // ── "N new posts" pill ──────────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.newPostsCount > 0,
                enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { -it },
                exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                NewPostsPill(
                    count = uiState.newPostsCount,
                    onClick = {
                        haptic.tap()
                        viewModel.showLatest()
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                )
            }

            // ── Swipe-to-switch hint ────────────────────────────────────────
            AnimatedVisibility(
                visible = dragPreviewTab != null,
                enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.88f, animationSpec = tween(180)),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.92f, animationSpec = tween(120)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                        .border(1.dp, OrangePrimary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Release to ${dragPreviewTab?.replaceFirstChar { it.uppercase() } ?: ""}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}
