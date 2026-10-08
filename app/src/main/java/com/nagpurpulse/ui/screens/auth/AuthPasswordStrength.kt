package com.nagpurpulse.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.authPalette

internal enum class PasswordStrength(val bars: Int) { Weak(1), Fair(2), Good(3), Strong(4) }

private val CommonPasswords = setOf(
    "password", "password1", "12345678", "123456789", "1234567890", "qwerty123", "qwertyuiop",
    "iloveyou", "11111111", "00000000", "abcd1234", "admin123", "welcome1", "nagpur123"
)

internal fun passwordStrength(p: String): PasswordStrength {
    if (p.length < 8 || p.lowercase() in CommonPasswords) return PasswordStrength.Weak
    var points = 0
    if (p.length >= 12) points++
    if (p.any(Char::isLowerCase) && p.any(Char::isUpperCase)) points++
    if (p.any(Char::isDigit)) points++
    if (p.any { !it.isLetterOrDigit() }) points++
    return when {
        points >= 3 -> PasswordStrength.Strong
        points == 2 -> PasswordStrength.Good
        else -> PasswordStrength.Fair
    }
}

/** Four-segment strength bar shown under the "new password" field. */
@Composable
internal fun PasswordStrengthMeter(password: String) {
    val colors = authPalette()
    val level = remember(password) { passwordStrength(password) }
    val levelColor = when (level) {
        PasswordStrength.Weak -> colors.error
        PasswordStrength.Fair -> Color(0xFFF59E0B)
        PasswordStrength.Good -> Color(0xFF65A30D)
        PasswordStrength.Strong -> colors.success
    }
    val label = stringResource(when (level) {
        PasswordStrength.Weak -> R.string.pw_strength_weak
        PasswordStrength.Fair -> R.string.pw_strength_fair
        PasswordStrength.Good -> R.string.pw_strength_good
        PasswordStrength.Strong -> R.string.pw_strength_strong
    })
    AnimatedVisibility(
        visible = password.isNotEmpty(),
        enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp)
                .semantics(mergeDescendants = true) { contentDescription = label },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..4) {
                    val c by animateColorAsState(
                        if (i <= level.bars) levelColor else colors.outline.copy(alpha = 0.25f), label = "pw-bar")
                    Box(Modifier.weight(1f).height(4.dp).background(c, RoundedCornerShape(50)))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(label, color = levelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
