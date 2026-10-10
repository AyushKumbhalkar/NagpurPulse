//java/com/nagpurpulse/ui/screens/thread/CreateThreadComponents.kt

package com.nagpurpulse.ui.screens.thread

import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.nagpurpulse.data.model.formatMediaDuration
import com.nagpurpulse.ui.components.AdaptivePostImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement.spacedBy
import coil.compose.AsyncImage
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.shimmerEffect
import kotlinx.coroutines.delay

// ═════════════════════════════════════════════════════════════════════════════
//  Shared helpers
// ═════════════════════════════════════════════════════════════════════════════

/** Theme-aware hairline colour (the old BorderGray was dark-mode only). */
@Composable
internal fun hairline(): Color = MaterialTheme.colorScheme.outlineVariant

/** Small human-friendly number, e.g. 1,284 → "1.2k". */
internal fun compactCount(n: Int): String = when {
    n >= 10_000 -> "${n / 1000}k"
    n >= 1_000  -> String.format("%.1fk", n / 1000f)
    else        -> n.toString()
}

/** Distance in km as a number (formatting happens at display time). */
internal fun distanceKm(
    userLat: Double,
    userLng: Double,
    areaLat: Double,
    areaLng: Double
): Float {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(userLat, userLng, areaLat, areaLng, results)
    return results[0] / 1000f
}

internal fun formatDistance(km: Float): String =
    if (km < 1f) "Under 1 km away" else String.format("%.1f km away", km)

