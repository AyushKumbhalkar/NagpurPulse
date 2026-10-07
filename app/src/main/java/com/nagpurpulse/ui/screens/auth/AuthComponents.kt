@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

// this is the AuthComponents.kt file
//java/com/nagpurpulse/ui/screens/auth/AuthComponents.kt

package com.nagpurpulse.ui.screens.auth

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.semantics.contentDescription

import androidx.compose.material.icons.filled.Close


import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.OrangePrimary
import com.nagpurpulse.ui.theme.RedAlert
import com.nagpurpulse.ui.theme.RedSubtle
import com.nagpurpulse.ui.theme.TextSecondary
import com.nagpurpulse.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics


// ─── Cinematic city background ────────────────────────────────────────────────
@Composable
fun CinematicBackground(isLogin: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isLogin) {
            // The signup illustration already contains the NagpurPulse wordmark,
            // landmarks, decorative slogans and green footer area.
            Image(
                painter = painterResource(id = R.drawable.nagpurpulse_signup_background),
                contentDescription = "NagpurPulse illustrated cityscape with railway station and metro",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val t = rememberInfiniteTransition(label = "bg")
            val glow1 by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "g1"
            )
            val glow2 by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(6000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "g2"
            )
            val waveY by t.animateFloat(
                0f, 12f,
                infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "wy"
            )

            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
            )
            Box(
                modifier = Modifier.size(400.dp).align(Alignment.TopCenter)
                    .offset(y = (-80 + waveY * 0.5f).dp)
                    .background(Brush.radialGradient(listOf(
                        OrangePrimary.copy(alpha = 0.10f + glow1 * 0.04f),
                        Color(0xFFFF8A00).copy(alpha = 0.05f),
                        Color.Transparent
                    )))
            )
            Box(
                modifier = Modifier.size(300.dp).align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (60 + waveY).dp)
                    .background(Brush.radialGradient(listOf(
                        Color(0xFFFF6000).copy(alpha = 0.08f + glow2 * 0.03f),
                        Color.Transparent
                    )))
            )
            Box(
                modifier = Modifier.size(250.dp).align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = 40.dp)
                    .background(Brush.radialGradient(listOf(Color(0xFFFF8C00).copy(alpha = 0.06f), Color.Transparent)))
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter)
                    .offset(y = 280.dp)
                    .background(Brush.horizontalGradient(listOf(
                        Color.Transparent,
                        OrangePrimary.copy(0.15f),
                        OrangePrimary.copy(0.30f),
                        OrangePrimary.copy(0.15f),
                        Color.Transparent
                    )))
            )
            repeat(3) { i ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomCenter)
                        .offset(y = (-40 - i * 18 + waveY * (i + 1) * 0.3f).dp)
                        .background(Brush.horizontalGradient(listOf(
                            Color.Transparent,
                            OrangePrimary.copy(alpha = (0.08f - i * 0.02f)),
                            OrangePrimary.copy(alpha = (0.12f - i * 0.03f)),
                            OrangePrimary.copy(alpha = (0.08f - i * 0.02f)),
                            Color.Transparent
                        )))
                )
            }
        }
    }
}


// ─── Animated logo ────────────────────────────────────────────────────────────


