@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.nagpurpulse.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nagpurpulse.R
import com.nagpurpulse.ui.components.pressScale
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.authPalette
import kotlinx.coroutines.delay
import java.util.Locale

private const val RESEND_COOLDOWN_SECONDS = 30
private val DialogMaxWidth = 420.dp

/**
 * "Forgot password" dialog.
 *
 * Two states inside one dialog: the email form, and a "check your inbox" confirmation
 * (open mail app, resend with cooldown, change email). State that must outlive the dialog
 * (loading / sent / error) stays in [AuthViewModel]; everything else is local.
 */
@Composable
internal fun ForgotPasswordDialog(
    initialEmail: String,
    isLoading: Boolean,
    sent: Boolean,
    error: String?,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = authPalette()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val reduced = rememberReduceMotion()

    var email by remember { mutableStateOf(initialEmail) }
    var touched by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    // True once the user has pressed send/resend in THIS dialog, so stale VM state
    // (an old login error, an old "sent" flag) never leaks into the dialog.
    var submitted by remember { mutableStateOf(false) }
    var sentTo by remember { mutableStateOf<String?>(null) }
    var lastSentTo by remember { mutableStateOf("") } // survives the success -> form fade-out
    var cooldown by remember { mutableIntStateOf(0) }
    var mailHint by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    val valid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }
    val dismissible = !isLoading
    val showSuccess = sentTo != null

    val serverError = if (submitted && !isLoading && !sent) error else null
    val formatError = if ((attempted || (touched && email.isNotBlank())) && !valid)
        stringResource(R.string.login_err_email_invalid) else null

    fun submit(target: String) {
        if (isLoading) return
        submitted = true
        mailHint = null
        onSend(target)
    }

    fun submitForm() {
        attempted = true
        if (!valid) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            return
        }
        submit(email.trim())
    }

    LaunchedEffect(sent, isLoading, submitted) {
        if (sent && submitted && !isLoading) {
            sentTo = email.trim()
            lastSentTo = email.trim()
            cooldown = RESEND_COOLDOWN_SECONDS
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(serverError) {
        if (serverError != null) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000L)
            cooldown -= 1
        }
    }
    LaunchedEffect(Unit) {
        // Only pop the keyboard when there is nothing prefilled to confirm.
        if (email.isBlank()) {
            delay(300L)
            runCatching { focusRequester.requestFocus() }
        }
    }

    val enter = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) enter.snapTo(1f) else enter.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
    }

    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible
        )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(32.dp)
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .widthIn(max = DialogMaxWidth)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.88f)
                    .graphicsLayer {
                        val t = enter.value
                        alpha = t
                        scaleX = 0.92f + 0.08f * t
                        scaleY = 0.92f + 0.08f * t
                        translationY = (1f - t) * 24.dp.toPx()
                    }
                    .shadow(
                        elevation = 24.dp, shape = shape, clip = false,
                        ambientColor = colors.accent.copy(alpha = 0.25f),
                        spotColor = colors.accent.copy(alpha = 0.35f)
                    )
                    .clip(shape)
                    .background(colors.surface)
                    .border(1.dp, colors.outline.copy(alpha = 0.6f), shape)
                    .verticalScroll(rememberScrollState())
            ) {
                // Brand accent strip: the only decoration, no images.
                Box(
                    Modifier.fillMaxWidth().height(5.dp)
                        .background(if (showSuccess) Brush.horizontalGradient(listOf(colors.success, colors.success.copy(alpha = 0.55f))) else AuthTokens.Gradient)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val closeLabel = stringResource(R.string.login_reset_close)
                    AnimatedContent(
                        targetState = showSuccess,
                        transitionSpec = {
                            if (reduced) EnterTransition.None togetherWith ExitTransition.None
                            else fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120))
                        },
                        label = "reset_content"
                    ) { ok ->
                        if (ok) {
                            ResetSuccessContent(
                                email = lastSentTo,
                                loading = isLoading,
                                cooldown = cooldown,
                                message = serverError ?: mailHint,
                                closeLabel = closeLabel,
                                dismissible = dismissible,
                                onClose = onDismiss,
                                onOpenMail = {
                                    if (!openMailApp(context)) {
                                        mailHint = context.getString(R.string.login_reset_no_mail_app)
                                    }
                                },
                                onResend = { sentTo?.let { submit(it) } },
                                onChangeEmail = {
                                    sentTo = null
                                    submitted = false
                                    cooldown = 0
                                    mailHint = null
                                }
                            )
                        } else {
                            ResetFormContent(
                                email = email,
                                onEmail = { email = it; submitted = false },
                                valid = valid,
                                loading = isLoading,
                                message = formatError ?: serverError,
                                focusRequester = focusRequester,
                                closeLabel = closeLabel,
                                dismissible = dismissible,
                                onClose = onDismiss,
                                onBlur = { touched = true },
                                onSubmit = { submitForm() },
                                onCancel = { if (dismissible) onDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Header row: title on the left, a 44dp-target close button on the right. */
@Composable
private fun ResetHeader(
    title: String, closeLabel: String, dismissible: Boolean, onClose: () -> Unit, success: Boolean = false
) {
    val colors = authPalette()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Row(Modifier.weight(1f).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (success) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(colors.success.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = colors.success, modifier = Modifier.size(17.dp)) }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                title, color = colors.ink, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() }
            )
        }
        Box(
            modifier = Modifier.size(44.dp)
                .alpha(if (dismissible) 1f else 0.4f)
                .pressScale { if (dismissible) onClose() }
                .semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(colors.sand), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, null, tint = colors.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ResetFormContent(
    email: String, onEmail: (String) -> Unit, valid: Boolean, loading: Boolean,
    message: String?, focusRequester: FocusRequester, closeLabel: String, dismissible: Boolean,
    onClose: () -> Unit, onBlur: () -> Unit, onSubmit: () -> Unit, onCancel: () -> Unit
) {
    val colors = authPalette()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        ResetHeader(stringResource(R.string.login_reset_title), closeLabel, dismissible, onClose)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_reset_body),
            color = colors.muted, fontSize = 15.sp, lineHeight = 22.sp
        )
        Spacer(Modifier.height(20.dp))
        PremiumInputField(
            value = email,
            onValueChange = onEmail,
            placeholder = stringResource(R.string.login_email_hint),
            leadingIcon = { Icon(Icons.Filled.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            autofillTypes = listOf(AutofillType.EmailAddress),
            onBlur = onBlur,
            errorMessage = message,
            focusRequester = focusRequester,
            enabled = !loading,
            clearEmail = true
        )
        ResetMessage(message)
        Spacer(Modifier.height(20.dp))
        GradientActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.login_reset_link),
            loadingLabel = stringResource(R.string.login_reset_sending),
            enabled = valid,
            loading = loading,
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onSubmit
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ResetTextAction(
                label = stringResource(R.string.login_cancel),
                enabled = dismissible, color = colors.muted, onClick = onCancel
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(colors.sand.copy(alpha = 0.6f)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Lock, null, tint = colors.muted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.login_reset_secure_note),
                color = colors.muted, fontSize = 12.sp, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ResetSuccessContent(
    email: String, loading: Boolean, cooldown: Int, message: String?,
    closeLabel: String, dismissible: Boolean, onClose: () -> Unit,
    onOpenMail: () -> Unit, onResend: () -> Unit, onChangeEmail: () -> Unit
) {
    val colors = authPalette()
    val canResend = cooldown == 0 && !loading
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        ResetHeader(stringResource(R.string.login_reset_success_title), closeLabel, dismissible, onClose, success = true)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_reset_success_body),
            color = colors.muted, fontSize = 15.sp, lineHeight = 22.sp
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(colors.sand)
                .border(1.dp, colors.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Email, null, tint = colors.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                email, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(colors.sand.copy(alpha = 0.6f)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Info, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.login_reset_spam_tip),
                color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp
            )
        }
        ResetMessage(message)
        Spacer(Modifier.height(20.dp))
        GradientActionButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.login_reset_open_mail),
            enabled = true,
            loading = false,
            leadingIcon = Icons.Filled.Email,
            onClick = onOpenMail
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ResetTextAction(
                label = stringResource(R.string.login_reset_change_email),
                enabled = !loading, color = colors.muted, onClick = onChangeEmail
            )
            if (loading) {
                Box(Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.accent, strokeWidth = 2.dp)
                }
            } else {
                ResetTextAction(
                    label = if (cooldown > 0)
                        stringResource(R.string.login_reset_resend_in, String.format(Locale.ROOT, "0:%02d", cooldown))
                    else stringResource(R.string.login_reset_resend),
                    enabled = canResend, color = colors.accent, onClick = onResend
                )
            }
        }
    }
}

