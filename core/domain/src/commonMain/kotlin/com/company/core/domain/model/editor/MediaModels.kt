package com.company.core.domain.model.editor

/**
 * Video in the device gallery that the user can add to a project.
 *
 * @property id Unique ID of the video.
 * @property name File name of the video.
 * @property duration Length of the video in seconds.
 * @property thumbnailHue Hue of the placeholder thumbnail.
 */
data class SourceVideo(
    val id: String,
    val name: String,
    val duration: Double,
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
