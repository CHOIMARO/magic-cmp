package com.company.core.domain.repository

import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.VideoSummary
import kotlinx.coroutines.flow.StateFlow

/** Gives the projects of the user: drafts and completed videos. */
interface MyVideoRepository {
    /** Projects that the user did not complete. */
    val drafts: StateFlow<List<VideoSummary>>

    /** Videos that the user saved. The newest video is first. */
    val completedVideos: StateFlow<List<VideoSummary>>

    /**
     * Adds a saved video to the top of [completedVideos].
     *
     * @param video Summary of the saved video.
     */
    fun addCompletedVideo(video: VideoSummary)

    /**
     * Loads a draft project.
     *
     * @param name Name of the draft.
     */
    suspend fun loadDraftProject(name: String): EditorProject
}
