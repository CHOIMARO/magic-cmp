package com.company.core.domain.model.editor

/** Shortest video clip length after a trim, in seconds of the source video. A photo clip uses [PHOTO_CLIP_MIN_LENGTH]. */
const val MIN_CLIP_SOURCE_LENGTH = 0.5

/** A split must be at least this far from the clip edges, in seconds. */
const val SPLIT_EDGE_MARGIN = 0.15

/** Shortest time that a text overlay stays on screen after a change of its time, in seconds. */
const val MIN_TEXT_LENGTH = 0.5

/** Smallest size factor of a text overlay. */
const val MIN_TEXT_SCALE = 0.5

/** Largest size factor of a text overlay. */
const val MAX_TEXT_SCALE = 3.0

/** Number of text size steps. */
const val SIZE_STEP_COUNT = 5

/** Shortest animation length that the user can pick, in milliseconds. */
const val MIN_ANIMATION_MILLIS = 200

/** Longest animation length that the user can pick, in milliseconds. */
const val MAX_ANIMATION_MILLIS = 1000

/** Times closer than this, in seconds, count as the same time when the editor checks overlaps. */
internal const val TIME_EPSILON = 1e-6

/**
 * Clip at a time on the timeline.
 *
 * @property clip The clip.
 * @property index Index of the clip in [EditorProject.clips].
 * @property localTime Position in the source video.
 */
data class ClipPosition(val clip: Clip, val index: Int, val localTime: Double)

/**
 * Finds the clip at [time]. A time after the end returns the last clip.
 *
 * @param time Time on the timeline.
 * @return The clip position, or null if the project has no clips.
 */
fun EditorProject.clipAt(time: Double): ClipPosition? {
    var start = 0.0
    clips.forEachIndexed { index, clip ->
        val length = clip.duration
        if (time < start + length || index == clips.lastIndex) {
            return ClipPosition(clip, index, clip.inPoint + (time - start) * clip.speed)
        }
        start += length
    }
    return null
}

/**
 * Splits the clip at [time] into two clips.
 *
 * @param time Time on the timeline.
 * @return The changed project, or null if [time] is not inside a clip.
 */
fun EditorProject.splitAt(time: Double): EditorProject? {
    var start = 0.0
    clips.forEachIndexed { index, clip ->
        val length = clip.duration
        if (time > start + SPLIT_EDGE_MARGIN && time < start + length - SPLIT_EDGE_MARGIN) {
            val local = clip.inPoint + (time - start) * clip.speed
            val first = clip.copy(outPoint = local)
            val second = clip.copy(id = newEditorId(), inPoint = local)
            return copy(clips = clips.take(index) + first + second + clips.drop(index + 1))
        }
        start += length
    }
    return null
}

/**
 * Moves the start of a clip in its source video.
 *
 * @param clipId ID of the clip.
 * @param inPoint New start. The function keeps it inside the valid range.
 */
fun EditorProject.trimClipStart(clipId: String, inPoint: Double): EditorProject = mapClip(clipId) {
    it.copy(inPoint = inPoint.coerceIn(0.0, it.outPoint - it.minSourceLength))
}

/**
 * Moves the end of a clip in its source video.
 *
 * @param clipId ID of the clip.
 * @param outPoint New end. The function keeps it inside the valid range.
 */
fun EditorProject.trimClipEnd(clipId: String, outPoint: Double): EditorProject = mapClip(clipId) {
    it.copy(outPoint = outPoint.coerceIn(it.inPoint + it.minSourceLength, it.sourceDuration))
}

/**
 * Puts a copy of a clip after the clip.
 *
 * @param clipId ID of the clip.
 * @return The changed project and the ID of the copy, or null if the clip does not exist.
 */
fun EditorProject.duplicateClip(clipId: String): Pair<EditorProject, String>? {
    val index = clips.indexOfFirst { it.id == clipId }
    if (index < 0) return null
    val copy = clips[index].copy(id = newEditorId())
    return copy(clips = clips.take(index + 1) + copy + clips.drop(index + 1)) to copy.id
}

