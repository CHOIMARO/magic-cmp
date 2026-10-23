package com.company.core.domain.repository

import com.company.core.domain.model.editor.DubbingAudio
import com.company.core.domain.model.editor.DubbingContent
import com.company.core.domain.model.editor.DubbingVoice

/** Makes spoken audio from a text with a voice (text to speech). */
interface DubbingRepository {
    /** Returns the voices that every user can use. The user's own voice is not in the list. */
    fun getVoices(): List<DubbingVoice>

    /**
     * Makes the audio of a dubbing.
     *
     * @param content Text, voice, and speed.
     * @return The audio.
     * @throws Exception when the audio cannot be made.
     */
    suspend fun synthesize(content: DubbingContent): DubbingAudio
}
