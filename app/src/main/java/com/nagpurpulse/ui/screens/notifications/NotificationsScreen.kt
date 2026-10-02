// ui/screens/notifications/NotificationsScreen.kt  — REPLACE entirely
package com.nagpurpulse.ui.screens.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Notification
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.ui.components.EmptyState
import com.nagpurpulse.ui.components.ShimmerPostCard
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class NotifUiState(
    val notifications: List<Notification> = emptyList(),
    val postPreviews: Map<String, Post> = emptyMap(),
    val isLoading:     Boolean = true,
    val activeFilter:  String  = "all",
    val showPushBanner: Boolean = false,   // determined by real permission check in VM
    val error:         String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val postRepository: PostRepository,
    @ApplicationContext private val appContext: Context        // ← injected, not passed from UI
) : ViewModel() {

    private val _s = MutableStateFlow(NotifUiState())
    val state: StateFlow<NotifUiState> = _s

    // One-shot event to ask the composable to launch the permission request
    private val _requestPermission = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val requestPermissionEvent: SharedFlow<Unit> = _requestPermission.asSharedFlow()

    init {
        load()
        startRealtimeUpdates()
        checkBannerState()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun load(showLoading: Boolean = true) {
        val uid = authRepository.currentUserId ?: run {
            _s.value = _s.value.copy(isLoading = false)
            return
        }
        viewModelScope.launch {
            _s.value = _s.value.copy(isLoading = showLoading)
            notificationRepository.getNotifications(uid).fold(
                onSuccess = { list ->
                    _s.value = _s.value.copy(notifications = list, isLoading = false, error = null)
                    loadPostPreviews(list)
                },
                onFailure = { e ->
                    _s.value = _s.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    private fun loadPostPreviews(notifications: List<Notification>) {
        // Keep preview requests bounded to reaction notifications until a batch
        // post-preview query is available; avoid one request per every inbox item.
        val postIds = notifications
            .filter { it.type in listOf("upvote", "like", "comment_like") }
            .mapNotNull { it.relatedPostId }
            .distinct()
        if (postIds.isEmpty()) return
        viewModelScope.launch {
            val previews = coroutineScope {
                postIds.map { postId ->
                    async { postId to postRepository.getPostPreviewById(postId).getOrNull() }
                }.awaitAll()
            }.mapNotNull { (id, post) -> post?.let { id to it } }.toMap()
            _s.value = _s.value.copy(postPreviews = _s.value.postPreviews + previews)
        }
    }

    private fun startRealtimeUpdates() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.subscribeToNotifications(uid).collect {
                load(showLoading = false)
            }
        }
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    private fun checkBannerState() {
        val alreadyDismissed = NotifPrefsHelper.isBannerDismissed(appContext)
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true   // pre-T devices don't need runtime permission
        _s.value = _s.value.copy(showPushBanner = !alreadyDismissed && !hasPermission)
    }

    /** Tapping Enable → emit event so composable launches permission dialog */
    fun onEnablePushClicked() {
        viewModelScope.launch { _requestPermission.emit(Unit) }
    }

    /** Called after permission granted or dismissed */
    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            viewModelScope.launch {
                com.nagpurpulse.notifications.ScheduledPushManager.schedule(appContext)
            }
        }
        // Either way, hide the banner (user has responded)
        dismissPushBanner()
    }

    fun dismissPushBanner() {
        _s.value = _s.value.copy(showPushBanner = false)
        // Persist so it never reappears after dismiss
        viewModelScope.launch {
            userPreferencesRepository.dismissNotifBanner(appContext)
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun markAllRead() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.markAllRead(uid).fold(
                onSuccess = {
                    _s.value = _s.value.copy(
                        notifications = _s.value.notifications.map { it.copy(isRead = true) },
                        error = null
                    )
                },
                onFailure = { error ->
                    _s.value = _s.value.copy(error = "Couldn't mark notifications as read. Please try again.")
                    android.util.Log.w("NotificationsVM", "markAllRead failed", error)
                }
            )
        }
    }

    fun markOneRead(id: String) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.markOneRead(uid, id).fold(
                onSuccess = {
                    _s.value = _s.value.copy(
                        notifications = _s.value.notifications.map { n ->
                            if (n.id == id) n.copy(isRead = true) else n
                        },
                        error = null
                    )
                },
                onFailure = { error ->
                    _s.value = _s.value.copy(error = "Couldn't update this notification. Please try again.")
                    android.util.Log.w("NotificationsVM", "markOneRead failed", error)
                }
            )
        }
    }

    fun markGroupRead(ids: List<String>) {
        val uid = authRepository.currentUserId ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            var failed = false
            ids.forEach { id ->
                if (notificationRepository.markOneRead(uid, id).isFailure) failed = true
            }
            if (failed) {
                _s.value = _s.value.copy(error = "Some notifications couldn't be marked as read. Please try again.")
            } else {
                val idSet = ids.toSet()
                _s.value = _s.value.copy(
                    notifications = _s.value.notifications.map { n ->
                        if (n.id in idSet) n.copy(isRead = true) else n
                    },
                    error = null
                )
            }
        }
    }

    fun clearAll() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.deleteAll(uid).fold(
                onSuccess = { _s.value = _s.value.copy(notifications = emptyList(), postPreviews = emptyMap(), error = null) },
                onFailure = { error ->
                    _s.value = _s.value.copy(error = "Couldn't clear notifications. Please try again.")
                    android.util.Log.w("NotificationsVM", "deleteAll failed", error)
                }
            )
        }
    }

    fun setFilter(f: String) { _s.value = _s.value.copy(activeFilter = f) }

    // ── Filtered list (correct type mappings) ─────────────────────────────────

    fun filteredNotifs(): List<Notification> = when (_s.value.activeFilter) {
        "replies"  -> _s.value.notifications.filter {
            // PostRepository inserts "comment" type for new comments
            it.type in listOf("comment", "reply")
        }
        "mentions" -> _s.value.notifications.filter {
            it.type == "mention"
        }
        "upvotes"  -> _s.value.notifications.filter {
            it.type in listOf("upvote", "like", "comment_like")
        }
        "alerts"   -> _s.value.notifications.filter {
            it.type in listOf("alert", "emergency", "badge", "trending",
                "community", "digest", "admin_warning", "admin_suspension", "admin_ban")
        }
        "messages" -> _s.value.notifications.filter {
            it.type == "message"
        }
        else       -> _s.value.notifications
    }
}


