
// java/com/nagpurpulse/ui/screens/onboarding/AreaSelectionScreen.kt
package com.nagpurpulse.ui.screens.onboarding

import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nagpurpulse.data.repository.AuthRepository
import com.nagpurpulse.data.repository.ProfileRepository
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.BackgroundDark
import com.nagpurpulse.ui.theme.SurfaceOne
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.TextPrimary
import com.nagpurpulse.ui.theme.TextSecondary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AreaSelectionViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _selectedAreas = MutableStateFlow<List<String>>(emptyList())
    val selectedAreas: StateFlow<List<String>> = _selectedAreas

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun toggleArea(area: String) {
        val current = _selectedAreas.value.toMutableList()
        if (current.contains(area)) current.remove(area)
        else if (current.size < 3) current.add(area)
        _selectedAreas.value = current
    }

    fun saveAreas(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId ?: return@launch
            _isLoading.value = true
            profileRepository.saveAreas(userId, _selectedAreas.value)
            _isLoading.value = false
            onSuccess()
        }
    }
}

val nagpurAreas = listOf(
    "Dharampeth", "Sitabuldi", "VNIT Area", "Sadar",
    "Pratap Nagar", "Trimurti Nagar", "Manish Nagar", "Wardha Road",
    "Byramji Town", "Civil Lines", "Ramdaspeth", "Bajaj Nagar"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaSelectionScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AreaSelectionViewModel = hiltViewModel()
) {
    val selectedAreas by viewModel.selectedAreas.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80); headerVisible = true
        delay(150); contentVisible = true
    }

    val filteredAreas = if (searchQuery.isEmpty()) nagpurAreas
    else nagpurAreas.filter { it.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(52.dp))

        // ── Header + progress ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { -30 })
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    Spacer(Modifier.weight(1f))
                    Text("Step 2 of 3", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(24.dp))
                }

                Spacer(Modifier.height(12.dp))

                // Animated progress bar
                val progressAnim by animateFloatAsState(
                    targetValue = 0.66f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label = "progress"
                )
                LinearProgressIndicator(
                    progress = { progressAnim },
                    modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                    color = OrangePrimary,
                    trackColor = Color(0xFF2A2A2A)
                )

                Spacer(Modifier.height(24.dp))

                Text("Where are you in Nagpur?", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("We'll show you the most relevant posts", color = TextSecondary, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { 40 })
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search your area...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangePrimary,
                    unfocusedBorderColor = Color(0xFF2A2A2A),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = OrangePrimary,
                    focusedContainerColor = SurfaceOne,
                    unfocusedContainerColor = SurfaceOne
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Area cards grid ────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(filteredAreas) { index, area ->
                val isSelected = selectedAreas.contains(area)

                // Staggered entrance
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(contentVisible.let { if (it) 0L else 200L } + index * 50L)
                    itemVisible = true
                }

                // Tap scale
                val itemScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.03f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "area_scale_$index"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) OrangePrimary else SurfaceOne,
                    animationSpec = tween(250),
                    label = "area_bg_$index"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) OrangePrimary else Color(0xFF2A2A2A),
                    animationSpec = tween(250),
                    label = "area_border_$index"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else TextPrimary,
                    animationSpec = tween(200),
                    label = "area_text_$index"
                )

                AnimatedVisibility(
                    visible = itemVisible,
                    enter = fadeIn(tween(300)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .scale(itemScale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleArea(area) }
                            .padding(
    DensityManager.cardPadding.dp
)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.LocationOn, null,
                                tint = if (isSelected) Color.White else OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = area,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            // Animated checkmark
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy)) + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Icon(Icons.Filled.CheckCircle, "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Counter row
        AnimatedContent(
            targetState = selectedAreas.size,
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "area_counter"
        ) { count ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                        append("$count/3")
                    }
                    withStyle(SpanStyle(color = TextSecondary, fontSize = 14.sp)) {
                        append(" areas selected")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(12.dp))

        // Continue button
        Button(
            onClick = { viewModel.saveAreas(onContinue) },
            modifier = Modifier.fillMaxWidth().height(52.dp).pressScale(onClick = { viewModel.saveAreas(onContinue) }),
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            shape = RoundedCornerShape(26.dp),
            enabled = !isLoading
        ) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "btn_content"
            ) { loading ->
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Saving...", color = Color.White, fontSize = 15.sp)
                    }
                } else {
                    Text("Continue  →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now", color = TextSecondary, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))
    }
}
