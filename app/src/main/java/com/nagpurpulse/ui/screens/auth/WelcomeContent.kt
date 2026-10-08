@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.nagpurpulse.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.nagpurpulse.R
import com.nagpurpulse.ui.locale.AppLocale
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.authPalette

// White text on the old #FF941F start colour fails contrast (2.2:1). These stops keep the same
// saffron-to-vermilion look with 3.1:1+ against white bold 18sp text (WCAG AA large text).
private val WelcomeButtonBrush = Brush.horizontalGradient(listOf(Color(0xFFF26A00), Color(0xFFE8321A)))
internal val WelcomeAccentBrushLight = Brush.horizontalGradient(listOf(Color(0xFFEE6A00), Color(0xFFE5381A)))

private enum class WelcomeTier { Compact, Medium, Expanded }

/**
 * Nunito from res/font. Looked up by name so a wrong file name can never break the build:
 * if the files are missing or misnamed the screen simply falls back to the system font.
 * Expected files: nunito_regular, nunito_semibold, nunito_bold, nunito_extrabold (.ttf).
 */
@Composable
internal fun rememberWelcomeFont(): FontFamily {
    val context = LocalContext.current
    return remember(context) {
        val fonts = listOf(
            "nunito_regular" to FontWeight.Normal,
            "nunito_semibold" to FontWeight.SemiBold,
            "nunito_bold" to FontWeight.Bold,
            "nunito_extrabold" to FontWeight.ExtraBold
        ).mapNotNull { (name, weight) ->
            val id = context.resources.getIdentifier(name, "font", context.packageName)
            if (id != 0) Font(id, weight) else null
        }
        if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
    }
}

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
    val font = rememberWelcomeFont()
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
                WelcomeHeader(font)
                WelcomeHero(
                    modifier = Modifier.fillMaxWidth().then(if (largeText) Modifier.height(220.dp) else Modifier.weight(1f)),
                    liveCount = liveCount.takeIf { !compact },
                    dark = dark,
                    font = font
                )
                if (!compact) {
                    WelcomeTopicChips(font, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                    AuthEntrance(1) {
                        WelcomeHeadline(
                            first = stringResource(R.string.auth_city),
                            accent = stringResource(R.string.auth_together),
                            baseSp = headlineSp,
                            brush = if (dark) AuthTokens.Gradient else WelcomeAccentBrushLight,
                            inkColor = colors.ink,
                            font = font,
                            limitLines = !largeText
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    AuthEntrance(2) {
                        Text(
                            stringResource(R.string.auth_welcome_subtitle),
                            Modifier.fillMaxWidth(),
                            color = colors.muted, fontSize = subtitleSp.sp, lineHeight = (subtitleSp + 6).sp,
                            fontFamily = font, fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center, maxLines = if (largeText) Int.MAX_VALUE else 3
                        )
                    }
                    Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
                    AuthEntrance(3) {
                        WelcomeButton(stringResource(R.string.auth_get_started), buttonHeight, font, onGetStarted)
                    }
                    WelcomeLoginLink(stringResource(R.string.auth_login_footer), colors.muted, colors.ink, font, onLogin)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun WelcomeHeader(font: FontFamily) {
    val colors = authPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            modifier = Modifier.weight(1f).height(36.dp)
        )
        Spacer(Modifier.width(8.dp))
        WelcomeLanguageChip(colors.ink, colors.sand, font)
    }
}

/**
 * Compact language chip: 36dp visible pill inside a 48dp touch target. Same behaviour and
 * analytics event as LanguagePickerChip, which stays untouched for Login / Sign up.
 */
@Composable
internal fun WelcomeLanguageChip(ink: Color, background: Color, font: FontFamily) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val currentTag = AppLocale.currentTag(context)
    val currentName = AppLocale.options.first { it.tag == currentTag }.nativeName
    val description = stringResource(R.string.language_picker_cd)
    Box(
        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClickLabel = description) { expanded = true },
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(50)).background(background)
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Language, contentDescription = description, tint = ink, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(currentName, color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = font)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLocale.options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.nativeName,
                            fontWeight = if (option.tag == currentTag) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        if (option.tag != currentTag) {
                            AuthAnalytics.log(context, "language_changed", "language" to option.tag)
                            context.welcomeActivity()?.let { AppLocale.apply(it, option.tag) }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Full-bleed hero: sunset sky drawn in Compose, then the transparent illustration on top.
 * FillWidth + BottomCenter keeps the steps and faces anchored; when space is short, only the
 * top (sky / tree tops) is cropped. Sky details are positioned in dp from the TOP so they stay
 * in the open sky on every screen height. Only the bottom corners are rounded.
 */
@Composable
private fun WelcomeHero(modifier: Modifier, liveCount: Int?, dark: Boolean, font: FontFamily) {
    val sky = if (dark) {
        listOf(Color(0xFF3A2A1E), Color(0xFF5A3A22), Color(0xFF171411))
    } else {
        listOf(Color(0xFFF7C27A), Color(0xFFFBD9A0), Color(0xFFFFF3E2))
    }
    Box(
        modifier.clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)).drawBehind {
            drawRect(Brush.verticalGradient(sky))
            // soft clouds (fade out at both ends, so no hard oval edges)
            val cloud = Color.White.copy(alpha = if (dark) 0.10f else 0.55f)
            drawCloud(Offset(size.width * 0.02f, 84.dp.toPx()), Size(110.dp.toPx(), 18.dp.toPx()), cloud)
            drawCloud(Offset(size.width * 0.58f, 14.dp.toPx()), Size(120.dp.toPx(), 18.dp.toPx()), cloud)
            // sun in the gap between the Zero Mile stone and the dome
            val sun = Offset(size.width * 0.66f, 46.dp.toPx())
            val core = 20.dp.toPx()
            drawCircle(
                Brush.radialGradient(listOf(Color(0xCCFFE9B0), Color(0x00FFE9B0)), sun, core * 3.2f),
                radius = core * 3.2f, center = sun
            )
            drawCircle(Color(0xFFFFF1C9), radius = core, center = sun)
            val bird = Color(0xFF8A4B1F).copy(alpha = 0.55f)
            drawBird(Offset(size.width * 0.76f, 20.dp.toPx()), 7.dp.toPx(), bird)
            drawBird(Offset(size.width * 0.82f, 34.dp.toPx()), 5.dp.toPx(), bird)
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
            Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) { LiveActivityPill(liveCount, font) }
        }
    }
}

