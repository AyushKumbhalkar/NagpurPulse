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
import androidx.compose.ui.res.stringResource
import com.nagpurpulse.R

/** Single place to change the legal page URLs. */
object LegalLinks {
    const val TERMS_URL = "https://www.nagpurpulse.in/legal/terms.html"
    const val PRIVACY_URL = "https://www.nagpurpulse.in/legal/privacy.html"
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

    val termsLabel = stringResource(R.string.legal_terms)
    val privacyLabel = stringResource(R.string.legal_privacy)
    val fullText = stringResource(R.string.legal_consent, termsLabel, privacyLabel)

    val annotated = buildAnnotatedString {
        append(fullText)
        // Links are located by their label, so word order can differ per language.
        listOf(
            termsLabel to LegalLinks.TERMS_URL,
            privacyLabel to LegalLinks.PRIVACY_URL
        ).forEach { (label, url) ->
            val start = fullText.indexOf(label)
            if (start >= 0) {
                val end = start + label.length
                addStyle(linkStyle, start, end)
                addStringAnnotation(tag = "URL", annotation = url, start = start, end = end)
            }
        }
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