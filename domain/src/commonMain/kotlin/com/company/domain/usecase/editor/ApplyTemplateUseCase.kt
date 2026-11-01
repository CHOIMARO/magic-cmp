package com.company.domain.usecase.editor

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.applyTemplate

/** Applies a template from the template tab to a new project. */
class ApplyTemplateUseCase {
    /**
     * @param project Project to change.
     * @param template Template to apply. Null returns [project] without a change.
     * @param defaultText Text of the title that a thumbnail template adds.
     */
    operator fun invoke(project: EditorProject, template: EditorTemplate?, defaultText: String): EditorProject {
        if (template == null) return project
        return project.applyTemplate(template, defaultText)
    }
}
