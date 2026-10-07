package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun SignupContent(
    email: String = "", password: String = "", confirmPassword: String = "",
    passwordStep: Boolean = false, loading: Boolean = false, online: Boolean = true,
    entrancePlayed: Boolean = false, emailError: String? = null,
    passwordError: String? = null, confirmError: String? = null,
    error: String? = null, info: String? = null, keyboardPreview: Boolean = false,
    emailFocusRequester: FocusRequester? = null,
    onEmail: (String) -> Unit = {}, onPassword: (String) -> Unit = {}, onConfirm: (String) -> Unit = {},
    onEmailBlur: () -> Unit = {}, onPasswordBlur: () -> Unit = {}, onConfirmBlur: () -> Unit = {},
    onContinue: () -> Unit = {}, onBack: () -> Unit = {}, onGoogle: () -> Unit = {},
    onGuest: () -> Unit = {}, onLogin: () -> Unit = {}, onHaveCode: () -> Unit = {}
) {
    val colors = authPalette()
    val confirmFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    AuthScaffold(
        online = online, keyboardPreview = keyboardPreview,
        primary = {
            AuthEntrance(3, entrancePlayed) {
                AuthAction(stringResource(if (loading) R.string.signup_creating_account
                    else if (passwordStep) R.string.signup_create_account else R.string.auth_continue_email),
                    loading, onClick = onContinue)
            }
        },
        footer = { spec ->
            // Legal links retain the existing expanded 48dp hit logic and custom actions.
            LegalConsentText(colors.muted, colors.ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AuthLink(stringResource(R.string.auth_explore_guest), enabled = !loading, modifier = Modifier.weight(1f), onClick = onGuest)
                if (spec.tier == AuthHeightTier.Compact) AuthLink(stringResource(R.string.auth_login_footer),
                    enabled = !loading, modifier = Modifier.weight(1f), onClick = onLogin)

            }
            if (spec.tier != AuthHeightTier.Compact) AuthLink(stringResource(R.string.auth_login_footer), enabled = !loading,
                modifier = Modifier.fillMaxWidth(), onClick = onLogin)
        }
    ) { spec ->
        if (!passwordStep && !spec.keyboard && online && error == null && emailError == null && passwordError == null && confirmError == null) {
            AuthHeadline(stringResource(R.string.signup_headline_prefix), stringResource(R.string.auth_online),
                spec.tier == AuthHeightTier.Compact)
            AuthFitText(stringResource(R.string.auth_signup_subtitle), Modifier.fillMaxWidth().height(32.dp), maxSize = 12, maxLines = 2)
            if (spec.tier != AuthHeightTier.Compact) Spacer(Modifier.height(spec.gap))
        }
        if (!passwordStep) {
            if (!spec.keyboard) {
                AuthEntrance(0, entrancePlayed) {
                    AuthAction(stringResource(R.string.signup_continue_google), loading = loading, google = true, onClick = onGoogle)
                }
                AuthOr()
            }
            AuthEntrance(1, entrancePlayed) {
                Column {
                    AuthEmailField(email, onEmail, !loading, emailError, onEmailBlur, onContinue, emailFocusRequester)
                }
            }
        } else {
            if (!spec.minimal) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AuthLink(stringResource(R.string.auth_change_email), enabled = !loading, onClick = onBack)
                AuthLink(stringResource(R.string.verify_have_code), enabled = !loading, onClick = onHaveCode)
            }
            AuthPasswordField(password, onPassword, !loading, stringResource(R.string.signup_password_hint),
                passwordError, onPasswordBlur, { confirmFocus.requestFocus() }, newPassword = true, next = true)
            Spacer(Modifier.height(8.dp))
            AuthPasswordField(confirmPassword, onConfirm, !loading, stringResource(R.string.signup_confirm_hint),
                confirmError, onConfirmBlur, { focus.clearFocus(); onContinue() },
                newPassword = true, focusRequester = confirmFocus)
            if (password.isNotEmpty() && password == confirmPassword && confirmError == null) {
                AuthNotice(stringResource(R.string.signup_passwords_match), isError = false)
            }
        }
        AuthNotice(error)
        AuthNotice(info, isError = false)
        Spacer(Modifier.height(8.dp))
    }
}
