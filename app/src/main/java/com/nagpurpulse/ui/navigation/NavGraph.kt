// ui/navigation/NavGraph.kt
// CHANGES: 1) Added Screen.AdminPanel object  2) Added AdminPanelScreen composable at bottom
// Everything else is identical to the original.

package com.nagpurpulse.ui.navigation

import androidx.compose.material3.*
import com.nagpurpulse.ui.screens.admin.AdminPanelScreen          // NEW
import com.nagpurpulse.ui.screens.explore.CategoryPostsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.nagpurpulse.ui.screens.settings.DisplayDensityScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
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

@Composable
fun NagpurPulseNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    authRepository: AuthRepository
) {

    var selectedGender by remember { mutableStateOf<String?>(null) }
    var selectedUsername by remember { mutableStateOf<String?>(null) }
    var selectedAvatar by remember { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }
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
            SplashScreen(onFinished = {
                val dest = if (authRepository.isLoggedIn())
                    Screen.Home.route
                else
                    Screen.Signup.route
                navController.navigate(dest) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        // Onboarding
        composable(
            Screen.Onboarding.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) }) {
            OnboardingScreen(
                onGetStarted = { navController.navigate(Screen.Signup.route) },
                onLogin = { navController.navigate(Screen.Login.route) },
                onGuestMode = {
                    authRepository.enterGuestMode()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Auth
        composable(
            Screen.Login.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onGoogleNewUser = {
                    navController.navigate(Screen.Identity.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(
            Screen.Signup.route,
            enterTransition = { tabEnter(this) }, exitTransition = { tabExit(this) },
            popEnterTransition = { tabEnter(this) }, popExitTransition = { tabExit(this) }) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Identity.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onGuestContinue = {
                    android.util.Log.d("GUEST_FLOW", "SIGNUP GUEST CLICKED")
                    navController.navigate(Screen.ProfilePicture.route)
                },
                onExistingGoogleUser = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Screen.Identity.route,
            enterTransition = { slideEnter(this) },
            exitTransition = { slideExit(this) },
            popEnterTransition = { slidePopEnter(this) },
            popExitTransition = { slidePopExit(this) }) {
            IdentityScreen(
                onBack = { navController.popBackStack() },
                onContinue = { gender ->
                    selectedGender = gender
                    navController.navigate(Screen.Username.route)
                }
            )
        }

        composable(
            Screen.Username.route,
            enterTransition = { slideEnter(this) },
            exitTransition = { slideExit(this) },
            popEnterTransition = { slidePopEnter(this) },
            popExitTransition = { slidePopExit(this) }) {
            UsernameScreen(
                onBack = { navController.popBackStack() },
                onNext = { username ->
                    selectedUsername = username
                    navController.navigate(Screen.ProfilePicture.route)
                }
            )
        }

        composable(
            Screen.ProfilePicture.route,
            enterTransition = { slideEnter(this) },
            exitTransition = { slideExit(this) },
            popEnterTransition = { slidePopEnter(this) },
            popExitTransition = { slidePopExit(this) }) {
            ProfilePictureScreen(
                onBack = { navController.popBackStack() },
                isSaving = isSavingProfile,
                onContinue = { avatarUrl ->
                    isSavingProfile = true
                    selectedAvatar = avatarUrl
                    android.util.Log.d(
                        "ONBOARDING",
                        "Gender=$selectedGender Username=$selectedUsername Avatar=$selectedAvatar"
                    )
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        val userId = authRepository.currentUserId
                        if (userId == null) {
                            authRepository.enterGuestMode(avatarUrl = avatarUrl)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                            return@launch
                        }
                        if (!selectedUsername.isNullOrBlank()) {
                            android.util.Log.e("AYUSH_TEST", "BEFORE createProfileIfMissing")
                            authRepository.createProfileIfMissing(
                                userId = userId,
                                username = selectedUsername ?: "NagpurUser",
                                avatarUrl = selectedAvatar,
                                gender = selectedGender
                            )
                            android.util.Log.e("AYUSH_TEST", "AFTER createProfileIfMissing")
                            android.util.Log.d("ONBOARDING", "About to save profile")
                            val result = authRepository.updateFullProfile(
                                userId = userId,
                                displayName = selectedUsername,
                                avatarUrl = selectedAvatar,
                                gender = selectedGender
                            )
                            android.util.Log.d("ONBOARDING", "updateFullProfile returned")
                            result.fold(
                                onSuccess = {
                                    android.util.Log.d("ONBOARDING", "PROFILE SAVED")
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Signup.route) { inclusive = true }
                                    }
                                },
                                onFailure = {
                                    isSavingProfile = false
                                    android.util.Log.e(
                                        "ONBOARDING",
                                        "SAVE FAILED: ${it.message}",
                                        it
                                    )
                                }
                            )
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

        composable(Screen.Notifications.route) {
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
            PublicProfileScreen(
                userId = back.arguments?.getString("userId") ?: "",
                onBack = { navController.popBackStack() },
                onPostClick = { navController.navigate(Screen.Thread.createRoute(it)) }
            )
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

        // ── Settings ──────────────────────────────────────────────────────────
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.AccountProfile.route) {
            AccountProfileScreen(navController = navController)
        }

        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(navController = navController)
        }

        composable(Screen.IncognitoSettings.route) {
            IncognitoSettingsScreen(navController = navController)
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(navController = navController)
        }

        composable(Screen.NotifSettings.route) {
            NotifSettingsScreen(navController = navController)
        }

        composable(Screen.TextSize.route) {
            TextSizeScreen(navController = navController)
        }

        composable(Screen.DisplayDensity.route) {
            DisplayDensityScreen(navController = navController)
        }

        // ── Admin Panel (NEW) ──────────────────────────────────────────────────
        composable(
            route = Screen.AdminPanel.route,
            enterTransition = { sheetEnter(this) },
            exitTransition = { sheetExit(this) },
            popExitTransition = { sheetExit(this) }
        ) {
            AdminPanelScreen(navController = navController)
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
