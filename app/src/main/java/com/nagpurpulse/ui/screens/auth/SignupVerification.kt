@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.nagpurpulse.ui.screens.auth

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.delay
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

private data class VerifyPalette(
    val card: Color,
    val ink: Color,
    val muted: Color,
    val orange: Color,
    val orangeSoft: Color,
    val orangeLine: Color,
    val orangeFilled: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val error: Color,
    val errorSoft: Color,
    val neutral: Color,
    val artFill: Color,
    val artStroke: Color,
    val artGlow: Color,
    val pillBorder: Color,
    val iconNeutral: Color,
    val fieldActive: Color,
    val fieldIdle: Color,
    val closeBg: Color,
    val scrim: Color,
    val scrimDialog: Color,
    val overlayCard: Color,
    val overlayRing: Color
)

private val LightVerifyPalette = VerifyPalette(
    card = Color(0xFFFFFCF7),
    ink = Color(0xFF142033),
    muted = Color(0xFF64748B),
    orange = Color(0xFFF4511E),
    orangeSoft = Color(0xFFFFF1E6),
    orangeLine = Color(0xFFFFCBAA),
    orangeFilled = Color(0xFFFFA36B),
    gradientStart = Color(0xFFFF941F),
    gradientEnd = Color(0xFFFF3D1F),
    error = Color(0xFFD93025),
    errorSoft = Color(0xFFFFF1F0),
    neutral = Color(0xFFF6F1EA),
    artFill = Color(0xFFFFE5CF),
    artStroke = Color(0xFFF4B07D),
    artGlow = Color(0xFFFFE2CC),
    pillBorder = Color(0xFFFFE0C8),
    iconNeutral = Color(0xFF4B5563),
    fieldActive = Color.White,
    fieldIdle = Color(0xFFFFFBF7),
    closeBg = Color(0xFFFFF7EF),
    scrim = Color(0xCCFFF9F2),
    scrimDialog = Color(0xAFFFFCF7),
    overlayCard = Color.White,
    overlayRing = Color(0xFFFFD6B8)
)

private val DarkVerifyPalette = VerifyPalette(
    card = Color(0xFF181818),
    ink = Color(0xFFF2F2F7),
    muted = Color(0xFF9A9AA2),
    orange = Color(0xFFFF7A45),
    orangeSoft = Color(0xFF2B1A10),
    orangeLine = Color(0xFF5A3420),
    orangeFilled = Color(0xFFB5582C),
    gradientStart = Color(0xFFFF941F),
    gradientEnd = Color(0xFFFF3D1F),
    error = Color(0xFFFF6B60),
    errorSoft = Color(0xFF3A1A18),
    neutral = Color(0xFF242426),
    artFill = Color(0xFF2E1D12),
    artStroke = Color(0xFF7A4A2A),
    artGlow = Color(0xFF3A2314),
    pillBorder = Color(0xFF4A2D1A),
    iconNeutral = Color(0xFFB0B0B8),
    fieldActive = Color(0xFF202022),
    fieldIdle = Color(0xFF1C1C1E),
    closeBg = Color(0xFF262626),
    scrim = Color(0xCC080808),
    scrimDialog = Color(0xB3000000),
    overlayCard = Color(0xFF1C1C1E),
    overlayRing = Color(0xFF5A3420)
)

@Composable
private fun rememberVerifyPalette(): VerifyPalette =
    if (LocalIsDarkTheme.current) DarkVerifyPalette else LightVerifyPalette

