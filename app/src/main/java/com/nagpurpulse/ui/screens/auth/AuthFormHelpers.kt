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
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.nagpurpulse.R
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration

// ─────────────────────────────────────────────────────────────────────────────
//  Password strength
// ─────────────────────────────────────────────────────────────────────────────

/** Order matters: the enum is compared with <= / >= in the UI. */
enum class PasswordStrength(@StringRes val labelRes: Int, val filledSegments: Int) {
    Empty(0, 0),
    Weak(R.string.strength_weak, 1),
    Fair(R.string.strength_fair, 2),
    Good(R.string.strength_good, 3),
    Strong(R.string.strength_strong, 4)
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
    val strengthLabel = if (strength.labelRes != 0) stringResource(strength.labelRes) else ""
    val strengthDescription = stringResource(R.string.strength_cd, strengthLabel)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = strengthDescription
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
            text = strengthLabel,
            color = activeColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Inline field message (error / hint) that animates in and out
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
//  Email typo suggestion ("gmial.com" -> "gmail.com")
// ─────────────────────────────────────────────────────────────────────────────

/** Only well-known typos are corrected, so valid but uncommon domains are never "fixed". */
private val KNOWN_EMAIL_TYPOS = mapOf(
    "gmial.com" to "gmail.com", "gmai.com" to "gmail.com", "gamil.com" to "gmail.com",
    "gnail.com" to "gmail.com", "gmaill.com" to "gmail.com", "gmail.con" to "gmail.com",
    "gmail.co" to "gmail.com", "gmail.cm" to "gmail.com", "gmail.om" to "gmail.com",
    "gmail.comm" to "gmail.com", "gmail.vom" to "gmail.com", "gmail.cim" to "gmail.com",
    "yahooo.com" to "yahoo.com", "yaho.com" to "yahoo.com", "yahoo.con" to "yahoo.com",
    "hotmial.com" to "hotmail.com", "hotmail.con" to "hotmail.com", "hotmal.com" to "hotmail.com",
    "outlok.com" to "outlook.com", "outlook.con" to "outlook.com", "outllok.com" to "outlook.com",
    "iclod.com" to "icloud.com", "icloud.con" to "icloud.com",
    "rediffmail.con" to "rediffmail.com"
)

/** Returns a corrected full address when the domain is a known typo, otherwise null. */
fun suggestEmailCorrection(email: String): String? {
    val trimmed = email.trim()
    val at = trimmed.lastIndexOf('@')
    if (at <= 0 || at == trimmed.length - 1) return null
    val fixedDomain = KNOWN_EMAIL_TYPOS[trimmed.substring(at + 1).lowercase(java.util.Locale.ROOT)] ?: return null
    return trimmed.substring(0, at) + "@" + fixedDomain
}

// ─────────────────────────────────────────────────────────────────────────────
//  Open the user's email app (used by the verification dialog)
// ─────────────────────────────────────────────────────────────────────────────

/** Opens the default email app's inbox. Returns false when the phone has no email app. */
fun openEmailApp(context: Context): Boolean = try {
    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}