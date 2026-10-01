// ui/screens/admin/AdminPanelScreen.kt
package com.nagpurpulse.ui.screens.admin


import com.nagpurpulse.data.model.QueueItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.navigation.Screen
import com.nagpurpulse.ui.theme.Background
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.RedAlert

// ── Tab enum ─────────────────────────────────────────────────────────────────

private enum class AdminTab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Filled.Dashboard),
    Queue    ("Queue",     Icons.Filled.Flag),
    Posts    ("Posts",     Icons.Filled.Article),
    Users    ("Users",     Icons.Filled.Group),
    Logs     ("Logs",      Icons.Filled.Assignment)
}

// ── Root Screen ───────────────────────────────────────────────────────────────

@Composable
fun AdminPanelScreen(
    navController: NavController,
    viewModel: AdminViewModel = hiltViewModel()
) {
    var activeTab by remember { mutableStateOf(AdminTab.Dashboard) }
    var postsShowComments by remember { mutableStateOf(false) }
    val queueState by viewModel.queue.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = { AdminTopBar(onBack = { navController.popBackStack() }) },
        bottomBar = {
            AdminBottomNav(
                activeTab   = activeTab,
                queueBadge  = queueState.items.size.takeIf { it > 0 },
                onTabChange = { tab ->
                    activeTab = tab
                    when (tab) {
                        AdminTab.Dashboard -> viewModel.loadDashboard()
                        AdminTab.Queue     -> viewModel.loadQueue()
                        AdminTab.Posts     -> { postsShowComments = false; viewModel.loadPosts() }
                        AdminTab.Users     -> viewModel.loadUsers()
                        AdminTab.Logs      -> viewModel.loadLogs()
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeTab) {
                AdminTab.Dashboard -> AdminDashboardContent(
                    viewModel = viewModel,
                    onNavigateToPosts = { showComments ->
                        postsShowComments = showComments
                        activeTab = AdminTab.Posts
                        if (showComments) viewModel.loadComments() else viewModel.loadPosts()
                    },
                    onNavigateToUsers = {
                        activeTab = AdminTab.Users
                        viewModel.loadUsers()
                    },
                    onNavigateToQueue = { filter ->
                        activeTab = AdminTab.Queue
                        viewModel.loadQueue(filter)
                    },
                    onViewQueueItem = { item ->
                        navController.navigate(
                            Screen.Thread.createRoute(
                                postId = if (item.type == "post") item.targetId else item.postId,
                                commentId = if (item.type == "comment") item.commentId else null
                            )
                        )
                    }
                )
                AdminTab.Queue -> AdminQueueContent(
                    navController = navController,
                    viewModel = viewModel
                )
                AdminTab.Posts -> AdminPostsContent(viewModel = viewModel, initialShowComments = postsShowComments)
                AdminTab.Users -> AdminUsersContent(viewModel = viewModel)
                AdminTab.Logs -> AdminLogsContent(viewModel = viewModel)
            }
        }
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────

@Composable
private fun AdminTopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Security,
                    null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "ADMIN PANEL",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.8.sp
            )
            Text(
                "  |  Restricted Access",
                color = Color.White.copy(0.45f),
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    null,
                    tint = Color.White.copy(0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = OrangePrimary.copy(0.3f),
            thickness = 1.dp
        )
    }
}

// ── Bottom Nav ────────────────────────────────────────────────────────────────

@Composable
private fun AdminBottomNav(
    activeTab: AdminTab,
    queueBadge: Int?,
    onTabChange: (AdminTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0D0D))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            color = Color.White.copy(0.08f),
            thickness = 0.5.dp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AdminTab.entries.forEach { tab ->
                val active = tab == activeTab

                Box(contentAlignment = Alignment.TopEnd) {
                    Column(
                        modifier = Modifier
                            .pressScale(onClick = { onTabChange(tab) })
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (active) OrangePrimary else Color.White.copy(0.4f),
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            tab.label,
                            color = if (active) OrangePrimary else Color.White.copy(0.4f),
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )

                        if (active) {
                            Spacer(Modifier.height(2.dp))
                            Box(
                                Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary)
                            )
                        }
                    }

                    if (tab == AdminTab.Queue && queueBadge != null && queueBadge > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = (-2).dp, y = 2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(RedAlert),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (queueBadge > 99) "99+" else queueBadge.toString(),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