@Composable
internal fun SignupEmailVerificationDialog(
    email: String,
    code: String,
    seconds: Int,
    isLoading: Boolean,
    error: String?,
    rateLimited: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    val vc = rememberVerifyPalette()
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var otpFocused by remember { mutableStateOf(false) }
    var otpShake by remember { mutableStateOf(0) }
    val otpShakeOffset = remember { Animatable(0f) }

    // Submit by itself as soon as the 6th digit is in (typed or pasted).
    LaunchedEffect(code) {
        if (code.length == 6 && !isLoading) {
            delay(180L)
            onVerify()
        }
    }

    // Open the keyboard automatically (and again after a failed attempt).
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(280L)
            runCatching { focusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    // Shake + haptic on error.
    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            otpShake++
        }
    }
    LaunchedEffect(otpShake) {
        if (otpShake == 0) return@LaunchedEffect
        otpShakeOffset.snapTo(0f)
        otpShakeOffset.animateTo(-8f, tween(55))
        otpShakeOffset.animateTo(8f, tween(55))
        otpShakeOffset.animateTo(-6f, tween(45))
        otpShakeOffset.animateTo(6f, tween(45))
        otpShakeOffset.animateTo(0f, tween(45))
    }

    // Gentle floating envelope.
    val floatTransition = rememberInfiniteTransition(label = "verify-float")
    val floatY by floatTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "envelope-float"
    )

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = RoundedCornerShape(32.dp)
            val cardWidth = minOf(maxWidth * 0.92f, 400.dp)
            val compactWidth = cardWidth < 330.dp
            val compactHeight = maxHeight < 700.dp
            val hPad = if (compactWidth) 18.dp else 24.dp
            val illustration = if (compactHeight) 84.dp else 100.dp
            val otpGap = if (compactWidth) 6.dp else 9.dp
            val canVerify = code.length == 6 && !isLoading
            val hasError = !error.isNullOrBlank()

            // Card HEIGHT WRAPS ITS CONTENT (no more dead space at the bottom).
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .heightIn(max = maxHeight * 0.92f)
                    .clip(cardShape)
                    .background(vc.card)
            ) {
                // ── Decorative layer: never affects layout ──
                Box(modifier = Modifier.matchParentSize()) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val fill = Path().apply {
                            moveTo(size.width * 0.48f, 0f)
                            cubicTo(
                                size.width * 0.66f, size.height * 0.08f,
                                size.width * 0.80f, size.height * 0.04f,
                                size.width, size.height * 0.30f
                            )
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(fill, brush = SolidColor(vc.artFill))

                        val line = Path().apply {
                            moveTo(size.width * 0.60f, 0f)
                            cubicTo(
                                size.width * 0.74f, size.height * 0.13f,
                                size.width * 0.87f, size.height * 0.08f,
                                size.width, size.height * 0.36f
                            )
                        }
                        drawPath(
                            line,
                            brush = SolidColor(vc.artStroke),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }

                    // Only the bottom 84dp of the wave artwork is shown (cropped, not stretched).
                    Image(
                        painter = painterResource(R.drawable.transparent_peach_wave_footer_overlay),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .align(Alignment.BottomCenter)
                            .alpha(0.85f)
                    )
                }

                // ── Foreground content ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = hPad, end = hPad, top = 30.dp, bottom = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Illustration with soft glow
                    Box(
                        modifier = Modifier.size(illustration + 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(vc.artGlow, vc.artGlow.copy(alpha = 0f))
                                    ),
                                    CircleShape
                                )
                        )
                        Image(
                            painter = painterResource(R.drawable.verify_email),
                            contentDescription = stringResource(R.string.verify_cd_icon),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(illustration)
                                .offset(y = floatY.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = vc.ink, fontWeight = FontWeight.ExtraBold)) {
                                append(stringResource(R.string.verify_title_ink) + " ")
                            }
                            withStyle(SpanStyle(color = vc.orange, fontWeight = FontWeight.ExtraBold)) {
                                append(stringResource(R.string.verify_title_accent))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = if (compactWidth) 26.sp else 29.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        stringResource(R.string.verify_enter_code),
                        modifier = Modifier.fillMaxWidth(),
                        color = vc.muted,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(10.dp))

                    // Email shown as a chip: long addresses ellipsize instead of breaking the layout.
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(vc.orangeSoft)
                            .border(1.dp, vc.pillBorder, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            tint = vc.orange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            email,
                            modifier = Modifier.weight(1f, fill = false),
                            color = vc.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── OTP input ──
                    BasicTextField(
                        value = code,
                        onValueChange = { if (!isLoading) onCodeChange(it.filter(Char::isDigit).take(6)) },
                        readOnly = isLoading,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onVerify() }),
                        textStyle = TextStyle(color = Color.Transparent),
                        cursorBrush = SolidColor(Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = otpShakeOffset.value.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { otpFocused = it.isFocused },
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(otpGap)
                                ) {
                                    repeat(6) { index ->
                                        OtpDigitBox(
                                            digit = code.getOrNull(index),
                                            isActive = otpFocused && !isLoading && index == code.length,
                                            isError = hasError && code.isEmpty()
                                        )
                                    }
                                }
                                // The REAL text field must be composed, otherwise taps never focus it
                                // and the keyboard never opens. It sits invisibly over the boxes.
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .alpha(0f)
                                ) {
                                    innerTextField()
                                }
                            }
                        }
                    )

                    // Error sits directly under the boxes, where the eye already is.
                    AnimatedVisibility(
                        visible = hasError,
                        enter = fadeIn(tween(160)) + expandVertically(),
                        exit = fadeOut(tween(120)) + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = vc.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                error.orEmpty(),
                                modifier = Modifier.weight(1f, fill = false),
                                color = vc.error,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // ── Primary button: dimmed until all 6 digits are entered ──
                    val buttonAlpha by animateFloatAsState(
                        targetValue = if (canVerify) 1f else 0.55f,
                        animationSpec = tween(180),
                        label = "verify-button-alpha"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = if (canVerify) 10.dp else 0.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = Color(0x66FF5A1F),
                                spotColor = Color(0x99FF5A1F)
                            )
                            .alpha(buttonAlpha)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(vc.gradientStart, vc.gradientEnd)
                                )
                            )
                            .pressScale(onClick = onVerify),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(if (isLoading) R.string.verify_button_loading else R.string.verify_button),
                                color = Color.White,
                                fontSize = if (compactWidth) 18.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── Open the user's email app ──
                    val dialogContext = LocalContext.current
                    val noEmailAppMessage = stringResource(R.string.verify_no_email_app)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(vc.neutral)
                                .clickable(role = Role.Button) {
                                    if (!openEmailApp(dialogContext)) {
                                        Toast.makeText(dialogContext, noEmailAppMessage, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = vc.orange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.verify_open_email_app),
                                color = vc.ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                        val pasteNone = stringResource(R.string.verify_paste_none)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(vc.neutral)
                                .clickable(role = Role.Button, enabled = !isLoading) {
                                    val found = Regex("\\d{6}").find(clipboard.getText()?.text.orEmpty())?.value
                                    if (found != null) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onCodeChange(found)
                                    } else {
                                        Toast.makeText(dialogContext, pasteNone, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = vc.orange, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.verify_paste_code),
                                color = vc.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // ── Resend / timer ──
                    if (seconds > 0) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(vc.neutral)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = vc.muted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = vc.muted)) {
                                        append(stringResource(if (rateLimited) R.string.verify_retry_in else R.string.verify_resend_in) + " ")
                                    }
                                    withStyle(SpanStyle(color = vc.orange, fontWeight = FontWeight.Bold)) {
                                        // mm:ss, so 75s shows 01:15 (the old code showed 00:75)
                                        append("%02d:%02d".format(seconds / 60, seconds % 60))
                                    }
                                },
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(vc.orangeSoft)
                                .pressScale(onClick = onResend)
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = vc.orange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.verify_resend_code),
                                color = vc.orange,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // ── Secondary action ──
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .pressScale(onClick = onChangeEmail)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = vc.muted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.verify_change_email),
                            color = vc.muted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ── Close button: 48dp touch target, 34dp visual ──
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .pressScale(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(vc.orangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = vc.iconNeutral,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.matchParentSize().clip(cardShape)) {
                        NagpurPulseLoadingOverlay(
                            message = stringResource(R.string.loading_verifying_email),
                            inDialog = true
                        )
                    }
                }
            }
        }
    }
}

