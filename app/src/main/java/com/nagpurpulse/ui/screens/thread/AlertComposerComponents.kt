//java/com/nagpurpulse/ui/screens/thread/AlertComposerComponents.kt

package com.nagpurpulse.ui.screens.thread

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.theme.BlueInfo
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PinkEvents
import com.nagpurpulse.ui.theme.RedAlert
import com.nagpurpulse.ui.theme.YellowWarn
import kotlinx.coroutines.delay

// ── Alert types ───────────────────────────────────────────────────────────────

internal data class AlertTypeOption(
    val key: String,
    val label: String,
    val emoji: String,
    val color: Color
)

internal val alertTypeOptions = listOf(
    AlertTypeOption("traffic", "Traffic", "🚗", RedAlert),
    AlertTypeOption("power", "Power cut", "⚡", YellowWarn),
    AlertTypeOption("water", "Water", "💧", Color(0xFF64D2FF)),
    AlertTypeOption("weather", "Weather", "🌧", Color(0xFF5AC8FA)),
    AlertTypeOption("safety", "Safety", "🛡️", Color(0xFFBF5AF2)),
    AlertTypeOption("events", "Road / Event", "🚧", PinkEvents)
)

internal val alertTypeKeys: Set<String> = alertTypeOptions.map { it.key }.toSet()

internal data class AlertSeverityOption(val key: String, val label: String, val color: Color)

internal val alertSeverityOptions = listOf(
    AlertSeverityOption("low", "Minor", BlueInfo),
    AlertSeverityOption("medium", "Moderate", YellowWarn),
    AlertSeverityOption("high", "Serious", OrangePrimary),
    AlertSeverityOption("critical", "Critical", RedAlert)
)

/** One-tap titles so a report takes seconds, not minutes. */
private val alertTemplates: Map<String, List<String>> = mapOf(
    "traffic" to listOf("Heavy traffic jam", "Accident on the road", "Road blocked", "Signal not working", "Waterlogged road"),
    "power" to listOf("Power cut in our area", "Voltage fluctuation", "Transformer sparking", "Power restored"),
    "water" to listOf("No water supply", "Pipeline leakage", "Dirty water coming in taps", "Water tanker needed"),
    "weather" to listOf("Heavy rain right now", "Strong winds, tree fallen", "Hailstorm", "Extreme heat advisory"),
    "safety" to listOf("Suspicious activity nearby", "Street lights are off", "Stray animal danger", "Fire reported"),
    "events" to listOf("Road closed for an event", "Procession causing diversion", "Construction blocking road", "Large crowd gathering")
)

internal fun alertPrompts(area: String?): List<String> {
    val place = area?.takeIf { it.isNotBlank() && it != CITY } ?: "your area"
    return listOf(
        "What's happening in $place?",
        "Power cut near $place?",
        "Road blocked or flooded?",
        "Tell your neighbours what you see"
    )
}

@Composable
internal fun AlertTypePicker(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(
            "What's happening?",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        alertTypeOptions.chunked(3).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    AlertTypeTile(
                        option = option,
                        selected = option.key == selected,
                        onClick = { onSelect(option.key) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertTypeTile(
    option: AlertTypeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        if (selected) 1.04f else 1f,
        spring(Spring.DampingRatioMediumBouncy), label = "tile_scale"
    )
    val border by animateColorAsState(
        if (selected) option.color else MaterialTheme.colorScheme.outlineVariant,
        tween(180), label = "tile_border"
    )
    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) option.color.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface
            )
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .pressScale(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(option.emoji, fontSize = 26.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            option.label,
            color = if (selected) option.color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
internal fun AlertSeverityPicker(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text(
            "How serious is it?",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            alertSeverityOptions.forEach { option ->
                val isSelected = option.key == selected
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) option.color.copy(alpha = 0.20f) else Color.Transparent)
                        .clickable { onSelect(option.key) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(option.color))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        option.label,
                        color = if (isSelected) option.color else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun AlertTemplateChips(typeKey: String, onPick: (String) -> Unit) {
    val templates = alertTemplates[typeKey] ?: return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        templates.forEach { template ->
            Text(
                template,
                color = OrangeMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(OrangeMain.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, OrangeMain.copy(alpha = 0.35f)), RoundedCornerShape(50))
                    .pressScale { onPick(template) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
internal fun AlertLocationCard(
    area: String,
    onChange: () -> Unit,
    onUseLocation: () -> Unit
) {
    val isCity = area == CITY
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📍", fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).clickable { onChange() }) {
            Text(
                if (isCity) "Add where this is happening" else area,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                if (isCity) "Nearby neighbours see it first" else "Tap to change area",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(OrangeMain.copy(alpha = 0.12f))
                .pressScale { onUseLocation() }
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.MyLocation, null, tint = OrangeMain, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text("Use mine", color = OrangeMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Success moment ────────────────────────────────────────────────────────────

/**
 * Replaces the generic "post is live" screen for alerts. Shows impact (reach) first, then lets the
 * reporter share the alert straight to WhatsApp or anywhere else. It stays until "Done" is tapped.
 */
@Composable
internal fun AlertSuccessOverlay(
    visible: Boolean,
    typeKey: String,
    title: String,
    area: String,
    reach: Int?,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val option = alertTypeOptions.firstOrNull { it.key == typeKey }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var popped by remember { mutableStateOf(false) }
                LaunchedEffect(visible) {
                    if (visible) {
                        haptic.success()
                        delay(60)
                        popped = true
                    }
                }
                val scale by animateFloatAsState(
                    if (popped) 1f else 0.4f,
                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "success_pop"
                )
                Box(
                    Modifier
                        .size(96.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background((option?.color ?: GreenSuccess).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Text(option?.emoji ?: "✅", fontSize = 44.sp) }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Alert sent. Thank you!",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                val place = if (area == CITY) "across Nagpur" else "near $area"
                Text(
                    if (reach != null && reach >= 10)
                        "Reaching ~${if (reach >= 1000) "1,000+" else reach.toString()} neighbours $place right now."
                    else
                        "Your neighbours $place can see it right now.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "+5 karma · people can confirm your alert",
                    color = GreenSuccess,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(28.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF25D366))
                        .pressScale {
                            haptic.tap()
                            shareAlertToWhatsAppOrAny(context, typeKey, title, area)
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Share on WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                        .pressScale { onDone() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Done",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

private fun shareAlertToWhatsAppOrAny(context: Context, typeKey: String, title: String, area: String) {
    val option = alertTypeOptions.firstOrNull { it.key == typeKey }
    val where = if (area == CITY) "" else " in $area"
    val text = "${option?.emoji ?: "⚠️"} ${option?.label ?: "Alert"}$where: $title\n\nLive neighbourhood alerts on Nagpur Pulse 🍊"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent(send).setPackage("com.whatsapp"))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent.createChooser(send, "Share alert"))
        } catch (_: Exception) {
            // No share targets installed.
        }
    }
}
