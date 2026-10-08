package com.company.core.domain.repository

import com.company.core.domain.model.editor.EditorProject
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the project that the user edits now, with an undo history.
 *
 * The gallery, the magic screen, the editor, and the export screen share this project.
 */
interface EditorProjectRepository {
    /** Current project. */
    val project: StateFlow<EditorProject>

    /** True when [undo] can restore an earlier state. */
    val canUndo: StateFlow<Boolean>

    /**
     * Replaces the project and clears the undo history.
     *
     * @param project New project.
     */
    fun start(project: EditorProject)

    /**
     * Changes the project and keeps the old state in the undo history.
     *
     * @param transform Change to apply.
     */
    fun commit(transform: (EditorProject) -> EditorProject)

    /** Keeps the current state in the undo history. Call it before a drag that uses [update]. */
    fun saveCheckpoint()

    /**
     * Changes the project without a new undo step, for example during a drag.
     *
     * @param transform Change to apply.
     */
    fun update(transform: (EditorProject) -> EditorProject)

    /**
     * Restores the last state in the undo history.
     *
     * @return True if a state was restored.
     */
    fun undo(): Boolean
}
