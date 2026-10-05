package com.nagpurpulse.ui.screens.auth


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.LocationCity
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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.nagpurpulse.R
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onGoogleNewUser: () -> Unit,
    onGuestContinue: () -> Unit = onLoginSuccess,
    viewModel: AuthViewModel = hiltViewModel()
){
    val uiState       by viewModel.uiState.collectAsState()
    var email         by remember { mutableStateOf("") }
    val emailLooksValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    var password      by remember { mutableStateOf("") }
    var pwVisible     by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDarkTheme = LocalIsDarkTheme.current
    val pageBackground = if (isDarkTheme) Background else Color(0xFFFFF9F2)
    val cardBackground = if (isDarkTheme) Surface else Color(0xFFFEFEFF)
    val ink = if (isDarkTheme) PrimaryText else Color(0xFF111827)
    val muted = if (isDarkTheme) SecondaryText else Color(0xFF64748B)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(pageBackground))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Spacer(Modifier.height(40.dp))
            Image(
                painter = painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
                contentDescription = "NagpurPulse",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.height(48.dp)
            )
            Spacer(Modifier.height(14.dp))
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
                    "WELCOME BACK",
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Welcome back.",
                color = ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Pick up where you left off.",
                color = muted,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(18.dp))


            // ── Form card ───────────────────────────────────────────────
            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBackground)
                        .border(
                            1.dp,
                            OrangePrimary.copy(0.30f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(DensityManager.cardPadding.dp)
                ) {
                    // Email
                    PremiumInputField(
                        value = email, onValueChange = { email = it },
                        placeholder = "Email",
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = OrangePrimary, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        index = 0
                    )
                    if (email.isNotBlank() && !emailLooksValid) {
                        Text(
                            text = "Enter a valid email address",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Password
                    PremiumInputField(
                        value = password, onValueChange = { password = it },
                        placeholder = "Password",
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = OrangePrimary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { pwVisible = !pwVisible }) {
                                AnimatedContent(pwVisible, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "eye") { v ->
                                    Icon(if (v) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        index = 1
                    )

                    Spacer(Modifier.height(8.dp))

                    // Keep only the action that is implemented. Session persistence
                    // is managed by Supabase and is not controlled by a local checkbox.
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "Forgot password?",
                                color = OrangePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.pressScale(onClick = { resetEmail = email; showForgotPasswordDialog = true })
                            )
                        }

                    Spacer(Modifier.height(6.dp))
                    AnimatedErrorMessage(uiState.error)
                    // Password reset success message
                    AnimatedVisibility(
                        visible = uiState.forgotPasswordSent,
                        enter = fadeIn(tween(300)) + expandVertically(),
                        exit  = fadeOut(tween(200)) + shrinkVertically()
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
                                "Reset email sent! Check your inbox.",
                                color = GreenSuccess,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // Login button
                    PremiumButton(
                        text = "Login  →",
                        isLoading = uiState.isLoading,
                        enabled = emailLooksValid && password.isNotBlank(),
                        onClick = { viewModel.signIn(email, password, onLoginSuccess) },
                        delayMs = 420
                    )

                    Spacer(Modifier.height(16.dp))

                    // Divider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            Modifier.weight(1f),
                            color = Divider
                        )
                        Text("  or continue with  ", color = muted, fontSize = 12.sp)
                        HorizontalDivider(
                            Modifier.weight(1f),
                            color = Divider
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SocialButton(
                            label = "Google",
                            onClick = {

                                scope.launch {

                                    try {

                                        val token =
                                            GoogleAuthManager(context)
                                                .getGoogleIdToken()

                                        viewModel.signInWithGoogleToken(
                                            idToken = token,

                                            onExistingUser = {
                                                onLoginSuccess()
                                            },

                                            onNewUser = {
                                                onGoogleNewUser()
                                            }
                                        )

                                    } catch (_: Exception) {
                                        viewModel.showError("Google sign-in was cancelled or failed. Please try again.")
                                    }
                                }
                            },
                            delayMs = 520,
                            modifier = Modifier.fillMaxWidth()
                        )

                    }

                    Spacer(modifier = Modifier.height(12.dp))

                        // Continue as Guest button









            }

            Spacer(Modifier.height(8.dp))

            GuestContinueCard(
                modifier = Modifier.padding(horizontal = 20.dp),
                onClick = onGuestContinue
            )

            // ── Sign-up CTA card ────────────────────────────────────────
            Spacer(Modifier.height(8.dp))

            var linkVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(560)
                linkVisible = true
            }

            AnimatedVisibility(
                linkVisible,
                enter = fadeIn(tween(400))
            ) {

                Row(
                    modifier = Modifier.pressScale(
                        onClick = onNavigateToSignup
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "Don't have an account? ",
                        color = TertiaryText,
                        fontSize = 14.sp
                    )

                    Text(
                        "Create Account",
                        color = OrangePrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            var trustVisible by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(650)
                trustVisible = true
            }

            AnimatedVisibility(
                trustVisible,
                enter = fadeIn(tween(400))
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    TrustBadge(
                        Icons.Filled.Shield,
                        "Secure & private",
                        Modifier.weight(1f)
                    )

                    TrustBadge(
                        Icons.Filled.LocationCity,
                        "Be a part of Nagpur",
                        Modifier.weight(1f)
                    )
                }
            }
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
                            "Reset your password",
                            color = ink,
                            fontSize = 25.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Enter the email linked to your NagpurPulse account. We’ll send you a secure reset link.",
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
                            placeholder = { Text("Email address", color = muted) },
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
                                Text("Cancel", color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
                                        Text("Send reset link", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