/**
 * Removes a clip. The transitions at both edges of the clip and the captions bound to the clip go away.
 *
 * @param clipId ID of the clip.
 */
fun EditorProject.removeClip(clipId: String): EditorProject {
    val index = clips.indexOfFirst { it.id == clipId }
    if (index < 0) return this
    val nextId = clips.getOrNull(index + 1)?.id
    return copy(
        clips = clips.filterNot { it.id == clipId },
        transitions = transitions - setOfNotNull(clipId, nextId),
        // 첫 클립을 지우면 다음 클립이 첫 클립이 되므로 그 클립 앞 전환은 없어진다. 영상 시작 전환은 그대로 둔다.
        captions = captions.filterNot { it.clipId == clipId },
    )
}

/**
 * Changes the volume of the original sound of a clip.
 *
 * @param clipId ID of the clip.
 * @param volume New volume. The function keeps it from 0 to [MAX_VOLUME].
 */
fun EditorProject.setClipVolume(clipId: String, volume: Int): EditorProject = mapClip(clipId) {
    it.copy(volume = volume.coerceIn(0, MAX_VOLUME))
}

/**
 * Changes the play speed of a clip. A photo clip keeps its speed.
 *
 * @param clipId ID of the clip.
 * @param speed New play speed.
 */
fun EditorProject.setClipSpeed(clipId: String, speed: Double): EditorProject = mapClip(clipId) {
    if (it.canChangeSpeed) it.copy(speed = speed) else it
}

/**
 * Adds clips at the end of the timeline.
 *
 * @param newClips Clips to add.
 */
fun EditorProject.addClips(newClips: List<Clip>): EditorProject = copy(clips = clips + newClips)

/**
 * Look of a text overlay that the text sheet changes.
 *
 * @property text Text to show.
 * @property style Look of the text.
 * @property font Font of the text, or null for the font of [style].
 * @property sizeStep Size step from 1 to 5.
 * @property decorations Decorations of character ranges in [text].
 */
data class TextContent(
    val text: String,
    val style: TextStyleType,
    val font: TextFontType? = null,
    val sizeStep: Int = DEFAULT_SIZE_STEP,
    val decorations: List<DecorationRange> = emptyList(),
)

/**
 * Adds a text overlay at [time].
 *
 * The overlay goes to the first row of the text track. When it overlaps a text in that row, it goes to the next row
 * without an overlap. When all rows have an overlap, it goes to a new row.
 *
 * @param content Text and look of the overlay.
 * @param time Current time on the timeline.
 * @return The changed project and the ID of the new overlay.
 */
fun EditorProject.addText(content: TextContent, time: Double): Pair<EditorProject, String> {
    val total = totalDuration
    val start = time.coerceIn(0.0, maxOf(0.0, total - 1))
    val length = maxOf(1.0, minOf(3.0, total - start))
    val overlay = TextOverlay(
        id = newEditorId(),
        text = content.text,
        start = start,
        length = length,
        style = content.style,
        x = 50.0,
        y = 40.0,
        layer = texts.firstFreeLayer(start, start + length),
        font = content.font,
        sizeStep = content.sizeStep.coerceIn(1, SIZE_STEP_COUNT),
        decorations = content.decorations,
    )
    return copy(texts = texts + overlay) to overlay.id
}

/**
 * Adds a text overlay at [time].
 *
 * @param text Text to show.
 * @param style Look of the text.
 * @param time Current time on the timeline.
 * @param font Font of the text, or null for the font of [style].
 */
fun EditorProject.addText(text: String, style: TextStyleType, time: Double, font: TextFontType? = null): EditorProject =
    addText(TextContent(text = text, style = style, font = font), time).first

/**
 * Changes the text and the look of a text overlay. The time, the place, and the animations stay.
 *
 * @param textId ID of the overlay.
 * @param content New text and look.
 */
fun EditorProject.updateText(textId: String, content: TextContent): EditorProject = mapText(textId) {
    it.copy(
        text = content.text,
        style = content.style,
        font = content.font,
        sizeStep = content.sizeStep.coerceIn(1, SIZE_STEP_COUNT),
        decorations = content.decorations,
    )
}

