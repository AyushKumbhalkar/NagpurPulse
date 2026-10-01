//This is the HomeSCreen.kt file and below is the path of the file
// java/com/nagpurpulse/ui/screens/home/HomeScreen.kt

package com.nagpurpulse.ui.screens.home



import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Context
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
    val unreadMsgCount: Int = 0,
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
        startPresenceWhenAuthenticated()
    }

    private fun loadUnreadMessages() {
        viewModelScope.launch {
            messageRepository.getConversations().onSuccess { conversations ->
                _uiState.value = _uiState.value.copy(
                    unreadMsgCount = conversations.sumOf { it.myUnreadCount }
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


    fun loadPosts(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = !refresh, isRefreshing = refresh)
            postRepository.getPosts(
                category = _uiState.value.category,
                sortBy = _uiState.value.sortBy
            ).fold(
                onSuccess = { posts ->
                    _uiState.value = _uiState.value.copy(
                        posts = posts, isLoading = false, isRefreshing = false
                    )
                    loadSavedPostIds()
                    loadUserVotes()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, isRefreshing = false, error = e.message
                    )
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

        val votes = mutableMapOf<String, String?>()

        _uiState.value.posts.forEach { post ->

            android.util.Log.d(
                "VOTE_DEBUG",
                "currentUserId=$userId postId=${post.id}"
            )
            postRepository.getUserVote(
                userId,
                post.id
            ).getOrNull()?.let { vote ->

                votes[post.id] = vote
            }
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
            postRepository.votePost(
                userId,
                postId,
                voteType
            )
        }
    }

    fun setSortBy(sort: String) {
        _uiState.value = _uiState.value.copy(sortBy = sort)
        loadPosts()
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
    val t = rememberInfiniteTransition(label = "home_t")
    val bellPulse by t.animateFloat(
        1f, 1.15f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bell"
    )

    Scaffold(
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
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrowHeader = maxWidth < 360.dp
                        val headerIconSize = if (isNarrowHeader) 34.dp else 40.dp
                        val headerIconSpacing = if (isNarrowHeader) 3.dp else 6.dp

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = if (isNarrowHeader) 10.dp else 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        // Brand
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (isNarrowHeader) 18.sp else 22.sp
                                    )
                                ) {
                                    append("Nagpur ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = OrangePrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = if (isNarrowHeader) 18.sp else 22.sp
                                    )
                                ) {
                                    append("Pulse")
                                }
                                // animated waveform suffix
                                withStyle(
                                    SpanStyle(
                                        color = OrangePrimary,
                                        fontSize = if (isNarrowHeader) 12.sp else 14.sp
                                    )
                                ) {
                                    append(" ·⌇")
                                }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(Modifier.weight(1f))

                        // Search
                        HeaderIcon(Icons.Filled.Search, badge = 0, size = headerIconSize) {
                            navController.navigate(Screen.Explore.route)
                        }

                        Spacer(Modifier.width(headerIconSpacing))

                        Box(
                            modifier = Modifier
                                .size(headerIconSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                modifier = Modifier.size(if (isNarrowHeader) 18.dp else 20.dp)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        // Notifications — with badge + pulse
                        Box(
                            modifier = Modifier
                                .size(headerIconSize)
                                .scale(bellPulse)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pressScale(onClick = onNotifications),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Notifications, null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (isNarrowHeader) 18.dp else 20.dp)
                            )
                            if (uiState.unreadNotifCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = if (uiState.unreadNotifCount > 9) 18.dp else 14.dp,
                                            height = 14.dp
                                        )
                                        .clip(CircleShape)
                                        .background(OrangePrimary)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-3).dp),
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
                                .size(headerIconSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    android.util.Log.d("MSG_TEST", "Messages Clicked")
                                    navController.navigate(Screen.Messages.route)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = "Messages",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(if (isNarrowHeader) 18.dp else 20.dp)
                            )

                            if (uiState.unreadMsgCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(OrangePrimary)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-3).dp),
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val isNarrowHomeHeader = maxWidth < 360.dp
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val isDaytime = hour in 5..19
                        val aqiText = uiState.aqi?.let { "AQI $it" } ?: "AQI --"
                        val aqiColor = when (uiState.aqi) {
                            null -> OrangePrimary
                            in 0..50 -> Color(0xFF4CAF50)
                            in 51..100 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }

                        if (isNarrowHomeHeader) {
                            // Small phones: keep weather/AQI and sort controls on separate rows.
                            // This prevents the Top/New/Hot selector from being pushed off-screen.
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    QuickInfoChip(
                                        icon = if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                                        text = uiState.temperature?.let { "$it°" } ?: "--°",
                                        color = if (isDaytime) Color(0xFFFFB300) else Color(0xFF5C9EFF)
                                    )
                                    QuickInfoChip(
                                        icon = Icons.Filled.Eco,
                                        text = aqiText,
                                        color = aqiColor
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    SortChipGroup(
                                        selected = uiState.sortBy,
                                        onSelected = { viewModel.setSortBy(it) }
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuickInfoChip(
                                    icon = if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                                    text = uiState.temperature?.let { "$it°" } ?: "--°",
                                    color = if (isDaytime) Color(0xFFFFB300) else Color(0xFF5C9EFF)
                                )
                                QuickInfoChip(
                                    icon = Icons.Filled.Eco,
                                    text = aqiText,
                                    color = aqiColor
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                SortChipGroup(
                                    selected = uiState.sortBy,
                                    onSelected = { viewModel.setSortBy(it) }
                                )
                            }
                        }
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
            modifier = Modifier.padding(paddingValues)
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
    size: androidx.compose.ui.unit.Dp = 40.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
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

            val isSelected = item == selected

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected)
                            OrangePrimary.copy(alpha = 0.12f)
                        else
                            Color.Transparent
                    )
                    .clickable {
                        onSelected(item)
                    }
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
            ) {
                Text(
                    text = item.replaceFirstChar { it.uppercase() },
                    color =
                        if (isSelected)
                            OrangePrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight =
                        if (isSelected)
                            FontWeight.Bold
                        else
                            FontWeight.Medium
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
