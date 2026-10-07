//java/com/nagpurpulse/ui/components/AnimationUtils.kt

package com.nagpurpulse.ui.components

import com.nagpurpulse.ui.theme.*
import com.nagpurpulse.ui.preferences.DensityManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nagpurpulse.ui.theme.SurfaceTwo
import com.nagpurpulse.ui.theme.SurfaceThree

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

fun Modifier.pressScale(
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "press_scale"
    )
    val click = onClick
    this
        .scale(scale)
        .semantics(mergeDescendants = true) {
            role = Role.Button
            onClick(label = null) { click(); true }
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap   = { onClick() }
            )
        }
}

// ── Shimmer ───────────────────────────────────────────────────────────────────
fun Modifier.shimmerEffect(
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
): Modifier = composed {

    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors =
        if (LocalIsDarkTheme.current) {
            listOf(
                SurfaceTwo,
                SurfaceThree,
                SurfaceTwo
            )
        } else {
            listOf(
                Color(0xFFF2F2F2),
                Color(0xFFFFFFFF),
                Color(0xFFF2F2F2)
            )
        }

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 400f, 0f),
            end = Offset(translateAnim, 0f)
        ),
        shape = shape
    )
}

// ── Shimmer PostCard placeholder ──────────────────────────────────────────────
@Composable
fun ShimmerPostCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(
    DensityManager.cardPadding.dp
)
    ) {
        Box(Modifier.width(72.dp).height(20.dp).shimmerEffect(RoundedCornerShape(20.dp)))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(17.dp).shimmerEffect())
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth(0.72f).height(17.dp).shimmerEffect())
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(130.dp).height(12.dp).shimmerEffect())
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).shimmerEffect())
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth(0.55f).height(12.dp).shimmerEffect())
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(72.dp).height(30.dp).shimmerEffect(RoundedCornerShape(15.dp)))
            Box(Modifier.width(38.dp).height(30.dp).shimmerEffect(RoundedCornerShape(15.dp)))
            Box(Modifier.width(62.dp).height(30.dp).shimmerEffect(RoundedCornerShape(15.dp)))
        }
    }
}

// ── Staggered entrance ────────────────────────────────────────────────────────
@Composable
fun StaggeredItem(
    index: Int,
    content: @Composable () -> Unit
) {
    if (index < 3) {

        var visible by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(250)
            )
        ) {
            content()
        }

    } else {
        content()
    }
}

// ── Pulse scale ───────────────────────────────────────────────────────────────
@Composable
fun rememberPulseScale(): Float {
    val t = rememberInfiniteTransition(label = "pulse")
    return t.animateFloat(
        initialValue = 1f,
        targetValue  = 1.22f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    ).value
}

// ── Upvote burst ──────────────────────────────────────────────────────────────
@Composable
fun rememberUpvoteScale(active: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue  = if (active) 1.35f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh),
        label = "upvote_burst"
    )
    return scale
}

// ── Slide in from bottom ──────────────────────────────────────────────────────
@Composable
fun SlideInFromBottom(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight / 2 }
        ) + fadeOut()
    ) {
        content()
    }
}

// ── Fade-scale entrance ───────────────────────────────────────────────────────
@Composable
fun FadeScaleIn(visible: Boolean, delayMs: Int = 0, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350, delayMs)) + scaleIn(
            initialScale  = 0.88f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ),
        exit = fadeOut() + scaleOut(targetScale = 0.88f)
    ) { content() }
}
