package com.company.feature.main.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Timing and positions of the splash animation. The values come from the splash of Magic Editor v9.
 *
 * All times are in milliseconds from the start of the splash. A fraction is relative to the box of the "magic" word:
 * 0 is the left or top edge, and 1 is the right or bottom edge.
 */
internal object SplashTimeline {
    /** Full length of the splash. The home screen shows after this time. */
    const val TotalMillis = 2200

    /** Size of the "magic" word. */
    const val WordSizeDp = 68f

    /** Size of the pen star when it lands. */
    const val StarSizeDp = 21f

    /** Size of the flash when the pen lands. */
    const val FlashSizeDp = 46f

    /** Distance from the right edge of the word to the center of the landed pen. */
    const val LandOffsetDp = 11f

    /** Height of the landed pen, as a fraction of the word height. */
    const val LandYFraction = 0.21f

    // ===========================================================
    // Times
    // ===========================================================

    /** Time when the pen starts to move. */
    private const val PenStart = TotalMillis * 0.04f

    /** Time that the pen takes to go over the word. */
    private const val WriteMillis = TotalMillis * 0.58f

    /** Time when the pen lands next to the word. */
    private const val LandTime = PenStart + WriteMillis

    /** Time that the pen takes to grow and shrink when it lands. */
    private const val SettleMillis = TotalMillis * 0.12f

    /** Time that the pen takes to come into view. */
    private const val AppearMillis = TotalMillis * 0.1f

    /** Length of the flash. */
    private const val FlashMillis = TotalMillis * 0.2f

    /** Time when the splash starts to fade out. */
    private const val FadeStart = TotalMillis * 0.86f

    // ===========================================================
    // Easing (CSS timing functions of the prototype)
    // ===========================================================

    private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    private val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    private val LandEasing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
    private val SpinEasing = CubicBezierEasing(0.3f, 0.6f, 0.4f, 1f)
    private val SettleEasing = CubicBezierEasing(0.3f, 1.3f, 0.5f, 1f)

    // ===========================================================
    // Pen
    // ===========================================================

    /** Height of the pen in percent of the word height, at the percent of the write time. It goes up and down over the letters. */
    private val PenPath = listOf(
        0 to 78, 5 to 46, 9 to 78, 13 to 46, 17 to 78, 21 to 46, 26 to 78, 33 to 50, 39 to 78,
        45 to 50, 50 to 96, 56 to 56, 62 to 78, 66 to 28, 72 to 52, 80 to 78, 85 to 66,
    )

    private val PenX = listOf(Keyframe(0f, -0.02f), Keyframe(0.85f, 1f, LandEasing), Keyframe(1f, 1f))
    private val PenXOffset = listOf(Keyframe(0f, 0f), Keyframe(0.85f, 0f, LandEasing), Keyframe(1f, LandOffsetDp))
    private val PenY = PenPath.map { (at, y) ->
        Keyframe(at / 100f, y / 100f, if (at == 85) LandEasing else EaseInOut)
    } + Keyframe(1f, LandYFraction)
    private val SettleScale = listOf(Keyframe(0f, 1f, SettleEasing), Keyframe(0.45f, 1.9f, SettleEasing), Keyframe(1f, 4f / 3f))

    /**
     * Returns the pen star at [time].
     *
     * The pen comes into view at the left of the word and turns two times while it goes over the word. At the end it
     * grows, then lands at the top right of the word with the size [StarSizeDp].
     *
     * @param time Time in milliseconds from the start of the splash.
     */
    fun penAt(time: Float): PenFrame {
        val write = progressOf(time, PenStart, WriteMillis)
        val spin = SpinEasing.transform(progressOf(time, PenStart, WriteMillis + SettleMillis))
        val appear = EaseOut.transform(progressOf(time, PenStart, AppearMillis))
        val settle = SettleScale.valueAt(progressOf(time, LandTime, SettleMillis))
        return PenFrame(
            xFraction = PenX.valueAt(write),
            xOffsetDp = PenXOffset.valueAt(write),
            yFraction = PenY.valueAt(write),
            rotation = -720f + 720f * spin,
            scale = (0.3f + 0.45f * appear) * settle,
            alpha = appear,
        )
    }

