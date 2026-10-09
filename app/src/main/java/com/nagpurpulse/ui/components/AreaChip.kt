package com.nagpurpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.theme.*

@Composable
fun AreaChip(
    area: String,
    isSelected: Boolean = false,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) OrangeSubtle else SurfaceAlt)
            .border(
                1.dp,
                if (isSelected) OrangePrimary.copy(0.6f) else Divider,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.LocationOn, null,
            tint     = if (isSelected) OrangePrimary else SecondaryText,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            area,
            color      = if (isSelected) OrangePrimary else PrimaryText,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (onRemove != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Filled.Close, "Remove",
                tint     = OrangePrimary,
                modifier = Modifier.size(13.dp).clickable(onClick = onRemove)
            )
        }
    }
}

fun badgeColor(badgeType: String): Color = when (badgeType) {
    "food_expert"     -> Color(0xFF7C2D12)
    "night_owl"       -> PurpleNight.copy(alpha = 0.7f)
    "top_contributor" -> OrangeDim
    "area_local"      -> TealNeighborhood.copy(alpha = 0.4f)
    "trend_spotter"   -> Color(0xFF065F46)
    else              -> SurfaceThree
}

@Composable
fun BadgeChip(
    emoji: String,
    label: String,
    badgeType: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(badgeColor(badgeType))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
