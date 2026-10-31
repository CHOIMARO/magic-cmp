package com.company.domain.model.editor

import kotlin.random.Random

/**
 * Video project that the user edits.
 *
 * All times are in seconds on the project timeline.
 *
 * @property name Name of the project.
 * @property clips Clips in play order.
 * @property layout Screen layout of the clips.
 * @property captionStyle Style of the captions.
 * @property texts Text overlays.
 * @property musicId ID of the background music. Null means no music.
 * @property captions Caption segments. A caption that is bound to a clip gets its time from the clip.
 *   Use [placedCaptions] for the times on the timeline.
 * @property captionPosition Height of all captions from the bottom of the screen, in percent of the screen height.
 *   Null uses the height of the caption style. See [captionPositionFor].
 * @property musicRange Part of the music track that plays from the project start, or null for the whole track.
 * @property musicFade True to fade the music in and out for 1 second at the start and the end.
 * @property musicVolume Volume of the music, from 0 to 100.
 * @property dubbings Dubbings: spoken audio that a voice made from a text.
 * @property soundEffects Short sound effects.
 * @property transitions Transitions between clips. The key is the ID of the clip after the boundary.
 * @property openingTransition Transition at the start of the video, or null.
 * @property draftId ID of the draft in the "내 영상" list that shows this project, or null. The draft gets the length
 *   and the thumbnail of the project when the user leaves the editor.
 */