/** One OTP cell: empty / active (cursor) / filled / error. */
@Composable
private fun RowScope.OtpDigitBox(
    digit: Char?,
    isActive: Boolean,
    isError: Boolean
) {
    val vc = rememberVerifyPalette()
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> vc.error
            isActive -> vc.orange
            digit != null -> vc.orangeFilled
            else -> vc.orangeLine
        },
        animationSpec = tween(160),
        label = "otp-border"
    )
    val background by animateColorAsState(
        targetValue = when {
            isError -> vc.errorSoft
            isActive || digit != null -> vc.fieldActive
            else -> vc.fieldIdle
        },
        animationSpec = tween(160),
        label = "otp-background"
    )
    val pop by animateFloatAsState(
        targetValue = if (digit != null) 1f else 0.6f,
        animationSpec = tween(140),
        label = "otp-pop"
    )
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(0.80f)
            .clip(shape)
            .background(background)
            .border(if (isActive || isError) 2.dp else 1.5.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            digit != null -> Text(
                text = digit.toString(),
                color = if (isError) vc.error else vc.ink,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                }
            )
            isActive -> OtpCursor()
        }
    }
}

@Composable
private fun OtpCursor() {
    val vc = rememberVerifyPalette()
    val transition = rememberInfiniteTransition(label = "otp-cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor-alpha"
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(26.dp)
            .alpha(cursorAlpha)
            .background(vc.orange, RoundedCornerShape(2.dp))
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  LOADING OVERLAY (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NagpurPulseLoadingOverlay(
    message: String,
    inDialog: Boolean = false
) {
    val vc = rememberVerifyPalette()
    val infiniteTransition = rememberInfiniteTransition(label = "nagpurpulse-loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo-ring-rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (inDialog) vc.scrimDialog else vc.scrim
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (inDialog) 86.dp else 92.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(vc.overlayCard)
                    .border(
                        width = 1.dp,
                        color = vc.overlayRing,
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                        .rotate(rotation)
                ) {
                    drawArc(
                        color = vc.orange,
                        startAngle = -55f,
                        sweepAngle = 105f,
                        useCenter = false,
                        style = Stroke(
                            width = 3.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }

                Image(
                    painter = painterResource(R.drawable.nagpurpulse_orange_n_icon),
                    contentDescription = "NagpurPulse",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(if (inDialog) 46.dp else 50.dp)
                        .then(Modifier.rotate((pulse - 1f) * 2.5f))
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = message,
                color = vc.ink,
                fontSize = if (inDialog) 14.sp else 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.loading_please_wait),
                color = vc.muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  EMAIL-ALREADY-USED DIALOG (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun SignupEmailAlreadyUsedDialog(
    onGoToLogin: () -> Unit,
    onTryDifferentEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    val vc = rememberVerifyPalette()
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Keep the same modal frame family as the Verify Email dialog.
            // The foreground is a real Column layout: no child is vertically
            // positioned with offsets, so text can never be covered by a button.
            val popupWidth = maxWidth * 0.86f
            val popupHeight = maxHeight * 0.74f
            val horizontalPadding = minOf(popupWidth * 0.062f, 26.dp)
            val illustrationSize = minOf(
                popupWidth * 0.42f,
                popupHeight * 0.23f
            )
            val compactWidth = popupWidth < 330.dp
            val compactHeight = popupHeight < 620.dp
            val titleSize = when {
                compactWidth -> 28.sp
                compactHeight -> 29.sp
                else -> 31.sp
            }
            val bodySize = if (compactWidth || compactHeight) 15.sp else 16.sp
            val bodyLineHeight = if (compactWidth || compactHeight) 21.sp else 22.sp
            val contentGap = if (compactHeight) 12.dp else 16.dp
            val actionGap = if (compactHeight) 20.dp else 24.dp

            Box(
                modifier = Modifier
                    .width(popupWidth)
                    .height(popupHeight)
                    .clip(RoundedCornerShape(30.dp))
                    .background(vc.card)
            ) {
                // BACKGROUND LAYER: these decorations never participate in the
                // foreground Column's measurement or push its content.
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(popupHeight * 0.19f)
                        .align(Alignment.TopCenter)
                ) {
                    val fill = Path().apply {
                        moveTo(size.width * 0.48f, 0f)
                        cubicTo(
                            size.width * 0.66f, size.height * 0.08f,
                            size.width * 0.80f, size.height * 0.04f,
                            size.width, size.height * 0.30f
                        )
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(
                        fill,
                        brush = SolidColor(vc.artFill)
                    )

                    val line = Path().apply {
                        moveTo(size.width * 0.60f, 0f)
                        cubicTo(
                            size.width * 0.74f, size.height * 0.13f,
                            size.width * 0.87f, size.height * 0.08f,
                            size.width, size.height * 0.36f
                        )
                    }
                    drawPath(
                        line,
                        brush = SolidColor(vc.artStroke),
                        style = Stroke(
                            width = (popupWidth * 0.0027f).toPx()
                        )
                    )
                }

                Image(
                    painter = painterResource(R.drawable.transparent_peach_wave_footer_overlay),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2048f / 682f)
                        .align(Alignment.BottomCenter)
                )

                // FOREGROUND LAYER.
                // Scroll is only an overflow safety net for large font/display
                // scaling. On normal screens the complete content fits without
                // scrolling.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            top = 42.dp,
                            bottom = 18.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.email_alert),
                        contentDescription = stringResource(R.string.used_cd_icon),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(illustrationSize)
                    )

                    Spacer(Modifier.height(contentGap))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = vc.ink,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(stringResource(R.string.used_title_ink) + " ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = vc.orange,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            ) {
                                append(stringResource(R.string.used_title_accent))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = titleSize,
                        lineHeight = if (compactWidth) 33.sp else 36.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(Modifier.height(contentGap))

                    Text(
                        text = stringResource(R.string.used_body),
                        modifier = Modifier.fillMaxWidth(),
                        color = vc.muted,
                        fontSize = bodySize,
                        lineHeight = bodyLineHeight,
                        textAlign = TextAlign.Center
                    )

                    // The description is a normal Column child. This fixed
                    // spacing guarantees it is completely measured before the
                    // primary action can begin.
                    Spacer(Modifier.height(actionGap))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF941F), Color(0xFFFF3D1F))
                                )
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = onGoToLogin
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.size(30.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val stroke = (w * 0.11f).coerceAtLeast(1.5f)

                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.52f, h * 0.10f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.38f, h * 0.80f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f),
                                    style = Stroke(
                                        width = stroke
                                    )
                                )
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.50f),
                                    end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.33f),
                                    end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                drawLine(
                                    color = Color.White,
                                    start = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.67f),
                                    end = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.50f),
                                    strokeWidth = stroke,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Text(
                                stringResource(R.string.used_go_login),
                                color = Color.White,
                                fontSize = if (compactWidth) 19.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.width(10.dp))

                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(60.dp))
                            .background(vc.fieldIdle)
                            .border(
                                width = 1.dp,
                                color = vc.orangeLine,
                                shape = RoundedCornerShape(60.dp)
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = onTryDifferentEmail
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.used_try_different),
                            color = vc.ink,
                            fontSize = if (compactWidth) 18.sp else 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                // 44–48dp accessible touch target; the X itself stays visually
                // compact and is kept above the artwork/content.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(vc.closeBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = vc.iconNeutral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  HEADLINE + SMALL HELPERS (unchanged)
