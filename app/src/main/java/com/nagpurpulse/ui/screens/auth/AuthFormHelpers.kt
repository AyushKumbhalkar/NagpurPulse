@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package com.nagpurpulse.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
//  Password strength
// ─────────────────────────────────────────────────────────────────────────────

/** Order matters: the enum is compared with <= / >= in the UI. */
enum class PasswordStrength(val label: String, val filledSegments: Int) {
    Empty("", 0),
    Weak("Weak", 1),
    Fair("Fair", 2),
    Good("Good", 3),
    Strong("Strong", 4)
}

private val CommonPasswords = setOf(
    "password", "password1", "password12", "password123", "passw0rd",
    "12345678", "123456789", "1234567890", "11111111", "00000000",
    "qwertyui", "qwerty123", "qwertyuiop", "abc12345", "abcd1234",
    "iloveyou", "welcome1", "letmein1", "admin123", "india123",
    "nagpur123", "nagpurpulse", "nagpurpulse1", "nagpurpulse123"
)

/**
 * Simple, offline heuristic (this is a hint for the user, not a security gate;
 * the real policy is enforced by Supabase).
 *
 * - under 8 characters, a very common password, a password built from the
 *   email name, or one that repeats 1-2 characters  -> Weak
 * - otherwise score = character classes (a-z, A-Z, 0-9, symbols)
 *   + 1 for 12+ characters + 1 for 16+ characters
 * - "Strong" additionally requires 12+ characters.
 */
fun evaluatePasswordStrength(password: String, email: String = ""): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.Empty
    if (password.length < 8) return PasswordStrength.Weak

    val lower = password.lowercase()
    if (lower in CommonPasswords) return PasswordStrength.Weak
    if (password.toSet().size <= 2) return PasswordStrength.Weak

    val emailName = email.substringBefore('@').trim().lowercase()
    if (emailName.length >= 4 && lower.contains(emailName)) return PasswordStrength.Weak

    var classes = 0
    if (password.any { it.isLowerCase() }) classes++
    if (password.any { it.isUpperCase() }) classes++
    if (password.any { it.isDigit() }) classes++
    if (password.any { !it.isLetterOrDigit() }) classes++

    var score = classes
    if (password.length >= 12) score++
    if (password.length >= 16) score++

    return when {
        score <= 1 -> PasswordStrength.Weak
        score == 2 -> PasswordStrength.Fair
        score == 3 -> PasswordStrength.Good
        password.length >= 12 -> PasswordStrength.Strong
        else -> PasswordStrength.Good
    }
}

private fun strengthColor(strength: PasswordStrength): Color = when (strength) {
    PasswordStrength.Empty -> Color.Transparent
    PasswordStrength.Weak -> Color(0xFFE5484D)
    PasswordStrength.Fair -> Color(0xFFF59E0B)
    PasswordStrength.Good -> Color(0xFF65A30D)
    PasswordStrength.Strong -> Color(0xFF16A34A)
}

/**
 * Four-segment strength bar with a text label ("Weak" ... "Strong").
 * The label means colour is never the only signal, and TalkBack reads one
 * clear description instead of five separate pieces.
 */
@Composable
fun PasswordStrengthMeter(
    strength: PasswordStrength,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val activeColor by animateColorAsState(
        targetValue = strengthColor(strength),
        animationSpec = tween(220),
        label = "password-strength-color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "Password strength: ${strength.label}"
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < strength.filledSegments) activeColor else trackColor)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = strength.label,
            color = activeColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Inline field message (error / hint) that animates in and out
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Shows [message] below a field, or nothing when it is null. The height change
 * is animated, and screen readers announce the message when it appears.
 */
@Composable
fun AuthFieldMessage(
    message: String?,
    color: Color,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = message,
        transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
        modifier = modifier.fillMaxWidth(),
        label = "auth-field-message"
    ) { text ->
        if (text != null) {
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier
                    .padding(start = 10.dp, top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        } else {
            Spacer(Modifier.height(0.dp))
        }
    }
}

/**
 * Everything shown under the password field: the strength bar, plus one line of
 * text that is either the validation error (when [errorMessage] is set) or a
 * tip while the password is still Weak/Fair.
 */
@Composable
internal fun StrengthSection(
    password: String,
    email: String,
    trackColor: Color,
    hintColor: Color,
    errorColor: Color,
    errorMessage: String?
) {
    val strength = evaluatePasswordStrength(password, email)
    Column(modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 8.dp)) {
        if (strength != PasswordStrength.Empty) {
            PasswordStrengthMeter(strength = strength, trackColor = trackColor)
        }
        val hint = when {
            errorMessage != null -> errorMessage
            strength != PasswordStrength.Empty && strength <= PasswordStrength.Fair ->
                "Tip: use 12+ characters with letters, numbers and symbols."
            else -> null
        }
        AnimatedContent(
            targetState = hint,
            transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
            label = "password-hint"
        ) { text ->
            if (text != null) {
                Text(
                    text = text,
                    color = if (errorMessage != null) errorColor else hintColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            } else {
                Spacer(Modifier.height(0.dp))
            }
        }
    }
}