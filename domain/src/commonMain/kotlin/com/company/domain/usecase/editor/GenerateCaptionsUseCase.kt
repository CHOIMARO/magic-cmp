package com.company.domain.usecase.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.Clip
import com.company.domain.repository.CaptionRepository

/** Makes captions for the video clips of a project. Photo clips get no captions. */
class GenerateCaptionsUseCase(
    private val captionRepository: CaptionRepository,
) {
    /**
     * @param clips Clips of the project in play order.
     * @param from Time on the timeline where the captions start, for example the playhead. The speech before it is
     *   not used.
     */
    suspend operator fun invoke(clips: List<Clip>, from: Double = 0.0): List<CaptionSegment> =
        captionRepository.generateSpeechCaptions(clips, from)
}
