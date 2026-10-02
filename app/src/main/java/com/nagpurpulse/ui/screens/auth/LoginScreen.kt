package com.nagpurpulse.ui.screens.auth


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.preferences.DensityManager
import android.util.Log
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
    var password      by remember { mutableStateOf("") }
    var pwVisible     by remember { mutableStateOf(false) }
    var rememberMe    by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Staggered content entrance
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(60); contentReady = true }

    Box(modifier = Modifier.fillMaxSize()) {
        CinematicBackground(isLogin = true)



        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            // ── Logo ────────────────────────────────────────────────────
            PremiumLogo("Welcome back to Nagpur")

          //  PremiumLogo(" ")
            Spacer(Modifier.height(8.dp))


            // ── Form card ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = contentReady,
                enter = fadeIn(animationSpec = tween(500, delayMillis = 100)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
            )  {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                if (LocalIsDarkTheme.current) {
                                    listOf(
                                        Color(0xFF101010),
                                        Color(0xFF0C0C0C)
                                    )
                                } else {
                                    listOf(
                                        Surface,
                                        SurfaceAlt
                                    )
                                }
                            )
                        )
                        .border(
                            1.dp,
                            OrangePrimary.copy(0.30f),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(
    DensityManager.cardPadding.dp
)
                ) {
                    // Email
                    PremiumInputField(
                        value = email, onValueChange = { email = it },
                        placeholder = "Email",
                        leadingIcon = { Icon(Icons.Filled.Email, null, tint = OrangePrimary, modifier = Modifier.size(18.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        index = 0
                    )

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

                    // Remember me + Forgot password row
                    var rememberVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(380); rememberVisible = true }
                    AnimatedVisibility(rememberVisible, enter = fadeIn(tween(350))) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(
                                modifier = Modifier.pressScale { rememberMe = !rememberMe },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            if (rememberMe)
                                                OrangePrimary
                                            else
                                                SurfaceAlt
                                        )
                                        .border(
                                            1.dp,
                                            if (rememberMe)
                                                OrangePrimary
                                            else
                                                Divider, RoundedCornerShape(5.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rememberMe) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("Remember me", color = TertiaryText, fontSize = 13.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Text("Forgot password?", color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.pressScale(onClick = { viewModel.sendPasswordReset(email) }))
                        }
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
                    Spacer(Modifier.height(16.dp))

                    // Login button
                    PremiumButton(
                        text = "Login  →",
                        isLoading = uiState.isLoading,
                        enabled = email.isNotBlank() && password.isNotBlank(),
                        onClick = { viewModel.signIn(email, password, onLoginSuccess) },
                        delayMs = 420
                    )

                    Spacer(Modifier.height(22.dp))

                    // Divider
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            Modifier.weight(1f),
                            color = Divider
                        )
                        Text("  or continue with  ", color = TertiaryText, fontSize = 12.sp)
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

                                    } catch (e: Exception) {

                                        Log.e(
                                            "GOOGLE_LOGIN",
                                            "Google login failed",
                                            e
                                        )
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






            }

            Spacer(Modifier.height(20.dp))

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
}
