package com.nagpurpulse.ui.screens.onboarding

import com.nagpurpulse.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay

private val WelcomeCream = Color(0xFFFFF9F2)
private val WelcomeOrange = Color(0xFFFF5A00)
private val WelcomeText = Color(0xFF171717)
private val WelcomeSecondary = Color(0xFF737373)

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onLogin: () -> Unit,
    onGuestMode: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp

    // Scale the composition from the reference design while keeping safe bounds
    // for compact devices such as older 6.5-inch phones.
    val compact = screenHeight < 700
    val veryCompact = screenHeight < 620
    val horizontalPadding = if (screenWidth < 360) 18.dp else 24.dp
    // The hero is intentionally dominant, matching the reference composition.
    // Keep it wide on normal phones while giving it enough vertical room to
    // preserve the illustration's visual scale.
    val heroMaxWidth = when {
        screenWidth < 360 -> 350.dp
        screenWidth < 400 -> 390.dp
        else -> 440.dp
    }
    val heroHeight = when {
        veryCompact -> 270.dp
        compact -> 315.dp
        screenHeight < 800 -> 380.dp
        else -> 550.dp
    }
    val logoWidth = when {
        screenWidth < 360 -> 190.dp
        screenWidth < 400 -> 220.dp
        else -> 245.dp
    }
    val headlineSize = when {
        screenWidth < 360 -> 29.sp
        screenWidth < 400 -> 32.sp
        else -> 36.sp
    }
    val bodySize = if (screenWidth < 360) 14.sp else 15.sp

    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WelcomeCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                        .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (compact) 18.dp else 26.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(500)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(dampingRatio = 0.8f)
                )
            ) {
                Image(
                    painter = painterResource(R.drawable.nagpurpulse_logo),
                    contentDescription = "NagpurPulse",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(logoWidth)
                )
            }

            Spacer(Modifier.height(if (compact) 10.dp else 16.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(650, delayMillis = 120)) +
                    slideInVertically(
                        initialOffsetY = { 40 },
                        animationSpec = tween(650, easing = FastOutSlowInEasing)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = heroMaxWidth)
                        .height(heroHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.nagpurpulse_hero),
                        contentDescription = "People connecting through NagpurPulse in Nagpur",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(1.25f)
                    )
                }
            }

            Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(550, delayMillis = 220)) +
                    slideInVertically(initialOffsetY = { 24 })
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = WelcomeText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Your city, ")
                        }
                        withStyle(
                            SpanStyle(
                                color = WelcomeOrange,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("together.")
                        }
                    },
                    fontSize = headlineSize,
                    lineHeight = headlineSize * 1.12f,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(550, delayMillis = 280))
            ) {
                Text(
                    text = "Ask questions, share finds and discover what's happening around you in Nagpur.",
                    color = WelcomeSecondary,
                    fontSize = bodySize,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(if (compact) 18.dp else 24.dp))

            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(tween(550, delayMillis = 360)) +
                    slideInVertically(initialOffsetY = { 30 })
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (screenWidth < 360) 54.dp else 58.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WelcomeOrange,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 5.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = if (screenWidth < 360) 17.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(if (veryCompact) 16.dp else 24.dp))
        }
    }
}