@Composable
private fun ResetMessage(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(200)) + expandVertically(),
        exit = fadeOut(tween(150)) + shrinkVertically()
    ) {
        AuthNotice(message)
    }
}

@Composable
private fun ResetTextAction(label: String, enabled: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp)
            .pressScale(0.97f) { if (enabled) onClick() }
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (enabled) color else authPalette().muted.copy(alpha = 0.7f),
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GradientActionButton(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean,
    loading: Boolean,
    loadingLabel: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val onGradient = AuthTokens.OnGradient
    Box(
        modifier = modifier.height(54.dp)
            .pressScale { if (enabled && !loading) onClick() }
            .semantics { if (!enabled) disabled() }
            .alpha(if (enabled || loading) 1f else 0.5f)
            .clip(RoundedCornerShape(50))
            .background(AuthTokens.Gradient),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = onGradient, strokeWidth = 2.dp)
                if (loadingLabel != null) {
                    Text(loadingLabel, color = onGradient, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                if (leadingIcon != null) Icon(leadingIcon, null, tint = onGradient, modifier = Modifier.size(18.dp))
                Text(label, color = onGradient, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (trailingIcon != null) Icon(trailingIcon, null, tint = onGradient, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun openMailApp(context: Context): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
    true
} catch (_: Exception) {
    false
}
