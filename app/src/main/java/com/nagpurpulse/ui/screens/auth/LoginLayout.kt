@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.nagpurpulse.ui.screens.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.authPalette
import kotlin.math.roundToInt

private val SignupPillHeight = 44.dp
private val MaxHeaderHeight = 300.dp

/** Every vertical size on the Login screen, derived from the real screen height. */
internal data class LoginDims(
    val headlineSp: Int, val headlineLine: Int,
    val row: Dp, val button: Dp, val google: Dp, val gap: Dp, val forgot: Dp,
    val showWelcome: Boolean, val footerArt: Dp,
    /** Natural height of everything between the header and the sign-up pill. */
    val estimate: Dp
)

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun loginDims(screenHeight: Dp): LoginDims {
    val t = ((screenHeight.value - 560f) / (900f - 560f)).coerceIn(0f, 1f)
    val sp = lerpF(26f, 38f, t).roundToInt()
    val line = (sp * 1.16f).roundToInt()
    val row = lerpF(48f, 60f, t); val button = lerpF(46f, 54f, t); val google = lerpF(44f, 48f, t)
    val gap = lerpF(8f, 14f, t); val forgot = lerpF(36f, 48f, t)
    val welcome = screenHeight.value >= 700f
    // headline block + welcome + card + forgot row + login + or + google (+4 slack)
    val estimate = 8f + 16f + 2f * line + (if (welcome) 22f else 0f) + gap + 2f * row + forgot +
        button + gap + 16f + gap + google + 4f
    return LoginDims(sp, line, row.dp, button.dp, google.dp, gap.dp, forgot.dp, welcome,
        lerpF(110f, 170f, t).dp, estimate.dp)
}

/** Weighted spacer that only exists in the no-scroll layout. */
@Composable
internal fun ColumnScope.FlexSpacer(flex: Boolean, weight: Float = 1f) {
    if (flex) Spacer(Modifier.weight(weight))
}

/**
 * Login layout (mockup panels 3 + 4).
 *
 * Normal case: NOTHING scrolls. The header illustration is the flexible element - it takes
 * whatever height is left after the form (sized from the real screen height) and the sign-up
 * pill are measured, and any extra space is shared by [FlexSpacer]s. That makes the screen fit
 * from small phones to tablets without per-device numbers.
 *
 * Scrolling is only a safety net for: keyboard open, landscape / very short windows, and
 * accessibility font scale above 1.15.
 */
