package com.nagpurpulse.ui.screens.thread

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.ui.components.pressScale


// ── Composable: Horizontal action card (Photo/Video, Add Location) ────────────
@Composable
 fun ActionCardHorizontal(
    modifier : Modifier = Modifier,
    icon     : ImageVector,
    iconTint : Color,
    title    : String,
    subtitle : String,
    active   : Boolean,
    onClick  : () -> Unit
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) OrangeSubtleC else DarkSurface)
            .border(1.dp, if (active) OrangeMain.copy(0.6f) else BorderGray, RoundedCornerShape(16.dp))
            .pressScale(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title,    color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = TextGray,  fontSize = 10.sp)
        }
    }
}


// ── Composable: Anonymous card with real toggle switch ────────────────────────
@Composable
 fun AnonymousCard(
    modifier    : Modifier = Modifier,
    isAnonymous : Boolean,
    onToggle    : () -> Unit
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isAnonymous) OrangeSubtleC else DarkSurface)
            .border(1.dp, if (isAnonymous) OrangeMain.copy(0.6f) else BorderGray, RoundedCornerShape(16.dp))
            .pressScale(onClick = onToggle)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Anonymous",    color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Post privately", color = TextGray,  fontSize = 10.sp)
        }
        // Real toggle switch
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isAnonymous) OrangeMain else Color(0xFF3A3A3C))
        ) {
            val thumbOffset by animateDpAsState(
                targetValue = if (isAnonymous) 18.dp else 2.dp,
                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                label = "toggleThumb"
            )
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(16.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .background(TextWhite)
            )
        }
    }
}


// ── Composable: Bottom toolbar circle item ────────────────────────────────────
@Composable
fun BottomToolItem(
    icon    : ImageVector,
    tint    : Color = TextGray,
    label   : String,
    onClick : () -> Unit
) {
    Column(
        modifier = Modifier
            .pressScale(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = TextGray, fontSize = 10.sp)
    }
}