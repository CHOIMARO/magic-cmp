package com.company.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.theme.oklch
import kotlin.math.sqrt

/** Chroma of the stripe placeholder colors. */
private const val STRIPE_CHROMA = 0.08f

/** Width of one stripe. Two stripes make one period. */
private val StripeWidth = 7.dp

/**
 * Placeholder thumbnail with diagonal stripes.
 *
 * The prototype has no real video. Each clip shows stripes in its own hue.
 *
 * @param hue Hue of the stripes in degrees.
 * @param modifier Modifier for the box.
 * @param lightness OKLCH lightness of the light stripe. The dark stripe is 0.06 darker.
 * @param shape Shape that clips the stripes.
 * @param colorFilter Optional filter for the stripes, for example a video filter preview.
 * @param stripeOffset Horizontal shift of the stripes. Use it to animate playback.
 * @param content Content on top of the stripes.
 */
@Composable
fun StripeBox(
    hue: Float,
    modifier: Modifier = Modifier,
    lightness: Float = 0.5f,
    shape: Shape = RectangleShape,
    colorFilter: ColorFilter? = null,
    stripeOffset: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = remember(hue, lightness) { stripeColors(hue, lightness) }
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind { drawStripes(colors, colorFilter, stripeOffset.toPx()) },
        content = content,
    )
}

/**
 * Returns the light and dark stripe colors for a hue.
 *
 * @param hue Hue in degrees.
 * @param lightness OKLCH lightness of the light stripe.
 */
fun stripeColors(hue: Float, lightness: Float = 0.5f): Pair<Color, Color> =
    oklch(lightness, STRIPE_CHROMA, hue) to oklch(lightness - 0.06f, STRIPE_CHROMA, hue)

/**
 * Draws 135-degree stripes that fill the draw area.
 *
 * @param colors Light and dark stripe colors.
 * @param colorFilter Optional color filter.
 * @param offsetX Horizontal shift in pixels.
 */
fun DrawScope.drawStripes(colors: Pair<Color, Color>, colorFilter: ColorFilter?, offsetX: Float = 0f) {
    val period = StripeWidth.toPx() * 2
    val step = period / sqrt(2f)
    val brush = Brush.linearGradient(
        0f to colors.first,
        0.5f to colors.first,
        0.5f to colors.second,
        1f to colors.second,
        start = Offset(offsetX, 0f),
        end = Offset(offsetX + step, step),
        tileMode = TileMode.Repeated,
    )
    drawRect(brush = brush, colorFilter = colorFilter)
}