    // ===========================================================
    // Word, flash, and fade
    // ===========================================================

    /**
     * Returns the progress from 0 to 1 of the mask that shows the word from left to right.
     *
     * @param time Time in milliseconds from the start of the splash.
     */
    fun revealAt(time: Float): Float = progressOf(time, PenStart + WriteMillis * 0.04f, WriteMillis)

    /**
     * Returns the edges of the mask over the word box, as fractions of the box width.
     *
     * The word is visible to the left of the first value and hidden to the right of the second value. The prototype
     * uses a CSS mask that is 2.2 times as wide as the box. The mask is opaque to 50 % and clear from 54.5 %, and it
     * moves from 100 % to 0 %.
     *
     * @param progress Value from [revealAt].
     */
    fun revealEdges(progress: Float): Pair<Float, Float> {
        val opaqueEnd = -0.1f + 1.2f * progress
        return opaqueEnd to opaqueEnd + 0.099f
    }

    /**
     * Returns the white flash at the landed pen at [time]. It grows, turns 45 degrees, and fades out.
     *
     * @param time Time in milliseconds from the start of the splash.
     */
    fun flashAt(time: Float): FlashFrame {
        val progress = progressOf(time, LandTime, FlashMillis)
        val eased = EaseOut.transform(progress)
        return FlashFrame(scale = 0.2f + 2.4f * eased, rotation = 45f * eased, alpha = FlashAlpha.valueAt(progress))
    }

    private val FlashAlpha = listOf(Keyframe(0f, 0f, EaseOut), Keyframe(0.3f, 0.9f, EaseOut), Keyframe(1f, 0f))

    /**
     * Returns the opacity of the whole splash at [time]. The splash fades out at the end, so the home screen shows
     * through it.
     *
     * @param time Time in milliseconds from the start of the splash.
     */
    fun overlayAlphaAt(time: Float): Float = 1f - EaseIn.transform(progressOf(time, FadeStart, TotalMillis - FadeStart))

    // ===========================================================
    // Sparks
    // ===========================================================

    /** Sparks that the pen leaves behind. Their positions use the same pseudo-random values as the prototype. */
    val Sparks: List<Spark> = List(18) { index ->
        val percent = 4.0 + index * 81.0 / 17.0
        val random = abs(sin(index * 12.9898) * 43758.5453) % 1.0
        val size = 3 + (random * 4).roundToInt()
        Spark(
            xFraction = ((-2.0 + percent / 85.0 * 102.0 + (random - 0.5) * 4.0) / 100.0).toFloat(),
            yFraction = ((pathHeightAt(percent) + (random - 0.5) * 22.0) / 100.0).toFloat(),
            sizeDp = size.toFloat(),
            star = size > 4,
            light = index % 3 == 0,
            delay = (PenStart + percent / 100.0 * WriteMillis + 30.0).toFloat(),
            duration = (TotalMillis * (0.22 + random * 0.12)).toFloat(),
        )
    }

    private val SparkAlpha = listOf(Keyframe(0f, 0f, EaseOut), Keyframe(0.25f, 1f, EaseOut), Keyframe(1f, 0f))
    private val SparkScale = listOf(Keyframe(0f, 0f, EaseOut), Keyframe(0.25f, 1f, EaseOut), Keyframe(1f, 0.4f))
    private val SparkDrop = listOf(Keyframe(0f, 0f, EaseOut), Keyframe(0.25f, 0f, EaseOut), Keyframe(1f, 14f))

