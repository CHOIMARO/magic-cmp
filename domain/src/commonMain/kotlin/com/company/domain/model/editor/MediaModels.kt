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
 * Short summary of a work in "최근 작업" and "내 영상" (F15). A work is one edit that the user started in the editor.
 * Saves and exports do not make a new work.
 *
 * @property id Unique ID of the work.
 * @property name Name of the project.
 * @property duration Length of the video in seconds.
 * @property thumbnailHue Hue of the first clip, or null for a black card when the project has no clips (F16).
 * @property modifiedAt Time of the last saved change, in milliseconds since 1970-01-01 UTC. When the user only opens
 *   or exports the work, it does not change (D44).
 * @property exported True after the user exported the work one time or more (D43).
 * @property storageBytes Bytes that the work uses on the device: the copies of its source files and its project
 *   file (D51).
 * @property thumbnail First frame of the work for its card, or null when the first clip has no source file, for
 *   example a sample work (D52). The card then shows the placeholder of [thumbnailHue].
 */
data class VideoSummary(
    val id: String,
    val name: String,
    val duration: Double,
    val thumbnailHue: Int?,
    val modifiedAt: Long,
    val exported: Boolean = false,
    val storageBytes: Long = 0,
    val thumbnail: WorkThumbnail? = null,
)

/**
 * First frame of a work for its card (D52): the frame at the start of the first clip. It has no texts, captions, or
 * layout.
 *
 * @property uri Location of the source file of the first clip.
 * @property type Photo or video.
 * @property time Source time of the frame in seconds: the in point of the first clip. A photo ignores it.
 */
data class WorkThumbnail(val uri: String, val type: MediaType, val time: Double)

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
 * @property favoriteTextStyles Text templates that the user marked as favorites, the newest first (F14). They are
 *   not part of a project, so the undo history does not keep them.
 */
data class EditorSettings(
    val templateNews: Boolean = false,
    val watermark: Boolean = false,
    val favoriteTextStyles: List<TextStyleType> = emptyList(),
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
