package com.company.domain.repository

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.VideoSummary
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps the works of the user (F15). A work is one edit that the user started in the editor: its project and its
 * summary in "최근 작업" and "내 영상".
 *
 * The functions that write can fail, for example when the device storage is full. They then throw an exception.
 */
interface MyVideoRepository {
    /** Works, the work with the most recent change first (D44). */
    val works: StateFlow<List<VideoSummary>>

    /**
     * Adds a new work at the top of [works]. The work gets a new ID and the current time.
     *
     * @param project Project of the work. The work keeps it with [EditorProject.workId] set to the new ID.
     * @return The new work.
     */
    suspend fun addWork(project: EditorProject): VideoSummary

    /**
     * Saves the project of a work. When the project is the same as the last saved project, nothing changes. Otherwise
     * the work gets the length, the thumbnail, and the current time, and it goes to the top of [works].
     *
     * @param id ID of the work. Nothing changes when no work has this ID.
     * @param project Project to save.
     * @return True when the project changed and the repository saved it.
     */
    suspend fun saveWork(id: String, project: EditorProject): Boolean

    /**
     * Loads the last saved project of a work.
     *
     * @param id ID of the work.
     * @return The project with [EditorProject.workId] set, or null when no work has this ID.
     */
    suspend fun loadWork(id: String): EditorProject?

    /**
     * Removes a work and its project. The source files and the exported videos in the gallery stay.
     *
     * @param id ID of the work.
     */
    suspend fun deleteWork(id: String)

    /**
     * Marks a work as exported. The time of the last change and the order of [works] do not change.
     *
     * @param id ID of the work.
     */
    suspend fun markExported(id: String)
}
