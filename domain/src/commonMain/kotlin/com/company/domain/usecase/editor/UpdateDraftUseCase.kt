package com.company.domain.usecase.editor

import com.company.domain.repository.EditorProjectRepository
import com.company.domain.repository.MyVideoRepository

/**
 * Copies the length and the first clip of the current project to its draft. Call it when the user leaves the editor.
 *
 * A project without a draft, for example a sample draft that the user opened, does not change the list.
 */
class UpdateDraftUseCase(
    private val projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) {
    operator fun invoke() {
        val project = projectRepository.project.value
        val draftId = project.draftId ?: return
        myVideoRepository.updateDraft(draftId, project.totalDuration, project.clips.firstOrNull()?.thumbnailHue)
    }
}
