@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.nagpurpulse.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.authPalette

// White text on the old #FF941F start colour fails contrast (2.2:1). These two stops keep the
// same saffron-to-vermilion look but give 3.1:1+ against white bold 18sp text (WCAG AA large).
private val WelcomeButtonBrush = Brush.horizontalGradient(listOf(Color(0xFFF26A00), Color(0xFFE8321A)))
private val WelcomeAccentBrushLight = Brush.horizontalGradient(listOf(Color(0xFFEE6A00), Color(0xFFE5381A)))

private enum class WelcomeTier { Compact, Medium, Expanded }

/**
 * Welcome screen. Self-contained: it does not use AuthScaffold, because the scaffold pads the
 * artwork inside 16dp margins and clips all four corners. Existing callbacks are unchanged.
 */
@Composable
internal fun WelcomeContent(
    onGetStarted: () -> Unit = {},
    onLogin: () -> Unit = {},
    liveCount: Int? = null,
    @Suppress("UNUSED_PARAMETER") keyboardPreview: Boolean = false
) {
    val colors = authPalette()
    val dark = LocalIsDarkTheme.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    WelcomeSystemBars()

    Box(
        Modifier.fillMaxSize().background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            val tier = when {
                maxHeight < 640.dp -> WelcomeTier.Compact
                maxHeight <= 780.dp -> WelcomeTier.Medium
                else -> WelcomeTier.Expanded
            }
            val compact = tier == WelcomeTier.Compact
            val headlineSp = when (tier) { WelcomeTier.Compact -> 32; WelcomeTier.Medium -> 40; WelcomeTier.Expanded -> 44 }
            val subtitleSp = if (compact) 14 else 16
            val buttonHeight = if (tier == WelcomeTier.Expanded) 56.dp else 52.dp

            // Large-font accessibility fallback only. At normal font scale nothing scrolls:
            // the hero is the single flexible element and absorbs the leftover height.
            Column(
                Modifier.fillMaxSize().then(if (largeText) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeHeader()
                WelcomeHero(
                    modifier = Modifier.fillMaxWidth().then(if (largeText) Modifier.height(220.dp) else Modifier.weight(1f)),
                    liveCount = liveCount.takeIf { !compact },
                    dark = dark
                )
                if (!compact) {
                    WelcomeTopicChips(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                    AuthEntrance(1) {
                        WelcomeHeadline(
                            first = stringResource(R.string.auth_city),
                            accent = stringResource(R.string.auth_together),
                            baseSp = headlineSp,
                            brush = if (dark) AuthTokens.Gradient else WelcomeAccentBrushLight,
                            inkColor = colors.ink,
                            limitLines = !largeText
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    AuthEntrance(2) {
                        Text(
                            stringResource(R.string.auth_welcome_subtitle),
                            Modifier.fillMaxWidth(),
                            color = colors.muted, fontSize = subtitleSp.sp, lineHeight = (subtitleSp + 6).sp,
                            textAlign = TextAlign.Center, maxLines = if (largeText) Int.MAX_VALUE else 3
                        )
                    }
                    Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
                    AuthEntrance(3) {
                        WelcomeButton(stringResource(R.string.auth_get_started), buttonHeight, onGetStarted)
                    }
                    WelcomeLoginLink(stringResource(R.string.auth_login_footer), colors.muted, colors.ink, onLogin)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    val colors = authPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f).height(40.dp)
        )
        Spacer(Modifier.width(8.dp))
        LanguagePickerChip(colors.ink, colors.sand, colors.sand)
    }
}

/**
 * Full-bleed hero: sunset sky drawn in Compose, then the transparent illustration on top.
 * FillWidth + BottomCenter keeps the steps and faces anchored; when space is short, only
 * the top (sky / tree tops) is cropped. Only the bottom corners are rounded.
 */
@Composable
private fun WelcomeHero(modifier: Modifier, liveCount: Int?, dark: Boolean) {
    val sky = if (dark) {
        listOf(Color(0xFF3A2A1E), Color(0xFF5A3A22), Color(0xFF171411))
    } else {
        listOf(Color(0xFFF7C27A), Color(0xFFFBD9A0), Color(0xFFFFF3E2))
    }
    Box(
        modifier.clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).drawBehind {
            drawRect(Brush.verticalGradient(sky))
            val sun = Offset(size.width * 0.66f, size.height * 0.30f)
            val core = 26.dp.toPx()
            drawCircle(
                Brush.radialGradient(listOf(Color(0xCCFFE9B0), Color(0x00FFE9B0)), sun, core * 3.2f),
                radius = core * 3.2f, center = sun
            )
            drawCircle(Color(0xFFFFF1C9), radius = core, center = sun)
            val cloud = Color.White.copy(alpha = if (dark) 0.08f else 0.35f)
            drawOval(cloud, Offset(size.width * 0.06f, size.height * 0.16f), Size(96.dp.toPx(), 20.dp.toPx()))
            drawOval(cloud, Offset(size.width * 0.58f, size.height * 0.09f), Size(110.dp.toPx(), 22.dp.toPx()))
            val bird = Color(0xFF8A4B1F).copy(alpha = 0.55f)
            drawBird(Offset(size.width * 0.30f, size.height * 0.13f), 7.dp.toPx(), bird)
            drawBird(Offset(size.width * 0.38f, size.height * 0.19f), 5.dp.toPx(), bird)
        }
    ) {
        Image(
            painterResource(R.drawable.hero_nagpur),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        )
        if (liveCount != null && liveCount >= 0) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) { LiveActivityPill(liveCount) }
        }
    }
}

private fun DrawScope.drawBird(center: Offset, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - s, center.y)
        quadraticBezierTo(center.x - s / 2, center.y - s / 2, center.x, center.y)
        quadraticBezierTo(center.x + s / 2, center.y - s / 2, center.x + s, center.y)
    }
    drawPath(path, color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
}

