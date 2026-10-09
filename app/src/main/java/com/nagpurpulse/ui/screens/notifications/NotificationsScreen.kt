@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.nagpurpulse.ui.screens.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeLabel
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.ui.components.EmptyState
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

// ── Type sets ─────────────────────────────────────────────────────────────────

private const val PAGE_SIZE = 60
private const val PAGE_STEP = 40
private const val GROUP_WINDOW_MIN = 180L

private val REACTION_TYPES = setOf("upvote", "like", "comment_like")
private val COMMENT_TYPES  = setOf("comment", "reply")
private val URGENT_TYPES   = setOf("alert", "emergency", "admin_warning", "admin_suspension", "admin_ban")
private val ADMIN_TYPES    = setOf("admin_warning", "admin_suspension", "admin_ban")
private val ALERT_TAB_TYPES = setOf(
    "alert", "emergency", "badge", "trending", "community", "digest",
    "admin_warning", "admin_suspension", "admin_ban", "milestone", "streak"
)
private val PREVIEW_TYPES = REACTION_TYPES + COMMENT_TYPES + setOf("milestone", "trending")
private val HIGH_PRIORITY_TYPES = COMMENT_TYPES + setOf("mention", "message", "milestone")

private val FILTER_TABS = listOf("all", "replies", "mentions", "upvotes", "messages", "alerts")

private fun filterLabel(f: String) = when (f) {
    "all" -> "All"
    "replies" -> "Replies"
    "mentions" -> "Mentions"
    "upvotes" -> "Upvotes"
    "messages" -> "Messages"
    "alerts" -> "Alerts"
    else -> f.replaceFirstChar { it.uppercase() }
}

private fun filterNotifs(list: List<Notification>, filter: String): List<Notification> = when (filter) {
    "replies"  -> list.filter { it.type in COMMENT_TYPES }
    "mentions" -> list.filter { it.type == "mention" }
    "upvotes"  -> list.filter { it.type in REACTION_TYPES }
    "alerts"   -> list.filter { it.type in ALERT_TAB_TYPES }
    "messages" -> list.filter { it.type == "message" }
    else       -> list
}

// ── State ─────────────────────────────────────────────────────────────────────

