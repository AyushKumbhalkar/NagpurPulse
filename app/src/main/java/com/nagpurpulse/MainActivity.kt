// MainActivity.kt  — REPLACE entirely
package com.nagpurpulse

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.remote.SupabaseClientProvider
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.notifications.NotifDeepLink
import com.nagpurpulse.ui.navigation.NagpurPulseNavGraph
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.preferences.PreferenceManager
import com.nagpurpulse.ui.screens.settings.utilis.LockScreen
import com.nagpurpulse.ui.theme.NagpurPulseTheme
import com.nagpurpulse.ui.theme.ThemeManager
import com.nagpurpulse.ui.theme.ThemeTransitionOverlay
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private val pendingAuthRecovery = kotlinx.coroutines.flow.MutableStateFlow(false)

private fun isPasswordRecoveryCallback(intent: Intent?): Boolean {
    val uri = intent?.data ?: return false
    if (uri.scheme != "nagpurpulse" || uri.host != "auth") return false

    // Supabase callback parameters may be in the query or fragment.
    val parameters = listOfNotNull(uri.query, uri.fragment)
        .flatMap { it.split('&') }
        .mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) null
            else part.substring(0, separator) to part.substring(separator + 1)
        }

    return parameters.any { (key, value) ->
        key.equals("type", ignoreCase = true) &&
            value.equals("recovery", ignoreCase = true)
    }
}

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var presenceRepository: PresenceRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    // ── Deep link from cold-start tap on a notification ───────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {

        val amoledMode = getSharedPreferences(
            "theme_prefs",
            MODE_PRIVATE
        ).getBoolean("amoled_mode", false)

        if (amoledMode) {
            setTheme(R.style.Theme_NagpurPulse_Dark)
        } else {
            setTheme(R.style.Theme_NagpurPulse_Light)
        }

        super.onCreate(savedInstanceState)

        // Let supabase-kt parse and import auth/OTP callback sessions from deep links.
        SupabaseClientProvider.client.handleDeeplinks(intent)
        if (isPasswordRecoveryCallback(intent)) {
            pendingAuthRecovery.value = true
        }

        enableEdgeToEdge()

        // Load saved theme BEFORE Compose starts
        val savedTheme = userPreferencesRepository.getSavedTheme(this)
        ThemeManager.isLightTheme = !savedTheme

        android.util.Log.d(
            "THEME_STARTUP",
            "Saved AMOLED = $savedTheme | isLightTheme = ${ThemeManager.isLightTheme}"
        )

        // Set comment first so the combined deep-link collector sees the full destination.
        intent?.getStringExtra("comment_id")?.let {
            NotifDeepLink.pendingCommentId.value = it
        }
        intent?.getStringExtra("post_id")?.let {
            NotifDeepLink.pendingPostId.value = it
        }
        intent?.getStringExtra("conversation_id")?.takeIf { it.isNotBlank() }?.let {
            NotifDeepLink.pendingConversationId.value = it
        }

        setContent {
            // Load display preferences
            LaunchedEffect(Unit) {
                PreferenceManager.updateTextSize(userPreferencesRepository.getTextSize())
                DensityManager.density = userPreferencesRepository.getDisplayDensity()
                FeedLayoutManager.feedStyle = userPreferencesRepository.getFeedStyle()

                // Restore saved theme
                val amoledMode = userPreferencesRepository.getAmoledMode(applicationContext)
                ThemeManager.isLightTheme = !amoledMode

                // Sync notification prefs to SharedPrefs (so FCM service reads correct values)
                userPreferencesRepository.loadAndSyncNotifPrefs(applicationContext)
            }

            val context = LocalContext.current
            val biometricEnabled = remember {
                context.getSharedPreferences("security", android.content.Context.MODE_PRIVATE)
                    .getBoolean("biometric_enabled", false)
            }
            var authenticated by remember { mutableStateOf(!biometricEnabled) }

            NagpurPulseTheme(darkTheme = !ThemeManager.isLightTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        if (!authenticated) {
                            LockScreen { authenticated = true }
                        } else {
                            val navController = rememberNavController()

                            LaunchedEffect(navController, "auth-recovery-deeplink") {
                                pendingAuthRecovery.collect { shouldOpenRecovery ->
                                    if (shouldOpenRecovery) {
                                        navController.currentBackStackEntryFlow.first {
                                            it.destination.route != null &&
                                                it.destination.route != Screen.Splash.route
                                        }
                                        navController.navigate(Screen.PasswordRecovery.route) {
                                            launchSingleTop = true
                                        }
                                        pendingAuthRecovery.value = false
                                    }
                                }
                            }

                            // ── Deep link collector ───────────────────────────
                            // Handles both cold-start (set in onCreate above)
                            // and warm-start (set in onNewIntent below)
                            LaunchedEffect(navController) {
                                combine(
                                    NotifDeepLink.pendingPostId,
                                    NotifDeepLink.pendingCommentId
                                ) { postId, commentId -> postId to commentId }.collect { (postId, commentId) ->
                                    if (postId != null) {
                                        // Wait for the NavHost to finish Splash -> Home before handling a cold-start tap.
                                        navController.currentBackStackEntryFlow.first {
                                            it.destination.route != null && it.destination.route != Screen.Splash.route
                                        }
                                        navController.navigate(Screen.Thread.createRoute(postId, commentId)) {
                                            launchSingleTop = true
                                        }
                                        NotifDeepLink.pendingPostId.value = null
                                        NotifDeepLink.pendingCommentId.value = null
                                    }
                                }
                            }

                            LaunchedEffect(navController, "conversation-notification-deeplink") {
                                NotifDeepLink.pendingConversationId.collect { conversationId ->
                                    if (!conversationId.isNullOrBlank()) {
                                        // Wait for the NavHost to finish Splash -> Home before handling a cold-start tap.
                                        navController.currentBackStackEntryFlow.first {
                                            it.destination.route != null && it.destination.route != Screen.Splash.route
                                        }
                                        navController.navigate(Screen.Chat.createRoute(conversationId)) {
                                            launchSingleTop = true
                                        }
                                        NotifDeepLink.pendingConversationId.value = null
                                    } else if (conversationId != null) {
                                        // Ignore malformed notification intents; "chat/" is not a valid route.
                                        NotifDeepLink.pendingConversationId.value = null
                                    }
                                }
                            }

                            NagpurPulseNavGraph(
                                navController    = navController,
                                startDestination = Screen.Splash.route,
                                authRepository   = authRepository
                            )
                        }
                    }
                    ThemeTransitionOverlay()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        presenceRepository.start()
    }

    override fun onStop() {
        presenceRepository.stop()
        super.onStop()
    }

    // ── Deep link from warm-start tap (app already running) ───────────────────
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Process warm-start auth callbacks as well as notification deep links.
        SupabaseClientProvider.client.handleDeeplinks(intent)
        if (isPasswordRecoveryCallback(intent)) {
            pendingAuthRecovery.value = true
        }
        intent.getStringExtra("comment_id")?.let {
            NotifDeepLink.pendingCommentId.value = it
        }
        intent.getStringExtra("post_id")?.let {
            NotifDeepLink.pendingPostId.value = it
        }
        intent.getStringExtra("conversation_id")?.takeIf { it.isNotBlank() }?.let {
            NotifDeepLink.pendingConversationId.value = it
        }
    }
}