/**
 * Changes the text, the style, and the font of a text overlay.
 *
 * @param textId ID of the overlay.
 * @param text New text.
 * @param style New look.
 * @param font New font, or null for the font of [style].
 */
fun EditorProject.updateText(textId: String, text: String, style: TextStyleType, font: TextFontType? = null): EditorProject =
    mapText(textId) { it.copy(text = text, style = style, font = font) }

/**
 * Changes the entrance or the exit animation of a text overlay.
 *
 * @param textId ID of the overlay.
 * @param entrance New entrance, or null to keep it.
 * @param exit New exit, or null to keep it.
 */
fun EditorProject.setTextAnimation(textId: String, entrance: TextEntrance? = null, exit: TextExit? = null): EditorProject =
    mapText(textId) { it.copy(entrance = entrance ?: it.entrance, exit = exit ?: it.exit) }

/**
 * Changes the length of the entrance or the exit animation of a text overlay.
 *
 * @param textId ID of the overlay.
 * @param entranceMillis New entrance length, or null to keep it.
 * @param exitMillis New exit length, or null to keep it.
 */
fun EditorProject.setTextAnimationLength(textId: String, entranceMillis: Int? = null, exitMillis: Int? = null): EditorProject =
    mapText(textId) {
        it.copy(
            entranceMillis = entranceMillis?.coerceIn(MIN_ANIMATION_MILLIS, MAX_ANIMATION_MILLIS) ?: it.entranceMillis,
            exitMillis = exitMillis?.coerceIn(MIN_ANIMATION_MILLIS, MAX_ANIMATION_MILLIS) ?: it.exitMillis,
        )
    }

/**
 * Removes the animations that the user picked. The text overlay uses the animations of its style again.
 *
 * @param textId ID of the overlay.
 */
fun EditorProject.resetTextAnimation(textId: String): EditorProject =
    mapText(textId) { it.copy(entrance = null, entranceMillis = null, exit = null, exitMillis = null) }

/** True when the user picked an animation or an animation length for the text. */
val TextOverlay.hasCustomAnimation: Boolean
    get() = entrance != null || entranceMillis != null || exit != null || exitMillis != null

/**
 * Removes a text overlay. A row of the text track that becomes empty goes away, and the rows below it move up.
 *
 * @param textId ID of the overlay.
 */
fun EditorProject.removeText(textId: String): EditorProject = copy(texts = texts.filterNot { it.id == textId }).compactTextLayers()

/**
 * Moves the start of a text overlay. The end stays.
 *
 * @param textId ID of the overlay.
 * @param start New start time. The function keeps it at 0 or later, after the previous text in the same row,
 *   and [MIN_TEXT_LENGTH] before the end.
 */
fun EditorProject.trimTextStart(textId: String, start: Double): EditorProject = mapText(textId) { text ->
    // 같은 줄의 앞 글자와 겹치지 않게 한다. 이미 겹쳐 있던 글자는 지금 위치보다 뒤로 밀지 않는다.
    val previousEnd = texts.filter { it.id != text.id && it.layer == text.layer && it.start < text.start }
        .maxOfOrNull { it.end }?.coerceAtMost(text.start) ?: 0.0
    val newStart = start.coerceAtMost(text.end - MIN_TEXT_LENGTH).coerceAtLeast(previousEnd)
    text.copy(start = newStart, length = text.end - newStart)
}

/**
 * Moves the end of a text overlay. The start stays.
 *
 * @param textId ID of the overlay.
 * @param end New end time. The function keeps it inside the project, before the next text in the same row,
 *   and [MIN_TEXT_LENGTH] after the start.
 */
fun EditorProject.trimTextEnd(textId: String, end: Double): EditorProject = mapText(textId) { text ->
    // 같은 줄의 뒤 글자와 겹치지 않게 한다. 이미 겹쳐 있던 글자는 지금 위치보다 앞으로 당기지 않는다.
    val nextStart = texts.filter { it.id != text.id && it.layer == text.layer && it.start > text.start }
        .minOfOrNull { it.start }?.coerceAtLeast(text.end) ?: totalDuration
    val newEnd = end.coerceAtMost(minOf(totalDuration, nextStart)).coerceAtLeast(text.start + MIN_TEXT_LENGTH)
    text.copy(length = newEnd - text.start)
}

