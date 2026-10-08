@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nagpurpulse.ui.theme.AuthTokens
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

private val FooterArtHeight = 170.dp
private val SignupPillHeight = 44.dp

/**
 * Login-only layout (mockup panels 3 + 4):
 *  - skyline illustration bleeds behind the status bar, logo + language chip sit on top of it
 *  - peach wave overlay closes the illustration; the headline overlaps the wave
 *  - lake illustration sits at the bottom with the "Sign up" pill floating over it
 * With the keyboard open both illustrations collapse and the form scrolls.
 */
@Composable
internal fun LoginScaffold(
    online: Boolean,
    keyboardPreview: Boolean,
    signupText: String,
    signupEnabled: Boolean,
    onSignup: () -> Unit,
    content: @Composable ColumnScope.(keyboard: Boolean, compact: Boolean) -> Unit
) {
    val colors = authPalette()
    val dark = LocalIsDarkTheme.current
    val context = LocalContext.current
    AuthSystemBars()
    val keyboard = WindowInsets.isImeVisible || keyboardPreview
    val footerId = remember(context) {
        // new_footer.png is the target asset; fall back to the old lake art if it is missing.
        listOf("new_footer", "img_footer_lake")
            .map { context.resources.getIdentifier(it, "drawable", context.packageName) }
            .firstOrNull { it != 0 } ?: 0
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        BoxWithConstraints(Modifier.widthIn(max = 480.dp).fillMaxSize().align(Alignment.TopCenter)) {
            val compact = maxHeight < 640.dp
            val headerHeight: Dp = when {
                keyboard -> 0.dp
                else -> (maxHeight * 0.31f).coerceIn(150.dp, 280.dp)
            }

            // ── Footer art (behind content, anchored to bottom) ───────────────
            if (!keyboard && footerId != 0) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(FooterArtHeight)) {
                    Image(
                        painterResource(footerId), null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop, alignment = Alignment.BottomCenter
                    )
                    // Fade the top edge of the art into the page background.
                    Box(Modifier.fillMaxWidth().height(90.dp).background(
                        Brush.verticalGradient(listOf(colors.background, colors.background.copy(alpha = 0f)))
                    ))
                    if (dark) Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.55f)))
                }
            }

            Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
                // ── Header: illustration + wave + top bar ─────────────────────
                Box(Modifier.fillMaxWidth().then(
                    if (keyboard) Modifier.wrapContentHeight() else Modifier.height(headerHeight)
                )) {
                    if (!keyboard) {
                        Box(Modifier.matchParentSize().background(
                            Brush.verticalGradient(listOf(
                                if (dark) colors.sand else Color(0xFFFFD9BF),
                                colors.background
                            ))
                        ))
                        Image(
                            painterResource(R.drawable.new_header), null,
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                            contentScale = ContentScale.FillWidth, alignment = Alignment.BottomCenter
                        )
                        if (dark) Box(Modifier.matchParentSize().background(colors.background.copy(alpha = 0.55f)))
                        LoginWaveFill(Modifier.fillMaxWidth().height(70.dp).align(Alignment.BottomCenter), colors.background)
                        Image(
                            painterResource(R.drawable.transparent_peach_wave_footer_overlay), null,
                            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(70.dp),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                    LoginTopBar(Modifier.statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp))
                }

                if (!online) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        AuthNotice(stringResource(R.string.auth_offline), isError = false)
                    }
                }

                // ── Form area ────────────────────────────────────────────────
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = if (keyboard) 8.dp else SignupPillHeight + 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { content(keyboard, compact) }
            }

            // ── Sign-up pill floating over the footer art ───────────────────
            if (!keyboard) {
                SignupPill(
                    signupText, signupEnabled, onSignup,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                        .padding(horizontal = 16.dp).padding(bottom = 12.dp)
                )
            }
        }
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

/** Same logo asset + sizing as the onboarding Welcome header; compact language chip on the right. */
@Composable
private fun LoginTopBar(modifier: Modifier = Modifier) {
    val colors = authPalette()
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
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
 * Cream fill that sits under the peach/orange wave PNG. The PNG only paints the two corners
 * (its centre is transparent), so without this the illustration just fades out.
 * Points = the PNG's own top edge, as a fraction of its height, sampled every 10% of width.
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
