package com.company.domain.usecase.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.speechRanges
import com.company.domain.repository.CaptionRepository

/**
 * Makes captions only for the video parts of the clips. Photo clips have no sound, so they get no captions.
 *
 * @param clips Clips in play order.
 * @return Caption segments with times on the project timeline.
 */
internal suspend fun CaptionRepository.generateSpeechCaptions(clips: List<Clip>): List<CaptionSegment> =
    clips.speechRanges().flatMap { range ->
        generateCaptions(range.length).map { it.copy(start = it.start + range.start) }
    }
