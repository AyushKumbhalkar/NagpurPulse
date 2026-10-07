//java/com/nagpurpulse/ui/screens/auth/LegalConsent.kt

package com.nagpurpulse.ui.screens.auth


import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/** Single place to change the legal page URLs. */
object LegalLinks {
    const val TERMS_URL = "https://YOUR-DOMAIN.com/terms"      // TODO: replace
    const val PRIVACY_URL = "https://YOUR-DOMAIN.com/privacy"  // TODO: replace
}

@Composable
fun LegalConsentText(
    textColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val linkStyle = SpanStyle(
        color = linkColor,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline
    )

    val annotated = buildAnnotatedString {
        append("By creating an account or continuing with Google, you agree to our ")
        pushStringAnnotation(tag = "URL", annotation = LegalLinks.TERMS_URL)
        withStyle(linkStyle) { append("Terms of Service") }
        pop()
        append(" and ")
        pushStringAnnotation(tag = "URL", annotation = LegalLinks.PRIVACY_URL)
        withStyle(linkStyle) { append("Privacy Policy") }
        pop()
        append(".")
    }

    ClickableText(
        text = annotated,
        modifier = modifier.fillMaxWidth(),
        style = TextStyle(
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        ),
        onClick = { offset ->
            annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { runCatching { uriHandler.openUri(it.item) } }
        }
    )
}