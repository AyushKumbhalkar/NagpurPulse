// this is the ProfileScreen.kt file

// java/com/nagpurpulse/ui/screens/profile/ProfileScreen.kt
//
// The signed-in user's own account screen.
// Pure logic (levels, streaks, milestones...) lives in ProfileGamification.kt and the
// reusable pieces live in ProfileComponents.kt.

package com.nagpurpulse.ui.screens.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.model.*
import com.nagpurpulse.data.repository.*
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

// ── UI state ──────────────────────────────────────────────────────────────────

data class ProfileUiState(
    val profile: Profile? = null,
    val posts: List<Post> = emptyList(),
    val comments: List<Comment> = emptyList(),
    /** postId -> thread title, so "my comments" can show which thread they belong to. */
    val postTitles: Map<String, String> = emptyMap(),
    val savedPosts: List<Post> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val rank: KarmaRank? = null,
    val postsLoaded: Boolean = false,
    val commentsLoaded: Boolean = false,
    val savedCount: Int = 0,
    val unreadNotifCount: Int = 0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val activeTab: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val savedPostsRepository: SavedPostsRepository,
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private var lastFullRefreshAt = 0L

    val guestAvatarUrl: String?
        get() = authRepository.guestAvatarUrl

    val guestUsername: String?
        get() = authRepository.guestUsername

    init { refresh(showLoading = true) }

    /** Cheap profile-only refresh (karma, avatar, tagline). */
    fun refreshProfile() {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            profileRepository.getProfile(userId).onSuccess { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
        }
    }

    /**
     * Called every time the screen resumes (e.g. after posting a thread and coming back).
     * Always refreshes the profile; refreshes everything silently at most every 20 seconds
     * so streaks, karma and the impact card never look stale.
     */
    fun onResume() {
        refreshProfile()
        val now = SystemClock.elapsedRealtime()
        if (now - lastFullRefreshAt >= 20_000L) refresh(showLoading = false, silent = true)
    }

    fun refresh(showLoading: Boolean = false, silent: Boolean = false) {
        val userId = authRepository.currentUserId ?: return
        lastFullRefreshAt = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = showLoading && it.profile == null,
                    isRefreshing = !showLoading && !silent
                )
            }
            try {
                val profileJob = async { profileRepository.getProfile(userId) }
                val postsJob = async { postRepository.getPostsByUser(userId) }
                val commentsJob = async { postRepository.getCommentsByUser(userId) }
                val badgesJob = async { profileRepository.getBadges(userId) }
                val notifJob = async { notificationRepository.getUnreadCount(userId) }
                val savedIdsJob = async { savedPostsRepository.getSavedPostIds(userId) }
                val rankJob = async { profileRepository.getKarmaRank() }

                profileJob.await().onSuccess { p -> _uiState.update { it.copy(profile = p) } }
                postsJob.await().onSuccess { p -> _uiState.update { it.copy(posts = p, postsLoaded = true) } }
                badgesJob.await().onSuccess { b -> _uiState.update { it.copy(badges = b) } }
                notifJob.await().onSuccess { c -> _uiState.update { it.copy(unreadNotifCount = c) } }
                savedIdsJob.await().onSuccess { ids -> _uiState.update { it.copy(savedCount = ids.size) } }
                rankJob.await()?.let { r -> _uiState.update { it.copy(rank = r) } }
                commentsJob.await().onSuccess { c -> applyComments(c) }
                if (_uiState.value.activeTab == 2) loadSavedPosts(userId)
            } finally {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    /** Stores the comments, then fills in the thread titles they belong to. */
    private suspend fun applyComments(all: List<Comment>) {
        val comments = all.filter { !it.isDeleted }
        _uiState.update { it.copy(comments = comments, commentsLoaded = true) }
        val missing = comments.map { it.postId }.distinct().filter { it !in _uiState.value.postTitles }
        if (missing.isNotEmpty()) {
            postRepository.getPostTitles(missing).onSuccess { titles ->
                _uiState.update { it.copy(postTitles = it.postTitles + titles) }
            }
        }
    }

    private suspend fun loadComments(userId: String) {
        postRepository.getCommentsByUser(userId).onSuccess { applyComments(it) }
    }

    private suspend fun loadSavedPosts(userId: String) {
        savedPostsRepository.getSavedPostIds(userId).onSuccess { ids ->
            postRepository.getSavedPosts(ids).onSuccess { posts ->
                _uiState.update { it.copy(savedPosts = posts, savedCount = posts.size) }
            }
        }
    }

    fun setActiveTab(tab: Int) {
        if (_uiState.value.activeTab == tab) return
        _uiState.update { it.copy(activeTab = tab) }
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            when (tab) {
                1 -> if (!_uiState.value.commentsLoaded) loadComments(userId)
                2 -> loadSavedPosts(userId)
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch { authRepository.signOut(); onSuccess() }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId).fold(
                onSuccess = {
                    _uiState.update { s -> s.copy(posts = s.posts.filter { it.id != postId }) }
                },
                onFailure = {}
            )
        }
    }

    fun toggleSave(postId: String) {
        val userId = authRepository.currentUserId ?: return
        viewModelScope.launch {
            savedPostsRepository.unsavePost(userId, postId)
            _uiState.update { s ->
                s.copy(
                    savedPosts = s.savedPosts.filter { it.id != postId },
                    savedCount = (s.savedCount - 1).coerceAtLeast(0)
                )
            }
        }
    }
}

