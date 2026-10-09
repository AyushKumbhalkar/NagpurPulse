//java/com/nagpurpulse/ui/screens/alerts/AlertsScreen.kt

package com.nagpurpulse.ui.screens.alerts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.model.AlertStatus
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.model.alertStatus
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

private data class AlertFilter(val key: String, val label: String, val emoji: String)

private val alertFilters = listOf(
    AlertFilter("all", "All", "🔔"),
    AlertFilter("traffic", "Traffic", "🚗"),
    AlertFilter("power", "Power", "⚡"),
    AlertFilter("water", "Water", "💧"),
    AlertFilter("weather", "Weather", "🌧"),
    AlertFilter("safety", "Safety", "🛡️"),
    AlertFilter("events", "Events", "📢")
)

@Composable
fun AlertsScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onReportAlert: () -> Unit,
    onProfileClick: () -> Unit,
    onCreatePost: () -> Unit,
    onNotifications: () -> Unit = {},
    isLoggedIn: () -> Boolean = { true },
    onLoginRequired: () -> Unit = {},
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Re-evaluates "live / ended" and "updated Xm ago" every 30 seconds.
    val nowMs by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    // Live dot
    val t = rememberInfiniteTransition(label = "live")
    val dotAlpha by t.animateFloat(
        1f, 0.2f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot_alpha"
    )

    // FAB entrance + collapse-on-scroll
    var fabVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); fabVisible = true }
    val fabScale by animateFloatAsState(
        if (fabVisible) 1f else 0f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "fab"
    )
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    // Status is computed once per tick so every card agrees.
    val now = remember(nowMs) { Instant.ofEpochMilli(nowMs) }
    val statusById = remember(uiState.alerts, now) {
        uiState.alerts.associate { it.id to it.alertStatus(now) }
    }
    val activeAlerts = remember(uiState.alerts, statusById) {
        uiState.alerts.filter { statusById[it.id] == AlertStatus.ACTIVE }
    }
    val visible = remember(uiState.alerts, uiState.selectedFilter) {
        if (uiState.selectedFilter == "all") uiState.alerts
        else uiState.alerts.filter { it.category.equals(uiState.selectedFilter, ignoreCase = true) }
    }
    val visibleActive = visible.filter { statusById[it.id] == AlertStatus.ACTIVE }
    val visibleEarlier = visible.filter { statusById[it.id] != AlertStatus.ACTIVE }

    fun requireLogin(action: () -> Unit) {
        if (isLoggedIn()) action() else onLoginRequired()
    }

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(RedAlert.copy(alpha = 0.10f), Background))
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RedAlert.copy(alpha = dotAlpha))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Live Alerts", color = PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            alertsSubtitle(activeAlerts.size, uiState.lastUpdatedMs, nowMs),
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceAlt)
                            .pressScale { haptic.tap(); onNotifications() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Filter chips with live counts
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alertFilters.forEach { f ->
                        val selected = uiState.selectedFilter == f.key
                        val count = if (f.key == "all") activeAlerts.size
                        else activeAlerts.count { it.category.equals(f.key, ignoreCase = true) }
                        FilterChipItem(
                            label = f.label,
                            emoji = f.emoji,
                            count = count,
                            selected = selected,
                            onClick = { haptic.tap(); viewModel.setFilter(f.key) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Divider)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptic.alert(); onReportAlert() },
                containerColor = RedAlert,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.scale(fabScale)
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = "Report an alert")
                    AnimatedVisibility(visible = fabExpanded) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.width(8.dp))
                            Text("Report Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onCreatePost = onCreatePost,
                onProfileClick = onProfileClick,
                hasAlertBadge = activeAlerts.isNotEmpty()
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            SwipeRefresh(
                state = swipeRefreshState,
                onRefresh = { viewModel.loadAlerts(refresh = true) }
            ) {
                when {
                    uiState.isLoading -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) { ShimmerPostCard() }
                    }

                    uiState.loadFailed -> ScrollableCenter {
                        AlertsMessageState(
                            emoji = "📡",
                            title = "Can't reach live alerts",
                            body = "Check your connection and try again. Don't rely on this screen until it loads.",
                            primaryLabel = "Try again",
                            primaryColor = BlueInfo,
                            onPrimary = { viewModel.loadAlerts() }
                        )
                    }

                    uiState.alerts.isEmpty() -> ScrollableCenter {
                        AlertsMessageState(
                            emoji = "✅",
                            title = "All quiet in Nagpur",
                            body = "No alerts right now. Seen a jam, a power cut or heavy rain? Tell your neighbours in 10 seconds.",
                            primaryLabel = "Report an alert",
                            primaryColor = RedAlert,
                            onPrimary = { haptic.alert(); onReportAlert() }
                        )
                    }

                    visible.isEmpty() -> ScrollableCenter {
                        val label = alertFilters.firstOrNull { it.key == uiState.selectedFilter }?.label ?: "These"
                        AlertsMessageState(
                            emoji = "🎉",
                            title = "No $label alerts",
                            body = "Nothing reported in this category. Check all alerts or report something you've seen.",
                            primaryLabel = "Show all alerts",
                            primaryColor = OrangePrimary,
                            onPrimary = { viewModel.setFilter("all") }
                        )
                    }

                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (visibleActive.isNotEmpty()) {
                            item(key = "header_active") {
                                SectionHeader("Active now", visibleActive.size, RedAlert)
                            }
                            itemsIndexed(visibleActive, key = { _, a -> a.id }) { index, alert ->
                                StaggeredItem(index) {
                                    AlertRow(
                                        alert = alert,
                                        status = AlertStatus.ACTIVE,
                                        uiState = uiState,
                                        onPostClick = onPostClick,
                                        onConfirm = {
                                            requireLogin {
                                                haptic.success()
                                                viewModel.confirmAlert(alert.id)
                                            }
                                        },
                                        onResolve = {
                                            haptic.success()
                                            viewModel.resolveAlert(alert.id)
                                        }
                                    )
                                }
                            }
                        }
                        if (visibleEarlier.isNotEmpty()) {
                            item(key = "header_earlier") {
                                SectionHeader("Earlier", visibleEarlier.size, TextTertiary)
                            }
                            itemsIndexed(visibleEarlier, key = { _, a -> a.id }) { _, alert ->
                                AlertRow(
                                    alert = alert,
                                    status = statusById[alert.id] ?: AlertStatus.EXPIRED,
                                    uiState = uiState,
                                    onPostClick = onPostClick,
                                    onConfirm = {},
                                    onResolve = {}
                                )
                            }
                        }
                    }
                }
            }

            // "N new alerts" pill: new alerts never shove the list while you read it.
            AnimatedVisibility(
                visible = uiState.pendingNew.isNotEmpty(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                val n = uiState.pendingNew.size
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(RedAlert)
                        .pressScale {
                            haptic.tap()
                            viewModel.showPending()
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ArrowUpward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (n == 1) "1 new alert" else "$n new alerts",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertRow(
    alert: Post,
    status: AlertStatus,
    uiState: AlertsUiState,
    onPostClick: (String) -> Unit,
    onConfirm: () -> Unit,
    onResolve: () -> Unit
) {
    AlertCard(
        post = alert,
        status = status,
        isConfirmed = alert.id in uiState.confirmedIds,
        isOwner = alert.userId == uiState.currentUserId,
        onClick = { onPostClick(alert.id) },
        onConfirm = onConfirm,
        onResolve = onResolve
    )
}

@Composable
private fun FilterChipItem(
    label: String,
    emoji: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) RedAlert else SurfaceAlt)
            .border(1.dp, if (selected) RedAlert else Divider, RoundedCornerShape(50))
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) Color.White else SecondaryText,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                "$count",
                color = if (selected) RedAlert else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else RedAlert)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, start = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            title.uppercase(),
            color = SecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(8.dp))
        Text("$count", color = TextTertiary, fontSize = 12.sp)
    }
}

@Composable
private fun ScrollableCenter(content: @Composable () -> Unit) {
    // Keeps pull-to-refresh working on empty / error states.
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun AlertsMessageState(
    emoji: String,
    title: String,
    body: String,
    primaryLabel: String,
    primaryColor: Color,
    onPrimary: () -> Unit
) {
    Column(
        Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(primaryColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 40.sp) }
        Spacer(Modifier.height(20.dp))
        Text(title, color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = SecondaryText, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(primaryColor)
                .pressScale(onClick = onPrimary)
                .padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(primaryLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

private fun alertsSubtitle(activeCount: Int, lastUpdatedMs: Long?, nowMs: Long): String {
    val updated = lastUpdatedMs?.let {
        val mins = ((nowMs - it) / 60_000).coerceAtLeast(0)
        if (mins < 1) "updated just now" else "updated ${mins}m ago"
    }
    val active = when (activeCount) {
        0 -> "No active alerts"
        1 -> "1 active alert"
        else -> "$activeCount active alerts"
    }
    return if (updated != null) "$active in Nagpur · $updated" else "$active in Nagpur"
}
