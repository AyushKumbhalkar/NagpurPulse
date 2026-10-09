// java/com/nagpurpulse/ui/screens/profile/ProfileComponents.kt
//
// Building blocks for ProfileScreen. Kept separate so the screen file stays readable.
// Logic (levels, streaks, milestones...) lives in ProfileGamification.kt.

package com.nagpurpulse.ui.screens.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nagpurpulse.data.model.KarmaRank
import com.nagpurpulse.ui.components.formatCount
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.components.shimmerEffect
import com.nagpurpulse.ui.theme.GreenSuccess
import com.nagpurpulse.ui.theme.OrangeLight
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.RedAlert
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ── Design tokens (one radius / spacing scale for the whole screen) ───────────

internal object ProfileDims {
    val radiusCard: Dp = 20.dp
    val radiusInner: Dp = 12.dp
    val spaceXs: Dp = 4.dp
    val spaceS: Dp = 8.dp
    val spaceM: Dp = 12.dp
    val spaceL: Dp = 16.dp
    val spaceXl: Dp = 24.dp
    val bannerHeight: Dp = 132.dp
    val avatarBox: Dp = 140.dp
    val minTouch: Dp = 48.dp
}

internal fun Long.asColor(): Color = Color(this)

internal fun ProfileLevel.brush(): Brush =
    Brush.linearGradient(listOf(colorStart.asColor(), colorEnd.asColor()))

// ── Generic card ──────────────────────────────────────────────────────────────

@Composable
internal fun ProfileCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tint: Color? = null,
    contentPadding: PaddingValues = PaddingValues(ProfileDims.spaceL),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(ProfileDims.radiusCard)
    val base = if (onClick != null) modifier.pressScale(pressedScale = 0.98f, onClick = onClick) else modifier
    Column(
        modifier = base
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (tint != null) Modifier.background(tint.copy(alpha = 0.08f)) else Modifier)
            .border(
                0.5.dp,
                tint?.copy(alpha = 0.35f) ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
internal fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

// ── Banner ────────────────────────────────────────────────────────────────────

@Composable
internal fun ProfileBanner(coverUrl: String?, level: ProfileLevel, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colorScheme.background
    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(
                    level.colorStart.asColor().copy(alpha = 0.55f),
                    level.colorEnd.asColor().copy(alpha = 0.30f),
                    bg
                )
            )
        )
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Fade into the page background so the avatar overlap looks seamless.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.9f))))
        )
    }
}

// ── Avatar with completeness ring ─────────────────────────────────────────────

