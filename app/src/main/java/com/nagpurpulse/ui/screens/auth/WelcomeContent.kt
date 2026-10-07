package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun WelcomeContent(
    onGetStarted: () -> Unit = {}, onLogin: () -> Unit = {},
    liveCount: Int? = null, keyboardPreview: Boolean = false
) {
    val colors = authPalette()
    AuthScaffold(welcome = true, keyboardPreview = keyboardPreview,
        primary = { AuthEntrance(3) { AuthAction(stringResource(R.string.auth_get_started), onClick = onGetStarted) } },
        footer = { _ -> AuthLink(stringResource(R.string.auth_login_footer), modifier = Modifier.fillMaxWidth(), onClick = onLogin) }
    ) { spec ->
        // TODO: wire a real online-count stream when the existing repository exposes one.
        // There is intentionally no placeholder or simulated community count.
        LiveActivityPill(liveCount)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.auth_topic_traffic, R.string.auth_topic_poha, R.string.auth_topic_sunset).forEach {
                Box(Modifier.weight(1f).background(colors.sand, RoundedCornerShape(24.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    AuthFitText(stringResource(it), maxSize = 11, minSize = 9, maxLines = 2)
                }
            }
        }
        AuthEntrance(1) {
            AuthHeadline(stringResource(R.string.auth_city), stringResource(R.string.auth_together), spec.tier == AuthHeightTier.Compact)
        }
        AuthEntrance(2) {
            AuthFitText(stringResource(R.string.auth_welcome_subtitle), Modifier.fillMaxWidth(), maxSize = 14, maxLines = 3)
        }
        Spacer(Modifier.height(spec.gap))
    }
}

@Composable
internal fun LiveActivityPill(count: Int?) {
    if (count == null || count < 0) return
    val colors = authPalette()
    Text(stringResource(R.string.auth_live_count, count), color = colors.ink,
        modifier = Modifier.padding(top = 8.dp).background(colors.sand, RoundedCornerShape(24.dp)).padding(8.dp))
}
