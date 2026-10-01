// java/com/nagpurpulse/ui/components/FeedCardVariants.kt

package com.nagpurpulse.ui.components

import com.nagpurpulse.ui.preferences.DensityManager
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════
//  COMPACT POST CARD  (text-only, minimal chrome, dense)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun CompactPostCard(
    post: Post,
    onClick: () -> Unit,
    onUpvote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var upvoted by remember { mutableStateOf(false) }
    var count   by remember { mutableIntStateOf(post.upvotes) }
    var burst   by remember { mutableStateOf(false) }
    val upScale by animateFloatAsState(
        if (burst) 1.5f else 1f,
        spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
        label = "compact_up",
        finishedListener = { burst = false }
    )

    val catColor = categoryColor(post.category)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(catColor)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            SmartTitle(post.title, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(post.category, modifier = Modifier.height(20.dp))
                Text("  ·  ", color = TertiaryText, fontSize = 11.sp)
                Text(post.timeAgo(), color = TertiaryText, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.width(10.dp))

        // Compact vote
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.KeyboardArrowUp, null,
                tint = if (upvoted) OrangePrimary else TertiaryText,
                modifier = Modifier.size(20.dp).scale(upScale).pressScale {
                    upvoted = !upvoted; count = if (upvoted) post.upvotes + 1 else post.upvotes; burst = true; onUpvote()
                }
            )
            Text(formatCount(count), color = if (upvoted) OrangePrimary else TertiaryText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  FEATURED POST CARD  (high-upvote posts, more visual weight)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun FeaturedPostCard(
    post: Post,
    onClick: () -> Unit,
    onUpvote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val catColor = categoryColor(post.category)
    var upvoted  by remember { mutableStateOf(false) }
    var count    by remember { mutableIntStateOf(post.upvotes) }
    var burst    by remember { mutableStateOf(false) }
    val upScale  by animateFloatAsState(if (burst) 1.4f else 1f, spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh), label = "feat_up", finishedListener = { burst = false })

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        catColor.copy(0.07f),
                        Surface,
                        Surface
                    )
                )
            )
            .border(1.dp, catColor.copy(0.20f), RoundedCornerShape(20.dp))
            .pressScale(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(
    DensityManager.cardPadding.dp
)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TrendingBadge()
                Spacer(Modifier.width(8.dp))
                CategoryBadge(post.category)
                Spacer(Modifier.weight(1f))
                Text(post.timeAgo(), color = TertiaryText, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            // Large title for featured
            SmartTitle(
                post.title,
                color = PrimaryText,
                modifier = Modifier.fillMaxWidth()
            )

            if (!post.body.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(post.body, color = SecondaryText, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(post.imageUrl).crossfade(true).build(),
                    loading = { Box(Modifier.fillMaxWidth().height(180.dp).shimmerEffect(RoundedCornerShape(14.dp))) },
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Divider, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (post.isAnonymous) "u/Anonymous" else "u/${post.username ?: "unknown"}", color = OrangePrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (post.areaTag != null) {
                    Text("  ·  ", color = TertiaryText, fontSize = 12.sp)
                    Icon(Icons.Filled.LocationOn, null, tint = TertiaryText, modifier = Modifier.size(11.dp))
                    Text(" ${post.areaTag}", color = TertiaryText, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                // Upvote pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (upvoted) OrangeSubtle else SurfaceAlt)
                        .pressScale { upvoted = !upvoted; count = if (upvoted) post.upvotes + 1 else post.upvotes; burst = true; onUpvote() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp, null, tint = if (upvoted) OrangePrimary else SecondaryText, modifier = Modifier.size(16.dp).scale(upScale))
                    Spacer(Modifier.width(4.dp))
                    Text(formatCount(count), color = if (upvoted) OrangePrimary else SecondaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceAlt).padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.MailOutline, null, tint = SecondaryText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatCount(post.commentCount), color = SecondaryText, fontSize = 12.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  EMERGENCY CARD  (urgent / critical alert posts)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun EmergencyPostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "emg")
    val borderAlpha by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "em_border")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
    if (LocalIsDarkTheme.current)
        Color(0xFF1A0505)
    else
        RedAlert.copy(alpha = 0.06f)
)
            .border(1.5.dp, RedAlert.copy(borderAlpha), RoundedCornerShape(18.dp))
            .pressScale(onClick = onClick)
    ) {
        // Top accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(RedAlert, OrangePrimary, RedAlert)))
                .align(Alignment.TopCenter)
        )

        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulse(RedAlert, 8.dp)
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(RedAlert.copy(0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🆘", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("EMERGENCY", color = RedAlert, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(post.timeAgo(), color = TertiaryText, fontSize = 11.sp)
            }

            Spacer(Modifier.height(10.dp))
            Text(
                post.title,
                color = if (LocalIsDarkTheme.current)
                    Color(0xFFFFE5E5)
                else
                    PrimaryText
            , fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)

            if (!post.body.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(post.body, color = SecondaryText, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (post.areaTag != null) {
                    Icon(Icons.Filled.LocationOn, null, tint = RedAlert.copy(0.8f), modifier = Modifier.size(12.dp))
                    Text(" ${post.areaTag}", color = RedAlert.copy(0.8f), fontSize = 12.sp)
                    Text("  ·  ", color = TertiaryText, fontSize = 12.sp)
                }
                Text(post.timeAgo(), color = TertiaryText, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                if (post.viewCount > 0) Text("${formatCount(post.viewCount)} views", color = TertiaryText, fontSize = 11.sp)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  EVENT CARD  (polished card for local events)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun EventPostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (LocalIsDarkTheme.current)
                    Color(0xFF050A14)
                else
                    BlueInfo.copy(alpha = 0.04f)
            )
            .border(1.dp, PinkEvents.copy(0.25f), RoundedCornerShape(18.dp))
            .pressScale(onClick = onClick)
    ) {
        // Top gradient stripe
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.horizontalGradient(listOf(PinkEvents, BlueInfo, PinkEvents)))
                .align(Alignment.TopCenter)
        )

        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            // Event date block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PinkEvents.copy(0.12f))
                    .border(1.dp, PinkEvents.copy(0.25f), RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                Text("EVENT", color = PinkEvents, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(2.dp))
                Text("📅", fontSize = 18.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                SmartTitle(post.title, color = PrimaryText)
                if (!post.body.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(post.body, color = SecondaryText, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (post.isAnonymous) "u/Anonymous" else "u/${post.username ?: "unknown"}", color = OrangePrimary, fontSize = 12.sp)
                    if (post.areaTag != null) {
                        Text("  ·  ", color = TertiaryText, fontSize = 12.sp)
                        Icon(Icons.Filled.LocationOn, null, tint = TertiaryText, modifier = Modifier.size(11.dp))
                        Text(" ${post.areaTag}", color = TertiaryText, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(post.timeAgo(), color = TertiaryText, fontSize = 11.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SMART FEED CARD ROUTER
//  Choose the right card variant based on post content/type
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SmartPostCard(
    post: Post,
    onClick: () -> Unit,
    onUpvote: () -> Unit = {},
    onDownvote: () -> Unit = {},
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    isOwnPost: Boolean = false,
    onDelete: () -> Unit = {}
) {
    val isEmergency = post.category.lowercase() in listOf("emergency", "police")
    val isEvent     = post.category.lowercase() == "events"
    val isFeatured  = post.upvotes > 200 && !isEmergency && !isEvent
    val isCompact   = post.body.isNullOrBlank() && post.imageUrl.isNullOrBlank() && post.upvotes < 20

    when {
        isEmergency -> EmergencyPostCard(post = post, onClick = onClick)
        isEvent     -> EventPostCard(post = post, onClick = onClick)
        isFeatured  -> FeaturedPostCard(post = post, onClick = onClick, onUpvote = onUpvote)
        isCompact   -> CompactPostCard(post = post, onClick = onClick, onUpvote = onUpvote)
        else        -> PostCard(
            post         = post,
            onClick      = onClick,
            onUserClick  = onUserClick,
            onUpvote     = onUpvote,
            onDownvote   = onDownvote,
            isSaved      = isSaved,
            onToggleSave = onToggleSave,
            isOwnPost    = isOwnPost,
            onDelete     = onDelete
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SMART TITLE (override in PostCard — shorter = bigger)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun SmartTitle(
    text: String,
    color: Color = TextPrimary,
    maxLines: Int = 2,
    modifier: Modifier = Modifier
) {
    val fontSize = when {
        text.length < 25 -> 18.sp
        text.length < 45 -> 16.sp
        text.length < 65 -> 15.sp
        else             -> 14.sp
    }
    Text(
        text = text, color = color, fontWeight = FontWeight.Bold,
        fontSize = fontSize, lineHeight = (fontSize.value * 1.35f).sp,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
