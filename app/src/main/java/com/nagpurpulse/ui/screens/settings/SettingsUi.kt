// ui/screens/settings/SettingsUi.kt
//
// Shared building blocks for the settings sub-screens, so every screen has the same
// premium header, the same glowing hero icon and the same "pick one" cards.
package com.nagpurpulse.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.SurfaceAlt
import com.nagpurpulse.ui.theme.TertiaryText

/** The shared header used by every settings sub-screen. */
@Composable
fun SettingsTopBar(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Column(
        Modifier
            .background(Brush.verticalGradient(listOf(Surface, Background), 0f, 130f))
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryText)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing()
        }
        HorizontalDivider(color = Divider, thickness = 0.5.dp)
    }
}

/** An icon sitting in a softly pulsing halo. Pass active = false for a calm, static look. */
@Composable
fun GlowIcon(icon: ImageVector, tint: Color, active: Boolean = true, size: Dp = 56.dp) {
    val transition = rememberInfiniteTransition(label = "glow_icon")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_pulse"
    )
    Box(Modifier.size(size + 22.dp), contentAlignment = Alignment.Center) {
        if (active) {
            Box(
                Modifier
                    .size(size)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f))
            )
        }
        Box(
            Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f))
                .border(1.dp, tint.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.42f))
        }
    }
}

/** A tappable "pick one" card with a morphing border and a check mark. */
@Composable
fun ChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (selected) OrangePrimary else Divider,
        animationSpec = tween(220),
        label = "choice_border"
    )
    val fillColor by animateColorAsState(
        if (selected) OrangePrimary.copy(alpha = 0.10f) else Surface,
        animationSpec = tween(220),
        label = "choice_fill"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(fillColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .pressScale(0.98f, onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, color = SecondaryText, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) OrangePrimary else SurfaceAlt)
                .border(1.dp, if (selected) OrangePrimary else TertiaryText.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** A thin segmented meter, e.g. 3 of 5 steps lit. */
@Composable
fun SegmentMeter(filled: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until total) {
            val segment by animateColorAsState(
                if (i < filled) color else color.copy(alpha = 0.15f),
                animationSpec = tween(350),
                label = "segment_$i"
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(segment)
            )
        }
    }
}

/** Small colored pill used for status text. */
@Composable
fun StatusPill(text: String, color: Color = GreenSuccess) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
