//PostCard.kt file
// java/com/nagpurpulse/ui/components/PostCard.kt
//
// The feed card for a post.
//
// Interactions are OPT-IN: pass a handler to enable a control. A control without a handler is
// hidden (save, report, edit, delete) or rendered read-only (upvote), so the card never plays a
// "success" animation for something that does nothing.

package com.nagpurpulse.ui.components

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.R
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.ageMinutesOrNull
import com.nagpurpulse.data.model.isPostTrending
import com.nagpurpulse.data.model.relativeTimeLabel
import com.nagpurpulse.data.model.sharePreview
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.OrangeSubtle
import com.nagpurpulse.ui.theme.RedAlert
import kotlinx.coroutines.delay

private const val MINUTES_PER_DAY = 24 * 60L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostCard(
    post: Post,
    currentVote: String? = null,
    onClick: () -> Unit,
    onUserClick: ((String) -> Unit)? = null,
    onUpvote: (() -> Unit)? = null,
    /** Reserved: the feed card has no downvote control, kept so existing call sites compile. */
    @Suppress("UNUSED_PARAMETER") onDownvote: (() -> Unit)? = null,
    isSaved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isOwnPost: Boolean = false,
    isAdmin: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    /** Called with a database reason: spam, harassment, misinformation, inappropriate, other. */
    onReport: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = rememberHaptic()
    val isUpvoted = currentVote == "up"

    // ── Strings (resolved once here so they can be used in non-composable lambdas) ──
    val authorFallback = stringResource(R.string.post_card_author_fallback)
    val anonymousLabel = stringResource(R.string.post_card_anonymous)
    val upvoteActionLabel = stringResource(
        if (isUpvoted) R.string.post_card_remove_upvote else R.string.post_card_upvote
    )
    val upvoteStateLabel = stringResource(
        if (isUpvoted) R.string.post_card_upvoted else R.string.post_card_not_upvoted
    )
    val saveActionLabel = stringResource(
        if (isSaved) R.string.post_card_unsave_post else R.string.post_card_save_post
    )
    val shareActionLabel = stringResource(R.string.post_card_share_post)
    val profileActionLabel = stringResource(R.string.post_card_view_profile)
    val openPostLabel = stringResource(R.string.post_card_open_post)
    val shareFooter = stringResource(R.string.post_card_share_footer)
    val linkCopiedLabel = stringResource(R.string.post_card_link_copied)

    val shareUrl = remember(post.id) { PostLinks.shareUrl(post.id) }

    fun sharePost() {
        val text = buildString {
            append(post.title)
            sharePreview(post.body)?.let { append("\n\n").append(it) }
            shareUrl?.let { append("\n\n").append(it) }
            append("\n\n").append(shareFooter)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, null)
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            // No app can handle the share intent; nothing sensible to show.
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // ── Upvote feedback (only ever triggered when onUpvote is wired) ──────────────
    var upvoteBurst by remember { mutableStateOf(false) }
    val upvoteScale by animateFloatAsState(
        targetValue = if (upvoteBurst) 1.45f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
        label = "up_scale",
        finishedListener = { upvoteBurst = false }
    )
    var burstKey by remember { mutableIntStateOf(0) }
    val flash = remember { Animatable(1f) }
    LaunchedEffect(burstKey) {
        if (burstKey > 0) {
            flash.snapTo(0f)
            flash.animateTo(1f, tween(420))
        }
    }

    // ── Age: keeps "NEW" and the "5m ago" label honest while the card is on screen ──
    val ageMinutes by produceState<Long?>(
        initialValue = ageMinutesOrNull(post.createdAt),
        key1 = post.createdAt
    ) {
        while (true) {
            val age = ageMinutesOrNull(post.createdAt)
            value = age
            if (age == null || age >= MINUTES_PER_DAY) break // older posts never change minute to minute
            delay(60_000L)
        }
    }
    val timeLabel = remember(post.createdAt, ageMinutes) { relativeTimeLabel(post.createdAt) }
    val isFresh = ageMinutes?.let { it in 0..29 } ?: false
    val isTrending = isPostTrending(post.upvotes, post.commentCount, ageMinutes)

    val authorName = if (post.isAnonymous) {
        anonymousLabel
    } else {
        post.username?.takeIf { it.isNotBlank() } ?: authorFallback
    }
    val canOpenProfile = !post.isAnonymous && post.userId.isNotBlank() && onUserClick != null
    val isCompact = FeedLayoutManager.feedStyle == "compact"
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp < 360
    val hasImage = !post.imageUrl.isNullOrBlank()
    val canModify = isOwnPost || isAdmin

    val shape = RoundedCornerShape(18.dp)
    val subtleText = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f), shape)
            .clickable(onClickLabel = openPostLabel, onClick = onClick)
            .semantics {
                // Everything reachable by touch is also reachable from the TalkBack actions menu.
                val actions = mutableListOf<CustomAccessibilityAction>()
                if (onUpvote != null) {
                    actions += CustomAccessibilityAction(upvoteActionLabel) { onUpvote(); true }
                }
                if (onToggleSave != null) {
                    actions += CustomAccessibilityAction(saveActionLabel) { onToggleSave(); true }
                }
                actions += CustomAccessibilityAction(shareActionLabel) { sharePost(); true }
                if (canOpenProfile) {
                    actions += CustomAccessibilityAction(profileActionLabel) {
                        onUserClick?.invoke(post.userId); true
                    }
                }
                customActions = actions
            }
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {

            // ── Pinned banner ─────────────────────────────────────────────
            if (post.isPinned) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.post_card_pinned),
                        color = OrangePrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Header: avatar + author + area · time + menu ──────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    name = authorName,
                    imageUrl = post.authorAvatarUrl,
                    isAnonymous = post.isAnonymous,
                    size = 40.dp,
                    modifier = Modifier.clickable(enabled = canOpenProfile) {
                        onUserClick?.invoke(post.userId)
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
                                    onUserClick?.invoke(post.userId)
                                }
                        )
                        if (post.isVerified) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.post_card_verified),
                                tint = OrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (isOwnPost) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(OrangePrimary.copy(alpha = 0.14f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.post_card_you),
                                    color = OrangePrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!post.areaTag.isNullOrBlank()) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = subtleText,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = post.areaTag,
                                color = subtleText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                            Text(
                                text = "  ·  ",
                                color = subtleText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = timeLabel,
                            color = subtleText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.post_card_more_options),
                            tint = subtleText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (canModify && onEdit != null) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                text = { Text(stringResource(R.string.post_card_edit)) },
                                onClick = { menuExpanded = false; onEdit() }
                            )
                        }
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            text = { Text(stringResource(R.string.post_card_share)) },
                            onClick = { menuExpanded = false; sharePost() }
                        )
                        if (shareUrl != null) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) },
                                text = { Text(stringResource(R.string.post_card_copy_link)) },
                                onClick = {
                                    menuExpanded = false
                                    clipboard.setText(AnnotatedString(shareUrl))
                                    Toast.makeText(context, linkCopiedLabel, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        if (onToggleSave != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = null
                                    )
                                },
                                text = {
                                    Text(
                                        stringResource(
                                            if (isSaved) R.string.post_card_unsave else R.string.post_card_save
                                        )
                                    )
                                },
                                onClick = { menuExpanded = false; onToggleSave() }
                            )
                        }
                        if (!isOwnPost && onReport != null) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                                text = { Text(stringResource(R.string.post_card_report)) },
                                onClick = { menuExpanded = false; showReportDialog = true }
                            )
                        }
                        if (canModify && onDelete != null) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = RedAlert)
                                },
                                text = {
                                    Text(stringResource(R.string.post_card_delete), color = RedAlert)
                                },
                                onClick = { menuExpanded = false; showDeleteDialog = true }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Title block — shape depends on the density setting ─────────
            if (isCompact) {
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
                        fontWeight = FontWeight.SemiBold,
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
                if (!post.body.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = post.body,
                        color = subtleText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = post.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        // em, not sp: scales with the font size and with taller scripts like Devanagari.
                        lineHeight = 1.4.em,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!post.body.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = post.body,
                            color = subtleText,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 1.45.em,
                            maxLines = if (hasImage) 2 else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (hasImage) {
                    Spacer(Modifier.height(10.dp))
                    AdaptivePostImage(
                        imageUrl = post.imageUrl!!,
                        modifier = Modifier.fillMaxWidth(),
                        minRatio = 0.75f,   // tallest shown uncropped: 3:4
                        maxRatio = 1.78f,   // widest shown uncropped: 16:9
                        cornerRadius = 0.dp // the card already clips the corners
                    )
                }
            }

            // ── Tags: category + live state ────────────────────────────────
            if (post.category.isNotBlank() || isFresh || isTrending || post.isLocked) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (post.category.isNotBlank()) CategoryBadge(post.category)
                    if (isFresh) PostTag(stringResource(R.string.post_card_new), GreenSuccess)
                    if (isTrending) PostTag("🔥 " + stringResource(R.string.post_card_trending), OrangePrimary)
                    if (post.isLocked) PostTag("🔒 " + stringResource(R.string.post_card_locked), subtleText)
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Actions: one filled pill, everything else is a quiet ghost button ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                UpvotePill(
                    count = post.upvotes,
                    isUpvoted = isUpvoted,
                    interactive = onUpvote != null,
                    iconScale = upvoteScale,
                    flashProgress = { flash.value },
                    actionLabel = upvoteActionLabel,
                    stateLabel = upvoteStateLabel,
                    onClick = {
                        if (!isUpvoted) {
                            haptic.upvote()
                            upvoteBurst = true
                            burstKey++
                        } else {
                            haptic.tap()
                        }
                        onUpvote?.invoke()
                    }
                )

                // Comments: opens the post
                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .pressScale { onClick() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ChatBubbleOutline,
                        contentDescription = stringResource(R.string.post_card_comments_count, post.commentCount),
                        tint = subtleText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        formatCount(post.commentCount),
                        color = subtleText,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(Modifier.weight(1f))

                if (onToggleSave != null) {
                    GhostIconButton(
                        icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        description = stringResource(
                            if (isSaved) R.string.post_card_unsave_post else R.string.post_card_save_post
                        ),
                        tint = if (isSaved) OrangePrimary else subtleText,
                        onClick = {
                            haptic.tap()
                            onToggleSave()
                        }
                    )
                }

                GhostIconButton(
                    icon = Icons.Filled.Share,
                    description = shareActionLabel,
                    tint = subtleText,
                    onClick = { sharePost() }
                )
            }
        }
    }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.post_card_delete_title)) },
            text = { Text(stringResource(R.string.post_card_delete_message)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(stringResource(R.string.post_card_delete), color = RedAlert)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.post_card_cancel))
                }
            }
        )
    }

    if (showReportDialog && onReport != null) {
        PostReportDialog(
            onDismiss = { showReportDialog = false },
            onReport = onReport
        )
    }
}

