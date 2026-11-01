package com.company.domain.usecase.editor

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.nextWorkName
import com.company.domain.repository.EditorProjectRepository
import com.company.domain.repository.MyVideoRepository

/**
 * Starts a new project that the user made from picked media or from the camera (F15).
 *
 * The project has clips, so it is saved at once as a new work at the top of "최근 작업". The editor then saves each
 * change to the work (see [SaveWorkUseCase]). The work gets a name that no other work has (D53).
 */
class StartNewProjectUseCase(
    private val projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) {
    /**
     * @param project New project. It replaces the current project and clears the undo history. Its name is the base
     *   of the work name, for example "새 작업". When a work has that name, a number goes after it.
     * @throws Exception When the work cannot be saved. The current project then does not change.
     */
    suspend operator fun invoke(project: EditorProject) {
        val named = project.copy(name = nextWorkName(project.name, myVideoRepository.workNames()))
        val work = myVideoRepository.addWork(named)
        projectRepository.start(named.copy(workId = work.id))
    }
}
