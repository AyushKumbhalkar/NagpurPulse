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
    val isUpvoted = currentVote == "up"
    val isDownvoted = currentVote == "down"

    fun sharePost() {
        val i = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, "${post.title}\n\n${post.body ?: ""}")
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

    val cardColor = MaterialTheme.colorScheme.surface
    val catColor = categoryColor(post.category)
    val isCompact = FeedLayoutManager.feedStyle == "compact"
    val hasImage = !post.imageUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(cardColor)
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
        // way to the card's edges — exactly like Reddit's own feed cards —
        // while every other row keeps its normal text margin.
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)) {

            // ── Row 1: badge + time + menu ────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.category.isNotBlank()) {
                    CategoryBadge(post.category)
                }

                Spacer(Modifier.weight(1f))
                Text(
                    post.timeAgo(),
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
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
                            text = { Text(if (isSaved) "Unsave" else "Save", color = TextPrimary) },
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

            Spacer(Modifier.height(4.dp))

            // ── Row 2: title block — shape depends on density setting ─────
            if (isCompact) {

                // COMPACT: dense single row, 2-line title, fixed square
                // thumbnail on the right. This mirrors Reddit's own
                // "Compact" list style — every thumbnail is the same size no
                // matter what shape the source photo is, so rows scan fast
                // and stay perfectly aligned.
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val thumbnailSize = if (maxWidth < 340.dp) 64.dp else 76.dp
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
                            maxLines = if (maxWidth < 340.dp) 3 else 2,
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

                // Compact body preview — one short line, kept tight so the
                // row stays scannable.
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
                        lineHeight = 22.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!post.body.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = post.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            // Give text-only posts more room to breathe since
                            // there's no photo competing for attention.
                            maxLines = if (hasImage) 2 else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // EXPANDED image: full-bleed, real aspect ratio, Reddit-style.
                // No horizontal padding here on purpose — it touches both
                // edges of the card.
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

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = if (post.isAnonymous) "u/Anonymous" else "u/${post.username ?: "unknown"}",
                    color = OrangePrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable {
                            if (!post.isAnonymous) onUserClick(post.userId)
                        }
                )

                if (post.isVerified) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = OrangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                if (post.areaTag != null) {

                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )

                    Spacer(Modifier.width(2.dp))

                    Text(
                        text = post.areaTag,
                        color = TextTertiary,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 104.dp)
                    )
                }

                Spacer(Modifier.weight(1f))


            }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.outline,
                thickness = 0.5.dp
            )

            Spacer(Modifier.height(12.dp))


            // ── Row 4: action bar ─────────────────────────────────────────
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                // Upvote
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isUpvoted)
                                OrangeSubtle.copy(alpha = 0.25f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .pressScale {
                            upvoteBurst = true
                            onUpvote()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        null,
                        tint = if (isUpvoted)
                            OrangePrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .scale(upvoteScale)
                    )
                    Spacer(Modifier.width(5.dp))
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



                // Comments
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatCount(post.commentCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                }

                if (useScrollableActions) {
                    Spacer(Modifier.width(8.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Save
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (isSaved)
                                OrangeSubtle.copy(0.2f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .pressScale(onClick = onToggleSave)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        null,
                        tint = if (isSaved)
                            OrangePrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Share
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale(onClick = { sharePost() })
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Filled.Share, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
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

fun formatCount(count: Int): String = when {

    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0)
    else               -> count.toString()
}
