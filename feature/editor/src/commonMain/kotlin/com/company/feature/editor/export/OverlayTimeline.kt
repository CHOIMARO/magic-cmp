package com.company.feature.editor.export

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.clipAt
import com.company.domain.model.editor.currentWordIndex
import com.company.domain.model.editor.placedCaptions
import com.company.feature.editor.editor.isVisibleAt
import com.company.feature.editor.editor.visibleAt
import com.company.ui.editor.TextAnimation
import com.company.ui.editor.animatedRanges
import com.company.ui.editor.look
import com.company.ui.editor.splitWords
import kotlin.math.floor

/** Shortest time range, in seconds, that gets its own overlay image. */
private const val MIN_SPAN_LENGTH = 0.0001

/** Length of one animation frame of the overlay, in seconds. The overlay changes at 30 frames per second during an animation. */
internal const val OVERLAY_FRAME_SECONDS = 1.0 / 30

/**
 * Time range that uses one overlay image.
 *
 * @property start Start time in seconds.
 * @property end End time in seconds.
 * @property time Time to draw the image for. It is inside the range.
 */
internal data class OverlaySpan(val start: Double, val end: Double, val time: Double)

/**
 * State of one text or caption on screen.
 *
 * @property id ID of the text or the caption.
 * @property animationFrame Index of the animation frame from the start of the item, or null between the entrance and the exit.
 */
internal data class ItemState(val id: String, val animationFrame: Int?)

/**
 * What the text layer shows at a time. Two times with the same state give the same image.
 *
 * @property texts Texts on screen.
 * @property caption Caption on screen, or null.
 * @property spokenWords Number of words that the speaker already said in the caption.
 * @property watermark True when the watermark shows.
 */
internal data class TextLayerState(
    val texts: List<ItemState>,
    val caption: ItemState?,
    val spokenWords: Int,
    val watermark: Boolean,
) {
    /** True when the layer draws nothing. */
    val isEmpty: Boolean
        get() = texts.isEmpty() && caption == null && !watermark
}

/**
 * Splits the timeline into ranges where the layout layer does not change.
 *
 * The layout layer holds the layout background and the other cells. They change where the next clip starts.
 *
 * @return Ranges in time order. It is empty for the full-screen layout.
 */
internal fun EditorProject.layoutSpans(): List<OverlaySpan> {
    if (layout == LayoutType.FULL) return emptyList()
    val boundaries = buildList {
        add(0.0)
        var start = 0.0
        clips.forEach { clip ->
            start += clip.duration
            add(start)
        }
    }
    return spans(boundaries) { time -> clipAt(time)?.index }
}

/**
 * Splits the timeline into ranges where the text layer does not change.
 *
 * A range ends where a text or a caption starts or ends, and where the next caption word starts.
 * During an entrance or an exit, a range is one animation frame long. Ranges without text are left out.
 *
 * @param watermark True when the watermark shows.
 * @return Ranges in time order.
 */
internal fun EditorProject.textSpans(watermark: Boolean): List<OverlaySpan> {
    val boundaries = buildList {
        add(0.0)
        texts.forEach { text -> addItemBoundaries(text.start, text.length, text.look().animation) }
        val captionAnimation = captionStyle.look().animation
        placedCaptions.forEach { caption ->
            addItemBoundaries(caption.start, caption.length, captionAnimation)
            // 단어마다 글자 수에 비례한 시간이 있다. 다음 단어가 시작하는 곳에서 모습이 바뀐다.
            val words = caption.text.splitWords()
            val characters = words.sumOf { it.length }.coerceAtLeast(1)
            var before = 0
            words.dropLast(1).forEach { word ->
                before += word.length
                add(caption.start + caption.length * before / characters)
            }
        }
    }
    return spans(boundaries) { time -> textLayerStateAt(time, watermark).takeUnless { it.isEmpty } }
}

/** State of the text layer at [time]. */
internal fun EditorProject.textLayerStateAt(time: Double, watermark: Boolean): TextLayerState {
    val caption = placedCaptions.visibleAt(time)
    return TextLayerState(
        texts = texts.filter { it.isVisibleAt(time) }.map { ItemState(it.id, animationFrame(time - it.start, it.length, it.look().animation)) },
        caption = caption?.let { ItemState(it.id, animationFrame(time - it.start, it.length, captionStyle.look().animation)) },
        spokenWords = caption?.let { it.currentWordIndex(time) + 1 } ?: 0,
        watermark = watermark,
    )
}

/** Adds the start and the end of an item, and the edges and the frame starts of its animations. */
private fun MutableList<Double>.addItemBoundaries(start: Double, length: Double, animation: TextAnimation) {
    add(start)
    add(start + length)
    animation.animatedRanges(length).forEach { range ->
        add(start + range.start)
        add(start + range.endInclusive)
        var frame = floor(range.start / OVERLAY_FRAME_SECONDS).toInt() + 1
        while (frame * OVERLAY_FRAME_SECONDS < range.endInclusive) {
            add(start + frame * OVERLAY_FRAME_SECONDS)
            frame++
        }
    }
}

/**
 * Index of the animation frame at [elapsed] seconds from the start of an item.
 *
 * @return The index during the entrance or the exit, or null between them.
 */
private fun animationFrame(elapsed: Double, length: Double, animation: TextAnimation): Int? =
    if (animation.animatedRanges(length).any { elapsed >= it.start && elapsed < it.endInclusive }) {
        floor(elapsed / OVERLAY_FRAME_SECONDS).toInt()
    } else {
        null
    }

/**
 * Makes ranges between the boundaries. Next ranges with the same state become one range.
 *
 * @param boundaries Times where the state can change.
 * @param stateAt State at a time, or null when the layer draws nothing.
 */
private fun EditorProject.spans(boundaries: List<Double>, stateAt: (Double) -> Any?): List<OverlaySpan> {
    val total = totalDuration
    if (total <= 0.0) return emptyList()
    val times = (boundaries + total).filter { it in 0.0..total }.sorted()
    val spans = mutableListOf<Pair<OverlaySpan, Any>>()
    times.zipWithNext().forEach { (start, end) ->
        if (end - start < MIN_SPAN_LENGTH) return@forEach
        val time = (start + end) / 2
        val state = stateAt(time) ?: return@forEach
        val last = spans.lastOrNull()
        if (last != null && last.second == state && last.first.end == start) {
            spans[spans.lastIndex] = last.first.copy(end = end) to state
        } else {
            spans += OverlaySpan(start, end, time) to state
        }
    }
    return spans.map { it.first }
}
