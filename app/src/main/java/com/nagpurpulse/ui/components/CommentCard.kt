// java/com/nagpurpulse/ui/components/CommentCard.kt
package com.nagpurpulse.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nagpurpulse.R
import com.nagpurpulse.data.model.Comment
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

private const val COLLAPSED_LINES = 6

/**
 * A single comment or reply. Likes are applied optimistically by the ViewModel; the card has
 * no reply text field of its own: Reply points the screen's shared composer at this comment.
 *
 * Admins can edit any comment. That edit is silent — only the author's own edits ever show "Edited".
 */
@Composable
fun CommentCard(
    navController: NavController,
    comment: Comment,
    isLoggedIn: Boolean = false,
    isAdmin: Boolean = false,
    isHighlighted: Boolean = false,
    isReply: Boolean = false,
    replyToName: String? = null,
    onUpvote: (String) -> Unit = {},
    onReply: (Comment) -> Unit = {},
    onReport: (String, String) -> Unit = { _, _ -> },
    onEdit: (String, String) -> Unit = { _, _ -> },
    onDelete: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayName = commentDisplayName(comment)
    val nowMillis = LocalNowMillis.current
    val haptics = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val canEdit = (comment.isMine || isAdmin) && !comment.isDeleted
    val canDelete = comment.isMine && !comment.isDeleted
    val canReport = isLoggedIn && !comment.isMine && !comment.isDeleted

    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var expanded by remember(comment.id) { mutableStateOf(false) }
    var canExpand by remember(comment.id) { mutableStateOf(false) }
    var editText by remember { mutableStateOf(comment.body) }

    var highlightOn by remember { mutableStateOf(isHighlighted) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) { highlightOn = true; delay(2500); highlightOn = false }
    }

    // Burst only when the like flips on, not when an already-liked comment loads.
    val liked = comment.likedByCurrentUser
    var previouslyLiked by remember(comment.id) { mutableStateOf(liked) }
    var likeBurst by remember { mutableStateOf(false) }
    LaunchedEffect(liked) {
        if (liked && !previouslyLiked) { likeBurst = true; delay(180); likeBurst = false }
        previouslyLiked = liked
    }
    val likeScale by animateFloatAsState(
        targetValue = if (likeBurst) 1.5f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh), label = "comment_like_scale"
    )

    val baseColor = if (isReply) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val bgColor by animateColorAsState(
        if (highlightOn) OrangePrimary.copy(alpha = 0.18f) else baseColor, label = "comment_bg"
    )
    val shape = RoundedCornerShape(if (isReply) 10.dp else 12.dp)

    Box(
        modifier = modifier.fillMaxWidth().clip(shape).background(bgColor)
            .let { if (isReply) it.border(1.dp, OrangePrimary.copy(0.12f), shape) else it }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (comment.isDeleted) {
            RemovedCommentRow(byAuthor = comment.deletedByAuthor)
            return@Box
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val canOpenProfile = !comment.isAnonymous && comment.userId.isNotBlank()
                CommentAvatar(
                    displayName = displayName, avatarUrl = comment.avatarUrl, isAnonymous = comment.isAnonymous,
                    modifier = Modifier.clickable(enabled = canOpenProfile) {
                        navController.navigate("user_profile/${comment.userId}")
                    }
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (comment.isAnonymous) displayName else "u/$displayName",
                            color = if (comment.isAnonymous) MaterialTheme.colorScheme.onSurfaceVariant else OrangePrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false).clickable(enabled = canOpenProfile) {
                                navController.navigate("user_profile/${comment.userId}")
                            }
                        )
                        if (comment.isAnonymous) { Spacer(Modifier.width(4.dp)); CommentBadge(stringResource(R.string.comment_anon_badge)) }
                        if (comment.isPostAuthor) { Spacer(Modifier.width(4.dp)); CommentBadge(stringResource(R.string.comment_op_badge), filled = true) }
                        if (comment.isMine) { Spacer(Modifier.width(4.dp)); CommentBadge(stringResource(R.string.comment_you_badge)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.timeAgo(nowMillis), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        // Only the author's own edits are ever labelled.
                        if (comment.editedAt != null) {
                            Text("  ·  ${stringResource(R.string.comment_edited)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, maxLines = 1)
                        }
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.comment_more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuExpanded, onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (canEdit) DropdownMenuItem(
                            text = { Text(stringResource(R.string.comment_edit)) },
                            leadingIcon = { MenuIcon(Icons.Filled.Edit) },
                            onClick = { menuExpanded = false; editText = comment.body; showEditDialog = true })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.comment_copy)) },
                            leadingIcon = { MenuIcon(Icons.Filled.ContentCopy) },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(comment.body))
                                Toast.makeText(context, R.string.comment_copied, Toast.LENGTH_SHORT).show()
                                menuExpanded = false
                            })
                        if (canDelete) DropdownMenuItem(
                            text = { Text(stringResource(R.string.comment_delete)) },
                            leadingIcon = { MenuIcon(Icons.Filled.Delete) },
                            onClick = { menuExpanded = false; showDeleteDialog = true })
                        if (canReport) DropdownMenuItem(
                            text = { Text(stringResource(R.string.comment_report)) },
                            leadingIcon = { MenuIcon(Icons.Filled.Flag) },
                            onClick = { menuExpanded = false; showReportDialog = true })
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (!replyToName.isNullOrBlank()) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(OrangeSubtle)
                    .padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("↩ ${stringResource(R.string.comment_replying_to, replyToName)}",
                        color = OrangePrimary, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
            }

            val accent = OrangePrimary
            val styledBody = remember(comment.body, accent) { linkifyComment(comment.body, accent) }
            SelectionContainer {
                Text(
                    text = styledBody, color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge, lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { if (!expanded) canExpand = it.hasVisualOverflow }
                )
            }
            if (canExpand || expanded) {
                Text(
                    stringResource(if (expanded) R.string.comment_show_less else R.string.comment_read_more),
                    color = OrangePrimary, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp).clickable { expanded = !expanded }
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val likeColor = if (liked) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (liked) OrangeSubtle else MaterialTheme.colorScheme.surfaceVariant)
                        .let {
                            if (isLoggedIn) it.pressScale {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpvote(comment.id)
                            } else it
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(if (liked) R.string.comment_unlike else R.string.comment_like),
                        tint = likeColor, modifier = Modifier.size(16.dp).scale(likeScale))
                    if (comment.upvotes > 0) {
                        Spacer(Modifier.width(4.dp))
                        AnimatedContent(
                            targetState = comment.upvotes,
                            transitionSpec = {
                                if (targetState > initialState)
                                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                                else
                                    (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                            },
                            label = "comment_like_count"
                        ) { count ->
                            Text(formatCount(count), color = likeColor,
                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (isLoggedIn) {
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pressScale { onReply(comment) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.comment_reply), color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.comment_edit_title)) },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { if (it.length <= MAX_COMMENT_LENGTH) editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.comment_edit_hint)) },
                    minLines = 3, maxLines = 8,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(stringResource(R.string.comment_char_counter, editText.length, MAX_COMMENT_LENGTH),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangePrimary, cursorColor = OrangePrimary)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = editText.trim()
                    when {
                        trimmed.isEmpty() -> Toast.makeText(context, R.string.comment_empty_error, Toast.LENGTH_SHORT).show()
                        trimmed != comment.body.trim() -> { onEdit(comment.id, trimmed); showEditDialog = false }
                        else -> showEditDialog = false
                    }
                }) { Text(stringResource(R.string.comment_save), color = OrangePrimary) }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.comment_cancel)) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.comment_delete_title)) },
            text = { Text(stringResource(R.string.comment_delete_message)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete(comment.id) }) {
                    Text(stringResource(R.string.comment_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.comment_cancel)) } }
        )
    }

    if (showReportDialog) {
        val reasons = listOf(
            R.string.comment_report_spam to "spam", R.string.comment_report_harassment to "harassment",
            R.string.comment_report_misinformation to "misinformation",
            R.string.comment_report_inappropriate to "inappropriate", R.string.comment_report_other to "other"
        )
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(stringResource(R.string.comment_report_title)) },
            text = {
                Column {
                    reasons.forEach { (label, value) ->
                        TextButton(onClick = { showReportDialog = false; onReport(comment.id, value) },
                            modifier = Modifier.fillMaxWidth()) { Text(stringResource(label)) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text(stringResource(R.string.comment_cancel)) } }
        )
    }
}

