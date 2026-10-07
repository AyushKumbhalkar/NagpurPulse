package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nagpurpulse.R
import kotlin.math.abs

/** Single place to change the legal page URLs. */
object LegalLinks {
    const val TERMS_URL = "https://www.nagpurpulse.in/legal/terms.html"
    const val PRIVACY_URL = "https://www.nagpurpulse.in/legal/privacy.html"
}

/**
 * Consent sentence with two links. U7: the tap area is at least 48dp tall (extra vertical
 * padding is part of the touch area) and a tap that lands a few characters beside a link
 * still counts, so the small 12sp links are easy to hit. TalkBack gets two custom actions.
 */
@Composable
fun LegalConsentText(
    textColor: Color,
    linkColor: Color,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val density = LocalDensity.current
    val linkStyle = SpanStyle(
        color = linkColor,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline
    )

    val termsLabel = stringResource(R.string.legal_terms)
    val privacyLabel = stringResource(R.string.legal_privacy)
    val fullText = stringResource(R.string.legal_consent, termsLabel, privacyLabel)

    // Links are located by their label, so word order can differ per language.
    val links = listOf(
        termsLabel to LegalLinks.TERMS_URL,
        privacyLabel to LegalLinks.PRIVACY_URL
    ).mapNotNull { (label, url) ->
        val start = fullText.indexOf(label)
        if (start >= 0) Triple(start, start + label.length, url) else null
    }

    val annotated = buildAnnotatedString {
        append(fullText)
        links.forEach { (start, end, _) -> addStyle(linkStyle, start, end) }
    }

    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val touchPadding = 4.dp
    val paddingPx = with(density) { touchPadding.toPx() }
    val openUrl: (String) -> Unit = { url -> runCatching { uriHandler.openUri(url) } }

    BasicText(
        text = annotated,
        onTextLayout = { layout = it },
        style = TextStyle(
            color = textColor,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center
        ),
        modifier = modifier
            .fillMaxWidth()
            // pointerInput is BEFORE padding on purpose: the padding becomes touch area.
            .pointerInput(links) {
                detectTapGestures { pos: Offset ->
                    val l = layout ?: return@detectTapGestures
                    val offset = l.getOffsetForPosition(Offset(pos.x, pos.y - paddingPx))
                    // nearest link within 6 characters (0 = tapped right on it)
                    links.map { (start, end, url) ->
                        val distance = when {
                            offset < start -> start - offset
                            offset >= end -> offset - end + 1
                            else -> 0
                        }
                        distance to url
                    }.filter { abs(it.first) <= 6 }
                        .minByOrNull { it.first }
                        ?.let { openUrl(it.second) }
                }
            }
            .heightIn(min = 48.dp)
            .padding(vertical = touchPadding)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(termsLabel) { openUrl(LegalLinks.TERMS_URL); true },
                    CustomAccessibilityAction(privacyLabel) { openUrl(LegalLinks.PRIVACY_URL); true }
                )
            }
    )
}