data class NotifUiState(
    val notifications: List<Notification> = emptyList(),
    val postPreviews: Map<String, Post> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val activeFilter: String = "all",
    val showPushBanner: Boolean = false,   // determined by real permission check in VM
    val error: String? = null,
    val info: String? = null,
    val pageSize: Int = PAGE_SIZE,
    val canLoadMore: Boolean = false,
    val undoBatch: List<Notification>? = null,
    val replyTarget: Notification? = null,
    val isSendingReply: Boolean = false,
    val lastOpenedPostId: String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val postRepository: PostRepository,
    @ApplicationContext private val appContext: Context
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
        viewModelScope.launch { notificationRepository.trackAnalytics("inbox_open") }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    fun load(showLoading: Boolean = true) {
        val uid = authRepository.currentUserId ?: run {
            _s.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        viewModelScope.launch {
            _s.update { it.copy(isLoading = showLoading) }
            val limit = _s.value.pageSize
            notificationRepository.getNotifications(uid, limit).fold(
                onSuccess = { list ->
                    // Never resurrect items that are waiting in the undo window.
                    val hidden = _s.value.undoBatch?.map { it.id }?.toSet().orEmpty()
                    val visible = if (hidden.isEmpty()) list else list.filter { it.id !in hidden }
                    _s.update {
                        it.copy(
                            notifications = visible,
                            isLoading = false,
                            isRefreshing = false,
                            canLoadMore = list.size >= limit,
                            error = null
                        )
                    }
                    loadPostPreviews(visible)
                },
                onFailure = { e ->
                    _s.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
                }
            )
        }
    }

    fun refresh() {
        _s.update { it.copy(isRefreshing = true) }
        load(showLoading = false)
    }

    fun loadMore() {
        if (!_s.value.canLoadMore) return
        _s.update { it.copy(pageSize = it.pageSize + PAGE_STEP) }
        load(showLoading = false)
    }

    private fun loadPostPreviews(notifications: List<Notification>) {
        // Bounded: only the most recent distinct posts that are not cached yet.
        val known = _s.value.postPreviews.keys
        val postIds = notifications
            .filter { it.type in PREVIEW_TYPES }
            .mapNotNull { it.relatedPostId }
            .distinct()
            .filter { it !in known }
            .take(30)
        if (postIds.isEmpty()) return
        viewModelScope.launch {
            val previews = coroutineScope {
                postIds.map { postId ->
                    async { postId to postRepository.getPostPreviewById(postId).getOrNull() }
                }.awaitAll()
            }.mapNotNull { (id, post) -> post?.let { id to it } }.toMap()
            _s.update { it.copy(postPreviews = it.postPreviews + previews) }
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
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true   // pre-T devices don't need runtime permission
        val show = !hasPermission && NotifPrefsHelper.consumeBannerVisibility(appContext)
        _s.update { it.copy(showPushBanner = show) }
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
        dismissPushBanner()
    }

    fun dismissPushBanner() {
        _s.update { it.copy(showPushBanner = false) }
        NotifPrefsHelper.markBannerDismissedNow(appContext)
        viewModelScope.launch {
            userPreferencesRepository.dismissNotifBanner(appContext)
        }
    }

    // ── Read / unread ─────────────────────────────────────────────────────────

    fun markAllRead() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.markAllRead(uid).fold(
                onSuccess = {
                    _s.update { st ->
                        st.copy(
                            notifications = st.notifications.map { it.copy(isRead = true) },
                            info = "All caught up ✨",
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _s.update { it.copy(error = "Couldn't mark notifications as read. Please try again.") }
                    android.util.Log.w("NotificationsVM", "markAllRead failed", error)
                }
            )
        }
    }

    fun markGroupRead(ids: List<String>) = setGroupRead(ids, true)

    fun markGroupUnread(ids: List<String>) = setGroupRead(ids, false)

    private fun setGroupRead(ids: List<String>, read: Boolean) {
        val uid = authRepository.currentUserId ?: return
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        // Optimistic: the UI updates instantly; the batch call reconciles on failure.
        _s.update { st ->
            st.copy(notifications = st.notifications.map { n ->
                if (n.id in idSet) n.copy(isRead = read) else n
            })
        }
        viewModelScope.launch {
            notificationRepository.setManyRead(uid, ids, read).onFailure { error ->
                android.util.Log.w("NotificationsVM", "setManyRead failed", error)
                _s.update { it.copy(error = "Couldn't update this notification. Please try again.") }
                load(showLoading = false)
            }
        }
    }

    // ── Delete with undo ──────────────────────────────────────────────────────

    fun deleteGroup(ids: List<String>) {
        commitDelete()   // flush any earlier batch still inside its undo window
        val idSet = ids.toSet()
        val removed = _s.value.notifications.filter { it.id in idSet }
        if (removed.isEmpty()) return
        _s.update { st ->
            st.copy(
                notifications = st.notifications.filterNot { it.id in idSet },
                undoBatch = removed
            )
        }
    }

    fun undoDelete() {
        val batch = _s.value.undoBatch ?: return
        _s.update { st ->
            st.copy(
                undoBatch = null,
                notifications = (st.notifications + batch).sortedByDescending { it.createdAt }
            )
        }
    }

    fun commitDelete() {
        val batch = _s.value.undoBatch ?: return
        val uid = authRepository.currentUserId ?: return
        _s.update { it.copy(undoBatch = null) }
        viewModelScope.launch {
            notificationRepository.deleteMany(uid, batch.map { it.id }).onFailure { error ->
                android.util.Log.w("NotificationsVM", "deleteMany failed", error)
                _s.update { st ->
                    st.copy(
                        error = "Couldn't delete this notification. Please try again.",
                        notifications = (st.notifications + batch).sortedByDescending { it.createdAt }
                    )
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        // The screen is gone: finish a pending delete so it isn't silently lost.
        val batch = _s.value.undoBatch
        val uid = authRepository.currentUserId
        if (batch != null && uid != null) {
            GlobalScope.launch { notificationRepository.deleteMany(uid, batch.map { it.id }) }
        }
        super.onCleared()
    }

    fun clearAll() {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            notificationRepository.deleteAll(uid).fold(
                onSuccess = {
                    _s.update {
                        it.copy(notifications = emptyList(), postPreviews = emptyMap(), undoBatch = null, error = null)
                    }
                },
                onFailure = { error ->
                    _s.update { it.copy(error = "Couldn't clear notifications. Please try again.") }
                    android.util.Log.w("NotificationsVM", "deleteAll failed", error)
                }
            )
        }
    }

    // ── Quick reply ───────────────────────────────────────────────────────────

    fun openReply(notification: Notification) = _s.update { it.copy(replyTarget = notification) }

    fun closeReply() = _s.update { it.copy(replyTarget = null) }

    fun sendReply(text: String) {
        val target = _s.value.replyTarget ?: return
        val uid = authRepository.currentUserId ?: return
        val postId = target.relatedPostId ?: return
        val body = text.trim()
        if (body.isEmpty() || _s.value.isSendingReply) return
        _s.update { it.copy(isSendingReply = true) }
        viewModelScope.launch {
            postRepository.addComment(
                postId = postId,
                body = body,
                isAnonymous = false,
                parentId = target.relatedCommentId
            ).fold(
                onSuccess = {
                    _s.update { it.copy(isSendingReply = false, replyTarget = null, info = "Reply sent 🎉") }
                    setGroupRead(listOf(target.id), true)
                },
                onFailure = { error ->
                    android.util.Log.w("NotificationsVM", "sendReply failed", error)
                    _s.update { it.copy(isSendingReply = false, error = "Couldn't send your reply. Please try again.") }
                }
            )
        }
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    fun onNotificationOpened(ids: List<String>, notification: Notification) {
        markGroupRead(ids)
        viewModelScope.launch {
            notificationRepository.trackAnalytics("notification_open", notification.type)
        }
        if (notification.type in COMMENT_TYPES || notification.type in REACTION_TYPES) {
            _s.update { it.copy(lastOpenedPostId = notification.relatedPostId) }
        }
    }

    fun dismissFollowUp() = _s.update { it.copy(lastOpenedPostId = null) }

    fun clearInfo() = _s.update { it.copy(info = null) }

    fun clearError() = _s.update { it.copy(error = null) }

    fun setFilter(f: String) = _s.update { it.copy(activeFilter = f) }
}

// ── Date / grouping helpers ───────────────────────────────────────────────────

private fun parseInstant(value: String): Instant? = try {
    Instant.parse(value)
} catch (_: Exception) {
    try { OffsetDateTime.parse(value).toInstant() } catch (_: Exception) { null }
}

private enum class Section(val label: String) {
    ATTENTION("Needs attention"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    WEEK("This week"),
    EARLIER("Earlier")
}

private data class NotifDisplay(
    val notification: Notification,      // newest member (display copy)
    val ids: List<String>,
    val members: List<Notification>,
    val names: List<String>,
    val avatars: List<String?>
) {
    val isGroup: Boolean get() = ids.size > 1
}

private fun groupKey(n: Notification): String? {
    val post = n.relatedPostId?.takeIf { it.isNotBlank() }
    return when (n.type) {
        "comment_like" -> n.relatedCommentId?.takeIf { it.isNotBlank() }?.let { "comment_like:$it" }
        "upvote", "like" -> post?.let { "${n.type}:$it" }
        "comment" -> post?.let { "comment:$it" }
        else -> null
    }
}

/**
 * Reactions and new comments on the same post/comment that arrive within a few hours are
 * merged into one row ("Rahul, Priya and 3 others liked your post"). Replies, mentions,
 * messages and safety/admin alerts always stay individual.
 */
private fun groupNotificationsForDisplay(notifications: List<Notification>): List<NotifDisplay> {
    val result = mutableListOf<NotifDisplay>()
    val used = mutableSetOf<Int>()
    notifications.forEachIndexed { index, notification ->
        if (index in used) return@forEachIndexed
        val key = groupKey(notification)
        val baseTime = parseInstant(notification.createdAt)
        val memberIndices = if (key == null || baseTime == null) listOf(index) else {
            notifications.indices.filter { i ->
                if (i in used) return@filter false
                val c = notifications[i]
                if (groupKey(c) != key) return@filter false
                val t = parseInstant(c.createdAt) ?: return@filter false
                kotlin.math.abs(Duration.between(baseTime, t).toMinutes()) <= GROUP_WINDOW_MIN
            }
        }
        memberIndices.forEach { used += it }
        val members = memberIndices.map { notifications[it] }.sortedByDescending { it.createdAt }
        val newest = members.first()
        val names = members.mapNotNull { it.senderUsername?.takeIf { n -> n.isNotBlank() } }.distinct()
        val avatars = members.map { it.senderAvatarUrl }.distinct().take(3)
        val display = if (members.size < 2) newest else newest.copy(isRead = members.all { it.isRead })
        result += NotifDisplay(display, members.map { it.id }, members, names, avatars)
    }
    return result.sortedByDescending { it.notification.createdAt }
}

private fun sectionOf(item: NotifDisplay, today: LocalDate, zone: ZoneId): Section {
    val n = item.notification
    if (!item.members.all { it.isRead } && n.type in URGENT_TYPES) return Section.ATTENTION
    val date = parseInstant(n.createdAt)?.atZone(zone)?.toLocalDate() ?: return Section.EARLIER
    return when {
        date == today -> Section.TODAY
        date == today.minusDays(1) -> Section.YESTERDAY
        date.isAfter(today.minusDays(7)) -> Section.WEEK
        else -> Section.EARLIER
    }
}

private fun priorityRank(item: NotifDisplay): Int =
    if (item.notification.type in HIGH_PRIORITY_TYPES) 0 else 1

// ── List model ────────────────────────────────────────────────────────────────

private sealed interface Entry { val key: String }
private data class HeaderEntry(val section: Section, val unread: Int) : Entry { override val key = "h_${section.name}" }
private data class RowEntry(val item: NotifDisplay, val first: Boolean, val last: Boolean) : Entry {
    override val key = "n_${item.ids.first()}"
}
private data class SummaryEntry(val reactions: Int, val comments: Int, val deltaPct: Int?) : Entry { override val key = "summary" }
private data class FollowUpEntry(val postId: String, val count: Int) : Entry { override val key = "followup" }
private object BannerEntry : Entry { override val key = "banner" }
private object LoadMoreEntry : Entry { override val key = "load_more" }

private fun buildWeeklySummary(all: List<Notification>): SummaryEntry? {
    val now = Instant.now()
    val weekAgo = now.minus(Duration.ofDays(7))
    val twoWeeksAgo = now.minus(Duration.ofDays(14))
    var rThis = 0; var cThis = 0; var prev = 0
    all.forEach { n ->
        val t = parseInstant(n.createdAt) ?: return@forEach
        val isReaction = n.type in REACTION_TYPES
        val isComment = n.type in COMMENT_TYPES
        if (!isReaction && !isComment) return@forEach
        when {
            t.isAfter(weekAgo) -> if (isReaction) rThis++ else cThis++
            t.isAfter(twoWeeksAgo) -> prev++
        }
    }
    val total = rThis + cThis
    if (total < 3) return null
    val delta = if (prev > 0 && total > prev) ((total - prev) * 100) / prev else null
    return SummaryEntry(rThis, cThis, delta)
}

private fun buildEntries(
    items: List<NotifDisplay>,
    filter: String,
    today: LocalDate,
    zone: ZoneId,
    showBanner: Boolean,
    summary: SummaryEntry?,
    followUp: FollowUpEntry?,
    canLoadMore: Boolean
): List<Entry> {
    val entries = mutableListOf<Entry>()
    if (filter == "all") {
        followUp?.let { entries += it }
        summary?.let { entries += it }
    }
    val grouped = items.groupBy { sectionOf(it, today, zone) }
    var rowsAdded = 0
    var bannerPlaced = !showBanner
    Section.values().forEach { section ->
        val sectionItems = grouped[section].orEmpty().let { list ->
            if (filter == "all") list.sortedWith(compareBy<NotifDisplay> { priorityRank(it) }
                .thenByDescending { it.notification.createdAt }) else list
        }
        if (sectionItems.isEmpty()) return@forEach
        entries += HeaderEntry(section, sectionItems.count { !it.notification.isRead })
        sectionItems.forEachIndexed { i, item ->
            entries += RowEntry(item, first = i == 0, last = i == sectionItems.lastIndex)
            rowsAdded++
            // Show the push prompt after the user has seen real content, not above it.
            if (!bannerPlaced && rowsAdded == 3) {
                entries += BannerEntry
                bannerPlaced = true
            }
        }
    }
    if (!bannerPlaced) entries += BannerEntry
    if (canLoadMore) entries += LoadMoreEntry
    return entries
}

// ── Type helpers ──────────────────────────────────────────────────────────────

private fun notificationTypeIcon(type: String): ImageVector = when (type.lowercase()) {
    "comment", "reply" -> Icons.Filled.ModeComment
    "mention" -> Icons.Filled.AlternateEmail
    "upvote", "like", "comment_like" -> Icons.Filled.ThumbUp
    "message" -> Icons.AutoMirrored.Filled.Message
    "alert", "emergency", "admin_warning" -> Icons.Filled.Warning
    "badge" -> Icons.Filled.Star
    "milestone", "streak" -> Icons.Filled.EmojiEvents
    "trending" -> Icons.Filled.TrendingUp
    "community" -> Icons.Filled.Groups
    "digest" -> Icons.Filled.Campaign
    "admin_suspension" -> Icons.Filled.GppBad
    "admin_ban" -> Icons.Filled.Block
    else -> Icons.Filled.Notifications
}

private fun notificationTypeLabel(type: String): String = when (type.lowercase()) {
    "comment" -> "New comment"
    "reply" -> "New reply"
    "mention" -> "Mention"
    "upvote", "like" -> "Upvote"
    "comment_like" -> "Comment like"
    "message" -> "Message"
    "alert", "emergency" -> "Alert"
    "badge" -> "Badge"
    "milestone" -> "Milestone"
    "streak" -> "Streak"
    "trending" -> "Trending"
    "community" -> "Community"
    "digest" -> "Digest"
    "admin_warning", "admin_suspension", "admin_ban" -> "Account notice"
    else -> "Notification"
}

@Composable
private fun typeColor(type: String): Color = when (type) {
    "comment", "reply"     -> OrangePrimary
    "mention"              -> BlueInfo
    "upvote", "like", "comment_like" -> GreenSuccess
    "message"              -> BlueInfo
    "alert", "emergency"   -> RedAlert
    "badge", "milestone", "streak" -> YellowWarn
    "trending"             -> OrangePrimary
    "community"            -> PurpleNight
    "digest"               -> GreenSuccess
    "admin_warning"        -> YellowWarn
    "admin_suspension"     -> OrangePrimary
    "admin_ban"            -> RedAlert
    else                   -> TertiaryText
}

private fun whoLabel(item: NotifDisplay): String {
    val count = item.members.size
    val shown = item.names.take(2)
    val extra = count - shown.size
    return when {
        shown.isEmpty() -> "$count people"
        extra <= 0 -> shown.joinToString(" and ")
        else -> shown.joinToString(", ") + " and $extra ${if (extra == 1) "other" else "others"}"
    }
}

private fun groupAction(type: String): String = when (type) {
    "comment_like" -> "liked your comment"
    "comment" -> "commented on your post"
    else -> "liked your post"
}

private fun compactCount(n: Int): String = if (n >= 1000) "${n / 100 / 10.0}k" else n.toString()

private data class NotifActions(
    val onTap: (NotifDisplay) -> Unit,
    val onSetRead: (List<String>, Boolean) -> Unit,
    val onDelete: (List<String>) -> Unit,
    val onReply: (Notification) -> Unit,
    val onEnablePush: () -> Unit,
    val onDismissPush: () -> Unit,
    val onOpenPushSettings: () -> Unit,
    val onOpenNotifSettings: () -> Unit,
    val onCreatePost: () -> Unit,
    val onLoadMore: () -> Unit,
    val onRefresh: () -> Unit,
    val onOpenFollowUp: (String) -> Unit,
    val onDismissFollowUp: () -> Unit
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun NotificationsScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val s = viewModel.state.collectAsState().value
    val unread = s.notifications.count { !it.isRead }
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()

    LaunchedEffect(s.error) {
        s.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(s.info) {
        s.info?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfo()
        }
    }
    // Undo window for swipe-to-delete
    LaunchedEffect(s.undoBatch) {
        if (s.undoBatch != null) {
            val result = snackbarHostState.showSnackbar(
                message = "Notification removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.commitDelete()
        }
    }

    // Permission launcher wired to the VM's event
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(Unit) {
        viewModel.requestPermissionEvent.collect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onPermissionResult(true)
            }
        }
    }

    val pagerState = rememberPagerState { FILTER_TABS.size }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { viewModel.setFilter(FILTER_TABS[it]) }
    }
    val tabListState = rememberLazyListState()
    LaunchedEffect(pagerState.currentPage) { tabListState.animateScrollToItem(pagerState.currentPage) }

    val unreadByFilter = remember(s.notifications) {
        FILTER_TABS.associateWith { f -> filterNotifs(s.notifications, f).count { !it.isRead } }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val openPushSettings = {
        ctx.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            }
        )
    }

    // One shared tap handler for every row (previously duplicated per section)
    val onNotificationTap: (NotifDisplay) -> Unit = { item ->
        val notif = item.notification
        viewModel.onNotificationOpened(item.ids, notif)
        val conversationId = notif.relatedConversationId
        val postId = notif.relatedPostId
        if (conversationId != null) {
            navController.navigate(Screen.Chat.createRoute(conversationId))
        } else if (postId != null) {
            val commentId = notif.relatedCommentId
            if (!commentId.isNullOrBlank()) {
                navController.navigate(Screen.Thread.createRoute(postId, commentId))
            } else {
                onPostClick(postId)
            }
        } else if (notif.type in listOf("alert", "emergency", "admin_warning")) {
            navController.navigate(Screen.Alerts.route)
        }
    }

    val actions = NotifActions(
        onTap = onNotificationTap,
        onSetRead = { ids, read ->
            if (read) viewModel.markGroupRead(ids) else viewModel.markGroupUnread(ids)
        },
        onDelete = { ids -> viewModel.deleteGroup(ids) },
        onReply = { viewModel.openReply(it) },
        onEnablePush = { viewModel.onEnablePushClicked() },
        onDismissPush = { viewModel.dismissPushBanner() },
        onOpenPushSettings = { openPushSettings() },
        onOpenNotifSettings = { navController.navigate(Screen.NotifSettings.route) },
        onCreatePost = { navController.navigate(Screen.CreatePost.createRoute()) },
        onLoadMore = { viewModel.loadMore() },
        onRefresh = { viewModel.refresh() },
        onOpenFollowUp = { postId ->
            viewModel.dismissFollowUp()
            onPostClick(postId)
        },
        onDismissFollowUp = { viewModel.dismissFollowUp() }
    )

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PrimaryText)
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
                        AnimatedVisibility(visible = unread > 0, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                "$unread unread",
                                color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    AnimatedVisibility(visible = unread > 0, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = {
                            haptic.success()
                            viewModel.markAllRead()
                        }) {
                            Icon(Icons.Filled.DoneAll, "Mark all as read", tint = OrangePrimary)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, "More options", tint = SecondaryText)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Notification settings") },
                                leadingIcon = { Icon(Icons.Filled.Settings, null) },
                                onClick = { menuOpen = false; actions.onOpenNotifSettings() }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear all") },
                                leadingIcon = { Icon(Icons.Filled.DeleteSweep, null) },
                                enabled = s.notifications.isNotEmpty(),
                                onClick = { menuOpen = false; confirmClear = true }
                            )
                        }
                    }
                }

                // Filter tabs with unread badges; fades out at the right edge to hint scrolling
                Box(Modifier.fillMaxWidth()) {
                    LazyRow(
                        state = tabListState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(FILTER_TABS) { index, f ->
                            NotifFilterChip(
                                label = filterLabel(f),
                                selected = pagerState.currentPage == index,
                                unread = if (f == "all") 0 else unreadByFilter[f] ?: 0,
                                onClick = {
                                    haptic.tap()
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                            )
                        }
                    }
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(28.dp)
                            .height(48.dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, Background)))
                    )
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { pad ->
        if (s.isLoading) {
            NotifSkeletonList(Modifier.fillMaxWidth().padding(pad))
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(pad),
                key = { FILTER_TABS[it] }
            ) { page ->
                NotifPage(
                    filter = FILTER_TABS[page],
                    s = s,
                    actions = actions
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all notifications?") },
            text = { Text("This permanently removes every notification. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearAll() }) {
                    Text("Clear all", color = RedAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            }
        )
    }

    s.replyTarget?.let { target ->
        QuickReplySheet(
            target = target,
            isSending = s.isSendingReply,
            onDismiss = { viewModel.closeReply() },
            onSend = { viewModel.sendReply(it) }
        )
    }
}

// ── One pager page (one filter) ───────────────────────────────────────────────

@Composable
private fun NotifPage(
    filter: String,
    s: NotifUiState,
    actions: NotifActions
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val filtered = remember(s.notifications, filter) { filterNotifs(s.notifications, filter) }
    val items = remember(filtered) { groupNotificationsForDisplay(filtered) }
    val summary = remember(s.notifications) { buildWeeklySummary(s.notifications) }
    val followUp = remember(s.notifications, s.lastOpenedPostId) {
        val postId = s.lastOpenedPostId
        if (postId == null) null else {
            val count = s.notifications.count { !it.isRead && it.type in COMMENT_TYPES && it.relatedPostId == postId }
            if (count > 0) FollowUpEntry(postId, count) else null
        }
    }
    val entries = remember(items, filter, s.showPushBanner, summary, followUp, s.canLoadMore) {
        buildEntries(items, filter, today, zone, s.showPushBanner, summary, followUp, s.canLoadMore)
    }

    PullToRefreshBox(
        isRefreshing = s.isRefreshing,
        onRefresh = actions.onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        if (items.isEmpty()) {
            LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(
                        Modifier.fillParentMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (s.showPushBanner) {
                            PushEnableBanner(actions.onEnablePush, actions.onDismissPush, actions.onOpenPushSettings)
                            Spacer(Modifier.height(8.dp))
                        }
                        Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                            EmptyContent(filter, actions.onCreatePost)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = rememberLazyListState(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                entries.forEach { entry ->
                    when (entry) {
                        is HeaderEntry -> stickyHeader(key = entry.key) {
                            SectionHeader(entry, Modifier.animateItem())
                        }
                        is RowEntry -> item(key = entry.key) {
                            val preview = entry.item.notification.relatedPostId?.let { s.postPreviews[it] }
                            val shape = RoundedCornerShape(
                                topStart = if (entry.first) 20.dp else 0.dp,
                                topEnd = if (entry.first) 20.dp else 0.dp,
                                bottomStart = if (entry.last) 20.dp else 0.dp,
                                bottomEnd = if (entry.last) 20.dp else 0.dp
                            )
                            Column(
                                Modifier
                                    .animateItem()
                                    .padding(horizontal = 14.dp)
                                    .clip(shape)
                                    .background(Surface)
                            ) {
                                SwipeableNotifRow(entry.item, preview, actions)
                                if (!entry.last) {
                                    HorizontalDivider(
                                        color = Divider.copy(alpha = 0.85f),
                                        thickness = 0.75.dp,
                                        modifier = Modifier.padding(start = 70.dp, end = 12.dp)
                                    )
                                }
                            }
                        }
                        is SummaryEntry -> item(key = entry.key) {
                            WeeklySummaryCard(entry, Modifier.animateItem())
                        }
                        is FollowUpEntry -> item(key = entry.key) {
                            FollowUpCard(
                                entry,
                                onOpen = { actions.onOpenFollowUp(entry.postId) },
                                onDismiss = actions.onDismissFollowUp,
                                modifier = Modifier.animateItem()
                            )
                        }
                        BannerEntry -> item(key = entry.key) {
                            Column(Modifier.animateItem().padding(vertical = 8.dp)) {
                                PushEnableBanner(actions.onEnablePush, actions.onDismissPush, actions.onOpenPushSettings)
                            }
                        }
                        LoadMoreEntry -> item(key = entry.key) {
                            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                                TextButton(onClick = actions.onLoadMore) {
                                    Text("Load earlier notifications", color = OrangePrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(filter: String, onCreatePost: () -> Unit) {
    val (title, subtitle) = when (filter) {
        "all" -> "You're all caught up" to "Nagpur's quiet right now. Share something about your area and get the conversation going."
        "replies" -> "No replies yet" to "Ask a question or share a tip to start a conversation."
        "mentions" -> "No mentions yet" to "When someone tags you with @, it shows up here."
        "upvotes" -> "No upvotes yet" to "Post something useful for your neighbourhood and watch the upvotes roll in."
        "messages" -> "No messages yet" to "Private messages from other Nagpurians will land here."
        else -> "No alerts right now" to "Milestones, trending posts and city alerts will appear here."
    }
    EmptyState(
        icon = Icons.Filled.NotificationsNone,
        title = title,
        subtitle = subtitle,
        ctaLabel = "Create a post",
        onCta = onCreatePost
    )
}

// ── Header / chips ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(entry: HeaderEntry, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            entry.section.label,
            color = if (entry.section == Section.ATTENTION) RedAlert else PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (entry.unread > 0) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(if (entry.section == Section.ATTENTION) RedAlert else OrangePrimary)
            )
        }
    }
}

@Composable
private fun NotifFilterChip(label: String, selected: Boolean, unread: Int, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) OrangePrimary else SurfaceAlt, tween(220), label = "chip_bg")
    val fg by animateColorAsState(if (selected) Color.White else SecondaryText, tween(220), label = "chip_fg")
    Box(Modifier.minimumInteractiveComponentSize()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(bg)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = fg,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp
            )
            if (unread > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    if (unread > 99) "99+" else unread.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.28f) else OrangePrimary)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

// ── Cards: weekly summary + follow-up ─────────────────────────────────────────

@Composable
private fun WeeklySummaryCard(entry: SummaryEntry, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(OrangePrimary.copy(alpha = 0.18f), OrangeSubtle)))
            .border(1.dp, OrangePrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(OrangePrimary.copy(alpha = 0.2f)),
            Alignment.Center
        ) {
            Icon(Icons.Filled.TrendingUp, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("This week on your posts", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStat(Icons.Filled.ThumbUp, entry.reactions, "reactions")
                SummaryStat(Icons.Filled.ModeComment, entry.comments, "comments")
            }
        }
        entry.deltaPct?.let { pct ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenSubtle)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ArrowUpward, null, tint = GreenSuccess, modifier = Modifier.size(12.dp))
                Text("$pct%", color = GreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryStat(icon: ImageVector, value: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SecondaryText, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text("$value $label", color = SecondaryText, fontSize = 12.sp)
    }
}

@Composable
private fun FollowUpCard(entry: FollowUpEntry, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ModeComment, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "${entry.count} more new ${if (entry.count == 1) "reply" else "replies"} on this post",
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text("View", color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, "Dismiss", tint = TertiaryText, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Swipeable row ─────────────────────────────────────────────────────────────

@Composable
private fun SwipeableNotifRow(item: NotifDisplay, postPreview: Post?, actions: NotifActions) {
    val notif = item.notification
    val isUnread = !item.members.all { it.isRead }
    val isAdmin = notif.type in ADMIN_TYPES
    val currentItem by rememberUpdatedState(item)
    val currentActions by rememberUpdatedState(actions)
    val haptic = rememberHaptic()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.tap()
                    currentActions.onSetRead(currentItem.ids, true)
                    false   // snap back; the row just turns "read"
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.alert()
                    currentActions.onDelete(currentItem.ids)
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = isUnread,
        enableDismissFromEndToStart = !isAdmin,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bg = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> GreenSuccess
                SwipeToDismissBoxValue.EndToStart -> RedAlert
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().background(bg).padding(horizontal = 22.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction != SwipeToDismissBoxValue.Settled) {
                    Icon(
                        if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Filled.DoneAll else Icons.Filled.Delete,
                        contentDescription = if (direction == SwipeToDismissBoxValue.StartToEnd) "Mark as read" else "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        NotifRowContent(item, postPreview, actions)
    }
}

// ── Row content ───────────────────────────────────────────────────────────────

private val celebratedIds = mutableSetOf<String>()

@Composable
private fun NotifRowContent(item: NotifDisplay, postPreview: Post?, actions: NotifActions) {
    val notif = item.notification
    val isUnread = !item.members.all { it.isRead }
    val accent = typeColor(notif.type)
    val isMilestone = notif.type == "milestone"
    val haptic = rememberHaptic()
    var menuOpen by remember { mutableStateOf(false) }

    val tint by animateFloatAsState(if (isUnread) 1f else 0f, tween(400), label = "unread_tint")
    val dotPulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (isUnread) {
            dotPulse.animateTo(1.7f, tween(260))
            dotPulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val celebrate = remember(notif.id) { isMilestone && isUnread && celebratedIds.add(notif.id) }

    val bodyColor = if (isUnread) PrimaryText else SecondaryText
    val headline: AnnotatedString = buildAnnotatedString {
        if (item.isGroup) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryText)) { append(whoLabel(item)) }
            withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = bodyColor)) {
                append(" ${groupAction(notif.type)}")
            }
        } else {
            val name = notif.senderUsername?.takeIf { it.isNotBlank() && notif.title.startsWith(it) }
            if (name != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = PrimaryText)) { append(name) }
                withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = bodyColor)) {
                    append(notif.title.removePrefix(name))
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontWeight = if (isUnread || isMilestone) FontWeight.SemiBold else FontWeight.Normal,
                        color = bodyColor
                    )
                ) { append(notif.title) }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .then(
                        if (isMilestone) Modifier.background(
                            Brush.horizontalGradient(listOf(YellowWarn.copy(alpha = 0.16f), OrangePrimary.copy(alpha = 0.06f)))
                        ) else Modifier
                    )
                    .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.11f * tint), Color.Transparent)))
                    .drawBehind {
                        drawRect(accent.copy(alpha = tint), size = Size(3.dp.toPx(), size.height))
                    }
                    .combinedClickable(
                        onClick = { haptic.tap(); actions.onTap(item) },
                        onLongClick = { haptic.alert(); menuOpen = true }
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                AvatarCluster(
                    urls = item.avatars,
                    names = item.names,
                    accent = accent,
                    type = notif.type,
                    compact = compact
                )

                Spacer(Modifier.width(if (compact) 8.dp else 12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        headline,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!notif.body.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(accent.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                notif.body.orEmpty(),
                                color = SecondaryText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val time = notif.timeLabel()
                        if (time.isNotEmpty()) Text(time, color = TertiaryText, fontSize = 11.sp)
                        val views = postPreview?.viewCount ?: 0
                        val showReach = views >= 10 &&
                            (notif.type in REACTION_TYPES || notif.type in COMMENT_TYPES || isMilestone)
                        if (showReach) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Visibility, null, tint = TertiaryText, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Seen by ${compactCount(views)} Nagpurians", color = TertiaryText, fontSize = 11.sp)
                            }
                        }
                    }
                    if (!item.isGroup && notif.type in COMMENT_TYPES && notif.relatedPostId != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(OrangeSubtle)
                                .border(1.dp, OrangePrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .clickable { haptic.tap(); actions.onReply(notif) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reply", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.width(if (compact) 6.dp else 10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Box(Modifier.size(10.dp), Alignment.Center) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isUnread,
                            exit = fadeOut(tween(300)) + scaleOut(tween(300))
                        ) {
                            Box(
                                Modifier
                                    .size((8f * dotPulse.value).dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary)
                            )
                        }
                    }
                    if (notif.relatedPostId != null) {
                        Spacer(Modifier.height(4.dp))
                        PostThumb(postPreview, accent, compact)
                    }
                }
            }

            if (celebrate) {
                ConfettiBurst(Modifier.matchParentSize())
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (isUnread) "Mark as read" else "Mark as unread") },
                    leadingIcon = { Icon(if (isUnread) Icons.Filled.DoneAll else Icons.Filled.MarkEmailUnread, null) },
                    onClick = { menuOpen = false; actions.onSetRead(item.ids, isUnread.not()) }
                )
                if (notif.type !in ADMIN_TYPES) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = RedAlert) },
                        onClick = { menuOpen = false; actions.onDelete(item.ids) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Notification settings") },
                    leadingIcon = { Icon(Icons.Filled.Settings, null) },
                    onClick = { menuOpen = false; actions.onOpenNotifSettings() }
                )
            }
        }
    }
}

