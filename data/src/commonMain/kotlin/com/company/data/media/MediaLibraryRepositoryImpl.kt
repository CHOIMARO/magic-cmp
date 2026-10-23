package com.company.data.media

import com.company.data.editor.EditorSampleData
import com.company.domain.model.editor.MusicTrack
import com.company.domain.model.editor.SoundEffect
import com.company.domain.model.editor.SourceMedia
import com.company.domain.repository.MediaLibraryRepository

/**
 * Reads the picked photos and videos from the device, and gives the sample music tracks and sound effects.
 *
 * @param metadataReader Reads the type, the length, and the name of a file.
 */
internal class MediaLibraryRepositoryImpl(
    private val metadataReader: MediaMetadataReader,
) : MediaLibraryRepository {

    override suspend fun loadSourceMedia(uris: List<String>): List<SourceMedia> = uris.mapNotNull { uri ->
        val metadata = metadataReader.read(uri) ?: return@mapNotNull null
        SourceMedia(
            uri = uri,
            type = metadata.type,
            name = metadata.name,
            duration = metadata.duration,
            thumbnailHue = placeholderHue(uri),
        )
    }

    override fun getMusicTracks(): List<MusicTrack> = EditorSampleData.MusicTracks

    override fun getSoundEffects(): List<SoundEffect> = EditorSampleData.SoundEffects

    // 같은 항목은 항상 같은 자리표시 색을 갖도록 URI에서 색상을 정한다.
    private fun placeholderHue(uri: String): Int = (uri.hashCode() and Int.MAX_VALUE) % 360
}
