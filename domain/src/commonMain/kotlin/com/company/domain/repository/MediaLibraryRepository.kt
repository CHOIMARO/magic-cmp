package com.company.domain.repository

import com.company.domain.model.editor.MusicTrack
import com.company.domain.model.editor.SoundEffect
import com.company.domain.model.editor.SourceMedia

/** Gives the photos, the videos, the music, and the sound effects that the user can add to a project. */
interface MediaLibraryRepository {
    /**
     * Reads the details of the photos and videos that the user picked in the system picker.
     *
     * The result does not include an item that the app cannot read.
     *
     * @param uris Locations of the picked items.
     * @return Items in the same order as [uris].
     */
    suspend fun loadSourceMedia(uris: List<String>): List<SourceMedia>

    /** Returns the background music tracks. */
    fun getMusicTracks(): List<MusicTrack>

    /** Returns the sound effects. */
    fun getSoundEffects(): List<SoundEffect>
}
