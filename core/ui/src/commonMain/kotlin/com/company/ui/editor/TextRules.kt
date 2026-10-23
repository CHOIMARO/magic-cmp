package com.company.ui.editor

import androidx.compose.runtime.Immutable
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import kotlin.math.floor
import kotlin.math.pow

// =====================================================================
// Text rules of the "Magic Text Templates" spec.
// The spec uses pixels of the 1080×1920 canvas. TextLook sizes are dp of the editor stage at scale 1.
// =====================================================================

/** Width of the editor stage at scale 1, in dp. It is the 1080 px width of the spec canvas. */
private const val STAGE_WIDTH_DP = 264.375f

/** Converts spec canvas pixels to stage dp. */
private const val DP_PER_SPEC_PX = STAGE_WIDTH_DP / 1080f

/** Safety margin of the character count per line. */
private const val CHARS_PER_LINE_MARGIN = 0.9f

/** Smallest font scale when a text is too long. */
private const val MIN_FONT_SCALE = 0.7f

/** Distance of the slide-up entrance, in spec pixels. */
private const val SLIDE_UP_DISTANCE_PX = 70f

/** Distance of the fade-up entrance, in spec pixels. */
private const val FADE_UP_DISTANCE_PX = 40f

/** Height of the bounce entrance, in spec pixels. */
private const val BOUNCE_HEIGHT_PX = 160f

/** Distance of the slide-down exit, in spec pixels. */
private const val SLIDE_DOWN_DISTANCE_PX = 40f

/** Distance of the fly-up exit, in spec pixels. */
private const val FLY_UP_DISTANCE_PX = 120f

/** Size that the shrink exit takes away, as a factor of the size. */
private const val SHRINK_AMOUNT = 0.6f

/** Start size of the stamp entrance, as a factor of the final size. */
private const val STAMP_START_SCALE = 1.9f

/** Time that the text stays still in an animation preview, after the entrance. */
private const val PREVIEW_HOLD_MILLIS = 1000L

/** Time that no text shows in an animation preview, after the exit and before the next entrance. */
private const val PREVIEW_PAUSE_MILLIS = 300L

/** Opacity steps of the flicker entrance. */
private val FlickerSteps = floatArrayOf(0f, 0.9f, 0.1f, 0.8f, 0.3f, 1f, 0.6f, 1f)

/**
 * Entrance and exit of a text.
 *
 * @property entrance How the text comes on screen.
 * @property entranceMillis Length of the entrance.
 * @property exitMillis Length of the exit.
 * @property exit How the text goes off screen.
 */
@Immutable
data class TextAnimation(
    val entrance: TextEntrance,
    val entranceMillis: Int,
    val exitMillis: Int,
    val exit: TextExit = TextExit.FADE,
) {
    /** Length of the entrance in seconds. */
    val entranceSeconds: Double
        get() = entranceMillis / 1000.0

    /** Length of the exit in seconds. */
    val exitSeconds: Double
        get() = exitMillis / 1000.0
}

/** Length of the exit of a title when the user picks no length. */
const val DEFAULT_TITLE_EXIT_MILLIS = 200

/**
 * Applies the animation that the user picked for one text.
 *
 * A null value keeps the value of the style. The default exit is a fade. "없음" has no length. When the entrance and
 * the exit together are longer than the text, both become shorter in the same proportion.
 *
 * @param entrance Entrance that the user picked, or null.
 * @param entranceMillis Entrance length that the user picked, or null.
 * @param exit Exit that the user picked, or null.
 * @param exitMillis Exit length that the user picked, or null.
 * @param lengthSeconds Time of the text on screen.
 */
