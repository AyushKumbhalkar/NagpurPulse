@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.nagpurpulse.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.foundation.layout.widthIn
import android.widget.Toast

private val inkLight = Color(0xFF111827)
private val mutedLight = Color(0xFF64748B)
private val SignupBorder = Color(0xFFE5E7EB)
private val SignupOrange = Color(0xFFFF7518)

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
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    // Validation UX: errors appear after the user leaves a field, or after they
    // tap Create Account / press Done - never while they are still typing.
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmTouched by remember { mutableStateOf(false) }
    var submitAttempted by remember { mutableStateOf(false) }
    var showEmailVerificationDialog by rememberSaveable { mutableStateOf(false) }
    var showEmailAlreadyUsedDialog by remember { mutableStateOf(false) }
    var showGoogleExistingDialog by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var verificationSeconds by remember { mutableStateOf(48) }
    var verificationRateLimited by remember { mutableStateOf(false) }
    // Remembers the last code that was auto-submitted so the same wrong code
    // can never be sent to the server again and again.
    var lastSubmittedCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { AuthAnalytics.log(context, "signup_view") }
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val autofillManager = remember(context) {
        context.getSystemService(android.view.autofill.AutofillManager::class.java)
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
                viewModel.verifySignupEmailOtp(email, verificationCode) {
                    AuthAnalytics.log(context, "signup_verified")
                    showEmailVerificationDialog = false
                    onSignupSuccess()
                }
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
        if (!uiState.isLoading) {
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

    val isDarkTheme = LocalIsDarkTheme.current
    val pageBackground = if (isDarkTheme) Background else Color(0xFFFFF9F2)
    val cardBackground = if (isDarkTheme) Surface else Color(0xFFFEFEFF)
    val inputBackground = if (isDarkTheme) SurfaceAlt else Color.White
    val ink = if (isDarkTheme) PrimaryText else inkLight
    val muted = if (isDarkTheme) SecondaryText else mutedLight
    val fieldBorder = if (isDarkTheme) OrangePrimary.copy(alpha = 0.28f) else SignupBorder

    LaunchedEffect(showEmailVerificationDialog) {
        if (!showEmailVerificationDialog) return@LaunchedEffect
        while (showEmailVerificationDialog && verificationSeconds > 0) {
            delay(1000L)
            if (showEmailVerificationDialog) verificationSeconds--
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 800.dp
        // Keep the logo at the exact same vertical position as LoginScreen.
        val pageTop = 40.dp
        // Match the login screen logo size exactly.
        val logoHeight = 48.dp
        val logoGap = if (compact) 5.dp else 14.dp
        val cardPaddingV = if (compact) 10.dp else 20.dp
        val fieldGap = if (compact) 4.dp else 9.dp
        val buttonHeight = if (compact) 50.dp else 58.dp
        Box(modifier = Modifier.fillMaxSize().background(pageBackground))

        // New NagpurPulse auth header artwork. It is transparent, so the logo
        // and headline can sit naturally on top of the city/wave illustration.
        if (!isDarkTheme) {
            Image(
                painter = painterResource(R.drawable.auth_screen_header),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1774f / 850f)
                    .alpha(0.70f)
                    .offset(y = 0.dp)
                    .align(Alignment.TopCenter)
            )
        }

        // Decorative peach wave footer. The asset is transparent above the wave,
        // so it sits behind the content and naturally reveals the page background.
        if (!isDarkTheme) {
            Image(
                painter = painterResource(R.drawable.auth_screen_foooter),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2048f / 682f)
                    .alpha(0.70f)
                    .offset(y = 19.dp)
                    .align(Alignment.BottomCenter)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .then(
                    if (maxHeight < 760.dp) Modifier.verticalScroll(rememberScrollState()) else Modifier
                )
                .padding(top = pageTop, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Keep the logo + signup card together as the top section so the
            // footer can never be laid out over the header on compact devices.
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
                    contentDescription = "NagpurPulse",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(logoHeight)
                )
                // Keep the animated headline directly on the same page background as LoginScreen.
                // Login and Signup therefore feel like the same screen when navigating between them.
                Spacer(Modifier.height(if (compact) 59.dp else 82.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-19).dp)
                        .padding(horizontal = 36.dp)
                ) {
                    SignupAnimatedHeadline(
                        compact = compact,
                        ink = ink,
                        orange = SignupOrange
                    )
                }
                // The subtitle was removed to keep the complete signup form visible on smaller screens.
                // The form now starts directly after the animated headline.
                Spacer(Modifier.height(if (compact) 6.dp else 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-19).dp)
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(pageBackground)
                        .border(1.dp, pageBackground, RoundedCornerShape(32.dp))
                        .padding(horizontal = 18.dp, vertical = cardPaddingV)
                ) {
                    SignupFieldContainer {
                        PremiumInputField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = stringResource(R.string.signup_email_hint),
                            leadingIcon = {
                                Icon(Icons.Filled.Email, null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false,
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            autofillTypes = listOf(AutofillType.EmailAddress),
                            onBlur = { emailTouched = true },
                            errorMessage = emailError,
                            index = 0,
                            containerColor = if (isDarkTheme) SurfaceAlt else Color(0xFFFFF8F2)
                        )
                    }
                    AuthFieldMessage(
                        message = emailError,
                        color = MaterialTheme.colorScheme.error
                    )
                    // "Did you mean name@gmail.com?" - catches typos before the OTP is sent to nowhere.
                    EmailSuggestionHint(
                        suggestion = if (emailTouched && emailError == null) suggestEmailCorrection(email) else null,
                        color = SignupOrange,
                        onAccept = { email = it }
                    )
                    Spacer(Modifier.height(fieldGap))

                    SignupFieldContainer {
                        PremiumInputField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = stringResource(R.string.signup_password_hint),
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    AnimatedContent(
                                        targetState = passwordVisible,
                                        transitionSpec = { fadeIn(tween(130)) togetherWith fadeOut(tween(130)) },
                                        label = "password-visibility"
                                    ) { visible ->
                                        Icon(
                                            if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = stringResource(if (visible) R.string.cd_hide_password else R.string.cd_show_password),
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            autofillTypes = listOf(AutofillType.NewPassword),
                            onBlur = { passwordTouched = true },
                            errorMessage = passwordError,
                            index = 1,
                            containerColor = cardBackground
                        )
                    }
                    AnimatedVisibility(
                        visible = password.isNotEmpty() || passwordError != null,
                        enter = fadeIn(tween(140)) + expandVertically(),
                        exit = fadeOut(tween(100)) + shrinkVertically()
                    ) {
                        StrengthSection(
                            password = password,
                            email = email,
                            trackColor = muted.copy(alpha = 0.25f),
                            hintColor = muted,
                            errorColor = MaterialTheme.colorScheme.error,
                            errorMessage = passwordError
                        )
                    }
                    Spacer(Modifier.height(if (compact) 3.dp else 10.dp))

                    SignupFieldContainer {
                        PremiumInputField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = stringResource(R.string.signup_confirm_hint),
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    AnimatedContent(
                                        targetState = confirmPasswordVisible,
                                        transitionSpec = { fadeIn(tween(130)) togetherWith fadeOut(tween(130)) },
                                        label = "confirm-password-visibility"
                                    ) { visible ->
                                        Icon(
                                            if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = stringResource(if (visible) R.string.cd_hide_confirm_password else R.string.cd_show_confirm_password),
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    submitSignup()
                                }
                            ),
                            autofillTypes = listOf(AutofillType.NewPassword),
                            onBlur = { confirmTouched = true },
                            errorMessage = confirmError,
                            index = 2,
                            containerColor = cardBackground
                        )
                    }

                    AuthFieldMessage(
                        message = confirmError,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(10.dp))
                    AnimatedErrorMessage(uiState.error)
                    uiState.infoMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(if (compact) 7.dp else 12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(40.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))))
                            .then(
                                Modifier.pressScale(
                                    onClick = { submitSignup() }
                                )
                            )
                            .height(buttonHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = stringResource(if (uiState.isLoading) R.string.signup_creating_account else R.string.signup_create_account),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.size(12.dp))
                            Text("→", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                    LegalConsentText(textColor = muted, linkColor = SignupOrange)
                    if (emailLooksValid) {
                        // For users who closed the verification dialog and still have the code.
                        Text(
                            text = stringResource(R.string.verify_have_code),
                            color = SignupOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    AuthAnalytics.log(context, "signup_have_code_tap")
                                    verificationCode = ""
                                    verificationSeconds = 0
                                    verificationRateLimited = false
                                    showEmailVerificationDialog = true
                                }
                                .padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(if (compact) 4.dp else 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(Modifier.weight(1f), color = fieldBorder)

                        Text("  ${stringResource(R.string.signup_or_continue_with)}  ", color = muted, fontSize = 12.sp)
                        HorizontalDivider(Modifier.weight(1f), color = fieldBorder)
                    }
                    Spacer(Modifier.height(if (compact) 6.dp else 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 44.dp else 48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(inputBackground)
                            .border(1.dp, fieldBorder, RoundedCornerShape(18.dp))
                            .pressScale(onClick = {
                                AuthAnalytics.log(context, "signup_google_tap")
                                scope.launch {
                                    when (val outcome = GoogleAuthManager(context).signIn()) {
                                        is GoogleSignInOutcome.Success ->
                                            viewModel.signInWithGoogleToken(
                                                idToken = outcome.idToken,
                                                nonce = outcome.rawNonce,
                                                onExistingUser = { showGoogleExistingDialog = true },
                                                onNewUser = onSignupSuccess
                                            )
                                        GoogleSignInOutcome.Cancelled -> Unit
                                        is GoogleSignInOutcome.Failure ->
                                            viewModel.showError(outcome.message)
                                    }
                                }
                            })
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.size(9.dp))
                        Text(stringResource(R.string.signup_continue_google), color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) SurfaceAlt else Color(0xFFFFF5EC))
                            .border(1.dp, if (isDarkTheme) OrangePrimary.copy(alpha = 0.20f) else Color(0xFFFFE4CF), RoundedCornerShape(20.dp))
                            .pressScale(onClick = {
                                AuthAnalytics.log(context, "signup_guest_tap")
                                onGuestContinue()
                            })
                            .padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 34.dp else 38.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFFFFE4CF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = SignupOrange, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.signup_guest_title), color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.signup_guest_subtitle), color = muted, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_continue_guest), tint = SignupOrange, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Keep the footer close to the form card; don't leave a large
            // artificial gap between the guest card and the login CTA.
            Spacer(Modifier.height(if (compact) 27.dp else 31.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-38).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(0.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (isDarkTheme) SurfaceAlt.copy(alpha = 0.92f)
                            else Color.White.copy(alpha = 0.90f)
                        )
                        .border(
                            1.dp,
                            if (isDarkTheme) OrangePrimary.copy(alpha = 0.16f)
                            else Color(0xFFF1E7DD),
                            RoundedCornerShape(24.dp)
                        )
                        .pressScale(onClick = {
                            AuthAnalytics.log(context, "signup_login_tap")
                            onNavigateToLogin()
                        })
                        .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(R.string.signup_have_account),
                        color = muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        stringResource(R.string.signup_log_in),
                        color = SignupOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "→",
                        color = SignupOrange,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Explicit visual separation between the login CTA and the benefit row.
            Spacer(Modifier.height(if (compact) 28.dp else 32.dp))
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-38).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Shield, null, tint = Color(0xFF168447), modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.benefit_secure),
                    background = if (isDarkTheme) Color(0xFF123322) else Color(0xFFD9F7E5),
                    modifier = Modifier.weight(1f)
                )
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Groups, null, tint = Color(0xFFE85D0D), modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.benefit_part_of_nagpur),
                    background = if (isDarkTheme) Color(0xFF3A2112) else Color(0xFFFFE1C8),
                    modifier = Modifier.weight(1f)
                )
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Forum, null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.benefit_discussions),
                    background = if (isDarkTheme) Color(0xFF142A43) else Color(0xFFDCEEFF),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        }

        LanguagePickerChip(
            contentColor = ink,
            backgroundColor = cardBackground.copy(alpha = 0.92f),
            borderColor = fieldBorder,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 6.dp, end = 14.dp)
        )
    }

    // Network-loading overlay: show NagpurPulse branding immediately after
    // Create Account is pressed, so the user never sees a blank waiting period.
    if (uiState.isLoading && !showEmailVerificationDialog) {
        NagpurPulseLoadingOverlay(
            message = stringResource(R.string.loading_creating_account)
        )
    }

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
                    viewModel.verifySignupEmailOtp(email, verificationCode) {
                        AuthAnalytics.log(context, "signup_verified")
                        showEmailVerificationDialog = false
                        onSignupSuccess()
                    }
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

    if (showGoogleExistingDialog) {
        SignupGoogleExistingDialog(
            onContinue = {
                showGoogleExistingDialog = false
                onExistingGoogleUser()
            },
            onDismiss = {
                if (!uiState.isLoading) showGoogleExistingDialog = false
            }
        )
    }

    if (showEmailAlreadyUsedDialog) {
        SignupEmailAlreadyUsedDialog(
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

// ═══════════════════════════════════════════════════════════════════════════
//  VERIFY-EMAIL DIALOG (redesigned)
// ═══════════════════════════════════════════════════════════════════════════

/** Single source of truth for the dialog palette. Light and dark variants. */
private data class VerifyPalette(
    val card: Color,
    val ink: Color,
    val muted: Color,
    val orange: Color,
    val orangeSoft: Color,
    val orangeLine: Color,
    val orangeFilled: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val error: Color,
    val errorSoft: Color,
    val neutral: Color,
    val artFill: Color,
    val artStroke: Color,
    val artGlow: Color,
    val pillBorder: Color,
    val iconNeutral: Color,
    val fieldActive: Color,
    val fieldIdle: Color,
    val closeBg: Color,
    val scrim: Color,
    val scrimDialog: Color,
    val overlayCard: Color,
    val overlayRing: Color
)

private val LightVerifyPalette = VerifyPalette(
    card = Color(0xFFFFFCF7),
    ink = Color(0xFF142033),
    muted = Color(0xFF64748B),
    orange = Color(0xFFF4511E),
    orangeSoft = Color(0xFFFFF1E6),
    orangeLine = Color(0xFFFFCBAA),
    orangeFilled = Color(0xFFFFA36B),
    gradientStart = Color(0xFFFF941F),
    gradientEnd = Color(0xFFFF3D1F),
    error = Color(0xFFD93025),
    errorSoft = Color(0xFFFFF1F0),
    neutral = Color(0xFFF6F1EA),
    artFill = Color(0xFFFFE5CF),
    artStroke = Color(0xFFF4B07D),
    artGlow = Color(0xFFFFE2CC),
    pillBorder = Color(0xFFFFE0C8),
    iconNeutral = Color(0xFF4B5563),
    fieldActive = Color.White,
    fieldIdle = Color(0xFFFFFBF7),
    closeBg = Color(0xFFFFF7EF),
    scrim = Color(0xCCFFF9F2),
    scrimDialog = Color(0xAFFFFCF7),
    overlayCard = Color.White,
    overlayRing = Color(0xFFFFD6B8)
)

private val DarkVerifyPalette = VerifyPalette(
    card = Color(0xFF181818),
    ink = Color(0xFFF2F2F7),
    muted = Color(0xFF9A9AA2),
    orange = Color(0xFFFF7A45),
    orangeSoft = Color(0xFF2B1A10),
    orangeLine = Color(0xFF5A3420),
    orangeFilled = Color(0xFFB5582C),
    gradientStart = Color(0xFFFF941F),
    gradientEnd = Color(0xFFFF3D1F),
    error = Color(0xFFFF6B60),
    errorSoft = Color(0xFF3A1A18),
    neutral = Color(0xFF242426),
    artFill = Color(0xFF2E1D12),
    artStroke = Color(0xFF7A4A2A),
    artGlow = Color(0xFF3A2314),
    pillBorder = Color(0xFF4A2D1A),
    iconNeutral = Color(0xFFB0B0B8),
    fieldActive = Color(0xFF202022),
    fieldIdle = Color(0xFF1C1C1E),
    closeBg = Color(0xFF262626),
    scrim = Color(0xCC080808),
    scrimDialog = Color(0xB3000000),
    overlayCard = Color(0xFF1C1C1E),
    overlayRing = Color(0xFF5A3420)
)

@Composable
private fun rememberVerifyPalette(): VerifyPalette =
    if (LocalIsDarkTheme.current) DarkVerifyPalette else LightVerifyPalette

@Composable
private fun SignupEmailVerificationDialog(
    email: String,
    code: String,
    seconds: Int,
    isLoading: Boolean,
    error: String?,
    rateLimited: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    val vc = rememberVerifyPalette()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var otpFocused by remember { mutableStateOf(false) }
    var otpShake by remember { mutableStateOf(0) }
    val otpShakeOffset = remember { Animatable(0f) }

    // Open the keyboard automatically (and again after a failed attempt).
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(280L)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    // Shake + haptic on error.
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            otpShake++
        }
    }
    LaunchedEffect(otpShake) {
        if (otpShake == 0) return@LaunchedEffect
        otpShakeOffset.snapTo(0f)
        otpShakeOffset.animateTo(-8f, tween(55))
        otpShakeOffset.animateTo(8f, tween(55))
        otpShakeOffset.animateTo(-6f, tween(45))
        otpShakeOffset.animateTo(6f, tween(45))
        otpShakeOffset.animateTo(0f, tween(45))
    }

    // Gentle floating envelope.
    val floatTransition = rememberInfiniteTransition(label = "verify-float")
    val floatY by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "envelope-float"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = RoundedCornerShape(32.dp)
            val cardWidth = minOf(maxWidth * 0.92f, 400.dp)
            val compactWidth = cardWidth < 330.dp
            val compactHeight = maxHeight < 700.dp
            val hPad = if (compactWidth) 18.dp else 24.dp
            val illustration = if (compactHeight) 84.dp else 100.dp
            val otpGap = if (compactWidth) 6.dp else 9.dp
            val canVerify = code.length == 6 && !isLoading
            val hasError = !error.isNullOrBlank()

            // Card HEIGHT WRAPS ITS CONTENT (no more dead space at the bottom).
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .heightIn(max = maxHeight * 0.92f)
                    .clip(cardShape)
                    .background(vc.card)
            ) {
                // ── Decorative layer: never affects layout ──
                Box(modifier = Modifier.matchParentSize()) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val fill = Path().apply {
                            moveTo(size.width * 0.48f, 0f)
                            cubicTo(
                                size.width * 0.66f, size.height * 0.08f,
                                size.width * 0.80f, size.height * 0.04f,
                                size.width, size.height * 0.30f
                            )
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(fill, brush = SolidColor(vc.artFill))

                        val line = Path().apply {
                            moveTo(size.width * 0.60f, 0f)
                            cubicTo(
                                size.width * 0.74f, size.height * 0.13f,
                                size.width * 0.87f, size.height * 0.08f,
                                size.width, size.height * 0.36f
                            )
                        }
                        drawPath(
                            line,
                            brush = SolidColor(vc.artStroke),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }

                    // Only the bottom 84dp of the wave artwork is shown (cropped, not stretched).
                    Image(
                        painter = painterResource(R.drawable.transparent_peach_wave_footer_overlay),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .align(Alignment.BottomCenter)
                            .alpha(0.85f)
                    )
                }

                // ── Foreground content ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = hPad, end = hPad, top = 30.dp, bottom = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Illustration with soft glow
                    Box(
                        modifier = Modifier.size(illustration + 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(vc.artGlow, vc.artGlow.copy(alpha = 0f))
                                    ),
                                    CircleShape
                                )
                        )
                        Image(
                            painter = painterResource(R.drawable.verify_email),
                            contentDescription = stringResource(R.string.verify_cd_icon),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(illustration)
                                .offset(y = floatY.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = vc.ink, fontWeight = FontWeight.ExtraBold)) {
                                append(stringResource(R.string.verify_title_ink) + " ")
                            }
                            withStyle(SpanStyle(color = vc.orange, fontWeight = FontWeight.ExtraBold)) {
                                append(stringResource(R.string.verify_title_accent))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = if (compactWidth) 26.sp else 29.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.verify_enter_code),
                        modifier = Modifier.fillMaxWidth(),
                        color = vc.muted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.verify_email_delivery_note),
                        modifier = Modifier.fillMaxWidth(),
                        color = vc.muted.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    // Email shown as a chip: long addresses ellipsize instead of breaking the layout.
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(vc.orangeSoft)
                            .border(1.dp, vc.pillBorder, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = vc.orange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            email,
                            modifier = Modifier.weight(1f, fill = false),
                            color = vc.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── OTP input ──
                    BasicTextField(
                        value = code,
                        onValueChange = { if (!isLoading) onCodeChange(it.filter(Char::isDigit).take(6)) },
                        readOnly = isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onVerify() }),
                        textStyle = TextStyle(color = Color.Transparent),
                        cursorBrush = SolidColor(Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = otpShakeOffset.value.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { otpFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(otpGap)
                                ) {
                                    repeat(6) { index ->
                                        OtpDigitBox(
                                            digit = code.getOrNull(index),
                                            isActive = otpFocused && !isLoading && index == code.length,
                                            isError = hasError && code.isEmpty()
                                        )
                                    }
                                }
                                // The REAL text field must be composed, otherwise taps never focus it
                                // and the keyboard never opens. It sits invisibly over the boxes.
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .alpha(0f)
                                ) {
                                    innerTextField()
                                }
                            }
                        }
                    )

                    // Error sits directly under the boxes, where the eye already is.
                    AnimatedVisibility(
                        visible = hasError,
                        enter = fadeIn(tween(160)) + expandVertically(),
                        exit = fadeOut(tween(120)) + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = vc.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                error.orEmpty(),
                                modifier = Modifier.weight(1f, fill = false),
                                color = vc.error,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── Primary button: dimmed until all 6 digits are entered ──
                    val buttonAlpha by animateFloatAsState(
                        targetValue = if (canVerify) 1f else 0.55f,
                        animationSpec = tween(180),
                        label = "verify-button-alpha"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = if (canVerify) 10.dp else 0.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = Color(0x66FF5A1F),
                                spotColor = Color(0x99FF5A1F)
                            )
                            .alpha(buttonAlpha)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(vc.gradientStart, vc.gradientEnd)
                                )
                            )
                            .pressScale(onClick = onVerify),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(if (isLoading) R.string.verify_button_loading else R.string.verify_button),
                                color = Color.White,
                                fontSize = if (compactWidth) 18.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── Open the user's email app ──
                    val dialogContext = LocalContext.current
                    val noEmailAppMessage = stringResource(R.string.verify_no_email_app)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(vc.neutral)
                            .clickable(role = Role.Button) {
                                if (!openEmailApp(dialogContext)) {
                                    Toast.makeText(dialogContext, noEmailAppMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = vc.orange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.verify_open_email_app),
                            color = vc.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // ── Resend / timer ──
                    if (seconds > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(vc.neutral)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = vc.muted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = vc.muted)) {
                                        append(stringResource(if (rateLimited) R.string.verify_retry_in else R.string.verify_resend_in) + " ")
                                    }
                                    withStyle(SpanStyle(color = vc.orange, fontWeight = FontWeight.Bold)) {
                                        // mm:ss, so 75s shows 01:15 (the old code showed 00:75)
                                        append("%02d:%02d".format(seconds / 60, seconds % 60))
                                    }
                                },
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(vc.orangeSoft)
                                .pressScale(onClick = onResend)
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = vc.orange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.verify_resend_code),
                                color = vc.orange,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // ── Secondary action ──
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .pressScale(onClick = onChangeEmail)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = vc.muted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.verify_change_email),
                            color = vc.muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── Close button: 48dp touch target, 34dp visual ──
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(vc.orangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = vc.iconNeutral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.matchParentSize().clip(cardShape)) {
                        NagpurPulseLoadingOverlay(
                            message = stringResource(R.string.loading_verifying_email),
                            inDialog = true
                        )
                    }
                }
            }
        }
    }
}