private fun DrawScope.drawCloud(topLeft: Offset, size: Size, color: Color) {
    drawOval(
        Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent), startX = topLeft.x, endX = topLeft.x + size.width),
        topLeft, size
    )
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
internal fun LiveActivityPill(count: Int?, font: FontFamily = FontFamily.Default) {
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
            color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = font, maxLines = 1
        )
    }
}

@Composable
private fun WelcomeTopicChips(font: FontFamily, modifier: Modifier = Modifier) {
    val colors = authPalette()
    val topics = listOf(
        Icons.Filled.DirectionsCar to R.string.auth_topic_traffic,
        Icons.Filled.Restaurant to R.string.auth_topic_poha,
        Icons.Filled.WbTwilight to R.string.auth_topic_sunset
    )
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        topics.forEach { (icon, label) ->
            Row(
                Modifier.weight(1f).heightIn(min = 32.dp).clip(RoundedCornerShape(50)).background(colors.sand)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                WelcomeFitText(
                    stringResource(label), Modifier.weight(1f, fill = false), colors.ink,
                    maxSize = 12, minSize = 10, weight = FontWeight.SemiBold, font = font
                )
            }
        }
    }
}

/** One line, shrinks to [minSize]; only if that still does not fit does it wrap to two lines. */
@Composable
internal fun WelcomeFitText(
    text: String, modifier: Modifier, color: Color, maxSize: Int, minSize: Int,
    weight: FontWeight, font: FontFamily
) {
    var size by remember(text, maxSize) { mutableStateOf(maxSize) }
    var wrap by remember(text, maxSize) { mutableStateOf(false) }
    Text(
        text, modifier, color = color, fontSize = size.sp, lineHeight = (size + 3).sp,
        fontWeight = weight, fontFamily = font, textAlign = TextAlign.Center,
        maxLines = if (wrap) 2 else 1, softWrap = wrap, overflow = TextOverflow.Ellipsis,
        onTextLayout = {
            if (it.hasVisualOverflow) {
                if (size > minSize) size-- else if (!wrap) wrap = true
            }
        }
    )
}

