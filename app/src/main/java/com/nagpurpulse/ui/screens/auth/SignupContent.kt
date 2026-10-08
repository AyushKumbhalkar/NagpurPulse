@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.LocalIsDarkTheme
import com.nagpurpulse.ui.theme.authPalette

private enum class SignupTier { Compact, Medium, Expanded }

/**
 * Sign up screen (self-contained, same parameters as before, so SignupScreen.kt and the previews
 * need no change). Layout rules:
 *  - header art (res/drawable/new_header.png) is the ONLY flexible element and absorbs spare height
 *  - footer art (res/drawable/new_footer.png) is a background layer: it takes no layout height
 *  - nothing scrolls at normal font scale; the keyboard hides the art, Google button and legal text
 *  - font scale above 1.3 falls back to a scrolling column (accessibility only)
 */
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
    val dark = LocalIsDarkTheme.current
    val font = rememberWelcomeFont()
    val confirmFocus = remember { FocusRequester() }
    val focus = LocalFocusManager.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    val keyboard = WindowInsets.isImeVisible || keyboardPreview
    // Looked up by name: a missing file just hides that artwork instead of breaking the build.
    val headerArt = rememberSignupDrawable("new_header")
    val footerArt = rememberSignupDrawable("new_footer")
    WelcomeSystemBars()

    Box(Modifier.fillMaxSize().background(colors.background)) {
        // Footer art: full-bleed behind everything, under the navigation bar too.
        if (footerArt != 0 && !keyboard && !largeText) {
            Image(
                painterResource(footerArt), contentDescription = null,
                contentScale = ContentScale.FillWidth, alignment = Alignment.BottomCenter,
                alpha = if (dark) 0.5f else 1f,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().heightIn(max = 210.dp)
            )
        }

        Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .navigationBarsPadding().imePadding()
                .padding(bottom = if (keyboardPreview) 260.dp else 0.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
                val tier = when {
                    maxHeight < 640.dp -> SignupTier.Compact
                    maxHeight <= 780.dp -> SignupTier.Medium
                    else -> SignupTier.Expanded
                }
                val compact = tier == SignupTier.Compact
                val minimal = keyboard && maxHeight < 440.dp
                val headlineSp = when (tier) { SignupTier.Compact -> 30; SignupTier.Medium -> 38; SignupTier.Expanded -> 44 }
                val controlHeight = when (tier) { SignupTier.Compact -> 48.dp; SignupTier.Medium -> 52.dp; SignupTier.Expanded -> 56.dp }

                Column(
                    Modifier.fillMaxSize().then(if (largeText) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!minimal) WelcomeHeader(font)
                    if (!online) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            AuthNotice(stringResource(R.string.auth_offline), isError = false)
                        }
                    }

                    // Header art: flexible. A soft peach sky sits behind the transparent PNG.
                    if (!keyboard && !largeText) {
                        Box(
                            Modifier.fillMaxWidth().weight(1f).background(
                                Brush.verticalGradient(
                                    listOf(
                                        colors.background,
                                        if (dark) Color(0xFF2A211B) else Color(0xFFFDEBD6),
                                        colors.background
                                    )
                                )
                            ),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (headerArt != 0) {
                                Image(
                                    painterResource(headerArt), contentDescription = null,
                                    contentScale = ContentScale.FillWidth, alignment = Alignment.BottomCenter,
                                    alpha = if (dark) 0.6f else 1f,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.height(4.dp))
                    }

                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!passwordStep && !keyboard) {
                            AuthEntrance(0, entrancePlayed) {
                                WelcomeHeadline(
                                    first = stringResource(R.string.signup_headline_prefix),
                                    accent = stringResource(R.string.auth_online),
                                    baseSp = headlineSp,
                                    brush = if (dark) AuthTokens.Gradient else WelcomeAccentBrushLight,
                                    inkColor = colors.ink, font = font, limitLines = !largeText
                                )
                            }
                            if (!compact) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.auth_signup_subtitle), Modifier.fillMaxWidth(),
                                    color = colors.muted, fontSize = 15.sp, lineHeight = 21.sp, fontFamily = font,
                                    textAlign = TextAlign.Center, maxLines = if (largeText) Int.MAX_VALUE else 2
                                )
                            }
                            Spacer(Modifier.height(if (compact) 8.dp else 14.dp))
                        }

                        if (!passwordStep) {
                            if (!keyboard) {
                                AuthEntrance(1, entrancePlayed) {
                                    SignupGoogleButton(stringResource(R.string.signup_continue_google), loading, controlHeight, font, onGoogle)
                                }
                                AuthOr()
                            }
                            AuthEntrance(2, entrancePlayed) {
                                Column {
                                    AuthEmailField(email, onEmail, !loading, emailError, onEmailBlur, onContinue, emailFocusRequester)
                                }
                            }
                        } else {
                            if (!minimal) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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

                        AuthEntrance(3, entrancePlayed) {
                            WelcomeButton(
                                stringResource(
                                    if (loading) R.string.signup_creating_account
                                    else if (passwordStep) R.string.signup_create_account else R.string.auth_continue_email
                                ),
                                controlHeight, font, onContinue, loading
                            )
                        }

                        if (!keyboard) {
                            // Legal links keep the existing 48dp hit logic and TalkBack actions.
                            LegalConsentText(colors.muted, colors.ink)
                            if (compact) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    SignupGuestLink(stringResource(R.string.auth_explore_guest), !loading, colors.ink, font, Modifier.weight(1f), onGuest)
                                    Box(Modifier.weight(1f)) {
                                        WelcomeLoginLink(stringResource(R.string.auth_login_footer), colors.muted, colors.ink, font, onLogin, !loading)
                                    }
                                }
                            } else {
                                SignupGuestLink(stringResource(R.string.auth_explore_guest), !loading, colors.ink, font, Modifier, onGuest)
                                WelcomeLoginLink(stringResource(R.string.auth_login_footer), colors.muted, colors.ink, font, onLogin, !loading)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberSignupDrawable(name: String): Int {
    val context = LocalContext.current
    return remember(context, name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
}

/** White pill with a soft shadow; the Google "G" becomes a spinner while a request is running. */
@Composable
private fun SignupGoogleButton(text: String, loading: Boolean, height: Dp, font: FontFamily, onClick: () -> Unit) {
    val colors = authPalette()
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.fillMaxWidth().height(height).alpha(if (loading) 0.65f else 1f)
            .shadow(2.dp, shape).clip(shape).background(colors.surface)
            .clickable(enabled = !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = colors.accent, strokeWidth = 2.dp)
        else Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        WelcomeFitText(text, Modifier.weight(1f, fill = false), colors.ink, maxSize = 17, minSize = 12, weight = FontWeight.Bold, font = font)
    }
}

@Composable
private fun SignupGuestLink(
    text: String, enabled: Boolean, ink: Color, font: FontFamily, modifier: Modifier, onClick: () -> Unit
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = font,
            textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center, maxLines = 2
        )
    }
}