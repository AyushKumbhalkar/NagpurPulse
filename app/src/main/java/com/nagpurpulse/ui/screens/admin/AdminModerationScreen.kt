// java/com/nagpurpulse/ui/screens/admin/AdminDashboardScreen.kt

package com.nagpurpulse.ui.screens.admin

import android.util.Log

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest

import com.nagpurpulse.data.model.*
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.*

// ══════════════════════════════════════════════════════════════════════════════

// 2. MODERATION QUEUE
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminQueueContent(
    navController: NavController,
    viewModel: AdminViewModel
) {
    val state          by viewModel.queue.collectAsState()
    val loadingIds     by viewModel.loadingItemIds.collectAsState()
    var showDeleteId   by remember { mutableStateOf<String?>(null) }
    var pendingItem    by remember { mutableStateOf<QueueItem?>(null) }

    LaunchedEffect(Unit) { if (state.items.isEmpty()) viewModel.loadQueue() }

    if (showDeleteId != null && pendingItem != null) {
        ReasonDialog(
            title      = "Delete ${if (pendingItem!!.type == "post") "Post" else "Comment"}",
            onDismiss  = { showDeleteId = null; pendingItem = null },
            onConfirm  = { reason ->
                viewModel.deleteFromQueue(pendingItem!!, reason)
                showDeleteId = null; pendingItem = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter chips
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pending = state.items.size
            AdminFilterChip("All${if (pending > 0) " ($pending)" else ""}", state.selectedFilter == "all") {
                viewModel.loadQueue("all")
            }
            AdminFilterChip("Posts",    state.selectedFilter == "posts")    { viewModel.loadQueue("posts")    }
            AdminFilterChip("Comments", state.selectedFilter == "comments") { viewModel.loadQueue("comments") }
            AdminFilterChip("Urgent", state.selectedFilter == "urgent") { viewModel.loadQueue("urgent") }
        }

        if (state.error != null && !state.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(RedAlert.copy(0.10f))
                    .border(1.dp, RedAlert.copy(0.25f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ErrorOutline, null, tint = RedAlert, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.error ?: "Unable to load moderation queue", color = RedAlert, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = GreenSuccess,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No pending reports", color = SecondaryText, fontSize = 15.sp)
                    Text("The moderation queue is clear", color = TertiaryText, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    QueueItemCard(
                        item      = item,
                        isLoading = item.id in loadingIds,
                        onView = {
                            navController.navigate(
                                Screen.Thread.createRoute(
                                    postId = if (item.type == "post") item.targetId else item.postId,
                                    commentId = if (item.type == "comment") item.commentId else null
                                )
                            )
                        },
                        onDelete  = { showDeleteId = item.id; pendingItem = item },
                        onIgnore  = { viewModel.dismissReport(item) },
                        onWarn    = { viewModel.warnUserFromQueue(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemCard(
    item: QueueItem,
    isLoading: Boolean,
    onView: () -> Unit,
    onDelete: () -> Unit,
    onIgnore: () -> Unit,
    onWarn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                color    = OrangePrimary,
                trackColor = Color.Transparent
            )
        }
        Column(Modifier.padding(14.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(RedAlert.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Flag, null, tint = RedAlert, modifier = Modifier.size(15.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (item.type == "post") "Reported Post" else "Reported Comment",
                    color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                // Reason chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RedAlert.copy(0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Reason: ${item.topReason}", color = RedAlert, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Content
            Text(
                item.title,
                color = PrimaryText, fontSize = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Group, null, tint = TertiaryText, modifier = Modifier.size(12.dp))
                Text(
                    " Reported by ${item.reportCount} ${if (item.reportCount == 1) "user" else "users"}  •  Posted by @${item.authorUsername}  •  ${item.timeAgo}",
                    color = TertiaryText, fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(10.dp))

            // Action row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QueueAction("View",    OrangePrimary,  false, Modifier.weight(1f)) { onView() }
                QueueAction("Delete",  RedAlert,       true,  Modifier.weight(1f)) { onDelete() }
                QueueAction("Ignore",  SecondaryText,  false, Modifier.weight(1f)) { onIgnore() }
                QueueAction("Warn",    YellowWarn,     false, Modifier.weight(1f)) { onWarn() }
            }
        }
    }
}

@Composable
private fun QueueAction(label: String, color: Color, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (filled) Modifier.background(color)
                else Modifier.border(1.dp, color.copy(0.4f), RoundedCornerShape(8.dp))
            )
            .pressScale(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (filled) Color.White else color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
