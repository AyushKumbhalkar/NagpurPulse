package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R

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
    val passwordFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    AuthScaffold(
        online = online, keyboardPreview = keyboardPreview,
        primary = {
            AuthEntrance(3) {
                AuthAction(stringResource(if (loading) R.string.login_signing_in else R.string.login_button),
                    loading, onClick = onLogin)
            }
        },
        footer = { _ ->
            AuthOr()
            AuthAction(stringResource(R.string.login_continue_google), loading, google = true, onClick = onGoogle)
            Row(Modifier.fillMaxWidth()) {
                AuthLink(stringResource(R.string.auth_signup_footer), enabled = !loading,
                    modifier = Modifier.weight(2f), onClick = onSignup)
                AuthLink(stringResource(R.string.auth_explore_guest), enabled = !loading,
                    modifier = Modifier.weight(1f), onClick = onGuest)
            }
        }
    ) { spec ->
        if (!spec.keyboard && online && error == null && emailError == null && passwordError == null && !verificationRequired) {
            AuthHeadline(stringResource(R.string.login_headline_prefix).trim(), stringResource(R.string.auth_talking),
                spec.tier == AuthHeightTier.Compact)
            AuthFitText(stringResource(R.string.login_welcome_back))
            Spacer(Modifier.height(spec.gap))
        }
        AuthEntrance(1) {
            Column {
                AuthEmailField(email, onEmail, !loading, emailError, onEmailBlur, { passwordFocus.requestFocus() })
            }
        }
        Spacer(Modifier.height(8.dp))
        AuthEntrance(2) {
            Column {
                AuthPasswordField(password, onPassword, !loading, stringResource(R.string.login_password_hint),
                    passwordError, onPasswordBlur, { focus.clearFocus(); onLogin() }, focusRequester = passwordFocus)
            }
        }
        AuthNotice(error)
        AuthNotice(info, isError = false)
        if (verificationRequired) AuthLink(stringResource(R.string.login_resend_verification), enabled = !loading, onClick = onResend)
        if (!spec.minimal) AuthLink(stringResource(R.string.login_forgot_password), enabled = !loading,
            modifier = Modifier.fillMaxWidth(), onClick = onForgot)
        Spacer(Modifier.height(8.dp))
    }
}
