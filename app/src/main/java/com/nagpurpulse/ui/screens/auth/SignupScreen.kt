package com.nagpurpulse.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.nagpurpulse.R
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showEmailVerificationDialog by remember { mutableStateOf(false) }
    var showEmailAlreadyUsedDialog by remember { mutableStateOf(false) }
    var verificationCode by remember { mutableStateOf("") }
    var verificationSeconds by remember { mutableStateOf(45) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val emailLooksValid = email.isNotBlank() &&
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    val isDarkTheme = LocalIsDarkTheme.current
    val pageBackground = if (isDarkTheme) Background else Color(0xFFFFF9F2)
    val cardBackground = if (isDarkTheme) Surface else Color(0xFFFEFEFF)
    val inputBackground = if (isDarkTheme) SurfaceAlt else Color.White
    val ink = if (isDarkTheme) PrimaryText else inkLight
    val muted = if (isDarkTheme) SecondaryText else mutedLight
    val fieldBorder = if (isDarkTheme) OrangePrimary.copy(alpha = 0.28f) else SignupBorder

    LaunchedEffect(showEmailVerificationDialog) {
        if (!showEmailVerificationDialog) return@LaunchedEffect
        verificationSeconds = 45
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
                contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
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
            // Keep the logo + signup card together as the top section so the
            // footer can never be laid out over the header on compact devices.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
                contentDescription = "NagpurPulse",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
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
            Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-19).dp)
                    .padding(horizontal = 36.dp)
            ) {
                Text(
                    text = "Create your account and join the conversations happening around Nagpur.",
                    color = if (isDarkTheme) PrimaryText else Color(0xFF111827),
                    fontSize = if (compact) 15.sp else 16.sp,
                    lineHeight = if (compact) 21.sp else 23.sp
                )
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
                    SignupFieldContainer {
                        PremiumInputField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email address",
                            leadingIcon = {
                                Icon(Icons.Filled.Email, null, tint = OrangePrimary, modifier = Modifier.size(21.dp))
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            index = 0,
                            containerColor = if (isDarkTheme) SurfaceAlt else Color(0xFFFFF8F2)
                        )
                    }
                    if (email.isNotBlank() && !emailLooksValid) {
                        Text(
                            text = "Enter a valid email address",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 10.dp, top = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(fieldGap))

                    SignupFieldContainer {
                        PremiumInputField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
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
                                            contentDescription = if (visible) "Hide password" else "Show password",
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            index = 1,
                            containerColor = cardBackground
                        )
                    }
                    Spacer(Modifier.height(if (compact) 3.dp else 10.dp))

                    SignupFieldContainer {
                        PremiumInputField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Confirm password",
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
                                            contentDescription = if (visible) "Hide confirm password" else "Show confirm password",
                                            tint = OrangePrimary
                                        )
                                    }
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            index = 2,
                            containerColor = cardBackground
                        )
                    }

                    if (password.isNotEmpty() && password.length < 8) {
                        Text(
                            text = "Password must be at least 8 characters",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 10.dp, top = 7.dp)
                        )
                    } else if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text(
                            text = "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 10.dp, top = 7.dp)
                        )
                    }
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
                                    onClick = {
                                        if (emailLooksValid && password.length >= 8 &&
                                            confirmPassword.isNotBlank() && password == confirmPassword &&
                                            !uiState.isLoading
                                        ) {
                                            viewModel.signUp(
                                                email = email.trim(),
                                                password = password,
                                                onSuccess = {
                                                    verificationCode = ""
                                                    verificationSeconds = 45
                                                    showEmailVerificationDialog = true
                                                },
                                                onEmailAlreadyUsed = {
                                                    showEmailAlreadyUsedDialog = true
                                                }
                                            )
                                        }
                                    }
                                )
                            )
                            .height(buttonHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = if (uiState.isLoading) "Creating account…" else "Create Account",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.size(12.dp))
                            Text("→", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(if (compact) 7.dp else 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(Modifier.weight(1f), color = fieldBorder)
                        Text("  or continue with  ", color = muted, fontSize = 12.sp)
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
                                scope.launch {
                                    try {
                                        val token = GoogleAuthManager(context).getGoogleIdToken()
                                        viewModel.signInWithGoogleToken(
                                            idToken = token,
                                            onExistingUser = onExistingGoogleUser,
                                            onNewUser = onSignupSuccess
                                        )
                                    } catch (_: Exception) {
                                        viewModel.showError("Google sign-in was cancelled or failed. Please try again.")
                                    }
                                }
                            })
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = com.nagpurpulse.R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.size(9.dp))
                        Text("Continue with Google", color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) SurfaceAlt else Color(0xFFFFF5EC))
                            .border(1.dp, if (isDarkTheme) OrangePrimary.copy(alpha = 0.20f) else Color(0xFFFFE4CF), RoundedCornerShape(20.dp))
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
                            Icon(Icons.Filled.Groups, contentDescription = null, tint = SignupOrange, modifier = Modifier.size(21.dp))
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continue as Guest", color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Explore without an account", color = muted, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Continue as guest", tint = SignupOrange, modifier = Modifier.size(20.dp))
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
                    .pressScale(onClick = onNavigateToLogin)
                    .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Already have an account?",
                    color = muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Log in",
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
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-38).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Shield, null, tint = Color(0xFF168447), modifier = Modifier.size(22.dp)) },
                    title = "Secure\n& private",
                    background = if (isDarkTheme) Color(0xFF123322) else Color(0xFFD9F7E5),
                    modifier = Modifier.weight(1f)
                )
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Groups, null, tint = Color(0xFFE85D0D), modifier = Modifier.size(22.dp)) },
                    title = "Be a part\nof Nagpur",
                    background = if (isDarkTheme) Color(0xFF3A2112) else Color(0xFFFFE1C8),
                    modifier = Modifier.weight(1f)
                )
                SignupBenefit(
                    icon = { Icon(Icons.Filled.Forum, null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp)) },
                    title = "Interesting\ndiscussions",
                    background = if (isDarkTheme) Color(0xFF142A43) else Color(0xFFDCEEFF),
                    modifier = Modifier.weight(1f)
                )
            }
                Spacer(Modifier.height(if (compact) 4.dp else 8.dp))
        }
    }

    if (showEmailVerificationDialog) {
        SignupEmailVerificationDialog(
            email = email,
            code = verificationCode,
            seconds = verificationSeconds,
            isLoading = uiState.isLoading,
            error = uiState.error,
            onCodeChange = { verificationCode = it },
            onVerify = {
                if (verificationCode.length == 6 && !uiState.isLoading) {
                    viewModel.verifySignupEmailOtp(email, verificationCode) {
                        showEmailVerificationDialog = false
                        onSignupSuccess()
                    }
                }
            },
            onResend = {
                viewModel.resendSignupEmailOtp(email) { error ->
                    if (error == null) verificationSeconds = 45
                }
            },
            onChangeEmail = {
                showEmailVerificationDialog = false
                verificationCode = ""
            },
            onDismiss = {
                if (!uiState.isLoading) showEmailVerificationDialog = false
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
            },
            onDismiss = {
                if (!uiState.isLoading) showEmailAlreadyUsedDialog = false
            }
        )
    }
}

