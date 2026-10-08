package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nagpurpulse.R
import com.nagpurpulse.ui.theme.AuthTokens
import com.nagpurpulse.ui.theme.authPalette

@Composable
internal fun AuthAction(text: String, loading: Boolean = false, enabled: Boolean = true, google: Boolean = false, contentColor: Color? = null, onClick: () -> Unit) {
    val colors = authPalette()
    val shape = RoundedCornerShape(24.dp)
    val ink = contentColor ?: if (google) colors.ink else AuthTokens.OnGradient
    Button(
        onClick = onClick, enabled = enabled && !loading, shape = shape,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent, contentColor = ink,
            disabledContainerColor = Color.Transparent, disabledContentColor = ink
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp).alpha(if (loading || !enabled) 0.65f else 1f)
            .clip(shape).then(if (google) Modifier.background(colors.surface) else Modifier.background(AuthTokens.Gradient))
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = ink, strokeWidth = 2.dp)
        else if (google) Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
        if (loading || google) Spacer(Modifier.width(8.dp))
        AuthFitText(text, Modifier.weight(1f, fill = false), color = ink, maxSize = 16, minSize = 11, weight = FontWeight.Bold)
        if (!google && !loading) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowForward, null, Modifier.size(20.dp))
        }
    }
}
