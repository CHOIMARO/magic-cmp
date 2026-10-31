package com.company.domain.usecase.editor

import com.company.domain.model.editor.EditorProject
import com.company.domain.repository.EditorProjectRepository
import com.company.domain.repository.MyVideoRepository

/**
 * Starts a new project that the user made from picked media or from the camera.
 *
 * The project goes to the top of the drafts at once. The draft gets the length and the thumbnail of the project when
 * the user leaves the editor (see [UpdateDraftUseCase]).
 */
class StartNewProjectUseCase(
    private val projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) {
    /**
     * @param project New project. It replaces the current project and clears the undo history.
     */
    operator fun invoke(project: EditorProject) {
        val draft = myVideoRepository.addDraft(
            name = project.name,
            duration = project.totalDuration,
            thumbnailHue = project.clips.firstOrNull()?.thumbnailHue ?: DEFAULT_THUMBNAIL_HUE,
        )
        projectRepository.start(project.copy(draftId = draft.id))
    }
}

/** Hue of the placeholder thumbnail of a project without clips. */
const val DEFAULT_THUMBNAIL_HUE = 300
