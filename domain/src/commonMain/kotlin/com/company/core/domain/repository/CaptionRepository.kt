package com.company.core.domain.repository

import com.company.core.domain.model.editor.CaptionSegment

/** Makes captions from the speech in a project. */
interface CaptionRepository {
    /**
     * Makes caption segments for a project.
     *
     * @param totalDuration Length of the project in seconds.
     */
    suspend fun generateCaptions(totalDuration: Double): List<CaptionSegment>
}