data class EditorProject(
    val name: String,
    val clips: List<Clip> = emptyList(),
    val layout: LayoutType = LayoutType.FULL,
    val captionStyle: CaptionStyleType = CaptionStyleType.POP,
    val texts: List<TextOverlay> = emptyList(),
    val musicId: String? = null,
    val captions: List<CaptionSegment> = emptyList(),
    val captionPosition: Double? = null,
    val musicRange: MusicRange? = null,
    val musicFade: Boolean = true,
    val musicVolume: Int = DEFAULT_MUSIC_VOLUME,
    val dubbings: List<Dubbing> = emptyList(),
    val soundEffects: List<SoundEffectClip> = emptyList(),
    val transitions: Map<String, ClipTransition> = emptyMap(),
    val openingTransition: ClipTransition? = null,
    val draftId: String? = null,
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
 * @property volume Volume of the original sound, from 0 to 100. A photo has no sound.
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
    val volume: Int = MAX_VOLUME,
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
 * @property font Font of the text, or null for the font of [style].
 * @property scale Size factor of the whole text box. 1 is the size of [style]. The line breaks do not change.
 * @property rotation Angle of the whole text box in degrees, from -180 to below 180. A positive value turns it
 *   clockwise. The rotation of [style] adds to it.
 * @property sizeStep Size step from 1 to 5. 3 is the size of [style]. The size on screen is the step size times [scale].
 * @property decorations Decorations of character ranges. They go on top of [style].
 * @property entrance Entrance animation, or null for the entrance of [style].
 * @property entranceMillis Length of the entrance, or null for the length of [style].
 * @property exit Exit animation, or null for [TextExit.FADE].
 * @property exitMillis Length of the exit, or null for the default length.
 * @property loop Movement that repeats while the text is on screen, or null for no movement.
 */
data class TextOverlay(
    override val id: String,
    val text: String,
    override val start: Double,
    override val length: Double,
    val style: TextStyleType,
    val x: Double,
    val y: Double,
    override val layer: Int = 0,
    val font: TextFontType? = null,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val sizeStep: Int = DEFAULT_SIZE_STEP,
    val decorations: List<DecorationRange> = emptyList(),
    val entrance: TextEntrance? = null,
    val entranceMillis: Int? = null,
    val exit: TextExit? = null,
    val exitMillis: Int? = null,
    val loop: TextLoop? = null,
) : TrackItem

/**
 * One caption line.
 *
 * A caption from speech has a fixed time. A caption from a typed script is bound to a clip: it moves and changes its
 * length with the clip. A trim of the caption on the timeline ends the binding.
 *
 * @property id Unique ID in the project.
 * @property text Words of the line.
 * @property start Start time on the timeline. A bound caption gets its time from the clip, see [EditorProject.placedCaptions].
 * @property length Time on screen.
 * @property decorations Decorations of character ranges. They go on top of the caption style.
 * @property clipId ID of the clip that the caption is bound to, or null for a fixed time.
 * @property clipOffset Seconds from the clip start to the caption start, or null to cover the whole clip.
 * @property clipLength Time on screen of a bound caption with [clipOffset]. The clip end cuts it.
 */
data class CaptionSegment(
    val id: String,
    val text: String,
    val start: Double,
    val length: Double,
    val decorations: List<DecorationRange> = emptyList(),
    val clipId: String? = null,
    val clipOffset: Double? = null,
    val clipLength: Double? = null,
) {
    /** End time on the timeline. */
    val end: Double
        get() = start + length
}

/**
 * Decoration of letters. A null or false value keeps the look of the style.
 *
 * @property color Text color as 0xRRGGBB, or null.
 * @property bold True to use the bold weight of the font.
 * @property outline True to draw a black outline.
 * @property shadow True to draw a soft shadow.
 * @property background Background color as 0xRRGGBB, or null. The text color then becomes white or black automatically.
 */
data class Decoration(
    val color: Int? = null,
    val bold: Boolean = false,
    val outline: Boolean = false,
    val shadow: Boolean = false,
    val background: Int? = null,
) {
    /** True when the decoration changes nothing. */
    val isEmpty: Boolean
        get() = color == null && !bold && !outline && !shadow && background == null
}

/**
 * Decoration of a range of characters.
 *
 * @property start Index of the first character.
 * @property end Index after the last character.
 * @property decoration Decoration of the range.
 */
data class DecorationRange(val start: Int, val end: Int, val decoration: Decoration)

/**
 * Part of a music track, in seconds of the track.
 *
 * @property start Start in the track.
 * @property end End in the track.
 */
data class MusicRange(val start: Double, val end: Double) {
    /** Length of the part. */
    val length: Double
        get() = end - start
}

/**
 * Spoken audio that a voice made from a text.
 *
 * @property id Unique ID in the project.
 * @property text Text that the voice reads.
 * @property voiceId ID of the voice.
 * @property speed Reading speed.
 * @property start Start time on the timeline.
 * @property length Length on the timeline.
 * @property layer Row of the dubbing track. Dubbings in one row do not overlap in time.
 * @property volume Volume from 0 to 100.
 * @property captionId ID of the caption that the text came from, or null.
 * @property rate Play rate. A value above 1 plays faster, so that the dubbing fits its caption.
 * @property audioUri Location of the audio file, or null when the app has no file.
 */
data class Dubbing(
    override val id: String,
    val text: String,
    val voiceId: String,
    val speed: DubbingSpeed,
    override val start: Double,
    override val length: Double,
    override val layer: Int = 0,
    val volume: Int = MAX_VOLUME,
    val captionId: String? = null,
    val rate: Double = 1.0,
    val audioUri: String? = null,
) : TrackItem

/**
 * Sound effect on the timeline.
 *
 * @property id Unique ID in the project.
 * @property effectId ID of the effect in the catalog.
 * @property name Name of the effect.
 * @property start Start time on the timeline.
 * @property length Length of the effect. The user cannot trim it.
 * @property layer Row of the effect track. Effects in one row do not overlap in time.
 * @property volume Volume from 0 to 100.
 */
data class SoundEffectClip(
    override val id: String,
    val effectId: String,
    val name: String,
    override val start: Double,
    override val length: Double,
    override val layer: Int = 0,
    val volume: Int = MAX_VOLUME,
) : TrackItem

/**
 * Item in a track with rows: a text overlay, a dubbing, or a sound effect. Items in one row do not overlap in time.
 * Items that only touch do not overlap.
 */
interface TrackItem {
    /** Unique ID in the project. */
    val id: String

    /** Start time on the timeline. */
    val start: Double

    /** Length on the timeline. */
    val length: Double

    /** Row of the track. 0 is the first row. */
    val layer: Int

    /** End time on the timeline. */
    val end: Double
        get() = start + length
}

/**
 * Transition at a clip boundary or at the start of the video.
 *
 * @property type Kind of the transition.
 * @property direction Side that the next scene comes from. Only [TransitionType.PUSH] uses it.
 */
data class ClipTransition(val type: TransitionType, val direction: PushDirection = PushDirection.RIGHT)

/** Size step of a new text. */
const val DEFAULT_SIZE_STEP = 3

/** Largest volume. */
const val MAX_VOLUME = 100

/** Volume of new music. */
const val DEFAULT_MUSIC_VOLUME = 80

/** Creates a new random ID for clips, texts, captions, dubbings, and sound effects. */
fun newEditorId(): String = "c" + Random.nextLong().toULong().toString(16)
