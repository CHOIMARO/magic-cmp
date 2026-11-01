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

/** Time on screen of each script line where no clip is left, in seconds (F16). */
const val SCRIPT_LINE_SECONDS = 2.0

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
 * A caption bound to a clip (D47) shows where its part of the source video plays now. The clip edges cut it, and a
 * caption whose part is trimmed away is left out. A caption of a clip that is not in the project is left out. The
 * stored source times do not change, so a bound caption comes back when the clip gets longer again.
 *
 * A caption with a fixed time keeps its time, also after the last clip. It then makes the video longer (F16).
 */
val EditorProject.placedCaptions: List<CaptionSegment>
    get() {
        if (captions.all { it.clipId == null }) return captions
        val starts = clipBoundaries()
        val indexes = clips.withIndex().associate { (index, clip) -> clip.id to index }
        return captions.mapNotNull { caption ->
            val clipId = caption.clipId ?: return@mapNotNull caption
            val index = indexes[clipId] ?: return@mapNotNull null
            val clip = clips[index]
            val clipStart = starts[index]
            // 원본 시간을 지금 클립의 시작점과 속도로 타임라인 시간으로 바꾼다. 잘려 나간 부분은 보이지 않는다.
            val from = clipStart + ((caption.sourceStart ?: clip.inPoint) - clip.inPoint) / clip.speed
            val to = clipStart + ((caption.sourceEnd ?: clip.outPoint) - clip.inPoint) / clip.speed
            val start = maxOf(from, clipStart)
            val end = minOf(to, starts[index + 1])
            if (end - start < MIN_PLACED_CAPTION_LENGTH) null else caption.copy(start = start, length = end - start)
        }
    }

/**
 * Binds a caption to [clip] at its time on the timeline (D47). The caption keeps this part of the source video.
 *
 * @param clip Clip that plays during the caption.
 * @param clipStart Start of the clip on the timeline.
 * @return The bound caption.
 */
fun CaptionSegment.boundTo(clip: Clip, clipStart: Double): CaptionSegment = copy(
    clipId = clip.id,
    sourceStart = clip.inPoint + (start - clipStart) * clip.speed,
    sourceEnd = clip.inPoint + (end - clipStart) * clip.speed,
)

/** The caption at [start] for [length] seconds, with a fixed time. A binding to a clip ends. */
private fun CaptionSegment.withFixedTime(start: Double, length: Double): CaptionSegment =
    copy(start = start, length = length, clipId = null, sourceStart = null, sourceEnd = null)

/**
 * Splits a typed script into caption lines. Each line of the script is one caption. Empty lines and the spaces at
 * the start and the end of a line are ignored.
 *
 * @param script Text that the user typed or pasted.
 */
fun scriptLines(script: String): List<String> = script.lines().map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Makes captions with fixed times from script lines (D47).
 *
 * From a clip, the n-th line covers the n-th clip from [ScriptStart.clipIndex], and the first line starts at
 * [ScriptStart.offset]. Lines after the last clip are not used. Clips after the last line get no caption.
 *
 * When no clip is left at the start, each line stays on screen for [SCRIPT_LINE_SECONDS] from [ScriptStart.time], one
 * after another (F16). Lines that end after [MAX_PROJECT_LENGTH] are not used.
 *
 * @param lines Caption lines.
 * @param start Place of the first line. See [scriptStart].
 * @return Captions with fixed times.
 */
fun EditorProject.captionsFromScript(lines: List<String>, start: ScriptStart = ScriptStart(0, 0.0, 0.0)): List<CaptionSegment> {
    if (start.clipIndex >= clips.size) {
        return lines.mapIndexed { index, text ->
            CaptionSegment(id = newEditorId(), text = text, start = start.time + index * SCRIPT_LINE_SECONDS, length = SCRIPT_LINE_SECONDS)
        }.filter { it.end <= MAX_PROJECT_LENGTH + TIME_EPSILON }
    }
    val boundaries = clipBoundaries()
    return lines.zip(clips.indices.drop(start.clipIndex)).mapIndexed { line, (text, index) ->
        val from = boundaries[index] + if (line == 0) start.offset else 0.0
        CaptionSegment(id = newEditorId(), text = text, start = from, length = boundaries[index + 1] - from)
    }
}

/**
 * Number of script lines that [captionsFromScript] can use from [start]: the clips that are left, or all lines when
 * no clip is left.
 *
 * @param start Place of the first line.
 * @return The number of lines, or null when the lines do not depend on the clips.
 */
fun EditorProject.scriptClipCount(start: ScriptStart): Int? = (clips.size - start.clipIndex).takeIf { it > 0 }

/**
 * Place where the script lines start when the user makes captions from the playhead.
 *
 * @property clipIndex Index of the clip of the first line. It is the number of clips when no clip is left.
 * @property offset Time in that clip where the first line starts.
 * @property time The same place on the timeline. When no clip is left, it is the playhead.
 */
data class ScriptStart(val clipIndex: Int, val offset: Double, val time: Double)

/**
 * Finds where the script lines start for captions from [time] (F04).
 *
 * The first line starts at [time] in the clip at [time]. When [time] is less than [MIN_CAPTION_LENGTH] before the
 * end of that clip, the first line goes to the next clip. A time at 0 starts at the first clip. When no clip is left,
 * the lines start at [time] (F16).
 *
 * @param time Playhead position.
 */
fun EditorProject.scriptStart(time: Double): ScriptStart {
    if (time <= TIME_EPSILON) return ScriptStart(clipIndex = 0, offset = 0.0, time = 0.0)
    if (clips.isEmpty()) return ScriptStart(clipIndex = 0, offset = 0.0, time = time)
    val boundaries = clipBoundaries()
    var clipIndex = clipAt(time)?.index ?: 0
    var offset = time - boundaries[clipIndex]
    if (offset > clips[clipIndex].duration - MIN_CAPTION_LENGTH) {
        clipIndex++
        offset = 0.0
    }
    if (offset <= SCRIPT_OFFSET_MIN) offset = 0.0
    val start = if (clipIndex < clips.size) boundaries[clipIndex] + offset else time
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
            else -> caption.withFixedTime(position.start, time - position.start)
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
    val first = caption.withFixedTime(caption.start, time - caption.start).copy(
        text = firstText,
        decorations = caption.decorations.slice(0, firstText.length),
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
 * The edge stays between the neighbor captions, [MIN_CAPTION_LENGTH] from the other edge, and not later than
 * [MAX_PROJECT_LENGTH]. An end after the video end makes the video longer (F16).
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
        ?: MAX_PROJECT_LENGTH
    val newStart = if (start) time.coerceAtMost(caption.end - MIN_CAPTION_LENGTH).coerceAtLeast(previousEnd) else caption.start
    val newEnd = if (start) caption.end else time.coerceAtMost(nextStart).coerceAtLeast(caption.start + MIN_CAPTION_LENGTH)
    return copy(
        captions = captions.map {
            if (it.id == captionId) {
                it.withFixedTime(newStart, newEnd - newStart)
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
