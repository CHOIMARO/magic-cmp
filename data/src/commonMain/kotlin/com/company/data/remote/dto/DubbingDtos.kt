package com.company.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Body of the dubbing request. Put exactly one of [presetVoice] and [customVoiceId].
 *
 * @property text Text that the voice reads, up to 200 characters.
 * @property presetVoice ID of a voice of the server, for example "SEOHYEON".
 * @property customVoiceId ID of the own voice of the user.
 * @property speed Reading speed from 0.5 to 2.0.
 */
@Serializable
internal data class CreateDubbingJobRequest(
    val text: String,
    val presetVoice: String? = null,
    val customVoiceId: Long? = null,
    val speed: Double,
)

/**
 * Dubbing job of the server.
 *
 * @property id ID of the job.
 * @property status PENDING, PROCESSING, COMPLETED, or FAILED.
 * @property audioUrl Presigned address of the mp3 file when the job is complete. It is valid for 10 minutes.
 */
@Serializable
internal data class DubbingJobResponse(
    val id: Long,
    val status: String,
    val audioUrl: String? = null,
)

/**
 * Voice of the server.
 *
 * @property id ID of the voice.
 * @property name Name of the voice.
 * @property description Short description of the voice.
 */
@Serializable
internal data class PresetVoiceResponse(val id: String, val name: String, val description: String = "")

/**
 * Options of the dubbing sheet. The API is public.
 *
 * @property voices Voices of the server.
 * @property maxTextLength Largest number of characters of a text.
 */
@Serializable
internal data class DubbingOptionsResponse(
    val voices: List<PresetVoiceResponse> = emptyList(),
    val maxTextLength: Int? = null,
)

/** Status values of [DubbingJobResponse.status]. */
internal object DubbingJobStatus {
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
}
