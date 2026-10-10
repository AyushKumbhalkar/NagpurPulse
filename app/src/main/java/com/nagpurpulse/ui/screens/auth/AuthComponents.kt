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

// ─── Animated logo ────────────────────────────────────────────────────────────

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
    clearEmail: Boolean = false,
    bare: Boolean = false,
    fieldHeight: androidx.compose.ui.unit.Dp? = null
) {
    val rowHeight = fieldHeight ?: if (bare) 60.dp else 56.dp
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
        Modifier.fillMaxWidth().height(rowHeight)
            .then(if (bare) Modifier else Modifier.clip(shape)
                .background(containerColor ?: colors.sand)
                .border(if (focused || errorMessage != null) 2.dp else 1.dp,
                    if (errorMessage != null) colors.error else if (focused) colors.accent else colors.outline.copy(alpha = 0.5f), shape))
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
            textStyle = TextStyle(color = if (enabled) colors.ink else colors.muted, fontSize = if (bare) 14.sp else 15.sp, fontFamily = LocalAuthFont.current),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.weight(1f).height(rowHeight)
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
                    if (value.isEmpty()) AuthFitText(placeholder, color = if (bare) colors.ink.copy(alpha = 0.45f) else colors.muted, maxSize = if (bare) 13 else 14, minSize = 9)
                    inner()
                }
            }
        )
        if (clearEmail) {
            // Keep the trailing slot stable while the user types or presses Backspace.
            val showClear = focused && value.isNotEmpty()
            androidx.compose.material3.IconButton(
                onClick = { onValueChange("") },
                enabled = enabled && showClear,
                modifier = Modifier.size(48.dp).alpha(if (showClear) 1f else 0f)
            ) {
                Icon(androidx.compose.material.icons.Icons.Filled.Close, clearLabel, tint = colors.ink)
            }
        } else trailingIcon?.invoke()
    }
}

