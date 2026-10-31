package com.company.domain.model.editor

import kotlin.math.abs
import kotlin.math.roundToInt

// =====================================================================
// Captions (F04): typed scripts, the caption track, and caption edits
// =====================================================================

/** Shortest caption length after a trim or an addition, in seconds. */
const val MIN_CAPTION_LENGTH = 0.3

/** A caption split must be at least this far from the caption edges, in seconds. */
const val CAPTION_SPLIT_MARGIN = 0.2

/** Shortest length of a bound caption that still shows, in seconds. */
private const val MIN_PLACED_CAPTION_LENGTH = 0.05

/** A script that starts this near to a clip start, in seconds, starts at the clip start. */
private const val SCRIPT_OFFSET_MIN = 0.05

/** Lowest caption height from the bottom of the screen, in percent. */
const val CAPTION_POSITION_MIN = 6.0

/** Highest caption height from the bottom of the screen, in percent. The top safe area (12 %) stays free. */
const val CAPTION_POSITION_MAX = 76.0

/** A caption height this near to the height of the caption style, in percent, snaps to the style height. */
const val CAPTION_POSITION_SNAP = 2.5

/**
 * Times of the clip boundaries on the timeline: 0, the end of each clip, and the project end.
 *
 * @return One more item than the number of clips.
 */
fun EditorProject.clipBoundaries(): List<Double> {
    val boundaries = ArrayList<Double>(clips.size + 1)
    var time = 0.0
    boundaries += time
    clips.forEach { clip ->
        time += clip.duration
        boundaries += time
    }
    return boundaries
}

/**
 * Captions with their times on the timeline, in the order of [EditorProject.captions].
 *
 * A caption bound to a clip gets the time of the clip. A caption with a clip offset starts that far after the clip
 * start, and the clip end cuts it. A caption of a clip that is not in the project is left out.
 *
 * The project end cuts all captions, and a caption that starts at the end or later is left out. The stored times do
 * not change, so a caption gets its length back when the clips get longer again.
 */
val EditorProject.placedCaptions: List<CaptionSegment>
    get() {
        val total = totalDuration
        if (captions.all { it.clipId == null && it.end <= total }) return captions
        val starts = clipBoundaries()
        val indexes = clips.withIndex().associate { (index, clip) -> clip.id to index }
        return captions.mapNotNull { caption ->
            val clipId = caption.clipId
            val placed = if (clipId == null) {
                caption
            } else {
                val index = indexes[clipId] ?: return@mapNotNull null
                val clipStart = starts[index]
                val clipLength = clips[index].duration
                val offset = caption.clipOffset
                if (offset == null) {
                    caption.copy(start = clipStart, length = clipLength)
                } else {
                    // 클립이 줄어 시작 거리가 클립보다 길면 클립 시작으로 당긴다.
                    val from = if (offset >= clipLength) 0.0 else offset
                    caption.copy(start = clipStart + from, length = minOf(caption.clipLength ?: clipLength, clipLength - from))
                }
            }
            // 클립이 줄어 프로젝트가 짧아지면 프로젝트 끝에서 자른다. 타임라인은 프로젝트 끝까지만 보인다.
            val length = minOf(placed.length, total - placed.start)
            if (length < MIN_PLACED_CAPTION_LENGTH) null else placed.copy(length = length)
        }
    }

/**
 * Splits a typed script into caption lines. Each line of the script is one caption. Empty lines and the spaces at
 * the start and the end of a line are ignored.
 *
 * @param script Text that the user typed or pasted.
 */
fun scriptLines(script: String): List<String> = script.lines().map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Makes captions from script lines. The n-th line goes to the n-th clip from [firstClip] and covers the rest of the
 * clip. Lines after the last clip are not used. Clips after the last line get no caption.
 *
 * @param lines Caption lines.
 * @param firstClip Index of the clip of the first line.
 * @param firstOffset Time in the first clip where the first line starts. 0 starts at the clip start.
 * @return Captions bound to the clips.
 */
fun EditorProject.captionsFromScript(lines: List<String>, firstClip: Int = 0, firstOffset: Double = 0.0): List<CaptionSegment> =
    lines.zip(clips.drop(firstClip)).mapIndexed { index, (text, clip) ->
        if (index == 0 && firstOffset > 0.0) {
            CaptionSegment(
                id = newEditorId(), text = text, start = 0.0, length = 0.0,
                clipId = clip.id, clipOffset = firstOffset, clipLength = clip.duration - firstOffset,
            )
        } else {
            CaptionSegment(id = newEditorId(), text = text, start = 0.0, length = 0.0, clipId = clip.id)
        }
    }