    /**
     * Returns [spark] at [time]. It grows into view, then falls a little and fades out.
     *
     * @param spark One of [Sparks].
     * @param time Time in milliseconds from the start of the splash.
     */
    fun sparkAt(spark: Spark, time: Float): SparkFrame {
        val progress = progressOf(time, spark.delay, spark.duration)
        return SparkFrame(
            scale = SparkScale.valueAt(progress),
            alpha = SparkAlpha.valueAt(progress),
            dropDp = SparkDrop.valueAt(progress),
        )
    }

    /** Height of the pen path in percent at [percent] of the write time, with straight lines between the points. */
    private fun pathHeightAt(percent: Double): Double {
        for (index in 1 until PenPath.size) {
            val (endAt, endY) = PenPath[index]
            if (percent <= endAt) {
                val (startAt, startY) = PenPath[index - 1]
                return startY + (endY - startY) * (percent - startAt) / (endAt - startAt)
            }
        }
        return PenPath.last().second.toDouble()
    }
}

/**
 * Pen star at one moment.
 *
 * @property xFraction Horizontal position as a fraction of the word width.
 * @property xOffsetDp Distance to add to [xFraction].
 * @property yFraction Vertical position as a fraction of the word height.
 * @property rotation Rotation in degrees. Positive is clockwise.
 * @property scale Size as a factor of [SplashTimeline.StarSizeDp].
 * @property alpha Opacity from 0 to 1.
 */
internal data class PenFrame(
    val xFraction: Float,
    val xOffsetDp: Float,
    val yFraction: Float,
    val rotation: Float,
    val scale: Float,
    val alpha: Float,
)

/**
 * Flash at one moment.
 *
 * @property scale Size as a factor of [SplashTimeline.FlashSizeDp].
 * @property rotation Rotation in degrees.
 * @property alpha Opacity from 0 to 1.
 */
internal data class FlashFrame(val scale: Float, val rotation: Float, val alpha: Float)

/**
 * One spark that the pen leaves behind.
 *
 * @property xFraction Horizontal position of the center as a fraction of the word width.
 * @property yFraction Vertical position of the center as a fraction of the word height.
 * @property sizeDp Width and height of the spark.
 * @property star True to draw a four-point star. False to draw a circle.
 * @property light True to use the light text color. False to use the sparkle color.
 * @property delay Time when the spark starts to show.
 * @property duration Time that the spark shows.
 */
internal data class Spark(
    val xFraction: Float,
    val yFraction: Float,
    val sizeDp: Float,
    val star: Boolean,
    val light: Boolean,
    val delay: Float,
    val duration: Float,
)

/**
 * Spark at one moment.
 *
 * @property scale Size as a factor of [Spark.sizeDp].
 * @property alpha Opacity from 0 to 1.
 * @property dropDp Distance that the spark moved down.
 */
internal data class SparkFrame(val scale: Float, val alpha: Float, val dropDp: Float)

/**
 * One CSS keyframe.
 *
 * @property at Position from 0 to 1 in the animation.
 * @property value Value at [at].
 * @property easing Easing from this keyframe to the next one.
 */
internal class Keyframe(val at: Float, val value: Float, val easing: Easing = LinearEasing)

/** Returns the value of the keyframes at [progress]. Before the first and after the last keyframe, the value stays. */
internal fun List<Keyframe>.valueAt(progress: Float): Float {
    if (progress <= first().at) return first().value
    if (progress >= last().at) return last().value
    val index = indexOfLast { it.at <= progress }
    val from = this[index]
    val to = this[index + 1]
    val local = (progress - from.at) / (to.at - from.at)
    return from.value + (to.value - from.value) * from.easing.transform(local)
}

/**
 * Returns the progress from 0 to 1 of an animation that starts at [delay] and takes [duration]. The progress stays 0
 * before the start and 1 after the end, as CSS "animation-fill-mode: both".
 */
internal fun progressOf(time: Float, delay: Float, duration: Float): Float = ((time - delay) / duration).coerceIn(0f, 1f)
