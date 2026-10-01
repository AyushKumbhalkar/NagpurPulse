//java/com/nagpurpulse/ui/components/AlertCard.kt
package com.nagpurpulse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.theme.*

fun alertAccentColor(category: String): Color = when (category.lowercase()) {
    "traffic"   -> RedAlert
    "alerts"    -> OrangePrimary
    "weather"   -> Color(0xFF5AC8FA)
    "power"     -> Color(0xFFFFD60A)
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
    "power"     -> "POWER"
    "police"    -> "POLICE"
    "emergency" -> "EMERGENCY"
    "events"    -> "EVENTS"
    else        -> category.uppercase()
}

fun alertEmoji(category: String): String = when (category.lowercase()) {
    "traffic"   -> "🚗"
    "alerts"    -> "⚠️"
    "weather"   -> "🌤"
    "power"     -> "⚡"
    "police"    -> "🚔"
    "emergency" -> "🆘"
    "events"    -> "📢"
    else        -> "⚠️"
}

@Composable
fun AlertCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = alertAccentColor(post.category)

    val infiniteTransition = rememberInfiniteTransition(label = "alert_pulse")
    val liveDotAlpha by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "live_dot"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .pressScale(onClick = onClick)
    ) {
        // Top accent gradient strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(listOf(accent, accent.copy(0f)))
                )
                .align(Alignment.TopCenter)
        )

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)) {

            // Badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(accent.copy(0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(alertEmoji(post.category), fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        alertLabel(post.category),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Live dot for time-critical alerts
                if (post.category in listOf("traffic", "emergency", "alerts")) {
                    Spacer(Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(RedAlert.copy(alpha = liveDotAlpha))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("LIVE", color = RedAlert, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(post.timeAgo(), color = TertiaryText, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))

            // Title + optional image
            if (!post.imageUrl.isNullOrBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(post.title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!post.body.isNullOrBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(post.body, color = SecondaryText, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Text(post.title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!post.body.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(post.body, color = SecondaryText, fontSize = 13.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Footer row - username + location on left, views + comments + share on right
            Column {
                // Username row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (post.isAnonymous) "u/Anonymous" else "u/${post.username ?: "unknown"}",
                        color = OrangePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Location + views/comments + share row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.areaTag != null) {
                        Icon(Icons.Filled.LocationOn, null, tint = TertiaryText, modifier = Modifier.size(12.dp))
                        Text(" ${post.areaTag}", color = TertiaryText, fontSize = 11.sp)
                    }
                    if (post.viewCount > 0) {
                        if (post.areaTag != null) Text("  ·  ", color = TertiaryText, fontSize = 11.sp)
                        Text("👁 ${formatCount(post.viewCount)}", color = TertiaryText, fontSize = 11.sp)
                    }
                    if (post.commentCount > 0) {
                        Text("  ·  ", color = TertiaryText, fontSize = 11.sp)
                        Text("💬 ${post.commentCount}", color = TertiaryText, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    // Share button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceAlt)
                            .border(1.dp, Divider, RoundedCornerShape(16.dp))
                            .pressScale(onClick = {})
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Share, null, tint = SecondaryText, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
