package com.company.data.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.newEditorId
import com.company.domain.repository.CaptionRepository

/**
 * Makes sample captions.
 *
 * The lines spread evenly over the project. Replace this class when real speech recognition is ready.
 */
class FakeCaptionRepository : CaptionRepository {
    override suspend fun generateCaptions(totalDuration: Double): List<CaptionSegment> {
        val phrases = EditorSampleData.CaptionPhrases
        val segment = totalDuration / phrases.size
        return phrases.mapIndexed { index, text ->
            CaptionSegment(id = newEditorId(), text = text, start = index * segment, length = segment * 0.94)
        }
    }
}
