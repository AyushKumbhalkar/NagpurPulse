package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun AuthHeader() {
    val colors = authPalette()
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(if (LocalIsDarkTheme.current) R.drawable.nagpurpulse_logo_dark else R.drawable.nagpurpulse_logo),
            contentDescription = stringResource(R.string.app_name), contentScale = ContentScale.Fit,
            modifier = Modifier.weight(1f).height(48.dp)
        )
        Spacer(Modifier.width(8.dp))
        LanguagePickerChip(colors.ink, colors.sand, colors.sand)
    }
}
