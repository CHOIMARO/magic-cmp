package com.company.core.media

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================
// Color matrices of the transitions for Media3 (F08).
// Media3 multiplies a column-major 4x4 matrix with (r, g, b, 1). The last column holds the offsets in 0..1.
// The hue and saturation values are the W3C Filter Effects matrices, as in the CSS of the prototype.
// =====================================================================

/** Column-major 4x4 matrix that keeps the colors. */
internal val IDENTITY_COLOR_MATRIX: FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

/**
 * Color matrix of a live scene: the hue rotation, then the saturation, then the opacity on black.
 *
 * Opacity on black multiplies the colors by the alpha.
 *
 * @param look Look of the scene.
 * @return A column-major 4x4 matrix for Media3.
 */
internal fun sceneColorMatrix(look: SceneLook): FloatArray {
    // 3x3 행렬은 행 우선으로 계산한다. 나중 단계를 왼쪽에 곱한다.
    val color = multiply3(saturationMatrix(look.saturation), hueMatrix(look.hueDegrees))
    for (i in color.indices) color[i] *= look.alpha
    return floatArrayOf(
        color[0], color[3], color[6], 0f,
        color[1], color[4], color[7], 0f,
        color[2], color[5], color[8], 0f,
        0f, 0f, 0f, 1f,
    )
}

/**
 * Color matrix that mixes the colors with the cover color.
 *
 * @param cover Color on top.
 * @param alpha Opacity of the cover, from 0 to 1.
 * @return A column-major 4x4 matrix for Media3.
 */
internal fun coverColorMatrix(cover: CoverColor, alpha: Float): FloatArray {
    val keep = 1f - alpha
    val offset = when (cover) {
        CoverColor.BLACK -> 0f
        CoverColor.WHITE -> alpha
    }
    return floatArrayOf(
        keep, 0f, 0f, 0f,
        0f, keep, 0f, 0f,
        0f, 0f, keep, 0f,
        offset, offset, offset, 1f,
    )
}

/** Row-major 3x3 hue rotation matrix. */
private fun hueMatrix(degrees: Float): FloatArray {
    val radians = degrees * PI.toFloat() / 180f
    val c = cos(radians)
    val s = sin(radians)
    return floatArrayOf(
        0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f,
        0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f,
        0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f,
    )
}

/** Row-major 3x3 saturation matrix. */
private fun saturationMatrix(value: Float): FloatArray = floatArrayOf(
    0.213f + 0.787f * value, 0.715f - 0.715f * value, 0.072f - 0.072f * value,
    0.213f - 0.213f * value, 0.715f + 0.285f * value, 0.072f - 0.072f * value,
    0.213f - 0.213f * value, 0.715f - 0.715f * value, 0.072f + 0.928f * value,
)

/** Returns `a × b` for two row-major 3x3 matrices. The result applies [b] first. */
private fun multiply3(a: FloatArray, b: FloatArray): FloatArray {
    val result = FloatArray(9)
    for (row in 0 until 3) {
        for (col in 0 until 3) {
            var sum = 0f
            for (k in 0 until 3) sum += a[row * 3 + k] * b[k * 3 + col]
            result[row * 3 + col] = sum
        }
    }
    return result
}