/** One OTP cell: empty / active (cursor) / filled / error. */
@Composable
private fun RowScope.OtpDigitBox(
    digit: Char?,
    isActive: Boolean,
    isError: Boolean
) {
    val vc = rememberVerifyPalette()
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> vc.error
            isActive -> vc.orange
            digit != null -> vc.orangeFilled
            else -> vc.orangeLine
        },
        animationSpec = tween(160),
        label = "otp-border"
    )
    val background by animateColorAsState(
        targetValue = when {
            isError -> vc.errorSoft
            isActive || digit != null -> vc.fieldActive
            else -> vc.fieldIdle
        },
        animationSpec = tween(160),
        label = "otp-background"
    )
    val pop by animateFloatAsState(
        targetValue = if (digit != null) 1f else 0.6f,
        animationSpec = tween(140),
        label = "otp-pop"
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(0.80f)
            .clip(shape)
            .background(background)
            .border(if (isActive || isError) 2.dp else 1.5.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            digit != null -> Text(
                text = digit.toString(),
                color = if (isError) vc.error else vc.ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                }
            )
            isActive -> OtpCursor()
        }
    }
}

@Composable
private fun OtpCursor() {
    val vc = rememberVerifyPalette()
    val transition = rememberInfiniteTransition(label = "otp-cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor-alpha"
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(26.dp)
            .alpha(cursorAlpha)
            .background(vc.orange, RoundedCornerShape(2.dp))
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  LOADING OVERLAY (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NagpurPulseLoadingOverlay(
    message: String,
    inDialog: Boolean = false
) {
    val vc = rememberVerifyPalette()
    val infiniteTransition = rememberInfiniteTransition(label = "nagpurpulse-loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo-ring-rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (inDialog) vc.scrimDialog else vc.scrim
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (inDialog) 86.dp else 92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(vc.overlayCard)
                    .border(
                        width = 1.dp,
                        color = vc.overlayRing,
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .rotate(rotation)
                ) {
                    drawArc(
                        color = vc.orange,
                        startAngle = -55f,
                        sweepAngle = 105f,
                        useCenter = false,
                        style = Stroke(
                            width = 3.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                Image(
                    painter = painterResource(R.drawable.nagpurpulse_orange_n_icon),
                    contentDescription = "NagpurPulse",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(if (inDialog) 46.dp else 50.dp)
                        .then(Modifier.rotate((pulse - 1f) * 2.5f))
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = message,
                color = vc.ink,
                fontSize = if (inDialog) 14.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.loading_please_wait),
                color = vc.muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EMAIL-ALREADY-USED DIALOG (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SignupGoogleExistingDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val vc = rememberVerifyPalette()
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val popupWidth = minOf(maxWidth * 0.86f, 400.dp)
            val compact = maxHeight < 650.dp
            Box(
                modifier = Modifier
                    .width(popupWidth)
                    .clip(RoundedCornerShape(30.dp))
                    .background(vc.card)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val fill = Path().apply {
                        moveTo(size.width * 0.48f, 0f)
                        cubicTo(
                            size.width * 0.66f, size.height * 0.08f,
                            size.width * 0.80f, size.height * 0.04f,
                            size.width, size.height * 0.30f
                        )
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(fill, brush = SolidColor(vc.artFill))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = if (compact) 28.dp else 34.dp,
                            bottom = 24.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.nagpurpulse_orange_n_icon),
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 58.dp else 68.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.google_existing_title),
                        color = vc.ink,
                        fontSize = if (compact) 26.sp else 29.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.google_existing_body),
                        color = vc.muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))
                                )
                            )
                            .clickable(role = Role.Button, onClick = onContinue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.google_existing_continue),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(vc.closeBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = vc.iconNeutral,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SignupEmailAlreadyUsedDialog(
    onGoToLogin: () -> Unit,
    onTryDifferentEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    val vc = rememberVerifyPalette()
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Keep the same modal frame family as the Verify Email dialog.
            // The foreground is a real Column layout: no child is vertically
            // positioned with offsets, so text can never be covered by a button.
            val popupWidth = maxWidth * 0.86f
            val popupHeight = maxHeight * 0.74f
            val horizontalPadding = minOf(popupWidth * 0.062f, 26.dp)
            val illustrationSize = minOf(
                popupWidth * 0.42f,
                popupHeight * 0.23f
            )
            val compactWidth = popupWidth < 330.dp
            val compactHeight = popupHeight < 620.dp
            val titleSize = when {
                compactWidth -> 28.sp
                compactHeight -> 29.sp
                else -> 31.sp
            }
            val bodySize = if (compactWidth || compactHeight) 15.sp else 16.sp
            val bodyLineHeight = if (compactWidth || compactHeight) 21.sp else 22.sp
            val contentGap = if (compactHeight) 12.dp else 16.dp
            val actionGap = if (compactHeight) 20.dp else 24.dp

            Box(
                modifier = Modifier
                    .width(popupWidth)
                    .height(popupHeight)
                    .clip(RoundedCornerShape(30.dp))
                    .background(vc.card)
            ) {
                // BACKGROUND LAYER: these decorations never participate in the
                // foreground Column's measurement or push its content.
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(popupHeight * 0.19f)
                        .align(Alignment.TopCenter)
                ) {
                    val fill = Path().apply {
                        moveTo(size.width * 0.48f, 0f)
                        cubicTo(
                            size.width * 0.66f, size.height * 0.08f,
                            size.width * 0.80f, size.height * 0.04f,
                            size.width, size.height * 0.30f
                        )
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(
                        fill,
                        brush = SolidColor(vc.artFill)
                    )

                    val line = Path().apply {
                        moveTo(size.width * 0.60f, 0f)
                        cubicTo(
                            size.width * 0.74f, size.height * 0.13f,
                            size.width * 0.87f, size.height * 0.08f,
                            size.width, size.height * 0.36f
                        )
                    }
                    drawPath(
                        line,
                        brush = SolidColor(vc.artStroke),
                        style = Stroke(
                            width = (popupWidth * 0.0027f).toPx()
                        )
                    )
                }

                Image(
                    painter = painterResource(R.drawable.transparent_peach_wave_footer_overlay),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2048f / 682f)
                        .align(Alignment.BottomCenter)
                )

                // FOREGROUND LAYER.
                // Scroll is only an overflow safety net for large font/display
                // scaling. On normal screens the complete content fits without
                // scrolling.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = 42.dp,
                            bottom = 18.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.email_alert),
                        contentDescription = stringResource(R.string.used_cd_icon),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(illustrationSize)
                    )

                    Spacer(Modifier.height(contentGap))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = vc.ink,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(stringResource(R.string.used_title_ink) + " ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = vc.orange,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(stringResource(R.string.used_title_accent))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = titleSize,
                        lineHeight = if (compactWidth) 33.sp else 36.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(contentGap))

                    Text(
                        text = stringResource(R.string.used_body),
                        modifier = Modifier.fillMaxWidth(),
                        color = vc.muted,
                        fontSize = bodySize,
                        lineHeight = bodyLineHeight,
                        textAlign = TextAlign.Center
                    )

                    // The description is a normal Column child. This fixed
                    // spacing guarantees it is completely measured before the
                    // primary action can begin.
                    Spacer(Modifier.height(actionGap))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))
                                )
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = onGoToLogin
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.size(30.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val stroke = (w * 0.11f).coerceAtLeast(1.5f)

                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.10f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.80f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                                    style = Stroke(
                                        width = stroke
                                    )
                                )
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.50f),
                                    end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.33f),
                                    end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.67f),
                                    end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Text(
                                stringResource(R.string.used_go_login),
                                color = Color.White,
                                fontSize = if (compactWidth) 19.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.width(10.dp))

                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(vc.fieldIdle)
                            .border(
                                width = 1.dp,
                                color = vc.orangeLine,
                                shape = RoundedCornerShape(60.dp)
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = onTryDifferentEmail
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.used_try_different),
                            color = vc.ink,
                            fontSize = if (compactWidth) 18.sp else 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // 44–48dp accessible touch target; the X itself stays visually
                // compact and is kept above the artwork/content.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(vc.closeBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = vc.iconNeutral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HEADLINE + SMALL HELPERS (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SignupAnimatedHeadline(
    compact: Boolean,
    ink: Color,
    orange: Color
) {
    val phrases = stringArrayResource(R.array.signup_headline_phrases).toList()
    val headlinePrefix = stringResource(R.string.signup_headline_prefix)

    var animatedText by remember { mutableStateOf("") }

    LaunchedEffect(phrases) {
        var currentIndex = 0

        while (true) {
            val phrase = phrases[currentIndex]

            // Keep one character visible during locale changes so the headline
            // never collapses to zero height and makes the whole screen flicker.
            animatedText = phrase.take(1)

            for (index in 1 until phrase.length) {
                animatedText = phrase.substring(0, index + 1)
                delay(75L)
            }

            // Keep the COMPLETE phrase visible so it can be read.
            delay(2500L)

            // Delete EVERY character continuously.
            for (index in phrase.length - 1 downTo 0) {
                animatedText = phrase.substring(0, index)
                delay(50L)
            }

            // Brief clean gap before the next phrase starts.
            delay(400L)

            currentIndex = (currentIndex + 1) % phrases.size
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(if (compact) 68.dp else 76.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = ink,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append("$headlinePrefix ")
            }
            withStyle(
                SpanStyle(
                    color = orange,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append(animatedText)
            }
        },
        // Keep the full animated phrase visible on narrow phones instead of
        // clipping it after "are". The headline can use two lines when needed.
        fontSize = if (compact) 28.sp else 32.sp,
        lineHeight = if (compact) 32.sp else 36.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.7).sp,
            maxLines = 2
        )
    }
}

@Composable
private fun SignupFieldContainer(content: @Composable () -> Unit) {
    // Give all signup fields a stronger, darker orange outline.
    // The rounded wrapper keeps the border clean and consistent across
    // Email, Password, and Confirm Password.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 0.75.dp,
                color = Color(0xFFFF7518),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun SignupBenefit(
    icon: @Composable () -> Unit,
    title: String,
    background: Color,
    modifier: Modifier = Modifier
) {
    val benefitText = if (LocalIsDarkTheme.current) PrimaryText else inkLight

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(title, color = benefitText, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
    }
}