/**
 * Place where the script lines start when the user makes captions from the playhead.
 *
 * @property clipIndex Index of the clip of the first line. It is the number of clips when no clip is left.
 * @property offset Time in that clip where the first line starts.
 * @property time The same place on the timeline.
 */
data class ScriptStart(val clipIndex: Int, val offset: Double, val time: Double)

/**
 * Finds where the script lines start for captions from [time] (F04).
 *
 * The first line starts at [time] in the clip at [time]. When [time] is less than [MIN_CAPTION_LENGTH] before the
 * end of that clip, the first line goes to the next clip. A time at 0 starts at the first clip.
 *
 * @param time Playhead position.
 */
fun EditorProject.scriptStart(time: Double): ScriptStart {
    if (clips.isEmpty() || time <= TIME_EPSILON) return ScriptStart(clipIndex = 0, offset = 0.0, time = 0.0)
    val boundaries = clipBoundaries()
    var clipIndex = clipAt(time)?.index ?: 0
    var offset = time - boundaries[clipIndex]
    if (offset > clips[clipIndex].duration - MIN_CAPTION_LENGTH) {
        clipIndex++
        offset = 0.0
    }
    if (offset <= SCRIPT_OFFSET_MIN) offset = 0.0
    val start = if (clipIndex < clips.size) boundaries[clipIndex] + offset else totalDuration
    return ScriptStart(clipIndex = clipIndex, offset = offset, time = start)
}

/**
 * Captions that stay when new captions start at [time]. The captions before [time] stay. A caption across [time] ends
 * at [time] and gets a fixed time, or goes away when it gets shorter than [MIN_CAPTION_LENGTH]. The captions after
 * [time] go away.
 *
 * @param time Start of the new captions. At 0, no caption stays.
 */
fun EditorProject.captionsBefore(time: Double): List<CaptionSegment> {
    if (time <= TIME_EPSILON) return emptyList()
    val placed = placedCaptions.associateBy { it.id }
    return captions.mapNotNull { caption ->
        val position = placed[caption.id] ?: return@mapNotNull null
        when {
            position.start >= time - TIME_EPSILON -> null
            position.end <= time + TIME_EPSILON -> caption
            time - position.start < MIN_CAPTION_LENGTH -> null
            else -> caption.copy(start = position.start, length = time - position.start, clipId = null, clipOffset = null, clipLength = null)
        }
    }
}

/**
 * Number of captions that new captions from [time] replace or cut: the captions that end after [time].
 *
 * @param time Start of the new captions.
 */
fun EditorProject.captionCountFrom(time: Double): Int = placedCaptions.count { it.end > time + TIME_EPSILON }

/**
 * Height of all captions after the user drags a caption on the stage (F04 caption move).
 *
 * The height stays between [CAPTION_POSITION_MIN] and [CAPTION_POSITION_MAX]. Near the height of the caption style,
 * it snaps to the style height.
 *
 * @param position Height from the bottom of the screen that the drag gives, in percent.
 * @param stylePosition Height of the caption style, in percent.
 * @return The height rounded to 0.1, or null for the height of the caption style.
 */
fun captionPositionFor(position: Double, stylePosition: Double): Double? {
    val clamped = position.coerceIn(CAPTION_POSITION_MIN, CAPTION_POSITION_MAX)
    if (abs(clamped - stylePosition) < CAPTION_POSITION_SNAP) return null
    return (clamped * 10).roundToInt() / 10.0
}

/**
 * Index of the word that the speaker says at [time].
 *
 * Captions have no time for each word. So each word gets a part of the caption length, in proportion to the number
 * of its characters.
 *
 * @param time Time on the timeline.
 * @return The index from 0 to the last word, or -1 when the caption has no words.
 */
fun CaptionSegment.currentWordIndex(time: Double): Int {
    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return -1
    val total = words.sumOf { it.length }.coerceAtLeast(1)
    val fraction = if (length > 0.0) (time - start) / length else 1.0
    var before = 0
    var current = 0
    words.forEachIndexed { index, word ->
        if (fraction >= before.toDouble() / total) current = index
        before += word.length
    }
    return current
}

/**
 * Changes the text and the decorations of a caption.
 *
 * @param captionId ID of the caption.
 * @param text New text.
 * @param decorations New decorations.
 */