// Entrance animations play once per app session; coming back to the tab is instant.
private var profileIntroPlayed = false

private const val PREFS_NAME = "profile_progress"

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
@Suppress("UNUSED_PARAMETER")
fun ProfileScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onNotifications: () -> Unit,
    // Sign-out lives in Settings. Kept so existing call sites keep compiling.
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val profile = uiState.profile
    val userId = profile?.id?.takeIf { it.isNotBlank() }

    // ── Derived data ─────────────────────────────────────────────────────────
    val karma = profile?.karma ?: 0
    val levelProgress = remember(karma) { ProfileLevels.progress(karma) }
    val activity = remember(uiState.posts, uiState.comments) {
        activityByDay(uiState.posts.map { it.createdAt } + uiState.comments.map { it.createdAt })
    }
    val streak = remember(activity) { computeStreak(activity.keys, LocalDate.now()) }
    val heatmap = remember(activity) { buildHeatmap(activity, LocalDate.now()) }
    val impact = remember(uiState.posts) {
        computeImpact(uiState.posts.map { PostStat(it.createdAt, it.upvotes, it.commentCount, it.viewCount) })
    }
    val completeness = remember(profile, uiState.posts.size) {
        computeCompleteness(
            hasAvatar = !profile?.avatarUrl.isNullOrBlank(),
            hasTagline = !profile?.tagline.isNullOrBlank(),
            hasLocation = !profile?.location.isNullOrBlank(),
            hasAreas = !profile?.areas.isNullOrEmpty(),
            hasThread = uiState.posts.isNotEmpty()
        )
    }
    val milestones = remember(uiState.posts.size, uiState.comments.size, impact.totalUpvotes, karma, streak.longest, completeness.isComplete) {
        sortMilestones(
            buildMilestones(
                MilestoneInput(
                    threads = uiState.posts.size,
                    comments = uiState.comments.size,
                    upvotesReceived = impact.totalUpvotes,
                    karma = karma,
                    longestStreak = streak.longest,
                    profileComplete = completeness.isComplete
                )
            )
        )
    }
    val badgeTiles = remember(milestones, uiState.badges) {
        val serverTiles = uiState.badges.map {
            BadgeTileUi(
                id = "server_${it.id}",
                emoji = it.emoji(),
                title = it.displayName(),
                earned = true,
                fraction = 1f,
                caption = "Awarded",
                hint = "Awarded by Nagpur Pulse"
            )
        }
        val milestoneTiles = milestones.map {
            BadgeTileUi(
                id = it.id,
                emoji = it.emoji,
                title = it.title,
                earned = it.earned,
                fraction = it.fraction,
                caption = if (it.earned) "Unlocked" else "${it.current.coerceAtMost(it.target)}/${it.target}",
                hint = it.hint
            )
        }
        serverTiles + milestoneTiles
    }
    val bestPost = remember(uiState.posts) { uiState.posts.filter { it.upvotes > 0 }.maxByOrNull { it.upvotes } }
    val nextAction = remember(uiState.posts.size, uiState.comments.size, streak) {
        pickNextAction(uiState.posts.size, uiState.comments.size, streak)
    }
    val rank = uiState.rank?.takeIf { it.totalUsers >= 10 }

    // ── "Since your last visit" + celebrations (stored on device) ────────────
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val baselineUpvotes = remember(userId) { userId?.let { prefs.getInt("up_$it", -1) } ?: -1 }
    val baselineReplies = remember(userId) { userId?.let { prefs.getInt("re_$it", -1) } ?: -1 }
    val deltaUpvotes =
        if (uiState.postsLoaded && baselineUpvotes >= 0) (impact.totalUpvotes - baselineUpvotes).coerceAtLeast(0) else 0
    val deltaReplies =
        if (uiState.postsLoaded && baselineReplies >= 0) (impact.totalReplies - baselineReplies).coerceAtLeast(0) else 0

    val latestSnapshot by rememberUpdatedState(Triple(userId, impact, uiState.postsLoaded))
    DisposableEffect(lifecycleOwner) {
        fun save() {
            val (id, stats, loaded) = latestSnapshot
            if (id != null && loaded) {
                prefs.edit().putInt("up_$id", stats.totalUpvotes).putInt("re_$id", stats.totalReplies).apply()
            }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) save() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { save(); lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var celebration by remember { mutableStateOf<CelebrationMessage?>(null) }
    val dataReady = uiState.postsLoaded && uiState.commentsLoaded
    val earnedCount = milestones.count { it.earned }
    LaunchedEffect(userId, dataReady, levelProgress.level.number, earnedCount) {
        if (userId == null || !dataReady) return@LaunchedEffect
        val levelKey = "level_$userId"
        val earnedKey = "earned_$userId"
        val storedLevel = prefs.getInt(levelKey, 0)
        val storedEarned = prefs.getStringSet(earnedKey, null)
        val earnedNow = milestones.filter { it.earned }.map { it.id }.toSet()
        val level = levelProgress.level

        var message: CelebrationMessage? = null
        if (storedLevel in 1 until level.number) {
            message = CelebrationMessage(level.emoji, "Level up! You're now a ${level.title}", "${level.unlock} unlocked")
        } else if (storedEarned != null) {
            val fresh = milestones.firstOrNull { it.earned && it.id !in storedEarned }
            if (fresh != null) {
                message = CelebrationMessage(fresh.emoji, "Badge unlocked: ${fresh.title}", "Nice work, keep going!")
            }
        }
        prefs.edit()
            .putInt(levelKey, level.number)
            .putStringSet(earnedKey, (storedEarned.orEmpty() + earnedNow).toSet())
            .apply()
        if (message != null) {
            haptic.success()
            celebration = message
        }
    }

    // Plays the entrance animation only the first time in this app session.
    val animateIntro = remember { !profileIntroPlayed }
    LaunchedEffect(uiState.isLoading, profile) {
        if (!uiState.isLoading && profile != null) {
            delay(1200)
            profileIntroPlayed = true
        }
    }

    // ── Dialogs / sheets ─────────────────────────────────────────────────────
    var showLevels by remember { mutableStateOf(false) }
    var showStreak by remember { mutableStateOf(false) }
    var showBadges by remember { mutableStateOf(false) }
    if (showLevels) LevelsDialog(karma) { showLevels = false }
    if (showStreak) StreakDialog(streak) { showStreak = false }
    if (showBadges) BadgesSheet(badgeTiles) { showBadges = false }

    // ── Header values ────────────────────────────────────────────────────────
    val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
        ?: profile?.username?.takeIf { it.isNotBlank() }
        ?: viewModel.guestUsername?.takeIf { it.isNotBlank() }
        ?: "NagpurUser"
    val avatarUrl = profile?.avatarUrl?.takeIf { it.isNotBlank() }
        ?: viewModel.guestAvatarUrl?.takeIf { it.isNotBlank() }

    val goHome: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val goEditProfile: () -> Unit = { navController.navigate(Screen.AccountProfile.route) }
    val shareProfile: () -> Unit = {
        val level = levelProgress.level
        val text = buildString {
            append("I'm a ${level.emoji} ${level.title} on Nagpur Pulse with ${formatCount(karma)} karma")
            if (streak.days > 1) append(" and a ${streak.days}-day streak 🔥")
            append(". Come join the conversation about our city!")
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, "Share your profile")
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Sections shown between the header and the tabs. Order = order on screen.
    val sections = buildList {
        if (profile != null && !completeness.isComplete) add("complete")
        add("level")
        add("next")
        if (impact.hasAnything || deltaUpvotes > 0 || deltaReplies > 0 || rank != null) add("impact")
        add("badges")
        if (bestPost != null) add("best")
        if (activity.isNotEmpty()) add("heatmap")
    }
    val tabsIndex = 1 + sections.size

    fun selectTab(tab: Int, scrollToTabs: Boolean) {
        haptic.tap()
        viewModel.setActiveTab(tab)
        if (scrollToTabs) scope.launch { listState.animateScrollToItem(tabsIndex) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Profile",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.weight(1f))
                TopBarIconButton(
                    icon = Icons.Filled.Notifications,
                    description = "Notifications",
                    onClick = onNotifications,
                    badgeCount = uiState.unreadNotifCount
                )
                Spacer(Modifier.width(4.dp))
                TopBarIconButton(
                    icon = Icons.Filled.Settings,
                    description = "Settings",
                    onClick = { navController.navigate(Screen.Settings.route) }
                )
            }
        },
        bottomBar = { BottomNavBar(navController = navController, onCreatePost = onCreatePost) }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                ProfileSkeleton()
            } else {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(uiState.isRefreshing),
                    onRefresh = { viewModel.refresh(showLoading = false) },
                    indicator = { state, trigger ->
                        com.google.accompanist.swiperefresh.SwipeRefreshIndicator(
                            state, trigger, contentColor = OrangePrimary
                        )
                    }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // ── Header (banner, avatar, identity, actions, stats) ─────────
                        item(key = "header") {
                            ProfileHeader(
                                profile = profile,
                                displayName = displayName,
                                avatarUrl = avatarUrl,
                                levelProgress = levelProgress,
                                streak = streak,
                                completeness = completeness,
                                threads = uiState.posts.size,
                                comments = uiState.comments.size,
                                animateIn = animateIntro,
                                onEdit = goEditProfile,
                                onShare = shareProfile,
                                onKarma = { showLevels = true },
                                onThreads = { selectTab(0, true) },
                                onComments = { selectTab(1, true) },
                                onStreak = { showStreak = true }
                            )
                        }

                        // ── Sections ───────────────────────────────────────────────
                        items(sections, key = { "section_$it" }) { key ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 12.dp)
                            ) {
                                when (key) {
                                    "complete" -> CompleteProfileCard(completeness, goEditProfile)
                                    "level" -> LevelCard(levelProgress, karma) { showLevels = true }
                                    "next" -> NextActionCard(
                                        action = nextAction,
                                        firstArea = profile?.areas?.firstOrNull(),
                                        onClick = {
                                            when (nextAction) {
                                                NextAction.FIRST_THREAD, NextAction.SAVE_STREAK -> onCreatePost()
                                                else -> goHome()
                                            }
                                        }
                                    )
                                    "impact" -> ImpactCard(impact, deltaUpvotes, deltaReplies, rank)
                                    "badges" -> BadgesSection(badgeTiles) { showBadges = true }
                                    "best" -> bestPost?.let {
                                        BestThreadCard(
                                            title = it.title,
                                            upvotes = it.upvotes,
                                            replies = it.commentCount,
                                            views = it.viewCount,
                                            onClick = { onPostClick(it.id) }
                                        )
                                    }
                                    "heatmap" -> ActivityHeatmapCard(heatmap)
                                }
                            }
                        }

                        // ── Sticky tabs ────────────────────────────────────────────
                        stickyHeader(key = "tabs") {
                            Column(
                                Modifier
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(top = 12.dp)
                            ) {
                                ProfileTabs(
                                    activeTab = uiState.activeTab,
                                    threads = uiState.posts.size,
                                    comments = if (uiState.commentsLoaded) uiState.comments.size else null,
                                    saved = uiState.savedCount,
                                    onSelect = { selectTab(it, false) }
                                )
                            }
                        }

                        // ── Tab content ────────────────────────────────────────────
                        when (uiState.activeTab) {
                            0 -> {
                                if (uiState.posts.isEmpty()) {
                                    item(key = "empty_threads") {
                                        ProfileEmptyState(
                                            emoji = "✍️",
                                            title = "Start your Nagpur story",
                                            subtitle = "Share what's happening around you and start earning karma.",
                                            buttonLabel = "Create your first thread",
                                            onClick = onCreatePost
                                        )
                                    }
                                } else {
                                    itemsIndexed(uiState.posts, key = { _, p -> "post_${p.id}" }) { i, item ->
                                        StaggeredItem(i) {
                                            PostCard(
                                                post = item,
                                                onClick = { onPostClick(item.id) },
                                                isOwnPost = true,
                                                onEdit = {
                                                    navController.navigate(Screen.CreatePost.createEditRoute(item.id))
                                                },
                                                onDelete = { viewModel.deletePost(item.id) },
                                                modifier = Modifier
                                                    .padding(horizontal = 14.dp)
                                                    .padding(top = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                if (!uiState.commentsLoaded) {
                                    item(key = "comments_loading") {
                                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                                            CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                } else if (uiState.comments.isEmpty()) {
                                    item(key = "empty_comments") {
                                        ProfileEmptyState(
                                            emoji = "💬",
                                            title = "Join the conversation",
                                            subtitle = "Reply to a thread and your comments will show up here.",
                                            buttonLabel = "Browse threads",
                                            onClick = goHome
                                        )
                                    }
                                } else {
                                    items(uiState.comments, key = { "comment_${it.id}" }) { c ->
                                        MyCommentCard(
                                            comment = c,
                                            threadTitle = uiState.postTitles[c.postId],
                                            onClick = { onPostClick(c.postId) },
                                            modifier = Modifier
                                                .padding(horizontal = 14.dp)
                                                .padding(top = 10.dp)
                                        )
                                    }
                                }
                            }

                            else -> {
                                if (uiState.savedPosts.isEmpty()) {
                                    item(key = "empty_saved") {
                                        ProfileEmptyState(
                                            emoji = "🔖",
                                            title = "Nothing saved yet",
                                            subtitle = "Tap the bookmark on any thread to keep it here.",
                                            buttonLabel = "Find threads to save",
                                            onClick = goHome
                                        )
                                    }
                                } else {
                                    itemsIndexed(uiState.savedPosts, key = { _, p -> "saved_${p.id}" }) { i, item ->
                                        StaggeredItem(i) {
                                            PostCard(
                                                post = item,
                                                onClick = { onPostClick(item.id) },
                                                isSaved = true,
                                                onToggleSave = { viewModel.toggleSave(item.id) },
                                                modifier = Modifier
                                                    .padding(horizontal = 14.dp)
                                                    .padding(top = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            CelebrationOverlay(celebration) { celebration = null }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(
    profile: Profile?,
    displayName: String,
    avatarUrl: String?,
    levelProgress: LevelProgress,
    streak: StreakInfo,
    completeness: Completeness,
    threads: Int,
    comments: Int,
    animateIn: Boolean,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onKarma: () -> Unit,
    onThreads: () -> Unit,
    onComments: () -> Unit,
    onStreak: () -> Unit
) {
    val level = levelProgress.level
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val nameSize = when {
        displayName.length > 20 -> 20.sp
        screenWidth < 360 -> 22.sp
        else -> 26.sp
    }

    var shown by remember { mutableStateOf(!animateIn) }
    LaunchedEffect(Unit) { if (animateIn) { delay(60); shown = true } }
    val avatarAlpha by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "avatar_alpha")
    val avatarScale by animateFloatAsState(
        if (shown) 1f else 0.7f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "avatar_scale"
    )
    val textAlpha by animateFloatAsState(
        if (shown) 1f else 0f, tween(450, delayMillis = 120, easing = FastOutSlowInEasing), label = "text_alpha"
    )

    val joined = remember(profile?.createdAt) { joinedLabel(profile?.createdAt) }
    val location = profile?.location?.takeIf { it.isNotBlank() }

    Box(Modifier.fillMaxWidth()) {
        ProfileBanner(
            coverUrl = profile?.coverUrl,
            level = level,
            modifier = Modifier
                .fillMaxWidth()
                .height(ProfileDims.bannerHeight)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = ProfileDims.bannerHeight - ProfileDims.avatarBox / 2)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatar(
                avatarUrl = avatarUrl,
                initial = displayName.firstOrNull()?.uppercase() ?: "N",
                level = level,
                completeness = completeness,
                onEdit = onEdit,
                modifier = Modifier.graphicsLayer {
                    alpha = avatarAlpha
                    scaleX = avatarScale
                    scaleY = avatarScale
                }
            )

            Column(
                Modifier.graphicsLayer {
                    alpha = textAlpha
                    translationY = (1f - textAlpha) * 40f
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(
                        displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = nameSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile?.isVerified == true) {
                        Spacer(Modifier.width(6.dp))
                        VerifiedTick()
                    }
                }
                if (!profile?.username.isNullOrBlank()) {
                    Text(
                        "u/${profile?.username}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!profile?.tagline.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        profile?.tagline.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(ProfileDims.spaceM))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfilePill(
                        text = "Lv ${level.number} · ${level.title}",
                        leadingEmoji = level.emoji,
                        tint = level.colorStart.asColor()
                    )
                    StreakChip(streak, onStreak)
                }

                val areas = profile?.areas.orEmpty()
                if (areas.isNotEmpty()) {
                    Spacer(Modifier.height(ProfileDims.spaceM))
                    AreaChipFlow(areas)
                }

                if (location != null || joined != null) {
                    Spacer(Modifier.height(ProfileDims.spaceM))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (location != null) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                location,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (location != null && joined != null) {
                            Text(
                                "  •  ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (joined != null) {
                            Text(
                                joined,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(Modifier.height(ProfileDims.spaceL))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Edit profile", Icons.Filled.Edit, onEdit, Modifier.weight(1f), primary = true)
                    PillButton("Share", Icons.Filled.Share, onShare, Modifier.weight(1f))
                }

                Spacer(Modifier.height(ProfileDims.spaceM))
                StatsCard(
                    karma = profile?.karma ?: 0,
                    threads = threads,
                    comments = comments,
                    animate = animateIn,
                    onKarma = onKarma,
                    onThreads = onThreads,
                    onComments = onComments
                )
            }
        }
    }
}

private fun joinedLabel(createdAt: String?): String? {
    val date = parseInstant(createdAt)?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: return null
    val month = date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
    return "Joined $month ${date.year}"
}

// ── Tabs ──────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTabs(
    activeTab: Int,
    threads: Int,
    comments: Int?,
    saved: Int,
    onSelect: (Int) -> Unit
) {
    val labels = listOf("Threads", "Comments", "Saved")
    val counts = listOf<Int?>(threads, comments, saved)
    TabRow(
        selectedTabIndex = activeTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = OrangePrimary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                color = OrangePrimary
            )
        }
    ) {
        labels.forEachIndexed { i, label ->
            val selected = activeTab == i
            Tab(
                selected = selected,
                onClick = { onSelect(i) },
                modifier = Modifier.defaultMinSize(minHeight = ProfileDims.minTouch),
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val count = counts[i]
                        if (count != null && count > 0) {
                            Spacer(Modifier.width(6.dp))
                            CountPill(count, selected)
                        }
                    }
                }
            )
        }
    }
}

// ── My comments ───────────────────────────────────────────────────────────────

@Composable
private fun MyCommentCard(
    comment: Comment,
    threadTitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProfileCard(modifier = modifier, onClick = onClick) {
        Text(
            "💬 On: ${threadTitle?.takeIf { it.isNotBlank() } ?: "a thread"}",
            color = OrangePrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
            comment.body,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            buildString {
                append("▲ ${formatCount(comment.upvotes)}  ·  ${comment.timeAgo()}")
                if (comment.isAnonymous) append("  ·  🕶 Anonymous")
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
