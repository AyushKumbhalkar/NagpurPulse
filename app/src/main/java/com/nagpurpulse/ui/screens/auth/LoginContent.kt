package com.nagpurpulse.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun LoginContent(
    email: String = "", password: String = "", loading: Boolean = false, online: Boolean = true,
    emailError: String? = null, passwordError: String? = null, error: String? = null,
    info: String? = null, verificationRequired: Boolean = false, keyboardPreview: Boolean = false,
    lastMethod: String = "",
    onEmail: (String) -> Unit = {}, onPassword: (String) -> Unit = {},
    onEmailBlur: () -> Unit = {}, onPasswordBlur: () -> Unit = {},
    onLogin: () -> Unit = {}, onForgot: () -> Unit = {}, onGoogle: () -> Unit = {},
    onSignup: () -> Unit = {}, onResend: () -> Unit = {}, onGuest: () -> Unit = {}
) {
    val colors = authPalette()
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val fieldMessage = emailError ?: passwordError ?: error

    // Wrong password / invalid email: short horizontal shake + haptic tick (skipped for reduce-motion).
    val shake = remember { Animatable(0f) }
    LaunchedEffect(fieldMessage) {
        if (fieldMessage != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (!reduceMotion) for (x in floatArrayOf(-10f, 8f, -6f, 4f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    // Rare extra rows reserve space up-front so the layout never overflows.
    // Card lights up while a field inside it has focus.
    var cardFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        when {
            fieldMessage != null -> colors.error.copy(alpha = 0.6f)
            cardFocused -> colors.accent
            else -> colors.outline.copy(alpha = 0.25f)
        }, label = "card-border")
    val glow by animateFloatAsState(if (cardFocused) 0.32f else 0.15f, label = "card-glow")
    val suggestion = remember(email) { suggestEmailCorrection(email) }

    val extra = (if (info != null) 28.dp else 0.dp) + (if (verificationRequired) 48.dp else 0.dp)

    LoginScaffold(
        online = online, keyboardPreview = keyboardPreview,
        signupText = stringResource(R.string.auth_signup_footer),
        signupEnabled = !loading, onSignup = onSignup, extraHeight = extra
    ) { d, keyboard, flex ->
        AnimatedVisibility(
            visible = !keyboard,
            enter = if (reduceMotion) EnterTransition.None else fadeIn() + expandVertically(),
            exit = if (reduceMotion) ExitTransition.None else fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AuthEntrance(0) {
                    Column(Modifier.padding(top = 8.dp).semantics(mergeDescendants = true) { heading() },
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        AuthHeadline(stringResource(R.string.login_headline_prefix).trim(),
                            stringResource(R.string.auth_talking), compact = false, brightAccent = true,
                            sizeSp = d.headlineSp, lineDp = d.headlineLine)
                        if (d.showWelcome) Text(
                            stringResource(R.string.login_welcome_back), color = colors.ink,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(d.gap))
            }
        }
        if (keyboard) Spacer(Modifier.height(8.dp))
        FlexSpacer(flex)

        // One joined card: email row, hairline, password row.
        val cardShape = RoundedCornerShape(24.dp)
        AuthEntrance(1) {
            Column(
                Modifier.fillMaxWidth()
                    .graphicsLayer { translationX = shake.value.dp.toPx() }
                    .shadow(if (cardFocused) 10.dp else 6.dp, cardShape, ambientColor = colors.accent.copy(alpha = glow),
                        spotColor = colors.accent.copy(alpha = glow))
                    .clip(cardShape).background(colors.surface.copy(alpha = 0.92f))
                    .border(if (cardFocused || fieldMessage != null) 1.5.dp else 1.dp, borderColor, cardShape)
            ) {
                AuthEmailField(email, onEmail, !loading, emailError, onEmailBlur,
                    {}, bare = true, fieldHeight = d.row, emailImeAction = ImeAction.Done)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outline.copy(alpha = 0.25f))
                AuthPasswordField(password, onPassword, !loading, stringResource(R.string.login_password_hint),
                    passwordError, onPasswordBlur, { keyboardController?.hide(); onLogin() },
                    bare = true, fieldHeight = d.row)
            }
        }

        // Error (left) and "Forgot password?" (right) share one row, so an error never shifts the layout.
        Row(Modifier.fillMaxWidth().heightIn(min = d.forgot), verticalAlignment = Alignment.CenterVertically) {
            if (fieldMessage != null) LoginError(fieldMessage, Modifier.weight(1f))
            else if (suggestion != null && !loading) Text(
                stringResource(R.string.email_suggest_prompt, suggestion), color = colors.accent,
                fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
                    .clickable(role = Role.Button) { onEmail(suggestion) }
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            else Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.login_forgot_password), color = colors.ink, fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.heightIn(min = d.forgot).wrapContentHeight(Alignment.CenterVertically)
                    .then(if (!loading) Modifier.clickableNoRipple(onForgot) else Modifier)
            )
        }
        AuthNotice(info, isError = false)
        if (verificationRequired) AuthLink(stringResource(R.string.login_resend_verification),
            enabled = !loading, onClick = onResend)

        AuthEntrance(3) {
            Box {
                AuthAction(stringResource(if (loading) R.string.login_signing_in else R.string.login_button),
                    loading, contentColor = Color.White, height = d.button, elevated = true, onClick = onLogin)
                if (lastMethod == "email" && !loading) LastUsedBadge(Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(d.gap))
        AuthOr()
        Spacer(Modifier.height(d.gap))
        Box {
            AuthAction(stringResource(R.string.login_continue_google), loading, google = true,
                height = d.google, elevated = true, onClick = onGoogle)
            if (lastMethod == "google" && !loading) LastUsedBadge(Modifier.align(Alignment.TopEnd))
        }
        FlexSpacer(flex)
    }
}

/** Small "Last used" tag on the button the person used last time. */
@Composable
private fun LastUsedBadge(modifier: Modifier = Modifier) {
    val colors = authPalette()
    Text(
        stringResource(R.string.login_last_used), color = Color.White, fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier.offset(x = (-16).dp, y = (-8).dp)
            .background(colors.accent, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun LoginError(message: String, modifier: Modifier = Modifier) {
    val colors = authPalette()
    Row(modifier.padding(start = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(16.dp), tint = colors.error)
        Spacer(Modifier.width(8.dp))
        Text(message, color = colors.error, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 3,
            modifier = Modifier.weight(1f))
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() },
        indication = null, role = Role.Button, onClick = onClick)
}
