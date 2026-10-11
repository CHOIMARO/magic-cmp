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
    val captions: List<CaptionSegment> = emptyList(),
) {
    /** Length of the project. It is the sum of all clip lengths. */
    val totalDuration: Double
        get() = clips.sumOf { it.duration }
}

/**
 * One part of a source video, or one photo, on the timeline.
 *
 * A photo clip uses the same fields as a video clip. Its source is [PHOTO_CLIP_MAX_LENGTH] long,
 * so [inPoint] and [outPoint] set how long the photo shows.
 *
 * @property id Unique ID in the project.
 * @property name Name of the source file.
 * @property sourceDuration Length of the source video. For a photo, it is [PHOTO_CLIP_MAX_LENGTH].
 * @property inPoint Start position in the source.
 * @property outPoint End position in the source.
 * @property speed Play speed. 2.0 plays two times faster. A photo clip always uses 1.0.
 * @property thumbnailHue Hue of the placeholder thumbnail. The prototype has no real frames.
 * @property sourceUri Location of the source file. Null for a sample clip that has no real file.
 * @property type Photo or video.
 */
data class Clip(
    val id: String,
    val name: String,
    val sourceDuration: Double,
    val inPoint: Double,
    val outPoint: Double,
    val speed: Double = 1.0,
    val thumbnailHue: Int,
    val sourceUri: String? = null,
    val type: MediaType = MediaType.VIDEO,
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
 * @property layer Row of the text track on the timeline. 0 is the first row. Texts in one row do not overlap in time.
 */
data class TextOverlay(
    val id: String,
    val text: String,
    val start: Double,
    val length: Double,
    val style: TextStyleType,
    val x: Double,
    val y: Double,
    val layer: Int = 0,
) {
    /** End time on the timeline. */
    val end: Double
        get() = start + length
}

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
