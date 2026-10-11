package com.company.feature.editor.export

import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.clipAt
import com.company.feature.editor.editor.isVisibleAt
import com.company.feature.editor.editor.spokenWordCount
import com.company.feature.editor.editor.visibleAt

/** Shortest time range, in seconds, that gets its own overlay image. */
private const val MIN_SPAN_LENGTH = 0.0001

/**
 * What the overlay shows at a time. Two times with the same state give the same image.
 *
 * @property textIds Texts on screen.
 * @property captionId Caption on screen, or null.
 * @property spokenWords Number of highlighted words of the caption.
 * @property clipIndex Clip at the playhead, for a layout with more than one cell. The other cells show the next clips.
 * @property watermark True when the watermark shows.
 */
internal data class OverlayState(
    val textIds: List<String>,
    val captionId: String?,
    val spokenWords: Int,
    val clipIndex: Int?,
    val watermark: Boolean,
) {
    /** True when the overlay draws nothing. */
    val isEmpty: Boolean
        get() = textIds.isEmpty() && captionId == null && clipIndex == null && !watermark
}

/**
 * Time range that uses one overlay image.
 *
 * @property start Start time in seconds.
 * @property end End time in seconds.
 * @property time Time to draw the image for. It is inside the range.
 */
internal data class OverlaySpan(val start: Double, val end: Double, val time: Double)

/**
 * Splits the timeline into ranges where the overlay does not change.
 *
 * A range ends where a text or a caption starts or ends, where the next caption word starts,
 * and, for a layout with more than one cell, where the next clip starts. Ranges without a visible
 * overlay are left out.
 *
 * @param watermark True when the watermark shows.
 * @return Ranges in time order.
 */
internal fun EditorProject.overlaySpans(watermark: Boolean): List<OverlaySpan> {
    val total = totalDuration
    if (total <= 0.0) return emptyList()
    val boundaries = buildList {
        add(0.0)
        add(total)
        texts.forEach { add(it.start); add(it.start + it.length) }
        captions.forEach { caption ->
            val words = caption.text.split(" ").size
            for (step in 0..words) add(caption.start + caption.length * step / words)
        }
        if (layout != LayoutType.FULL) {
            var start = 0.0
            clips.forEach { clip ->
                start += clip.duration
                add(start)
            }
        }
    }.filter { it in 0.0..total }.sorted()

    val spans = mutableListOf<Pair<OverlaySpan, OverlayState>>()
    boundaries.zipWithNext().forEach { (start, end) ->
        if (end - start < MIN_SPAN_LENGTH) return@forEach
        val time = (start + end) / 2
        val state = overlayStateAt(time, watermark)
        val last = spans.lastOrNull()
        if (last != null && last.second == state && last.first.end == start) {
            spans[spans.lastIndex] = last.first.copy(end = end) to state
        } else {
            spans += OverlaySpan(start, end, time) to state
        }
    }
    return spans.filterNot { it.second.isEmpty }.map { it.first }
}

/** State of the overlay at [time]. */
internal fun EditorProject.overlayStateAt(time: Double, watermark: Boolean): OverlayState {
    val caption = captions.visibleAt(time)
    return OverlayState(
        textIds = texts.filter { it.isVisibleAt(time) }.map { it.id },
        captionId = caption?.id,
        spokenWords = caption?.spokenWordCount(time) ?: 0,
        clipIndex = if (layout == LayoutType.FULL) null else clipAt(time)?.index,
        watermark = watermark,
    )
}
