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
import com.nagpurpulse.data.model.emoji
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.ui.components.EmptyState
import com.nagpurpulse.ui.components.ShimmerPostCard
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class NotifUiState(
    val notifications: List<Notification> = emptyList(),
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
    @ApplicationContext private val appContext: Context        // ← injected, not passed from UI
) : ViewModel() {

    private val _s = MutableStateFlow(NotifUiState())
    val state: StateFlow<NotifUiState> = _s

    // One-shot event to ask the composable to launch the permission request
    private val _requestPermission = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val requestPermissionEvent: SharedFlow<Unit> = _requestPermission.asSharedFlow()

    init {
        load()
        checkBannerState()
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun load() {
        val uid = authRepository.currentUserId ?: run {
            _s.value = _s.value.copy(isLoading = false)
            return
        }
        viewModelScope.launch {
            _s.value = _s.value.copy(isLoading = true)
            notificationRepository.getNotifications(uid).fold(
                onSuccess = { list ->
                    _s.value = _s.value.copy(notifications = list, isLoading = false, error = null)
                },
                onFailure = { e ->
                    _s.value = _s.value.copy(isLoading = false, error = e.message)
                }
            )
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
            notificationRepository.markAllRead(uid)
            _s.value = _s.value.copy(
                notifications = _s.value.notifications.map { it.copy(isRead = true) }
            )
        }
    }

    fun markOneRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markOneRead(id)
            _s.value = _s.value.copy(
                notifications = _s.value.notifications.map { n ->
                    if (n.id == id) n.copy(isRead = true) else n
                }
            )
        }
    }

    fun clearAll() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.markAllRead(uid)
            _s.value = _s.value.copy(notifications = emptyList())
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
            it.type in listOf("upvote", "like")
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

// ── Type helpers ──────────────────────────────────────────────────────────────

@Composable
private fun typeColor(type: String): Color = when (type) {
    "comment", "reply"     -> OrangePrimary
    "mention"              -> BlueInfo
    "upvote", "like"       -> GreenSuccess
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

    // Group into Today vs Earlier
    val (todayNotifs, earlierNotifs) = filtered.partition { n ->
        try {
            java.time.Duration.between(
                java.time.Instant.parse(n.createdAt),
                java.time.Instant.now()
            ).toHours() < 24
        } catch (_: Exception) { false }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                Modifier
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 180f))
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Notifications",
                            color = PrimaryText, fontWeight = FontWeight.Black, fontSize = 20.sp
                        )
                        if (unread > 0) {
                            Text(
                                "$unread unread",
                                color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (unread > 0) {
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
                                color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.clearAll() }) {
                        Icon(Icons.Filled.Delete, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { navController.navigate(Screen.NotifSettings.route) }) {
                        Icon(Icons.Filled.Settings, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                    }
                }

                // Filter tabs
                ScrollableTabRow(
                    selectedTabIndex = FILTER_TABS.indexOf(s.activeFilter).coerceAtLeast(0),
                    containerColor   = Color.Transparent,
                    contentColor     = OrangePrimary,
                    edgePadding      = 16.dp,
                    indicator = {}, divider = {}
                ) {
                    FILTER_TABS.forEach { f ->
                        val sel = s.activeFilter == f
                        Tab(selected = sel, onClick = { viewModel.setFilter(f) }, text = {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(if (sel) OrangePrimary else SurfaceAlt)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    when (f) {
                                        "all"      -> "All"
                                        "replies"  -> "Replies"
                                        "mentions" -> "Mentions"
                                        "upvotes"  -> "Upvotes"
                                        "messages" -> "Messages"
                                        "alerts"   -> "Alerts"
                                        else       -> f.replaceFirstChar { it.uppercase() }
                                    },
                                    color      = if (sel) Color.White else SecondaryText,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    fontSize   = 13.sp
                                )
                            }
                        })
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
                Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.NotificationsNone,
                        title = "No notifications yet",
                        subtitle = "When people interact with your posts, you'll see it here."
                    )
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
                                todayNotifs.forEachIndexed { i, notif ->
                                    NotifRow(
                                        notif  = notif,
                                        onTap  = {
                                            viewModel.markOneRead(notif.id)
                                            notif.relatedPostId?.let { onPostClick(it) }
                                        }
                                    )
                                    if (i < todayNotifs.lastIndex) {
                                        HorizontalDivider(
                                            color     = Divider.copy(0.5f),
                                            thickness = 0.5.dp,
                                            modifier  = Modifier.padding(start = 72.dp)
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
                                earlierNotifs.forEachIndexed { i, notif ->
                                    NotifRow(
                                        notif = notif,
                                        onTap = {
                                            viewModel.markOneRead(notif.id)
                                            notif.relatedPostId?.let { onPostClick(it) }
                                        }
                                    )
                                    if (i < earlierNotifs.lastIndex) {
                                        HorizontalDivider(
                                            color     = Divider.copy(0.5f),
                                            thickness = 0.5.dp,
                                            modifier  = Modifier.padding(start = 72.dp)
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
private fun NotifRow(notif: Notification, onTap: () -> Unit) {
    val isUnread = !notif.isRead
    val accent   = typeColor(notif.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isUnread) accent.copy(0.05f) else Color.Transparent)
            .pressScale(onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar — real sender photo if available, emoji fallback otherwise
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    Text(notif.emoji(), fontSize = 10.sp)
                }
            }
            // Type badge overlaid at bottom-right
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accent.copy(0.2f))
                    .border(1.dp, accent.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(notif.emoji(), fontSize = 10.sp) }
        }

        Spacer(Modifier.width(12.dp))

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
                lineHeight = 20.sp
            )
            if (!notif.body.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    "\"${notif.body}\"",
                    color    = TertiaryText,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(notif.timeAgo(), color = TertiaryText, fontSize = 11.sp)
        }

        Spacer(Modifier.width(10.dp))

        Column(horizontalAlignment = Alignment.End) {
            if (isUnread) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(OrangePrimary))
            }
            if (notif.relatedPostId != null) {
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceAlt),
                    Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Description,
                        null,
                        tint = TertiaryText,
                        modifier = Modifier.size(20.dp)
                    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, OrangePrimary.copy(0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
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

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "Stay updated, Nagpur!",
                color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
            )
            Text(
                "Enable push notifications to never miss replies, alerts & trending posts.",
                color = SecondaryText, fontSize = 12.sp, lineHeight = 16.sp
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
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Enable", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, tint = TertiaryText, modifier = Modifier.size(16.dp))
            }
        }
    }
}
