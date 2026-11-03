package com.company.domain.repository

import com.company.domain.model.editor.DubbingAudio
import com.company.domain.model.editor.DubbingContent
import com.company.domain.model.editor.DubbingVoice
import kotlinx.coroutines.flow.StateFlow

/**
 * Makes spoken audio from a text with a voice (text to speech). The server makes the audio (F05, F17).
 *
 * The voice list is public. The preview and the audio need an account.
 */
interface DubbingRepository {
    /**
     * Voices that every user can use, in the order of the server. The user's own voice is not in the list.
     * The list is empty until [loadVoices] gets it.
     */
    val voices: StateFlow<List<DubbingVoice>>

    /** Gets the voices from the server. A failure keeps the last list. */
    suspend fun loadVoices()

    /**
     * Makes the audio of a dubbing for a preview. A later [synthesize] with the same content uses the same audio.
     *
     * @param content Text, voice, and speed.
     * @return Address of the audio to stream. It is valid for a short time only, so play it at once.
     * @throws Exception when the audio cannot be made.
     */
    suspend fun preview(content: DubbingContent): String

    /**
     * Makes the audio of a dubbing and keeps the file on the device.
     *
     * @param content Text, voice, and speed.
     * @return The audio.
     * @throws Exception when the audio cannot be made.
     */
    suspend fun synthesize(content: DubbingContent): DubbingAudio
}