/**
 * Cuts a text overlay at the project end. The timeline shows a text only up to the project end, so the edit tools
 * call this before they change the text. A text inside the project, or a text that starts at the end or later,
 * does not change.
 *
 * @param textId ID of the overlay.
 */
fun EditorProject.cutTextAtEnd(textId: String): EditorProject {
    val total = totalDuration
    val text = texts.find { it.id == textId } ?: return this
    if (text.end <= total || text.start >= total) return this
    return mapText(textId) { it.copy(length = (total - it.start).coerceAtLeast(MIN_TEXT_LENGTH)) }
}

/**
 * Moves a text overlay in time, and to another row if [layer] is not null. The length stays.
 *
 * The function does not check overlaps. Use [overlapsOtherText] to check the new place, and call [compactTextLayers]
 * when the move ends.
 *
 * @param textId ID of the overlay.
 * @param start New start time. The function keeps the whole overlay inside the project.
 * @param layer New row, or null to keep the row. The function keeps it at 0 or more.
 */
fun EditorProject.moveTextTime(textId: String, start: Double, layer: Int? = null): EditorProject = mapText(textId) {
    it.copy(
        start = start.coerceAtMost(totalDuration - it.length).coerceAtLeast(0.0),
        layer = layer?.coerceAtLeast(0) ?: it.layer,
    )
}

/**
 * Tells if a text overlay overlaps another text in its row. Texts that only touch do not overlap.
 *
 * @param textId ID of the overlay.
 * @return True if it overlaps. False also when the overlay does not exist.
 */
fun EditorProject.overlapsOtherText(textId: String): Boolean {
    val text = texts.find { it.id == textId } ?: return false
    return texts.overlapsLayer(text.layer, text.start, text.end, exceptId = textId)
}

/**
 * Puts a copy of a text overlay right after it in time. Near the project end, the copy moves back to stay inside the project.
 * The copy goes to the first row without an overlap.
 *
 * @param textId ID of the overlay.
 * @return The changed project and the ID of the copy, or null if the overlay does not exist.
 */
fun EditorProject.duplicateText(textId: String): Pair<EditorProject, String>? {
    val index = texts.indexOfFirst { it.id == textId }
    if (index < 0) return null
    val original = texts[index]
    val start = original.end.coerceAtMost(totalDuration - original.length).coerceAtLeast(0.0)
    val copy = original.copy(id = newEditorId(), start = start, layer = texts.firstFreeLayer(start, start + original.length))
    return copy(texts = texts.take(index + 1) + copy + texts.drop(index + 1)) to copy.id
}

/** Number of rows that the text track needs. It is at least 1, so an empty track shows one row. */
val EditorProject.textLayerCount: Int
    get() = texts.layerCount

/** Number of rows that a track with these items needs. It is at least 1. */
val List<TrackItem>.layerCount: Int
    get() = maxOf(1, (maxOfOrNull { it.layer } ?: 0) + 1)

/**
 * Finds the first row that has no item in a time range.
 *
 * @param start Start of the range.
 * @param end End of the range.
 * @param exceptId ID of an item to ignore, or null.
 * @return The row. It is a new row after the last one when all rows have an overlap.
 */
fun List<TrackItem>.firstFreeLayer(start: Double, end: Double, exceptId: String? = null): Int {
    val lastLayer = filter { it.id != exceptId }.maxOfOrNull { it.layer } ?: -1
    return (0..lastLayer + 1).first { !overlapsLayer(it, start, end, exceptId) }
}

/** Tells if an item in [layer] overlaps the time range. Items that only touch do not overlap. */
fun List<TrackItem>.overlapsLayer(layer: Int, start: Double, end: Double, exceptId: String? = null): Boolean =
    any { it.id != exceptId && it.layer == layer && it.start < end - TIME_EPSILON && start < it.end - TIME_EPSILON }

/**
 * Removes the empty rows. The rows below an empty row move up.
 *
 * @param withLayer Makes a copy of an item with a new row.
 */
