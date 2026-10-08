package com.company.core.domain.model.editor

/** Shortest clip length after a trim, in seconds of the source video. */
const val MIN_CLIP_SOURCE_LENGTH = 0.5

/** A split must be at least this far from the clip edges, in seconds. */
const val SPLIT_EDGE_MARGIN = 0.15

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
    it.copy(inPoint = inPoint.coerceIn(0.0, it.outPoint - MIN_CLIP_SOURCE_LENGTH))
}

/**
 * Moves the end of a clip in its source video.
 *
 * @param clipId ID of the clip.
 * @param outPoint New end. The function keeps it inside the valid range.
 */
fun EditorProject.trimClipEnd(clipId: String, outPoint: Double): EditorProject = mapClip(clipId) {
    it.copy(outPoint = outPoint.coerceIn(it.inPoint + MIN_CLIP_SOURCE_LENGTH, it.sourceDuration))
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
 * Removes a clip.
 *
 * @param clipId ID of the clip.
 */
fun EditorProject.removeClip(clipId: String): EditorProject = copy(clips = clips.filterNot { it.id == clipId })

/**
 * Changes the play speed of a clip.
 *
 * @param clipId ID of the clip.
 * @param speed New play speed.
 */
fun EditorProject.setClipSpeed(clipId: String, speed: Double): EditorProject = mapClip(clipId) { it.copy(speed = speed) }

/**
 * Adds clips at the end of the timeline.
 *
 * @param newClips Clips to add.
 */
fun EditorProject.addClips(newClips: List<Clip>): EditorProject = copy(clips = clips + newClips)

/**
 * Applies a vibe preset.
 *
 * @param vibe Preset to apply.
 */
fun EditorProject.applyVibe(vibe: Vibe): EditorProject = copy(
    vibe = vibe,
    filter = vibe.filter,
    musicId = if (vibe == Vibe.NONE) musicId else vibe.musicId,
    captionStyle = vibe.captionStyle ?: captionStyle,
    texts = vibe.textStyle?.let { style -> texts.map { it.copy(style = style) } } ?: texts,
)

/**
 * Adds a text overlay at [time].
 *
 * @param text Text to show.
 * @param style Look of the text.
 * @param time Current time on the timeline.
 */
fun EditorProject.addText(text: String, style: TextStyleType, time: Double): EditorProject {
    val total = totalDuration
    val start = time.coerceIn(0.0, maxOf(0.0, total - 1))
    val overlay = TextOverlay(
        id = newEditorId(),
        text = text,
        start = start,
        length = maxOf(1.0, minOf(3.0, total - start)),
        style = style,
        x = 50.0,
        y = 40.0,
    )
    return copy(texts = texts + overlay)
}

/**
 * Changes the text and the style of a text overlay.
 *
 * @param textId ID of the overlay.
 * @param text New text.
 * @param style New look.
 */
fun EditorProject.updateText(textId: String, text: String, style: TextStyleType): EditorProject =
    copy(texts = texts.map { if (it.id == textId) it.copy(text = text, style = style) else it })

/**
 * Removes a text overlay.
 *
 * @param textId ID of the overlay.
 */
fun EditorProject.removeText(textId: String): EditorProject = copy(texts = texts.filterNot { it.id == textId })

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
