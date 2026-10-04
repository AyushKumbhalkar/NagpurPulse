// ui/screens/settings/SettingsScreen.kt
// CHANGES: SettingsUiState + SettingsViewModel now load isAdmin via AdminRepository.
//          SettingsScreen renders an ADMIN section when isAdmin == true.
package com.nagpurpulse.ui.screens.settings

import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.preferences.PreferenceManager
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.data.repository.AdminRepository           // NEW
import kotlinx.coroutines.delay
import com.nagpurpulse.ui.theme.ThemeManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Profile
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val offlineMode: Boolean = false,
    val isDarkTheme: Boolean = true,
    val cacheSize: String = "Clear",
    val isAdmin: Boolean = false,          // NEW
    val adminRole: String = "",            // NEW  e.g. "super_admin" | "admin" | "moderator"
    val postCount: Int = 0,                // REAL — fetched from Supabase
    val commentCount: Int = 0,             // REAL — fetched from Supabase
    val badgeCount: Int = 0,               // REAL — fetched from badges table
    val settingsMessage: String? = null,
    val textSize: String = "medium"
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val adminRepository: AdminRepository          // NEW
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadProfile()
        loadTextSize()
        loadDisplayDensity()
        loadFeedStyle()
        checkAdminRole()                                 // NEW
    }

    // ── NEW: Check if this user has any admin role ─────────────────────────────
    private fun checkAdminRole() {
        viewModelScope.launch {
            val isAdmin = adminRepository.isAdmin()
            val role    = if (isAdmin) adminRepository.getAdminRole() ?: "" else ""
            _uiState.value = _uiState.value.copy(isAdmin = isAdmin, adminRole = role)
        }
    }
    // ──────────────────────────────────────────────────────────────────────────

    fun dismissSettingsMessage() {
        _uiState.value = _uiState.value.copy(settingsMessage = null)
    }

    fun updateTextSize(size: String) {
        viewModelScope.launch {
            PreferenceManager.updateTextSize(size)
            val result = userPreferencesRepository.saveTextSize(size)
            _uiState.value = _uiState.value.copy(
                textSize = size,
                settingsMessage = if (result.isSuccess) "Text size saved." else "Text size is applied for this session, but couldn't be saved to your account."
            )
        }
    }

    private fun loadDisplayDensity() {
        viewModelScope.launch {
            DensityManager.density = userPreferencesRepository.getDisplayDensity()
        }
    }

    private fun loadFeedStyle() {
        viewModelScope.launch {
            FeedLayoutManager.feedStyle = userPreferencesRepository.getFeedStyle()
        }
    }

    private val appearanceSaveMutex = kotlinx.coroutines.sync.Mutex()

    fun updateDisplayDensity(density: String) {
        viewModelScope.launch {
            appearanceSaveMutex.withLock {
                val previous = DensityManager.density
                val result = userPreferencesRepository.saveDisplayDensity(density)
                if (result.isSuccess) {
                    DensityManager.density = density
                } else {
                    DensityManager.density = previous
                }
                _uiState.value = _uiState.value.copy(
                    settingsMessage = if (result.isSuccess) "Display density saved." else "Display density couldn't be saved. Your previous value was restored."
                )
            }
        }
    }

    fun updateFeedStyle(style: String) {
        viewModelScope.launch {
            appearanceSaveMutex.withLock {
                val previous = FeedLayoutManager.feedStyle
                val result = userPreferencesRepository.saveFeedStyle(style)
                if (result.isSuccess) {
                    FeedLayoutManager.feedStyle = style
                } else {
                    FeedLayoutManager.feedStyle = previous
                }
                _uiState.value = _uiState.value.copy(
                    settingsMessage = if (result.isSuccess) "Feed style saved." else "Feed style couldn't be saved. Your previous value was restored."
                )
            }
        }
    }

    private fun loadTextSize() {
        viewModelScope.launch {
            val size = userPreferencesRepository.getTextSize()
            PreferenceManager.updateTextSize(size)
            _uiState.value = _uiState.value.copy(textSize = size)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getCurrentProfile().fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(profile = profile)
                    // Load real stats using AdminRepository (SupabaseClient already injected)
                    try {
                        val (posts, comments, badges) = adminRepository.getUserQuickStats(profile.id)
                        _uiState.value = _uiState.value.copy(
                            postCount    = posts,
                            commentCount = comments,
                            badgeCount   = badges
                        )
                    } catch (_: Exception) {}
                },
                onFailure = { error ->
                    android.util.Log.e("SETTINGS_VM", "Profile load failed", error)
                }
            )
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            offlineMode = enabled,
            settingsMessage = "Offline reading is not implemented yet; no offline content was downloaded."
        )
    }

    fun clearCache(context: Context) {
        authRepository.clearLocalCache(context)
        _uiState.value = _uiState.value.copy(cacheSize = "Cleared")
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            notificationRepository.deleteFcmToken()
            authRepository.signOut()
            onDone()
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState    = viewModel.uiState.collectAsState().value
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    val profile    = uiState.profile
    var showLogout by remember { mutableStateOf(false) }
    var unavailableMessage by remember { mutableStateOf<String?>(null) }

    unavailableMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { unavailableMessage = null },
            title = { Text("Not available yet", color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = SecondaryText) },
            confirmButton = {
                TextButton(onClick = { unavailableMessage = null }) {
                    Text("Got it", color = OrangePrimary)
                }
            },
            containerColor = SurfaceAlt
        )
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor   = SurfaceAlt,
            title = {
                Text("Sign out?", color = PrimaryText, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("You'll need to sign in again.", color = SecondaryText)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogout = false
                        viewModel.logout {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                ) { Text("Sign Out", color = RedAlert, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 150f))
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
                            "Settings",
                            color      = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Manage your preferences",
                            color = SecondaryText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ── Profile card ────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Surface)
                        .border(1.dp, OrangePrimary.copy(0.12f), RoundedCornerShape(18.dp))
                        .pressScale(onClick = { navController.navigate(Screen.AccountProfile.route) })
                        .padding(DensityManager.cardPadding.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary.copy(0.15f))
                                .border(2.dp, OrangePrimary.copy(0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(profile!!.avatarUrl).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier           = Modifier.fillMaxSize(),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile?.username ?: "User", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                if (profile?.isVerified == true) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        Modifier.size(16.dp).clip(CircleShape).background(OrangePrimary),
                                        Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Verified",
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Text(profile?.displayName?.takeIf { it.isNotBlank() } ?: "NagpurPulse member", color = SecondaryText, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationOn, null, tint = TertiaryText, modifier = Modifier.size(11.dp))
                                Text(" ${profile?.areas?.firstOrNull() ?: profile?.location?.takeIf { it.isNotBlank() } ?: "Location not set"}", color = TertiaryText, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(OrangePrimary.copy(0.15f))
                                    .border(1.dp, OrangePrimary.copy(0.3f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text("Pulse Member", color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TertiaryText, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(Icons.Filled.Bolt, "${profile?.karma ?: 0}", "Pulse Points", OrangePrimary, Modifier.weight(1f))
                    StatPill(Icons.Filled.Description, "${uiState.postCount}", "Posts", BlueInfo, Modifier.weight(1f))
                    StatPill(Icons.Filled.Message, "${uiState.commentCount}", "Comments", PurpleNight, Modifier.weight(1f))
                    StatPill(Icons.Filled.Star, "${uiState.badgeCount}", "Badges", GreenSuccess, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── ADMIN PANEL (only visible to admins) ────────────────────────
          //
            if (uiState.isAdmin) {
                item { SectionHeader("ADMIN") }
                item {
                    AdminPanelSettingsCard(
                        role      = uiState.adminRole,
                        onClick   = { navController.navigate(Screen.AdminPanel.route) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── ACCOUNT ─────────────────────────────────────────────────────
            item { SectionHeader("ACCOUNT") }
            item {
                SettingsGroup {
                    SettingsRow("Account & Profile", "Edit your profile, avatar and bio",      Icons.Filled.Person,        OrangePrimary) { navController.navigate(Screen.AccountProfile.route) }
                    SettingsDivider()
                    SettingsRow("Privacy & Safety",  "Control your privacy and safety settings",   Icons.Filled.PrivacyTip,    GreenSuccess)  { navController.navigate(Screen.PrivacySettings.route) }
                    SettingsDivider()
                    SettingsRow("Incognito Settings","Manage incognito mode and visibility",   Icons.Filled.VisibilityOff, PurpleNight)   { navController.navigate(Screen.IncognitoSettings.route) }
                    SettingsDivider()
                    SettingsRow("Security",          "Password, 2FA and login activity",       Icons.Filled.Lock,          BlueInfo)      { navController.navigate(Screen.SecuritySettings.route) }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── PREFERENCES ─────────────────────────────────────────────────
            item { SectionHeader("PREFERENCES") }
            item {
                SettingsGroup {
                    SettingsRow("Notifications", "Manage alerts and notification preferences", Icons.Filled.Notifications, OrangePrimary) { navController.navigate(Screen.NotifSettings.route) }
                    SettingsDivider()
                    SettingsRowToggle(
                        label         = "Light Theme",
                        sub           = "Switch between dark and light mode",
                        icon          = Icons.Filled.Palette,
                        iconTint      = PurpleNight,
                        checked       = ThemeManager.isLightTheme,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                delay(180)
                                ThemeManager.toggleTheme(enabled)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsRow("Language", "English — more languages coming later", Icons.Filled.Language, BlueInfo) { unavailableMessage = "Language selection is not wired to translated app resources yet, so changing it would not actually translate the app." }
                    SettingsDivider()
                    SettingsRow("Text Size", "Adjust text size throughout the app", Icons.Filled.TextFields, TextSecondary) {
                        navController.navigate(Screen.TextSize.route)
                    }
                    SettingsDivider()
                    SettingsRow("Display Density", "Compact, comfortable or spacious layout", Icons.Filled.Dashboard, BlueInfo) {
                        navController.navigate(Screen.DisplayDensity.route)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── APP SETTINGS ─────────────────────────────────────────────────
            item { SectionHeader("APP SETTINGS") }
            item {
                SettingsGroup {
                    SettingsRow("Content Preferences", "Topics, categories and content filters", Icons.Filled.PushPin, RedAlert) { unavailableMessage = "Topic filtering is not connected to the feed query yet. This option will become active when those filters are implemented." }
                    SettingsDivider()
                    SettingsRow("Data & Storage", "Manage data usage and media quality", Icons.Filled.Download, BlueInfo) { unavailableMessage = "Per-network media quality and data-usage controls are not implemented yet." }
                    SettingsDivider()
                    SettingsRow("Offline Reading", "Offline downloads are not available yet", Icons.Filled.WifiOff, TextSecondary) { unavailableMessage = "NagpurPulse does not currently download and sync posts for offline reading. This control has been removed rather than pretending it works." }
                    SettingsDivider()
                    SettingsRowAction("Clear Cache", "Clear temporary app and image cache", Icons.Filled.Delete, RedAlert, badge = uiState.cacheSize) {
                        viewModel.clearCache(context)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── ABOUT ────────────────────────────────────────────────────────
            item { SectionHeader("ABOUT") }
            item {
                SettingsGroup {
                    SettingsRow("About Nagpur Pulse", "App info, version and terms", Icons.Filled.Info, BlueInfo) { unavailableMessage = "The dedicated in-app About and legal-information screen is not connected yet." }
                    SettingsDivider()
                    SettingsRow("Help & Support", "FAQs, guides and contact support", Icons.Filled.HelpOutline, GreenSuccess) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://nagpurpulse.in/support")))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Log out ──────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface)
                        .border(1.dp, RedAlert.copy(0.2f), RoundedCornerShape(16.dp))
                        .pressScale(onClick = { showLogout = true })
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(RedAlert.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = RedAlert, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Log Out", color = RedAlert, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("Sign out from your account", color = TertiaryText, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ── Admin Panel Entry Card (NEW) ──────────────────────────────────────────────

@Composable
private fun AdminPanelSettingsCard(role: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, OrangePrimary.copy(0.4f), RoundedCornerShape(16.dp))
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Shield icon with orange glow effect
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary.copy(0.2f))
                    .border(1.dp, OrangePrimary.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Security, null, tint = OrangePrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Admin Panel",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    // Role badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrangePrimary.copy(0.2f))
                            .border(1.dp, OrangePrimary.copy(0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text  = when (role) {
                                "super_admin" -> "Super Admin"
                                "moderator"   -> "Moderator"
                                else          -> "Admin"
                            },
                            color      = OrangePrimary,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "Moderation, reports, users & logs",
                    color    = Color.White.copy(0.5f),
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                null,
                tint     = OrangePrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── Shared Settings composables (unchanged from original) ─────────────────────

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        color        = TertiaryText,
        fontSize     = 11.sp,
        fontWeight   = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier     = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface),
        content  = content
    )
}

@Composable
fun SettingsRow(label: String, sub: String, icon: ImageVector, iconTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(0.12f)), Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = PrimaryText,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(sub, color = TertiaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun SettingsRowBadge(label: String, sub: String, icon: ImageVector, iconTint: Color, badge: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().pressScale(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(0.12f)), Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TertiaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(badge, color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun SettingsRowToggle(label: String, sub: String, icon: ImageVector, iconTint: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(0.12f)), Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TertiaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = OrangePrimary,
                uncheckedTrackColor = SurfaceAlt
            )
        )
    }
}

@Composable
fun SettingsRowAction(label: String, sub: String, icon: ImageVector, iconTint: Color, badge: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().pressScale(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(0.12f)), Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(sub, color = TertiaryText, fontSize = 12.sp)
        }
        if (badge != null) Text(badge, color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 66.dp), color = Divider.copy(0.6f), thickness = 0.5.dp)
}

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, color.copy(0.12f), RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = TertiaryText, fontSize = 10.sp)
    }
}
