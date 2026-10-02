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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.nagpurpulse.data.model.Post
import com.nagpurpulse.data.repository.AlertRepository
import com.nagpurpulse.ui.components.*
import com.nagpurpulse.ui.navigation.BottomNavBar
import com.nagpurpulse.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedFilter: String = "all",
    val error: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertRepository: AlertRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState

    init { loadAlerts(); subscribeToRealtime() }

    fun loadAlerts(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = !refresh, isRefreshing = refresh)
            val category = _uiState.value.selectedFilter.let { if (it == "all") null else it }
            alertRepository.getAlerts(category).fold(
                onSuccess = { alerts -> _uiState.value = _uiState.value.copy(alerts = alerts, isLoading = false, isRefreshing = false) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false) }
            )
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadAlerts()
    }

    private fun subscribeToRealtime() {
        viewModelScope.launch {
            alertRepository.subscribeToAlerts().collect { newAlert ->
                val current = _uiState.value.alerts.toMutableList()
                current.add(0, newAlert)
                _uiState.value = _uiState.value.copy(alerts = current)
            }
        }
    }
}

private val alertFilters = listOf("all", "traffic", "power", "weather", "events", "rants")

private fun filterLabel(f: String) = when (f) {
    "all" -> "All"
    "traffic" -> "Traffic"
    "power" -> "Power"
    "weather" -> "Weather"
    "events" -> "Events"
    "rants" -> "Rants"
    else -> f.replaceFirstChar { it.uppercase() }
}

private fun filterIcon(f: String) = when (f) {
    "all" -> Icons.Filled.GridView
    "traffic" -> Icons.Filled.DirectionsCar
    "power" -> Icons.Filled.Bolt
    "weather" -> Icons.Filled.Cloud
    "events" -> Icons.Filled.Event
    "rants" -> Icons.Filled.Mood
    else -> Icons.Filled.Label
}

@Composable
fun AlertsScreen(
    navController: NavController,
    onPostClick: (String) -> Unit,
    onReportAlert: () -> Unit,
    onProfileClick: () -> Unit,
    onCreatePost: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState           = viewModel.uiState.collectAsState().value
    val swipeRefreshState = rememberSwipeRefreshState(uiState.isRefreshing)

    // Live dot
    val t = rememberInfiniteTransition(label = "live")
    val dotAlpha by t.animateFloat(1f, 0.2f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "dot_alpha")

    // FAB entrance
    var fabVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(350); fabVisible = true }
    val fabScale by animateFloatAsState(
        if (fabVisible) 1f else 0f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "fab"
    )

    Scaffold(
        containerColor = Background,
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Surface, Background),
                            0f,
                            280f
                        )
                    )
                    .statusBarsPadding()
            ) {
                // Title row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing live dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RedAlert.copy(alpha = dotAlpha))
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = RedAlert,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(Modifier.width(7.dp))

                        Text(
                            "Live Alerts",
                            color = PrimaryText,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Heartbeat icon suffix

                    Spacer(Modifier.weight(1f))
                    // Search button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceAlt)
                            .pressScale(onClick = {}),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Search, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    // Notification bell
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceAlt)
                            .pressScale(onClick = {}),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Notifications, null, tint = SecondaryText, modifier = Modifier.size(20.dp))
                        // Badge
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(OrangePrimary)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = SecondaryText,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        "Real-time updates from Nagpur",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    alertFilters.forEach { filter ->
                        val isSelected = uiState.selectedFilter == filter
                        val chipScale by animateFloatAsState(
                            if (isSelected) 1.05f else 1f,
                            spring(Spring.DampingRatioMediumBouncy),
                            label = "chip_$filter"
                        )
                        Row(
                            modifier = Modifier
                                .scale(chipScale)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
    if (isSelected)
        OrangeSubtle
    else
        SurfaceAlt
)
                                .border(
                                    1.dp,
                                    if (isSelected) OrangePrimary.copy(alpha = 0.6f) else Color.Transparent,
                                    RoundedCornerShape(22.dp)
                                )
                                .pressScale { viewModel.setFilter(filter) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = filterIcon(filter),
                                contentDescription = null,
                                tint = if (isSelected) OrangePrimary else SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(Modifier.width(5.dp))

                            Text(
                                filterLabel(filter),
                                color = if (isSelected) OrangePrimary else SecondaryText,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
            }
        },
        floatingActionButton = {
            Box(modifier = Modifier.scale(fabScale)) {
                FloatingActionButton(
                    onClick        = onReportAlert,
                    containerColor = RedAlert,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Report Alert", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                navController = navController,
                onCreatePost  = onCreatePost,
                onProfileClick = onProfileClick,
                hasAlertBadge = uiState.alerts.isNotEmpty()
            )
        }
    ) { paddingValues ->
        SwipeRefresh(
            state     = swipeRefreshState,
            onRefresh = { viewModel.loadAlerts(refresh = true) },
            modifier  = Modifier.padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { items(5) { ShimmerPostCard() } }

                uiState.alerts.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("All clear in Nagpur!", color = PrimaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Text("No active alerts right now.", color = SecondaryText, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(uiState.alerts) { index, alert ->
                        StaggeredItem(index = index) {
                            AlertCard(post = alert, onClick = { onPostClick(alert.id) })
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
