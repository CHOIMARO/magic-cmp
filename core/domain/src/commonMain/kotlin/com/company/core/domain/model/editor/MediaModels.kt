package com.company.core.domain.model.editor

/** Kind of a media file that the user can add to a project. */
enum class MediaType {
    /** VIDEO : Video file. It has its own length and sound. */
    VIDEO,

    /** PHOTO : Still photo. It shows for a length that the user sets, and it has no sound. */
    PHOTO,
}

/**
 * Photo or video that the user picked in the system picker.
 *
 * @property uri Location of the file. It is also the unique ID of the item.
 *   Android uses a content URI. iOS uses a file URL.
 * @property type Photo or video.
 * @property name File name without the extension.
 * @property duration Length of the video in seconds. Null for a photo.
 * @property thumbnailHue Hue of the placeholder thumbnail. Screens without real frames use it.
 */
data class SourceMedia(
    val uri: String,
    val type: MediaType,
    val name: String,
    val duration: Double?,
    val thumbnailHue: Int,
)

/**
 * Short summary of a project in the "내 영상" list.
 *
 * @property name Name of the project.
 * @property duration Length of the project in seconds.
 * @property thumbnailHue Hue of the placeholder thumbnail.
 */
data class VideoSummary(
    val name: String,
    val duration: Double,
    val thumbnailHue: Int,
)

/**
 * Background music track.
 *
 * @property id Unique ID of the track.
 * @property title Title of the track.
 * @property mood Short mood description.
 * @property durationSeconds Length of the track.
 * @property thumbnailHue Hue of the placeholder cover.
 */
data class MusicTrack(
    val id: String,
    val title: String,
    val mood: String,
    val durationSeconds: Int,
    val thumbnailHue: Int,
)

/**
 * User settings of the editor.
 *
 * @property templateNews True to get news about new templates.
 * @property autoMagic True to decorate new videos automatically.
 * @property watermark True to show the "magic" watermark on the video.
 */
data class EditorSettings(
    val templateNews: Boolean = false,
    val autoMagic: Boolean = true,
    val watermark: Boolean = false,
)
