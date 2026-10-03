//java/com/nagpurpulse/ui/screens/onboarding/OnboardingScreen.kt

package com.nagpurpulse.ui.screens.onboarding

import com.nagpurpulse.R
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.TertiaryText
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    onGuestMode: () -> Unit = {}
) {
    // ── entrance triggers ──────────────────────────────────────────────────
    var heroVisible   by remember { mutableStateOf(false) }
    var logoVisible   by remember { mutableStateOf(false) }
    var f1Visible     by remember { mutableStateOf(false) }
    var f2Visible     by remember { mutableStateOf(false) }
    var f3Visible     by remember { mutableStateOf(false) }
    var btnsVisible   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        heroVisible  = true;  delay(180)
        logoVisible  = true;  delay(120)
        f1Visible    = true;  delay(100)
        f2Visible    = true;  delay(100)
        f3Visible    = true;  delay(120)
        btnsVisible  = true
    }

    // ── floating orbs animation ────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_orbs")
    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val orbOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb2"
    )
    val logoRingScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ── background glow orbs ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 140.dp, y = (-80 + orbOffset1).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(OrangePrimary.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = (260 + orbOffset2).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8C00).copy(alpha = 0.09f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── hero illustration area ────────────────────────────────────
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(tween(700)) + slideInVertically(initialOffsetY = { -80 })
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = if (LocalIsDarkTheme.current) {
                                        listOf(
                                            Color(0xFF2D0F00),
                                            Color(0xFF1A0800),
                                            Color(0xFF0D0D0D)
                                        )
                                    } else {
                                        listOf(
                                            OrangePrimary.copy(alpha = 0.08f),
                                            Surface,
                                            Background
                                        )
                                    }
                                )
                            )
                    )
                    // Radial glow centre
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.radialGradient(
                                colors = listOf(OrangePrimary.copy(alpha = 0.25f), Color.Transparent),
                                radius = 500f
                            )
                        )
                    )
                    // Simulated skyline silhouettes
                    Row(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(100, 140, 80, 160, 110, 90, 130, 75).forEachIndexed { i, h ->
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(h.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = if (LocalIsDarkTheme.current) {
                                                listOf(
                                                    Color(0xFF2A1000).copy(alpha = 0.9f),
                                                    Color(0xFF0D0D0D)
                                                )
                                            } else {
                                                listOf(
                                                    OrangePrimary.copy(alpha = 0.15f),
                                                    SurfaceAlt
                                                )
                                            }
                                        )
                                    )
                            )
                        }
                    }
                    // Simulated lit windows
                    repeat(12) { i ->
                        Box(
                            modifier = Modifier
                                .size(4.dp, 3.dp)
                                .offset(
                                    x = (30 + (i % 6) * 52).dp,
                                    y = (80 + (i / 6) * 30 + (i % 3) * 14).dp
                                )
                                .background(
                                    OrangePrimary.copy(alpha = 0.7f),
                                    RoundedCornerShape(1.dp)
                                )
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Background),
                                startY = 100f
                            )
                        )
                    )
                    Text(
                        "SITABULDI",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp, bottom = 60.dp),
                        color = OrangePrimary.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    // NagpurPulse brand mark in the hero's top-right corner.
                    Image(
                        painter = painterResource(id = R.drawable.nagpurpulse_app_icon_transparent),
                        contentDescription = "NagpurPulse logo",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 18.dp, end = 18.dp)
                            .size(44.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Logo ─────────────────────────────────────────────────
                AnimatedVisibility(
                    visible = logoVisible,
                    enter = fadeIn(tween(500)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Animated ring around logo
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .scale(logoRingScale)
                                .clip(CircleShape)
                                .background(OrangePrimary.copy(alpha = 0.12f))
                                .border(1.5.dp, OrangePrimary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("∿", color = OrangePrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        
                        Spacer(Modifier.height(14.dp))

                        // ── Continue as Guest ──────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(27.dp))
                                .background(SurfaceAlt)
                                .border(
                                    1.dp,
                                    com.nagpurpulse.ui.theme.Divider,
                                    androidx.compose.foundation.shape.RoundedCornerShape(27.dp)
                                )
                                .pressScale(onClick = onGuestMode),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {

                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(Modifier.width(10.dp))

                                Column {
                                    Text(
                                        "Continue as Guest",
                                        color = PrimaryText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )

                                    Text(
                                        "Explore without an account",
                                        color = TertiaryText,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                Text(
                                    "→",
                                    color = SecondaryText,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = PrimaryText, fontWeight = FontWeight.Bold)) { append("Nagpur ") }
                                withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Bold)) { append("Pulse") }
                            },
                            fontSize = 26.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Nagpur ki awaaz.\nAb ek jagah.",
                            color = PrimaryText,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Your city. Your voice. Your community.",
                            color = SecondaryText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Feature rows ─────────────────────────────────────────
                AnimatedFeatureRow(
                    visible = f1Visible,
                    icon = Icons.Filled.LocalFireDepartment,
                    text = "Trending local threads daily",
                    delay = 0
                )

                AnimatedFeatureRow(
                    visible = f2Visible,
                    icon = Icons.Filled.NotificationsActive,
                    text = "Live alerts from your area",
                    delay = 80
                )

                AnimatedFeatureRow(
                    visible = f3Visible,
                    icon = Icons.Filled.ChatBubbleOutline,
                    text = "Ask Nagpur anything",
                    delay = 160
                )

                Spacer(Modifier.height(28.dp))

                // ── Buttons ───────────────────────────────────────────────
                AnimatedVisibility(
                    visible = btnsVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(
                        initialOffsetY = { 60 },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onGetStarted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .pressScale(onClick = onGetStarted),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(27.dp)
                        ) {
                            Text("Get Started  →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .pressScale(onClick = onLogin),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PrimaryText
                            ),
                            border = BorderStroke(
                                1.dp,
                                Divider
                            ),
                            shape = RoundedCornerShape(27.dp)
                        ) {
                            Text(
                                text = "I already have an account",
                                color = PrimaryText,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = SecondaryText)) { append("By continuing you agree to our ") }
                                withStyle(SpanStyle(color = OrangePrimary)) { append("Terms") }
                                withStyle(SpanStyle(color = SecondaryText)) { append(" & ") }
                                withStyle(SpanStyle(color = OrangePrimary)) { append("Privacy Policy") }
                            },
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedFeatureRow(
    visible: Boolean,
    icon: ImageVector,
    text: String,
    delay: Int
){
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 400,
                delayMillis = delay
            )
        ) + slideInHorizontally(
            initialOffsetX = { -50 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.linearGradient(
                            colors = if (LocalIsDarkTheme.current) {
                                listOf(
                                    Color(0xFF2A1000),
                                    Color(0xFF1A0A00)
                                )
                            } else {
                                listOf(
                                    OrangePrimary.copy(alpha = 0.12f),
                                    SurfaceAlt
                                )
                            }
                        )
                    )
                    .border(
                        1.dp,
                        OrangePrimary.copy(alpha = 0.20f),
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = text,
                color = PrimaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
