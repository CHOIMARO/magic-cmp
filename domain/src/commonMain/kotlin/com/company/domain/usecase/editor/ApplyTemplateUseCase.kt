package com.company.domain.usecase.editor

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.applyTemplate
import com.company.domain.repository.CaptionRepository

/**
 * Applies a template to a project.
 *
 * A caption template needs captions. If the project has none, this use case makes them first from the video clips.
 */
class ApplyTemplateUseCase(
    private val captionRepository: CaptionRepository,
) {
    /**
     * @param project Project to change.
     * @param template Template to apply. Null returns [project] without a change.
     * @param defaultText Text of the new overlay when a text template finds no text.
     */
    suspend operator fun invoke(
        project: EditorProject,
        template: EditorTemplate?,
        defaultText: String,
    ): EditorProject {
        if (template == null) return project
        val needsCaptions = template is EditorTemplate.Caption && project.captions.isEmpty()
        val captions = if (needsCaptions) captionRepository.generateSpeechCaptions(project.clips) else emptyList()
        return project.applyTemplate(template, defaultText, captions)
    }
}
