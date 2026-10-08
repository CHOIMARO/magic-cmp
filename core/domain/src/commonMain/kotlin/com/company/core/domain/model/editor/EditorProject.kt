package com.company.core.domain.model.editor

import kotlin.random.Random

/**
 * Video project that the user edits.
 *
 * All times are in seconds on the project timeline.
 *
 * @property name Name of the project.
 * @property clips Clips in play order.
 * @property layout Screen layout of the clips.
 * @property captionStyle Style of the automatic captions.
 * @property texts Text overlays.
 * @property musicId ID of the background music. Null means no music.
 * @property filter Color filter for all clips.
 * @property vibe Last vibe preset that the user applied. Null means no preset.
 * @property captions Automatic caption segments.
 */
data class EditorProject(
    val name: String,
    val clips: List<Clip> = emptyList(),
    val layout: LayoutType = LayoutType.FULL,
    val captionStyle: CaptionStyleType = CaptionStyleType.POP,
    val texts: List<TextOverlay> = emptyList(),
    val musicId: String? = null,
    val filter: FilterType = FilterType.NONE,
    val vibe: Vibe? = null,
    val captions: List<CaptionSegment> = emptyList(),
) {
    /** Length of the project. It is the sum of all clip lengths. */
    val totalDuration: Double
        get() = clips.sumOf { it.duration }
}

/**
 * One part of a source video on the timeline.
 *
 * @property id Unique ID in the project.
 * @property name Name of the source video.
 * @property sourceDuration Length of the source video.
 * @property inPoint Start position in the source video.
 * @property outPoint End position in the source video.
 * @property speed Play speed. 2.0 plays two times faster.
 * @property thumbnailHue Hue of the placeholder thumbnail. The prototype has no real frames.
 */
data class Clip(
    val id: String,
    val name: String,
    val sourceDuration: Double,
    val inPoint: Double,
    val outPoint: Double,
    val speed: Double = 1.0,
    val thumbnailHue: Int,
) {
    /** Length of the clip on the timeline. */
    val duration: Double
        get() = (outPoint - inPoint) / speed
}

/**
 * Text that shows on top of the video for a time range.
 *
 * @property id Unique ID in the project.
 * @property text Text to show.
 * @property start Start time on the timeline.
 * @property length Time on screen.
 * @property style Look of the text.
 * @property x Horizontal center in percent of the screen width.
 * @property y Vertical center in percent of the screen height.
 */
data class TextOverlay(
    val id: String,
    val text: String,
    val start: Double,
    val length: Double,
    val style: TextStyleType,
    val x: Double,
    val y: Double,
)

/**
 * One automatic caption line.
 *
 * @property id Unique ID in the project.
 * @property text Spoken words of the line.
 * @property start Start time on the timeline.
 * @property length Time on screen.
 */
data class CaptionSegment(
    val id: String,
    val text: String,
    val start: Double,
    val length: Double,
)

/** Creates a new random ID for clips, texts, and captions. */
fun newEditorId(): String = "c" + Random.nextLong().toULong().toString(16)
