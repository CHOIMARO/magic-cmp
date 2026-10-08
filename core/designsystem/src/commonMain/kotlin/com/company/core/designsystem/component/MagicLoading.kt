package com.company.core.designsystem.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** CSS ease-in-out. */
private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

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

/**
 * Band of light that moves across the parent. One pass takes 1.4 seconds.
 *
 * Put it in a clipped box. Use it only while magic decorates the video.
 *
 * @param color Color of the light.
 */
@Composable
fun BoxScope.MagicSweep(color: Color) {
    val progress by rememberInfiniteTransition().animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOut), RepeatMode.Restart),
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { translationX = progress * size.width }
            .background(
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.25f to Color.Transparent,
                    0.5f to color,
                    0.75f to Color.Transparent,
                    1f to Color.Transparent,
                )
            )
    )
}

/**
 * Sparkle that grows, turns, and glows. One cycle takes 1.2 seconds.
 *
 * @param color Color of the sparkle.
 * @param modifier Modifier for the sparkle.
 * @param size Size of the sparkle.
 */
@Composable
fun MagicTwinkle(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    val progress by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
    )
    Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = color,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val scale = 0.8f + 0.32f * progress
                scaleX = scale
                scaleY = scale
                rotationZ = 18f * progress
                alpha = 0.6f + 0.4f * progress
            },
    )
}
