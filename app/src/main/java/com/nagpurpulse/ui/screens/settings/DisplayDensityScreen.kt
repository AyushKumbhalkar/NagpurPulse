//java/com/nagpurpulse/ui/screens/settings/DisplayDensityScreen.kt

package com.nagpurpulse.ui.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.preferences.DensityManager
import com.nagpurpulse.ui.preferences.FeedLayoutManager
import com.nagpurpulse.ui.theme.*

private val DENSITY_OPTIONS = listOf("compact", "comfortable", "spacious")
private val FEED_STYLE_OPTIONS = listOf("compact", "expanded")

private fun paddingFor(density: String): Dp = when (density) {
    "compact"  -> 10.dp
    "spacious" -> 22.dp
    else       -> 16.dp
}

private fun gapFor(density: String): Dp = when (density) {
    "compact"  -> 6.dp
    "spacious" -> 18.dp
    else       -> 12.dp
}

private fun densityDescription(density: String): String = when (density) {
    "compact"  -> "More posts per screen. For people who like to scan fast."
    "spacious" -> "Extra breathing room around every card."
    else       -> "The balanced default. Comfortable to read and scroll."
}

@Composable
fun DisplayDensityScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptic = rememberHaptic()

    LaunchedEffect(uiState.settingsMessage) {
        uiState.settingsMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissSettingsMessage()
        }
    }

    var selected by remember { mutableStateOf(DensityManager.density) }
    var selectedFeedStyle by remember { mutableStateOf(FeedLayoutManager.feedStyle) }
    LaunchedEffect(DensityManager.density) { selected = DensityManager.density }
    LaunchedEffect(FeedLayoutManager.feedStyle) { selectedFeedStyle = FeedLayoutManager.feedStyle }

    val cardPadding by animateDpAsState(
        targetValue = paddingFor(selected),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "density_padding"
    )
    val cardGap by animateDpAsState(
        targetValue = gapFor(selected),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "density_gap"
    )

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SettingsTopBar(
                title = "Display Density",
                subtitle = "Shape how your feed feels",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    SectionHeader("LIVE PREVIEW")
                    Column(verticalArrangement = Arrangement.spacedBy(cardGap)) {
                        PreviewRow("Metro Phase 2 ahead of schedule", "Sitabuldi station work wraps up early.", cardPadding)
                        PreviewRow("Fresh poha at Gandhibagh", "Locals are calling it the best breakfast in town.", cardPadding)
                        PreviewRow("Rain alert for the evening", "Light showers expected after 6 PM.", cardPadding)
                    }
                }
            }

            item {
                Column {
                    SectionHeader("SPACING")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DENSITY_OPTIONS.forEach { density ->
                            ChoiceCard(
                                title = density.replaceFirstChar { it.uppercase() },
                                description = densityDescription(density),
                                selected = selected == density
                            ) {
                                if (selected != density) {
                                    haptic.selection()
                                    selected = density
                                    viewModel.updateDisplayDensity(density)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    SectionHeader("FEED STYLE")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FEED_STYLE_OPTIONS.forEach { style ->
                            ChoiceCard(
                                title = if (style == "compact") "Compact" else "Expanded",
                                description = if (style == "compact")
                                    "Tight rows. Great for catching up quickly."
                                else "Roomy cards with more detail per post.",
                                selected = selectedFeedStyle == style,
                                leading = { FeedStyleThumb(style) }
                            ) {
                                if (selectedFeedStyle != style) {
                                    haptic.selection()
                                    selectedFeedStyle = style
                                    viewModel.updateFeedStyle(style)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Changes apply instantly and sync to your account.",
                    color = TertiaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(title: String, body: String, padding: Dp) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(OrangePrimary.copy(0.15f)),
            Alignment.Center
        ) {
            Text("N", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                body,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A tiny drawing of what each feed style looks like. */
@Composable
private fun FeedStyleThumb(style: String) {
    val bar = TertiaryText.copy(alpha = 0.35f)
    Column(
        Modifier
            .size(width = 56.dp, height = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceAlt)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (style == "expanded") {
            Box(Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp)).background(OrangePrimary.copy(0.35f)))
            Box(Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(bar))
            Box(Modifier.fillMaxWidth(0.55f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(bar))
        } else {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(OrangePrimary.copy(0.35f)))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(bar))
                }
            }
        }
    }
}
