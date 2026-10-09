package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun AuthAction(text: String, loading: Boolean = false, enabled: Boolean = true, google: Boolean = false, contentColor: Color? = null, height: androidx.compose.ui.unit.Dp = 56.dp, elevated: Boolean = false, onClick: () -> Unit) {
    val colors = authPalette()
    val shape = RoundedCornerShape(24.dp)
    val ink = contentColor ?: if (google) colors.ink else AuthTokens.OnGradient
    // Tactile feedback: the button dips slightly while pressed and ticks on tap.
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed) 0.97f else 1f, androidx.compose.animation.core.tween(90), label = "action-press")
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onClick() },
        interactionSource = interaction, enabled = enabled && !loading, shape = shape,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, contentColor = ink,
            disabledContainerColor = Color.Transparent, disabledContentColor = ink
        ),
        modifier = Modifier.fillMaxWidth().height(height).graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .alpha(if (loading || !enabled) 0.65f else 1f)
            .then(if (elevated && !loading && enabled) {
                // Orange glow under the primary button, soft lift under the Google button.
                if (google) Modifier.shadow(3.dp, shape, ambientColor = colors.ink.copy(alpha = 0.12f), spotColor = colors.ink.copy(alpha = 0.12f))
                else Modifier.shadow(10.dp, shape, ambientColor = AuthTokens.Vermilion.copy(alpha = 0.35f), spotColor = AuthTokens.Saffron.copy(alpha = 0.55f))
            } else Modifier)
            .clip(shape).then(if (google) Modifier.background(colors.surface) else Modifier.background(AuthTokens.Gradient))
            .then(if (google && elevated) Modifier.border(1.dp, colors.outline.copy(alpha = 0.22f), shape) else Modifier)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = ink, strokeWidth = 2.dp)
        else if (google) Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
        if (loading || google) Spacer(Modifier.width(8.dp))
        AuthFitText(text, Modifier.weight(1f, fill = false), color = ink, maxSize = 16, minSize = 11, weight = FontWeight.Bold)
        if (!google && !loading) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowForward, null, Modifier.size(20.dp))
        }
    }
}
