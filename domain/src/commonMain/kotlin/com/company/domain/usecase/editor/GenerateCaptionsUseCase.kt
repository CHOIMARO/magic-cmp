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
     */
    suspend operator fun invoke(clips: List<Clip>): List<CaptionSegment> =
        captionRepository.generateSpeechCaptions(clips)
}
