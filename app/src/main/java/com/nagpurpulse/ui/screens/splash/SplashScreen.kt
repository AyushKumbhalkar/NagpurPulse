//java/com/nagpurpulse/ui/screens/splash/SplashScreen.kt
package com.nagpurpulse.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nagpurpulse.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    /** True once the next destination is known. The splash leaves only when this is true. */
    ready: Boolean = true,
    /** True when the server could not be reached and nothing is cached: shows Retry. */
    failed: Boolean = false,
    onRetry: () -> Unit = {}
) {
    val currentOnFinished by rememberUpdatedState(onFinished)
    var minTimeDone by remember { mutableStateOf(false) }
    val lineProgress = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val dark = LocalIsDarkTheme.current
    // Same cream as the system splash / window background, so no colour jump between them.
    val splashBackground = if (dark) MaterialTheme.colorScheme.background else Color(0xFFFFF8EE)
    val compactWidth = configuration.screenWidthDp < 360
    // Must match res/drawable/splash_icon.xml (26% inset on 288dp) so the logo does not jump.
    val logoSize = 140.dp
    val brandFontSize = if (compactWidth) 30.sp else 38.sp
    var textVisible by remember { mutableStateOf(false) }
    var tagVisible  by remember { mutableStateOf(false) }
    var exitAnim    by remember { mutableStateOf(false) }

    val t = rememberInfiniteTransition(label = "splash")
    val glowAlpha by t.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val exitAlpha by animateFloatAsState(if (exitAnim) 0f else 1f, tween(350, easing = FastOutSlowInEasing), label = "exit_alpha")

    // The logo is already on screen (system splash / window background); the brand arrives around it.
    LaunchedEffect(Unit) {
        delay(120); textVisible = true
        delay(200); tagVisible  = true
        lineProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        delay(150); minTimeDone = true
    }
    LaunchedEffect(minTimeDone, ready) {
        if (minTimeDone && ready) {
            exitAnim = true
            delay(380)
            currentOnFinished()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(splashBackground),
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
                            OrangePrimary.copy(alpha = if (dark) glowAlpha * 0.07f else glowAlpha * 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Two equal weights around a fixed-size logo keep the logo at the exact screen centre.
        Column(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = exitAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.nagpurpulse_orange_n_icon),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(logoSize)
            )
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(textVisible, enter = fadeIn(tween(500)) + slideInVertically { 30 }) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = PrimaryText, fontWeight = FontWeight.Black, fontSize = brandFontSize)) { append("Nagpur") }
                        withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Black, fontSize = brandFontSize)) { append("Pulse") }
                    })
                }
                Spacer(Modifier.height(10.dp))
                AnimatedVisibility(tagVisible, enter = fadeIn(tween(500)) + slideInVertically { 20 }) {
                    Text(
                        stringResource(R.string.splash_tagline),
                        color = SecondaryText,
                        fontSize = if (compactWidth) 12.sp else 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(tagVisible, enter = fadeIn(tween(400, 300))) {
                    if (failed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                            Text(
                                stringResource(R.string.splash_connect_error),
                                color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).background(OrangePrimary)
                                    .clickable(role = Role.Button, onClick = onRetry)
                                    .padding(horizontal = 28.dp, vertical = 12.dp)
                            ) {
                                Text(stringResource(R.string.splash_retry), color = Color.White,
                                    fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // The pulse line draws itself, then gently glows until the app is ready.
                        val glow = if (lineProgress.value >= 1f) 0.55f + 0.45f * glowAlpha else 1f
                        Canvas(Modifier.size(width = 120.dp, height = 28.dp).graphicsLayer { alpha = glow }) {
                            val w = size.width; val h = size.height; val mid = h / 2f
                            val full = Path().apply {
                                moveTo(0f, mid)
                                lineTo(w * 0.30f, mid); lineTo(w * 0.40f, h * 0.05f); lineTo(w * 0.54f, h * 0.95f)
                                lineTo(w * 0.64f, mid * 0.8f); lineTo(w * 0.70f, mid); lineTo(w, mid)
                            }
                            val measure = PathMeasure().apply { setPath(full, false) }
                            val visible = Path()
                            measure.getSegment(0f, measure.length * lineProgress.value, visible, true)
                            drawPath(visible, OrangePrimary,
                                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
    }
}