fun TextAnimation.withChoices(
    entrance: TextEntrance?,
    entranceMillis: Int?,
    exit: TextExit?,
    exitMillis: Int?,
    lengthSeconds: Double,
): TextAnimation {
    val newEntrance = entrance ?: this.entrance
    val newExit = exit ?: TextExit.FADE
    var enterMs = if (newEntrance == TextEntrance.NONE) 0.0 else (entranceMillis ?: this.entranceMillis).toDouble()
    var exitMs = if (newExit == TextExit.NONE) 0.0 else (exitMillis ?: DEFAULT_TITLE_EXIT_MILLIS).toDouble()
    val limit = lengthSeconds * 1000
    if (enterMs + exitMs > limit && enterMs + exitMs > 0.0) {
        val ratio = limit / (enterMs + exitMs)
        enterMs *= ratio
        exitMs *= ratio
    }
    return TextAnimation(newEntrance, enterMs.toInt(), exitMs.toInt(), newExit)
}

/** How a caption shows the word that the speaker says now. */
enum class CaptionHighlight {
    /** All words use the text color. */
    NONE,

    /** The current word uses the highlight color. */
    CURRENT,
}

/**
 * Look of an animated text at one moment.
 *
 * @property alpha Opacity from 0 to 1.
 * @property scale Size factor around the center.
 * @property offsetY Vertical move in dp. A positive value moves down.
 * @property backgroundReveal Part of the background that shows, from the left edge, from 0 to 1.
 * @property textReveal Part of the text box that shows, from the left edge, from 0 to 1.
 */
@Immutable
data class TextFrame(
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val offsetY: Float = 0f,
    val backgroundReveal: Float = 1f,
    val textReveal: Float = 1f,
) {
    companion object {
        /** Text without animation. */
        val Static = TextFrame()
    }
}

/**
 * Calculates the look of the text at one moment.
 *
 * @param elapsed Seconds since the text came on screen.
 * @param remaining Seconds until the text goes off screen.
 * @return The look. It is [TextFrame.Static] between the entrance and the exit.
 */
fun TextAnimation.frameAt(elapsed: Double, remaining: Double): TextFrame {
    val p = if (entranceMillis <= 0) 1f else (elapsed / entranceSeconds).coerceIn(0.0, 1.0).toFloat()
    var frame = when (entrance) {
        TextEntrance.NONE -> TextFrame.Static
        TextEntrance.POP -> TextFrame(alpha = minOf(1f, p * 2.5f), scale = 0.6f + 0.4f * easeOutBack(p))
        TextEntrance.SLIDE_UP -> TextFrame(alpha = minOf(1f, p * 1.5f), offsetY = (1f - easeOutCubic(p)) * SLIDE_UP_DISTANCE_PX * DP_PER_SPEC_PX)
        TextEntrance.FADE -> TextFrame(alpha = easeInOutCubic(p))
        TextEntrance.FLICKER -> TextFrame(alpha = if (p >= 1f) 1f else FlickerSteps[(p * FlickerSteps.size).toInt()])
        TextEntrance.MARKER_SWEEP -> TextFrame(alpha = minOf(1f, p * 4f), backgroundReveal = easeOutCubic(p))
        TextEntrance.FADE_UP -> TextFrame(alpha = easeOutCubic(p), offsetY = (1f - easeOutCubic(p)) * FADE_UP_DISTANCE_PX * DP_PER_SPEC_PX)
        TextEntrance.WRITE -> TextFrame(textReveal = easeInOutCubic(p))
        TextEntrance.STAMP -> TextFrame(alpha = minOf(1f, p * 2f), scale = STAMP_START_SCALE - (STAMP_START_SCALE - 1f) * easeOutCubic(p))
        TextEntrance.BOUNCE -> TextFrame(alpha = minOf(1f, p * 3f), offsetY = -(1f - easeOutBounce(p)) * BOUNCE_HEIGHT_PX * DP_PER_SPEC_PX)
    }
    val progress = if (exitMillis <= 0) 0f else ((exitSeconds - remaining) / exitSeconds).coerceIn(0.0, 1.0).toFloat()
    if (progress > 0f) frame = frame.exitFrame(exit, progress)
    return frame
}

