// This is CommentCard.kt file

// java/com/nagpurpulse/ui/components/CommentCard.kt

package com.nagpurpulse.ui.components


import androidx.compose.material.icons.filled.Edit
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.Comment
import com.nagpurpulse.data.model.timeAgo
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CommentCard(
    navController: NavController,
    comment: Comment,
    isLoggedIn: Boolean = false,
    currentUserId: String? = null,
    isAdmin: Boolean = false,
    isHighlighted: Boolean = false,
    onUpvote: () -> Unit = {},
    onReplySubmit: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onReport: (String, String) -> Unit = { _, _ -> },
    onEdit: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    // Use persistent alias if anon, else real username
    val displayName = when {
        comment.isAnonymous && comment.anonAlias != null -> comment.anonAlias
        comment.isAnonymous                              -> "Anonymous"
        else                                             -> comment.username ?: "unknown"
    }
    val isAnonWithAlias = comment.isAnonymous && comment.anonAlias != null
    val avatarUrl   = "https://api.dicebear.com/7.x/avataaars/png?seed=$displayName"
    val isReply     = comment.parentId != null || comment.body.startsWith("↪ Reply to")
    val canEdit = currentUserId != null && (currentUserId == comment.userId || isAdmin)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var menuExpanded   by remember { mutableStateOf(false) }
    var upvoteBurst  by remember { mutableStateOf(false) }
    var hasUpvoted   by remember { mutableStateOf(false) }
    var localUpvotes by remember { mutableIntStateOf(comment.upvotes) }
    var showReply    by remember { mutableStateOf(false) }
    var replyText    by remember { mutableStateOf("") }
    var isReplyAnonymous by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(comment.body) }

    val upvoteIconScale by animateFloatAsState(
        targetValue   = if (upvoteBurst) 1.5f else 1f,
        animationSpec = spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
        label = "comment_upvote",
        finishedListener = { upvoteBurst = false }
    )

    val anonymousIconScale by animateFloatAsState(
        targetValue = if (isReplyAnonymous) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "anonymous_icon_scale"
    )

    val anonymousIconColor by animateColorAsState(
        targetValue =
            if (isReplyAnonymous)
                OrangePrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        label = "anonymous_icon_color"
    )

    val indentPadding = (depth * 16).coerceAtMost(48).dp
    val normalColor =
        if (isReply)
            MaterialTheme.colorScheme.surfaceVariant
        else
            MaterialTheme.colorScheme.surface

    val bgColor =
        if (isHighlighted)
            OrangePrimary.copy(alpha = 0.18f)
        else
            normalColor

    Box(modifier = modifier.fillMaxWidth()) {
        // Reply depth line
        if (isReply) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .padding(start = (indentPadding - 8.dp).coerceAtLeast(0.dp))
                    .background(OrangePrimary.copy(0.25f))
                    .clip(RoundedCornerShape(1.dp))
                    .align(Alignment.CenterStart)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (isReply) (indentPadding + 8.dp).coerceAtMost(56.dp) else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(if (isReply) 10.dp else 12.dp))
                    .background(bgColor)
                    .let {
                        if (isReply) it.border(
                            1.dp, OrangePrimary.copy(0.12f),
                            RoundedCornerShape(12.dp)
                        ) else it
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl).crossfade(true).build(),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.dp, OrangePrimary.copy(0.18f), CircleShape)
                                .background(OrangeGlow),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        if (!comment.isAnonymous) {
                                            navController.navigate("user_profile/${comment.userId}")
                                        }
                                    }
                                ) {
                                    if (comment.isAnonymous) {
                                        Icon(
                                            imageVector = Icons.Filled.VisibilityOff,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )

                                        Spacer(Modifier.width(4.dp))
                                    }

                                    Text(
                                        text = if (comment.isAnonymous) displayName else "u/$displayName",
                                        color =
                                            if (comment.isAnonymous)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else
                                                OrangePrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                // Alias badge — shows this is a consistent anonymous identity
                                if (isAnonWithAlias) {
                                    Spacer(Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                            .background(com.nagpurpulse.ui.theme.OrangeSubtle)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text("anon", color = com.nagpurpulse.ui.theme.OrangePrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text(
                                    "  ·  ",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Text(
                                    comment.timeAgo(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (comment.editedAt != null && !comment.editedByAdmin) {
                                    Text(
                                        "  ·  Edited",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic
                                    )
                                }


                            }
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {

                                if (canEdit && !comment.isDeleted) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Edit",
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Edit comment",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            editText = comment.body
                                            showEditDialog = true
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Copy", color = MaterialTheme.colorScheme.onSurface) },
                                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(comment.body.replace("↪ Reply to", "").trimStart()))
                                        menuExpanded = false
                                    }
                                )
                                if (!comment.isDeleted) {
                                    DropdownMenuItem(
                                        text = { Text("Report", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Filled.Flag,
                                                null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            onReport(comment.id, "other")
                                            Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (comment.isDeleted) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text("Removed by moderator")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(8.dp))
                    }

                    // Reply-to chip if it's a reply


                    if (isReply && comment.body.startsWith("↪ Reply to")) {
                        val replyTo = comment.body.substringAfter("↪ Reply to ").substringBefore(":")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(OrangeSubtle)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("↩ in reply to @$replyTo", color = OrangePrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    // Body
                    val bodyText = if (comment.isDeleted) {
                        "This comment was removed by a moderator."
                    } else {
                        comment.body
                            .replace("↪ Reply to", "")
                            .trimStart()
                    }
                    SelectionContainer {
                        Text(
                            text = bodyText,
                            color = if (comment.isDeleted)
                                Color.Gray
                            else
                                MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = if (comment.isDeleted)
                                FontStyle.Italic
                            else
                                FontStyle.Normal,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Actions
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        if (!comment.isDeleted) {

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (hasUpvoted) OrangeSubtle else MaterialTheme.colorScheme.surfaceVariant)
                                    .let {
                                        if (isLoggedIn) it.pressScale {
                                            if (!hasUpvoted) {
                                                hasUpvoted = true
                                                upvoteBurst = true
                                                localUpvotes++
                                                onUpvote()
                                            }
                                        } else it
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.KeyboardArrowUp,
                                    null,
                                    tint = if (hasUpvoted) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp).scale(upvoteIconScale)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    formatCount(localUpvotes),
                                    color = if (hasUpvoted) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                        }

                        if (isLoggedIn && !comment.isDeleted) {
                            Spacer(Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (showReply) OrangeSubtle else MaterialTheme.colorScheme.surfaceVariant)
                                    .pressScale { showReply = !showReply }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Reply,
                                    contentDescription = null,
                                    tint = if (showReply) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Reply",
                                    color = if (showReply) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Reply field
                    if (showReply && !comment.isDeleted) {
                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isReplyAnonymous,
                                onCheckedChange = {
                                    isReplyAnonymous = it
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OrangePrimary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = anonymousIconColor,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(anonymousIconScale)
                                )

                                Spacer(Modifier.width(4.dp))

                                Text(
                                    text = "Reply anonymously",
                                    color = anonymousIconColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isReplyAnonymous)
                                        FontWeight.SemiBold
                                    else
                                        FontWeight.Normal
                                )
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    "Write a reply…",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = OrangePrimary,
                                unfocusedBorderColor = Divider,
                                cursorColor          = OrangePrimary
                            ),
                            singleLine = false
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.pressScale {

                                    showReply = false

                                    replyText = ""

                                    focusManager.clearFocus()

                                    keyboardController?.hide()
                                }

                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                "Post Reply",
                                color = OrangePrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.pressScale {
                                    if (replyText.isNotBlank()) {

                                        onReplySubmit(
                                            comment.id,
                                            replyText,
                                            isReplyAnonymous
                                        )

                                        replyText = ""
                                        showReply = false
                                        isReplyAnonymous = false

                                        focusManager.clearFocus()

                                        keyboardController?.hide()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }


    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit comment") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write your comment…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangePrimary,
                        cursorColor = OrangePrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isBlank()) {
                            Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
                        } else if (editText.trim() != comment.body.trim()) {
                            onEdit(comment.id, editText.trim())
                            showEditDialog = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        } else {
                            showEditDialog = false
                        }
                    }
                ) { Text("Save", color = OrangePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}
