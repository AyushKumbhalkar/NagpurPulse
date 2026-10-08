// ui/navigation/NavGraph.kt
// CHANGES: 1) Added Screen.AdminPanel object  2) Added AdminPanelScreen composable at bottom
// Everything else is identical to the original.

package com.nagpurpulse.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nagpurpulse.ui.screens.admin.AdminPanelScreen          // NEW
import com.nagpurpulse.ui.screens.explore.CategoryPostsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nagpurpulse.ui.screens.settings.DisplayDensityScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.ui.screens.alerts.AlertsScreen
import com.nagpurpulse.ui.screens.auth.LoginScreen
import com.nagpurpulse.ui.screens.auth.SignupScreen
import com.nagpurpulse.ui.screens.explore.ExploreScreen
import com.nagpurpulse.ui.screens.home.HomeScreen
import com.nagpurpulse.ui.screens.messages.ChatScreen
import com.nagpurpulse.ui.screens.messages.MessagesScreen
import com.nagpurpulse.ui.screens.notifications.NotificationsScreen
import com.nagpurpulse.ui.screens.onboarding.OnboardingScreen
import com.nagpurpulse.ui.screens.onboarding.IdentityScreen
import com.nagpurpulse.ui.screens.onboarding.UsernameScreen
import com.nagpurpulse.ui.screens.onboarding.ProfilePictureScreen
import com.nagpurpulse.ui.screens.profile.ProfileScreen
import com.nagpurpulse.ui.screens.profile.PublicProfileScreen
import com.nagpurpulse.ui.screens.search.UserSearchScreen
import com.nagpurpulse.ui.screens.settings.*
import com.nagpurpulse.ui.screens.splash.SplashScreen
import com.nagpurpulse.ui.screens.thread.CreateThreadScreen
import com.nagpurpulse.ui.screens.thread.ThreadDetailScreen

// ── All routes ────────────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Splash            : Screen("splash")
    object Onboarding        : Screen("onboarding")
    object Login             : Screen("login")
    object PasswordRecovery  : Screen("password_recovery")
    object Signup            : Screen("signup")
    object Identity          : Screen("identity")
    object Username          : Screen("username")
    object ProfilePicture    : Screen("profile_picture")
    object Home              : Screen("home")
    object Explore           : Screen("explore")
    object CategoryPosts     : Screen("category/{category}") {
        fun createRoute(category: String) = "category/$category"
    }
    object Alerts            : Screen("alerts")
    object Profile           : Screen("profile")
    object Messages          : Screen("messages")
    object Notifications     : Screen("notifications")
    object CreatePost : Screen("create_post?postType={postType}&postId={postId}") {

        fun createRoute(postType: String = "normal") =
            "create_post?postType=$postType"

        fun createEditRoute(postId: String) =
            "create_post?postType=normal&postId=$postId"
    }
    object UserSearch        : Screen("user_search")
    // Settings
    object Settings          : Screen("settings")
    object AccountProfile    : Screen("settings/account_profile")
    object PrivacySettings   : Screen("settings/privacy")
    object IncognitoSettings : Screen("settings/incognito")
    object SecuritySettings  : Screen("settings/security")
    object NotifSettings     : Screen("settings/notifications")
    object TextSize          : Screen("settings/text_size")
    object DisplayDensity    : Screen("settings/display_density")
    // Detail screens
    object Thread : Screen("thread/{postId}?commentId={commentId}") {

        fun createRoute(
            postId: String,
            commentId: String? = null
        ): String {
            return if (commentId.isNullOrBlank()) {
                "thread/$postId"
            } else {
                "thread/$postId?commentId=$commentId"
            }
        }
    }
    object UserProfile       : Screen("user_profile/{userId}") {
        fun createRoute(id: String) = "user_profile/$id"
    }
    object Chat              : Screen("chat/{conversationId}") {
        fun createRoute(id: String) = "chat/$id"
    }

    // ── NEW ───────────────────────────────────────────────────────────────────
    object AdminPanel        : Screen("admin_panel")
    // ─────────────────────────────────────────────────────────────────────────
}

