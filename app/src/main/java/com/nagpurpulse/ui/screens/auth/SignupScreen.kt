@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.nagpurpulse.ui.screens.auth

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagpurpulse.R
import com.nagpurpulse.data.repository.EmailCheck
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.rememberUpdatedState

// Enable only after a separately reviewed repository/API implementation exists.
internal const val PASSWORDLESS_SIGNUP = false

// The keyboard used to open by itself (U4). With Google as the first option, an auto-opened
// keyboard hides it and makes the layout jump, so it is off. Set true to bring U4 back.
private const val AUTOFOCUS_EMAIL = false

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onGuestContinue: () -> Unit = onSignupSuccess,
    onExistingGoogleUser: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // email + dialog flag survive process death (user leaves to read the OTP mail).
    // Passwords are deliberately NOT saved.
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordStep by rememberSaveable { mutableStateOf(false) }

    // Validation UX: errors appear after the user leaves a field, or after they
    // tap Create Account / press Done - never while they are still typing.
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmTouched by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    var showEmailVerificationDialog by rememberSaveable { mutableStateOf(false) }
    var showEmailAlreadyUsedDialog by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var verificationSeconds by remember { mutableStateOf(48) }
    var verificationRateLimited by remember { mutableStateOf(false) }
    // Remembers the last code that was auto-submitted so the same wrong code
    // can never be sent to the server again and again.
    var lastSubmittedCode by remember { mutableStateOf("") }
    // U6: success tick shown after the OTP is verified, before moving on.
    var showSuccess by remember { mutableStateOf(false) }
    // U2/U3: true from the moment Create Account is tapped until loading ends.
    var googleBusy by remember { mutableStateOf(false) }
    var submitInFlight by remember { mutableStateOf(false) }
    // True while "Next" is asking the server whether this email is already registered.
    var checkingEmail by remember { mutableStateOf(false) }
    // U5: after the entrance animation has played once (also survives rotation) skip it.
    var entrancePlayed by rememberSaveable { mutableStateOf(false) }
    // U4: auto-focus for the email field.
    val emailFocusRequester = remember { FocusRequester() }
    // U8: connectivity.
    val isOnline by rememberIsOnline()
    androidx.activity.compose.BackHandler(enabled = passwordStep && !uiState.isLoading) { passwordStep = false }
    // U9: where the inline button and the visible area are, in window pixels.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AuthAnalytics.log(context, "signup_view") }
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val autofillManager = remember(context) {
        context.getSystemService(android.view.autofill.AutofillManager::class.java)
    }

    // U6: one place for "OTP accepted". Shows the success tick first;
    // navigation happens when the tick animation has finished.
    fun onOtpVerified() {
        AuthAnalytics.log(context, "signup_verified")
        showEmailVerificationDialog = false
        showSuccess = true
    }
    val currentOnSignupSuccess by rememberUpdatedState(onSignupSuccess)
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(1300L)
            currentOnSignupSuccess()
        }
    }

    // U4: open the keyboard on the email field (only on a fresh screen).
    LaunchedEffect(Unit) {
        if (AUTOFOCUS_EMAIL && email.isEmpty() && !showEmailVerificationDialog && !passwordStep) {
            delay(450L)
            runCatching { emailFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }
    // U5: remember that the entrance animation has played.
    LaunchedEffect(Unit) {
        delay(1200L)
        entrancePlayed = true
    }
    // U2/U3: clear the in-flight flag when the request ends.
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) submitInFlight = false
    }

    // Automatically verify as soon as the sixth digit is entered.
    // A failed verification clears the code so the user can immediately retry.
    LaunchedEffect(verificationCode, uiState.isLoading) {
        if (verificationCode.length < 6) lastSubmittedCode = ""
        if (showEmailVerificationDialog &&
            verificationCode.length == 6 &&
            verificationCode != lastSubmittedCode &&
            !uiState.isLoading
        ) {
            delay(120L)
            if (verificationCode.length == 6 && !uiState.isLoading) {
                lastSubmittedCode = verificationCode
                viewModel.verifySignupEmailOtp(email, verificationCode) { onOtpVerified() }
            }
        }
    }

    // Invalid OTP: clear the code so the user can immediately retry.
    LaunchedEffect(uiState.error) {
        if (showEmailVerificationDialog && !uiState.error.isNullOrBlank()) {
            verificationCode = ""
        }
    }
    val emailLooksValid = email.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    val emailError: String? = when {
        !(emailTouched || submitAttempted) -> null
        email.isBlank() -> stringResource(R.string.err_email_required)
        !emailLooksValid -> stringResource(R.string.err_email_invalid)
        else -> null
    }
    val passwordError: String? = when {
        !(passwordTouched || submitAttempted) -> null
        password.isEmpty() -> stringResource(R.string.err_password_required)
        password.length < 8 -> stringResource(R.string.err_password_short)
        else -> null
    }
    val confirmError: String? = when {
        confirmPassword.isEmpty() ->
            if (confirmTouched || submitAttempted) stringResource(R.string.err_confirm_required) else null
        password != confirmPassword &&
                (confirmTouched || submitAttempted || confirmPassword.length >= password.length) ->
            stringResource(R.string.err_password_mismatch)
        else -> null
    }

    // U1: both fields filled and equal.
    val passwordsMatch = confirmPassword.isNotEmpty() && password == confirmPassword

    // Single submit path used by both the Create Account button and the
    // keyboard "Done" key on the confirm-password field.
    fun submitSignup() {
        submitAttempted = true
        val formValid = emailLooksValid && password.length >= 8 &&
                confirmPassword.isNotBlank() && password == confirmPassword
        AuthAnalytics.log(context, "signup_submit", "form_valid" to formValid.toString())
        if (!formValid) {
            // Errors are now visible under each field; give a small nudge too.
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }
        if (!uiState.isLoading && !submitInFlight) {
            submitInFlight = true
            viewModel.signUp(
                email = email.trim().lowercase(java.util.Locale.ROOT),
                password = password,
                onSuccess = {
                    // Tells Google/Samsung password manager the form was submitted,
                    // so it can offer to save the new password.
                    autofillManager?.commit()
                    AuthAnalytics.log(context, "signup_otp_sent")
                    verificationCode = ""
                    verificationSeconds = 48
                    verificationRateLimited = false
                    showEmailVerificationDialog = true
                },
                onEmailAlreadyUsed = {
                    showEmailAlreadyUsedDialog = true
                },
                onRateLimited = { seconds ->
                    verificationCode = ""
                    verificationSeconds = seconds
                    verificationRateLimited = true
                    showEmailVerificationDialog = true
                }
            )
        }
    }

    LaunchedEffect(showEmailVerificationDialog, verificationSeconds) {
        if (showEmailVerificationDialog && verificationSeconds > 0) {
            delay(1000L)
            verificationSeconds--
        }
    }
    SignupContent(
        email = email, password = password, confirmPassword = confirmPassword,
        passwordStep = passwordStep, loading = uiState.isLoading || submitInFlight || googleBusy || checkingEmail,
        checkingEmail = checkingEmail,
        online = isOnline, entrancePlayed = entrancePlayed,
        emailError = emailError, passwordError = passwordError, confirmError = confirmError,
        error = uiState.error, info = uiState.infoMessage,
        emailFocusRequester = emailFocusRequester,
        onEmail = { email = it }, onPassword = { password = it },
        onConfirm = { confirmPassword = it },
        onEmailBlur = { emailTouched = true }, onPasswordBlur = { passwordTouched = true },
        onConfirmBlur = { confirmTouched = true },
        onContinue = {
            if (passwordStep) submitSignup() else {
                emailTouched = true
                if (emailLooksValid && !checkingEmail) {
                    // The repository only supports Email + password signup. Do not call
                    // resendSignupEmailOtp as a passwordless account-creation substitute.
                    check(!PASSWORDLESS_SIGNUP)
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    // Ask the server NOW (before any password is typed) if the email is taken.
                    checkingEmail = true
                    viewModel.checkEmailBeforeSignup(email) { result ->
                        checkingEmail = false
                        when (result) {
                            EmailCheck.EXISTS -> {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                AuthAnalytics.log(context, "signup_email_exists")
                                showEmailAlreadyUsedDialog = true
                            }
                            // Unable to check (offline / RPC not deployed): continue; signUp() re-checks.
                            else -> passwordStep = true
                        }
                    }
                }
            }
        },
        onBack = { passwordStep = false; submitAttempted = false },
        onGoogle = {
            AuthAnalytics.log(context, "signup_google_tap")
            if (!googleBusy && !uiState.isLoading) scope.launch {
                googleBusy = true
                try { when (val outcome = GoogleAuthManager(context).signIn()) {
                    is GoogleSignInOutcome.Success -> viewModel.signInWithGoogleToken(
                        idToken = outcome.idToken, nonce = outcome.rawNonce,
                        onExistingUser = onExistingGoogleUser, onNewUser = onSignupSuccess)
                    GoogleSignInOutcome.Cancelled -> Unit
                    is GoogleSignInOutcome.Failure -> viewModel.showError(outcome.message)
                } } finally { googleBusy = false }
            }
        },
        onGuest = { AuthAnalytics.log(context, "signup_guest_tap"); onGuestContinue() },
        onLogin = { AuthAnalytics.log(context, "signup_login_tap"); onNavigateToLogin() },
        onHaveCode = {
            AuthAnalytics.log(context, "signup_have_code_tap")
            verificationCode = ""; verificationSeconds = 0
            verificationRateLimited = false; showEmailVerificationDialog = true
        }
    )
    if (showSuccess) SignupSuccessOverlay()

    if (showEmailVerificationDialog) {
        SignupEmailVerificationDialog(
            email = email,
            code = verificationCode,
            seconds = verificationSeconds,
            isLoading = uiState.isLoading,
            error = uiState.error,
            rateLimited = verificationRateLimited,
            onCodeChange = { verificationCode = it },
            onVerify = {
                if (verificationCode.length == 6 && !uiState.isLoading) {
                    lastSubmittedCode = verificationCode
                    viewModel.verifySignupEmailOtp(email, verificationCode) { onOtpVerified() }
                }
            },
            onResend = {
                viewModel.resendSignupEmailOtp(email) { error ->
                    if (error == null) {
                        verificationSeconds = 48
                        verificationRateLimited = false
                    }
                }
            },
            onChangeEmail = {
                showEmailVerificationDialog = false
                verificationCode = ""
                verificationRateLimited = false
            },
            onDismiss = {
                if (!uiState.isLoading) showEmailVerificationDialog = false
            }
        )
    }

    if (showEmailAlreadyUsedDialog) {
        SignupEmailAlreadyUsedDialog(
            email = email,
            onGoToLogin = {
                showEmailAlreadyUsedDialog = false
                onNavigateToLogin()
            },
            onTryDifferentEmail = {
                showEmailAlreadyUsedDialog = false
                email = ""
                emailTouched = false
                submitAttempted = false
            },
            onDismiss = {
                if (!uiState.isLoading) showEmailAlreadyUsedDialog = false
            }
        )
    }
}


@Composable
private fun SignupSuccessOverlay() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xB3000000)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .padding(32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .background(androidx.compose.ui.graphics.Color(0xFFFFFCF7))
                .padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color(0xFF22A06B),
                modifier = androidx.compose.ui.Modifier.size(72.dp)
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = androidx.compose.ui.Modifier.height(18.dp)
            )
            androidx.compose.material3.Text(
                text = "Email verified!",
                color = androidx.compose.ui.graphics.Color(0xFF142033),
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.foundation.layout.Spacer(
                modifier = androidx.compose.ui.Modifier.height(8.dp)
            )
            androidx.compose.material3.Text(
                text = "Your account is ready. Welcome to NagpurPulse.",
                color = androidx.compose.ui.graphics.Color(0xFF64748B),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        }
    }
}