@Composable
internal fun ProfileAvatar(
    avatarUrl: String?,
    initial: String,
    level: ProfileLevel,
    completeness: Completeness,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val a = level.colorStart.asColor()
    val b = level.colorEnd.asColor()
    val track = MaterialTheme.colorScheme.outlineVariant
    val pageBg = MaterialTheme.colorScheme.background

    val glow by rememberInfiniteTransition(label = "avatar_glow").animateFloat(
        initialValue = 0.10f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "avatar_glow_alpha"
    )
    val ringProgress by animateFloatAsState(
        targetValue = completeness.fraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "completeness_ring"
    )

    val ringSize = 124.dp
    val ringStroke = 4.dp
    val photoSize = ringSize - 20.dp

    Box(
        modifier = modifier
            .size(ProfileDims.avatarBox)
            .semantics { contentDescription = "Profile photo, profile ${completeness.percent} percent complete" },
        contentAlignment = Alignment.Center
    ) {
        // Soft pulsing glow
        Box(
            Modifier
                .size(ringSize + 14.dp)
                .clip(CircleShape)
                .background(a.copy(alpha = glow))
        )
        // Page-coloured disc so the avatar looks "cut out" of the banner
        Box(
            Modifier
                .size(ringSize)
                .clip(CircleShape)
                .background(pageBg)
        )
        // Completeness ring (full + gradient when the profile is complete)
        Canvas(Modifier.size(ringSize)) {
            val strokePx = ringStroke.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            drawArc(
                color = track.copy(alpha = 0.6f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
            if (ringProgress > 0f) {
                drawArc(
                    brush = Brush.linearGradient(listOf(a, b), start = Offset.Zero, end = Offset(size.width, size.height)),
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        // Photo (initials show underneath while it loads or if there is no photo)
        Box(
            Modifier
                .size(photoSize)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(a, b))),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        // Completeness percentage chip
        if (!completeness.isComplete) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 10.dp, y = (-14).dp)
                    .defaultMinSize(minHeight = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OrangePrimary)
                    .border(2.dp, pageBg, RoundedCornerShape(50))
                    .pressScale(onClick = onEdit)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${completeness.percent}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Edit button: 48dp touch target around a 32dp visual
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-2).dp, y = (-2).dp)
                .size(ProfileDims.minTouch)
                .pressScale(onClick = onEdit),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OrangePrimary)
                    .border(2.dp, pageBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit profile photo",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
internal fun VerifiedTick(modifier: Modifier = Modifier) {
    val a by rememberInfiniteTransition(label = "verified").animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "verified_alpha"
    )
    Icon(
        Icons.Filled.CheckCircle,
        contentDescription = "Verified",
        tint = OrangePrimary,
        modifier = modifier
            .size(20.dp)
            .alpha(a)
    )
}

// ── Chips ─────────────────────────────────────────────────────────────────────

@Composable
internal fun ProfilePill(
    text: String,
    modifier: Modifier = Modifier,
    leadingEmoji: String? = null,
    tint: Color = OrangePrimary,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(50)
    val base = if (onClick != null) modifier.pressScale(onClick = onClick) else modifier
    Row(
        modifier = base
            .defaultMinSize(minHeight = 32.dp)
            .clip(shape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingEmoji != null) {
            Text(leadingEmoji, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun StreakChip(streak: StreakInfo, onClick: () -> Unit) {
    when {
        streak.days <= 0 -> ProfilePill(
            text = "Start a streak",
            leadingEmoji = "🔥",
            tint = Color(0xFF8E8E93),
            onClick = onClick
        )
        streak.atRisk -> ProfilePill(
            text = "${streak.days}-day streak · post today",
            leadingEmoji = "🔥",
            tint = RedAlert,
            onClick = onClick
        )
        else -> ProfilePill(
            text = "${streak.days}-day streak",
            leadingEmoji = "🔥",
            tint = OrangePrimary,
            onClick = onClick
        )
    }
}

@Composable
internal fun ProfileAreaChip(area: String) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.LocationOn,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            area,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AreaChipFlow(areas: List<String>, modifier: Modifier = Modifier, maxShown: Int = 6) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        areas.take(maxShown).forEach { ProfileAreaChip(it) }
        if (areas.size > maxShown) {
            ProfilePill(text = "+${areas.size - maxShown}", tint = Color(0xFF8E8E93))
        }
    }
}

// ── Buttons ───────────────────────────────────────────────────────────────────

@Composable
internal fun PillButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    val tint = if (primary) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(if (primary) OrangePrimary.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (primary) OrangePrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                shape
            )
            .pressScale(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (primary) OrangePrimary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ── Stats card (connected, tappable) ──────────────────────────────────────────

@Composable
private fun AnimatedCountText(value: Int, animate: Boolean, color: Color) {
    var target by remember { mutableIntStateOf(if (animate) 0 else value) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(
        targetValue = target,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "stat_count"
    )
    Text(
        formatCount(shown),
        color = color,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.headlineSmall
    )
}

@Composable
private fun StatCell(
    value: Int,
    label: String,
    color: Color,
    animate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = ProfileDims.minTouch)
            .pressScale(pressedScale = 0.95f, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedCountText(value, animate, color)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun StatsCard(
    karma: Int,
    threads: Int,
    comments: Int,
    animate: Boolean,
    onKarma: () -> Unit,
    onThreads: () -> Unit,
    onComments: () -> Unit
) {
    ProfileCard(contentPadding = PaddingValues(horizontal = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatCell(karma, "Karma", OrangePrimary, animate, onKarma, Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
            StatCell(threads, "Threads", MaterialTheme.colorScheme.onSurface, animate, onThreads, Modifier.weight(1f))
            Box(Modifier.width(0.5.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
            StatCell(comments, "Comments", MaterialTheme.colorScheme.onSurface, animate, onComments, Modifier.weight(1f))
        }
    }
}

// ── Progress bar ──────────────────────────────────────────────────────────────

@Composable
internal fun GradientProgressBar(
    fraction: Float,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "progress_bar"
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(Brush.horizontalGradient(if (colors.size >= 2) colors else listOf(colors.first(), colors.first())))
            )
        }
    }
}

// ── Complete-profile card ─────────────────────────────────────────────────────

@Composable
internal fun CompleteProfileCard(completeness: Completeness, onClick: () -> Unit) {
    val next = completeness.next ?: return
    ProfileCard(onClick = onClick, tint = OrangePrimary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Complete your profile",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Next: ${next.label}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "${completeness.doneCount}/${completeness.total}",
                color = OrangePrimary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = OrangePrimary
            )
        }
        Spacer(Modifier.height(ProfileDims.spaceM))
        GradientProgressBar(completeness.fraction, listOf(OrangePrimary, OrangeLight))
    }
}

// ── Level card ────────────────────────────────────────────────────────────────

@Composable
internal fun LevelCard(progress: LevelProgress, karma: Int, onClick: () -> Unit) {
    val level = progress.level
    val colors = listOf(level.colorStart.asColor(), level.colorEnd.asColor())
    ProfileCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(level.brush()),
                contentAlignment = Alignment.Center
            ) { Text(level.emoji, fontSize = 22.sp) }
            Spacer(Modifier.width(ProfileDims.spaceM))
            Column(Modifier.weight(1f)) {
                Text(
                    "Level ${level.number} · ${level.title}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    if (progress.next != null) "$karma / ${progress.next.minKarma} karma" else "$karma karma",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View levels",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(ProfileDims.spaceM))
        GradientProgressBar(progress.fraction, colors)
        Spacer(Modifier.height(ProfileDims.spaceS))
        val next = progress.next
        Text(
            if (next != null) "${progress.remaining} karma to ${next.emoji} ${next.title} · unlocks ${next.unlock.lowercase()}"
            else "You've reached the top level 👑",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ── Next best action ──────────────────────────────────────────────────────────

@Composable
internal fun NextActionCard(action: NextAction, firstArea: String?, onClick: () -> Unit) {
    val (emoji, title, subtitle, cta) = when (action) {
        NextAction.FIRST_THREAD -> ActionCopy(
            "✍️", "Share your first thread",
            "Tell Nagpur what's happening around you.", "Create thread"
        )
        NextAction.SAVE_STREAK -> ActionCopy(
            "🔥", "Keep your streak alive",
            "Post or comment today so you don't lose your run.", "Post now"
        )
        NextAction.FIRST_COMMENT -> ActionCopy(
            "💬", "Join a conversation",
            "Reply to a thread and start earning karma.", "Browse threads"
        )
        NextAction.JOIN_TRENDING -> ActionCopy(
            "📈", "See what's trending",
            if (firstArea != null) "Jump into the buzz around $firstArea." else "Jump into today's hottest threads.",
            "Explore"
        )
    }
    ProfileCard(onClick = onClick, tint = OrangePrimary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(ProfileDims.spaceM))
            Column(Modifier.weight(1f)) {
                Text(
                    "Next up",
                    color = OrangePrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(ProfileDims.spaceS))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(OrangePrimary)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(cta, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

private data class ActionCopy(val emoji: String, val title: String, val subtitle: String, val cta: String)

// ── Impact card ───────────────────────────────────────────────────────────────

@Composable
private fun ImpactMetric(emoji: String, value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            formatCount(value),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
internal fun ImpactCard(
    impact: ImpactStats,
    deltaUpvotes: Int,
    deltaReplies: Int,
    rank: KarmaRank?
) {
    val useWeek = impact.weekThreads > 0
    val views = if (useWeek) impact.weekViews else impact.totalViews
    val upvotes = if (useWeek) impact.weekUpvotes else impact.totalUpvotes
    val replies = if (useWeek) impact.weekReplies else impact.totalReplies

    ProfileCard {
        SectionTitle(
            "Your impact",
            trailing = {
                Text(
                    if (useWeek) "Threads from the last 7 days" else "All time",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
        Spacer(Modifier.height(ProfileDims.spaceM))
        Row(Modifier.fillMaxWidth()) {
            ImpactMetric("👁", views, "Views", Modifier.weight(1f))
            ImpactMetric("▲", upvotes, "Upvotes", Modifier.weight(1f))
            ImpactMetric("💬", replies, "Replies", Modifier.weight(1f))
        }
        if (deltaUpvotes > 0 || deltaReplies > 0) {
            Spacer(Modifier.height(ProfileDims.spaceM))
            val parts = buildList {
                if (deltaUpvotes > 0) add("+$deltaUpvotes upvote${if (deltaUpvotes == 1) "" else "s"}")
                if (deltaReplies > 0) add("+$deltaReplies repl${if (deltaReplies == 1) "y" else "ies"}")
            }
            ProfilePill(
                text = parts.joinToString(" · ") + " since your last visit",
                leadingEmoji = "🎉",
                tint = GreenSuccess
            )
        }
        if (rank != null) {
            Spacer(Modifier.height(ProfileDims.spaceM))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(Modifier.height(ProfileDims.spaceM))
            Text(
                "🏙  Top ${rank.rankPercent}% in Nagpur",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            val areaPct = rank.areaRankPercent
            if (!rank.area.isNullOrBlank() && areaPct != null && (rank.areaUsers ?: 0) >= 10) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "📍  Top $areaPct% in ${rank.area}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// ── Badges & milestones ───────────────────────────────────────────────────────

/** UI model that unifies server-awarded badges and client-side milestones. */
internal data class BadgeTileUi(
    val id: String,
    val emoji: String,
    val title: String,
    val earned: Boolean,
    val fraction: Float,
    val caption: String,
    val hint: String
)

@Composable
private fun BadgeTile(item: BadgeTileUi, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(ProfileDims.radiusCard)
    val earnedTint = OrangePrimary
    Column(
        modifier = modifier
            .width(112.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                if (item.earned) 1.dp else 0.5.dp,
                if (item.earned) earnedTint.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape
            )
            .padding(12.dp)
            .semantics {
                contentDescription = if (item.earned) "${item.title}, unlocked" else "${item.title}, locked. ${item.hint}"
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.earned) Brush.linearGradient(listOf(OrangePrimary.copy(0.30f), OrangeLight.copy(0.12f)))
                        else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 24.sp, modifier = Modifier.alpha(if (item.earned) 1f else 0.35f))
            }
            if (!item.earned) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            color = if (item.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2
        )
        Spacer(Modifier.height(6.dp))
        if (item.earned) {
            Text(item.caption, color = OrangePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        } else {
            GradientProgressBar(item.fraction, listOf(OrangePrimary, OrangeLight), height = 4.dp)
            Spacer(Modifier.height(4.dp))
            Text(item.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun BadgesSection(items: List<BadgeTileUi>, onViewAll: () -> Unit) {
    val earned = items.count { it.earned }
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(
            "Badges & milestones",
            trailing = {
                Row(
                    Modifier
                        .defaultMinSize(minHeight = ProfileDims.minTouch)
                        .pressScale(onClick = onViewAll),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$earned/${items.size} · View all",
                        color = OrangePrimary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(ProfileDims.spaceS)
        ) {
            items.take(8).forEach { BadgeTile(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BadgesSheet(items: List<BadgeTileUi>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ProfileDims.spaceL)
                .navigationBarsPadding()
        ) {
            Text(
                "Badges & milestones",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "${items.count { it.earned }} of ${items.size} unlocked",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(ProfileDims.spaceL))
            items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.earned) OrangePrimary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.emoji, fontSize = 22.sp, modifier = Modifier.alpha(if (item.earned) 1f else 0.35f))
                    }
                    Spacer(Modifier.width(ProfileDims.spaceM))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            if (item.earned) "Unlocked" else item.hint,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!item.earned) {
                            Spacer(Modifier.height(6.dp))
                            GradientProgressBar(item.fraction, listOf(OrangePrimary, OrangeLight), height = 5.dp)
                        }
                    }
                    Spacer(Modifier.width(ProfileDims.spaceS))
                    if (item.earned) {
                        Icon(Icons.Filled.Check, contentDescription = "Unlocked", tint = GreenSuccess)
                    } else {
                        Text(item.caption, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(ProfileDims.spaceXl))
        }
    }
}

// ── Best thread ───────────────────────────────────────────────────────────────

@Composable
internal fun BestThreadCard(
    title: String,
    upvotes: Int,
    replies: Int,
    views: Int,
    onClick: () -> Unit
) {
    ProfileCard(onClick = onClick) {
        Text(
            "🏆 Your top thread",
            color = OrangePrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "▲ ${formatCount(upvotes)}   💬 ${formatCount(replies)}   👁 ${formatCount(views)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ── Activity heatmap ──────────────────────────────────────────────────────────

@Composable
internal fun ActivityHeatmapCard(grid: List<List<Int>>) {
    val total = grid.sumOf { week -> week.sumOf { it.coerceAtLeast(0) } }
    val empty = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val weeks = grid.size
    ProfileCard {
        SectionTitle(
            "Activity",
            trailing = {
                Text(
                    "$total in the last $weeks weeks",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
        Spacer(Modifier.height(ProfileDims.spaceM))
        val gapRatio = 0.25f
        val ratio = (weeks + (weeks - 1) * gapRatio) / (7 + 6 * gapRatio)
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .semantics { contentDescription = "Activity heatmap: $total contributions in the last $weeks weeks" }
        ) {
            val cell = size.width / (weeks + (weeks - 1) * gapRatio)
            val gap = cell * gapRatio
            grid.forEachIndexed { c, week ->
                week.forEachIndexed { r, count ->
                    if (count >= 0) {
                        val color = when {
                            count == 0 -> empty
                            count == 1 -> OrangePrimary.copy(alpha = 0.35f)
                            count == 2 -> OrangePrimary.copy(alpha = 0.60f)
                            count == 3 -> OrangePrimary.copy(alpha = 0.80f)
                            else -> OrangePrimary
                        }
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(c * (cell + gap), r * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(cell * 0.28f, cell * 0.28f)
                        )
                    }
                }
            }
        }
    }
}

// ── Empty state with call-to-action ───────────────────────────────────────────

@Composable
internal fun ProfileEmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 32.sp) }
        Spacer(Modifier.height(ProfileDims.spaceL))
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(ProfileDims.spaceL))
        Box(
            Modifier
                .defaultMinSize(minHeight = ProfileDims.minTouch)
                .clip(RoundedCornerShape(50))
                .background(OrangePrimary)
                .pressScale(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Count pill used in tabs ───────────────────────────────────────────────────

@Composable
internal fun CountPill(count: Int, selected: Boolean) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 22.dp, minHeight = 20.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) OrangePrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            formatCount(count),
            color = if (selected) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Top bar icon button (48dp touch target) ───────────────────────────────────

@Composable
internal fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Box(
        Modifier
            .size(ProfileDims.minTouch)
            .pressScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        if (badgeCount > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OrangePrimary)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (badgeCount > 9) "9+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── Skeleton ──────────────────────────────────────────────────────────────────

@Composable
internal fun ProfileSkeleton() {
    val pill = RoundedCornerShape(50)
    val card = RoundedCornerShape(ProfileDims.radiusCard)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth().height(ProfileDims.bannerHeight).shimmerEffect(RoundedCornerShape(0.dp)))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ProfileDims.spaceL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .offset(y = (-48).dp)
                    .size(104.dp)
                    .shimmerEffect(pill)
            )
            Column(
                Modifier.offset(y = (-32).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(180.dp).height(24.dp).shimmerEffect())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(110.dp).height(14.dp).shimmerEffect())
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(110.dp).height(32.dp).shimmerEffect(pill))
                    Box(Modifier.width(120.dp).height(32.dp).shimmerEffect(pill))
                }
            }
            Row(
                Modifier.offset(y = (-16).dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f).height(44.dp).shimmerEffect(pill))
                Box(Modifier.weight(1f).height(44.dp).shimmerEffect(pill))
            }
            Box(Modifier.fillMaxWidth().height(76.dp).shimmerEffect(card))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(110.dp).shimmerEffect(card))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(86.dp).shimmerEffect(card))
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
internal fun LevelsDialog(karma: Int, onDismiss: () -> Unit) {
    val current = ProfileLevels.forKarma(karma)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text("Levels & karma", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Karma goes up when people upvote your threads and like your comments, " +
                            "and down when your threads are downvoted.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(ProfileDims.spaceM))
                ProfileLevels.all.forEach { level ->
                    val isCurrent = level.number == current.number
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(level.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(ProfileDims.spaceM))
                        Column(Modifier.weight(1f)) {
                            Text(
                                level.title + if (isCurrent) "  · you" else "",
                                color = if (isCurrent) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${level.minKarma}+ karma · ${level.unlock}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it", color = OrangePrimary, fontWeight = FontWeight.SemiBold) }
        }
    )
}

@Composable
internal fun StreakDialog(streak: StreakInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("🔥 Streaks", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Post a thread or write a comment on any day to build your streak.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(ProfileDims.spaceS))
                Text(
                    "One rest day per week is forgiven, so a single missed day won't break your run.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(ProfileDims.spaceM))
                Text(
                    "Current: ${streak.days} day${if (streak.days == 1) "" else "s"}   ·   Best: ${streak.longest} day${if (streak.longest == 1) "" else "s"}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (streak.restDayUsed) {
                    Spacer(Modifier.height(ProfileDims.spaceXs))
                    Text(
                        "A rest day is keeping your streak alive right now.",
                        color = OrangePrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it", color = OrangePrimary, fontWeight = FontWeight.SemiBold) }
        }
    )
}

// ── Celebration (confetti + banner) ───────────────────────────────────────────

internal data class CelebrationMessage(val emoji: String, val title: String, val subtitle: String)

private data class Particle(
    val x: Float,
    val speed: Float,
    val sway: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
    val delay: Float
)

@Composable
private fun Confetti(modifier: Modifier = Modifier) {
    val palette = remember {
        listOf(
            OrangePrimary, OrangeLight, Color(0xFFFFD60A), Color(0xFF32D74B),
            Color(0xFF0A84FF), Color(0xFFFF453A), Color(0xFF7C3AED)
        )
    }
    val particles = remember {
        List(44) {
            Particle(
                x = Random.nextFloat(),
                speed = 0.6f + Random.nextFloat() * 0.6f,
                sway = 2f + Random.nextFloat() * 6f,
                size = 10f + Random.nextFloat() * 10f,
                color = palette.random(),
                spin = 180f + Random.nextFloat() * 540f,
                delay = Random.nextFloat() * 0.25f
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    Canvas(modifier) {
        val progress = t.value
        particles.forEach { p ->
            val local = ((progress - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local > 0f) {
                val x = p.x * size.width + sin(local * p.sway * PI.toFloat()) * 24f
                val y = -20f + local * size.height * 0.8f * p.speed
                val alpha = 1f - ((local - 0.7f) / 0.3f).coerceIn(0f, 1f)
                rotate(p.spin * local, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(x - p.size / 2f, y - p.size / 3f),
                        size = Size(p.size, p.size * 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CelebrationOverlay(message: CelebrationMessage?, onDismiss: () -> Unit) {
    if (message == null) return
    LaunchedEffect(message) {
        delay(3800)
        onDismiss()
    }
    Box(Modifier.fillMaxSize()) {
        Confetti(Modifier.fillMaxSize())
        ProfileCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = ProfileDims.spaceL, vertical = ProfileDims.spaceL),
            onClick = onDismiss,
            tint = OrangePrimary
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.emoji, fontSize = 32.sp)
                Spacer(Modifier.width(ProfileDims.spaceM))
                Column {
                    Text(
                        message.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        message.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