@Composable
internal fun LoginScaffold(
    online: Boolean,
    keyboardPreview: Boolean,
    signupText: String,
    signupEnabled: Boolean,
    onSignup: () -> Unit,
    extraHeight: Dp = 0.dp,
    content: @Composable ColumnScope.(dims: LoginDims, keyboard: Boolean, flex: Boolean) -> Unit
) {
    val colors = authPalette()
    val dark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val density = LocalDensity.current
    AuthSystemBars()
    val reduceMotion = rememberReduceMotion()
    val keyboard = WindowInsets.isImeVisible || keyboardPreview
    val statusDp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navDp = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val footerId = remember(context, dark) {
        (if (dark) listOf("new_footer_dark", "new_footer", "img_footer_lake") else listOf("new_footer", "img_footer_lake"))
            .map { context.resources.getIdentifier(it, "drawable", context.packageName) }
            .firstOrNull { it != 0 } ?: 0
    }

    // Decoded once off the main thread, scaled to the screen width, then cached (see AuthArtCache).
    val footerBmp = rememberAuthArt(footerId)

    Box(Modifier.fillMaxSize().background(colors.background)) {
        BoxWithConstraints(Modifier.widthIn(max = 520.dp).fillMaxSize().align(Alignment.TopCenter)) {
            val dims = loginDims(maxHeight)
            val topBarHeight = statusDp + 8.dp + 48.dp
            val minHeader = topBarHeight + 44.dp
            val pillBlock = SignupPillHeight + 24.dp
            val offlineExtra = if (online) 0.dp else 28.dp
            val avail = maxHeight - navDp - pillBlock - dims.estimate - extraHeight - offlineExtra - 8.dp
            val fit = !keyboard && density.fontScale <= 1.15f && avail >= minHeader
            val headerHeight = when {
                keyboard -> topBarHeight
                fit -> avail.coerceAtMost(MaxHeaderHeight)
                else -> minHeader + 24.dp
            }

            val animatedHeader by animateDpAsState(headerHeight, tween(if (reduceMotion) 0 else 220), label = "login-header")

            // Footer lake art: behind the Google button / pill, only when there is room.
            if (fit && footerBmp != null && maxHeight >= 640.dp) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(dims.footerArt + navDp)) {
                    Image(footerBmp, null, Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop, alignment = Alignment.BottomCenter)
                    Box(Modifier.fillMaxWidth().height(70.dp).background(
                        Brush.verticalGradient(listOf(colors.background, colors.background.copy(alpha = 0f)))))
                    // The dark footer art is already night-toned; only a light scrim keeps the form legible.
                    if (dark) Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.25f)))
                }
            }

            // Keep the scroll modifier (and its focus nodes) attached during IME changes.
            // Previously the scroll node was inserted as the keyboard appeared, which
            // could invalidate the active text field's focus ancestry mid-gesture.
            val viewportHeight = maxHeight
            Column(
                Modifier.fillMaxSize().navigationBarsPadding().imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                LoginHeader(animatedHeader, topBarHeight, showArt = animatedHeader > topBarHeight + 6.dp, dark = dark)

                if (!online) Box(Modifier.padding(horizontal = 20.dp)) {
                    AuthNotice(stringResource(R.string.auth_offline), isError = false)
                }

                // A scrollable parent measures children with unbounded height. In the
                // normal no-scroll case, give the form its exact remaining height so
                // FlexSpacer weights still distribute free space without a weighted
                // child on the scroll container itself.
                val formHeight = (viewportHeight - navDp - animatedHeader - pillBlock - offlineExtra)
                    .coerceAtLeast(0.dp)
                Column(
                    Modifier.fillMaxWidth()
                        .then(if (fit) Modifier.height(formHeight) else Modifier)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { content(dims, keyboard, fit) }

                if (!keyboard) {
                    SignupPill(signupText, signupEnabled, onSignup,
                        Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun LoginHeader(height: Dp, topBarHeight: Dp, showArt: Boolean, dark: Boolean) {
    val colors = authPalette()
    Box(Modifier.fillMaxWidth().height(height).clipToBounds()) {
        if (showArt) {
            Box(Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(if (dark) colors.sand else Color(0xFFFFD9BF), colors.background))))
            // Very slow "breathing" zoom so the scene feels alive; static when animations are off.
            val reduce = rememberReduceMotion()
            val breathe by rememberInfiniteTransition(label = "login-art").animateFloat(
                1f, 1.035f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "breathe"
            )
            val headerBmp = rememberAuthArt(if (dark) R.drawable.new_header_dark else R.drawable.new_header)
            val artAlpha by animateFloatAsState(if (headerBmp != null) 1f else 0f, tween(250), label = "login-header-art")
            if (headerBmp != null) Image(
                headerBmp, null,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).graphicsLayer {
                    alpha = artAlpha
                    val sc = if (reduce) 1f else breathe
                    scaleX = sc; scaleY = sc; transformOrigin = TransformOrigin(0.5f, 1f)
                },
                contentScale = ContentScale.FillWidth, alignment = Alignment.BottomCenter
            )
            LoginWaveFill(Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter), colors.background)
            Image(
                painterResource(R.drawable.transparent_peach_wave_footer_overlay), null,
                // The solid peach crest would glare on the dark page, so keep only a faint warm glow.
                modifier = Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter).alpha(if (dark) 0.14f else 1f),
                contentScale = ContentScale.FillBounds
            )
            // Blend the peach corners of the wave into the page so no hard edge remains.
            Box(Modifier.fillMaxWidth().height(28.dp).align(Alignment.BottomCenter).background(
                Brush.verticalGradient(listOf(colors.background.copy(alpha = 0f), colors.background))))
        }
        LoginTopBar(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .heightIn(max = topBarHeight))
    }
}

/** Same logo asset + sizing as the onboarding Welcome header; compact language chip on the right. */
@Composable
private fun LoginTopBar(modifier: Modifier = Modifier) {
    val colors = authPalette()
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Image(
            painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f).height(36.dp)
        )
        Spacer(Modifier.width(8.dp))
        LanguagePickerChip(colors.ink, colors.surface.copy(alpha = 0.8f), colors.surface.copy(alpha = 0.8f), compact = true)
    }
}

/**
 * Cream fill under the peach/orange wave PNG. The PNG only paints the two corners (its centre
 * is transparent). Points = the PNG's own top edge as a fraction of its height, every 10% of width.
 */
@Composable
private fun LoginWaveFill(modifier: Modifier, color: Color) {
    val edge = floatArrayOf(0.31f, 0.50f, 0.66f, 0.77f, 0.87f, 0.91f, 0.89f, 0.79f, 0.64f, 0.38f, 0.23f)
    Canvas(modifier) {
        val step = size.width / (edge.size - 1)
        val path = Path().apply {
            moveTo(0f, edge[0] * size.height)
            for (i in 1 until edge.size) {
                val x0 = (i - 1) * step; val y0 = edge[i - 1] * size.height
                val x1 = i * step; val y1 = edge[i] * size.height
                val mx = (x0 + x1) / 2f
                cubicTo(mx, y0, mx, y1, x1, y1)
            }
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun SignupPill(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = authPalette()
    val shape = RoundedCornerShape(50)
    // "New to NagpurPulse? Sign up" -> highlight everything after the question mark.
    val split = text.indexOfFirst { it == '?' || it == '？' } + 1
    val label = buildAnnotatedString {
        if (split in 1 until text.length) {
            append(text.substring(0, split).trimEnd() + " ")
            withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Bold)) {
                append(text.substring(split).trim())
            }
        } else append(text)
    }
    Box(
        modifier.fillMaxWidth().heightIn(min = SignupPillHeight).clip(shape)
            .background(colors.surface.copy(alpha = 0.97f))
            .then(if (enabled) Modifier.pressScale { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = colors.ink, fontSize = 14.sp, maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}
