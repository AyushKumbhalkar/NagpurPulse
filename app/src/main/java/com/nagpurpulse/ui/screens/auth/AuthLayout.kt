package com.nagpurpulse.ui.screens.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.nagpurpulse.ui.theme.LocalIsDarkTheme

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