fun <T : TrackItem> List<T>.compactLayers(withLayer: (T, Int) -> T): List<T> {
    val used = map { it.layer }.distinct().sorted()
    if (used.withIndex().all { (index, layer) -> index == layer }) return this
    val newLayer = used.withIndex().associate { (index, layer) -> layer to index }
    return map { withLayer(it, newLayer.getValue(it.layer)) }
}

/**
 * Moves an item that overlaps another item in its row to the first free row, then removes the empty rows.
 *
 * @param id ID of the item to check.
 * @param withLayer Makes a copy of an item with a new row.
 */
fun <T : TrackItem> List<T>.relayer(id: String, withLayer: (T, Int) -> T): List<T> {
    val item = find { it.id == id } ?: return compactLayers(withLayer)
    if (!overlapsLayer(item.layer, item.start, item.end, exceptId = id)) return compactLayers(withLayer)
    val layer = firstFreeLayer(item.start, item.end, exceptId = id)
    return map { if (it.id == id) withLayer(it, layer) else it }.compactLayers(withLayer)
}

/** Removes the empty rows of the text track. The rows below an empty row move up. */
fun EditorProject.compactTextLayers(): EditorProject {
    val compacted = texts.compactLayers { text, layer -> text.copy(layer = layer) }
    return if (compacted === texts) this else copy(texts = compacted)
}

private fun EditorProject.mapText(textId: String, transform: (TextOverlay) -> TextOverlay): EditorProject =
    copy(texts = texts.map { if (it.id == textId) transform(it) else it })

/**
 * Moves a text overlay. The position stays inside the safe area of the screen.
 *
 * @param textId ID of the overlay.
 * @param x New horizontal center in percent.
 * @param y New vertical center in percent.
 */
fun EditorProject.moveText(textId: String, x: Double, y: Double): EditorProject = copy(
    texts = texts.map {
        if (it.id == textId) it.copy(x = x.coerceIn(12.0, 88.0), y = y.coerceIn(6.0, 94.0)) else it
    }
)

/**
 * Changes the size and the angle of a text overlay. The whole text box changes, and the line breaks do not change.
 *
 * @param textId ID of the overlay.
 * @param scale New size factor. The function keeps it from [MIN_TEXT_SCALE] to [MAX_TEXT_SCALE].
 * @param rotation New angle in degrees. A positive value turns the text clockwise. The function changes it to the
 *   same angle from -180 to below 180 degrees.
 */
fun EditorProject.transformText(textId: String, scale: Double, rotation: Double): EditorProject = mapText(textId) {
    it.copy(scale = scale.coerceIn(MIN_TEXT_SCALE, MAX_TEXT_SCALE), rotation = normalizeDegrees(rotation))
}

/** Changes an angle to the same angle from -180 to below 180 degrees. */
private fun normalizeDegrees(degrees: Double): Double = ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

/**
 * Applies a template.
 *
 * @param template Template to apply.
 * @param defaultText Text of the new overlay when a text template finds no text.
 * @param fallbackCaptions Captions to use when a caption template finds no captions.
 */
fun EditorProject.applyTemplate(
    template: EditorTemplate,
    defaultText: String,
    fallbackCaptions: List<CaptionSegment>,
): EditorProject = when (template) {
    is EditorTemplate.Layout -> copy(layout = template.layout)
    is EditorTemplate.Text -> copy(
        texts = if (texts.isNotEmpty()) {
            texts.map { it.copy(style = template.style) }
        } else {
            listOf(
                TextOverlay(
                    id = newEditorId(),
                    text = defaultText,
                    start = 0.0,
                    length = minOf(3.0, totalDuration),
                    style = template.style,
                    x = 50.0,
                    y = 26.0,
                )
            )
        }
    )
    is EditorTemplate.Caption -> copy(
        captionStyle = template.style,
        captions = captions.ifEmpty { fallbackCaptions },
    )
}

private inline fun EditorProject.mapClip(clipId: String, transform: (Clip) -> Clip): EditorProject =
    copy(clips = clips.map { if (it.id == clipId) transform(it) else it })
