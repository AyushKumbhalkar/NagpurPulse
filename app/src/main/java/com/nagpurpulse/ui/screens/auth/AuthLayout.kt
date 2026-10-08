@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.nagpurpulse.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.authPalette

internal enum class AuthHeightTier { Compact, Medium, Expanded }

internal data class AuthLayoutSpec(
    val tier: AuthHeightTier, val keyboard: Boolean, val minimal: Boolean
) {
    val gap get() = when (tier) {
        AuthHeightTier.Compact -> 8.dp
        AuthHeightTier.Medium -> 16.dp
        AuthHeightTier.Expanded -> 24.dp
    }
}

/**
 * At ordinary font scales the Column is bounded and has no scroll modifier.
 * At >1.3, its intrinsic (fixed-content) height is measured before laying it out.
 * Only an actual overflow installs verticalScroll. The primary action lives outside
 * this viewport and remains pinned even when the accessibility fallback is active.
 * Content must support intrinsic measurements (no nested BoxWithConstraints).
 */
@Composable
internal fun FitOrScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    var overflow by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier) {
        val viewportHeight = constraints.maxHeight
        val canScroll = largeText && overflow
        Layout(
            modifier = Modifier.fillMaxSize().then(if (canScroll) Modifier.verticalScroll(scroll) else Modifier),
            content = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, content = content)
            }
        ) { measurables, incoming ->
            val child = measurables.single()
            // Query only the plain Column, never the scroll modifier (whose
            // unbounded intrinsic constraints are unsupported by Compose).
            val needed = if (largeText) child.minIntrinsicHeight(incoming.maxWidth) else 0
            val nextOverflow = largeText && needed > viewportHeight
            if (overflow != nextOverflow) overflow = nextOverflow
            val height = if (canScroll) maxOf(viewportHeight, needed) else viewportHeight
            val placeable = child.measure(incoming.copy(minHeight = height, maxHeight = height))
            layout(incoming.maxWidth, height) { placeable.place(0, 0) }
        }
    }
}

@Composable
internal fun AuthScaffold(
    welcome: Boolean = false,
    keyboardPreview: Boolean = false,
    online: Boolean = true,
    primary: @Composable () -> Unit,
    footer: @Composable (AuthLayoutSpec) -> Unit,
    content: @Composable ColumnScope.(AuthLayoutSpec) -> Unit
) {
    val colors = authPalette()
    AuthSystemBars()
    val keyboard = WindowInsets.isImeVisible || keyboardPreview
    val largeText = LocalDensity.current.fontScale > 1.3f
    val reduceMotion = rememberReduceMotion()
    val artAlpha by animateFloatAsState(
        if (keyboard) 0f else 1f, tween(if (reduceMotion) 0 else 180), label = "auth-art-collapse"
    )
    Box(
        Modifier.fillMaxSize().background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .navigationBarsPadding().imePadding()
            .padding(bottom = if (keyboardPreview) 260.dp else 0.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            val tier = remember(maxHeight) {
                when {
                    maxHeight < 640.dp -> AuthHeightTier.Compact
                    maxHeight <= 780.dp -> AuthHeightTier.Medium
                    else -> AuthHeightTier.Expanded
                }
            }
            val spec = AuthLayoutSpec(tier, keyboard, keyboard && maxHeight < 440.dp)
            if (!welcome && !keyboard && tier == AuthHeightTier.Expanded) {
                AuthArtwork("img_footer_lake", Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(144.dp), alpha = 0.12f)
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                if (!spec.minimal) AuthHeader()
                if (!online) AuthNotice(androidx.compose.ui.res.stringResource(com.nagpurpulse.R.string.auth_offline), isError = false)
                FitOrScrollColumn(Modifier.weight(1f)) {
                    // The only flexible region. Fixed form elements consume their natural
                    // heights first; illustration gets exactly the remainder (including zero).
                    if (!largeText) {
                        AuthArtwork(
                            name = if (welcome) "img_welcome_scene" else "img_header_skyline",
                            modifier = Modifier.fillMaxWidth().weight(1f), alpha = artAlpha
                        )
                    }
                    content(spec)
                    if (largeText && !keyboard) footer(spec)
                }
                primary()
                if (!keyboard && !largeText) footer(spec)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private tailrec fun Context.authActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.authActivity()
    else -> null
}

@Composable
internal fun AuthSystemBars() {
    val view = LocalView.current
    val dark = LocalIsDarkTheme.current
    val preview = LocalInspectionMode.current
    DisposableEffect(view, dark, preview) {
        val window = if (preview) null else view.context.authActivity()?.window
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
