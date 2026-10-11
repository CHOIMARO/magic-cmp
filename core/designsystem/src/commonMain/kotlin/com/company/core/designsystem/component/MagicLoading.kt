package com.company.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview

/**
 * Spinner of the Magic Buttons spec: a ring with one colored quarter. One turn takes 0.8 seconds.
 *
 * @param color Color of the moving quarter.
 * @param trackColor Color of the ring.
 * @param modifier Modifier for the spinner.
 * @param size Size of the spinner.
 * @param strokeWidth Width of the ring.
 */
@Composable
fun MagicSpinner(
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    strokeWidth: Dp = 2.5.dp,
) {
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
    )
    Canvas(modifier = modifier.size(size).graphicsLayer { rotationZ = rotation }) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = Offset(stroke / 2, stroke / 2)
        drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
        drawArc(color, -135f, 90f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
    }
}

@Preview(name = "Loading", widthDp = 360)
@Composable
private fun MagicLoadingPreview() {
    MagicPreview {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagicSpinner(color = MagicColors.OnSurface, trackColor = MagicColors.OnSurface.copy(alpha = 0.18f))
            MagicSpinner(color = MagicColors.Accent, trackColor = MagicColors.SurfaceHigh, size = 24.dp)
        }
    }
}