// Parses timestamps into the device-local calendar date, accepting both UTC instants
// and offset-aware timestamps returned by Postgres/PostgREST.
private fun parseNotificationDate(value: String, zoneId: ZoneId): LocalDate? {
    return try {
        Instant.parse(value).atZone(zoneId).toLocalDate()
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(value).toInstant().atZone(zoneId).toLocalDate()
        } catch (_: Exception) {
            null
        }
    }
}


// Reaction notifications are grouped only when they refer to the same post/comment
// and arrived within 15 minutes. Replies, mentions, messages and safety/admin alerts
// are always shown individually.
private data class NotificationDisplayItem(
    val notification: Notification,
    val notificationIds: List<String>
)

private fun groupNotificationsForDisplay(notifications: List<Notification>): List<NotificationDisplayItem> {
    val result = mutableListOf<NotificationDisplayItem>()
    val groupedIndices = mutableSetOf<Int>()
    val reactionTypes = setOf("upvote", "like", "comment_like")

    notifications.forEachIndexed { index, notification ->
        if (index in groupedIndices) return@forEachIndexed
        val isReaction = notification.type.lowercase() in reactionTypes
        val target = if (notification.type == "comment_like") notification.relatedCommentId else notification.relatedPostId
        val matching = if (isReaction && !target.isNullOrBlank()) {
            val baseTime = runCatching { Instant.parse(notification.createdAt) }.getOrNull()
            if (baseTime == null) listOf(index) else notifications.indices.filter { candidateIndex ->
                if (candidateIndex in groupedIndices) return@filter false
                val candidate = notifications[candidateIndex]
                val candidateTarget = if (candidate.type == "comment_like") candidate.relatedCommentId else candidate.relatedPostId
                candidate.type == notification.type &&
                    candidate.relatedPostId == notification.relatedPostId &&
                    candidateTarget == target &&
                    runCatching { Instant.parse(candidate.createdAt) }.getOrNull()?.let { time ->
                        kotlin.math.abs(java.time.Duration.between(baseTime, time).toMinutes()) <= 15
                    } == true
            }
        } else listOf(index)

        val members = matching.map(notifications::get).sortedByDescending { it.createdAt }
        matching.forEach(groupedIndices::add)
        val newest = members.first()
        val display = if (members.size < 2) newest else {
            val count = members.size
            val title = when (newest.type) {
                "comment_like" -> "$count people liked your comment"
                else -> "$count people reacted to your post"
            }
            newest.copy(
                title = title,
                body = null,
                isRead = members.all { it.isRead }
            )
        }
        result += NotificationDisplayItem(display, members.map { it.id })
    }
    return result.sortedByDescending { it.notification.createdAt }
}

