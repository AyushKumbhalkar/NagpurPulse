package com.nagpurpulse.ui.screens.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.rememberHaptic
import com.nagpurpulse.ui.preferences.PreferenceManager
import com.nagpurpulse.ui.theme.*

private val TEXT_SIZE_OPTIONS = listOf("small", "medium", "large", "extra_large")

private fun textScaleFor(size: String): Float = when (size) {
    "small"       -> 0.90f
    "large"       -> 1.10f
    "extra_large" -> 1.20f
    else          -> 1.00f
}

private fun textSizeTitle(size: String): String = when (size) {
    "small"       -> "Small"
    "large"       -> "Large"
    "extra_large" -> "Extra large"
    else          -> "Medium"
}

private fun textSizeHint(size: String): String = when (size) {
    "small"       -> "Fits more on screen. Best if you like a compact, dense feed."
    "large"       -> "Easier on the eyes, great for reading long threads."
    "extra_large" -> "Maximum comfort. Everything is big and easy to tap."
    else          -> "The balanced default. Looks right on most phones."
}

@Composable
fun TextSizeScreen(
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

    var selected by remember { mutableStateOf(PreferenceManager.textSize) }
    LaunchedEffect(uiState.textSize) { selected = uiState.textSize }

    // The preview grows and shrinks smoothly so you can feel the difference.
    val previewScale by animateFloatAsState(
        targetValue = textScaleFor(selected),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "text_preview_scale"
    )

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SettingsTopBar(
                title = "Text Size",
                subtitle = "Make reading comfortable",
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
                    TextPreviewCard(previewScale)
                }
            }

            item {
                Column {
                    SectionHeader("CHOOSE A SIZE")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TEXT_SIZE_OPTIONS.forEach { size ->
                            SizeStep(
                                size = size,
                                selected = selected == size,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (selected != size) {
                                    haptic.selection()
                                    selected = size
                                    viewModel.updateTextSize(size)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        textSizeTitle(selected),
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        textSizeHint(selected),
                        color = SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                Text(
                    "Your choice syncs to your account, so it follows you to any device.",
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
private fun SizeStep(size: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val glyphSize = when (size) {
        "small" -> 14.sp
        "large" -> 22.sp
        "extra_large" -> 26.sp
        else -> 18.sp
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) OrangePrimary.copy(0.14f) else Surface)
            .border(1.5.dp, if (selected) OrangePrimary else Divider, RoundedCornerShape(14.dp))
            .pressScale(0.95f, onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Aa",
            color = if (selected) OrangePrimary else PrimaryText,
            fontSize = glyphSize,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            textSizeTitle(size).replace("Extra large", "XL"),
            color = if (selected) OrangePrimary else TertiaryText,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun TextPreviewCard(scale: Float) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, Divider, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(OrangePrimary.copy(0.15f)),
                Alignment.Center
            ) {
                Text("N", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Nagpur Pulse", color = PrimaryText, fontWeight = FontWeight.SemiBold, fontSize = (13f * scale).sp)
                Text("2h ago · Dharampeth", color = TertiaryText, fontSize = (11f * scale).sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Orange City Mela returns this weekend",
            color = PrimaryText,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * scale).sp,
            lineHeight = (24f * scale).sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Roads near Deekshabhoomi will be partly closed from 4 PM. Here's what to know before you head out.",
            color = SecondaryText,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("▲ 128", OrangePrimary)
            StatusPill("💬 24", BlueInfo)
        }
    }
}
