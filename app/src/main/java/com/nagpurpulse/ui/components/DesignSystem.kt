package com.nagpurpulse.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
//  SPACING SYSTEM
// ══════════════════════════════════════════════════════════════════════════════
object Spacing {
    val xxs  = 2.dp
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 20.dp
    val xxl  = 24.dp
    val xxxl = 32.dp
    val huge = 48.dp
}

// ══════════════════════════════════════════════════════════════════════════════


// ══════════════════════════════════════════════════════════════════════════════
//  HAPTIC FEEDBACK HELPER
//  Call this on interactive events
// ══════════════════════════════════════════════════════════════════════════════
object HapticHelper {
    // Actual haptic calls are done via LocalHapticFeedback at call sites.
    // This object centralises the API surface so it's easy to find & replace.
    const val UPVOTE     = "upvote"
    const val LIKE       = "like"
    const val POST       = "post_create"
    const val ALERT      = "alert"
    const val INTERACTION = "interaction"
}

// ══════════════════════════════════════════════════════════════════════════════
//  PREMIUM EMPTY STATES  (beautiful, not boring)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(100); visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(500)) + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Glowing emoji circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(OrangePrimary.copy(0.12f), OrangePrimary.copy(0.04f), Color.Transparent)
                        )
                    )
                    .border(1.dp, OrangePrimary.copy(0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(title, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)

            if (ctaLabel != null && onCta != null) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(OrangeSubtle)
                        .border(1.dp, OrangePrimary.copy(0.5f), RoundedCornerShape(24.dp))
                        .pressScale(onClick = onCta)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(ctaLabel, color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  ERROR STATE  (modern retry UI)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ErrorState(
    message: String = "Something went wrong",
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(RedSubtle)
                .border(1.dp, RedAlert.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Warning, null, tint = RedAlert, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.error_generic_title), color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(message, color = SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceAlt)
                .border(1.dp, Divider, RoundedCornerShape(24.dp))
                .pressScale(onClick = onRetry)
                .padding(horizontal = 24.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Refresh, null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.error_try_again), color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  GLOWING FAB  (animated post button)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun GlowingFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "fab_glow")
    val glow by t.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fab_g"
    )

    var clicked by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        if (clicked) 0.85f else 1f,
        spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
        label = "fab_click",
        finishedListener = { clicked = false }
    )

    Box(modifier = modifier.scale(fabScale)) {
        // Glow ring
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(OrangePrimary.copy(glow * 0.4f), Color.Transparent)
                    )
                )
        )
        // FAB
        Box(
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(OrangeLight, OrangePrimary))
                )
                .pressScale {
                    clicked = true
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, "Post", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  LIVE PULSE INDICATOR  (for alerts, realtime events)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun LivePulse(
    color: Color = RedAlert,
    size: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "live")
    val alpha by t.animateFloat(1f, 0.2f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "la")
    val scale by t.animateFloat(1f, 1.5f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ls")

    Box(modifier = modifier.size(size * 2), contentAlignment = Alignment.Center) {
        // Outer ring
        Box(
            modifier = Modifier
                .size(size * 2)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha * 0.3f))
        )
        // Solid dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TRENDING BADGE  (for hot posts)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun TrendingBadge(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "trending")
    val shimmer by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Restart), label = "sh")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF7C2D12).copy(0.8f),
                        Color(0xFF9A3412).copy(0.8f),
                        Color(0xFF7C2D12).copy(0.8f)
                    )
                )
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", fontSize = 10.sp)
        Spacer(Modifier.width(3.dp))
        Text("HOT", color = OrangeLight, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  NOTIFICATION BADGE  (animated, grouped)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun NotificationBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count <= 0) return

    val t = rememberInfiniteTransition(label = "badge")
    val scale by t.animateFloat(1f, 1.2f, infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b_sc")

    Box(
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(RedAlert),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SKELETON / SHIMMER VARIANTS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ShimmerCommentCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceOne)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).shimmerEffect(RoundedCornerShape(50)))
            Spacer(Modifier.width(10.dp))
            Column {
                Box(Modifier.width(100.dp).height(11.dp).shimmerEffect())
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(60.dp).height(10.dp).shimmerEffect())
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(13.dp).shimmerEffect())
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth(0.7f).height(13.dp).shimmerEffect())
        Spacer(Modifier.height(12.dp))
        Row { Box(Modifier.width(50.dp).height(28.dp).shimmerEffect(RoundedCornerShape(14.dp))) }
    }
}

@Composable
fun ShimmerProfileHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().background(SurfaceOne).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(84.dp).shimmerEffect(RoundedCornerShape(42.dp)))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(130.dp).height(18.dp).shimmerEffect())
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(90.dp).height(13.dp).shimmerEffect())
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(Modifier.weight(1f).height(56.dp).shimmerEffect(RoundedCornerShape(14.dp)))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  GLOW MODIFIER  (adds subtle orange glow to any composable)
// ══════════════════════════════════════════════════════════════════════════════
fun Modifier.glowEffect(color: Color = OrangePrimary, radius: Dp = 8.dp): Modifier =
    this.border(1.dp, color.copy(0.25f), RoundedCornerShape(radius))

// ══════════════════════════════════════════════════════════════════════════════
//  PRIORITY CARD STYLES  (variety in feed)
// ══════════════════════════════════════════════════════════════════════════════
enum class CardStyle { NORMAL, COMPACT, FEATURED, EMERGENCY, EVENT }

fun postCardStyle(upvotes: Int, isEmergency: Boolean, isEvent: Boolean, hasImage: Boolean): CardStyle {
    return when {
        isEmergency        -> CardStyle.EMERGENCY
        isEvent            -> CardStyle.EVENT
        upvotes > 200      -> CardStyle.FEATURED
        !hasImage && upvotes < 20 -> CardStyle.COMPACT
        else               -> CardStyle.NORMAL
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  BOTTOM SHEET WRAPPER  (polished, rounded)
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = SurfaceAlt,
        tonalElevation   = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TertiaryText.copy(0.4f))
            )
        }
    ) {
        content()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SURFACE CARD  (reusable card with consistent style)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SurfaceCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceOne)
            .then(if (onClick != null) Modifier.pressScale(onClick = onClick) else Modifier)
            .padding(
    DensityManager.cardPadding.dp
),
        content = content
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  VERIFICATION BADGE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun VerifiedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(OrangePrimary),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  TYPING INDICATOR  (for comment sections)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "typing")
    val offsets = (0..2).map { i ->
        t.animateFloat(
            0f, -6f,
            infiniteRepeatable(tween(500, delayMillis = i * 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "dot_$i"
        ).value
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Someone is typing", color = TertiaryText, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        offsets.forEachIndexed { i, offsetY ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(TertiaryText)
            )
            if (i < 2) Spacer(Modifier.width(3.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  PULL-TO-REFRESH INDICATOR
//  Used alongside SwipeRefresh — this just adds a styled indicator style note.
//  (SwipeRefresh from Accompanist handles actual gesture; style it with indicator)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun PremiumRefreshIndicator(refreshing: Boolean) {
    AnimatedVisibility(
        visible = refreshing,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = { fullHeight -> -fullHeight }
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                slideOutVertically(
                    targetOffsetY = { fullHeight -> -fullHeight }
                )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceAlt)
                    .border(1.dp, OrangePrimary.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color    = OrangePrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.feed_refreshing), color = SecondaryText, fontSize = 12.sp)
            }
        }
    }
}
