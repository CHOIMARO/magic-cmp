package com.company.core.domain.usecase.editor

import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.repository.CaptionRepository

/** Makes captions for the whole project. */
class GenerateCaptionsUseCase(
    private val captionRepository: CaptionRepository,
) {
    /**
     * @param totalDuration Length of the project in seconds.
     */
    suspend operator fun invoke(totalDuration: Double): List<CaptionSegment> =
        captionRepository.generateCaptions(totalDuration)
}
