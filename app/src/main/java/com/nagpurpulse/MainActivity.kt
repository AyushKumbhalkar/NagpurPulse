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
import com.nagpurpulse.data.repository.PresenceRepository
import com.nagpurpulse.data.repository.UserPreferencesRepository
import com.nagpurpulse.notifications.NotifDeepLink
import com.nagpurpulse.notifications.NotifPrefsHelper
import com.nagpurpulse.ui.navigation.NagpurPulseNavGraph
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.preferences.PreferenceManager
import com.nagpurpulse.ui.screens.settings.utilis.LockScreen
import com.nagpurpulse.ui.theme.NagpurPulseTheme
import com.nagpurpulse.ui.theme.ThemeManager
import com.nagpurpulse.ui.theme.ThemeTransitionOverlay
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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

        enableEdgeToEdge()

        // Load saved theme BEFORE Compose starts
        val savedTheme = userPreferencesRepository.getSavedTheme(this)
        ThemeManager.isLightTheme = !savedTheme

        android.util.Log.d(
            "THEME_STARTUP",
            "Saved AMOLED = $savedTheme | isLightTheme = ${ThemeManager.isLightTheme}"
        )

        // Capture post_id if launched by notification tap
        intent?.getStringExtra("post_id")?.let {
            NotifDeepLink.pendingPostId.value = it
        }

        setContent {
            // Load display preferences
            LaunchedEffect(Unit) {
                PreferenceManager.updateTextSize(userPreferencesRepository.getTextSize())
                DensityManager.density = userPreferencesRepository.getDisplayDensity()
                FeedLayoutManager.feedStyle = userPreferencesRepository.getFeedStyle()

                // Restore saved theme
                val amoledMode = userPreferencesRepository.getAmoledMode()
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

                            // ── Deep link collector ───────────────────────────
                            // Handles both cold-start (set in onCreate above)
                            // and warm-start (set in onNewIntent below)
                            LaunchedEffect(navController) {
                                NotifDeepLink.pendingPostId.collect { postId ->
                                    if (postId != null) {
                                        navController.navigate(Screen.Thread.createRoute(postId)) {
                                            launchSingleTop = true
                                        }
                                        NotifDeepLink.pendingPostId.value = null
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
        intent.getStringExtra("post_id")?.let {
            NotifDeepLink.pendingPostId.value = it
        }
    }
}
