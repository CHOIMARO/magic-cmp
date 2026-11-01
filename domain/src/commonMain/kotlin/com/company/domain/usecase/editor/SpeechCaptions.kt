package com.company.domain.usecase.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.MIN_CAPTION_LENGTH
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.boundTo
import com.company.domain.repository.CaptionRepository

/**
 * Makes captions only for the video parts of the clips. Photo clips have no sound, so they get no captions.
 *
 * Each video clip gets its own captions, and each caption is bound to its clip (D47). The caption then stays with the
 * speech when the clip is trimmed, moved, or played at another speed.
 *
 * @param clips Clips in play order.
 * @param from Time on the timeline where the captions start. The speech before it is not used.
 * @return Caption segments with times on the project timeline.
 */
internal suspend fun CaptionRepository.generateSpeechCaptions(clips: List<Clip>, from: Double = 0.0): List<CaptionSegment> {
    val captions = mutableListOf<CaptionSegment>()
    var clipStart = 0.0
    clips.forEach { clip ->
        val clipEnd = clipStart + clip.duration
        val start = maxOf(clipStart, from)
        if (clip.type == MediaType.VIDEO && clipEnd - start >= MIN_CAPTION_LENGTH) {
            captions += generateCaptions(clipEnd - start).map { caption ->
                caption.copy(start = caption.start + start).boundTo(clip, clipStart)
            }
        }
        clipStart = clipEnd
    }
    return captions
}