@Composable
private fun SignupEmailVerificationDialog(
    email: String,
    code: String,
    seconds: Int,
    isLoading: Boolean,
    error: String?,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onDismiss: () -> Unit
) {
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
            val popupWidth = maxWidth * 0.86f
            val popupHeight = maxHeight * 0.74f
            val refX: (Float) -> androidx.compose.ui.unit.Dp = { value ->
                popupWidth * (value / 810f)
            }
            val refY: (Float) -> androidx.compose.ui.unit.Dp = { value ->
                popupHeight * (value / 1105f)
            }

            Box(
                modifier = Modifier
                    .width(popupWidth)
                    .height(popupHeight)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFFFFCF7))
            ) {
                // Reference-matched top-right peach decoration.
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(refY(205f))
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
                    drawPath(fill, brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFE5CF)))
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
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF4B07D)),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = refX(2.2f).toPx())
                    )
                }

                // Shared peach footer used by Create Account and Login.
                Image(
                    painter = painterResource(R.drawable.transparent_peach_wave_footer_overlay),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2048f / 682f)
                        .align(Alignment.BottomCenter)
                )

                // Close button: reference position and scale.
                Box(
                    modifier = Modifier
                        .offset(x = refX(820f), y = refY(26f))
                        .size(refX(84f))
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFF7EF))
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color(0xFF777777),
                        modifier = Modifier.size(refX(38f))
                    )
                }

                // Illustration.
                Box(
                    modifier = Modifier
                        .offset(x = refX(268f), y = refY(65f))
                        .size(refX(275f))
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFF0E3)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.verify_email),
                        contentDescription = "Email verification",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.size(refX(275f))
                    )
                }

                // Heading.
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(Color(0xFF142033), fontWeight = FontWeight.ExtraBold)) {
                            append("Verify your ")
                        }
                        withStyle(SpanStyle(Color(0xFFF4511E), fontWeight = FontWeight.ExtraBold)) {
                            append("email")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = refY(310f)),
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center
                )

                // Description.
                Text(
                    "We’ve sent a 6-digit verification code to",
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = refY(399f)),
                    color = Color(0xFF64748B),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )

                // Email.
                Text(
                    email,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = refY(447f)),
                    color = Color(0xFF142033),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                // Instruction.
                Text(
                    "Enter the code below to continue.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = refY(496f)),
                    color = Color(0xFF64748B),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center
                )

                // OTP boxes.
                BasicTextField(
                    value = code,
                    onValueChange = { onCodeChange(it.filter(Char::isDigit).take(6)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                    modifier = Modifier
                        .offset(x = refX(50f), y = refY(565f))
                        .width(refX(710f))
                        .height(refY(118f)),
                    decorationBox = {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(refX(10f))
                        ) {
                            repeat(6) { index ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(refX(17f)))
                                        .background(Color(0xFFFFFBF7))
                                        .border(
                                            width = refX(1.5f),
                                            color = Color(0xFFFFCBAA),
                                            shape = RoundedCornerShape(refX(17f))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        code.getOrNull(index)?.toString() ?: "",
                                        color = Color(0xFF142033),
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                )

                // Resend state: show exactly one layout.
                if (seconds > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = refY(735f)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Didn’t receive the code? ",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                        Text(
                            "Resend in ",
                            color = Color(0xFF64748B),
                            fontSize = 15.sp
                        )
                        Text(
                            "00:" + seconds.toString().padStart(2, '0'),
                            color = Color(0xFFF4511E),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "Resend",
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = refY(735f))
                            .pressScale(onClick = onResend),
                        color = Color(0xFFF4511E),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (!error.isNullOrBlank()) {
                    Text(
                        error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = refY(775f)),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Verify button.
                Box(
                    modifier = Modifier
                        .offset(x = refX(50f), y = refY(798f))
                        .width(refX(710f))
                        .height(refY(113f))
                        .clip(RoundedCornerShape(60.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))
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
                            if (isLoading) "Verifying…" else "Verify & Continue",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(refX(16f)))
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(refX(31f))
                        )
                    }
                }

                // Change email.
                Text(
                    "Change email address",
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = refY(945f))
                        .pressScale(onClick = onChangeEmail),
                    color = Color(0xFFF4511E),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
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
            // IMPORTANT: use the EXACT same popup frame as the existing
            // email-verification / OTP dialog. The email-alert dialog must not
            // appear as a smaller card.
            //
            // Verification popup reference frame: 810 × 1105.
            // Keep this same frame and scale its contents inside it.
            val popupWidth = maxWidth * 0.86f
            val popupHeight = maxHeight * 0.74f
            val refX: (Float) -> androidx.compose.ui.unit.Dp = { value ->
                popupWidth * (value / 810f)
            }
            val refY: (Float) -> androidx.compose.ui.unit.Dp = { value ->
                popupHeight * (value / 1105f)
            }

            Box(
                modifier = Modifier
                    .width(popupWidth)
                    .height(popupHeight)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFFFFCF7))
            ) {
                // Reference-matched peach top-right decoration.
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(refY(205f))
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
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFE5CF))
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
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF4B07D)),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = refX(2.2f).toPx()
                        )
                    )
                }

                // Same footer language as the existing verification popup.
                Image(
                    painter = painterResource(R.drawable.transparent_peach_wave_footer_overlay),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2048f / 682f)
                        .align(Alignment.BottomCenter)
                )

                // Close button.
                Box(
                    modifier = Modifier
                        .offset(x = refX(710f), y = refY(22f))
                        .size(refX(80f))
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFFF7EF))
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(refX(36f))
                    )
                }

                // The supplied email_alert asset is used exactly as requested.
                Image(
                    painter = painterResource(R.drawable.email_alert),
                    contentDescription = "Email already used",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .offset(x = refX(250f), y = refY(92f))
                        .size(refX(430f))
                )

                // Heading: "Email" dark + "already used" in NagpurPulse orange.
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFF142033),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Email ")
                        }
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFF4511E),
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("already used")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = refY(463f)),
                    fontSize = 31.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center
                )

                // Reference copy, intentionally kept concise and reassuring.
                Text(
                    text = "This email address is already registered\nwith NagpurPulse. Please log in to\ncontinue.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = refX(32f))
                        .offset(y = refY(568f)),
                    color = Color(0xFF64748B),
                    // The reference uses a compact body type. Keeping this at
                    // 15sp prevents the first line from wrapping early on
                    // high-density phones, which was hiding the third line
                    // behind the primary button.
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )

                // Primary action.
                Box(
                    modifier = Modifier
                        .offset(x = refX(85f), y = refY(749f))
                        .width(refX(775f))
                        .height(refY(129f))
                        .clip(RoundedCornerShape(60.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))
                            )
                        )
                        .pressScale(onClick = onGoToLogin),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Reference-style "login" glyph: a door outline with an
                        // entering arrow, drawn locally so no extra icon dependency is needed.
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.size(refX(52f))
                        ) {
                            val w = size.width
                            val h = size.height

                            // Door.
                            drawRoundRect(
                                color = Color.White,
                                topLeft = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.10f),
                                size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.80f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = refX(4f).toPx()
                                )
                            )

                            // Entering arrow.
                            drawLine(
                                color = Color.White,
                                start = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.50f),
                                end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                strokeWidth = refX(4f).toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White,
                                start = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.33f),
                                end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                strokeWidth = refX(4f).toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White,
                                start = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.67f),
                                end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                strokeWidth = refX(4f).toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                        Spacer(Modifier.width(refX(22f)))
                        Text(
                            "Go to Login",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(refX(20f)))
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(refX(31f))
                        )
                    }
                }

                // Secondary action.
                Box(
                    modifier = Modifier
                        .offset(x = refX(87f), y = refY(899f))
                        .width(refX(773f))
                        .height(refY(112f))
                        .clip(RoundedCornerShape(60.dp))
                        .background(Color(0xFFFFFBF7))
                        .border(
                            width = refX(1.5f),
                            color = Color(0xFFFFCBAA),
                            shape = RoundedCornerShape(60.dp)
                        )
                        .pressScale(onClick = onTryDifferentEmail),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Try a different email",
                        color = Color(0xFF142033),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SignupAnimatedHeadline(
    compact: Boolean,
    ink: Color,
    orange: Color
) {
    val phrases = remember {
        listOf(
            "are here.",
            "are online.",
            "are talking.",
            "are joining.",
            "are active.",
            "are around.",
            "are asking."
        )
    }

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
                append("Your people ")
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
private fun SignupProviderTile(
    label: String,
    symbol: String,
    symbolColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, SignupBorder, RoundedCornerShape(20.dp))
            .pressScale(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(symbol, color = symbolColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(label, color = inkLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                .size(if (LocalIsDarkTheme.current) 32.dp else 32.dp)
                .clip(RoundedCornerShape(50))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(title, color = benefitText, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
    }
}