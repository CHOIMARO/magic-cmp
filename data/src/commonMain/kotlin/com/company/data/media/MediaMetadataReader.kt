package com.company.data.media

import com.company.domain.model.editor.MediaType

/**
 * Details of a photo or video file.
 *
 * @property name File name without the extension.
 * @property type Photo or video.
 * @property duration Length of the video in seconds. Null for a photo.
 */
internal data class MediaMetadata(val name: String, val type: MediaType, val duration: Double?)

/** Reads the details of a photo or video file with the platform media API. */
internal interface MediaMetadataReader {
    /**
     * Reads the details of a photo or video file.
     *
     * @param uri Location of the file.
     * @return Details of the file, or null if the app cannot read it.
     */
    suspend fun read(uri: String): MediaMetadata?
}
