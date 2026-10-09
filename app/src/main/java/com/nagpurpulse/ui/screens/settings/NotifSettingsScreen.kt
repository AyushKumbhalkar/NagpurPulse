// ui/screens/settings/NotifSettingsScreen.kt  — REPLACE entirely
package com.nagpurpulse.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.nagpurpulse.data.model.UserPreferences
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.notifications.ScheduledPushManager
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class NotifSettingsState(
    val pushEnabled:    Boolean = true,
    val notifReplies:   Boolean = true,
    val notifMentions:  Boolean = true,
    val notifMessages:  Boolean = true,
    val notifUpvotes:   Boolean = true,
    val notifDigest:    Boolean = true,
    val notifTrending:  Boolean = true,
    val notifCommunity: Boolean = true,
    val notifAlerts:    Boolean = true,
    // Device-local extras: delivery times, pause and quiet hours (minutes after midnight)
    val morningMin:     Int     = 8 * 60,
    val trendingMin:    Int     = 13 * 60,
    val eveningMin:     Int     = 18 * 60,
    val nightMin:       Int     = 22 * 60,
    val pausedUntil:    Long    = 0L,
    val quietEnabled:   Boolean = false,
    val quietStart:     Int     = 23 * 60,
    val quietEnd:       Int     = 7 * 60,
    val isLoading:      Boolean = true,
    val errorMessage:   String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NotifSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val appContext: Context   // ← injected, not from composable
) : ViewModel() {

    private val _state = MutableStateFlow(NotifSettingsState())
    val state: StateFlow<NotifSettingsState> = _state

    init { loadSettings() }

    fun dismissError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private var lastPersistedState: NotifSettingsState? = null
    private val saveMutex = kotlinx.coroutines.sync.Mutex()

    private fun loadSettings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val prefs = userPreferencesRepository.loadAndSyncNotifPrefs(appContext)
            val loaded = if (prefs != null) {
                NotifSettingsState(
                    pushEnabled = prefs.notifPush,
                    notifReplies = prefs.notifReplies,
                    notifMentions = prefs.notifMentions,
                    notifMessages = prefs.notifMessages,
                    notifUpvotes = prefs.notifUpvotes,
                    notifDigest = prefs.notifDigest,
                    notifTrending = prefs.notifTrending,
                    notifCommunity = prefs.notifCommunity,
                    notifAlerts = prefs.notifAlertsSummary,
                    isLoading = false
                )
            } else {
                NotifSettingsState(
                    pushEnabled = NotifPrefsHelper.isPushEnabled(appContext),
                    notifReplies = NotifPrefsHelper.isRepliesEnabled(appContext),
                    notifMentions = NotifPrefsHelper.isMentionsEnabled(appContext),
                    notifMessages = NotifPrefsHelper.isMessagesEnabled(appContext),
                    notifUpvotes = NotifPrefsHelper.isUpvotesEnabled(appContext),
                    notifDigest = NotifPrefsHelper.isDigestEnabled(appContext),
                    notifTrending = NotifPrefsHelper.isTrendingEnabled(appContext),
                    notifCommunity = NotifPrefsHelper.isCommunityEnabled(appContext),
                    notifAlerts = NotifPrefsHelper.isAlertsSummaryEnabled(appContext),
                    isLoading = false
                )
            }
            val withLocal = loaded.withLocalPrefs(appContext)
            _state.value = withLocal
            lastPersistedState = withLocal
        }
    }

    private fun fieldValue(state: NotifSettingsState, field: String): Boolean? = when (field) {
        "notif_push" -> state.pushEnabled
        "notif_replies" -> state.notifReplies
        "notif_mentions" -> state.notifMentions
        "notif_messages" -> state.notifMessages
        "notif_upvotes" -> state.notifUpvotes
        "notif_digest" -> state.notifDigest
        "notif_trending" -> state.notifTrending
        "notif_community" -> state.notifCommunity
        "notif_alerts_summary" -> state.notifAlerts
        else -> null
    }

    private fun withField(
        state: NotifSettingsState,
        field: String,
        value: Boolean,
        errorMessage: String? = state.errorMessage
    ): NotifSettingsState = when (field) {
        "notif_push" -> state.copy(pushEnabled = value, errorMessage = errorMessage)
        "notif_replies" -> state.copy(notifReplies = value, errorMessage = errorMessage)
        "notif_mentions" -> state.copy(notifMentions = value, errorMessage = errorMessage)
        "notif_messages" -> state.copy(notifMessages = value, errorMessage = errorMessage)
        "notif_upvotes" -> state.copy(notifUpvotes = value, errorMessage = errorMessage)
        "notif_digest" -> state.copy(notifDigest = value, errorMessage = errorMessage)
        "notif_trending" -> state.copy(notifTrending = value, errorMessage = errorMessage)
        "notif_community" -> state.copy(notifCommunity = value, errorMessage = errorMessage)
        "notif_alerts_summary" -> state.copy(notifAlerts = value, errorMessage = errorMessage)
        else -> state
    }

    fun toggle(field: String, value: Boolean) {
        val previous = _state.value
        val previousValue = fieldValue(previous, field) ?: return
        _state.value = withField(previous, field, value, errorMessage = null)

        viewModelScope.launch {
            saveMutex.withLock {
                val saveResult = userPreferencesRepository.saveNotifPref(appContext, field, value)
                if (saveResult.isFailure) {
                    val current = _state.value
                    val persisted = lastPersistedState ?: previous
                    _state.value = if (fieldValue(current, field) == value) {
                        withField(
                            current,
                            field,
                            fieldValue(persisted, field) ?: previousValue,
                            errorMessage = "Couldn't save notification settings. Your last saved value was restored."
                        )
                    } else {
                        current.copy(errorMessage = "A notification change couldn't be saved. Please try again.")
                    }
                    if (field in SCHEDULED_FIELDS) ScheduledPushManager.reschedule(appContext)
                    return@withLock
                }

                lastPersistedState = withField(lastPersistedState ?: previous, field, value, errorMessage = null)
                // A later successful save supersedes an earlier failed rapid toggle.
                // Clear its stale error so the UI reflects the final persisted state.
                _state.value = _state.value.copy(errorMessage = null)
                if (field in SCHEDULED_FIELDS) ScheduledPushManager.reschedule(appContext)

                if (!value) {
                    val notificationType = when (field) {
                        "notif_replies" -> "reply"
                        "notif_mentions" -> "mention"
                        "notif_messages" -> "message"
                        "notif_upvotes" -> "upvote"
                        "notif_digest" -> "digest"
                        "notif_trending" -> "trending"
                        "notif_community" -> "community"
                        "notif_alerts_summary" -> "alert"
                        else -> "all"
                    }
                    notificationRepository.trackAnalytics("push_opt_out", notificationType)
                }
            }
        }
    }

    // ── Device-local controls (no backend round-trip) ──────────────────────────

    fun setScheduledTime(slot: String, minutes: Int) {
        NotifPrefsHelper.setScheduledMinutes(appContext, slot, minutes)
        _state.value = _state.value.withLocalPrefs(appContext)
        ScheduledPushManager.reschedule(appContext)
    }

    /** hours = 0 resumes notifications. */
    fun pauseFor(hours: Int) {
        NotifPrefsHelper.pauseFor(appContext, hours)
        _state.value = _state.value.withLocalPrefs(appContext)
    }

    fun setQuietHours(enabled: Boolean, startMinutes: Int, endMinutes: Int) {
        NotifPrefsHelper.setQuietHours(appContext, enabled, startMinutes, endMinutes)
        _state.value = _state.value.withLocalPrefs(appContext)
    }

    private companion object {
        val SCHEDULED_FIELDS = setOf(
            "notif_push", "notif_digest", "notif_trending", "notif_community", "notif_alerts_summary"
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun NotifSettingsScreen(
    navController: NavController,
    vm: NotifSettingsViewModel = hiltViewModel()
) {
    val ctx   = LocalContext.current
    val state by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissError()
        }
    }

    val hasPermission = remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission.value = granted
        if (granted) vm.toggle("notif_push", true)
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsTopBar(
                title = "Notifications",
                subtitle = "Stay in the loop, on your terms",
                onBack = { navController.popBackStack() }
            )
        }
    ) { pad ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── OS permission banner ──────────────────────────────────────────
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission.value) {
                item {
                    NotifPermissionBanner(
                        onAllow = {
                            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        onSettings = {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                }
                            )
                        }
                    )
                }
            }

            item { NotifSummaryCard(state, hasPermission.value) }

            // ── Master push toggle ────────────────────────────────────────────
            item { SectionHeader("PUSH NOTIFICATIONS") }
            item {
                SettingsGroup {
                    NotifToggleRow(
                        label   = "Push Notifications",
                        sub     = if (hasPermission.value) "Master switch for all push alerts"
                                  else "Grant OS permission first",
                        icon    = Icons.Filled.Notifications,
                        color   = OrangePrimary,
                        checked = state.pushEnabled && hasPermission.value,
                        enabled = hasPermission.value
                    ) { on ->
                        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission.value) {
                            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            vm.toggle("notif_push", on)
                            if (!on) ScheduledPushManager.cancelAll(ctx)
                        }
                    }
                }
            }

            // ── Activity ──────────────────────────────────────────────────────
            item { SectionHeader("ACTIVITY") }
            item {
                SettingsGroup {
                    NotifToggleRow("Comment Replies",  "When someone comments on your post",        Icons.AutoMirrored.Filled.Reply,   BlueInfo,     state.notifReplies,   state.pushEnabled && hasPermission.value) { vm.toggle("notif_replies",  it) }
                    SettingsDivider()
                    NotifToggleRow("Mentions",         "When someone @mentions you",                 Icons.Filled.AlternateEmail,       PurpleNight,  state.notifMentions,  state.pushEnabled && hasPermission.value) { vm.toggle("notif_mentions", it) }
                    SettingsDivider()
                    NotifToggleRow("Direct Messages",  "New messages from other users",              Icons.AutoMirrored.Filled.Message,  GreenSuccess, state.notifMessages,  state.pushEnabled && hasPermission.value) { vm.toggle("notif_messages", it) }
                    SettingsDivider()
                    NotifToggleRow("Upvotes",          "When your posts or comments get upvoted",    Icons.Filled.ThumbUp,              OrangePrimary, state.notifUpvotes,  state.pushEnabled && hasPermission.value) { vm.toggle("notif_upvotes",  it) }
                }
            }

            // ── Scheduled ─────────────────────────────────────────────────────
            item { SectionHeader("SCHEDULED UPDATES") }
            item {
                SettingsGroup {
                    NotifToggleRow("Morning Digest",    "Top posts from Nagpur at ${formatClock(state.morningMin)}",         Icons.Filled.WbSunny,            YellowWarn,   state.notifDigest,    state.pushEnabled && hasPermission.value) { vm.toggle("notif_digest",          it) }
                    SettingsDivider()
                    NotifToggleRow("Trending Alert",    "What's hot in Nagpur at ${formatClock(state.trendingMin)}",          Icons.Filled.Whatshot,           OrangePrimary, state.notifTrending, state.pushEnabled && hasPermission.value) { vm.toggle("notif_trending",        it) }
                    SettingsDivider()
                    NotifToggleRow("Evening Community", "City update at ${formatClock(state.eveningMin)}",                   Icons.Filled.NightsStay,         PurpleNight,  state.notifCommunity, state.pushEnabled && hasPermission.value) { vm.toggle("notif_community",       it) }
                    SettingsDivider()
                    NotifToggleRow("Night Alert Summary","Active alert summary at ${formatClock(state.nightMin)}",        Icons.Filled.NotificationsActive, RedAlert,    state.notifAlerts,    state.pushEnabled && hasPermission.value) { vm.toggle("notif_alerts_summary",  it) }
                }
            }

            // ── Pause & quiet hours ───────────────────────────────────────────
            item { SectionHeader("PAUSE & QUIET HOURS") }
            item {
                SettingsGroup {
                    PauseRow(state.pausedUntil) { hours -> vm.pauseFor(hours) }
                    SettingsDivider()
                    NotifToggleRow(
                        label   = "Quiet hours",
                        sub     = if (state.quietEnabled)
                            "Silent from ${formatClock(state.quietStart)} to ${formatClock(state.quietEnd)}"
                        else "Mute pushes overnight so your sleep stays yours",
                        icon    = Icons.Filled.Bedtime,
                        color   = PurpleNight,
                        checked = state.quietEnabled
                    ) { on -> vm.setQuietHours(on, state.quietStart, state.quietEnd) }
                    if (state.quietEnabled) {
                        SettingsDivider()
                        SettingsRowAction("Starts at", "Quiet hours begin", Icons.Filled.Schedule, PurpleNight, badge = formatClock(state.quietStart)) {
                            pickTime(ctx, state.quietStart) { vm.setQuietHours(true, it, state.quietEnd) }
                        }
                        SettingsDivider()
                        SettingsRowAction("Ends at", "Notifications resume", Icons.Filled.Schedule, PurpleNight, badge = formatClock(state.quietEnd)) {
                            pickTime(ctx, state.quietEnd) { vm.setQuietHours(true, state.quietStart, it) }
                        }
                    }
                }
            }

            // ── Delivery times ────────────────────────────────────────────────
            item { SectionHeader("DELIVERY TIMES") }
            item {
                SettingsGroup {
                    SettingsRowAction("Morning Digest", "Tap to choose when it arrives", Icons.Filled.WbSunny, YellowWarn, badge = formatClock(state.morningMin)) {
                        pickTime(ctx, state.morningMin) { vm.setScheduledTime(NotifPrefsHelper.SLOT_MORNING, it) }
                    }
                    SettingsDivider()
                    SettingsRowAction("Trending Alert", "Tap to choose when it arrives", Icons.Filled.Whatshot, OrangePrimary, badge = formatClock(state.trendingMin)) {
                        pickTime(ctx, state.trendingMin) { vm.setScheduledTime(NotifPrefsHelper.SLOT_TRENDING, it) }
                    }
                    SettingsDivider()
                    SettingsRowAction("Evening Community", "Tap to choose when it arrives", Icons.Filled.NightsStay, PurpleNight, badge = formatClock(state.eveningMin)) {
                        pickTime(ctx, state.eveningMin) { vm.setScheduledTime(NotifPrefsHelper.SLOT_EVENING, it) }
                    }
                    SettingsDivider()
                    SettingsRowAction("Night Alert Summary", "Tap to choose when it arrives", Icons.Filled.NotificationsActive, RedAlert, badge = formatClock(state.nightMin)) {
                        pickTime(ctx, state.nightMin) { vm.setScheduledTime(NotifPrefsHelper.SLOT_NIGHT, it) }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

private fun NotifSettingsState.withLocalPrefs(ctx: Context): NotifSettingsState = copy(
    morningMin   = NotifPrefsHelper.scheduledMinutes(ctx, NotifPrefsHelper.SLOT_MORNING),
    trendingMin  = NotifPrefsHelper.scheduledMinutes(ctx, NotifPrefsHelper.SLOT_TRENDING),
    eveningMin   = NotifPrefsHelper.scheduledMinutes(ctx, NotifPrefsHelper.SLOT_EVENING),
    nightMin     = NotifPrefsHelper.scheduledMinutes(ctx, NotifPrefsHelper.SLOT_NIGHT),
    pausedUntil  = NotifPrefsHelper.pausedUntil(ctx),
    quietEnabled = NotifPrefsHelper.isQuietHoursEnabled(ctx),
    quietStart   = NotifPrefsHelper.quietStartMinutes(ctx),
    quietEnd     = NotifPrefsHelper.quietEndMinutes(ctx)
)

/** 8:05 AM style label for minutes after midnight. */
internal fun formatClock(minutes: Int): String {
    val h24 = (minutes / 60) % 24
    val m = minutes % 60
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12
    return String.format(java.util.Locale.ENGLISH, "%d:%02d %s", h12, m, if (h24 < 12) "AM" else "PM")
}

private fun formatPausedUntil(untilMillis: Long): String {
    val target = java.util.Calendar.getInstance().apply { timeInMillis = untilMillis }
    val now = java.util.Calendar.getInstance()
    val clock = formatClock(target.get(java.util.Calendar.HOUR_OF_DAY) * 60 + target.get(java.util.Calendar.MINUTE))
    val sameDay = target.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
        target.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
    return if (sameDay) clock
    else java.text.SimpleDateFormat("EEE", java.util.Locale.ENGLISH).format(target.time) + ", " + clock
}

private fun pickTime(ctx: Context, initialMinutes: Int, onPicked: (Int) -> Unit) {
    android.app.TimePickerDialog(
        ctx,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        false
    ).show()
}

@Composable
private fun PauseRow(pausedUntil: Long, onPick: (Int) -> Unit) {
    val paused = pausedUntil > System.currentTimeMillis()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(BlueInfo.copy(0.12f)), Alignment.Center) {
                Icon(Icons.Filled.NotificationsPaused, null, tint = BlueInfo, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Pause notifications", color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    if (paused) "Paused until ${formatPausedUntil(pausedUntil)}"
                    else "Take a breather. Emergency alerts still come through.",
                    color = if (paused) OrangePrimary else TertiaryText,
                    fontSize = 12.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PausePill(if (paused) "Resume" else "Off", selected = !paused, modifier = Modifier.weight(1f)) { onPick(0) }
            PausePill("8 hours", selected = false, modifier = Modifier.weight(1f)) { onPick(8) }
            PausePill("1 day", selected = false, modifier = Modifier.weight(1f)) { onPick(24) }
            PausePill("1 week", selected = false, modifier = Modifier.weight(1f)) { onPick(24 * 7) }
        }
    }
}

@Composable
private fun PausePill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) OrangePrimary.copy(0.16f) else SurfaceAlt)
            .border(1.dp, if (selected) OrangePrimary.copy(0.6f) else Divider, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) OrangePrimary else PrimaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun NotifPermissionBanner(onAllow: () -> Unit, onSettings: () -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val compact = maxWidth < 380.dp
        Row(
            modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrangePrimary.copy(0.1f))
            .border(1.dp, OrangePrimary.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(if (compact) 36.dp else 40.dp).clip(CircleShape).background(OrangePrimary.copy(0.2f)), Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(if (compact) 6.dp else 8.dp))
        Column(Modifier.weight(1f)) {
            Text("Notifications blocked", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Allow NagpurPulse to send you push notifications", color = SecondaryText, fontSize = 12.sp, maxLines = if (compact) 3 else 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp)).background(OrangePrimary)
                   .clickable { onAllow() }.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = 6.dp)
            ) { Text("Allow", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Text("Settings", color = OrangePrimary, fontSize = 11.sp, modifier = Modifier.clickable { onSettings() })
        }
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String, sub: String, icon: ImageVector, color: Color,
    checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit
) {
    val haptic = rememberHaptic()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(color.copy(if (enabled) 0.12f else 0.06f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = color.copy(if (enabled) 1f else 0.35f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) PrimaryText else TertiaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TertiaryText, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            modifier = Modifier.padding(start = 4.dp),
            checked = checked, onCheckedChange = { haptic.toggle(it); onCheckedChange(it) }, enabled = enabled,
            colors  = SwitchDefaults.colors(
                checkedThumbColor            = Color.White,
                checkedTrackColor            = color,
                uncheckedTrackColor          = SurfaceAlt,
                disabledCheckedTrackColor    = color.copy(0.3f),
                disabledUncheckedTrackColor  = SurfaceAlt.copy(0.5f)
            )
        )
    }
}


// ── Summary card ─────────────────────────────────────────────────────────────

@Composable
private fun NotifSummaryCard(state: NotifSettingsState, hasPermission: Boolean) {
    val on = state.pushEnabled && hasPermission
    val paused = state.pausedUntil > System.currentTimeMillis()
    val count = listOf(
        state.notifReplies, state.notifMentions, state.notifMessages, state.notifUpvotes,
        state.notifDigest, state.notifTrending, state.notifCommunity, state.notifAlerts
    ).count { it }
    val color = when {
        !on -> TertiaryText
        paused -> BlueInfo
        else -> OrangePrimary
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlowIcon(
            if (paused) Icons.Filled.NotificationsPaused else Icons.Filled.Notifications,
            color,
            active = on && !paused,
            size = 44.dp
        )
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    !on -> "Notifications are off"
                    paused -> "Paused for now"
                    else -> "You're in the loop"
                },
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                when {
                    !on -> "Turn on push to hear about replies, mentions and city alerts."
                    paused -> "We'll stay quiet until ${formatPausedUntil(state.pausedUntil)}."
                    else -> "$count of 8 updates on" + if (state.quietEnabled) " · Quiet hours set" else ""
                },
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
    }
}
