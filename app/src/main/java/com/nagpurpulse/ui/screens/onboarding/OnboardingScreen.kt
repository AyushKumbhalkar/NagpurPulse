package com.nagpurpulse.ui.screens.onboarding

import androidx.compose.runtime.Composable
import com.nagpurpulse.ui.screens.auth.WelcomeContent

/** Existing route and callback contract; the welcome UI shares the auth layout. */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    onGuestMode: () -> Unit = {}
) {
    WelcomeContent(onGetStarted = onGetStarted, onLogin = onLogin)
}
