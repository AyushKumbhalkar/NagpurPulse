package com.nagpurpulse.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.*
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nagpurpulse.ui.theme.authPalette

/** Resource lookup allows builds with the optional illustrations absent. */
@Composable
internal fun AuthArtwork(name: String, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val context = LocalContext.current
    val colors = authPalette()
    val id = remember(context, name) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
    Layout(modifier = modifier, content = {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
        .background(Brush.verticalGradient(listOf(colors.sand, colors.background))).alpha(alpha)) {
        if (id != 0) Image(
            painterResource(id), contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
    }, measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
            val child = measurables.single().measure(constraints)
            return layout(child.width, child.height) { child.place(0, 0) }
        }
        override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
        override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) = 0
    })
}
