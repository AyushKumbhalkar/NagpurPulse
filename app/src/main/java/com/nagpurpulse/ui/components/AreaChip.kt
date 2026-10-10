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

fun badgeColor(badgeType: String): Color = when (badgeType) {
    "food_expert"     -> Color(0xFF7C2D12)
    "night_owl"       -> PurpleNight.copy(alpha = 0.7f)
    "top_contributor" -> OrangeDim
    "area_local"      -> TealNeighborhood.copy(alpha = 0.4f)
    "trend_spotter"   -> Color(0xFF065F46)
    else              -> SurfaceThree
}