@Composable
internal fun WelcomeHeadline(
    first: String, accent: String, baseSp: Int, brush: Brush, inkColor: Color,
    font: FontFamily, limitLines: Boolean
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
        fontWeight = FontWeight.ExtraBold, fontFamily = font, letterSpacing = (-0.5).sp, textAlign = TextAlign.Center,
        maxLines = if (limitLines) 2 else Int.MAX_VALUE,
        onTextLayout = { if (limitLines && it.hasVisualOverflow && size > 26) size -= 2 }
    )
}

@Composable
internal fun WelcomeButton(
    text: String, height: Dp, font: FontFamily, onClick: () -> Unit, loading: Boolean = false
) {
    val shape = RoundedCornerShape(28.dp)
    // Gentle press feedback so the main button feels physical.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        if (pressed && !loading) 0.97f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "button-press"
    )
    Button(
        onClick = onClick, enabled = !loading, shape = shape, interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, contentColor = Color.White,
            disabledContainerColor = Color.Transparent, disabledContentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp,
            hoveredElevation = 0.dp, disabledElevation = 0.dp
        ),
        modifier = Modifier.fillMaxWidth().height(height)
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .alpha(if (loading) 0.65f else 1f)
            .clip(shape).background(WelcomeButtonBrush)
    ) {
        WelcomeFitText(
            text, Modifier.weight(1f, fill = false), Color.White,
            maxSize = 18, minSize = 12, weight = FontWeight.Bold, font = font
        )
        Spacer(Modifier.width(8.dp))
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
    }
}

/** "Already have an account? Log in": everything after the first '?' is bold ink (any language). */
@Composable
internal fun WelcomeLoginLink(
    text: String, muted: Color, ink: Color, font: FontFamily, onClick: () -> Unit, enabled: Boolean = true
) {
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
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(styled, fontSize = 14.sp, fontFamily = font, textAlign = TextAlign.Center, maxLines = 2)
    }
}

private tailrec fun Context.welcomeActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.welcomeActivity()
    else -> null
}

/**
 * Remembers the system-bar look from before the first auth screen and restores it only when the
 * LAST auth screen leaves. During a screen transition the old and new screen overlap, and the old
 * one used to restore white icons AFTER the new one had set dark ones (invisible clock on cream).
 */
private object AuthBars {
    var users = 0
    var oldStatus: Boolean? = null
    var oldNav: Boolean? = null
    var oldColor: Int? = null
}

/** Transparent status bar with dark icons on the light theme. */
@Composable
internal fun WelcomeSystemBars() {
    val view = LocalView.current
    val dark = LocalIsDarkTheme.current
    val preview = LocalInspectionMode.current
    DisposableEffect(view, dark, preview) {
        val window = if (preview) null else view.context.welcomeActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (window != null && controller != null) {
            if (AuthBars.users == 0) {
                AuthBars.oldStatus = controller.isAppearanceLightStatusBars
                AuthBars.oldNav = controller.isAppearanceLightNavigationBars
                AuthBars.oldColor = window.statusBarColor
            }
            AuthBars.users++
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
        onDispose {
            if (window != null && controller != null) {
                AuthBars.users = (AuthBars.users - 1).coerceAtLeast(0)
                if (AuthBars.users == 0) {
                    AuthBars.oldStatus?.let { controller.isAppearanceLightStatusBars = it }
                    AuthBars.oldNav?.let { controller.isAppearanceLightNavigationBars = it }
                    AuthBars.oldColor?.let { window.statusBarColor = it }
                }
            }
        }
    }
}
