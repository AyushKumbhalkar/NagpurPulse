// java/com/nagpurpulse/ui/screens/admin/AdminSharedComponents.kt

package com.nagpurpulse.ui.screens.admin

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.data.model.AdminComment
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.*

// SHARED COMPONENTS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ContentToggle(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) OrangePrimary else Color.Transparent)
            .pressScale(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color      = if (selected) Color.White else SecondaryText,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize   = 13.sp
        )
    }
}

@Composable
fun AdminCommentCard(
    adminComment: AdminComment,
    isLoading: Boolean,
    onDelete: () -> Unit,
    onWarn: () -> Unit
) {
    val comment = adminComment.comment

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, if (adminComment.reportCount > 0) RedAlert.copy(0.3f) else Divider, RoundedCornerShape(14.dp))
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier   = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                color      = OrangePrimary,
                trackColor = Color.Transparent
            )
        }
        Column(Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(OrangePrimary.copy(0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!comment.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(comment.avatarUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "@${comment.username ?: "user"}",
                        color = OrangePrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                    )
                    Text("Recently", color = TertiaryText, fontSize = 11.sp)
                }
                if (adminComment.reportCount > 0) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RedAlert.copy(0.15f)).padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = RedAlert,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                adminComment.reportCount.toString(),
                                color = RedAlert,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Filled.MoreVert, null, tint = TertiaryText, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            // Comment body
            Text(
                comment.body,
                color    = PrimaryText,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            // Post reference
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Article, null, tint = TertiaryText, modifier = Modifier.size(12.dp))
                Text(
                    "  on: ${adminComment.postTitle}",
                    color    = TertiaryText,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminIconAction(Icons.Filled.Delete,  RedAlert,      Modifier.weight(1f)) { onDelete() }
                AdminIconAction(Icons.Filled.Warning, YellowWarn,    Modifier.weight(1f)) { onWarn()   }
            }
        }
    }
}

@Composable
fun AdminFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) OrangePrimary else Surface)
            .border(
                1.dp,
                if (selected) OrangePrimary else Divider,
                RoundedCornerShape(20.dp)
            )
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color      = if (selected) Color.White else SecondaryText,
            fontSize   = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun ReasonDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceAlt,
        title = { Text(title, color = PrimaryText, fontWeight = FontWeight.Bold) },
        text  = {
            Column {
                Text("Select a reason:", color = SecondaryText, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                listOf("Spam", "Harassment", "Misinformation", "Inappropriate content", "Other").forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(onClick = { reason = opt })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = reason == opt,
                            onClick  = { reason = opt },
                            colors   = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                        )
                        Text(opt, color = if (reason == opt) PrimaryText else SecondaryText, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (reason.isNotBlank()) onConfirm(reason) },
                enabled  = reason.isNotBlank()
            ) {
                Text("Confirm", color = if (reason.isNotBlank()) RedAlert else TertiaryText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SecondaryText) }
        }
    )
}

@Composable
fun AdminIconAction(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(
                1.dp,
                tint.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
            .pressScale(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}