package com.company.domain.model.editor

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
 * @property watermark True to show the "magic" watermark on the video.
 */
data class EditorSettings(
    val templateNews: Boolean = false,
    val watermark: Boolean = false,
)

/**
 * Voice that reads a dubbing.
 *
 * @property id Unique ID of the voice.
 * @property name Name of the voice.
 * @property description Short description, for example "여성 · 맑고 밝은 목소리".
 * @property hue Hue of the avatar.
 */
data class DubbingVoice(
    val id: String,
    val name: String,
    val description: String,
    val hue: Int,
)

/**
 * Sound effect in the catalog.
 *
 * @property id Unique ID of the effect.
 * @property category Category of the effect.
 * @property name Name of the effect.
 * @property length Length in seconds, from 0.1 to 5.
 */
data class SoundEffect(
    val id: String,
    val category: SoundEffectCategory,
    val name: String,
    val length: Double,
)

/** State of the voice that the user records ("내 목소리"). */
sealed interface MyVoiceStatus {
    /** None : The user has no voice. */
    data object None : MyVoiceStatus

    /** Making(percent) : The voice is in production. [percent] goes from 0 to 100. */
    data class Making(val percent: Int) : MyVoiceStatus

    /** Ready : The voice is ready for dubbings. */
    data object Ready : MyVoiceStatus
}

/** ID of the user's own voice in [Dubbing.voiceId]. */
const val MY_VOICE_ID = "me"
