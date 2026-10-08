package com.company.core.data.editor

import com.company.core.domain.model.editor.MusicTrack
import com.company.core.domain.model.editor.SourceVideo
import com.company.core.domain.repository.MediaLibraryRepository

/** Gives the sample gallery videos and music tracks. */
class FakeMediaLibraryRepository : MediaLibraryRepository {
    override fun getSourceVideos(): List<SourceVideo> = EditorSampleData.SourceVideos

    override fun getMusicTracks(): List<MusicTrack> = EditorSampleData.MusicTracks
}
