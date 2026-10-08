@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.nagpurpulse.ui.screens.auth

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue


import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nagpurpulse.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.OrangePrimary
import kotlinx.coroutines.launch


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onGoogleNewUser: () -> Unit,
    onGuestContinue: () -> Unit = onLoginSuccess,
    viewModel: AuthViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val loginPrefs = remember(context) {
        context.getSharedPreferences("login_prefs", android.content.Context.MODE_PRIVATE)
    }
    var email by remember {
        mutableStateOf(loginPrefs.getString("remembered_email", "").orEmpty())
    }
    var password by remember { mutableStateOf("") }
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var googleBusy by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val lastMethod = remember { loginPrefs.getString("last_method", "").orEmpty() }
    fun rememberMethod(method: String) { loginPrefs.edit().putString("last_method", method).apply() }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { AuthAnalytics.log(context, "login_view") }

    val isDarkTheme = LocalIsDarkTheme.current
    val colors = com.nagpurpulse.ui.theme.authPalette()
    val ink = colors.ink
    val muted = colors.muted
    val emailLooksValid = email.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val emailError = if (emailTouched && !emailLooksValid) {
        stringResource(R.string.login_err_email_invalid)
    } else null
    val passwordError = if (passwordTouched && password.isBlank()) {
        stringResource(R.string.auth_err_enter_password)
    } else null
    val passwordFocusRequester = remember { FocusRequester() }

    fun persistEmailIfValid() {
        val normalized = email.trim().lowercase(java.util.Locale.ROOT)
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
            loginPrefs.edit().putString("remembered_email", normalized).apply()
        }
    }

    fun submitLogin() {
        emailTouched = true
        passwordTouched = true
        persistEmailIfValid()
        if (emailLooksValid && password.isNotBlank() && !uiState.isLoading) {
            viewModel.signIn(email.trim(), password) { rememberMethod("email"); onLoginSuccess() }
        }
    }

    val isOnline by rememberIsOnline()
    LoginContent(
        email = email, password = password, loading = uiState.isLoading || googleBusy,
        online = isOnline, emailError = emailError, passwordError = passwordError,
        error = if (uiState.loginCooldownSeconds > 0)
            stringResource(R.string.auth_err_login_cooldown, uiState.loginCooldownSeconds) else uiState.error,
        info = if (uiState.forgotPasswordSent) stringResource(R.string.login_reset_sent) else uiState.infoMessage,
        verificationRequired = uiState.emailVerificationRequired,
        lastMethod = lastMethod,
        onEmail = { email = it }, onPassword = { password = it },
        onEmailBlur = { emailTouched = true }, onPasswordBlur = { passwordTouched = true },
        onLogin = { submitLogin() },
        onForgot = {
            AuthAnalytics.log(context, "login_forgot_password_tap")
            showForgotPasswordDialog = true
        },
        onResend = {
            AuthAnalytics.log(context, "login_resend_verification_tap")
            viewModel.resendSignupEmailOtp(email) {}
        },
        onGoogle = {
            if (!googleBusy && !uiState.isLoading) scope.launch {
                googleBusy = true
                AuthAnalytics.log(context, "login_google_tap")
                try { when (val outcome = GoogleAuthManager(context).signIn()) {
                    is GoogleSignInOutcome.Success -> viewModel.signInWithGoogleToken(
                        idToken = outcome.idToken, nonce = outcome.rawNonce,
                        onExistingUser = { rememberMethod("google"); onLoginSuccess() },
                        onNewUser = { rememberMethod("google"); onGoogleNewUser() })
                    GoogleSignInOutcome.Cancelled -> Unit
                    is GoogleSignInOutcome.Failure -> viewModel.showError(outcome.message)
                } } finally { googleBusy = false }
            }
        },
        onSignup = onNavigateToSignup,
        onGuest = { AuthAnalytics.log(context, "login_guest_tap"); onGuestContinue() }
    )

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            isLoading = uiState.isLoading,
            sent = uiState.forgotPasswordSent,
            error = uiState.error,
            onSend = { viewModel.sendPasswordReset(it) },
            onDismiss = { showForgotPasswordDialog = false }
        )
    }
}