// ── Transitions ───────────────────────────────────────────────────────────────

private val slideEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { it / 3 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(animationSpec = tween(260))
}

private val slideExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { -it / 4 },
        animationSpec = tween(220)
    ) + fadeOut(animationSpec = tween(180))
}

private val slidePopEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { -it / 4 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(animationSpec = tween(240))
}

private val slidePopExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { it / 3 },
        animationSpec = tween(240)
    ) + fadeOut(animationSpec = tween(200))
}

private val tabEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(200))
}

private val tabExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(160))
}

private val sheetEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    ) + fadeIn(animationSpec = tween(280))
}

private val sheetExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(260)
    ) + fadeOut(animationSpec = tween(200))
}

// Simple, consistent animation for the four-step guest/onboarding flow.
private val onboardingStepEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { it / 8 },
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(240))
}

private val onboardingStepExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { -it / 12 },
        animationSpec = tween(240, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(180))
}

private val onboardingStepPopEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { -it / 12 },
        animationSpec = tween(240, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(200))
}

private val onboardingStepPopExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { it / 8 },
        animationSpec = tween(280, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(200))
}

@Composable
fun NagpurPulseNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    authRepository: AuthRepository
) {

    var selectedGender by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAvatar by rememberSaveable { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }
    var profileSaveError by rememberSaveable { mutableStateOf<String?>(null) }
    var onboardingOriginRoute by rememberSaveable { mutableStateOf(Screen.Signup.route) }
    val onboardingScope = rememberCoroutineScope()
    val navContext = androidx.compose.ui.platform.LocalContext.current
    var showLoginDialog by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideEnter(this) },
        exitTransition = { slideExit(this) },
        popEnterTransition = { slidePopEnter(this) },
        popExitTransition = { slidePopExit(this) }
    ) {
        // Splash
        composable(
            Screen.Splash.route,
            enterTransition = { fadeIn(tween(300)) }, exitTransition = { fadeOut(tween(500)) }) {
            // Work out the destination WHILE the splash animation plays, so a returning
            // user never waits on a blank screen and never sees a fixed 2.5s delay.
            var splashAttempt by remember { mutableStateOf(0) }
            var splashTarget by remember { mutableStateOf<String?>(null) }
            var splashFailed by remember { mutableStateOf(false) }
            val splashOnline by com.nagpurpulse.ui.screens.auth.rememberIsOnline()
            LaunchedEffect(splashAttempt) {
                splashFailed = false
                splashTarget = null
                val dest = kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                    if (!authRepository.isLoggedIn()) Screen.Onboarding.route
                    else when (com.nagpurpulse.ui.screens.auth.resolveOnboarding(navContext, authRepository)) {
                        com.nagpurpulse.ui.screens.auth.OnboardingState.Complete -> Screen.Home.route
                        com.nagpurpulse.ui.screens.auth.OnboardingState.Incomplete -> Screen.Identity.route
                        com.nagpurpulse.ui.screens.auth.OnboardingState.Unknown -> null
                    }
                }
                if (dest == null) splashFailed = true else splashTarget = dest
            }
            // Retry by itself as soon as the connection comes back.
            LaunchedEffect(splashFailed, splashOnline) {
                if (splashFailed && splashOnline) {
                    kotlinx.coroutines.delay(600)
                    splashAttempt++
                }
            }
            SplashScreen(
                ready = splashTarget != null,
                failed = splashFailed,
                onRetry = { splashAttempt++ },
                onFinished = {
                    val dest = splashTarget
                    if (dest != null) {
                        if (dest == Screen.Identity.route) {
                            // A restored session may belong to a user who closed the app
                            // before finishing onboarding: resume it, with Identity as origin.
                            onboardingOriginRoute = Screen.Identity.route
                            selectedGender = null
                            selectedUsername = null
                            selectedAvatar = null
                        }
                        navController.navigate(dest) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Onboarding
        composable(
            Screen.Onboarding.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) }) {
            OnboardingScreen(
                onGetStarted = { navController.navigate(Screen.Signup.route) { launchSingleTop = true } },
                onLogin = { navController.navigate(Screen.Login.route) { launchSingleTop = true } },
                onGuestMode = {
                    selectedGender = null
                    selectedUsername = null
                    selectedAvatar = null
                    profileSaveError = null
                    onboardingOriginRoute = Screen.Signup.route
                    authRepository.enterGuestMode()
                    navController.navigate(Screen.Signup.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Password recovery: the Supabase deep-link handler imports the recovery
        // session before this screen calls updateUser through AuthRepository.
        composable(Screen.PasswordRecovery.route) {
            var password by rememberSaveable { mutableStateOf("") }
            var confirmation by rememberSaveable { mutableStateOf("") }
            var saving by remember { mutableStateOf(false) }
            var recoveryError by remember { mutableStateOf<String?>(null) }
            var recoverySuccess by remember { mutableStateOf(false) }
            val recoveryScope = rememberCoroutineScope()

            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Set a new password", style = MaterialTheme.typography.headlineSmall)
                Spacer(androidx.compose.ui.Modifier.height(12.dp))
                Text("Choose a new password for your NagpurPulse account.")
                Spacer(androidx.compose.ui.Modifier.height(20.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; recoveryError = null },
                    label = { Text("New password") },
                    singleLine = true,
                    enabled = !saving && !recoverySuccess,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
                Spacer(androidx.compose.ui.Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; recoveryError = null },
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    enabled = !saving && !recoverySuccess,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
                if (recoveryError != null) {
                    Spacer(androidx.compose.ui.Modifier.height(8.dp))
                    Text(recoveryError!!, color = MaterialTheme.colorScheme.error)
                }
                if (recoverySuccess) {
                    Spacer(androidx.compose.ui.Modifier.height(8.dp))
                    Text("Password updated. You can sign in with your new password.")
                }
                Spacer(androidx.compose.ui.Modifier.height(20.dp))
                Button(
                    enabled = !saving && !recoverySuccess && password.isNotEmpty() && confirmation.isNotEmpty(),
                    onClick = {
                        when {
                            password.length < 8 -> recoveryError = "Use at least 8 characters."
                            password != confirmation -> recoveryError = "Passwords do not match."
                            else -> recoveryScope.launch {
                                saving = true
                                recoveryError = null
                                authRepository.changePassword(password).fold(
                                    onSuccess = { recoverySuccess = true },
                                    onFailure = {
                                        recoveryError = it.message
                                            ?: "The recovery link may have expired. Request a new one and try again."
                                    }
                                )
                                saving = false
                            }
                        }
                    },
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                ) {
                    if (saving) CircularProgressIndicator()
                    else Text("Update password")
                }
                if (recoverySuccess) {
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.PasswordRecovery.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) { Text("Return to sign in") }
                }
            }
        }

        // Auth
        composable(
            Screen.Login.route,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) }) {
            LoginScreen(
                onLoginSuccess = {
                    // A verified account may exist without having finished the
                    // identity/username/profile-picture flow. Never send such a
                    // user directly to Home after a later sign-in.
                    onboardingScope.launch {
                        val onboardingState = com.nagpurpulse.ui.screens.auth.resolveOnboarding(navContext, authRepository)
                        if (onboardingState == com.nagpurpulse.ui.screens.auth.OnboardingState.Unknown) {
                            // Server unreachable and no cached answer: let Splash show Retry.
                            navController.navigate(Screen.Splash.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                            return@launch
                        }
                        if (onboardingState == com.nagpurpulse.ui.screens.auth.OnboardingState.Complete) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            selectedGender = null
                            selectedUsername = null
                            selectedAvatar = null
                            profileSaveError = null
                            // Keep Identity as the onboarding back-stack origin so the
                            // final profile save can clear the interrupted setup flow cleanly.
                            onboardingOriginRoute = Screen.Identity.route
                            navController.navigate(Screen.Identity.route)
                        }
                    }
                },
                onGoogleNewUser = {
                    onboardingOriginRoute = Screen.Login.route
                    navController.navigate(Screen.Identity.route)
                },
                onGuestContinue = {
                    selectedGender = null
                    selectedUsername = null
                    selectedAvatar = null
                    profileSaveError = null
                    onboardingOriginRoute = Screen.Login.route
                    authRepository.enterGuestMode()
                    navController.navigate(Screen.Identity.route)
                },
                onNavigateToSignup = {
                    // Swap Login for Signup so Back leaves the auth flow instead of bouncing between them.
                    navController.navigate(Screen.Signup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            Screen.Signup.route,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) }) {
            SignupScreen(
                onSignupSuccess = {
                    onboardingOriginRoute = Screen.Signup.route
                    navController.navigate(Screen.Identity.route)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGuestContinue = {
                    selectedGender = null
                    selectedUsername = null
                    selectedAvatar = null
                    profileSaveError = null
                    onboardingOriginRoute = Screen.Signup.route
                    authRepository.enterGuestMode()
                    navController.navigate(Screen.Identity.route)
                },
                onExistingGoogleUser = {
                    // Existing Google users also pass through the same onboarding
                    // completion check so interrupted profiles can resume safely.
                    onboardingScope.launch {
                        val onboardingState = com.nagpurpulse.ui.screens.auth.resolveOnboarding(navContext, authRepository)
                        if (onboardingState == com.nagpurpulse.ui.screens.auth.OnboardingState.Unknown) {
                            // Server unreachable and no cached answer: let Splash show Retry.
                            navController.navigate(Screen.Splash.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                            return@launch
                        }
                        if (onboardingState == com.nagpurpulse.ui.screens.auth.OnboardingState.Complete) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Signup.route) { inclusive = true }
                            }
                        } else {
                            onboardingOriginRoute = Screen.Login.route
                            selectedGender = null
                            selectedUsername = null
                            selectedAvatar = null
                            profileSaveError = null
                            // Keep Identity as the onboarding back-stack origin so the
                            // final profile save can clear the interrupted setup flow cleanly.
                            onboardingOriginRoute = Screen.Identity.route
                            navController.navigate(Screen.Identity.route)
                        }
                    }
                }
            )
        }

        composable(
            Screen.Identity.route,
            enterTransition = { onboardingStepEnter(this) },
            exitTransition = { onboardingStepExit(this) },
            popEnterTransition = { onboardingStepPopEnter(this) },
            popExitTransition = { onboardingStepPopExit(this) }) {
            IdentityScreen(
                initialGender = selectedGender,
                onBack = { navController.popBackStack() },
                onContinue = { gender ->
                    selectedGender = gender
                    navController.navigate(Screen.Username.route)
                }
            )
        }

        composable(
            Screen.Username.route,
            enterTransition = { onboardingStepEnter(this) },
            exitTransition = { onboardingStepExit(this) },
            popEnterTransition = { onboardingStepPopEnter(this) },
            popExitTransition = { onboardingStepPopExit(this) }) {
            UsernameScreen(
                initialUsername = selectedUsername,
                checkUsernameAvailable = { candidate -> authRepository.isUsernameAvailable(candidate) },
                onBack = { navController.popBackStack() },
                onNext = { username ->
                    selectedUsername = username
                    navController.navigate(Screen.ProfilePicture.route)
                }
            )
        }

        composable(
            Screen.ProfilePicture.route,
            enterTransition = { onboardingStepEnter(this) },
            exitTransition = { onboardingStepExit(this) },
            popEnterTransition = { onboardingStepPopEnter(this) },
            popExitTransition = { onboardingStepPopExit(this) }) {
            ProfilePictureScreen(
                onBack = { navController.popBackStack() },
                isSaving = isSavingProfile,
                gender = selectedGender,
                saveError = profileSaveError,
                onContinue = { avatarUrl ->
                    if (isSavingProfile) return@ProfilePictureScreen
                    val username = selectedUsername?.trim()
                    val gender = selectedGender
                    if (gender.isNullOrBlank() || username.isNullOrBlank()) {
                        profileSaveError = "Your identity or username is missing. Please go back and complete the previous steps."
                        return@ProfilePictureScreen
                    }

                    isSavingProfile = true
                    profileSaveError = null
                    selectedAvatar = avatarUrl
                    onboardingScope.launch {
                        try {
                            if (authRepository.isGuest) {
                                authRepository.enterGuestMode(
                                    avatarUrl = avatarUrl,
                                    username = username,
                                    gender = gender
                                )
                                isSavingProfile = false
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(onboardingOriginRoute) { inclusive = true }
                                }
                                return@launch
                            }

                            val userId = authRepository.currentUserId
                            if (userId == null) {
                                profileSaveError = "Your session has expired. Please sign in again."
                                isSavingProfile = false
                                return@launch
                            }

                            val result = authRepository.updateFullProfile(
                                userId = userId,
                                username = username,
                                displayName = username,
                                avatarUrl = avatarUrl
                            )
                            result.fold(
                                onSuccess = {
                                    isSavingProfile = false
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(onboardingOriginRoute) { inclusive = true }
                                    }
                                },
                                onFailure = { error ->
                                    profileSaveError = if (error.message?.contains("username", ignoreCase = true) == true) {
                                        "That username is already taken. Go back and choose another."
                                    } else {
                                        "We couldn't save your profile. Check your connection and try again."
                                    }
                                    isSavingProfile = false
                                    android.util.Log.e("ONBOARDING", "Profile save failed", error)
                                }
                            )
                        } catch (error: Exception) {
                            profileSaveError = "We couldn't save your profile. Check your connection and try again."
                            isSavingProfile = false
                            android.util.Log.e("ONBOARDING", "Unexpected profile save failure", error)
                        }
                    }
                }
            )
        }

        // Main tabs
        composable(
            Screen.Home.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            // Fail closed for a restored/unexpected navigation state. Guests are
            // explicitly allowed to browse Home, but an unverified or otherwise
            // unauthenticated email/password session must never render the feed.
            if (!authRepository.isLoggedIn() && !authRepository.isGuest) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                HomeScreen(
                    navController = navController,
                    onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) },
                    onCreatePost = {
                        if (authRepository.isLoggedIn()) {
                            navController.navigate(Screen.CreatePost.route)
                        } else {
                            showLoginDialog = true
                        }
                    },

                    onProfileClick = {
                        if (authRepository.isLoggedIn()) {
                            navController.navigate(Screen.Profile.route)
                        } else {
                            showLoginDialog = true
                        }
                    },
                    onNotifications = { navController.navigate(Screen.Notifications.route) }
                )
            }
        }

        composable(
            Screen.Explore.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            ExploreScreen(
                navController = navController,
                onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) },

                onCreatePost = {
                    if (authRepository.isLoggedIn()) {
                        navController.navigate(Screen.CreatePost.route)
                    } else {
                        showLoginDialog = true
                    }
                },

                onProfileClick = {
                    if (authRepository.isLoggedIn()) {
                        navController.navigate(Screen.Profile.route)
                    } else {
                        showLoginDialog = true
                    }
                }
            )
        }

        composable(
            route = Screen.CategoryPosts.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            CategoryPostsScreen(
                category = backStackEntry.arguments?.getString("category") ?: "",
                navController = navController
            )
        }

        composable(
            Screen.Alerts.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            AlertsScreen(
                navController = navController,

                onPostClick = {
                    navController.navigate(Screen.Thread.createRoute(it))
                },

                onReportAlert = {
                    if (authRepository.isLoggedIn()) {
                        navController.navigate(Screen.CreatePost.createRoute("alert"))
                    } else {
                        showLoginDialog = true
                    }
                },

                onCreatePost = {
                    if (authRepository.isLoggedIn()) {
                        navController.navigate(Screen.CreatePost.route)
                    } else {
                        showLoginDialog = true
                    }
                },

                onProfileClick = {
                    if (authRepository.isLoggedIn()) {
                        navController.navigate(Screen.Profile.route)
                    } else {
                        showLoginDialog = true
                    }
                }
            )
        }

        composable(
            Screen.Messages.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            MessagesScreen(
                navController = navController,
                onCreatePost = {
    if (authRepository.isLoggedIn()) {
        navController.navigate(Screen.CreatePost.route)
    } else {
        showLoginDialog = true
    }
}
            )
        }

        composable(
            Screen.Profile.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            ProfileScreen(
                navController = navController,
                onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) },
                onCreatePost = {
    if (authRepository.isLoggedIn()) {
        navController.navigate(Screen.CreatePost.route)
    } else {
        showLoginDialog = true
    }
},
                onNotifications = { navController.navigate(Screen.Notifications.route) },
                onLogout = {
                    android.util.Log.e("AYUSH_LOGOUT", "LOGOUT CALLBACK EXECUTED")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // The inbox builds its list while opening; use a lightweight fade
        // rather than animating the full screen's position at the same time.
        composable(
            route = Screen.Notifications.route,
            enterTransition = { fadeIn(animationSpec = tween(160)) },
            exitTransition = { fadeOut(animationSpec = tween(120)) },
            popEnterTransition = { fadeIn(animationSpec = tween(160)) },
            popExitTransition = { fadeOut(animationSpec = tween(120)) }
        ) {
            NotificationsScreen(
                navController = navController,
                onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) }
            )
        }

        // Create post (slides up from bottom)
        composable(
            route = Screen.CreatePost.route,
            arguments = listOf(
                navArgument("postType") {
                    type = NavType.StringType
                    defaultValue = "normal"
                },
                navArgument("postId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = { sheetEnter(this) },
            exitTransition = { sheetExit(this) },
            popExitTransition = { sheetExit(this) }
        ) { backStackEntry ->
            CreateThreadScreen(
                postType = backStackEntry.arguments?.getString("postType") ?: "normal",
                editingPostId = backStackEntry.arguments?.getString("postId"),
                onClose = { navController.popBackStack() },
                onPostSuccess = { navController.popBackStack() }
            )
        }

        // Thread detail
        composable(
            route = Screen.Thread.route,
            arguments = listOf(
                navArgument("postId") {
                    type = NavType.StringType
                },
                navArgument("commentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { back ->
            ThreadDetailScreen(
                navController = navController,
                postId = back.arguments?.getString("postId") ?: "",
                commentId = back.arguments?.getString("commentId"),
                onBack = { navController.popBackStack() }
            )
        }

        // Public profile
        composable(
            Screen.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            if (authRepository.isGuest) {
                AlertDialog(
                    onDismissRequest = { navController.popBackStack() },
                    containerColor = Color(0xFF171318),
                    titleContentColor = Color(0xFFF7F3F5),
                    textContentColor = Color(0xFFC7C0CA),
                    shape = RoundedCornerShape(28.dp),
                    icon = {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(54.dp)
                                .background(Color(0xFFFF7A24).copy(alpha = 0.12f), androidx.compose.foundation.shape.CircleShape)
                                .border(1.dp, Color(0xFFFF7A24).copy(alpha = 0.45f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFF7A24))
                        }
                    },
                    title = { Text("Create an account to view profiles", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    text = { Text("You're browsing NagpurPulse as a guest. Sign in or create an account to open member profiles and connect with the community.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A24))
                        ) { Text("Sign in", color = Color.White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text("Keep browsing", color = Color(0xFFC7C0CA))
                        }
                    }
                )
            } else {
                PublicProfileScreen(
                    userId = back.arguments?.getString("userId") ?: "",
                    onBack = { navController.popBackStack() },
                    onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) }
                )
            }
        }

        // Chat
        composable(
            Screen.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { back ->
            ChatScreen(
                conversationId = back.arguments?.getString("conversationId") ?: "",
                onBack = { navController.popBackStack() },
                onOtherProfileClick = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }

        // User search
        composable(Screen.UserSearch.route) {
            UserSearchScreen(navController = navController)
        }

        // Settings flow: fast, gentle slide + fade, with no spring overshoot.
        val settingsEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
            slideInHorizontally(initialOffsetX = { it / 8 }, animationSpec = tween(240)) +
                fadeIn(animationSpec = tween(220))
        }
        val settingsExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 8 }, animationSpec = tween(220)) +
                fadeOut(animationSpec = tween(180))
        }
        val settingsPopEnter: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 8 }, animationSpec = tween(220)) +
                fadeIn(animationSpec = tween(180))
        }
        val settingsPopExit: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 8 }, animationSpec = tween(220)) +
                fadeOut(animationSpec = tween(180))
        }

        // ── Settings ──────────────────────────────────────────────────────────
        composable(
            route = Screen.Settings.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            SettingsScreen(navController = navController)
        }

        composable(
            route = Screen.AccountProfile.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            AccountProfileScreen(navController = navController)
        }

        composable(
            route = Screen.PrivacySettings.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            PrivacySettingsScreen(navController = navController)
        }

        composable(
            route = Screen.IncognitoSettings.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            IncognitoSettingsScreen(navController = navController)
        }

        composable(
            route = Screen.SecuritySettings.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            SecuritySettingsScreen(navController = navController)
        }

        composable(
            route = Screen.NotifSettings.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            NotifSettingsScreen(navController = navController)
        }

        composable(
            route = Screen.TextSize.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            TextSizeScreen(navController = navController)
        }

        composable(
            route = Screen.DisplayDensity.route,
            enterTransition = { settingsEnter(this) },
            exitTransition = { settingsExit(this) },
            popEnterTransition = { settingsPopEnter(this) },
            popExitTransition = { settingsPopExit(this) }
        ) {
            DisplayDensityScreen(navController = navController)
        }

        // ── Admin Panel ────────────────────────────────────────────────────────
        // Fail closed: the route is rendered only after a server-backed
        // admin_roles check confirms the current authenticated user is an admin.
        composable(
            route = Screen.AdminPanel.route,
            // Subtle, non-spring entrance: no overshoot or bounce.
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = tween(durationMillis = 220)
                ) + fadeIn(animationSpec = tween(220))
            },
            exitTransition = { fadeOut(animationSpec = tween(160)) },
            popEnterTransition = { fadeIn(animationSpec = tween(180)) },
            popExitTransition = { fadeOut(animationSpec = tween(160)) }
        ) {
            var adminAccess by remember { mutableStateOf<Boolean?>(null) }

            LaunchedEffect(Unit) {
                adminAccess = authRepository.isCurrentUserAdmin()
            }

            when (adminAccess) {
                true -> AdminPanelScreen(navController = navController)
                false -> {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        // ──────────────────────────────────────────────────────────────────────
    }
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = {
                showLoginDialog = false
            },
            title = {
                Text("Sign in Required")
            },
            text = {
                Text("To continue, please sign in or create an account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLoginDialog = false
                        navController.navigate(Screen.Login.route)
                    }
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLoginDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}
