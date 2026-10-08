package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun LoginContent(
    email: String = "", password: String = "", loading: Boolean = false, online: Boolean = true,
    emailError: String? = null, passwordError: String? = null, error: String? = null,
    info: String? = null, verificationRequired: Boolean = false, keyboardPreview: Boolean = false,
    onEmail: (String) -> Unit = {}, onPassword: (String) -> Unit = {},
    onEmailBlur: () -> Unit = {}, onPasswordBlur: () -> Unit = {},
    onLogin: () -> Unit = {}, onForgot: () -> Unit = {}, onGoogle: () -> Unit = {},
    onSignup: () -> Unit = {}, onResend: () -> Unit = {}, onGuest: () -> Unit = {}
) {
    val colors = authPalette()
    val passwordFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val fieldMessage = emailError ?: passwordError ?: error
    val hasProblem = fieldMessage != null || verificationRequired

    LoginScaffold(
        online = online, keyboardPreview = keyboardPreview,
        signupText = stringResource(R.string.auth_signup_footer),
        signupEnabled = !loading, onSignup = onSignup
    ) { keyboard, compact ->
        // Headline overlaps the wave, exactly like the mockup. Hidden while typing/erroring.
        if (!keyboard && !hasProblem) {
            Column(Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                AuthHeadline(stringResource(R.string.login_headline_prefix).trim(),
                    stringResource(R.string.auth_talking), compact, brightAccent = true, sizeSp = 38, lineDp = 44)
                Text(
                    stringResource(R.string.login_welcome_back), color = colors.ink,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = 0.dp)
                )
            }
            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))
        } else Spacer(Modifier.height(8.dp))

        // One joined card: email row, hairline, password row.
        val cardShape = RoundedCornerShape(24.dp)
        AuthEntrance(1) {
            Column(
                Modifier.fillMaxWidth()
                    .shadow(6.dp, cardShape, ambientColor = colors.accent.copy(alpha = 0.15f),
                        spotColor = colors.accent.copy(alpha = 0.15f))
                    .clip(cardShape).background(colors.surface.copy(alpha = 0.92f))
                    .border(1.dp, if (fieldMessage != null) colors.error.copy(alpha = 0.6f)
                        else colors.outline.copy(alpha = 0.25f), cardShape)
            ) {
                AuthEmailField(email, onEmail, !loading, emailError, onEmailBlur,
                    { passwordFocus.requestFocus() }, bare = true)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = colors.outline.copy(alpha = 0.25f))
                AuthPasswordField(password, onPassword, !loading, stringResource(R.string.login_password_hint),
                    passwordError, onPasswordBlur, { focus.clearFocus(); onLogin() },
                    focusRequester = passwordFocus, bare = true)
            }
        }

        // Error row (left) + "Forgot password?" (right, underlined) — as in panel 4.
        fieldMessage?.let { LoginError(it) }
        AuthNotice(info, isError = false)
        if (verificationRequired) AuthLink(stringResource(R.string.login_resend_verification),
            enabled = !loading, onClick = onResend)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                stringResource(R.string.login_forgot_password), color = colors.ink, fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.heightIn(min = 40.dp).wrapContentHeight(Alignment.CenterVertically)
                    .then(if (!loading) Modifier.clickableNoRipple(onForgot) else Modifier)
            )
        }

        Spacer(Modifier.height(8.dp))
        AuthEntrance(3) {
            AuthAction(stringResource(if (loading) R.string.login_signing_in else R.string.login_button),
                loading, contentColor = androidx.compose.ui.graphics.Color.White, height = 54.dp, onClick = onLogin)
        }
        Spacer(Modifier.height(16.dp))
        AuthOr()
        Spacer(Modifier.height(12.dp))
        AuthAction(stringResource(R.string.login_continue_google), loading, google = true, height = 48.dp, onClick = onGoogle)
    }
}

@Composable
private fun LoginError(message: String) {
    val colors = authPalette()
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(16.dp), tint = colors.error)
        Spacer(Modifier.width(8.dp))
        Text(message, color = colors.error, fontSize = 12.sp, maxLines = 3, modifier = Modifier.weight(1f))
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
}