@Composable
private fun PostThumb(postPreview: Post?, accent: Color, compact: Boolean) {
    val size = if (compact) 38.dp else 50.dp
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.12f)))),
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
            val initial = postPreview?.title?.trim()?.firstOrNull()?.uppercase()
            if (initial != null) {
                Text(initial, color = accent, fontWeight = FontWeight.Black, fontSize = if (compact) 16.sp else 20.sp)
            } else {
                Icon(Icons.Filled.Description, null, tint = accent, modifier = Modifier.size(if (compact) 16.dp else 20.dp))
            }
        }
    }
}

@Composable
private fun AvatarCluster(urls: List<String?>, names: List<String>, accent: Color, type: String, compact: Boolean) {
    val stacked = urls.size > 1
    val single = if (compact) 40.dp else 46.dp
    val small = if (compact) 30.dp else 34.dp
    val step = if (compact) 12.dp else 14.dp
    val d = if (stacked) small else single
    val clusterWidth = if (stacked) d + step * (urls.size - 1) else d

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(Modifier.size(width = clusterWidth, height = d)) {
            val list = if (urls.isEmpty()) listOf<String?>(null) else urls
            for (i in list.indices.reversed()) {
                val url = list[i]
                Box(
                    modifier = Modifier
                        .offset(x = step * i)
                        .size(d)
                        .clip(CircleShape)
                        .then(if (stacked) Modifier.border(2.dp, Surface, CircleShape) else Modifier)
                        .background(SurfaceAlt),
                    contentAlignment = Alignment.Center
                ) {
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                            contentDescription = names.getOrNull(i)?.let { "$it's profile photo" },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PersonOutline,
                            contentDescription = null,
                            tint = SecondaryText,
                            modifier = Modifier.size(if (stacked) 16.dp else 22.dp)
                        )
                    }
                }
            }
        }
        // Type badge: solid accent with a surface-coloured ring so it separates from the photo
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent)
                .border(1.5.dp, Surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notificationTypeIcon(type),
                contentDescription = notificationTypeLabel(type),
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ── Confetti (milestones) ─────────────────────────────────────────────────────

@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1400, easing = LinearOutSlowInEasing)) }
    val palette = listOf(OrangePrimary, YellowWarn, GreenSuccess, BlueInfo, PinkEvents)
    Canvas(modifier) {
        val p = progress.value
        if (p < 1f) {
            for (i in 0 until 18) {
                val angle = (i * 20.0 + (i % 3) * 7.0) * Math.PI / 180.0
                val dist = (40f + (i % 5) * 18f) * p * density
                val cx = size.width * 0.14f + (cos(angle) * dist).toFloat()
                val cy = size.height * 0.5f + (sin(angle) * dist).toFloat() + 30f * p * p * density
                drawCircle(
                    color = palette[i % palette.size].copy(alpha = 1f - p),
                    radius = (2f + (i % 3)) * density,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

@Composable
private fun NotifSkeletonList(modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
    ) {
        repeat(7) { i ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
                Box(Modifier.size(46.dp).shimmerEffect(CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Box(Modifier.fillMaxWidth(0.85f).height(14.dp).shimmerEffect())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.55f).height(12.dp).shimmerEffect())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(40.dp).height(10.dp).shimmerEffect())
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(50.dp).shimmerEffect(RoundedCornerShape(12.dp)))
            }
            if (i < 6) HorizontalDivider(color = Divider.copy(alpha = 0.85f), thickness = 0.75.dp, modifier = Modifier.padding(start = 70.dp, end = 12.dp))
        }
    }
}