fun EditorProject.updateCaption(captionId: String, text: String, decorations: List<DecorationRange>): EditorProject =
    copy(captions = captions.map { if (it.id == captionId) it.copy(text = text, decorations = decorations) else it })

/**
 * Removes a caption.
 *
 * @param captionId ID of the caption.
 */
fun EditorProject.removeCaption(captionId: String): EditorProject = copy(captions = captions.filterNot { it.id == captionId })

/** Result of a caption split. */
sealed interface CaptionSplit {
    /** Done(project) : The caption is now two captions. */
    data class Done(val project: EditorProject) : CaptionSplit

    /** OutsideCaption : The time is not inside the caption, or it is too near to an edge. */
    data object OutsideCaption : CaptionSplit

    /** OneWord : The caption has only one word. */
    data object OneWord : CaptionSplit
}

/**
 * Splits a caption into two captions at [time]. The words before the split go to the first caption.
 *
 * Each word gets a part of the caption length in proportion to the number of its characters, as in
 * [currentWordIndex]. Both new captions have a fixed time.
 *
 * @param captionId ID of the caption.
 * @param time Time on the timeline.
 */
fun EditorProject.splitCaption(captionId: String, time: Double): CaptionSplit {
    val caption = placedCaptions.find { it.id == captionId } ?: return CaptionSplit.OutsideCaption
    if (time <= caption.start + CAPTION_SPLIT_MARGIN || time >= caption.end - CAPTION_SPLIT_MARGIN) {
        return CaptionSplit.OutsideCaption
    }
    val words = Regex("\\S+").findAll(caption.text).toList()
    if (words.size < 2) return CaptionSplit.OneWord
    val total = words.sumOf { it.value.length }
    val fraction = (time - caption.start) / caption.length
    var before = 0
    var splitWord = 1
    words.forEachIndexed { index, word ->
        if (index > 0 && before.toDouble() / total <= fraction) splitWord = index
        before += word.value.length
    }
    val cut = words[splitWord].range.first
    val firstText = caption.text.substring(0, cut).trimEnd()
    val first = caption.copy(
        text = firstText,
        length = time - caption.start,
        decorations = caption.decorations.slice(0, firstText.length),
        clipId = null, clipOffset = null, clipLength = null,
    )
    val second = CaptionSegment(
        id = newEditorId(),
        text = caption.text.substring(cut),
        start = time,
        length = caption.end - time,
        decorations = caption.decorations.slice(cut, caption.text.length),
    )
    return CaptionSplit.Done(copy(captions = captions.flatMap { if (it.id == captionId) listOf(first, second) else listOf(it) }))
}

/**
 * Moves one edge of a caption. The caption gets a fixed time, so the binding to its clip ends.
 *
 * The edge stays between the neighbor captions, inside the project, and [MIN_CAPTION_LENGTH] from the other edge.
 *
 * @param captionId ID of the caption.
 * @param start True to move the start, false to move the end.
 * @param time New time of the edge.
 */
fun EditorProject.trimCaption(captionId: String, start: Boolean, time: Double): EditorProject {
    val placed = placedCaptions
    val caption = placed.find { it.id == captionId } ?: return this
    val others = placed.filter { it.id != captionId }
    val previousEnd = others.filter { it.start < caption.start }.maxOfOrNull { it.end }?.coerceAtMost(caption.start) ?: 0.0
    val nextStart = others.filter { it.start > caption.start }.minOfOrNull { it.start }?.coerceAtLeast(caption.end)
        ?: maxOf(totalDuration, caption.end)
    val newStart = if (start) time.coerceAtMost(caption.end - MIN_CAPTION_LENGTH).coerceAtLeast(previousEnd) else caption.start
    val newEnd = if (start) caption.end else time.coerceAtMost(nextStart).coerceAtLeast(caption.start + MIN_CAPTION_LENGTH)
    return copy(
        captions = captions.map {
            if (it.id == captionId) {
                it.copy(start = newStart, length = newEnd - newStart, clipId = null, clipOffset = null, clipLength = null)
            } else {
                it
            }
        },
    )
}

/**
 * Captions that show in a clip, with their times on the timeline.
 *
 * @param clipIndex Index of the clip.
 */
fun EditorProject.captionsInClip(clipIndex: Int): List<CaptionSegment> {
    val boundaries = clipBoundaries()
    if (clipIndex !in clips.indices) return emptyList()
    val from = boundaries[clipIndex]
    val to = boundaries[clipIndex + 1]
    return placedCaptions.filter { it.start < to - TIME_EPSILON && it.end > from + TIME_EPSILON }
}
