//java/com/nagpurpulse/ui/screens/admin/AdminDashboardScreen.kt

package com.nagpurpulse.ui.screens.admin

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.nagpurpulse.data.model.QueueItem
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.Divider
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.PrimaryText
import com.nagpurpulse.ui.theme.RedAlert
import com.nagpurpulse.ui.theme.SecondaryText
import com.nagpurpulse.ui.theme.Surface
import com.nagpurpulse.ui.theme.TertiaryText

// ══════════════════════════════════════════════════════════════════════════════
// 1. DASHBOARD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AdminDashboardContent(
    viewModel: AdminViewModel,
    onNavigateToPosts: (Boolean) -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToQueue: (String) -> Unit,
    onViewQueueItem: (QueueItem) -> Unit
) {
    val state by viewModel.dashboard.collectAsState()
    var pendingDelete by remember { mutableStateOf<QueueItem?>(null) }

    pendingDelete?.let { item ->
        ReasonDialog(
            title = "Delete ${if (item.type == "post") "Post" else "Comment"}",
            onDismiss = { pendingDelete = null },
            onConfirm = { reason ->
                viewModel.deleteFromQueue(item, reason)
                pendingDelete = null
            }
        )
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Admin Dashboard", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("NagpurPulse Moderation", color = SecondaryText, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, OrangePrimary.copy(0.4f), RoundedCornerShape(20.dp))
                        .background(OrangePrimary.copy(0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (state.adminRole == "super_admin") "Super Admin"
                            else if (state.adminRole == "moderator") "Moderator"
                            else "Admin",
                            color = OrangePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!state.adminProfile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(state.adminProfile!!.avatarUrl).crossfade(true).build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // ── Stats Row 1: Posts / Comments / New Users ───────────────────────
        item {
            if (state.isLoading) {
                ShimmerStatRow()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard("Posts Today",     state.stats.postsToday.toString(),     Icons.Filled.Article,     OrangePrimary, Modifier.weight(1f)) { onNavigateToPosts(false) }
                    DashStatCard("Comments Today",  state.stats.commentsToday.toString(),  Icons.Filled.ChatBubble,  OrangePrimary, Modifier.weight(1f)) { onNavigateToPosts(true) }
                    DashStatCard("New Users",       state.stats.newUsersToday.toString(),  Icons.Filled.PersonAdd,   OrangePrimary, Modifier.weight(1f)) { onNavigateToUsers() }
                }
            }
        }

        // ── Stats Row 2: Reports / Urgent ───────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlertStatCard(
                    label    = "Reported Posts",
                    value    = state.stats.reportedPosts.toString(),
                    icon     = Icons.Filled.Flag,
                    modifier = Modifier.weight(1f),
                    onClick  = { onNavigateToQueue("posts") }
                )
                AlertStatCard(
                    label    = "Reported Comments",
                    value    = state.stats.reportedComments.toString(),
                    icon     = Icons.Filled.Report,
                    modifier = Modifier.weight(1f),
                    onClick  = { onNavigateToQueue("comments") }
                )
                UrgentStatCard(
                    value    = state.stats.urgentReports.toString(),
                    modifier = Modifier.weight(1f),
                    onClick  = { onNavigateToQueue("urgent") }
                )
            }
        }

        // ── Activity Chart ──────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .border(1.dp, Divider, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Activity This Week", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Divider, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("This Week", color = SecondaryText, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ActivityLineChart(
                        data   = state.weeklyActivity,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach { day ->
                            Text(day, color = TertiaryText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // ── Pending Queue Preview ───────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pending Moderation Queue", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("View All →", color = OrangePrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.pressScale(onClick = { onNavigateToQueue("all") }))
            }
        }

        if (state.isLoading) {
            item { ShimmerCard() }
        } else if (state.recentQueue.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = GreenSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "No pending items",
                            color = GreenSuccess,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(state.recentQueue, key = { it.id }) { item ->
                QueueItemCard(
                    item      = item,
                    isLoading = false,
                    onView    = { onViewQueueItem(item) },
                    onDelete  = { pendingDelete = item },
                    onIgnore  = { viewModel.dismissReport(item) },
                    onWarn    = { viewModel.warnUserFromQueue(item) }
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Stat Cards ────────────────────────────────────────────────────────────────

@Composable
private fun DashStatCard(label: String, value: String, icon: ImageVector, iconTint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .pressScale(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(iconTint.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.height(8.dp))
        Text(label, color = SecondaryText, fontSize = 11.sp)
        Text(value, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun AlertStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .pressScale(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .drawBehind {
                drawRoundRect(
                    color        = RedAlert,
                    topLeft      = Offset.Zero,
                    size         = size.copy(width = 3.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f),
                )
            }
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(RedAlert.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = RedAlert, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(label, color = SecondaryText, fontSize = 11.sp)
        Text(value, color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun UrgentStatCard(value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .pressScale(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(RedAlert.copy(0.08f))
            .border(1.dp, RedAlert.copy(0.25f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(RedAlert.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Warning, null, tint = RedAlert, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.height(6.dp))
        Text("Urgent Reports", color = RedAlert, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = RedAlert, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

// ── Activity Line Chart ───────────────────────────────────────────────────────

@Composable
private fun ActivityLineChart(data: List<Int>, modifier: Modifier) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue    = 1f,
            animationSpec  = tween(durationMillis = 900, easing = FastOutLinearInEasing)
        )
    }
    // Read value in composable scope so Canvas redraws on each animation frame
    val progress = animProgress.value
    val orange   = OrangePrimary

    Canvas(modifier = modifier) {
        if (data.isEmpty() || data.all { it == 0 }) return@Canvas
        val maxVal  = (data.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val padL    = 0f
        val padR    = 0f
        val padT    = 10f
        val padB    = 10f
        val w       = size.width  - padL - padR
        val h       = size.height - padT - padB
        val n       = data.size

        fun xOf(i: Int) = padL + (i.toFloat() / (n - 1)) * w
        fun yOf(v: Int) = padT + h - (v / maxVal) * h

        val path = Path()
        data.forEachIndexed { i, v ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // Clipping for animation
        val clipW = size.width * progress
        clipRect(right = clipW) {
            // Fill gradient
            val fillPath = Path().apply {
                addPath(path)
                lineTo(xOf(data.size - 1), padT + h)
                lineTo(xOf(0), padT + h)
                close()
            }
            drawPath(
                fillPath,
                Brush.verticalGradient(
                    colors     = listOf(orange.copy(alpha = 0.28f), orange.copy(alpha = 0f)),
                    startY     = padT,
                    endY       = padT + h
                )
            )
            // Line stroke
            drawPath(
                path,
                color      = orange,
                style      = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Dots
            data.forEachIndexed { i, v ->
                drawCircle(color = orange, radius = 4.5f, center = Offset(xOf(i), yOf(v)))
                drawCircle(color = Color(0xFF111111), radius = 2f, center = Offset(xOf(i), yOf(v)))
            }
        }
    }
}

@Composable
private fun ShimmerStatRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
            )
        }
    }
}

@Composable
private fun ShimmerCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
    )
}
