package com.company.data.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.newEditorId
import com.company.domain.repository.CaptionRepository
import kotlin.math.floor

/** Speech length for one sample line, in seconds. A shorter part gets fewer lines. */
private const val SECONDS_PER_LINE = 0.8

/**
 * Makes sample captions.
 *
 * The lines spread evenly over the part. A short part gets fewer lines, as in the prototype. Replace this class when
 * real speech recognition is ready.
 */
class FakeCaptionRepository : CaptionRepository {
    override suspend fun generateCaptions(totalDuration: Double): List<CaptionSegment> {
        val count = floor(totalDuration / SECONDS_PER_LINE).toInt().coerceIn(1, EditorSampleData.CaptionPhrases.size)
        val segment = totalDuration / count
        return EditorSampleData.CaptionPhrases.take(count).mapIndexed { index, text ->
            CaptionSegment(id = newEditorId(), text = text, start = index * segment, length = segment * 0.94)
        }
    }
}
