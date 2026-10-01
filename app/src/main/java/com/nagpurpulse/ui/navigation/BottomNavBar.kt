
// This is the BottomNavBar.kt file
//java/com/nagpurpulse/ui/navigation/BottomNavBar.kt

package com.nagpurpulse.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.*

@Composable
fun BottomNavBar(
    navController: NavController,
    onCreatePost: () -> Unit,
    onProfileClick: (() -> Unit)? = null,
    hasAlertBadge: Boolean = false,
    messageCount: Int = 0
) {
    val backstackEntry by navController.currentBackStackEntryAsState()
    val current = backstackEntry?.destination?.route

    val t = rememberInfiniteTransition(label = "nav")
    val alertPulse by t.animateFloat(
        1f, 1.45f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alert_pulse"
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier       = Modifier.navigationBarsPadding()
    ) {
        // ── Home ──────────────────────────────────────────────────────
        NavItem(
            currentRoute = current,
            route        = Screen.Home.route,
            filledIcon   = Icons.Filled.Home,
            outlineIcon  = Icons.Outlined.Home,
            label        = "Home",
            onClick      = {
                if (current != Screen.Home.route)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
            }
        )

        // ── Explore ───────────────────────────────────────────────────
        NavItem(
            currentRoute = current,
            route        = Screen.Explore.route,
            filledIcon   = Icons.Filled.Explore,
            outlineIcon  = Icons.Outlined.Explore,
            label        = "Explore",
            onClick      = {
                if (current != Screen.Explore.route)
                    navController.navigate(Screen.Explore.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
            }
        )

        // ── Centre FAB ────────────────────────────────────────────────
        NavigationBarItem(
            selected = false,
            onClick  = onCreatePost,
            icon = {
                var burst by remember { mutableStateOf(false) }
                val fabScale by animateFloatAsState(
                    if (burst) 0.82f else 1f,
                    spring(Spring.DampingRatioHighBouncy, Spring.StiffnessHigh),
                    label = "fab_scale",
                    finishedListener = { burst = false }
                )
                val glowPulse by t.animateFloat(
                    0.4f, 0.85f,
                    infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "glow"
                )
                Box(
                    modifier = Modifier.size(58.dp).scale(fabScale),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(OrangePrimary.copy(glowPulse * 0.38f), Color.Transparent)
                                )
                            )
                    )
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(Brush.radialGradient(listOf(OrangeLight, OrangePrimary)))
                            .pressScale {
                                burst = true
                                onCreatePost()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, "Post", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            },
            label = {
                Text(
                    "Post",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors  = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // ── Alerts ────────────────────────────────────────────────────
        NavigationBarItem(
            selected = current == Screen.Alerts.route,
            onClick  = {
                if (current != Screen.Alerts.route)
                    navController.navigate(Screen.Alerts.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
            },
            icon = {
                Box {
                    val tintColor by animateColorAsState(
                        if (current == Screen.Alerts.route)
                            OrangePrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        tween(200), label = "alert_tint"
                    )
                    Icon(
                        if (current == Screen.Alerts.route) Icons.Filled.Warning else Icons.Outlined.Warning,
                        "Alerts", tint = tintColor
                    )
                    if (hasAlertBadge) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(alertPulse)
                                .clip(CircleShape)
                                .background(RedAlert)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                        )
                    }
                }
            },
            label   = {
                Text(
                    "Alerts", style = MaterialTheme.typography.labelSmall,
                    color = if (current == Screen.Alerts.route)
                        OrangePrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors  = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // ── Profile ───────────────────────────────────────────────────
        NavItem(
            currentRoute = current,
            route        = Screen.Profile.route,
            filledIcon   = Icons.Filled.Person,
            outlineIcon  = Icons.Outlined.Person,
            label        = "Profile",
            onClick = {
                if (current != Screen.Profile.route) {
                    if (onProfileClick != null) {
                        onProfileClick()
                    } else {
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        )
    }
}

// ── Shared tab item ───────────────────────────────────────────────────────────
@Composable
private fun RowScope.NavItem(
    currentRoute: String?,
    route: String,
    filledIcon: ImageVector,
    outlineIcon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val isSelected = currentRoute == route
    val iconScale  by animateFloatAsState(
        if (isSelected) 1.12f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "${label}_scale"
    )
    val labelColor by animateColorAsState(
        if (isSelected)
            OrangePrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        tween(200), label = "${label}_color"
    )

    NavigationBarItem(
        selected = isSelected,
        onClick  = onClick,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.scale(iconScale)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                OrangePrimary.copy(alpha = 0.12f)
                            )
                    )
                }
                Icon(
                    if (isSelected) filledIcon else outlineIcon,
                    label,
                    tint = if (isSelected)
                        OrangePrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        label   = { Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor) },
        colors  = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
    )
}