/** Adds the exit at [progress] from 0 to 1 to the look of the text. */
private fun TextFrame.exitFrame(exit: TextExit, progress: Float): TextFrame = when (exit) {
    TextExit.NONE -> this
    TextExit.FADE -> copy(alpha = alpha * (1f - easeInCubic(progress)))
    TextExit.SLIDE_DOWN -> copy(alpha = alpha * (1f - progress), offsetY = offsetY + easeInCubic(progress) * SLIDE_DOWN_DISTANCE_PX * DP_PER_SPEC_PX)
    TextExit.SHRINK -> copy(alpha = alpha * (1f - easeInCubic(progress)), scale = scale * (1f - SHRINK_AMOUNT * easeInCubic(progress)))
    TextExit.FLY_UP -> copy(alpha = alpha * (1f - progress), offsetY = offsetY - easeInCubic(progress) * FLY_UP_DISTANCE_PX * DP_PER_SPEC_PX)
    TextExit.FLICKER -> copy(
        alpha = alpha * if (progress >= 1f) 0f else FlickerSteps[FlickerSteps.lastIndex - (progress * FlickerSteps.size).toInt()],
    )
}

/**
 * Seconds from the start of the text, or to its end, where the animation changes the look.
 *
 * @param length Time on screen in seconds.
 * @return The entrance range and the exit range, from the start of the text.
 */
fun TextAnimation.animatedRanges(length: Double): List<ClosedFloatingPointRange<Double>> = listOf(
    0.0..minOf(entranceSeconds, length),
    maxOf(0.0, length - exitSeconds)..length,
)

/** Length of one loop of the preview: the entrance, the hold, the exit, and the pause. */
private val TextAnimation.previewLoopMillis: Long
    get() = entranceMillis + PREVIEW_HOLD_MILLIS + exitMillis + PREVIEW_PAUSE_MILLIS

/**
 * Calculates the look of the text in a looping preview.
 *
 * The text comes on screen, stays still for 1 second, and goes off screen. After a 0.3 second pause, the loop
 * starts again. The loop length changes with the entrance length, so texts with different animations do not go
 * off screen at the same time.
 *
 * @param elapsedMillis Time since the preview started.
 * @return The look of the text at that time.
 */
fun TextAnimation.previewFrameAt(elapsedMillis: Long): TextFrame {
    val time = elapsedMillis % previewLoopMillis
    return frameAt(
        elapsed = time / 1000.0,
        remaining = (entranceMillis + PREVIEW_HOLD_MILLIS + exitMillis - time) / 1000.0,
    )
}

private fun easeOutCubic(p: Float): Float = 1f - (1f - p).pow(3)

private fun easeInCubic(p: Float): Float = p.pow(3)

private fun easeInOutCubic(p: Float): Float = if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).pow(3) / 2f

/** Falls and bounces three times, as the easeOutBounce curve of the spec. */
private fun easeOutBounce(p: Float): Float {
    val n = 7.5625f
    val d = 2.75f
    return when {
        p < 1f / d -> n * p * p
        p < 2f / d -> (p - 1.5f / d).let { n * it * it + 0.75f }
        p < 2.5f / d -> (p - 2.25f / d).let { n * it * it + 0.9375f }
        else -> (p - 2.625f / d).let { n * it * it + 0.984375f }
    }
}

private fun easeOutBack(p: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (p - 1f).pow(3) + c1 * (p - 1f).pow(2)
}

// =====================================================================
// Overflow
// =====================================================================

/**
 * Lines of a text after the overflow rule.
 *
 * @property lines Words of each line.
 * @property fontScale Size factor of the text, from 0.7 to 1.
 */
@Immutable
data class TextLines(val lines: List<List<String>>, val fontScale: Float)

/**
 * Number of characters that fit on one line, as in the spec: the widest box divided by the average letter width.
 */
internal fun TextLook.charactersPerLine(): Int {
    val boxWidth = boxWidthRatio * STAGE_WIDTH_DP - paddingHorizontal * 2
    val letterWidth = fontSize * font.glyphWidthRatio * widthFactor + letterSpacingEm * fontSize
    return maxOf(1, floor(boxWidth / letterWidth * CHARS_PER_LINE_MARGIN).toInt())
}

