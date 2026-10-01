package com.nagpurpulse.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.theme.*

fun categoryColor(category: String): Color = when (category.lowercase()) {
    "food"          -> Color(0xFFFF9500)
    "nightlife"     -> PurpleNight
    "jobs"          -> BlueJobs
    "college"       -> GreenCollege
    "rants"         -> RedAlert
    "neighborhoods" -> TealNeighborhood
    "lost_found"    -> YellowLost
    "events"        -> PinkEvents
    "traffic"       -> RedAlert
    "alerts"        -> Color(0xFFFF6B00)
    "weather"       -> Color(0xFF5AC8FA)
    "power"         -> Color(0xFFFFD60A)
    else            -> OrangePrimary
}

fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "food"          -> Icons.Filled.Restaurant
    "nightlife"     -> Icons.Filled.Nightlife
    "jobs"          -> Icons.Filled.Work
    "college"       -> Icons.Filled.School
    "rants"         -> Icons.Filled.RecordVoiceOver
    "neighborhoods" -> Icons.Filled.LocationCity
    "lost_found"    -> Icons.Filled.Search
    "events"        -> Icons.Filled.Event
    "traffic"       -> Icons.Filled.Traffic
    "alerts"        -> Icons.Filled.Campaign
    "weather"       -> Icons.Filled.Cloud
    "power"         -> Icons.Filled.Bolt
    else            -> Icons.Filled.Chat
}

fun categoryDisplayName(category: String): String = when (category.lowercase()) {
    "food"          -> "Food"
    "nightlife"     -> "Nightlife"
    "jobs"          -> "Jobs"
    "college"       -> "College Life"
    "rants"         -> "Rants"
    "neighborhoods" -> "Neighborhoods"
    "lost_found"    -> "Lost & Found"
    "events"        -> "Events"
    "traffic"       -> "Traffic"
    "alerts"        -> "Alerts"
    "weather"       -> "Weather"
    "power"         -> "Power"
    else            -> category.replaceFirstChar { it.uppercase() }
}

@Composable
fun CategoryBadge(category: String, modifier: Modifier = Modifier) {
    val color   = categoryColor(category)
    val icon = categoryIcon(category)
    val name    = categoryDisplayName(category)

    Row(
        modifier = modifier
            .widthIn(max = 144.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text       = name,
            color      = color,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
