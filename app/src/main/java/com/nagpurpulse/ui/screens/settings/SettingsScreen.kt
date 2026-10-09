// ui/screens/settings/SettingsScreen.kt
//
// Settings hub. Highlights of this version:
//  • Hero profile card: real level + progress ring, streak, completeness nudge, shimmer while loading
//  • "Finish setting up" checklist with a one-time celebration
//  • Search, Dark/Light/System appearance control, live value badges on rows
//  • Snackbar feedback for every save (settingsMessage is finally shown)
//  • No dead-end rows: unfinished features are shown as "Soon" (non-tappable) or removed
package com.nagpurpulse.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Profile
import com.nagpurpulse.data.repository.AdminRepository
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.NotificationRepository
import com.nagpurpulse.data.repository.PostRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.preferences.PreferenceManager
import com.nagpurpulse.ui.preferences.WellbeingManager
import com.nagpurpulse.ui.screens.profile.Completeness
import com.nagpurpulse.ui.screens.profile.LevelProgress
import com.nagpurpulse.ui.screens.profile.ProfileLevels
import com.nagpurpulse.ui.screens.profile.activityByDay
import com.nagpurpulse.ui.screens.profile.computeCompleteness
import com.nagpurpulse.ui.screens.profile.computeStreak
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val profile: Profile? = null,
    val profileLoading: Boolean = true,
    val isLoading: Boolean = false,
    val isDarkTheme: Boolean = true,
    val cacheSize: String = "…",
    val isAdmin: Boolean = false,
    val adminRole: String = "",            // "super_admin" | "admin" | "moderator"
    val postCount: Int = 0,
    val commentCount: Int = 0,
    val badgeCount: Int = 0,
    val statsLoaded: Boolean = false,
    val streakDays: Int = 0,
    val streakAtRisk: Boolean = false,
    // Device-local state, refreshed every time the screen resumes
    val localLoaded: Boolean = false,
    val appLockEnabled: Boolean = false,
    val pushOn: Boolean = false,
    val notifSummary: String = "",
    val wellbeingEnabled: Boolean = false,
    val setupCelebrated: Boolean = false,
    val settingsMessage: String? = null,
    val textSize: String = "medium"
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val adminRepository: AdminRepository,
    private val postRepository: PostRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val appearanceSaveMutex = Mutex()

    init {
        loadProfile()
        loadTextSize()
        loadDisplayDensity()
        loadFeedStyle()
        checkAdminRole()
    }

    // ── Device-local state (cache size, app lock, notification summary, ...) ──
    fun refreshLocalState() {
        viewModelScope.launch {
            val bytes = computeCacheBytes()
            val ctx = appContext
            val pushOn = NotifPrefsHelper.isPushEnabled(ctx) &&
                NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            val enabledCount = listOf(
                NotifPrefsHelper.isRepliesEnabled(ctx),
                NotifPrefsHelper.isMentionsEnabled(ctx),
                NotifPrefsHelper.isMessagesEnabled(ctx),
                NotifPrefsHelper.isUpvotesEnabled(ctx),
                NotifPrefsHelper.isDigestEnabled(ctx),
                NotifPrefsHelper.isTrendingEnabled(ctx),
                NotifPrefsHelper.isCommunityEnabled(ctx),
                NotifPrefsHelper.isAlertsSummaryEnabled(ctx)
            ).count { it }
            val summary = when {
                !pushOn -> "Off"
                NotifPrefsHelper.isPaused(ctx) -> "Paused"
                else -> "$enabledCount of 8 on"
            }
            _uiState.value = _uiState.value.copy(
                cacheSize = formatBytes(bytes),
                appLockEnabled = ctx.getSharedPreferences("security", Context.MODE_PRIVATE)
                    .getBoolean("biometric_enabled", false),
                pushOn = pushOn,
                notifSummary = summary,
                wellbeingEnabled = WellbeingManager.isEnabled(ctx),
                setupCelebrated = ctx.getSharedPreferences("settings_ui", Context.MODE_PRIVATE)
                    .getBoolean("setup_celebrated", false),
                localLoaded = true
            )
        }
    }

    private suspend fun computeCacheBytes(): Long = withContext(Dispatchers.IO) {
        try {
            appContext.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } catch (_: Exception) {
            0L
        }
    }

    private fun checkAdminRole() {
        viewModelScope.launch {
            val isAdmin = adminRepository.isAdmin()
            val role    = if (isAdmin) adminRepository.getAdminRole() ?: "" else ""
            _uiState.value = _uiState.value.copy(isAdmin = isAdmin, adminRole = role)
        }
    }

    fun dismissSettingsMessage() {
        _uiState.value = _uiState.value.copy(settingsMessage = null)
    }

    fun markSetupCelebrated() {
        appContext.getSharedPreferences("settings_ui", Context.MODE_PRIVATE)
            .edit().putBoolean("setup_celebrated", true).apply()
        _uiState.value = _uiState.value.copy(setupCelebrated = true)
    }

    fun setWellbeing(enabled: Boolean) {
        WellbeingManager.setEnabled(appContext, enabled)
        _uiState.value = _uiState.value.copy(
            wellbeingEnabled = enabled,
            settingsMessage = if (enabled)
                "Got it. We'll nudge you after ${WellbeingManager.REMINDER_MINUTES} minutes in the app."
            else "Break reminder turned off."
        )
    }

    fun updateTextSize(size: String) {
        if (size !in setOf("small", "medium", "large", "extra_large")) return
        viewModelScope.launch {
            appearanceSaveMutex.withLock {
                val previous = PreferenceManager.textSize
                val result = userPreferencesRepository.saveTextSize(size)
                if (result.isSuccess) {
                    PreferenceManager.updateTextSize(size)
                    _uiState.value = _uiState.value.copy(textSize = size)
                } else {
                    PreferenceManager.updateTextSize(previous)
                    _uiState.value = _uiState.value.copy(textSize = previous)
                }
                _uiState.value = _uiState.value.copy(
                    settingsMessage = if (result.isSuccess) "Text size saved." else "Text size couldn't be saved. Your previous value was restored."
                )
            }
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
                    _uiState.value = _uiState.value.copy(profile = profile, profileLoading = false)
                    checkLevelUp(profile.karma)
                    loadStreak(profile.id)
                    try {
                        val (posts, comments, badges) = adminRepository.getUserQuickStats(profile.id)
                        _uiState.value = _uiState.value.copy(
                            postCount    = posts,
                            commentCount = comments,
                            badgeCount   = badges
                        )
                    } catch (_: Exception) {
                    } finally {
                        _uiState.value = _uiState.value.copy(statsLoaded = true)
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(profileLoading = false)
                    android.util.Log.e("SETTINGS_VM", "Profile load failed", error)
                }
            )
        }
    }

    /** Celebrates a new level once, the first time the user opens Settings after reaching it. */
    private fun checkLevelUp(karma: Int) {
        val level = ProfileLevels.forKarma(karma)
        val prefs = appContext.getSharedPreferences("settings_ui", Context.MODE_PRIVATE)
        val seen = prefs.getInt("last_level_seen", 0)
        if (seen in 1 until level.number) {
            _uiState.value = _uiState.value.copy(
                settingsMessage = "Level up! You're now ${level.emoji} ${level.title}. ${level.unlock} unlocked 🎉"
            )
        }
        if (seen != level.number) prefs.edit().putInt("last_level_seen", level.number).apply()
    }

    /** Current streak from the user's own posts + comments (same rules as the profile screen). */
    private fun loadStreak(userId: String) {
        viewModelScope.launch {
            try {
                val posts = postRepository.getPostsByUser(userId).getOrNull().orEmpty()
                val comments = postRepository.getCommentsByUser(userId).getOrNull().orEmpty()
                val days = activityByDay(posts.map { it.createdAt } + comments.map { it.createdAt }).keys
                val streak = computeStreak(days, LocalDate.now())
                _uiState.value = _uiState.value.copy(
                    streakDays = streak.days,
                    streakAtRisk = streak.atRisk
                )
            } catch (_: Exception) {
                // Streak is a nice-to-have; never block settings on it.
            }
        }
    }

    /** mode: "dark" | "light" | "system" */
    fun updateThemeMode(mode: String, systemDark: Boolean) {
        val followSystem = mode == "system"
        val light = when (mode) {
            "light" -> true
            "dark"  -> false
            else    -> !systemDark
        }
        // Theme stays responsive even if the account preference endpoint is unavailable.
        ThemeManager.followSystem = followSystem
        appContext.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("follow_system", followSystem).apply()
        ThemeManager.toggleTheme(light)
        userPreferencesRepository.saveThemeLocally(appContext, !light)
        viewModelScope.launch {
            val result = userPreferencesRepository.saveAmoledMode(!light)
            _uiState.value = _uiState.value.copy(
                settingsMessage = if (result.isSuccess) {
                    "Appearance saved."
                } else {
                    "Appearance changed on this device, but account sync failed. Check your connection and retry."
                }
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val before = computeCacheBytes()
            authRepository.clearLocalCache(appContext)
            val after = computeCacheBytes()
            _uiState.value = _uiState.value.copy(
                cacheSize = formatBytes(after),
                settingsMessage = if (before > 0L)
                    "Freed ${formatBytes((before - after).coerceAtLeast(0L))} ✨"
                else "Your cache is already clean ✨"
            )
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            // signOut() already performs best-effort device-token cleanup; avoid a second network round-trip.
            authRepository.signOut()
            onDone()
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L        -> "$bytes B"
    bytes < 1024L * 1024 -> "${bytes / 1024} KB"
    else                 -> String.format(java.util.Locale.ENGLISH, "%.1f MB", bytes / 1048576.0)
}

private fun appVersionName(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
} catch (_: Exception) {
    "1.0"
}

private fun textSizeLabel(size: String) = when (size) {
    "small"       -> "Small"
    "large"       -> "Large"
    "extra_large" -> "Extra large"
    else          -> "Medium"
}

private data class SettingsEntry(
    val label: String,
    val sub: String,
    val icon: ImageVector,
    val tint: Color,
    val keywords: String,
    val onClick: () -> Unit
)

private data class SetupItem(val label: String, val done: Boolean, val onClick: () -> Unit)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val systemDark = isSystemInDarkTheme()
    val profile = uiState.profile

    var showLogout by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val appVersion = remember { appVersionName(context) }

    // Refresh device-local state (cache size, app lock, notifications) whenever we come back.
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshLocalState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Every save/failure message from the ViewModel is now actually shown.
    LaunchedEffect(uiState.settingsMessage) {
        uiState.settingsMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissSettingsMessage()
        }
    }

    val openSupport: () -> Unit = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://nagpurpulse.in/support")))
        } catch (_: Exception) {
            scope.launch { snackbar.showSnackbar("Couldn't open a browser. Visit nagpurpulse.in/support") }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("NagpurPulse", color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version $appVersion", color = SecondaryText)
                    Text(
                        "The pulse of Nagpur: what's happening, what's trending and what your neighbours are talking about.",
                        color = SecondaryText
                    )
                    Text("Made with 🧡 in Nagpur", color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Close", color = OrangePrimary) }
            },
            containerColor = SurfaceAlt
        )
    }

    var logoutBusy by remember { mutableStateOf(false) }

    if (showLogout) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!logoutBusy) showLogout = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !logoutBusy,
                dismissOnClickOutside = !logoutBusy
            )
        ) {
            val isDark = LocalIsDarkTheme.current
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .heightIn(max = maxHeight * 0.88f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            if (isDark)
                                Brush.linearGradient(
                                    listOf(Color(0xFF211719), Color(0xFF111116), Color(0xFF211719))
                                )
                            else
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFF5F2), Color(0xFFFFFFFF), Color(0xFFFFF5F2))
                                )
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFFFF6848).copy(alpha = 0.88f)
                            else Color(0xFFFF6848).copy(alpha = 0.45f),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(SecondaryText.copy(alpha = 0.5f))
                    )

                    Spacer(Modifier.height(30.dp))

                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF6C43).copy(alpha = 0.18f), Color(0xFFFF6C43).copy(alpha = 0.08f))
                                )
                            )
                            .border(1.dp, Color(0xFFFF6548).copy(alpha = if (isDark) 1f else 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFFFF604C),
                            modifier = Modifier.size(43.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Ready to head out?",
                        color = PrimaryText,
                        fontSize = 27.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "You'll be signed out of NagpurPulse on this device. You can sign back in anytime.",
                        color = SecondaryText,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SurfaceAlt)
                                .border(1.dp, Divider, RoundedCornerShape(50))
                                .pressScale { if (!logoutBusy) showLogout = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Stay signed in",
                                color = PrimaryText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFF6652), Color(0xFFFF433D))
                                    )
                                )
                                .pressScale {
                                    if (!logoutBusy) {
                                        logoutBusy = true
                                        viewModel.logout {
                                            logoutBusy = false
                                            showLogout = false
                                            navController.navigate(Screen.Login.route) {
                                                popUpTo(0) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoutBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(23.dp)
                                    )
                                    Text(
                                        "Sign out",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Derived data ─────────────────────────────────────────────────────────
    val levelProgress = remember(profile?.karma) { ProfileLevels.progress(profile?.karma ?: 0) }
    val completeness: Completeness? = if (profile != null && uiState.statsLoaded) {
        computeCompleteness(
            hasAvatar = !profile.avatarUrl.isNullOrBlank(),
            hasTagline = !profile.tagline.isNullOrBlank(),
            hasLocation = !profile.location.isNullOrBlank(),
            hasAreas = profile.areas.isNotEmpty(),
            hasThread = uiState.postCount > 0
        )
    } else null

    val setupItems: List<SetupItem> = if (profile == null || !uiState.localLoaded) emptyList() else listOf(
        SetupItem("Add a profile photo", !profile.avatarUrl.isNullOrBlank()) { navController.navigate(Screen.AccountProfile.route) },
        SetupItem("Pick your Nagpur areas", profile.areas.isNotEmpty()) { navController.navigate(Screen.AccountProfile.route) },
        SetupItem("Turn on notifications", uiState.pushOn) { navController.navigate(Screen.NotifSettings.route) },
        SetupItem("Protect with app lock", uiState.appLockEnabled) { navController.navigate(Screen.SecuritySettings.route) }
    )
    val setupAllDone = setupItems.isNotEmpty() && setupItems.all { it.done }
    val showSetupCard = setupItems.isNotEmpty() && !setupAllDone
    val showCelebration = setupAllDone && !uiState.setupCelebrated

    val themeMode = if (ThemeManager.followSystem) "system" else if (ThemeManager.isLightTheme) "light" else "dark"

    val entries = buildList {
        add(SettingsEntry("Account & Profile", "Edit your profile, avatar and bio", Icons.Filled.Person, OrangePrimary,
            "name username photo avatar bio tagline location areas profile account") { navController.navigate(Screen.AccountProfile.route) })
        add(SettingsEntry("Privacy & Safety", "Control your privacy and safety settings", Icons.Filled.PrivacyTip, GreenSuccess,
            "privacy hide dm messages visibility search online status safety") { navController.navigate(Screen.PrivacySettings.route) })
        add(SettingsEntry("Incognito Mode", "Browse with extra privacy", Icons.Filled.VisibilityOff, PurpleNight,
            "incognito private anonymous hide") { navController.navigate(Screen.IncognitoSettings.route) })
        add(SettingsEntry("Security", "Password, app lock and sessions", Icons.Filled.Lock, BlueInfo,
            "password biometric fingerprint face lock sessions sign out security") { navController.navigate(Screen.SecuritySettings.route) })
        add(SettingsEntry("Notifications", "Alerts, pause, quiet hours and delivery times", Icons.Filled.Notifications, OrangePrimary,
            "push notifications pause quiet hours digest mentions replies upvotes time alerts") { navController.navigate(Screen.NotifSettings.route) })
        add(SettingsEntry("Text Size", "Adjust text size throughout the app", Icons.Filled.TextFields, SecondaryText,
            "font text size bigger larger smaller") { navController.navigate(Screen.TextSize.route) })
        add(SettingsEntry("Display Density", "Compact, comfortable or spacious layout", Icons.Filled.Dashboard, BlueInfo,
            "density compact spacious comfortable feed layout style") { navController.navigate(Screen.DisplayDensity.route) })
        add(SettingsEntry("Help & Support", "FAQs, guides and contact support", Icons.Filled.HelpOutline, GreenSuccess,
            "help support faq contact") { openSupport() })
        add(SettingsEntry("About NagpurPulse", "App info and version", Icons.Filled.Info, BlueInfo,
            "about version info app") { showAbout = true })
        if (uiState.isAdmin) {
            add(SettingsEntry("Admin Panel", "Moderation, reports, users & logs", Icons.Filled.Security, OrangePrimary,
                "admin moderation reports users logs") { navController.navigate(Screen.AdminPanel.route) })
        }
    }
    val q = query.trim().lowercase()
    val results = if (q.isEmpty()) emptyList() else entries.filter {
        it.label.lowercase().contains(q) || it.sub.lowercase().contains(q) || it.keywords.contains(q)
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Settings",
                            color      = PrimaryText,
                            fontWeight = FontWeight.Bold,
                            style      = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Make NagpurPulse yours",
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
            // ── Search ──────────────────────────────────────────────────────
            item(key = "search") {
                SettingsSearchBar(query) { query = it }
                Spacer(Modifier.height(10.dp))
            }

            if (q.isNotEmpty()) {
                // ── Search results ─────────────────────────────────────────
                item(key = "results") {
                    Column {
                        SectionHeader("RESULTS")
                        if (results.isEmpty()) {
                            Text(
                                "No settings match \"${query.trim()}\"",
                                color = TertiaryText,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                            )
                        } else {
                            SettingsGroup {
                                results.forEachIndexed { index, entry ->
                                    if (index > 0) SettingsDivider()
                                    SettingsRow(entry.label, entry.sub, entry.icon, entry.tint) { entry.onClick() }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Hero: level, streak, completeness, stats ────────────────
                item(key = "hero") {
                    StaggerIn(0) {
                        Column {
                            ProfileHeroCard(
                                profile = profile,
                                loading = uiState.profileLoading,
                                level = levelProgress,
                                streakDays = uiState.streakDays,
                                streakAtRisk = uiState.streakAtRisk,
                                completeness = completeness,
                                onOpenProfile = { navController.navigate(Screen.AccountProfile.route) },
                                onImprove = {
                                    if (completeness?.next?.key == "thread") {
                                        navController.navigate(Screen.CreatePost.createRoute())
                                    } else {
                                        navController.navigate(Screen.AccountProfile.route)
                                    }
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatPill(Icons.Filled.Bolt, "${profile?.karma ?: 0}", "Pulse Points", OrangePrimary, Modifier.weight(1f)) { navController.navigate(Screen.Profile.route) }
                                StatPill(Icons.Filled.Description, "${uiState.postCount}", "Posts", BlueInfo, Modifier.weight(1f)) { navController.navigate(Screen.Profile.route) }
                                StatPill(Icons.Filled.Message, "${uiState.commentCount}", "Comments", PurpleNight, Modifier.weight(1f)) { navController.navigate(Screen.Profile.route) }
                                StatPill(Icons.Filled.Star, "${uiState.badgeCount}", "Badges", GreenSuccess, Modifier.weight(1f)) { navController.navigate(Screen.Profile.route) }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }

                // ── Setup checklist / one-time celebration ──────────────────
                if (showSetupCard) {
                    item(key = "setup") {
                        StaggerIn(1) {
                            Column {
                                SetupCard(setupItems)
                                Spacer(Modifier.height(14.dp))
                            }
                        }
                    }
                }
                if (showCelebration) {
                    item(key = "celebration") {
                        Column {
                            SetupCelebrationCard { viewModel.markSetupCelebrated() }
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }

                // ── ADMIN PANEL (only visible to admins) ────────────────────
                if (uiState.isAdmin) {
                    item(key = "admin") {
                        StaggerIn(2) {
                            Column {
                                SectionHeader("ADMIN")
                                AdminPanelSettingsCard(
                                    role    = uiState.adminRole,
                                    onClick = { navController.navigate(Screen.AdminPanel.route) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }

                // ── ACCOUNT & SAFETY ────────────────────────────────────────
                item(key = "account") {
                    StaggerIn(3) {
                        Column {
                            SectionHeader("ACCOUNT & SAFETY")
                            SettingsGroup {
                                SettingsRow("Account & Profile", "Edit your profile, avatar and bio", Icons.Filled.Person, OrangePrimary) { navController.navigate(Screen.AccountProfile.route) }
                                SettingsDivider()
                                SettingsRow("Privacy & Safety", "Control your privacy and safety settings", Icons.Filled.PrivacyTip, GreenSuccess) { navController.navigate(Screen.PrivacySettings.route) }
                                SettingsDivider()
                                SettingsRowBadge(
                                    "Security",
                                    if (uiState.appLockEnabled) "App lock is protecting your account" else "Add app lock, change password, manage sessions",
                                    Icons.Filled.Lock,
                                    BlueInfo,
                                    if (uiState.appLockEnabled) "Lock on" else "Set up"
                                ) { navController.navigate(Screen.SecuritySettings.route) }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                // ── PREFERENCES ─────────────────────────────────────────────
                item(key = "preferences") {
                    StaggerIn(4) {
                        Column {
                            SectionHeader("PREFERENCES")
                            SettingsGroup {
                                SettingsRowBadge(
                                    "Notifications", "Alerts, pause, quiet hours and delivery times",
                                    Icons.Filled.Notifications, OrangePrimary, uiState.notifSummary
                                ) { navController.navigate(Screen.NotifSettings.route) }
                                SettingsDivider()
                                AppearanceRow(themeMode) { mode ->
                                    if (mode != themeMode) {
                                        haptic.tap()
                                        viewModel.updateThemeMode(mode, systemDark)
                                    }
                                }
                                SettingsDivider()
                                SettingsRowBadge(
                                    "Text Size", "Adjust text size throughout the app",
                                    Icons.Filled.TextFields, SecondaryText, textSizeLabel(uiState.textSize)
                                ) { navController.navigate(Screen.TextSize.route) }
                                SettingsDivider()
                                SettingsRowBadge(
                                    "Display Density", "Compact, comfortable or spacious layout",
                                    Icons.Filled.Dashboard, BlueInfo, DensityManager.density.replaceFirstChar { it.uppercase() }
                                ) { navController.navigate(Screen.DisplayDensity.route) }
                                SettingsDivider()
                                SettingsRowToggle(
                                    label = "Take-a-break reminder",
                                    sub = "A gentle nudge after ${WellbeingManager.REMINDER_MINUTES} minutes in the app",
                                    icon = Icons.Filled.Spa,
                                    iconTint = GreenSuccess,
                                    checked = uiState.wellbeingEnabled,
                                    onCheckedChange = { viewModel.setWellbeing(it) }
                                )
                                SettingsDivider()
                                SettingsRowSoon("Language", "Marathi and Hindi are on the way", Icons.Filled.Language, BlueInfo)
                                SettingsDivider()
                                SettingsRowSoon("Content Preferences", "Pick the areas and topics you care about", Icons.Filled.Tune, RedAlert)
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                // ── STORAGE ─────────────────────────────────────────────────
                item(key = "storage") {
                    StaggerIn(5) {
                        Column {
                            SectionHeader("STORAGE")
                            SettingsGroup {
                                SettingsRowAction(
                                    "Clear Cache", "Free up temporary app and image data",
                                    Icons.Filled.Delete, RedAlert, badge = uiState.cacheSize
                                ) {
                                    haptic.success()
                                    viewModel.clearCache()
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                // ── ABOUT ───────────────────────────────────────────────────
                item(key = "about") {
                    StaggerIn(6) {
                        Column {
                            SectionHeader("ABOUT")
                            SettingsGroup {
                                SettingsRow("About NagpurPulse", "App info and version", Icons.Filled.Info, BlueInfo) { showAbout = true }
                                SettingsDivider()
                                SettingsRow("Help & Support", "FAQs, guides and contact support", Icons.Filled.HelpOutline, GreenSuccess) { openSupport() }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                // ── Log out ─────────────────────────────────────────────────
                item(key = "logout") {
                    StaggerIn(7) {
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
                    }
                }

                item(key = "footer") {
                    SettingsFooter(appVersion) {
                        haptic.success()
                        scope.launch { snackbar.showSnackbar("You're part of the pulse of Nagpur 🧡") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ── Hero profile card ─────────────────────────────────────────────────────────

@Composable
private fun ProfileHeroCard(
    profile: Profile?,
    loading: Boolean,
    level: LevelProgress,
    streakDays: Int,
    streakAtRisk: Boolean,
    completeness: Completeness?,
    onOpenProfile: () -> Unit,
    onImprove: () -> Unit
) {
    val context = LocalContext.current
    val ringStart = Color(level.level.colorStart)
    val ringEnd = Color(level.level.colorEnd)
    val progress by animateFloatAsState(
        targetValue = level.fraction,
        animationSpec = tween(900),
        label = "level_ring"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, ringStart.copy(0.28f), RoundedCornerShape(20.dp))
    ) {
        if (profile == null) {
            // Skeleton (shimmer) while the profile loads, or a quiet fallback if it failed.
            Row(
                Modifier.fillMaxWidth().padding(DensityManager.cardPadding.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(68.dp).clip(CircleShape).shimmerEffect(RoundedCornerShape(50)))
                Spacer(Modifier.width(14.dp))
                Column {
                    Box(Modifier.width(140.dp).height(16.dp).shimmerEffect(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(100.dp).height(12.dp).shimmerEffect(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(170.dp).height(12.dp).shimmerEffect(RoundedCornerShape(8.dp)))
                }
            }
            if (!loading) {
                Text(
                    "Couldn't load your profile. Pull back and try again in a moment.",
                    color = TertiaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                )
            }
            return@Column
        }

        Row(
            Modifier
                .fillMaxWidth()
                .pressScale(onClick = onOpenProfile)
                .padding(DensityManager.cardPadding.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with level-progress ring
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = ringStart.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        brush = Brush.linearGradient(listOf(ringStart, ringEnd)),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(profile.avatarUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.username.ifBlank { "User" },
                        color = PrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (profile.isVerified) {
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
                Text(
                    profile.displayName?.takeIf { it.isNotBlank() } ?: "NagpurPulse member",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = TertiaryText, modifier = Modifier.size(11.dp))
                    Text(
                        " ${profile.areas.firstOrNull() ?: profile.location?.takeIf { it.isNotBlank() } ?: "Location not set"}",
                        color = TertiaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    MiniChip("${level.level.emoji} ${level.level.title}", ringStart)
                    if (streakDays > 0) MiniChip("🔥 $streakDays-day streak", YellowWarn)
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(ringStart.copy(0.15f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(ringStart, ringEnd)))
                    )
                }
                Spacer(Modifier.height(4.dp))
                val next = level.next
                Text(
                    when {
                        next != null && streakAtRisk && streakDays > 0 ->
                            "One post or comment today keeps your streak going"
                        next != null ->
                            "${level.remaining} Pulse Points to ${next.emoji} ${next.title}"
                        else -> "You've reached the top level 👑"
                    },
                    color = if (streakAtRisk && streakDays > 0) OrangePrimary else TertiaryText,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TertiaryText, modifier = Modifier.size(16.dp))
        }

        // Profile completeness nudge
        if (completeness != null && !completeness.isComplete) {
            HorizontalDivider(color = Divider.copy(0.6f), thickness = 0.5.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressScale(onClick = onImprove)
                    .padding(horizontal = DensityManager.cardPadding.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(OrangePrimary.copy(0.12f)),
                    Alignment.Center
                ) {
                    Text("${completeness.percent}%", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Complete your profile", color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        completeness.next?.label ?: "",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun MiniChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(0.15f))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// ── Setup checklist + celebration ─────────────────────────────────────────────

@Composable
private fun SetupCard(items: List<SetupItem>) {
    val done = items.count { it.done }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, OrangePrimary.copy(0.25f), RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("Finish setting up", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "$done of ${items.size} done. Each step makes NagpurPulse feel more like yours.",
                color = SecondaryText,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items.forEach { step ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (step.done) OrangePrimary else OrangePrimary.copy(0.15f))
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        items.forEach { step ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (step.done) Modifier else Modifier.pressScale(onClick = step.onClick))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (step.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (step.done) GreenSuccess else TertiaryText,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    step.label,
                    color = if (step.done) TertiaryText else PrimaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (!step.done) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = OrangePrimary, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun SetupCelebrationCard(onDismiss: () -> Unit) {
    val haptic = rememberHaptic()
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        haptic.success()
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(OrangePrimary.copy(0.28f), PurpleNight.copy(0.20f))))
            .border(1.dp, OrangePrimary.copy(0.4f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎉", fontSize = 44.sp, modifier = Modifier.scale(pop.value))
        Spacer(Modifier.height(8.dp))
        Text("You're all set!", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Your NagpurPulse is ready. Welcome to the pulse of Nagpur.",
            color = SecondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(OrangePrimary)
                .pressScale(onClick = onDismiss)
                .padding(horizontal = 28.dp, vertical = 10.dp)
        ) {
            Text("Nice!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ── Search bar + entrance animation ───────────────────────────────────────────

@Composable
private fun SettingsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = TertiaryText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search settings", color = TertiaryText, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = PrimaryText, fontSize = 14.sp),
                cursorBrush = SolidColor(OrangePrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear search",
                tint = TertiaryText,
                modifier = Modifier.size(18.dp).clickable { onQueryChange("") }
            )
        }
    }
}

/** Fades + slides content in with a small stagger. Items far down the list appear instantly. */
@Composable
private fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(index > 7) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay(index * 45L)
            visible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 8 }
    ) {
        content()
    }
}

// ── Appearance (Dark / Light / System) ────────────────────────────────────────

@Composable
private fun AppearanceRow(mode: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(PurpleNight.copy(0.12f)), Alignment.Center) {
                Icon(Icons.Filled.Palette, null, tint = PurpleNight, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Appearance", color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text("Dark, light, or follow your phone", color = TertiaryText, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceAlt)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppearanceSegment("Dark", Icons.Filled.DarkMode, mode == "dark", Modifier.weight(1f)) { onSelect("dark") }
            AppearanceSegment("Light", Icons.Filled.LightMode, mode == "light", Modifier.weight(1f)) { onSelect("light") }
            AppearanceSegment("System", Icons.Filled.SettingsBrightness, mode == "system", Modifier.weight(1f)) { onSelect("system") }
        }
    }
}

@Composable
private fun AppearanceSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) OrangePrimary.copy(0.18f) else Color.Transparent)
            .border(1.dp, if (selected) OrangePrimary.copy(0.55f) else Color.Transparent, RoundedCornerShape(11.dp))
            .pressScale(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) OrangePrimary else SecondaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) OrangePrimary else SecondaryText,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ── Footer ────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsFooter(version: String, onLongPress: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("NagpurPulse v$version", color = TertiaryText, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text("Made with 🧡 in Nagpur", color = TertiaryText, fontSize = 11.sp)
    }
}

// ── Admin Panel Entry Card (NEW) ──────────────────────────────────────────────

@Composable
private fun AdminPanelSettingsCard(role: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
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
                        color      = PrimaryText,
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
                    color    = SecondaryText,
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

// ── Shared Settings composables ───────────────────────────────────────────────

@Composable
fun SectionHeader(text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(50))
                .background(OrangePrimary)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            color        = TertiaryText,
            fontSize     = 11.sp,
            fontWeight   = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
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

/** A row for something that is planned but not built yet. Not tappable, so it can never be a dead end. */
@Composable
fun SettingsRowSoon(label: String, sub: String, icon: ImageVector, iconTint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(iconTint.copy(0.08f)), Alignment.Center) {
            Icon(icon, null, tint = iconTint.copy(0.5f), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = SecondaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TertiaryText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(OrangePrimary.copy(0.12f))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text("Soon", color = OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
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
        if (badge.isNotBlank()) {
            Text(badge, color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = TertiaryText, modifier = Modifier.size(14.dp))
    }
}

/** The whole row is the tap target (not just the switch), with a light haptic tick. */
@Composable
fun SettingsRowToggle(label: String, sub: String, icon: ImageVector, iconTint: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptic = rememberHaptic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = {
                    haptic.toggle(it)
                    onCheckedChange(it)
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
            onCheckedChange = null,
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = OrangePrimary,
                checkedBorderColor   = OrangePrimary,
                uncheckedThumbColor  = SecondaryText,
                uncheckedTrackColor  = SurfaceAlt,
                uncheckedBorderColor = TertiaryText.copy(alpha = 0.6f)
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
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, color.copy(0.12f), RoundedCornerShape(14.dp))
            .pressScale(onClick = onClick)
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
        Text(label, color = TertiaryText, fontSize = 10.sp, maxLines = 1)
    }
}
