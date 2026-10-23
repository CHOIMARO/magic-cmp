package com.company.data.editor

import com.company.domain.model.editor.DubbingAudio
import com.company.domain.model.editor.DubbingContent
import com.company.domain.model.editor.DubbingVoice
import com.company.domain.repository.DubbingRepository
import kotlinx.coroutines.delay

/**
 * Simulates the text to speech service. It makes no audio file.
 *
 * The length of the audio is an estimate: 0.16 seconds for each character without spaces, divided by the speed.
 * Replace this class when the vendor is decided (PRD D14).
 */
class FakeDubbingRepository : DubbingRepository {
    override fun getVoices(): List<DubbingVoice> = EditorSampleData.Voices

    override suspend fun synthesize(content: DubbingContent): DubbingAudio {
        delay(SYNTHESIS_MILLIS)
        val characters = content.text.count { !it.isWhitespace() }
        val length = maxOf(MIN_LENGTH, characters * SECONDS_PER_CHARACTER / content.speed.factor)
        return DubbingAudio(length = length, audioUri = null)
    }

    private companion object {
        const val SYNTHESIS_MILLIS = 1400L
        const val SECONDS_PER_CHARACTER = 0.16
        const val MIN_LENGTH = 0.6
    }
}
