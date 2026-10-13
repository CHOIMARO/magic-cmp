package com.company.core.media

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

// =====================================================================
// Size, crop, and blur of the still frames of the transitions (F08).
// It uses only Kotlin types, so the host tests can check it.
// =====================================================================

/** Longest side of a still frame in pixels. A larger video area shows a scaled still. */
internal const val STILL_MAX_SIDE = 1920

/** Blur radius in pixels of the still bitmap. A stronger blur makes the bitmap smaller, and the overlay scales it up. */
private const val STILL_BLUR_RADIUS = 4f

/** Number of box blur passes that make a Gaussian blur. */
private const val BOX_BLUR_PASSES = 3

/**
 * Size of a part of an image.
 *
 * @property width Width in pixels.
 * @property height Height in pixels.
 */
internal data class PixelSize(val width: Int, val height: Int)

/**
 * Size of the center part of a source image that the video area shows.
 *
 * Media3 first fills the frame and cuts the rest off. It then fills the area in the same way. Both cuts keep the
 * center.
 *
 * @param sourceWidth Width of the source image.
 * @param sourceHeight Height of the source image.
 * @param frameAspect Width divided by height of the output frame.
 * @param areaAspect Width divided by height of the video area.
 * @return The width and the height of the part, in source pixels.
 */
internal fun stillCropSize(sourceWidth: Int, sourceHeight: Int, frameAspect: Float, areaAspect: Float): Pair<Float, Float> {
    val (frameWidth, frameHeight) = centerCrop(sourceWidth.toFloat(), sourceHeight.toFloat(), frameAspect)
    return centerCrop(frameWidth, frameHeight, areaAspect)
}

/**
 * Size of a still bitmap for the video area.
 *
 * The still has the area size, but its longest side is not more than [STILL_MAX_SIDE]. A blurred still is smaller,
 * so that its blur radius is near [STILL_BLUR_RADIUS] pixels.
 *
 * @param areaWidth Width of the video area in pixels.
 * @param areaHeight Height of the video area in pixels.
 * @param blurRadius Blur radius in pixels of the video area. 0 means no blur.
 */
internal fun stillBitmapSize(areaWidth: Int, areaHeight: Int, blurRadius: Float): PixelSize {
    val capScale = minOf(1f, STILL_MAX_SIDE.toFloat() / max(areaWidth, areaHeight))
    val blurScale = if (blurRadius * capScale > STILL_BLUR_RADIUS) STILL_BLUR_RADIUS / (blurRadius * capScale) else 1f
    val scale = capScale * blurScale
    return PixelSize(
        width = (areaWidth * scale).roundToInt().coerceAtLeast(1),
        height = (areaHeight * scale).roundToInt().coerceAtLeast(1),
    )
}

/**
 * Radii of the box blurs that together make a Gaussian blur.
 *
 * @param sigma Standard deviation of the Gaussian blur in pixels.
 * @return One radius for each pass. A radius of 0 changes nothing.
 */
internal fun gaussianBoxRadii(sigma: Float): IntArray {
    if (sigma <= 0f) return IntArray(BOX_BLUR_PASSES)
    val n = BOX_BLUR_PASSES
    // 같은 폭의 상자 흐림을 n번 하면 가우스 흐림에 가깝다. 두 가지 폭을 섞어서 표준 편차를 맞춘다.
    val idealWidth = sqrt(12f * sigma * sigma / n + 1f)
    var lower = floor(idealWidth).toInt()
    if (lower % 2 == 0) lower--
    lower = lower.coerceAtLeast(1)
    val upper = lower + 2
    val idealLowerCount = (12f * sigma * sigma - n * lower * lower - 4f * n * lower - 3f * n) / (-4f * lower - 4f)
    val lowerCount = idealLowerCount.roundToInt().coerceIn(0, n)
    return IntArray(n) { pass -> ((if (pass < lowerCount) lower else upper) - 1) / 2 }
}

/**
 * Blurs ARGB pixels with box blurs. The edge pixels repeat outside the image.
 *
 * @param pixels Pixels in rows. The function changes them.
 * @param width Width of the image.
 * @param height Height of the image.
 * @param radii Radius of each box blur pass.
 */
internal fun boxBlur(pixels: IntArray, width: Int, height: Int, radii: IntArray) {
    val buffer = IntArray(pixels.size)
    for (radius in radii) {
        if (radius < 1) continue
        for (y in 0 until height) blurLine(pixels, buffer, start = y * width, step = 1, count = width, radius = radius)
        for (x in 0 until width) blurLine(buffer, pixels, start = x, step = width, count = height, radius = radius)
    }
}

/** Box blur of one line of pixels from [source] to [target]. */
private fun blurLine(source: IntArray, target: IntArray, start: Int, step: Int, count: Int, radius: Int) {
    val size = 2 * radius + 1
    val half = size / 2
    var a = 0
    var r = 0
    var g = 0
    var b = 0
    for (k in -radius..radius) {
        val pixel = source[start + k.coerceIn(0, count - 1) * step]
        a += pixel ushr 24
        r += (pixel shr 16) and 0xFF
        g += (pixel shr 8) and 0xFF
        b += pixel and 0xFF
    }
    for (i in 0 until count) {
        target[start + i * step] =
            ((a + half) / size shl 24) or ((r + half) / size shl 16) or ((g + half) / size shl 8) or ((b + half) / size)
        // 창을 한 칸 옮긴다. 나가는 픽셀을 빼고 들어오는 픽셀을 더한다.
        val leaving = source[start + (i - radius).coerceIn(0, count - 1) * step]
        val entering = source[start + (i + radius + 1).coerceIn(0, count - 1) * step]
        a += (entering ushr 24) - (leaving ushr 24)
        r += ((entering shr 16) and 0xFF) - ((leaving shr 16) and 0xFF)
        g += ((entering shr 8) and 0xFF) - ((leaving shr 8) and 0xFF)
        b += (entering and 0xFF) - (leaving and 0xFF)
    }
}

/** Size of the largest center part of a [width] × [height] image with the aspect [aspect]. */
private fun centerCrop(width: Float, height: Float, aspect: Float): Pair<Float, Float> =
    if (width / height > aspect) height * aspect to height else width to width / aspect