// ═══════════════════════════════════════════════════════════════════════════

/** U6: scrim + circle that pops in, then a tick that draws itself. */
@Composable
internal fun SignupSuccessOverlay() {
    val vc = rememberVerifyPalette()
    val reducedMotion = rememberReduceMotion()
    val pop = remember { Animatable(if (reducedMotion) 1f else 0.6f) }
    val tick = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reducedMotion) pop.snapTo(1f)
        else pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 400f))
    }
    LaunchedEffect(Unit) {
        if (reducedMotion) tick.snapTo(1f) else {
            delay(160L)
            tick.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(vc.scrim)
            // swallow touches so nothing underneath can be tapped during the moment
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(vc.gradientStart, vc.gradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(52.dp)) {
                    val w = size.width
                    val h = size.height
                    val a = Offset(w * 0.10f, h * 0.55f)
                    val b = Offset(w * 0.38f, h * 0.82f)
                    val c = Offset(w * 0.92f, h * 0.22f)
                    val t = tick.value
                    val stroke = 5.dp.toPx()
                    // first leg 0..0.4, second leg 0.4..1
                    val first = (t / 0.4f).coerceIn(0f, 1f)
                    val second = ((t - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    drawLine(
                        Color.White, a, Offset(a.x + (b.x - a.x) * first, a.y + (b.y - a.y) * first),
                        strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    if (second > 0f) {
                        drawLine(
                            Color.White, b, Offset(b.x + (c.x - b.x) * second, b.y + (c.y - b.y) * second),
                            strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.signup_success_title),
                color = vc.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}
