package com.company.ui.editor

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.company.core.domain.model.editor.FilterType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Color filter that gives the same result as the CSS filter of the design.
 *
 * @return The filter, or null for [FilterType.NONE].
 */
fun FilterType.toColorFilter(): ColorFilter? {
    val steps: List<FloatArray> = when (this) {
        FilterType.NONE -> return null
        FilterType.WARM -> listOf(sepia(0.35f), saturate(1.4f), hueRotate(-8f))
        FilterType.FADE -> listOf(contrast(0.8f), brightness(1.15f), saturate(0.6f))
        FilterType.COOL -> listOf(hueRotate(25f), saturate(1.2f), brightness(1.05f))
        FilterType.PUNCH -> listOf(contrast(1.4f), saturate(1.6f))
        FilterType.CINE -> listOf(contrast(1.25f), saturate(0.75f), brightness(0.9f), hueRotate(-12f))
        FilterType.MONO -> listOf(grayscale(1f), contrast(1.15f))
    }
    // CSS는 왼쪽 함수부터 차례로 적용한다. 행렬은 나중 단계를 왼쪽에 곱한다.
    val combined = steps.reduce { applied, next -> multiply(next, applied) }
    return ColorFilter.colorMatrix(ColorMatrix(combined))
}

// =====================================================================
// CSS filter functions as 4x5 color matrices (W3C Filter Effects).
// Offsets use the 0..255 range of Compose ColorMatrix.
// =====================================================================

private fun rgbMatrix(
    rr: Float, rg: Float, rb: Float,
    gr: Float, gg: Float, gb: Float,
    br: Float, bg: Float, bb: Float,
    offset: Float = 0f,
): FloatArray = floatArrayOf(
    rr, rg, rb, 0f, offset,
    gr, gg, gb, 0f, offset,
    br, bg, bb, 0f, offset,
    0f, 0f, 0f, 1f, 0f,
)

private fun sepia(amount: Float): FloatArray {
    val k = 1f - amount
    return rgbMatrix(
        0.393f + 0.607f * k, 0.769f - 0.769f * k, 0.189f - 0.189f * k,
        0.349f - 0.349f * k, 0.686f + 0.314f * k, 0.168f - 0.168f * k,
        0.272f - 0.272f * k, 0.534f - 0.534f * k, 0.131f + 0.869f * k,
    )
}

private fun grayscale(amount: Float): FloatArray {
    val k = 1f - amount
    return rgbMatrix(
        0.2126f + 0.7874f * k, 0.7152f - 0.7152f * k, 0.0722f - 0.0722f * k,
        0.2126f - 0.2126f * k, 0.7152f + 0.2848f * k, 0.0722f - 0.0722f * k,
        0.2126f - 0.2126f * k, 0.7152f - 0.7152f * k, 0.0722f + 0.9278f * k,
    )
}

private fun saturate(value: Float): FloatArray = rgbMatrix(
    0.213f + 0.787f * value, 0.715f - 0.715f * value, 0.072f - 0.072f * value,
    0.213f - 0.213f * value, 0.715f + 0.285f * value, 0.072f - 0.072f * value,
    0.213f - 0.213f * value, 0.715f - 0.715f * value, 0.072f + 0.928f * value,
)

private fun hueRotate(degrees: Float): FloatArray {
    val radians = degrees * PI.toFloat() / 180f
    val c = cos(radians)
    val s = sin(radians)
    return rgbMatrix(
        0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f,
        0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f,
        0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f,
    )
}

private fun brightness(value: Float): FloatArray = rgbMatrix(
    value, 0f, 0f,
    0f, value, 0f,
    0f, 0f, value,
)

private fun contrast(value: Float): FloatArray = rgbMatrix(
    value, 0f, 0f,
    0f, value, 0f,
    0f, 0f, value,
    offset = (0.5f - 0.5f * value) * 255f,
)

/** Returns `a × b` for two 4x5 color matrices. The result applies [b] first. */
private fun multiply(a: FloatArray, b: FloatArray): FloatArray {
    val result = FloatArray(20)
    for (row in 0 until 4) {
        for (col in 0 until 5) {
            var sum = if (col == 4) a[row * 5 + 4] else 0f
            for (k in 0 until 4) {
                sum += a[row * 5 + k] * b[k * 5 + col]
            }
            result[row * 5 + col] = sum
        }
    }
    return result
}
