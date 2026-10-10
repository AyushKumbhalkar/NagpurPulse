//java/com/nagpurpulse/ui/components/AlertCard.kt
package com.nagpurpulse.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nagpurpulse.data.model.AlertStatus
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.ageMinutes
import com.nagpurpulse.data.model.alertStatus
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.theme.*

fun alertAccentColor(category: String): Color = when (category.lowercase()) {
    "traffic"   -> RedAlert
    "alerts"    -> OrangePrimary
    "weather"   -> Color(0xFF5AC8FA)
    "power"     -> Color(0xFFFFD60A)
    "water"     -> Color(0xFF64D2FF)
    "safety"    -> Color(0xFFBF5AF2)
    "police"    -> Color(0xFF8E8E93)
    "emergency" -> RedAlert
    "events"    -> PinkEvents
    "rants"     -> Color(0xFFFF6961)
    else        -> OrangePrimary
}

fun alertLabel(category: String): String = when (category.lowercase()) {
    "traffic"   -> "TRAFFIC"
    "alerts"    -> "ALERT"
    "weather"   -> "WEATHER"
    "power"     -> "POWER CUT"
    "water"     -> "WATER"
    "safety"    -> "SAFETY"
    "police"    -> "POLICE"
    "emergency" -> "EMERGENCY"
    "events"    -> "EVENT"
    else        -> category.uppercase()
}

fun alertEmoji(category: String): String = when (category.lowercase()) {
    "traffic"   -> "🚗"
    "alerts"    -> "⚠️"
    "weather"   -> "🌧"
    "power"     -> "⚡"
    "water"     -> "💧"
    "safety"    -> "🛡️"
    "police"    -> "🚔"
    "emergency" -> "🆘"
    "events"    -> "📢"
    else        -> "⚠️"
}

/** Colour of the left edge: severity wins, category is the fallback for legacy alerts. */
fun alertSeverityColor(severity: String?, fallback: Color): Color = when (severity) {
    "critical" -> RedAlert
    "high"     -> OrangePrimary
    "medium"   -> YellowWarn
    "low"      -> BlueInfo
    else       -> fallback
}

private fun alertSeverityBadge(severity: String?): String? = when (severity) {
    "critical" -> "CRITICAL"
    "high"     -> "URGENT"
    else       -> null
}

fun shareAlert(context: Context, post: Post) {
    val where = post.areaTag?.takeIf { it.isNotBlank() && it != "Nagpur" }?.let { " in $it" }.orEmpty()
    val text = buildString {
        append(alertEmoji(post.category)).append(' ')
        append(alertLabel(post.category).lowercase().replaceFirstChar { it.uppercase() })
        append(" alert").append(where).append(": ").append(post.title)
        append("\n\nLive neighbourhood alerts on Nagpur Pulse 🍊")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Share alert"))
    } catch (_: Exception) {
        // No share targets available; nothing useful to do.
    }
}

@Composable
fun AlertCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: AlertStatus = post.alertStatus(),
    isConfirmed: Boolean = false,
    isOwner: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    onResolve: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showShareSheet by remember { mutableStateOf(false) }
    val isActive = status == AlertStatus.ACTIVE
    val accent = alertAccentColor(post.category)
    val edgeColor = alertSeverityColor(post.alertSeverity, accent)
    val severityBadge = if (isActive) alertSeverityBadge(post.alertSeverity) else null
    val isLive = isActive && post.ageMinutes() < 120

    val pulse = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "live_alpha"
    )

    if (showShareSheet) {
        PostShareSheet(post = post, onDismiss = { showShareSheet = false })
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, edgeColor.copy(alpha = if (isActive) 0.22f else 0.08f), RoundedCornerShape(18.dp))
            .pressScale(onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

            // Severity edge
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(edgeColor.copy(alpha = if (isActive) 1f else 0.35f), edgeColor.copy(alpha = 0.15f))
                        )
                    )
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
            ) {

                // ── Header: type · severity · live/status · time ─────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alertEmoji(post.category), fontSize = 12.sp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            alertLabel(post.category),
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                    }

                    if (severityBadge != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            severityBadge,
                            color = edgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(edgeColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    when {
                        isLive -> {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(RedAlert.copy(alpha = pulseAlpha))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("LIVE", color = RedAlert, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        status == AlertStatus.RESOLVED -> {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.CheckCircle, contentDescription = null,
                                tint = GreenSuccess, modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("RESOLVED", color = GreenSuccess, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        status == AlertStatus.EXPIRED -> {
                            Spacer(Modifier.width(8.dp))
                            Text("ENDED", color = TextTertiary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(Modifier.weight(1f))
                    Text(post.timeAgo(), color = TextTertiary, fontSize = 12.sp)
                }

                Spacer(Modifier.height(10.dp))

                // ── Title + optional thumbnail ───────────────────────────────
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            post.title,
                            color = if (isActive) PrimaryText else SecondaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!post.body.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                post.body,
                                color = SecondaryText,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!post.imageUrl.isNullOrBlank()) {
                        Spacer(Modifier.width(10.dp))
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "Alert photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Meta: place · author · views · comments ──────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!post.areaTag.isNullOrBlank()) {
                        Icon(
                            Icons.Filled.LocationOn, contentDescription = null,
                            tint = accent, modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            post.areaTag,
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text("  ·  ", color = TextTertiary, fontSize = 12.sp)
                    }
                    Text(
                        if (post.isAnonymous) "Anonymous" else "u/${post.username ?: "neighbour"}",
                        color = TextTertiary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.weight(1f))
                    Text("👁 ${post.viewCount}", color = TextTertiary, fontSize = 12.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("💬 ${post.commentCount}", color = TextTertiary, fontSize = 12.sp)
                }

                // ── Social proof ─────────────────────────────────────────────
                if (post.confirmCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (post.confirmCount == 1) "✓ 1 neighbour confirmed this"
                        else "✓ ${post.confirmCount} neighbours confirmed this",
                        color = GreenSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Actions ──────────────────────────────────────────────────
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isActive && isOwner && onResolve != null) {
                        AlertActionPill(
                            icon = Icons.Filled.CheckCircle,
                            label = "Mark resolved",
                            tint = GreenSuccess,
                            filled = false,
                            onClick = onResolve
                        )
                    } else if (isActive && !isOwner && onConfirm != null) {
                        AlertActionPill(
                            icon = Icons.Filled.CheckCircle,
                            label = if (isConfirmed) "Confirmed" else "Confirm",
                            tint = GreenSuccess,
                            filled = isConfirmed,
                            onClick = onConfirm
                        )
                    }
                    AlertActionPill(
                        icon = Icons.Filled.Share,
                        label = "Share",
                        tint = SecondaryText,
                        filled = false,
                        onClick = { showShareSheet = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    filled: Boolean,
    onClick: () -> Unit
) {
    // A small pop when an action flips to its "done" state.
    val scale by animateFloatAsState(
        targetValue = if (filled) 1f else 0.98f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "pill_scale"
    )
    Row(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(if (filled) tint.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, tint.copy(alpha = if (filled) 0.6f else 0.35f), RoundedCornerShape(50))
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