/** Only drawn when a REAL count is supplied. There is deliberately no placeholder number. */
@Composable
internal fun LiveActivityPill(count: Int?) {
    if (count == null || count < 0) return
    val colors = authPalette()
    val reduce = rememberReduceMotion()
    val pulse = if (reduce) 1f else {
        val t = rememberInfiniteTransition(label = "live-dot")
        val a by t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "live-dot-alpha")
        a
    }
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(colors.surface.copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).alpha(pulse).clip(CircleShape).background(Color(0xFF22C55E)))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.auth_live_count, count),
            color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
        )
    }
}

@Composable
private fun WelcomeTopicChips(modifier: Modifier = Modifier) {
    val colors = authPalette()
    val topics = listOf(
        Icons.Filled.DirectionsCar to R.string.auth_topic_traffic,
        Icons.Filled.Restaurant to R.string.auth_topic_poha,
        Icons.Filled.WbTwilight to R.string.auth_topic_sunset
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        topics.forEach { (icon, label) ->
            Row(
                Modifier.weight(1f).heightIn(min = 36.dp).clip(RoundedCornerShape(50)).background(colors.sand)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                AuthFitText(
                    stringResource(label), Modifier.weight(1f, fill = false), color = colors.ink,
                    maxSize = 12, minSize = 9, maxLines = 2, weight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeadline(
    first: String, accent: String, baseSp: Int, brush: Brush, inkColor: Color, limitLines: Boolean
) {
    var size by remember(first, accent, baseSp) { mutableStateOf(baseSp) }
    val text = buildAnnotatedString {
        append(first)
        append("\n")
        withStyle(SpanStyle(brush = brush)) { append(accent) }
    }
    Text(
        text, Modifier.fillMaxWidth(),
        color = inkColor, fontSize = size.sp, lineHeight = (size + 2).sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, textAlign = TextAlign.Center,
        maxLines = if (limitLines) 2 else Int.MAX_VALUE,
        onTextLayout = { if (limitLines && it.hasVisualOverflow && size > 26) size -= 2 }
    )
}

@Composable
private fun WelcomeButton(text: String, height: Dp, onClick: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Button(
        onClick = onClick, shape = shape,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp,
            hoveredElevation = 0.dp, disabledElevation = 0.dp
        ),
        modifier = Modifier.fillMaxWidth().height(height).clip(shape).background(WelcomeButtonBrush)
    ) {
        AuthFitText(
            text, Modifier.weight(1f, fill = false), color = Color.White,
            maxSize = 18, minSize = 12, weight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/** "Already have an account? Log in": everything after the first '?' is bold ink (any language). */
@Composable
private fun WelcomeLoginLink(text: String, muted: Color, ink: Color, onClick: () -> Unit) {
    val split = text.indexOf('?').let { if (it in 0 until text.lastIndex) it + 1 else -1 }
    val styled = buildAnnotatedString {
        if (split < 0) {
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.Bold)) { append(text) }
        } else {
            withStyle(SpanStyle(color = muted)) { append(text.substring(0, split)) }
            withStyle(SpanStyle(color = ink, fontWeight = FontWeight.Bold)) { append(text.substring(split)) }
        }
    }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(styled, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}

private tailrec fun Context.welcomeActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.welcomeActivity()
    else -> null
}

/** Transparent status bar with dark icons on the light theme (white-on-cream was unreadable). */
@Composable
private fun WelcomeSystemBars() {
    val view = LocalView.current
    val dark = LocalIsDarkTheme.current
    val preview = LocalInspectionMode.current
    DisposableEffect(view, dark, preview) {
        val window = if (preview) null else view.context.welcomeActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val oldStatus = controller?.isAppearanceLightStatusBars
        val oldNav = controller?.isAppearanceLightNavigationBars
        val oldColor = window?.statusBarColor
        window?.statusBarColor = android.graphics.Color.TRANSPARENT
        controller?.isAppearanceLightStatusBars = !dark
        controller?.isAppearanceLightNavigationBars = !dark
        onDispose {
            if (oldStatus != null) controller?.isAppearanceLightStatusBars = oldStatus
            if (oldNav != null) controller?.isAppearanceLightNavigationBars = oldNav
            if (oldColor != null) window?.statusBarColor = oldColor
        }
    }
}