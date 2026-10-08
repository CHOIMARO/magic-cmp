package com.company.core.domain.usecase.editor

import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.newEditorId
import com.company.core.domain.repository.CaptionRepository

/**
 * Decorates new clips automatically ("magic").
 *
 * The use case shortens long clips, picks a vibe, adds a title and captions,
 * and applies the template that the user picked.
 */
class CreateMagicProjectUseCase(
    private val captionRepository: CaptionRepository,
    private val applyTemplate: ApplyTemplateUseCase,
) {
    /**
     * @param name Name of the new project.
     * @param clips Clips in play order.
     * @param template Template that the user picked, or null.
     * @param titleText Text of the title overlay.
     */
    suspend operator fun invoke(
        name: String,
        clips: List<Clip>,
        template: EditorTemplate?,
        titleText: String,
    ): EditorProject {
        val trimmed = clips.map { clip ->
            if (clip.sourceDuration > LONG_CLIP_SECONDS) {
                clip.copy(inPoint = MAGIC_IN_POINT, outPoint = minOf(clip.sourceDuration, MAGIC_OUT_POINT))
            } else {
                clip
            }
        }
        val vibe = MagicVibes[clips.size % MagicVibes.size]
        val total = trimmed.sumOf { it.duration }
        val project = EditorProject(
            name = name,
            clips = trimmed,
            layout = LayoutType.FULL,
            captionStyle = vibe.captionStyle ?: CaptionStyleType.POP,
            texts = listOf(
                TextOverlay(
                    id = newEditorId(),
                    text = titleText,
                    start = 0.2,
                    length = minOf(2.8, total),
                    style = vibe.textStyle ?: TextStyleType.BOLD,
                    x = 50.0,
                    y = 24.0,
                )
            ),
            musicId = vibe.musicId,
            filter = vibe.filter,
            vibe = vibe,
            captions = captionRepository.generateCaptions(total),
        )
        return applyTemplate(project, template, titleText)
    }

    private companion object {
        /** Clips longer than this get a shorter range. */
        const val LONG_CLIP_SECONDS = 4.0

        /** Start of the shorter range. */
        const val MAGIC_IN_POINT = 0.4

        /** End of the shorter range. */
        const val MAGIC_OUT_POINT = 3.8

        /** Vibes that the magic picks from. The number of clips selects one. */
        val MagicVibes = listOf(Vibe.WARM, Vibe.DREAMY, Vibe.HYPE, Vibe.CINEMATIC, Vibe.FRESH)
    }
}