// ═════════════════════════════════════════════════════════════════════════════
//  Placeholder: typewriter prompt with a smooth blinking cursor
//  (isolated so its animation never recomposes the whole screen)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun TypewriterPlaceholder(
    prompts: List<String>,
    color: Color,
    showCursor: Boolean,
    fontSize: TextUnit = 18.sp
) {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(prompts) {
        if (prompts.isEmpty()) return@LaunchedEffect
        var index = prompts.indices.random()
        while (true) {
            val target = prompts[index]
            for (i in 1..target.length) {
                text = target.substring(0, i)
                delay((35..65).random().toLong())
            }
            delay(1800)
            for (i in target.length - 1 downTo 0) {
                text = target.substring(0, i)
                delay((15..30).random().toLong())
            }
            delay(250)
            if (prompts.size > 1) {
                index = prompts.indices.filter { it != index }.random()
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursor_alpha"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (showCursor) {
            Box(
                Modifier
                    .padding(start = 2.dp)
                    .width(2.dp)
                    .height(fontSize.value.dp)
                    .alpha(cursorAlpha)
                    .background(color)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Header inside the composer card — who is posting
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun ComposerHeader(
    displayName: String,
    avatarUrl: String?,
    isAnonymous: Boolean,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val avatarBg by animateColorAsState(
            if (isAnonymous) OrangeMain.copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceVariant,
            label = "avatar_bg"
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isAnonymous,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "avatar_content"
            ) { anon ->
                when {
                    anon -> Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Posting anonymously",
                        tint = OrangeMain,
                        modifier = Modifier.size(18.dp)
                    )
                    !avatarUrl.isNullOrBlank() -> AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Your profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp)
                    )
                    else -> Text(
                        displayName.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = if (isAnonymous) "Anonymous" else displayName.ifBlank { "You" },
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isAnonymous) "Your name is hidden" else "Posting publicly",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        trailing()
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Post-quality ring + Post button
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun QualityRing(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(450),
        label = "quality_progress"
    )
    val track = hairline()
    val complete = progress >= 0.999f

    Box(
        modifier = modifier
            .size(30.dp)
            .semantics { contentDescription = "Post completeness ${(progress * 100).toInt()} percent" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
            )
            drawArc(
                brush = OrangeGradient,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        if (complete) {
            Icon(
                Icons.Filled.Check, contentDescription = null,
                tint = OrangeMain, modifier = Modifier.size(14.dp)
            )
        }
    }
}

enum class PostButtonState { Disabled, Ready, Loading, Success }

@Composable
internal fun PostButton(
    state: PostButtonState,
    label: String,
    shakeOffset: Float,
    onClick: () -> Unit
) {
    val ready = state == PostButtonState.Ready
    val active = state != PostButtonState.Disabled

    val scale by animateFloatAsState(
        if (active) 1f else 0.95f,
        spring(Spring.DampingRatioMediumBouncy), label = "post_scale"
    )
    val disabledBrush = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
    )
    val shape = RoundedCornerShape(50.dp)

    Box(
        modifier = Modifier
            .graphicsLayer { translationX = shakeOffset }
            .scale(scale)
            .shadow(
                elevation = if (ready) 10.dp else 0.dp,
                shape = shape,
                ambientColor = OrangeMain.copy(alpha = 0.35f),
                spotColor = OrangeMain.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(if (active) OrangeGradient else disabledBrush)
            .pressScale(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "post_btn_content"
        ) { s ->
            when (s) {
                PostButtonState.Loading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                PostButtonState.Success -> Icon(
                    Icons.Filled.Check, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                else -> Text(
                    label,
                    color = if (s == PostButtonState.Disabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Draft restore banner
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun DraftBanner(
    preview: String,
    onRestore: () -> Unit,
    onDiscard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(OrangeMain.copy(alpha = 0.10f))
            .border(1.dp, OrangeMain.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Continue your draft?",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                preview,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "Discard",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .pressScale(onClick = onDiscard)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
        Text(
            "Restore",
            color = OrangeMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .pressScale(onClick = onRestore)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Social proof + trending strip
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun TrendingStrip(
    postsToday: Int?,
    trending: List<String>,
    onTrendingClick: (String) -> Unit
) {
    if (postsToday == null && trending.isEmpty()) return

    Column(Modifier.fillMaxWidth()) {
        if (postsToday != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (postsToday <= 0) "Be the first to post in Nagpur today"
                    else if (postsToday == 1) "1 neighbour posted today"
                    else "${compactCount(postsToday)} neighbours posted today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (trending.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = spacedBy(8.dp)) {
                item {
                    Text(
                        "Trending",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 7.dp, end = 2.dp)
                    )
                }
                items(trending, key = { it }) { cat ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .pressScale { onTrendingClick(cat) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            categoryChipIcon(cat), contentDescription = null,
                            tint = OrangeMain, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            categoryChipLabel(cat),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Starter chips under an empty title
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun StarterChips(onPick: (PostStarter) -> Unit) {
    LazyRow(horizontalArrangement = spacedBy(8.dp)) {
        items(postStarters, key = { it.label }) { starter ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .border(1.dp, hairline(), RoundedCornerShape(50.dp))
                    .pressScale { onPick(starter) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(starter.emoji, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    starter.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Category chips (top few + "More") with auto-scroll to the selection
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun CategoryChipRow(
    selected: String,
    onSelect: (String) -> Unit,
    onMore: () -> Unit
) {
    // A category picked from the sheet that isn't in the top list is pinned to the front.
    val visible = remember(selected) {
        if (selected in topPostCategories) topPostCategories
        else listOf(selected) + topPostCategories
    }
    val listState: LazyListState = rememberLazyListState()

    LaunchedEffect(selected) {
        val index = visible.indexOf(selected)
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyRow(
        state = listState,
        horizontalArrangement = spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp)
    ) {
        items(visible, key = { it }) { cat ->
            CategoryChip(
                label = categoryChipLabel(cat),
                icon = categoryChipIcon(cat),
                selected = cat == selected,
                onClick = { onSelect(cat) }
            )
        }
        item(key = "more") {
            CategoryChip(
                label = "More",
                icon = Icons.Filled.MoreHoriz,
                selected = false,
                onClick = onMore
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(36.dp)
            .shadow(
                elevation = if (selected) 4.dp else 0.dp,
                shape = shape,
                ambientColor = OrangeMain.copy(alpha = 0.15f),
                spotColor = OrangeMain.copy(alpha = 0.25f)
            )
            .clip(shape)
            .background(
                if (selected) OrangeGradient
                else Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                )
            )
            .border(1.dp, if (selected) OrangeMain else hairline(), shape)
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** Tappable "Looks like Food · Apply" suggestion. */
@Composable
internal fun CategorySuggestion(
    suggested: String,
    visible: Boolean,
    onApply: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(OrangeMain.copy(alpha = 0.12f))
                .border(1.dp, OrangeMain.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                .pressScale(onClick = onApply)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                categoryChipIcon(suggested), contentDescription = null,
                tint = OrangeMain, modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Looks like ${categoryChipLabel(suggested)}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "  ·  Apply",
                color = OrangeMain,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategorySheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            "Choose a topic",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.heightIn(max = 520.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
            ),
            verticalArrangement = spacedBy(4.dp)
        ) {
            val rows = postCategories.chunked(2)
            items(rows, key = { it.first() }) { pair ->
                Row(horizontalArrangement = spacedBy(8.dp)) {
                    pair.forEach { cat ->
                        SheetOption(
                            modifier = Modifier.weight(1f),
                            icon = categoryChipIcon(cat),
                            label = categoryChipLabel(cat),
                            subtitle = null,
                            selected = cat == selected,
                            onClick = { onSelect(cat) }
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Area selection
// ═════════════════════════════════════════════════════════════════════════════
data class AreaOption(
    val name: String,
    val subtitle: String,
    val isCurrent: Boolean = false,
    val isCity: Boolean = false
)

@Composable
internal fun AreaPills(
    options: List<AreaOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onMore: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        val index = options.indexOfFirst { it.name == selected }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyRow(state = listState, horizontalArrangement = spacedBy(8.dp)) {
        items(options, key = { it.name }) { option ->
            val isSelected = option.name == selected
            val shape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (isSelected) OrangeMain.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(1.dp, if (isSelected) OrangeMain else hairline(), shape)
                    .pressScale { onSelect(option.name) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        option.isCity    -> Icons.Filled.Public
                        option.isCurrent -> Icons.Filled.MyLocation
                        else             -> Icons.Filled.Place
                    },
                    contentDescription = null,
                    tint = if (isSelected) OrangeMain else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            option.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        if (option.isCurrent) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "NOW",
                                color = OrangeMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OrangeMain.copy(alpha = 0.14f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        option.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
        item(key = "more_areas") {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, hairline(), RoundedCornerShape(14.dp))
                    .pressScale(onClick = onMore)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.MoreHoriz, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "All areas",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AreaSheet(
    options: List<AreaOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            "Where is this about?",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.heightIn(max = 560.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = spacedBy(4.dp)
        ) {
            items(options, key = { it.name }) { option ->
                SheetOption(
                    modifier = Modifier.fillMaxWidth(),
                    icon = when {
                        option.isCity    -> Icons.Filled.Public
                        option.isCurrent -> Icons.Filled.MyLocation
                        else             -> Icons.Filled.Place
                    },
                    label = option.name,
                    subtitle = option.subtitle,
                    selected = option.name == selected,
                    onClick = { onSelect(option.name) }
                )
            }
        }
    }
}

@Composable
private fun SheetOption(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) OrangeMain.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (selected) OrangeMain else hairline(), shape)
            .pressScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (selected) OrangeMain else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Filled.Check, contentDescription = "Selected",
                tint = OrangeMain, modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Photos
// ═════════════════════════════════════════════════════════════════════════════
/**
 * What the author is about to post, shown the way the feed will draw it (3:4 .. 16:9, centre-cropped beyond that).
 * [framedPreview] is the bitmap produced by the frame editor; without it the raw file / existing URL is shown.
 * [onEditFrame] == null hides the frame button (e.g. when editing a post that already has uploaded media).
 */
@Composable
internal fun AttachedMediaPreview(
    model: Any,
    framedPreview: Bitmap?,
    frame: MediaFrame,
    isVideo: Boolean,
    durationMs: Long,
    busyLabel: String?,
    onEditFrame: (() -> Unit)?,
    onRemove: () -> Unit,
    onReplace: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (framedPreview != null) {
            val ratio = (framedPreview.width.toFloat() / framedPreview.height).coerceIn(0.75f, 1.78f)
            Image(
                bitmap = remember(framedPreview) { framedPreview.asImageBitmap() },
                contentDescription = "Photo attached to your post",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio)
            )
        } else {
            AdaptivePostImage(
                imageUrl = model.toString(),
                cornerRadius = 0.dp,
                showExpandHintWhenCropped = false
            )
        }

        // Scrim so the controls stay readable on bright photos
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent))
                )
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .pressScale(onClick = onReplace)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.SwapHoriz, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text("Replace", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .pressScale(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close, contentDescription = if (isVideo) "Remove video" else "Remove photo",
                tint = Color.White, modifier = Modifier.size(16.dp)
            )
        }

        // Bottom row: how it will look + frame button
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    buildString {
                        append("Feed preview")
                        if (framedPreview != null) append(" · ").append(frame.ratio.label)
                        if (isVideo && durationMs > 0L) append(" · ").append(formatMediaDuration(durationMs))
                    },
                    color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.weight(1f))
            if (onEditFrame != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(OrangeMain)
                        .pressScale(onClick = onEditFrame)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Crop, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (isVideo) "Frame & cover" else "Frame", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (busyLabel != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(busyLabel, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Recent photos row with three states: loading skeleton, permission prompt, and thumbnails.
 * [images] == null means "still loading".
 */
@Composable
internal fun RecentPhotosSection(
    hasPermission: Boolean,
    images: List<RecentMedia>?,
    selectedUri: Uri?,
    onRequestPermission: () -> Unit,
    onBrowse: () -> Unit,
    onToggle: (Uri) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.PhotoLibrary, contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Recent photos & videos",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Browse all",
                color = OrangeMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .pressScale(onClick = onBrowse)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            !hasPermission -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, hairline(), RoundedCornerShape(14.dp))
                    .pressScale(onClick = onRequestPermission)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.PhotoLibrary, contentDescription = null,
                    tint = OrangeMain, modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Show your recent photos & videos here",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Allow access for one-tap attaching",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                Text("Allow", color = OrangeMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            images == null -> LazyRow(horizontalArrangement = spacedBy(8.dp)) {
                items(6) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shimmerEffect(RoundedCornerShape(14.dp))
                    )
                }
            }

            images.isEmpty() -> Text(
                "No photos or videos found on this device",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )

            else -> LazyRow(horizontalArrangement = spacedBy(8.dp)) {
                items(images, key = { it.uri.toString() }) { item ->
                    val uri = item.uri
                    val isSelected = selectedUri == uri
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) OrangeMain else hairline(),
                                RoundedCornerShape(14.dp)
                            )
                            .pressScale { onToggle(uri) }
                    ) {
                        AsyncImage(
                            model = if (item.isVideo) {
                                ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .decoderFactory(VideoFrameDecoder.Factory())
                                    .build()
                            } else uri,
                            contentDescription = (if (item.isVideo) "Recent video" else "Recent photo") +
                                    (if (isSelected) ", selected, tap to remove" else ", tap to attach"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (item.isVideo) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                if (item.durationMs > 0L) {
                                    Text(formatMediaDuration(item.durationMs), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (isSelected) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(OrangeMain),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Anonymous card + reassurance banner
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun AnonymousCard(
    isAnonymous: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val accent by animateColorAsState(
        if (isAnonymous) OrangeMain else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "anon_accent"
    )
    val iconScale by animateFloatAsState(
        if (isAnonymous) 1.15f else 1f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "anon_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isAnonymous) OrangeMain.copy(alpha = 0.5f) else hairline()),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(pressedScale = 0.99f) { onToggle(!isAnonymous) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAnonymous) Icons.Filled.Lock
                    else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(iconScale)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Post anonymously",
                        color = accent,
                        fontWeight = if (isAnonymous) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Your name stays hidden",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                androidx.compose.material3.Switch(
                    checked = isAnonymous,
                    onCheckedChange = onToggle,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OrangeMain,
                        checkedBorderColor = OrangeMain,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = hairline()
                    )
                )
            }
            AnimatedVisibility(
                visible = isAnonymous,
                enter = fadeIn(tween(200)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrangeMain.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock, contentDescription = null,
                        tint = OrangeMain, modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Your name and photo won't be shown on this post.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Bottom action bar (pinned above the keyboard)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun ToolButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        if (active) OrangeMain.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        label = "tool_bg"
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .pressScale(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (active) OrangeMain else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

private val quickEmojis = listOf("😀", "🔥", "🙏", "📍", "🚗", "🍜", "🌧️", "🎉")

@Composable
internal fun QuickEmojiRow(visible: Boolean, onPick: (String) -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + expandVertically(),
        exit = fadeOut(tween(120)) + shrinkVertically()
    ) {
        LazyRow(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = spacedBy(6.dp)
        ) {
            items(quickEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale { onPick(emoji) }
                        .semantics { contentDescription = "Insert $emoji" },
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 18.sp) }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Success moment
// ═════════════════════════════════════════════════════════════════════════════
@Composable
internal fun SuccessOverlay(
    visible: Boolean,
    isEdit: Boolean,
    streak: Int
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
        ) {
            val pop = remember { Animatable(0.4f) }
            LaunchedEffect(visible) {
                if (visible) pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .scale(pop.value)
                        .clip(CircleShape)
                        .background(OrangeGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    if (isEdit) "Post updated" else "Your post is live!",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                if (!isEdit) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "+5 karma  ·  Thanks for helping Nagpur stay informed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    if (streak >= 2) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "🔥 $streak-day posting streak",
                            color = OrangeMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(OrangeMain.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