@Composable
fun PremiumLogo(subtitle: String, compact: Boolean = false) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(80)
        visible = true
    }

    val t = rememberInfiniteTransition(label = "logo")

    val ringPulse by t.animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )

    val glowAlpha by t.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(700))
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 132.dp else 210.dp),
            contentAlignment = Alignment.TopCenter
        ) {

            val isDarkTheme = LocalIsDarkTheme.current


            val pulseLogo =
                if (isDarkTheme)
                    R.drawable.heartbeat_dark
                else
                    R.drawable.heartbeat_light

            val titleColor =
                if (isDarkTheme)
                    Color.White
                else
                    MaterialTheme.colorScheme.onSurface

            val subtitleColor =
                if (isDarkTheme)
                    Color.White.copy(alpha = 0.92f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

            val textShadow =
                if (isDarkTheme) {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.55f),
                        offset = Offset(0f, 2f),
                        blurRadius = 8f
                    )
                } else {
                    Shadow(
                        color = Color.White.copy(alpha = 0.70f),
                        offset = Offset(0f, 2f),
                        blurRadius = 8f
                    )
                }

            val headerImage =
                if (isDarkTheme)
                    R.drawable.authtest3
                else
                    R.drawable.authtest3_light

            Image(
                painter = painterResource(id = headerImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            val overlayBrush =
                if (isDarkTheme) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.20f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )

            Column(
                modifier = Modifier.padding(top = if (compact) 18.dp else 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /*

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(70.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(ringPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        OrangePrimary.copy(glowAlpha * 0.20f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .scale(ringPulse)
                            .border(
                                2.dp,
                                OrangePrimary.copy(glowAlpha),
                                CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(
                                1.dp,
                                OrangePrimary.copy(0.8f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "∿",
                            color = OrangePrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                */

                /*

                Image(
                    painter = painterResource(id = pulseLogo),
                    contentDescription = "Nagpur Pulse Logo",
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(0.68f),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(4.dp))

                */

                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                shadow = textShadow,
                                color = titleColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp,
                                letterSpacing = 1.sp
                            )
                        ) {
                            append("NAGPUR ")
                        }

                        withStyle(
                            SpanStyle(
                                shadow = textShadow,
                                color = OrangePrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp,
                                letterSpacing = 1.sp
                            )
                        ) {
                            append("PULSE")
                        }
                    }
                )
                Spacer(Modifier.height(1.dp))

                Text(
                    text = subtitle,
                    color = subtitleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                Image(
                    painter = painterResource(id = pulseLogo),
                    contentDescription = "Nagpur Pulse Logo",
                    modifier = Modifier
                        .offset(y = (-8).dp)      // Move logo upward
                        .height(45.dp)
                        .fillMaxWidth(0.45f),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}


// ─── Premium input field ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PremiumInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    autofillTypes: List<AutofillType> = emptyList(),
    onBlur: (() -> Unit)? = null,
    index: Int = 0,
    containerColor: Color? = null,
    errorMessage: String? = null,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    clearEmail: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }

    // Autofill / password-manager support (Compose 1.6 API). Only active when
    // the caller passes autofillTypes, so LoginScreen is unaffected.
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val autofillNode = remember(autofillTypes) {
        if (autofillTypes.isEmpty()) null
        else AutofillNode(
            autofillTypes = autofillTypes,
            onFill = { filled -> currentOnValueChange(filled) }
        )
    }
    autofillNode?.let { autofillTree += it }
    val colors = com.nagpurpulse.ui.theme.authPalette()
    val shape = RoundedCornerShape(20.dp)
    val clearLabel = stringResource(com.nagpurpulse.R.string.auth_clear_email)
    Row(
        Modifier.fillMaxWidth().height(56.dp).clip(shape)
            .background(containerColor ?: colors.sand)
            .border(if (focused || errorMessage != null) 2.dp else 1.dp,
                if (errorMessage != null) colors.error else if (focused) colors.accent else colors.outline.copy(alpha = 0.5f), shape)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colors.ink) {
            Box(Modifier.size(20.dp)) { leadingIcon() }
        }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled,
            singleLine = true, visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            textStyle = TextStyle(color = if (enabled) colors.ink else colors.muted, fontSize = 15.sp),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f).height(56.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .semantics {
                    contentDescription = placeholder
                    if (errorMessage != null) error(errorMessage)
                }
                .onGloballyPositioned { autofillNode?.boundingBox = it.boundsInWindow() }
                .onFocusChanged { state ->
                    val hadFocus = focused
                    focused = state.isFocused
                    if (hadFocus && !state.isFocused) onBlur?.invoke()
                    autofillNode?.let { node ->
                        if (state.isFocused) autofill?.requestAutofillForNode(node)
                        else autofill?.cancelAutofillForNode(node)
                    }
                },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = colors.muted, fontSize = 14.sp, maxLines = 1)
                    inner()
                }
            }
        )
        if (clearEmail && focused && value.isNotEmpty()) {
            androidx.compose.material3.IconButton(onClick = { onValueChange("") }, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Icon(androidx.compose.material.icons.Icons.Filled.Close, clearLabel, tint = colors.ink)
            }
        } else trailingIcon?.invoke()
    }
}

