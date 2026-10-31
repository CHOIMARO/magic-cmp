package com.company.domain.model.editor

/** Show time of a new photo clip, in seconds. */
const val PHOTO_CLIP_DEFAULT_LENGTH = 2.0

/** Shortest show time of a photo clip, in seconds. */
const val PHOTO_CLIP_MIN_LENGTH = 0.1

/** Longest show time of a photo clip, in seconds. It is 10 minutes. */
const val PHOTO_CLIP_MAX_LENGTH = 600.0

/** True when the user can change the play speed of the clip. A photo has no speed. */
val Clip.canChangeSpeed: Boolean
    get() = type == MediaType.VIDEO

/** Shortest length of the clip after a trim, in seconds of the source. */
val Clip.minSourceLength: Double
    get() = if (type == MediaType.PHOTO) PHOTO_CLIP_MIN_LENGTH else MIN_CLIP_SOURCE_LENGTH

/**
 * Makes a clip from a picked photo or video.
 *
 * A video clip uses the whole video. A photo clip shows for [PHOTO_CLIP_DEFAULT_LENGTH].
 */
fun SourceMedia.toClip(): Clip = when (type) {
    MediaType.VIDEO -> Clip(
        id = newEditorId(),
        name = name,
        sourceDuration = duration ?: 0.0,
        inPoint = 0.0,
        outPoint = duration ?: 0.0,
        thumbnailHue = thumbnailHue,
        sourceUri = uri,
        type = MediaType.VIDEO,
    )
    MediaType.PHOTO -> Clip(
        id = newEditorId(),
        name = name,
        sourceDuration = PHOTO_CLIP_MAX_LENGTH,
        inPoint = 0.0,
        outPoint = PHOTO_CLIP_DEFAULT_LENGTH,
        thumbnailHue = thumbnailHue,
        sourceUri = uri,
        type = MediaType.PHOTO,
    )
}

/**
 * Time range on the timeline, in seconds.
 *
 * @property start Start time.
 * @property end End time.
 */
data class TimelineRange(val start: Double, val end: Double) {
    /** Length of the range. */
    val length: Double
        get() = end - start
}

/**
 * Finds the parts of the timeline that can have speech. Photo clips have no sound, so they are not included.
 *
 * Video clips that touch each other become one range.
 *
 * @return Ranges in time order.
 */
fun List<Clip>.speechRanges(): List<TimelineRange> {
    val ranges = mutableListOf<TimelineRange>()
    var time = 0.0
    forEach { clip ->
        val end = time + clip.duration
        if (clip.type == MediaType.VIDEO) {
            val last = ranges.lastOrNull()
            if (last != null && last.end == time) {
                ranges[ranges.lastIndex] = last.copy(end = end)
            } else {
                ranges += TimelineRange(time, end)
            }
        }
        time = end
    }
    return ranges
}

/**
 * Parts of [speechRanges] from [from]. A part that starts before [from] starts at [from]. Parts shorter than
 * [MIN_CAPTION_LENGTH] are left out, because they are too short for a caption.
 *
 * @param from Time on the timeline. 0 gives all parts.
 */
fun List<Clip>.speechRangesFrom(from: Double): List<TimelineRange> =
    speechRanges().map { TimelineRange(maxOf(it.start, from), it.end) }.filter { it.length >= MIN_CAPTION_LENGTH }