/** One slim line instead of a card + chip + repeated text. */
@Composable
private fun RemovedCommentRow(byAuthor: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Block, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (byAuthor) R.string.comment_deleted_author else R.string.comment_removed_mod),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
    }
}

@Composable
private fun CommentBadge(text: String, filled: Boolean = false) {
    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (filled) OrangePrimary else OrangeSubtle)
        .padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(text, color = if (filled) MaterialTheme.colorScheme.onPrimary else OrangePrimary,
            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun MenuIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
}

/** Local-only row for a comment that is still sending (or failed), with one-tap retry. */
@Composable
fun PendingCommentRow(
    body: String, isAnonymous: Boolean, failed: Boolean,
    onRetry: () -> Unit, onDiscard: () -> Unit, modifier: Modifier = Modifier
) {
    val accent = if (failed) MaterialTheme.colorScheme.error else OrangePrimary
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = if (failed) 1f else 0.7f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (failed) 1f else 0.7f),
            style = MaterialTheme.typography.bodyLarge, maxLines = COLLAPSED_LINES, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (failed) {
                Text(stringResource(R.string.comment_failed), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.comment_retry), color = OrangePrimary,
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onRetry() })
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.comment_discard), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable { onDiscard() })
            } else {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = OrangePrimary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.comment_sending), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium)
            }
            if (isAnonymous) { Spacer(Modifier.width(8.dp)); CommentBadge(stringResource(R.string.comment_anon_badge)) }
        }
    }
}
