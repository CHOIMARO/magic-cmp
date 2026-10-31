package com.company.domain.repository

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.VideoSummary
import kotlinx.coroutines.flow.StateFlow

/** Gives the projects of the user: drafts and completed videos. */
interface MyVideoRepository {
    /** Projects that the user did not complete. The newest draft is first. */
    val drafts: StateFlow<List<VideoSummary>>

    /** Videos that the user saved. The newest video is first. */
    val completedVideos: StateFlow<List<VideoSummary>>

    /**
     * Adds a new draft to the top of [drafts]. The draft gets a new ID and the current time.
     *
     * @param name Name of the project.
     * @param duration Length of the project in seconds.
     * @param thumbnailHue Hue of the placeholder thumbnail.
     * @return The new draft.
     */
    fun addDraft(name: String, duration: Double, thumbnailHue: Int): VideoSummary

    /**
     * Changes the length and the thumbnail of a draft. The draft keeps its place in [drafts].
     *
     * @param id ID of the draft. Nothing changes when no draft has this ID.
     * @param duration New length in seconds.
     * @param thumbnailHue New hue of the placeholder thumbnail, or null to keep the hue.
     */
    fun updateDraft(id: String, duration: Double, thumbnailHue: Int?)

    /**
     * Adds a saved video to the top of [completedVideos]. The video gets a new ID and the current time.
     *
     * @param name Name of the project.
     * @param duration Length of the video in seconds.
     * @param thumbnailHue Hue of the placeholder thumbnail.
     */
    fun addCompletedVideo(name: String, duration: Double, thumbnailHue: Int)

    /**
     * Loads a draft project.
     *
     * @param name Name of the draft.
     */
    suspend fun loadDraftProject(name: String): EditorProject
}
