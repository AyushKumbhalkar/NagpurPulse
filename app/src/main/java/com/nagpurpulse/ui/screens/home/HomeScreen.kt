//This is the HomeSCreen.kt file and below is the path of the file
// java/com/nagpurpulse/ui/screens/home/HomeScreen.kt

package com.nagpurpulse.ui.screens.home



import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
import android.util.Log
import com.nagpurpulse.data.repository.AdminRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.ui.theme.ThemeManager
import androidx.compose.foundation.lazy.rememberLazyListState
import com.nagpurpulse.ui.preferences.DensityManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.nagpurpulse.data.location.LocationHelper
import com.nagpurpulse.data.remote.weather.WeatherRepository
import java.util.Calendar
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.MessageRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.data.repository.SavedPostsRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel (unchanged logic) ───────────────────────────────────────────────
data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val userVotes: Map<String, String?> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val sortBy: String = "top",
    val category: String? = null,
    val unreadNotifCount: Int = 0,
    val hasLoadedUnreadNotifications: Boolean = false,
    val unreadMsgCount: Int = 0,
    val hasLoadedUnreadMessages: Boolean = false,
    val temperature: Int? = null,
    val aqi: Int? = null,
    val currentArea: String = "Near You",
    val snackbarMessage: String? = null
)



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

    init {
        loadPosts()
        loadWeather()
        loadLocation()
        checkAdminStatus()
        loadUnreadMessages()
        subscribeToUnreadMessages()
        loadUnreadNotifications()
        subscribeToUnreadNotifications()
        startPresenceWhenAuthenticated()
    }

    private fun loadUnreadMessages() {
        viewModelScope.launch {
            messageRepository.getConversations().onSuccess { conversations ->
                _uiState.value = _uiState.value.copy(
                    unreadMsgCount = conversations.sumOf { it.myUnreadCount },
                    hasLoadedUnreadMessages = true
                )
            }
        }
    }

    private fun subscribeToUnreadMessages() {
        viewModelScope.launch {
            messageRepository.subscribeToConversations().collect {
                loadUnreadMessages()
            }
        }
        // Realtime is the fast path; periodic refresh recovers missed events and
        // keeps the Home badge consistent across devices.
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                loadUnreadMessages()
            }
        }
    }

    private fun loadUnreadNotifications() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            notificationRepository.getUnreadCount(userId).onSuccess { count ->
                _uiState.value = _uiState.value.copy(unreadNotifCount = count, hasLoadedUnreadNotifications = true)
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
                delay(15_000)
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
            android.util.Log.d("ADMIN_CHECK", "Home admin status = $isAdmin")
        }
    }


    private fun loadWeather() {

        viewModelScope.launch {

            try {

                val coords =
                    locationHelper.getCoordinates()

                if (coords == null) return@launch

                val result =
                    weatherRepository.getWeatherAndAqi(
                        coords.first,
                        coords.second
                    )

                _uiState.value = _uiState.value.copy(
                    temperature = result.first,
                    aqi = result.second
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadLocation() {
        viewModelScope.launch {

            try {

                val area = locationHelper.getSubLocality()

                _uiState.value = _uiState.value.copy(
                    currentArea = area
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateTheme(
        context: Context,
        enabled: Boolean
    ) {

        android.util.Log.d(
            "HOME_THEME",
            "updateTheme() called. enabled = $enabled"
        )

        viewModelScope.launch {

            // Save immediately for next app launch
            userPreferencesRepository.saveThemeLocally(
                context,
                !enabled
            )

            // Save to Supabase
            userPreferencesRepository.saveAmoledMode(
                !enabled
            )

            ThemeManager.toggleTheme(enabled)
        }
    }


    // Only the most recently requested feed load may update the visible posts.
    // This prevents a slower response for an old tab from replacing a newer tab's feed.
    private var latestPostsLoadRequestId = 0L

    fun loadPosts(refresh: Boolean = false) {
        val requestId = ++latestPostsLoadRequestId
        val requestedSort = _uiState.value.sortBy
        val requestedCategory = _uiState.value.category

        viewModelScope.launch {
            if (requestId != latestPostsLoadRequestId) {
                Log.d("HomeSortTrace", "loadPosts STALE_BEFORE_START_IGNORED requestId=$requestId latest=$latestPostsLoadRequestId")
                return@launch
            }
            Log.d("HomeSortTrace", "loadPosts START requestId=$requestId refresh=$refresh sort=$requestedSort category=$requestedCategory")
            _uiState.value = _uiState.value.copy(isLoading = !refresh, isRefreshing = refresh)
            postRepository.getPosts(
                category = requestedCategory,
                sortBy = requestedSort
            ).fold(
                onSuccess = { posts ->
                    val currentState = _uiState.value
                    val isLatestRequest = requestId == latestPostsLoadRequestId
                    val matchesCurrentFeed = requestedSort == currentState.sortBy &&
                        requestedCategory == currentState.category

                    Log.d(
                        "HomeSortTrace",
                        "loadPosts SUCCESS requestId=$requestId latest=$isLatestRequest " +
                            "requestedSort=$requestedSort currentSort=${currentState.sortBy} " +
                            "returnedPosts=${posts.size}"
                    )

                    if (!isLatestRequest || !matchesCurrentFeed) {
                        Log.d(
                            "HomeSortTrace",
                            "loadPosts STALE_IGNORED requestId=$requestId " +
                                "requestedSort=$requestedSort currentSort=${currentState.sortBy} " +
                                "requestedCategory=$requestedCategory currentCategory=${currentState.category}"
                        )
                        return@fold
                    }

                    _uiState.value = _uiState.value.copy(
                        posts = posts, isLoading = false, isRefreshing = false, error = null
                    )
                    loadSavedPostIds()
                    loadUserVotes()
                    Log.d(
                        "HomeSortTrace",
                        "loadPosts STATE_APPLIED requestId=$requestId " +
                            "requestedSort=$requestedSort finalSort=${_uiState.value.sortBy} " +
                            "postCount=${_uiState.value.posts.size}"
                    )
                },
                onFailure = { e ->
                    Log.e(
                        "HomeSortTrace",
                        "loadPosts FAILED requestId=$requestId latest=${requestId == latestPostsLoadRequestId} " +
                            "requestedSort=$requestedSort error=${e.message}",
                        e
                    )
                    if (requestId == latestPostsLoadRequestId &&
                        requestedSort == _uiState.value.sortBy &&
                        requestedCategory == _uiState.value.category
                    ) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, isRefreshing = false, error = e.message
                        )
                    } else {
                        Log.d("HomeSortTrace", "loadPosts STALE_FAILURE_IGNORED requestId=$requestId")
                    }
                }
            )
        }
    }

    private suspend fun loadSavedPostIds() {
        val userId = authRepository.currentUserId ?: return
        savedPostsRepository.getSavedPostIds(userId).fold(
            onSuccess = { ids -> _uiState.value = _uiState.value.copy(savedPostIds = ids.toSet()) },
            onFailure = {}
        )
    }

    private suspend fun loadUserVotes() {

        val userId = authRepository.currentUserId ?: return

        // Preserve the last known vote if Supabase temporarily fails to read it.
        val votes = _uiState.value.userVotes.toMutableMap()

        _uiState.value.posts.forEach { post ->

            android.util.Log.d(
                "VOTE_DEBUG",
                "currentUserId=$userId postId=${post.id}"
            )
            postRepository.getUserVote(userId, post.id).fold(
                onSuccess = { vote -> votes[post.id] = vote },
                onFailure = { error ->
                    android.util.Log.w("VOTE_DEBUG", "Couldn't refresh vote for post ${post.id}", error)
                }
            )
        }

        _uiState.value = _uiState.value.copy(
            userVotes = votes
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
                android.util.Log.e("VOTE_DEBUG", "Saving vote failed for post $postId", error)
                // Refresh authoritative state after a failed save; do not assume it toggled off.
                loadUserVotes()
                loadPosts()
            }
        }
    }

    fun setSortBy(sort: String) {
        val normalized = sort.trim().lowercase()
        Log.d("HomeSortTrace", "ViewModel.setSortBy ENTER requested=$sort normalized=$normalized previous=${_uiState.value.sortBy}")
        _uiState.value = _uiState.value.copy(sortBy = normalized)
        Log.d("HomeSortTrace", "ViewModel.setSortBy STATE_WRITTEN sort=${_uiState.value.sortBy}")
        loadPosts()
        Log.d("HomeSortTrace", "ViewModel.setSortBy EXIT sort=${_uiState.value.sortBy}")
    }

    fun setCategory(cat: String?) {
        _uiState.value = _uiState.value.copy(category = cat)
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
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "Report submitted successfully."
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = it.message ?: "Failed to submit report."
                    )
                }
        }
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(
            snackbarMessage = null
        )
    }
}

// ── Quick stat cards (row below brand bar) ────────────────────────────────────
private data class StatCard(
    val emoji: String,
    val label: String,
    val value: String,
    val sub: String,
    val tint: Color,
    val isLive: Boolean = false,
    val route: String? = null
)

@Composable
fun HomeScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onCreatePost: () -> Unit,
    onProfileClick: () -> Unit,
    onNotifications: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    SideEffect {
        Log.d("HomeSortTrace", "COMPOSE HomeScreen recomposed sortBy=${uiState.sortBy} posts=${uiState.posts.size} loading=${uiState.isLoading}")
    }
    LaunchedEffect(uiState.sortBy) {
        Log.d("HomeSortTrace", "COMPOSE sortBy effect observed=${uiState.sortBy}")
    }
    val context = LocalContext.current
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    LaunchedEffect(Unit) {

        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {

            delay(500)

            locationPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }


    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)
    val listState = rememberLazyListState()
    var sortExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var headerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); headerVisible = true }
    val userName = "Ayush"
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

  /*  val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning, $userName ☀️"
            in 12..16 -> "Good Afternoon, $userName 🌤️"
            in 17..20 -> "Good Evening, $userName 🌆"
            else -> "Good Night, $userName 🌙"
        }
    }  */

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

    // Bell pulse
    var previousUnreadNotifCount by remember { mutableStateOf(uiState.unreadNotifCount) }
    var previousUnreadMsgCount by remember { mutableStateOf(uiState.unreadMsgCount) }
    var notificationCountInitialized by remember { mutableStateOf(false) }
    var messageCountInitialized by remember { mutableStateOf(false) }
    val bellRotation = remember { androidx.compose.animation.core.Animatable(0f) }
    val messageRotation = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(uiState.unreadNotifCount, uiState.hasLoadedUnreadNotifications) {
        val current = uiState.unreadNotifCount
        if (uiState.hasLoadedUnreadNotifications && notificationCountInitialized && current > previousUnreadNotifCount) {
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

    LaunchedEffect(uiState.unreadMsgCount, uiState.hasLoadedUnreadMessages) {
        val current = uiState.unreadMsgCount
        if (uiState.hasLoadedUnreadMessages && messageCountInitialized && current > previousUnreadMsgCount) {
            messageRotation.snapTo(0f)
            messageRotation.animateTo(0f, animationSpec = keyframes {
                durationMillis = 800
                10f at 100
                -10f at 200
                7f at 300
                -5f at 400
                0f at 550
            })
        }
        if (uiState.hasLoadedUnreadMessages) {
            previousUnreadMsgCount = current
            messageCountInitialized = true
        }
    }


    Scaffold(
        modifier = Modifier.pointerInput(uiState.sortBy) {
            var horizontalDistance = 0f
            Log.d("HomeSortTrace", "GESTURE detector CREATED keySort=${uiState.sortBy}")
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    horizontalDistance = 0f
                    Log.d("HomeSortTrace", "GESTURE START x=${offset.x} y=${offset.y} sortAtStart=${uiState.sortBy} detectorKey=${uiState.sortBy}")
                },
                onHorizontalDrag = { change, dragAmount ->
                    horizontalDistance += dragAmount
                    Log.d("HomeSortTrace", "GESTURE DRAG dx=$dragAmount totalDx=$horizontalDistance pointerX=${change.position.x} previousX=${change.previousPosition.x} sortSnapshot=${uiState.sortBy} consumed=${change.isConsumed}")
                },
                onDragEnd = {
                    val tabs = listOf("top", "new", "hot")
                    val sortSnapshot = uiState.sortBy.lowercase()
                    val currentIndex = tabs.indexOf(sortSnapshot)
                    val direction = when {
                        horizontalDistance < 0f -> "LEFT / next tab"
                        horizontalDistance > 0f -> "RIGHT / previous tab"
                        else -> "NO HORIZONTAL MOVEMENT"
                    }
                    Log.d("HomeSortTrace", "GESTURE END totalDx=$horizontalDistance direction=$direction sortSnapshot=$sortSnapshot index=$currentIndex tabs=$tabs threshold=80")
                    if (abs(horizontalDistance) > 80f) {
                        if (currentIndex >= 0) {
                            // From Top, either swipe direction opens New.
                            // From New, either swipe direction opens Hot.
                            // From Hot, retain normal bidirectional navigation.
                            val targetIndex = when {
                                sortSnapshot == "top" -> tabs.indexOf("new")
                                sortSnapshot == "new" -> tabs.indexOf("hot")
                                horizontalDistance < 0f -> (currentIndex + 1) % tabs.size
                                horizontalDistance > 0f -> (currentIndex - 1 + tabs.size) % tabs.size
                                else -> currentIndex
                            }
                            val target = tabs[targetIndex]
                            val gestureDirection = if (horizontalDistance < 0f) "LEFT/NEXT" else "RIGHT/PREVIOUS"
                            Log.d(
                                "HomeSortTrace",
                                "GESTURE ACTION $gestureDirection target=$target old=$sortSnapshot index=$currentIndex targetIndex=$targetIndex"
                            )
                            viewModel.setSortBy(target)
                            Log.d("HomeSortTrace", "GESTURE ACTION setSortBy invoked target=$target")
                        } else {
                            Log.w("HomeSortTrace", "GESTURE BAD_INDEX dx=$horizontalDistance sort=$sortSnapshot index=$currentIndex")
                        }
                    } else {
                        Log.d("HomeSortTrace", "GESTURE IGNORED below threshold absDx=${abs(horizontalDistance)}")
                    }
                    horizontalDistance = 0f
                },
                onDragCancel = {
                    Log.w("HomeSortTrace", "GESTURE CANCEL totalDx=$horizontalDistance sortSnapshot=${uiState.sortBy}")
                    horizontalDistance = 0f
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,

        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },

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
                                startY = 0f, endY = 340f
                            )
                        )
                        .statusBarsPadding()
                ) {

                    // ── Row 1: Brand + icons ──────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                ) {
                                    append("Nagpur ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                ) {
                                    append("Pulse")
                                }
                                // animated waveform suffix
                                withStyle(
                                    SpanStyle(
                                        color = OrangePrimary,
                                        fontSize = 14.sp
                                    )
                                ) {
                                    append(" ·⌇")
                                }
                            }
                        )

                        Spacer(Modifier.weight(1f))

                        // Search
                        HeaderIcon(Icons.Filled.Search, badge = 0) {
                            navController.navigate(Screen.Explore.route)
                        }

                        Spacer(Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .clickable {
                                    android.util.Log.d("HOME_THEME", "Moon clicked")
                                    viewModel.updateTheme(
                                        context = context,
                                        enabled = !ThemeManager.isLightTheme
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    if (ThemeManager.isLightTheme)
                                        Icons.Filled.DarkMode
                                    else
                                        Icons.Filled.LightMode,
                                contentDescription = "Theme",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        // Notifications — with badge + pulse
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .pressScale(onClick = onNotifications),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Notifications, null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp).rotate(bellRotation.value)
                            )
                            if (uiState.unreadNotifCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = if (uiState.unreadNotifCount > 9) 16.dp else 12.dp,
                                            height = 12.dp
                                        )
                                        .clip(CircleShape)
                                        .background(OrangePrimary)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-1).dp, y = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${uiState.unreadNotifCount.coerceAtMost(9)}${if (uiState.unreadNotifCount > 9) "+" else ""}",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(6.dp))

                        // Messages button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    android.util.Log.d("MSG_TEST", "Messages Clicked")
                                    navController.navigate(Screen.Messages.route)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Forum,
                                contentDescription = "Messages",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp).rotate(messageRotation.value)
                            )

                            if (uiState.unreadMsgCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(OrangePrimary)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-1).dp, y = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (uiState.unreadMsgCount > 9) "9+" else "${uiState.unreadMsgCount}",
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // ── Row 2: Location + Greeting ───────────────────

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 0.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(11.dp)
                            )

                            Text(
                                text = " ${uiState.currentArea}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }



                    }


                    // ── Row 3: Weather + AQI + Sort ───────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val isDaytime = hour in 5..19

                        QuickInfoChip(
                            icon = if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                            text = uiState.temperature?.let { "$it°" } ?: "--°",
                            color = if (isDaytime) Color(0xFFFFB300) else Color(0xFF5C9EFF)
                        )

                        val aqiText =
                            uiState.aqi?.let { "AQI $it" }
                                ?: "AQI --"

                        QuickInfoChip(
                            icon = Icons.Filled.Eco,
                            text = aqiText,
                            color = when (uiState.aqi) {
                                null -> OrangePrimary
                                in 0..50 -> Color(0xFF4CAF50)
                                in 51..100 -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )

                        Spacer(
                            modifier = Modifier.width(
                                if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 380) {
                                    24.dp
                                } else {
                                    8.dp
                                }
                            )
                        )

                        Log.d("HomeSortTrace", "HEADER rendering SortChipGroup selected=${uiState.sortBy}")
                        SortChipGroup(
                            selected = uiState.sortBy,
                            onSelected = { sort ->
                                Log.d("HomeSortTrace", "CHIP CALLBACK tapped=$sort stateBefore=${uiState.sortBy}")
                                viewModel.setSortBy(sort.lowercase())
                                Log.d("HomeSortTrace", "CHIP CALLBACK completed tapped=$sort")
                            }
                        )
                    }



                    Spacer(Modifier.height(2.dp))
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
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { viewModel.loadPosts(refresh = true) },
            modifier = Modifier
                .padding(paddingValues)

        ) {
            when {
                uiState.isLoading -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            horizontal = DensityManager.cardPadding.dp,
                            vertical = DensityManager.itemSpacing.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            DensityManager.itemSpacing.dp
                        )
                    ) {
                        items(6) {
                            ShimmerPostCard()
                        }
                    }
                }

                uiState.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationCity,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Nothing trending yet",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Be the first to post about Nagpur!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(OrangeSubtle)
                                    .border(
                                        1.dp,
                                        OrangePrimary.copy(0.5f),
                                        RoundedCornerShape(22.dp)
                                    )
                                    .pressScale(onClick = onCreatePost)
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "+ Create Post",
                                    color = OrangePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                else -> {

                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = DensityManager.itemSpacing.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(
                            DensityManager.itemSpacing.dp
                        )
                    )
                   {
                        itemsIndexed(
                            items = uiState.posts,
                            key = { _, post -> post.id }
                        ) { index, post ->
                            StaggeredItem(index = index) {
                                PostCard(
                                    post = post,
                                    currentVote = uiState.userVotes[post.id],
                                    isSaved = post.id in uiState.savedPostIds,
                                    isOwnPost = post.userId == viewModel.getCurrentUserId(),
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
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }



}

// ── Header icon button ────────────────────────────────────────────────────────
@Composable
private fun HeaderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary)
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SortChipGroup(
    selected: String,
    onSelected: (String) -> Unit
) {
    SideEffect {
        Log.d("HomeSortTrace", "CHIPS COMPOSE selectedRaw=$selected normalized=${selected.trim().lowercase()}")
    }
    // Keep the three sort labels on one line on narrow phones (e.g. Oppo A5).
    // The existing spacing and typography remain unchanged on wider phones.
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 380

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(18.dp)
            )
            .padding(3.dp)
    ) {
        listOf("top", "new", "hot").forEach { item ->
            val isSelected = item.equals(selected.trim(), ignoreCase = true)
            SideEffect {
                Log.d("HomeSortTrace", "CHIP RENDER item=$item selected=$selected isSelected=$isSelected")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) OrangePrimary.copy(alpha = 0.22f)
                        else Color.Transparent
                    )
                    .then(
                        if (isSelected) Modifier.border(
                            width = 1.dp,
                            color = OrangePrimary.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(14.dp)
                        ) else Modifier
                    )
                    .clickable { onSelected(item) }
                    .padding(
                        horizontal = if (compact) 5.dp else 10.dp,
                        vertical = 6.dp
                    )
            ) {
                Text(
                    text = item.replaceFirstChar { it.uppercase() },
                    maxLines = 1,
                    softWrap = false,
                    fontSize = if (compact) 14.sp else MaterialTheme.typography.bodyLarge.fontSize,
                    color = if (isSelected) OrangePrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold
                    else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuickInfoChip(
    icon: ImageVector,
    text: String,
    color: Color = OrangePrimary,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                color.copy(alpha = 0.25f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Quick stat card ───────────────────────────────────────────────────────────
@Composable
private fun QuickStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String = "",
    tint: Color,
    subColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selected: Boolean = false,
    isLive: Boolean = false,
    liveLabel: String = "",
    onClick: () -> Unit
) {
    val t = rememberInfiniteTransition(label = "live_$label")
    val liveAlpha by t.animateFloat(
        1f, 0.2f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "la"
    )

    Column(
        modifier = Modifier
            .width(106.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected)
                    Brush.linearGradient(listOf(OrangePrimary.copy(0.22f), OrangeLight.copy(0.10f)))
                else
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
            )
            .border(
                1.dp,
                if (selected) OrangePrimary.copy(0.55f) else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            )
            .pressScale(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(2.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)

        if (isLive && liveLabel.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = liveAlpha))
                )
                Spacer(Modifier.width(4.dp))
                Text(liveLabel, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                // Animated waveform bars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    listOf(0.6f, 1f, 0.7f).forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height((8 * h).dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(tint.copy(alpha = liveAlpha * 0.8f))
                        )
                    }
                }
            }
        } else if (sub.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(sub, color = subColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