/** Average letter width of the font, as a part of the font size. The values come from the spec. */
internal val TextFontType.glyphWidthRatio: Float
    get() = when (this) {
        TextFontType.PRETENDARD, TextFontType.GOWUN_BATANG, TextFontType.JUA, TextFontType.GUGI, TextFontType.SONG_MYUNG -> 0.95f
        TextFontType.NANUM_PEN, TextFontType.DONGLE -> 0.62f
        TextFontType.BLACK_HAN_SANS -> 0.98f
        TextFontType.DO_HYEON, TextFontType.OK_DANDAN -> 0.85f
        TextFontType.GAEGU, TextFontType.MYOEUN_HEULLIM -> 0.8f
        // 아래 글꼴은 한글 글자 폭을 재서, 기존 스펙 값과 같은 비율(약 1.1배)로 맞췄다.
        TextFontType.KKUBULIM -> 0.95f
        TextFontType.GELLYROLL -> 0.9f
        TextFontType.MONA12 -> 1.1f
        TextFontType.NEUJOH -> 0.57f
        TextFontType.STRONG_ARMY -> 0.97f
        TextFontType.KUKDETOPOKKI -> 1.02f
        TextFontType.ESAMANRU -> 1.04f
        TextFontType.OK_GUNG -> 1.1f
    }

/**
 * Breaks the words into lines with the overflow rule of the spec.
 *
 * Lines hold at most [TextLook.maxCharsPerLine] characters without spaces. A text that needs more
 * than [TextLook.maxLines] lines gets smaller, to 70% at most, so that more characters fit. The
 * lines have similar lengths. A word never breaks.
 *
 * @param words Words of the text.
 * @return The lines and the font scale.
 */
fun TextLook.breakLines(words: List<String>): TextLines {
    if (words.isEmpty()) return TextLines(emptyList(), 1f)
    val length = words.sumOf { it.length }
    val capacity = maxCharsPerLine * maxLines
    val fontScale = if (length > capacity) maxOf(MIN_FONT_SCALE, capacity.toFloat() / length) else 1f
    val perLine = floor(maxCharsPerLine / fontScale + 1e-4f).toInt()
    val lineCount = minOf(maxLines, greedyLineCount(words, perLine))
    return TextLines(balancedLines(words, lineCount), fontScale)
}

/** Number of lines that a simple fill needs. */
private fun greedyLineCount(words: List<String>, perLine: Int): Int {
    var lines = 1
    var used = 0
    words.forEach { word ->
        if (used > 0 && used + word.length > perLine) {
            lines++
            used = 0
        }
        used += word.length
    }
    return lines
}

/**
 * Splits the words into [lineCount] lines so that the longest line is as short as possible.
 *
 * A line length counts the letters and the spaces between the words.
 */
private fun balancedLines(words: List<String>, lineCount: Int): List<List<String>> {
    val n = words.size
    val count = lineCount.coerceIn(1, n)
    if (count == 1) return listOf(words)
    fun width(from: Int, to: Int): Int = (from until to).sumOf { words[it].length } + (to - from - 1)
    // best[k][i]: 앞의 i개 단어를 k줄로 나눌 때 가장 긴 줄의 최솟값.
    val best = Array(count + 1) { IntArray(n + 1) { Int.MAX_VALUE } }
    val cut = Array(count + 1) { IntArray(n + 1) }
    best[0][0] = 0
    for (k in 1..count) {
        for (i in k..n) {
            for (j in (k - 1) until i) {
                if (best[k - 1][j] == Int.MAX_VALUE) continue
                val longest = maxOf(best[k - 1][j], width(j, i))
                if (longest < best[k][i]) {
                    best[k][i] = longest
                    cut[k][i] = j
                }
            }
        }
    }
    val lines = ArrayDeque<List<String>>()
    var end = n
    for (k in count downTo 1) {
        val start = cut[k][end]
        lines.addFirst(words.subList(start, end))
        end = start
    }
    return lines.toList()
}