// ─── Premium CTA button ───────────────────────────────────────────────────────
@Composable
fun PremiumButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    delayMs: Int = 0,
    animateEntrance: Boolean = true
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (animateEntrance) {
            delay(delayMs.toLong())
            visible = true
        } else {
            visible = true
        }
    }

    val t = rememberInfiniteTransition(label = "btn_glow")
    val btnGlow by t.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )

    val btnScale by animateFloatAsState(
        targetValue = if (isLoading) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "btn_scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = if (animateEntrance) fadeIn(animationSpec = tween(400)) +
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    )
                ) else EnterTransition.None
    ) {


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(29.dp),
                    ambientColor = Color(0x66FF5A00),
                    spotColor = Color(0x66FF5A00)
                )
                .scale(btnScale)
                .clip(RoundedCornerShape(29.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF8C1A),   // darker orange
                            Color(0xFFFF6A00),   // rich orange center
                            Color(0xFFFF8220)    // orange, not yellow
                        )
                    )
                )
                .pressScale(
                    pressedScale = 0.96f,
                    onClick = { if (enabled && !isLoading) onClick() }),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                isLoading,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "btn_content"
            ) { loading ->
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Please wait…", color = Color.White)
                    }
                } else {
                    Text(
                        text,
                        color = Color(0xFFF8F8F8),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ─── Social sign-in buttons ───────────────────────────────────────────────────
@Composable
fun SocialButton(
    label: String,
    onClick: () -> Unit,
    delayMs: Int = 0,
    animateEntrance: Boolean = true,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (animateEntrance) {
            delay(delayMs.toLong())
        }
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = if (animateEntrance) {
            fadeIn(animationSpec = tween(350)) +
                    scaleIn(initialScale = 0.9f)
        } else {
            EnterTransition.None
        }
    ) {
        Row(
            modifier = modifier
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    OrangePrimary.copy(0.20f),
                    RoundedCornerShape(16.dp)
                )
                .pressScale(onClick = onClick)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google",
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(18.dp)
            )
            .clickable {
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = "Google",
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "Google",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// ─── Error message ────────────────────────────────────────────────────────────
@Composable
fun AnimatedAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    index: Int = 0
) {
    PremiumInputField(
        value = value,
        onValueChange = onValueChange,
        placeholder = label,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        index = index
    )
}


@Composable
fun AnimatedAuthButton(
    text: String, isLoading: Boolean, enabled: Boolean, onClick: () -> Unit, delayMs: Int = 0
) {
    PremiumButton(text, isLoading, enabled, onClick, delayMs)
}


@Composable
fun AnimatedErrorMessage(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(250)) + expandVertically(),
        exit = fadeOut(tween(200)) + shrinkVertically()
    ) {
        if (message != null) {
            val shake = remember { Animatable(0f) }
            LaunchedEffect(message) {
                repeat(4) { shake.animateTo(if (it % 2 == 0) 8f else -8f, tween(55)) }
                shake.animateTo(0f, tween(55))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = shake.value.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedSubtle)
                    .border(1.dp, RedAlert.copy(0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(message, color = RedAlert.copy(0.9f), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

// ─── Feature pill ─────────────────────────────────────────────────────────────
@Composable
fun FeaturePill(icon: ImageVector, label: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = OrangePrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Text(sub, color = TextTertiary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}


@Composable
fun TrustBadge(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OrangePrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GuestContinueCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                OrangePrimary.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .pressScale(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Continue as Guest",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    text = "Explore without an account",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "→",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
        }
    }
}