// ── Quick reply sheet ─────────────────────────────────────────────────────────

@Composable
private fun QuickReplySheet(
    target: Notification,
    isSending: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(300)
        runCatching { focusRequester.requestFocus() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("Quick reply", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(OrangePrimary.copy(alpha = 0.6f)))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(target.title, color = SecondaryText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!target.body.isNullOrBlank()) {
                        Text(target.body.orEmpty(), color = TertiaryText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text("Write a reply…", color = TertiaryText) },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedBorderColor = OrangePrimary,
                        cursorColor = OrangePrimary
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onSend(text) },
                    enabled = text.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (text.isNotBlank() && !isSending) OrangePrimary else SurfaceAlt)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send reply", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ── Push enable banner — real permission flow ─────────────────────────────────

@Composable
private fun PushEnableBanner(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Surface)
                .border(1.dp, OrangePrimary.copy(0.25f), RoundedCornerShape(18.dp))
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
                Icon(Icons.Filled.Notifications, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Never miss a reply, Nagpur!",
                    color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Get notified the moment someone replies to you or your post takes off.",
                    color = SecondaryText, fontSize = 12.sp, lineHeight = 16.sp,
                    maxLines = if (compact) 3 else 4, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Manage in Settings →",
                    color = OrangePrimary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { onOpenSettings() }
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(OrangePrimary)
                        .pressScale(onClick = onEnable)
                        .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = 8.dp)
                ) {
                    Text("Enable", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Dismiss", tint = TertiaryText, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