// ── Type helpers ──────────────────────────────────────────────────────────────

private fun notificationTypeIcon(type: String) = when (type.lowercase()) {
    "comment", "reply" -> Icons.Filled.ModeComment
    "mention" -> Icons.Filled.AlternateEmail
    "upvote", "like", "comment_like" -> Icons.Filled.ThumbUp
    "message" -> Icons.AutoMirrored.Filled.Message
    "alert", "emergency", "admin_warning" -> Icons.Filled.Warning
    "badge" -> Icons.Filled.Star
    "trending" -> Icons.Filled.TrendingUp
    "community" -> Icons.Filled.Groups
    "digest" -> Icons.Filled.Campaign
    "admin_suspension" -> Icons.Filled.GppBad
    "admin_ban" -> Icons.Filled.Block
    else -> Icons.Filled.Notifications
}

@Composable
private fun typeColor(type: String): Color = when (type) {
    "comment", "reply"     -> OrangePrimary
    "mention"              -> BlueInfo
    "upvote", "like", "comment_like" -> GreenSuccess
    "message"              -> BlueInfo
    "alert", "emergency"   -> RedAlert
    "badge"                -> YellowWarn
    "trending"             -> OrangePrimary
    "community"            -> PurpleNight
    "digest"               -> GreenSuccess
    "admin_warning"        -> YellowWarn
    "admin_suspension"     -> OrangePrimary
    "admin_ban"            -> RedAlert
    else                   -> TertiaryText
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val FILTER_TABS = listOf("all", "replies", "mentions", "upvotes", "messages", "alerts")

@Composable
fun NotificationsScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val ctx      = LocalContext.current
    val s        = viewModel.state.collectAsState().value
    val filtered = viewModel.filteredNotifs()
    val unread   = s.notifications.count { !it.isRead }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(s.error) {
        s.error?.let { snackbarHostState.showSnackbar(it) }
    }

    // Permission launcher wired to the VM's event
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    // Collect one-shot permission request events from VM
    LaunchedEffect(Unit) {
        viewModel.requestPermissionEvent.collect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onPermissionResult(true)
            }
        }
    }

    // Group by the user's local calendar date, not a rolling 24-hour window.
    val localZone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(localZone) }
    val displayItems = remember(filtered) { groupNotificationsForDisplay(filtered) }
    val (todayNotifs, earlierNotifs) = displayItems.partition { item ->
        parseNotificationDate(item.notification.createdAt, localZone) == today
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                Modifier
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 180f))
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Notifications",
                            color = PrimaryText,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (unread > 0) {
                            Text(
                                "$unread unread",
                                color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.NotifSettings.route) }) {
                        Icon(Icons.Filled.Settings, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                    }
                }

                if (unread > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(OrangeSubtle)
                                .border(1.dp, OrangePrimary.copy(0.3f), RoundedCornerShape(14.dp))
                                .pressScale { viewModel.markAllRead() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Mark all read",
                                color = OrangePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Filter tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FILTER_TABS.forEach { f ->
                        val sel = s.activeFilter == f
                        Box(
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (sel) OrangePrimary else SurfaceAlt)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                ) { viewModel.setFilter(f) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                when (f) {
                                    "all" -> "All"
                                    "replies" -> "Replies"
                                    "mentions" -> "Mentions"
                                    "upvotes" -> "Upvotes"
                                    "messages" -> "Messages"
                                    "alerts" -> "Alerts"
                                    else -> f.replaceFirstChar { it.uppercase() }
                                },
                                color = if (sel) Color.White else SecondaryText,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { pad ->
        when {
            s.isLoading -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) { items(7) { ShimmerPostCard() } }
            }

            filtered.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize().padding(pad),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (s.showPushBanner) {
                        PushEnableBanner(
                            onEnable = { viewModel.onEnablePushClicked() },
                            onDismiss = { viewModel.dismissPushBanner() },
                            onOpenSettings = {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                    }
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                        EmptyState(
                            icon = Icons.Filled.NotificationsNone,
                            title = if (s.activeFilter == "all") "You're all caught up" else "Nothing in this filter",
                            subtitle = if (s.activeFilter == "all")
                                "When people interact with your posts, you'll see it here."
                            else "Try another filter to see more activity."
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(pad),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {

                    // ── Push permission banner (real check, persisted dismiss) ──
                    if (s.showPushBanner) {
                        item {
                            PushEnableBanner(
                                onEnable  = { viewModel.onEnablePushClicked() },
                                onDismiss = { viewModel.dismissPushBanner() },
                                onOpenSettings = {
                                    ctx.startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                        }
                                    )
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── TODAY ────────────────────────────────────────────────────
                    if (todayNotifs.isNotEmpty()) {
                        item {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Today", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Box(Modifier.size(8.dp).clip(CircleShape).background(OrangePrimary))
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Surface)
                            ) {
                                todayNotifs.forEachIndexed { i, item ->
                                    val notif = item.notification
                                    NotifRow(
                                        notif  = notif,
                                        postPreview = notif.relatedPostId?.let { s.postPreviews[it] },
                                        groupedCount = item.notificationIds.size.takeIf { it > 1 },
                                        onTap  = {
                                            viewModel.markGroupRead(item.notificationIds)
                                            notif.relatedConversationId?.let {
                                                navController.navigate(Screen.Chat.createRoute(it))
                                            } ?: notif.relatedPostId?.let { postId ->
                                                if (!notif.relatedCommentId.isNullOrBlank()) {
                                                    navController.navigate(Screen.Thread.createRoute(postId, notif.relatedCommentId))
                                                } else {
                                                    onPostClick(postId)
                                                }
                                            } ?: if (notif.type in listOf("alert", "emergency", "admin_warning")) {
                                                navController.navigate(Screen.Alerts.route)
                                            } else Unit
                                        }
                                    )
                                    if (i < todayNotifs.lastIndex) {
                                        HorizontalDivider(
                                            color = Divider.copy(alpha = 0.85f),
                                            thickness = 0.75.dp,
                                            modifier = Modifier.padding(start = 66.dp, end = 12.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }

                    // ── EARLIER ──────────────────────────────────────────────────
                    if (earlierNotifs.isNotEmpty()) {
                        item {
                            Text(
                                "Earlier",
                                color    = PrimaryText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        item {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Surface)
                            ) {
                                earlierNotifs.forEachIndexed { i, item ->
                                    val notif = item.notification
                                    NotifRow(
                                        notif = notif,
                                        postPreview = notif.relatedPostId?.let { s.postPreviews[it] },
                                        groupedCount = item.notificationIds.size.takeIf { it > 1 },
                                        onTap = {
                                            viewModel.markGroupRead(item.notificationIds)
                                            notif.relatedConversationId?.let {
                                                navController.navigate(Screen.Chat.createRoute(it))
                                            } ?: notif.relatedPostId?.let { postId ->
                                                if (!notif.relatedCommentId.isNullOrBlank()) {
                                                    navController.navigate(Screen.Thread.createRoute(postId, notif.relatedCommentId))
                                                } else {
                                                    onPostClick(postId)
                                                }
                                            } ?: if (notif.type in listOf("alert", "emergency", "admin_warning")) {
                                                navController.navigate(Screen.Alerts.route)
                                            } else Unit
                                        }
                                    )
                                    if (i < earlierNotifs.lastIndex) {
                                        HorizontalDivider(
                                            color = Divider.copy(alpha = 0.85f),
                                            thickness = 0.75.dp,
                                            modifier = Modifier.padding(start = 66.dp, end = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── NotifRow — real sender avatar + full type coverage ────────────────────────

@Composable
private fun NotifRow(notif: Notification, postPreview: Post?, groupedCount: Int? = null, onTap: () -> Unit) {
    val isUnread = !notif.isRead
    val accent   = typeColor(notif.type)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 360.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isUnread) accent.copy(0.05f) else Color.Transparent)
            .pressScale(onClick = onTap)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar — real sender photo if available, emoji fallback otherwise
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(if (compact) 40.dp else 46.dp)
                    .clip(CircleShape)
                    .background(SurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                if (!notif.senderAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(notif.senderAvatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PersonOutline,
                        contentDescription = "Sender",
                        tint = SecondaryText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            // Type badge overlaid at bottom-right
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.42f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notificationTypeIcon(notif.type),
                    contentDescription = notif.type,
                    tint = accent,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(Modifier.width(if (compact) 6.dp else 12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isUnread) PrimaryText else SecondaryText
                        )
                    ) { append(notif.title) }
                },
                fontSize   = 14.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!notif.body.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "\"${notif.body}\"",
                    color    = TertiaryText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(notif.timeAgo(), color = TertiaryText, fontSize = 11.sp)
                groupedCount?.let { count ->
                    Text("$count", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(OrangeSubtle).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        Spacer(Modifier.width(if (compact) 6.dp else 10.dp))

        Column(horizontalAlignment = Alignment.End) {
            if (isUnread) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(OrangePrimary))
            }
            if (notif.relatedPostId != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(if (compact) 36.dp else 52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceAlt),
                    Alignment.Center
                ) {
                    if (!postPreview?.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(postPreview?.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.Description,
                                null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(if (compact) 14.dp else 17.dp)
                            )
                            if (!postPreview?.title.isNullOrBlank()) {
                                Text(
                                    postPreview?.title.orEmpty(),
                                    color = SecondaryText,
                                    fontSize = 7.sp,
                                    lineHeight = 8.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text("Post", color = TertiaryText, fontSize = 8.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

// ── Push enable banner — real permission flow ─────────────────────────────────

@Composable
private fun PushEnableBanner(
    onEnable:       () -> Unit,
    onDismiss:      () -> Unit,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
    val compact = maxWidth < 360.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, OrangePrimary.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(if (compact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(if (compact) 36.dp else 44.dp)
                .clip(CircleShape)
                .background(OrangeSubtle)
                .border(1.dp, OrangePrimary.copy(0.3f), CircleShape),
            Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(if (compact) 8.dp else 12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "Stay updated, Nagpur!",
                color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                "Enable push notifications to never miss replies, alerts & trending posts.",
                color = SecondaryText, fontSize = 12.sp, lineHeight = 16.sp,
                maxLines = if (compact) 3 else 4, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            // "Open in Settings" for users who previously denied
            Text(
                "Manage in Settings →",
                color    = OrangePrimary,
                fontSize = 11.sp,
                modifier = Modifier.clickable { onOpenSettings() }
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Enable button triggers real OS permission dialog
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrangePrimary)
                    .pressScale(onClick = onEnable)
                    .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 8.dp)
            ) {
                Text("Enable", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, tint = TertiaryText, modifier = Modifier.size(16.dp))
            }
        }
    }
    }
}
