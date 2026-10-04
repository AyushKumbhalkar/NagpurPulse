//java/com/nagpurpulse/ui/screens/splash/SplashScreen.kt
package com.nagpurpulse.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.nagpurpulse.R
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val configuration = LocalConfiguration.current
    val compactHeight = configuration.screenHeightDp < 700
    val compactWidth = configuration.screenWidthDp < 360
    val logoSize = if (compactHeight) 112.dp else 140.dp
    val brandFontSize = if (compactWidth) 30.sp else 38.sp
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var tagVisible  by remember { mutableStateOf(false) }
    var exitAnim    by remember { mutableStateOf(false) }

    val t = rememberInfiniteTransition(label = "splash")
    val glowAlpha by t.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val ringRotation by t.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "orbit")
    val floatY by t.animateFloat(0f, -10f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")

    val logoScale by animateFloatAsState(if (logoVisible) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "logo_scale")
    val exitAlpha by animateFloatAsState(if (exitAnim) 0f else 1f, tween(500, easing = FastOutSlowInEasing), label = "exit_alpha")

    LaunchedEffect(Unit) {
        delay(100); logoVisible = true
        delay(300); textVisible = true
        delay(200); tagVisible  = true
        delay(1200); exitAnim   = true
        delay(550); onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .aspectRatio(1f)
                .sizeIn(maxWidth = 500.dp, maxHeight = 500.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            OrangePrimary.copy(
                                alpha = if (LocalIsDarkTheme.current)
                                    glowAlpha * 0.07f
                                else
                                    glowAlpha * 0.12f
                            ),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.graphicsLayer { alpha = exitAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Official NagpurPulse logo artwork
            Image(
                painter = painterResource(id = R.drawable.nagpurpulse_orange_pulse_icon),
                contentDescription = "NagpurPulse logo",
                modifier = Modifier
                    .size(logoSize)
                    .offset(y = (floatY * if (compactHeight) 0.6f else 1f).dp)
                    .scale(logoScale),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(if (compactHeight) 16.dp else 28.dp))

            AnimatedVisibility(textVisible, enter = fadeIn(tween(500)) + slideInVertically { 30 }) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = PrimaryText, fontWeight = FontWeight.Black, fontSize = brandFontSize)) { append("Nagpur ") }
                    withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = brandFontSize)) { append("Pulse") }
                })
            }
            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(tagVisible, enter = fadeIn(tween(500)) + slideInVertically { 20 }) {
                Text(
                    "Your city. Your community. Your pulse.",
                    color = SecondaryText,
                    fontSize = if (compactWidth) 12.sp else 14.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
            }
            Spacer(Modifier.height(if (compactHeight) 28.dp else 60.dp))
            AnimatedVisibility(tagVisible, enter = fadeIn(tween(400, 300))) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { i ->
                        val dotAlpha by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(600, delayMillis = i * 200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dot_$i")
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(OrangePrimary.copy(dotAlpha)))
                    }
                }
            }
        }
    }
}
