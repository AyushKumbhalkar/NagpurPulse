@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.nagpurpulse.ui.screens.auth


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Forum
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.nagpurpulse.R
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.GreenSubtle
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.TertiaryText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// ═══════════════════════════════════════════════════════════════════════════════
// ─── LOGIN SCREEN ──────────────────────────────────────────────────────────────
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LoginAnimatedHeadline(
    compact: Boolean,
    ink: Color,
    orange: Color
) {
    val phrases = stringArrayResource(R.array.login_headline_phrases)

    var animatedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var currentIndex = 0

        while (true) {
            val phrase = phrases[currentIndex]

            // Start the new phrase completely empty.
            animatedText = ""

            // Type EVERY character continuously until the whole phrase is visible.
            for (index in phrase.indices) {
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

    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = ink,
                    fontWeight = FontWeight.ExtraBold
                )
            ) {
                append(stringResource(R.string.login_headline_prefix))
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

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onGoogleNewUser: () -> Unit,
    onGuestContinue: () -> Unit = onLoginSuccess,
    viewModel: AuthViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var pwVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDarkTheme = LocalIsDarkTheme.current
    val pageBackground = if (isDarkTheme) Background else Color(0xFFFFF9F2)
    val cardBackground = if (isDarkTheme) Surface else Color(0xFFFEFEFF)
    val inputBackground = if (isDarkTheme) SurfaceAlt else Color.White
    val ink = if (isDarkTheme) PrimaryText else Color(0xFF111827)
    val muted = if (isDarkTheme) SecondaryText else Color(0xFF64748B)
    val emailLooksValid = email.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordFocusRequester = remember { FocusRequester() }

    fun submitLogin() {
        if (emailLooksValid && password.isNotBlank() && !uiState.isLoading) {
            viewModel.signIn(email.trim(), password, onLoginSuccess)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 800.dp
        val pageTop = 40.dp
        val logoHeight = 48.dp
        val cardPaddingV = if (compact) 10.dp else 20.dp
        val fieldGap = if (compact) 8.dp else 14.dp
        val buttonHeight = if (compact) 50.dp else 58.dp

        Box(modifier = Modifier.fillMaxSize().background(pageBackground))

        // Match SignupScreen auth artwork exactly.
        if (!isDarkTheme) {
            Image(
                painter = painterResource(R.drawable.auth_screen_header),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1774f / 850f)
                    .alpha(0.70f)
                    .offset(y = 0.dp)
                    .align(Alignment.TopCenter)
            )
        }

        if (!isDarkTheme) {
            Image(
                painter = painterResource(R.drawable.auth_screen_foooter),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
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
                .padding(top = pageTop, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(if (isDarkTheme) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
                    contentDescription = "NagpurPulse",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.height(logoHeight)
                )

                Spacer(Modifier.height(if (compact) 59.dp else 82.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-19).dp)
                        .padding(horizontal = 36.dp)
                ) {
                    LoginAnimatedHeadline(
                        compact = compact,
                        ink = ink,
                        orange = Color(0xFFFF7518)
                    )
                }

                Spacer(Modifier.height(if (compact) 8.dp else 8.dp))

                // Keep the original centered "WELCOME BACK" treatment,
                // while reserving exactly the same header-slot height as Signup.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-19).dp)
                        .height(if (compact) 42.dp else 46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(OrangePrimary)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            stringResource(R.string.login_welcome_back),
                            color = OrangePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Spacer(Modifier.width(9.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(OrangePrimary)
                        )
                    }
                }

                Spacer(Modifier.height(if (compact) 12.dp else 16.dp))

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
                    LoginFieldContainer {
                        PremiumInputField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = stringResource(R.string.login_email_hint),
                            leadingIcon = {
                                Icon(Icons.Filled.Email, null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
                            autofillTypes = listOf(AutofillType.EmailAddress),
                            index = 0,
                            containerColor = if (isDarkTheme) SurfaceAlt else Color(0xFFFFF8F2)
                        )
                    }

                    if (email.isNotBlank() && !emailLooksValid) {
                        Text(
                            text = stringResource(R.string.login_err_email_invalid),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(fieldGap))

                    LoginFieldContainer {
                        PremiumInputField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = stringResource(R.string.login_password_hint),
                            leadingIcon = {
                                Icon(Icons.Filled.Lock, null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { pwVisible = !pwVisible }) {
                                    AnimatedContent(
                                        targetState = pwVisible,
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
                            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submitLogin() }),
                            focusRequester = passwordFocusRequester,
                            autofillTypes = listOf(AutofillType.Password),
                            index = 1,
                            containerColor = cardBackground
                        )
                    }

                    Spacer(Modifier.height(if (compact) 3.dp else 10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            stringResource(R.string.login_forgot_password),
                            color = OrangePrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.pressScale(
                                onClick = {
                                    resetEmail = email
                                    showForgotPasswordDialog = true
                                }
                            )
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    AnimatedErrorMessage(uiState.error)

                    AnimatedVisibility(
                        visible = uiState.forgotPasswordSent,
                        enter = fadeIn(tween(300)) + expandVertically(),
                        exit = fadeOut(tween(200)) + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GreenSubtle)
                                .border(1.dp, GreenSuccess.copy(0.4f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.login_reset_sent),
                                color = GreenSuccess,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(if (compact) 7.dp else 12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(40.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))))
                            .pressScale(
                                onClick = {
                                    if (emailLooksValid && password.isNotBlank() && !uiState.isLoading) {
                                        viewModel.signIn(email.trim(), password, onLoginSuccess)
                                    }
                                }
                            )
                            .height(buttonHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (uiState.isLoading) stringResource(R.string.login_signing_in) else stringResource(R.string.login_button),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.size(12.dp))
                            Text("→", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(if (compact) 7.dp else 16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(Modifier.weight(1f), color = if (isDarkTheme) OrangePrimary.copy(alpha = 0.28f) else Color(0xFFE5E7EB))
                        Text("  " + stringResource(R.string.login_or_continue_with) + "  ", color = muted, fontSize = 12.sp)
                        HorizontalDivider(Modifier.weight(1f), color = if (isDarkTheme) OrangePrimary.copy(alpha = 0.28f) else Color(0xFFE5E7EB))
                    }

                    Spacer(Modifier.height(if (compact) 6.dp else 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 44.dp else 48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(inputBackground)
                            .border(
                                1.dp,
                                if (isDarkTheme) OrangePrimary.copy(alpha = 0.28f) else Color(0xFFE5E7EB),
                                RoundedCornerShape(18.dp)
                            )
                            .pressScale(onClick = {
                                scope.launch {
                                    when (val outcome = GoogleAuthManager(context).signIn()) {
                                        is GoogleSignInOutcome.Success ->
                                            viewModel.signInWithGoogleToken(
                                                idToken = outcome.idToken,
                                                nonce = outcome.rawNonce,
                                                onExistingUser = onLoginSuccess,
                                                onNewUser = onGoogleNewUser
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
                        Text(stringResource(R.string.login_continue_google), color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Google sign-in can create a new account from this screen too.
                    Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
                    LegalConsentText(textColor = muted, linkColor = OrangePrimary)
                    Spacer(Modifier.height(if (compact) 6.dp else 12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) SurfaceAlt else Color(0xFFFFF5EC))
                            .border(
                                1.dp,
                                if (isDarkTheme) OrangePrimary.copy(alpha = 0.20f) else Color(0xFFFFE4CF),
                                RoundedCornerShape(20.dp)
                            )
                            .pressScale(onClick = onGuestContinue)
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
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.login_continue_guest), color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.login_guest_subtitle), color = muted, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_continue_guest), tint = OrangePrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Match the Create Account screen exactly: the footer CTA starts
            // after the same guest-card spacing and uses the same -38.dp visual lift.
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
                        .pressScale(onClick = onNavigateToSignup)
                        .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.login_no_account), color = muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(7.dp))
                    Text(stringResource(R.string.signup_create_account), color = Color(0xFFFF7518), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(4.dp))
                    Text("→", color = Color(0xFFFF7518), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Explicit visual separation between the login CTA and the benefit row.
            Spacer(Modifier.height(if (compact) 28.dp else 32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-38).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Shield, null, tint = Color(0xFF168447), modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.login_benefit_secure),
                    background = if (isDarkTheme) Color(0xFF123322) else Color(0xFFD9F7E5),
                    modifier = Modifier.weight(1f)
                )
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Groups, null, tint = Color(0xFFE85D0D), modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.login_benefit_nagpur),
                    background = if (isDarkTheme) Color(0xFF3A2112) else Color(0xFFFFE1C8),
                    modifier = Modifier.weight(1f)
                )
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Forum, null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.login_benefit_discussions),
                    background = if (isDarkTheme) Color(0xFF142A43) else Color(0xFFDCEEFF),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        }
    }

    if (showForgotPasswordDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!uiState.isLoading) showForgotPasswordDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = !uiState.isLoading,
                dismissOnClickOutside = !uiState.isLoading
            )
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                        .heightIn(max = maxHeight * 0.88f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isDarkTheme) Surface else Color(0xFFFEFEFF))
                        .border(1.dp, if (isDarkTheme) OrangePrimary.copy(alpha = 0.45f) else Color(0xFFFF793D), RoundedCornerShape(32.dp))
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.width(34.dp).height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFBDB2B6).copy(alpha = 0.85f))
                    )
                    Spacer(Modifier.height(28.dp))
                    Box(
                        modifier = Modifier.size(92.dp).clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFFFF8B3D).copy(alpha = 0.38f), Color(0xFF7B2418).copy(alpha = 0.28f))
                                )
                            )
                            .border(2.dp, Color(0xFFFF793D), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = Color(0xFFFF9A54),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.login_reset_title),
                        color = ink,
                        fontSize = 25.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.login_reset_body),
                        color = muted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        placeholder = { Text(stringResource(R.string.login_email_hint), color = muted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = muted)
                        },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ink,
                            unfocusedTextColor = ink,
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF3A3A3A) else Color(0xFFE5E7EB),
                            cursorColor = OrangePrimary,
                            focusedContainerColor = if (isDarkTheme) SurfaceAlt else Color.White,
                            unfocusedContainerColor = if (isDarkTheme) SurfaceAlt else Color.White
                        )
                    )
                    Spacer(Modifier.height(26.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(0.8f).height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isDarkTheme) SurfaceAlt else Color.White)
                                .border(1.dp, if (isDarkTheme) Color(0xFF3A3A3A) else Color(0xFFE5E7EB), RoundedCornerShape(50))
                                .pressScale { if (!uiState.isLoading) showForgotPasswordDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.login_cancel), color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier.weight(1.2f).height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFFF790D), Color(0xFFFF393D)))
                                )
                                .pressScale {
                                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(resetEmail.trim()).matches() && !uiState.isLoading) {
                                        viewModel.sendPasswordReset(resetEmail.trim())
                                        showForgotPasswordDialog = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoading) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(R.string.login_reset_link), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("→", color = Color.White, fontSize = 21.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginFieldContainer(content: @Composable () -> Unit) {
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
    val benefitText = if (LocalIsDarkTheme.current) PrimaryText else Color(0xFF111827)

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
        Text(
            title,
            color = benefitText,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}