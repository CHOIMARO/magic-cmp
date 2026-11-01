package com.company.domain.usecase.editor

import com.company.domain.repository.EditorProjectRepository
import com.company.domain.repository.MyVideoRepository

// =====================================================================
// Auto-save and work management (F15)
// =====================================================================

/**
 * Saves the project of the editor to its work (F15). A project without items is not saved, because an empty work
 * does not stay in the list.
 */
class SaveWorkUseCase(
    private val projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) {
    /**
     * @return True when a change was saved. False when nothing changed, the project is empty, or it is not a work.
     * @throws Exception When the storage fails. The editor keeps the changes and tries again at the next change.
     */
    suspend operator fun invoke(): Boolean {
        val project = projectRepository.project.value
        val workId = project.workId ?: return false
        if (project.isEmpty) return false
        return myVideoRepository.saveWork(workId, project)
    }
}

/** What happened to the work when the user left the editor. */
enum class LeaveResult {
    /** KEPT : The work stays in the list with its last changes, or the project is not a work. */
    KEPT,

    /** REMOVED_EMPTY : The project had no items, so the work went away (D45). */
    REMOVED_EMPTY,
}

/**
 * Leaves the editor without a question (F15). The work keeps the last changes. A work without items goes away, also
 * a work that the user opened again from the list (D45).
 */
class LeaveEditorUseCase(
    private val projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) {
    /**
     * @return What happened to the work.
     * @throws Exception When the storage fails.
     */
    suspend operator fun invoke(): LeaveResult {
        val project = projectRepository.project.value
        val workId = project.workId ?: return LeaveResult.KEPT
        if (project.isEmpty) {
            myVideoRepository.deleteWork(workId)
            return LeaveResult.REMOVED_EMPTY
        }
        myVideoRepository.saveWork(workId, project)
        return LeaveResult.KEPT
    }
}

/** Opens a work from "최근 작업" or "내 영상" in the editor, in its last saved state (F15). */
class OpenWorkUseCase(
    private val projectRepository: EditorProjectRepository,
    private val myVideoRepository: MyVideoRepository,
) {
    /**
     * @param workId ID of the work.
     * @return False when the work does not exist. The current project then does not change.
     */
    suspend operator fun invoke(workId: String): Boolean {
        val project = myVideoRepository.loadWork(workId) ?: return false
        // 다시 열면 되돌리기 이력은 비어 있다.
        projectRepository.start(project)
        return true
    }
}
