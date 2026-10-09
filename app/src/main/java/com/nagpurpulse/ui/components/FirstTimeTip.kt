// java/com/nagpurpulse/ui/components/FirstTimeTip.kt
package com.nagpurpulse.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.OrangeSubtle
import kotlinx.coroutines.delay

/** Remembers which one-time hints and "first time" moments the user has already had. */
object TipPrefs {
    private const val FILE = "premium_tips"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** True when this tip was already shown (also true if storage is unavailable, so we never nag). */
    fun seen(context: Context, key: String): Boolean =
        runCatching { prefs(context).getBoolean(key, false) }.getOrDefault(true)

    fun markSeen(context: Context, key: String) {
        runCatching { prefs(context).edit().putBoolean(key, true).apply() }
    }

    /** Returns true exactly once per install for [key]. */
    fun consume(context: Context, key: String): Boolean {
        if (seen(context, key)) return false
        markSeen(context, key)
        return true
    }
}

/**
 * A small, friendly hint shown once, ever. It slides in a moment after the screen
 * appears, hides itself after a few seconds, and never comes back.
 */
@Composable
fun FirstTimeTip(
    tipKey: String,
    message: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    autoHideMillis: Long = 8_000
) {
    val context = LocalContext.current
    var visible by remember(tipKey) { mutableStateOf(false) }

    LaunchedEffect(tipKey, enabled) {
        if (enabled && !TipPrefs.seen(context, tipKey)) {
            delay(900)
            visible = true
            // Marked as seen the moment it appears, so it can never nag even if the user leaves.
            TipPrefs.markSeen(context, tipKey)
            delay(autoHideMillis)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Box(Modifier.padding(vertical = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrangeSubtle)
                    .border(1.dp, OrangePrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.tip_got_it),
                    color = OrangePrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { visible = false }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}
