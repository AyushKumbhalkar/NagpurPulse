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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val emailLooksValid = email.isNotBlank() &&
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        contentReady = true
    }

    val isDarkTheme = LocalIsDarkTheme.current
    val pageBackground = if (isDarkTheme) Background else Color(0xFFFFF9F2)
    val cardBackground = if (isDarkTheme) Surface else Color(0xFFFEFEFF)
    val inputBackground = if (isDarkTheme) SurfaceAlt else Color.White
    val ink = if (isDarkTheme) PrimaryText else inkLight
    val muted = if (isDarkTheme) SecondaryText else mutedLight
    val fieldBorder = if (isDarkTheme) OrangePrimary.copy(alpha = 0.28f) else SignupBorder

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 800.dp
        val pageTop = if (compact) 22.dp else 30.dp
        val logoHeight = if (compact) 38.dp else 44.dp
        val logoGap = if (compact) 8.dp else 14.dp
        val cardPaddingV = if (compact) 14.dp else 20.dp
        val fieldGap = if (compact) 6.dp else 9.dp
        val buttonHeight = if (compact) 52.dp else 58.dp
        Box(modifier = Modifier.fillMaxSize().background(pageBackground))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = pageTop, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
                contentDescription = "NagpurPulse",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.height(logoHeight)
            )
            Spacer(Modifier.height(logoGap))

            AnimatedVisibility(
                visible = contentReady,
                enter = fadeIn(tween(450)) + slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = tween(450)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(cardBackground)
                        .border(1.dp, if (isDarkTheme) OrangePrimary.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.9f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 18.dp, vertical = cardPaddingV)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Your people",
                            color = ink,
                            fontSize = if (compact) 31.sp else 36.sp,
                            lineHeight = if (compact) 34.sp else 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.7).sp
                        )
                        Text(
                            text = "are here.",
                            color = SignupOrange,
                            fontSize = if (compact) 31.sp else 36.sp,
                            lineHeight = if (compact) 34.sp else 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.7).sp
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .padding(start = 48.dp)
                                .width(if (compact) 112.dp else 138.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(SignupOrange)
                        )
                    }
                    Spacer(Modifier.height(if (compact) 12.dp else 16.dp))
                    Text(
                        text = "Create your account and join\nthe conversations happening around\nNagpur.",
                        color = muted,
                        fontSize = if (compact) 15.sp else 16.sp,
                        lineHeight = if (compact) 21.sp else 23.sp
                    )
                    Spacer(Modifier.height(if (compact) 12.dp else 20.dp))

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
                            index = 0
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
                            index = 1
                        )
                    }
                    Spacer(Modifier.height(10.dp))

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
                            index = 2
                        )
                    }

                    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
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
                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(40.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))))
                            .then(
                                Modifier.pressScale(
                                    onClick = {
                                        if (emailLooksValid && password.isNotBlank() &&
                                            confirmPassword.isNotBlank() && password == confirmPassword &&
                                            !uiState.isLoading
                                        ) {
                                            viewModel.signUp(email.trim(), password, onSignupSuccess)
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

                    Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(Modifier.weight(1f), color = fieldBorder)
                        Text("  or continue with  ", color = muted, fontSize = 12.sp)
                        HorizontalDivider(Modifier.weight(1f), color = fieldBorder)
                    }
                    Spacer(Modifier.height(if (compact) 8.dp else 14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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

                    Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDarkTheme) SurfaceAlt else Color(0xFFFFF5EC))
                            .border(1.dp, if (isDarkTheme) OrangePrimary.copy(alpha = 0.20f) else Color(0xFFFFE4CF), RoundedCornerShape(20.dp))
                            .pressScale(onClick = onGuestContinue)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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

            Spacer(Modifier.height(if (compact) 5.dp else 10.dp))
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
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Already part of NagpurPulse?",
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

            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
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
            Spacer(Modifier.height(18.dp))
        }
    }
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
    // PremiumInputField owns the shape, border and background.
    // Keeping this wrapper neutral prevents the "double rounded box" effect.
    Box(
        modifier = Modifier.fillMaxWidth()
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
                .size(38.dp)
                .clip(RoundedCornerShape(50))
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(title, color = benefitText, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
    }
}