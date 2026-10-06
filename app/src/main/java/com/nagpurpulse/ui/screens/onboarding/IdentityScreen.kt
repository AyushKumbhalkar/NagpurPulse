// java/com/nagpurpulse/ui/screens/onboarding/IdentityScreen.kt

package com.nagpurpulse.ui.screens.onboarding


import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.*
import kotlinx.coroutines.delay

// ── Data ──────────────────────────────────────────────────────────────────────
data class GenderOption(
    val id: String,
    val label: String,
    val subtitle: String = "",
    val icon: ImageVector,
    val iconTint: Color,
    val bgTint: Color
)

private val genderOptions = listOf(
    GenderOption(
        id = "male",
        label = "Male",
        icon = Icons.Filled.Male,
        iconTint = Color(0xFFFF6B00),
        bgTint = Color(0xFFFFE5CC)
    ),
    GenderOption(
        id = "female",
        label = "Female",
        icon = Icons.Filled.Female,
        iconTint = Color(0xFFE91E8C),
        bgTint = Color(0xFFFFE4F0)
    ),
    GenderOption(
        id = "other",
        label = "Other",
        subtitle = "Non-binary / Prefer not to say",
        icon = Icons.Filled.Transgender,
        iconTint = Color(0xFF7C3AED),
        bgTint = Color(0xFFEDE9FE)
    ),
    GenderOption(
        id = "prefer_not",
        label = "Prefer not to say",
        subtitle = "I don't wish to disclose",
        icon = Icons.Filled.Lock,
        iconTint = Color(0xFF636366),
        bgTint = Color(0xFFEEEEEE)
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun IdentityScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
    initialGender: String? = null
) {
    var selectedGender by remember(initialGender) { mutableStateOf(initialGender) }
    var visible by remember { mutableStateOf(true) }

    val isDark = LocalIsDarkTheme.current
    val bgColor = if (isDark) BackgroundDark else BackgroundLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pressScale(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    "Back",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.weight(1f))
                // NagpurPulse brand mark
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, OrangePrimary.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.nagpurpulse_orange_n_icon),
                        contentDescription = "NagpurPulse",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Progress stepper ─────────────────────────────────────
            OnboardingProgressStepper(
                steps = listOf("Create Account", "Your Identity", "Choose Username", "Profile Picture"),
                currentStep = 1, // 0-based index; step 2 = index 1
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(8.dp))

            // ── Title ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                                append("Help us ")
                            }
                            withStyle(SpanStyle(color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                                append("personalize")
                            }
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                                append(" your experience")
                            }
                        },
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "This helps us show you relevant content and\ncreate a better experience.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Gender options ────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    genderOptions.forEach { option ->
                        GenderOptionCard(
                            option = option,
                            isSelected = selectedGender == option.id,
                            onClick = { selectedGender = option.id }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Privacy card ──────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrangeSubtle)
                        .border(1.dp, OrangePrimary.copy(0.20f), RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(OrangeGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Your privacy matters",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "We use this choice to tailor your avatar suggestions during setup. You can choose “Prefer not to say”.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Continue button ───────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = EnterTransition.None
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            if (selectedGender != null)
                                Brush.horizontalGradient(listOf(OrangePrimary, OrangeLight))
                            else
                                Brush.horizontalGradient(listOf(OrangePrimary.copy(0.4f), OrangeLight.copy(0.4f)))
                        )
                        .then(
                            if (selectedGender != null)
                                Modifier.pressScale(onClick = { onContinue(selectedGender!!) })
                            else
                                Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Continue →",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Gender option card ────────────────────────────────────────────────────────
@Composable
private fun GenderOptionCard(
    option: GenderOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val cardBg = if (isDark) SurfaceOne else Color.White
    val animatedBackground by animateColorAsState(
        targetValue = if (isSelected)
            OrangePrimary.copy(alpha = 0.06f)
        else
            cardBg,
        animationSpec = tween(250),
        label = ""
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = ""
    )


    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) OrangePrimary
        else MaterialTheme.colorScheme.outline.copy(0.5f),
        animationSpec = tween(250),
        label = ""
    )

    val iconBg = if (isDark)
        option.bgTint.copy(0.25f)
    else
        option.bgTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBackground)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = animatedBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                option.label,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            if (option.subtitle.isNotEmpty()) {
                Text(
                    option.subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        // Radio button
        AnimatedContent(
            targetState = isSelected,
            label = ""
        ) { selected ->

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            OrangePrimary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                )
            }
        }
    }
}

// ── Reusable onboarding progress stepper ──────────────────────────────────────
@Composable
fun OnboardingProgressStepper(
    steps: List<String>,
    currentStep: Int, // 0-based
    modifier: Modifier = Modifier
) {
    val safeStep = currentStep.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val targetProgress = if (steps.size <= 1) 1f else safeStep.toFloat() / (steps.size - 1).toFloat()
    // A destination is often newly composed after Next is tapped, so start one step
    // behind and animate forward instead of drawing the target position immediately.
    val progressAnimation = androidx.compose.runtime.remember(steps.size) {
        Animatable(
            if (steps.size <= 1 || safeStep == 0) 0f
            else (safeStep - 1).toFloat() / (steps.size - 1).toFloat()
        )
    }
    LaunchedEffect(safeStep, steps.size) {
        progressAnimation.animateTo(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 650, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    }
    val progress = progressAnimation.value
    val inactiveLine = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    // One shared drawing layer owns all connector geometry. Step centers are always
    // at the same fractions of the available width on every onboarding screen.
    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            if (steps.size > 1) {
                val centerY = 14.dp.toPx()
                val startX = size.width / steps.size / 2f
                val endX = size.width - startX
                val stroke = 2.dp.toPx()
                drawLine(
                    color = inactiveLine,
                    start = androidx.compose.ui.geometry.Offset(startX, centerY),
                    end = androidx.compose.ui.geometry.Offset(endX, centerY),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = OrangePrimary,
                    start = androidx.compose.ui.geometry.Offset(startX, centerY),
                    end = androidx.compose.ui.geometry.Offset(
                        x = startX + (endX - startX) * progress,
                        y = centerY
                    ),
                    strokeWidth = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            steps.forEachIndexed { index, label ->
                val isCompleted = index < safeStep
                val isActive = index == safeStep
                val isHighlighted = isCompleted || isActive
                val circleColor by animateColorAsState(
                    targetValue = if (isHighlighted) OrangePrimary else MaterialTheme.colorScheme.outline,
                    animationSpec = tween(durationMillis = 180),
                    label = "onboardingCircleColor"
                )
                val labelColor by animateColorAsState(
                    targetValue = if (isActive) OrangePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 180),
                    label = "onboardingLabelColor"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isHighlighted) circleColor else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, circleColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp),
                        fontSize = 9.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = labelColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 11.sp,
                        minLines = 2,
                        maxLines = 2,
                        softWrap = true
                    )
                }
            }
        }
    }
}
