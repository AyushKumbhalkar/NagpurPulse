// HomeComponents.kt
// java/com/nagpurpulse/ui/screens/home/HomeComponents.kt
//
// Reusable building blocks for the Home screen. HomeScreen.kt keeps the
// ViewModel + screen wiring; everything visual that is not a PostCard lives here.

package com.nagpurpulse.ui.screens.home

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.Festivals
import com.nagpurpulse.ui.components.UserAvatar
import com.nagpurpulse.ui.components.categoryColor
import com.nagpurpulse.ui.components.categoryDisplayName
import com.nagpurpulse.ui.components.categoryIcon
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.RedAlert
import java.util.Calendar

// ═════════════════════════════════════════════════════════════════════════════
//  Brand
// ═════════════════════════════════════════════════════════════════════════════

/** "Nagpur Pulse" wordmark with a tiny live "heartbeat" equaliser next to it. */
@Composable
internal fun PulseBrand(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "brand_pulse")
    val h1 by transition.animateFloat(
        0.35f, 1f,
        infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val h2 by transition.animateFloat(
        1f, 0.35f,
        infiniteRepeatable(tween(780, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val h3 by transition.animateFloat(
        0.5f, 0.95f,
        infiniteRepeatable(tween(540, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                ) { append("Nagpur ") }
                withStyle(
                    SpanStyle(
                        color = OrangePrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                ) { append("Pulse") }
            }
        )
        Spacer(Modifier.width(6.dp))
        Row(
            modifier = Modifier.height(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(h1, h2, h3).forEach { h ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((16 * h).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OrangePrimary)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Header buttons + badges
// ═════════════════════════════════════════════════════════════════════════════

/** Unread-count badge: 16dp minimum, 10sp text, "9+" cap. Renders nothing for 0. */
@Composable
internal fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val label = if (count > 9) "9+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(OrangePrimary)
            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Circular header button (44dp by default). The badge sits outside the clipped circle so it never gets cut. */
@Composable
internal fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    iconModifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    val description =
        if (badgeCount > 0) "$contentDescription, $badgeCount unread" else contentDescription

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics { this.contentDescription = description }
                .pressScale(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(22.dp)
                    .then(iconModifier)
            )
        }
        CountBadge(
            count = badgeCount,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 3.dp, y = (-3).dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Greeting + chips
// ═════════════════════════════════════════════════════════════════════════════

internal fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
}

internal data class AqiBand(val label: String, val color: Color)

/**
 * The weather repository returns raw PM2.5 (µg/m³), not an AQI. This converts it to an
 * *estimated* Indian CPCB AQI using the standard PM2.5 breakpoints, so the number and the
 * colour bands mean what people expect. (OpenWeather gives an instantaneous reading, not a
 * 24h average, so treat the result as an estimate.)
 */
internal fun estimateAqiFromPm25(pm25: Int): Int {
    val c = pm25.coerceAtLeast(0).toDouble()
    // (concLow, concHigh, aqiLow, aqiHigh)
    val bands = listOf(
        doubleArrayOf(0.0, 30.0, 0.0, 50.0),
        doubleArrayOf(31.0, 60.0, 51.0, 100.0),
        doubleArrayOf(61.0, 90.0, 101.0, 200.0),
        doubleArrayOf(91.0, 120.0, 201.0, 300.0),
        doubleArrayOf(121.0, 250.0, 301.0, 400.0),
        doubleArrayOf(251.0, 500.0, 401.0, 500.0)
    )
    val band = bands.firstOrNull { c <= it[1] } ?: bands.last()
    val aqi = (band[3] - band[2]) / (band[1] - band[0]) * (c - band[0]) + band[2]
    return aqi.toInt().coerceIn(0, 500)
}

internal fun aqiBand(aqi: Int): AqiBand = when {
    aqi <= 50 -> AqiBand("Good", Color(0xFF2EB85C))
    aqi <= 100 -> AqiBand("Satisfactory", Color(0xFF8BC34A))
    aqi <= 200 -> AqiBand("Moderate", Color(0xFFFFB300))
    aqi <= 300 -> AqiBand("Poor", Color(0xFFFF7043))
    aqi <= 400 -> AqiBand("Very poor", Color(0xFFE53935))
    else -> AqiBand("Severe", Color(0xFFB71C1C))
}

/** Lightweight identity ladder derived from karma. Thresholds are a starting point – tune freely. */
internal fun pulseLevel(karma: Int): String = when {
    karma >= 500 -> "Legend"
    karma >= 150 -> "Nagpurkar"
    karma >= 30 -> "Local"
    else -> "Newcomer"
}

@Composable
internal fun InfoChip(
    icon: ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .heightIn(min = 32.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ShimmerChip(width: androidx.compose.ui.unit.Dp = 64.dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(32.dp)
            .shimmerEffect(RoundedCornerShape(20.dp))
    )
}

/**
 * Time-aware greeting + location / weather / AQI / streak / level chips.
 * Lives inside the feed so it scrolls away and gives posts the full screen.
 */
@Composable
internal fun GreetingBlock(
    userName: String?,
    area: String,
    temperature: Int?,
    pm25: Int?,
    weatherAttempted: Boolean,
    streak: Int,
    karma: Int?,
    onLocationClick: () -> Unit,
    onLevelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val isDaytime = hour in 6..18
    val greeting = greetingForHour(hour)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (userName.isNullOrBlank()) greeting else "$greeting, $userName",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // A little local warmth on festival days.
        val festival = remember { Festivals.today() }
        if (festival != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${festival.emoji} ${stringResource(festival.messageRes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = OrangePrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoChip(
                icon = Icons.Filled.LocationOn,
                text = area,
                color = OrangePrimary,
                onClick = onLocationClick
            )

            when {
                temperature != null -> InfoChip(
                    icon = if (isDaytime) Icons.Filled.WbSunny else Icons.Filled.DarkMode,
                    text = "$temperature°",
                    color = if (isDaytime) Color(0xFFFFB300) else Color(0xFF5C9EFF)
                )
                !weatherAttempted -> ShimmerChip()
                else -> Unit // weather unavailable (e.g. no location permission): show nothing
            }

            when {
                pm25 != null -> {
                    val aqi = estimateAqiFromPm25(pm25)
                    val band = aqiBand(aqi)
                    InfoChip(
                        icon = Icons.Filled.Eco,
                        text = "AQI $aqi · ${band.label}",
                        color = band.color
                    )
                }
                !weatherAttempted -> ShimmerChip(width = 104.dp)
                else -> Unit
            }

            if (streak >= 2) {
                InfoChip(
                    icon = Icons.Filled.LocalFireDepartment,
                    text = "$streak-day streak",
                    color = Color(0xFFFF7043)
                )
            }

            if (karma != null) {
                InfoChip(
                    icon = Icons.Filled.Bolt,
                    text = "${pulseLevel(karma)} · $karma",
                    color = Color(0xFF9C8CFF),
                    onClick = onLevelClick
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Composer ("What's happening in …?")
// ═════════════════════════════════════════════════════════════════════════════

internal fun shortAreaLabel(area: String): String =
    area.substringBefore(',').trim().takeUnless { it.isBlank() || it == "Near You" } ?: "Nagpur"

@Composable
internal fun ComposerBar(
    userName: String?,
    avatarUrl: String?,
    area: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), shape)
            .semantics { contentDescription = "Create a post" }
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(name = userName, imageUrl = avatarUrl, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "What's happening in ${shortAreaLabel(area)}?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(OrangePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  "Nagpur right now" strip
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "live_dot")
    val alpha by transition.animateFloat(
        1f, 0.25f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "live_alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(GreenSuccess.copy(alpha = alpha))
    )
}

@Composable
private fun StripCard(
    icon: ImageVector,
    headline: String,
    sub: String,
    accent: Color,
    width: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .width(width)
            .height(92.dp)
            .clip(shape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .then(if (onClick != null) Modifier.pressScale(onClick = onClick) else Modifier)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = headline,
                color = accent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = sub,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Horizontal strip of live local signals. It only shows cards that have real data, and
 * disappears entirely when there is nothing to show – no empty placeholders.
 */
@Composable
internal fun RightNowStrip(
    onlineCount: Int,
    alertPost: Post?,
    trendingPost: Post?,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // A lone "1 online" (just you) is anti-social-proof, so wait for at least 2.
    val showOnline = onlineCount >= 2
    if (!showOnline && alertPost == null && trendingPost == null) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveDot()
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Nagpur right now",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showOnline) {
                StripCard(
                    icon = Icons.Filled.Groups,
                    headline = "$onlineCount online",
                    sub = "Nagpurians are active now",
                    accent = GreenSuccess,
                    width = 150.dp
                )
            }
            if (alertPost != null) {
                StripCard(
                    icon = Icons.Filled.Campaign,
                    headline = "Alert",
                    sub = alertPost.title,
                    accent = RedAlert,
                    width = 230.dp,
                    onClick = { onPostClick(alertPost.id) }
                )
            }
            if (trendingPost != null) {
                StripCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    headline = "Trending",
                    sub = trendingPost.title,
                    accent = OrangePrimary,
                    width = 230.dp,
                    onClick = { onPostClick(trendingPost.id) }
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Category chips + sort tabs
// ═════════════════════════════════════════════════════════════════════════════

private val FeedCategoryKeys = listOf(
    "food", "events", "traffic", "alerts", "jobs",
    "college", "nightlife", "neighborhoods", "lost_found", "rants"
)

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val haptic = rememberHaptic()
    Row(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(shape)
            .background(
                if (selected) color.copy(alpha = 0.20f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape
            )
            .clickable {
                haptic.tap()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

/** "All" + the app's categories. [selected] == null means All. */
@Composable
internal fun CategoryChipsRow(
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            CategoryChip(
                label = "All",
                icon = Icons.Filled.Apps,
                color = OrangePrimary,
                selected = selected == null,
                onClick = { onSelected(null) }
            )
        }
        items(items = FeedCategoryKeys, key = { it }) { key ->
            CategoryChip(
                label = categoryDisplayName(key),
                icon = categoryIcon(key),
                color = categoryColor(key),
                selected = selected.equals(key, ignoreCase = true),
                onClick = { onSelected(key) }
            )
        }
    }
}

/** Full-width Top / New / Hot control with an animated sliding highlight. */
@Composable
internal fun SortTabs(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("top" to "Top", "new" to "New", "hot" to "🔥 Hot")
    val index = tabs.indexOfFirst { it.first.equals(selected.trim(), ignoreCase = true) }
        .coerceAtLeast(0)
    val haptic = rememberHaptic()
    val outer = RoundedCornerShape(16.dp)
    val inner = RoundedCornerShape(12.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(outer)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), outer)
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorX by animateDpAsState(
            targetValue = tabWidth * index,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "sort_indicator"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(inner)
                .background(OrangePrimary.copy(alpha = 0.20f))
                .border(1.dp, OrangePrimary.copy(alpha = 0.60f), inner)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { i, (key, label) ->
                val isSelected = i == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(inner)
                        .clickable {
                            if (!isSelected) haptic.tap()
                            onSelected(key)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) OrangePrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Floating pill + permission card
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun NewPostsPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .shadow(8.dp, shape)
            .clip(shape)
            .background(OrangePrimary)
            .pressScale(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (count == 1) "1 new post" else "$count new posts",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Friendly pre-permission explainer, shown before the system location dialog. */
@Composable
internal fun LocationRationaleCard(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(OrangePrimary.copy(alpha = 0.10f))
            .border(1.dp, OrangePrimary.copy(alpha = 0.30f), shape)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.NearMe,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "See weather & posts near you",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Approximate location is enough. We only use it to show your area and local weather.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(4.dp))
            Button(
                onClick = onAllow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Allow", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Feed states
// ═════════════════════════════════════════════════════════════════════════════

@Composable
internal fun FeedEmptyState(
    category: String?,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🍊", fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (category == null) "Nothing here yet"
            else "No ${categoryDisplayName(category)} posts yet",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Be the first to post about Nagpur!",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onCreatePost,
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangePrimary,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Create post", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun FeedErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Can't reach Nagpur Pulse",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Check your connection and try again.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = OrangePrimary,
                contentColor = Color.White
            )
        ) {
            Text("Try again", fontWeight = FontWeight.SemiBold)
        }
    }
}

/** End-of-feed closure: a deliberate stopping point instead of an endless scroll. */
@Composable
internal fun FeedEndCard(
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🍊", fontSize = 28.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "You're all caught up in Nagpur",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Check back soon, or explore what's happening elsewhere.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onExplore) {
            Text("Explore", color = OrangePrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Local streak (device-only, gentle: it never shows a "you lost your streak" message)
// ═════════════════════════════════════════════════════════════════════════════

internal object HomeStreak {
    private const val PREFS = "nagpurpulse_home"
    private const val KEY_LAST_DAY = "streak_last_epoch_day"
    private const val KEY_COUNT = "streak_count"
    private const val KEY_LOCATION_SNOOZE_UNTIL = "location_prompt_snooze_until_day"

    /** Records today's visit and returns the current consecutive-day count (>= 1). */
    fun touch(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toEpochDay()
        val last = prefs.getLong(KEY_LAST_DAY, -1L)
        var count = prefs.getInt(KEY_COUNT, 0)

        when {
            last == today -> Unit
            last == today - 1 -> count += 1
            else -> count = 1
        }

        prefs.edit()
            .putLong(KEY_LAST_DAY, today)
            .putInt(KEY_COUNT, count)
            .apply()
        return count.coerceAtLeast(1)
    }

    /** True while the "enable location" card has been dismissed with "Not now". */
    fun isLocationPromptSnoozed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return java.time.LocalDate.now().toEpochDay() < prefs.getLong(KEY_LOCATION_SNOOZE_UNTIL, 0L)
    }

    fun snoozeLocationPrompt(context: Context, days: Long = 3L) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LOCATION_SNOOZE_UNTIL, java.time.LocalDate.now().toEpochDay() + days)
            .apply()
    }
}
