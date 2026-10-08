@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun AuthEmailField(
    value: String, onValue: (String) -> Unit, enabled: Boolean,
    error: String?, onBlur: () -> Unit, onNext: () -> Unit,
    focusRequester: FocusRequester? = null,
    bare: Boolean = false,
    fieldHeight: androidx.compose.ui.unit.Dp? = null
) {
    PremiumInputField(
        value, onValue, stringResource(R.string.signup_email_hint),
        leadingIcon = { Icon(Icons.Outlined.Email, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        autofillTypes = listOf(AutofillType.EmailAddress),
        onBlur = onBlur, errorMessage = error, focusRequester = focusRequester,
        enabled = enabled, clearEmail = true, bare = bare, fieldHeight = fieldHeight
    )
    if (!bare) AuthNotice(error)
}

@Composable
internal fun AuthPasswordField(
    value: String, onValue: (String) -> Unit, enabled: Boolean,
    label: String, error: String?, onBlur: () -> Unit, onDone: () -> Unit,
    newPassword: Boolean = false, focusRequester: FocusRequester? = null,
    next: Boolean = false,
    bare: Boolean = false,
    fieldHeight: androidx.compose.ui.unit.Dp? = null
) {
    var visible by remember { mutableStateOf(false) }
    PremiumInputField(
        value, onValue, label,
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Icon(if (visible) (if (bare) Icons.Outlined.VisibilityOff else Icons.Filled.VisibilityOff) else (if (bare) Icons.Outlined.Visibility else Icons.Filled.Visibility),
                    stringResource(if (visible) R.string.cd_hide_password else R.string.cd_show_password),
                    tint = authPalette().ink)
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,
            imeAction = if (next) ImeAction.Next else ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }, onNext = { onDone() }),
        autofillTypes = listOf(if (newPassword) AutofillType.NewPassword else AutofillType.Password),
        onBlur = onBlur, errorMessage = error, enabled = enabled, focusRequester = focusRequester, bare = bare, fieldHeight = fieldHeight
    )
    if (!bare) AuthNotice(error)
}
