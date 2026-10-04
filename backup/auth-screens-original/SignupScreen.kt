
// this is the SignupScreen.kt file
//java/com/nagpurpulse/ui/screens/auth/SignupScreen.kt


package com.nagpurpulse.ui.screens.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.LocationCity
import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onGuestContinue: () -> Unit = onSignupSuccess,
    onExistingGoogleUser: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
)

{
    val uiState   by viewModel.uiState.collectAsState()
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val emailLooksValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    var pwVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); contentReady = true }

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(isLogin = false)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            PremiumLogo("Join Nagpur's best community")
            Spacer(Modifier.height(8.dp))

            // ── Form card ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = contentReady,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 500,
                        delayMillis = 100
                    )
                ) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(
                            1.dp,
                            OrangePrimary.copy(0.30f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(DensityManager.cardPadding.dp)
                ) {
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

                    PremiumInputField(
                        value = password, onValueChange = { password = it },
                        placeholder = "Password",
                        leadingIcon = { Icon(Icons.Filled.Lock, null, tint = OrangePrimary, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { pwVisible = !pwVisible }) {
                                AnimatedContent(pwVisible, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) }, label = "eye") { v ->
                                    Icon(if (v) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        index = 2
                    )

                    Spacer(Modifier.height(4.dp))


                    PremiumInputField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm Password",
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Lock,
                                null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        visualTransformation =
                            if (pwVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        index = 1
                    )

                    Spacer(Modifier.height(8.dp))
                    AnimatedErrorMessage(uiState.error)
                    uiState.infoMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    if (
                        confirmPassword.isNotEmpty() &&
                        password != confirmPassword
                    ) {
                        Text(
                            text = "Passwords do not match",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    PremiumButton(
                        text = "Create Account  →",
                        isLoading = uiState.isLoading,
                        enabled =
                            emailLooksValid &&
                                    password.isNotBlank() &&
                                    confirmPassword.isNotBlank() &&
                                    password == confirmPassword,
                        onClick = { viewModel.signUp(
                            email,
                            password,
                            onSignupSuccess
                        ) },
                        delayMs = 460


                    )

                    Spacer(Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text("  or continue with  ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        HorizontalDivider(
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    )    {
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
                                                onExistingGoogleUser()
                                            },

                                            onNewUser = {
                                                onSignupSuccess()
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
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Already have account ────────────────────────────────────
            var linkVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(560); linkVisible = true }
            AnimatedVisibility(linkVisible, enter = fadeIn(tween(400))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Continue as Guest button


                    GuestContinueCard(
                        onClick = onGuestContinue
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.pressScale(onClick = onNavigateToLogin),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text("Login", color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Trust badges ────────────────────────────────────────────
            var trustVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(650); trustVisible = true }
            AnimatedVisibility(trustVisible, enter = fadeIn(tween(400))) {
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

            Spacer(Modifier.height(0.dp))
        }
    }
}