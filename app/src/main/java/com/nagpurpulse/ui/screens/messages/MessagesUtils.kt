// This is the MessageUtilis.kt file

// java/com/nagpurpulse/ui/screens/messages/MessagesUtils.kt

package com.nagpurpulse.ui.screens.messages

import androidx.compose.ui.graphics.Color
import com.nagpurpulse.ui.theme.*

fun incognitoColor(seed: String): Color {
    val hash = seed.fold(0L) { acc, c -> acc * 31 + c.code }
    val colors = listOf(OrangePrimary, PurpleNight, BlueInfo, GreenSuccess, PinkEvents, TealNeighborhood, RedAlert, Color(0xFFFFD60A))
    return colors[(hash.toInt().and(0x7FFFFFFF) % colors.size)]
}

fun incognitoEmoji(seed: String): String {
    val emojis = listOf("🦊","🐺","🦝","🐱","🦁","🐯","🦈","🦅","🦉","🐸","🦎","🐙","🦋","🦩","🦚","🐝","🦔","🦜","🦂","🦑")
    val hash = seed.fold(0L) { acc, c -> acc * 31 + c.code }
    return emojis[(hash.toInt().and(0x7FFFFFFF) % emojis.size)]
}
