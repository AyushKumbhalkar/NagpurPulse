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
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.notifications.ScheduledPushManager
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
    val isLoading:      Boolean = true
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class NotifSettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val appContext: Context   // ← injected, not from composable
) : ViewModel() {

    private val _state = MutableStateFlow(NotifSettingsState())
    val state: StateFlow<NotifSettingsState> = _state

    init { loadSettings() }

    private fun loadSettings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val prefs = userPreferencesRepository.loadAndSyncNotifPrefs(appContext)
            _state.value = if (prefs != null) {
                NotifSettingsState(
                    pushEnabled    = prefs.notifPush,
                    notifReplies   = prefs.notifReplies,
                    notifMentions  = prefs.notifMentions,
                    notifMessages  = prefs.notifMessages,
                    notifUpvotes   = prefs.notifUpvotes,
                    notifDigest    = prefs.notifDigest,
                    notifTrending  = prefs.notifTrending,
                    notifCommunity = prefs.notifCommunity,
                    notifAlerts    = prefs.notifAlertsSummary,
                    isLoading      = false
                )
            } else {
                // Fall back to SharedPreferences (works offline)
                NotifSettingsState(
                    pushEnabled    = NotifPrefsHelper.isPushEnabled(appContext),
                    notifReplies   = NotifPrefsHelper.isRepliesEnabled(appContext),
                    notifMentions  = NotifPrefsHelper.isMentionsEnabled(appContext),
                    notifMessages  = NotifPrefsHelper.isMessagesEnabled(appContext),
                    notifUpvotes   = NotifPrefsHelper.isUpvotesEnabled(appContext),
                    notifDigest    = NotifPrefsHelper.isDigestEnabled(appContext),
                    notifTrending  = NotifPrefsHelper.isTrendingEnabled(appContext),
                    notifCommunity = NotifPrefsHelper.isCommunityEnabled(appContext),
                    notifAlerts    = NotifPrefsHelper.isAlertsSummaryEnabled(appContext),
                    isLoading      = false
                )
            }
        }
    }

    fun toggle(field: String, value: Boolean) {
        // Update UI immediately (snappy)
        _state.value = when (field) {
            "notif_push"           -> _state.value.copy(pushEnabled    = value)
            "notif_replies"        -> _state.value.copy(notifReplies   = value)
            "notif_mentions"       -> _state.value.copy(notifMentions  = value)
            "notif_messages"       -> _state.value.copy(notifMessages  = value)
            "notif_upvotes"        -> _state.value.copy(notifUpvotes   = value)
            "notif_digest"         -> _state.value.copy(notifDigest    = value)
            "notif_trending"       -> _state.value.copy(notifTrending  = value)
            "notif_community"      -> _state.value.copy(notifCommunity = value)
            "notif_alerts_summary" -> _state.value.copy(notifAlerts    = value)
            else                   -> _state.value
        }
        // Reschedule workers when push master or any scheduled pref changes
        if (field in listOf("notif_push", "notif_digest", "notif_trending",
                "notif_community", "notif_alerts_summary")) {
            ScheduledPushManager.reschedule(appContext)
        }
        // Persist to Supabase + SharedPreferences
        viewModelScope.launch {
            userPreferencesRepository.saveNotifPref(appContext, field, value)
        }
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
        topBar = {
            Column(
                Modifier
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 120f))
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryText)
                    }
                    Text(
                        "Notifications",
                        color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
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
                    NotifToggleRow("Morning Digest",    "Top posts from Nagpur at 8:00 AM",         Icons.Filled.WbSunny,            YellowWarn,   state.notifDigest,    state.pushEnabled && hasPermission.value) { vm.toggle("notif_digest",          it) }
                    SettingsDivider()
                    NotifToggleRow("Trending Alert",    "What's hot in Nagpur at 1:00 PM",          Icons.Filled.Whatshot,           OrangePrimary, state.notifTrending, state.pushEnabled && hasPermission.value) { vm.toggle("notif_trending",        it) }
                    SettingsDivider()
                    NotifToggleRow("Evening Community", "City update at 6:00 PM",                   Icons.Filled.NightsStay,         PurpleNight,  state.notifCommunity, state.pushEnabled && hasPermission.value) { vm.toggle("notif_community",       it) }
                    SettingsDivider()
                    NotifToggleRow("Night Alert Summary","Active alert summary at 10:00 PM",        Icons.Filled.NotificationsActive, RedAlert,    state.notifAlerts,    state.pushEnabled && hasPermission.value) { vm.toggle("notif_alerts_summary",  it) }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun NotifPermissionBanner(onAllow: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrangePrimary.copy(0.1f))
            .border(1.dp, OrangePrimary.copy(0.3f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(OrangePrimary.copy(0.2f)), Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Notifications blocked", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Allow NagpurPulse to send you push notifications", color = SecondaryText, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp)).background(OrangePrimary)
                    .clickable { onAllow() }.padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Allow", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Text("Settings", color = OrangePrimary, fontSize = 11.sp, modifier = Modifier.clickable { onSettings() })
        }
    }
}

@Composable
private fun NotifToggleRow(
    label: String, sub: String, icon: ImageVector, color: Color,
    checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(color.copy(if (enabled) 0.12f else 0.06f)),
            Alignment.Center
        ) {
            Icon(icon, null, tint = color.copy(if (enabled) 1f else 0.35f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = if (enabled) PrimaryText else TertiaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TertiaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked = checked, onCheckedChange = onCheckedChange, enabled = enabled,
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