// ── Pieces ────────────────────────────────────────────────────────────────────

@Composable
private fun UpvotePill(
    count: Int,
    isUpvoted: Boolean,
    interactive: Boolean,
    iconScale: Float,
    flashProgress: () -> Float,
    actionLabel: String,
    stateLabel: String,
    onClick: () -> Unit
) {
    val tint = if (isUpvoted) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (isUpvoted) OrangeSubtle.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .drawBehind {
                val f = flashProgress()
                if (f < 1f) drawRect(OrangePrimary.copy(alpha = (1f - f) * 0.35f))
            }
            .let { base -> if (interactive) base.pressScale(onClick = onClick) else base }
            .semantics { stateDescription = stateLabel }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            contentDescription = if (interactive) actionLabel else null,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .scale(iconScale)
        )
        Spacer(Modifier.width(6.dp))
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                if (targetState > initialState)
                    (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                else
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "count"
        ) { value ->
            Text(
                formatCount(value),
                color = tint,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** Transparent 48dp touch target with a 22dp icon: quiet, but still easy to hit. */
@Composable
private fun GhostIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PostReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    // Same reason keys the comment report and thread report use, so the backend sees one vocabulary.
    val reasons = listOf(
        R.string.comment_report_spam to "spam",
        R.string.comment_report_harassment to "harassment",
        R.string.comment_report_misinformation to "misinformation",
        R.string.comment_report_inappropriate to "inappropriate",
        R.string.comment_report_other to "other"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.post_card_report_title)) },
        text = {
            Column {
                reasons.forEach { (label, value) ->
                    TextButton(
                        onClick = { onReport(value); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(label)) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.post_card_cancel)) }
        }
    )
}

/** Small rounded status tag shown next to the category (NEW, Trending, Locked…). */
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
