package com.company.core.domain.repository

import com.company.core.domain.model.editor.MusicTrack
import com.company.core.domain.model.editor.SourceVideo

/** Gives the videos and the music that the user can add to a project. */
interface MediaLibraryRepository {
    /** Returns the videos in the device gallery. */
    fun getSourceVideos(): List<SourceVideo>

    /** Returns the background music tracks. */
    fun getMusicTracks(): List<MusicTrack>
}
