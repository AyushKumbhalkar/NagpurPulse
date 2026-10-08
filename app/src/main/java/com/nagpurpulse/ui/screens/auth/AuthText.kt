@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.authPalette

/** Font used by the shared auth widgets. Screens that want Nunito provide it; default keeps old look. */
internal val LocalAuthFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** Width adaptation, not a font-scale cap. Large-text overflow can use the scroll fallback. */
@Composable
internal fun AuthFitText(
    text: String, modifier: Modifier = Modifier, color: Color = authPalette().ink,
    maxSize: Int = 14, minSize: Int = 11, maxLines: Int = 1,
    weight: FontWeight = FontWeight.Normal, gradient: Boolean = false, brightGradient: Boolean = false
) {
    var size by remember(text, maxSize) { mutableStateOf(maxSize) }
    Text(text, modifier, color = if (gradient) Color.Unspecified else color, fontSize = size.sp, lineHeight = (size + 4).sp,
        fontWeight = weight, fontFamily = LocalAuthFont.current, textAlign = TextAlign.Center, maxLines = maxLines,
        style = if (gradient) TextStyle(brush = if (brightGradient || LocalIsDarkTheme.current) AuthTokens.Gradient else AuthTokens.HeadlineGradient) else TextStyle.Default,
        onTextLayout = { if (it.hasVisualOverflow && size > minSize) size-- })
}

@Composable
internal fun AuthHeadline(first: String, accent: String, compact: Boolean, brightAccent: Boolean = false, sizeSp: Int? = null, lineDp: Int? = null) {
    val largeText = LocalDensity.current.fontScale > 1.3f
    val lineModifier = if (largeText) Modifier else Modifier.height(if (compact) 24.dp else (lineDp ?: 40).dp)
    Column(Modifier.fillMaxWidth().padding(vertical = if (compact) 0.dp else 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AuthFitText(first, lineModifier, maxSize = if (compact) 20 else (sizeSp ?: 36), minSize = 14, weight = FontWeight.ExtraBold)
        AuthFitText(accent, lineModifier, maxSize = if (compact) 20 else (sizeSp ?: 36), minSize = 14, weight = FontWeight.ExtraBold, gradient = true, brightGradient = brightAccent)
    }
}

@Composable
internal fun AuthNotice(message: String?, isError: Boolean = true) {
    if (message.isNullOrBlank()) return
    val colors = authPalette()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically) {
        if (isError) {
            Icon(Icons.Filled.ErrorOutline, null, Modifier.size(16.dp), tint = colors.error)
            Spacer(Modifier.width(8.dp))
        }
        AuthFitText(message, Modifier.weight(1f), color = if (isError) colors.error else colors.muted,
            maxSize = 12, minSize = 10, maxLines = 2)
    }
}

@Composable
internal fun AuthOr() {
    val colors = authPalette()
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = colors.outline.copy(alpha = 0.4f))
        AuthFitText(stringResource(R.string.auth_or), Modifier.padding(horizontal = 16.dp), color = colors.muted, maxSize = 12, minSize = 8)
        HorizontalDivider(Modifier.weight(1f), color = colors.outline.copy(alpha = 0.4f))
    }
}

@Composable
internal fun AuthLink(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick, enabled = enabled, modifier = modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
        AuthFitText(text, color = authPalette().ink, maxSize = 13, maxLines = 2, weight = FontWeight.SemiBold)
    }
}
