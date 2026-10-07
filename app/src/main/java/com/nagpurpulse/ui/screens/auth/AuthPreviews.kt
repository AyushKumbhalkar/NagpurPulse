package com.nagpurpulse.ui.screens.auth

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nagpurpulse.ui.theme.NagpurPulseTheme
import androidx.compose.ui.res.stringResource
import com.nagpurpulse.R

// Full Cartesian matrix; combined multipreview annotations alone would only form
// a union of sizes and font scales. No ViewModel, network, or keyboard side effects.
@Preview(name = "320x568 • 1.0 • no", widthDp = 320, heightDp = 568, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "320x568 • 1.0 • yes", widthDp = 320, heightDp = 568, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "320x568 • 1.3 • no", widthDp = 320, heightDp = 568, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "320x568 • 1.3 • yes", widthDp = 320, heightDp = 568, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "320x568 • 2.0 • no", widthDp = 320, heightDp = 568, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "320x568 • 2.0 • yes", widthDp = 320, heightDp = 568, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360x640 • 1.0 • no", widthDp = 360, heightDp = 640, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360x640 • 1.0 • yes", widthDp = 360, heightDp = 640, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360x640 • 1.3 • no", widthDp = 360, heightDp = 640, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360x640 • 1.3 • yes", widthDp = 360, heightDp = 640, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360x640 • 2.0 • no", widthDp = 360, heightDp = 640, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360x640 • 2.0 • yes", widthDp = 360, heightDp = 640, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360x800 • 1.0 • no", widthDp = 360, heightDp = 800, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360x800 • 1.0 • yes", widthDp = 360, heightDp = 800, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360x800 • 1.3 • no", widthDp = 360, heightDp = 800, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360x800 • 1.3 • yes", widthDp = 360, heightDp = 800, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "360x800 • 2.0 • no", widthDp = 360, heightDp = 800, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "360x800 • 2.0 • yes", widthDp = 360, heightDp = 800, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "412x915 • 1.0 • no", widthDp = 412, heightDp = 915, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "412x915 • 1.0 • yes", widthDp = 412, heightDp = 915, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "412x915 • 1.3 • no", widthDp = 412, heightDp = 915, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "412x915 • 1.3 • yes", widthDp = 412, heightDp = 915, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "412x915 • 2.0 • no", widthDp = 412, heightDp = 915, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "412x915 • 2.0 • yes", widthDp = 412, heightDp = 915, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "600x960 • 1.0 • no", widthDp = 600, heightDp = 960, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "600x960 • 1.0 • yes", widthDp = 600, heightDp = 960, fontScale = 1.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "600x960 • 1.3 • no", widthDp = 600, heightDp = 960, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "600x960 • 1.3 • yes", widthDp = 600, heightDp = 960, fontScale = 1.3f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "600x960 • 2.0 • no", widthDp = 600, heightDp = 960, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Preview(name = "600x960 • 2.0 • yes", widthDp = 600, heightDp = 960, fontScale = 2.0f, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
annotation class AuthDeviceMatrix

@AuthDeviceMatrix
@Composable
private fun WelcomePreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { WelcomeContent() }
}

@AuthDeviceMatrix
@Composable
private fun SignupPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { SignupContent() }
}

@AuthDeviceMatrix
@Composable
private fun LoginPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { LoginContent() }
}

@AuthDeviceMatrix
@Composable
private fun WelcomeKeyboardPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { WelcomeContent(keyboardPreview = true) }
}

@AuthDeviceMatrix
@Composable
private fun SignupKeyboardPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { SignupContent(email = "ayush@example.com", keyboardPreview = true) }
}

@AuthDeviceMatrix
@Composable
private fun LoginKeyboardPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { LoginContent(email = "ayush@example.com", password = "example123", keyboardPreview = true) }
}

@AuthDeviceMatrix
@Composable
private fun SignupPasswordPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { SignupContent(email = "ayush@example.com", passwordStep = true) }
}

@AuthDeviceMatrix
@Composable
private fun SignupPasswordKeyboardPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { SignupContent(passwordStep = true, keyboardPreview = true) }
}

@AuthDeviceMatrix
@Composable
private fun LoginErrorLoadingPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { LoginContent(email = "ayush@example.com", password = "example123", loading = true, error = stringResource(R.string.auth_err_enter_password), keyboardPreview = true) }
}

@AuthDeviceMatrix
@Composable
private fun SignupOfflinePreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { SignupContent(online = false) }
}

@AuthDeviceMatrix
@Composable
private fun SignupErrorPreview() {
    NagpurPulseTheme(darkTheme = isSystemInDarkTheme()) { SignupContent(emailError = stringResource(R.string.err_email_invalid)) }
}

@Preview(name = "Welcome hi compact", widthDp = 320, heightDp = 568, locale = "hi")
@Composable
private fun WelcomeHIPreview() {
    NagpurPulseTheme { WelcomeContent() }
}

@Preview(name = "Signup hi compact", widthDp = 320, heightDp = 568, locale = "hi")
@Composable
private fun SignupHIPreview() {
    NagpurPulseTheme { SignupContent() }
}

@Preview(name = "Login hi compact", widthDp = 320, heightDp = 568, locale = "hi")
@Composable
private fun LoginHIPreview() {
    NagpurPulseTheme { LoginContent() }
}

@Preview(name = "Welcome mr compact", widthDp = 320, heightDp = 568, locale = "mr")
@Composable
private fun WelcomeMRPreview() {
    NagpurPulseTheme { WelcomeContent() }
}

@Preview(name = "Signup mr compact", widthDp = 320, heightDp = 568, locale = "mr")
@Composable
private fun SignupMRPreview() {
    NagpurPulseTheme { SignupContent() }
}

@Preview(name = "Login mr compact", widthDp = 320, heightDp = 568, locale = "mr")
@Composable
private fun LoginMRPreview() {
    NagpurPulseTheme { LoginContent() }
}
