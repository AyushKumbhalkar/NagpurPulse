//PostCard.kt file
// java/com/nagpurpulse/ui/components/PostCard.kt

package com.nagpurpulse.ui.components

import androidx.compose.foundation.layout.aspectRatio
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.theme.*

@Composable
fun PostCard(
    post: Post,
    currentVote: String? = null,
    onClick: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onUpvote: () -> Unit = {},
    onDownvote: () -> Unit = {},
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    modifier: Modifier = Modifier,
    isOwnPost: Boolean = false,
    isAdmin: Boolean = false,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    onReport: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val isUpvoted = currentVote == "up"
    val isDownvoted = currentVote == "down"

    fun sharePost() {
        val shareText = buildString {
            append(post.title)
            if (!post.body.isNullOrBlank()) append("\n\n").append(post.body)
            append("\n\n— Shared from Nagpur Pulse 🍊")
        }
        val i = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(i, null))
    }

    var menuExpanded by remember { mutableStateOf(false) }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var upvoteBurst by remember { mutableStateOf(false) }
    val upvoteScale by animateFloatAsState(
        targetValue = if (upvoteBurst) 1.45f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
        label = "up_scale",
        finishedListener = { upvoteBurst = false }
    )

    // Soft orange flash inside the upvote pill when a vote is added.
    var burstKey by remember { mutableIntStateOf(0) }
    val flash = remember { Animatable(1f) }
    LaunchedEffect(burstKey) {
        if (burstKey > 0) {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(420))
        }
    }

    val cardColor = MaterialTheme.colorScheme.surface
    val catColor = categoryColor(post.category)
    val isCompact = FeedLayoutManager.feedStyle == "compact"
    val hasImage = !post.imageUrl.isNullOrBlank()

    val authorName = if (post.isAnonymous) "Anonymous" else (post.username ?: "unknown")
    val canOpenProfile = !post.isAnonymous && post.userId.isNotBlank()
    val minutesOld = remember(post.createdAt) { post.minutesOld() }
    val isFresh = minutesOld != null && minutesOld in 0..29
    val isTrending = post.isTrending()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .clickable {
                onClick()
            }
    ) {
        // Subtle left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(catColor.copy(0.7f), catColor.copy(0.0f))
                    )
                )
                .align(Alignment.CenterStart)
        )

        // NOTE: horizontal padding is applied per-row below (not on this outer
        // Column) so that, in expanded mode, the post image can bleed all the
        // way to the card's edges while every other row keeps its text margin.
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) {

            // ── Row 1: avatar + author + area · time + menu ───────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = authorName,
                    imageUrl = post.authorAvatarUrl,
                    isAnonymous = post.isAnonymous,
                    size = 36.dp,
                    modifier = Modifier.clickable(enabled = canOpenProfile) {
                        onUserClick(post.userId)
                    }
                )

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = authorName,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(enabled = canOpenProfile) {
                                    onUserClick(post.userId)
                                }
                        )
                        if (post.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = OrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!post.areaTag.isNullOrBlank()) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = post.areaTag,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                            Text(
                                text = "  ·  ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = post.timeAgo(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surface
                        )
                    ) {
                        if (isOwnPost || isAdmin) {

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text("Edit")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )

                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = RedAlert
                                    )
                                },
                                text = {
                                    Text(
                                        "Delete",
                                        color = RedAlert
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Share", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { sharePost(); menuExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isSaved) "Unsave" else "Save", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { onToggleSave(); menuExpanded = false }
                        )
                        if (!isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Report", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {
                                    menuExpanded = false
                                    onReport()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Row 2: title block — shape depends on density setting ─────
            if (isCompact) {

                // COMPACT: dense single row, 2-line title, fixed square
                // thumbnail on the right so every row scans and aligns the same.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrowScreen = maxWidth < 340.dp
                    val thumbnailSize = if (isNarrowScreen) 64.dp else 76.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = post.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = if (isNarrowScreen) 3 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (hasImage) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(post.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(thumbnailSize)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                if (!post.body.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = post.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }

            } else {

                // EXPANDED: full title + body preview, padded like normal text.
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = post.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        lineHeight = 24.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!post.body.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = post.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            // Give text-only posts more room to breathe since
                            // there's no photo competing for attention.
                            maxLines = if (hasImage) 2 else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // EXPANDED image: full-bleed, real aspect ratio.
                if (hasImage) {
                    Spacer(Modifier.height(10.dp))
                    AdaptivePostImage(
                        imageUrl = post.imageUrl!!,
                        modifier = Modifier.fillMaxWidth(),
                        minRatio = 0.75f,   // tallest shown uncropped: 3:4
                        maxRatio = 1.78f,   // widest shown uncropped: 16:9
                        cornerRadius = 0.dp // outer card already clips the corners
                    )
                }
            }

            // ── Row 3: category + live tags ───────────────────────────────
            if (post.category.isNotBlank() || isFresh || isTrending) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (post.category.isNotBlank()) {
                        CategoryBadge(post.category)
                    }
                    if (isFresh) {
                        PostTag(text = "NEW", color = GreenSuccess)
                    }
                    if (isTrending) {
                        PostTag(text = "🔥 Trending", color = OrangePrimary)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                thickness = 0.5.dp
            )

            Spacer(Modifier.height(8.dp))


            // ── Row 4: action bar (all hit areas are at least 40dp tall) ──
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                val useScrollableActions = maxWidth < 320.dp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (useScrollableActions) Modifier.horizontalScroll(rememberScrollState())
                            else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Upvote
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isUpvoted)
                                    OrangeSubtle.copy(alpha = 0.25f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .drawBehind {
                                if (flash.value < 1f) {
                                    drawRect(OrangePrimary.copy(alpha = (1f - flash.value) * 0.35f))
                                }
                            }
                            .pressScale {
                                if (!isUpvoted) {
                                    haptic.upvote()
                                    upvoteBurst = true
                                    burstKey++
                                } else {
                                    haptic.tap()
                                }
                                onUpvote()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = if (isUpvoted) "Remove upvote" else "Upvote",
                            tint = if (isUpvoted)
                                OrangePrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(upvoteScale)
                        )
                        Spacer(Modifier.width(6.dp))
                        AnimatedContent(
                            targetState = post.upvotes,
                            transitionSpec = {
                                if (targetState > initialState)
                                    (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                                else
                                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                            },
                            label = "count"
                        ) { count ->
                            Text(
                                formatCount(count),
                                color = if (isUpvoted)
                                    OrangePrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Comments — now a real tap target that opens the post
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pressScale { onClick() }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.ChatBubbleOutline,
                            contentDescription = "${post.commentCount} comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            formatCount(post.commentCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    if (useScrollableActions) {
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }

                    // Save
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isSaved)
                                    OrangeSubtle.copy(0.2f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .pressScale {
                                haptic.tap()
                                onToggleSave()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSaved) "Unsave post" else "Save post",
                            tint = if (isSaved)
                                OrangePrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pressScale { sharePost() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Delete Post?")
            },
            text = {
                Text("This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(
                        "Delete",
                        color = RedAlert
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/** Small rounded status tag shown next to the category (NEW, Trending…). */
@Composable
private fun PostTag(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Minutes since the post was created, or null when createdAt can't be parsed. */
private fun Post.minutesOld(): Long? = try {
    java.time.Duration
        .between(java.time.Instant.parse(createdAt), java.time.Instant.now())
        .toMinutes()
} catch (e: Exception) {
    null
}

/** Simple engagement score; comments count double because they signal conversation. */
private fun Post.isTrending(): Boolean = upvotes + commentCount * 2 >= 20


fun formatCount(count: Int): String = when {

    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0)
    else               -> count.toString()
}
