package com.company.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an OKLCH color to an sRGB [Color].
 *
 * The design uses OKLCH values. This function gives the same colors in Compose.
 *
 * @param lightness Perceived lightness from 0 to 1.
 * @param chroma Color strength. The design uses values from 0 to about 0.25.
 * @param hue Hue angle in degrees.
 * @param alpha Opacity from 0 to 1.
 */
fun oklch(lightness: Float, chroma: Float, hue: Float, alpha: Float = 1f): Color {
    val radians = hue * kotlin.math.PI / 180.0
    val a = chroma * cos(radians)
    val b = chroma * sin(radians)

    val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3)
    val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3)
    val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3)

    val red = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val green = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val blue = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

    return Color(
        red = encodeSrgb(red),
        green = encodeSrgb(green),
        blue = encodeSrgb(blue),
        alpha = alpha,
    )
}

private fun encodeSrgb(linear: Double): Float {
    val value = linear.coerceIn(0.0, 1.0)
    val encoded = if (value <= 0.0031308) 12.92 * value else 1.055 * value.pow(1 / 2.4) - 0.055
    return encoded.toFloat().coerceIn(0f, 1f)
}
