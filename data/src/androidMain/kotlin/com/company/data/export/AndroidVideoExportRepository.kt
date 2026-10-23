package com.company.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.company.domain.model.editor.ExportRequest
import com.company.domain.model.editor.ExportStatus
import com.company.domain.repository.VideoExportRepository
import com.company.core.media.buildMediaComposition
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Encodes the project with the Media3 Transformer and saves the file to Movies/magic.
 *
 * The Transformer uses the same composition as the preview player. Android 10 and higher can add a
 * video to the shared Movies folder without a permission.
 *
 * @param context Context for the encoder and the media store.
 */
internal class AndroidVideoExportRepository(private val context: Context) : VideoExportRepository {

    // Transformer는 만든 스레드의 Looper에서만 쓸 수 있으므로 메인 스레드에서 실행한다.
    @OptIn(UnstableApi::class)
    override fun export(request: ExportRequest): Flow<ExportStatus> = channelFlow {
        val output = File(File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }, "${UUID.randomUUID()}.mp4")
        val done = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    done.complete(Unit)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exception: ExportException) {
                    done.completeExceptionally(exception)
                }
            })
            .build()
        try {
            val composition = buildMediaComposition(
                clips = request.clips,
                filter = request.filter,
                frameWidth = request.width,
                frameHeight = request.height,
                videoArea = request.videoArea,
                overlayTracks = request.overlayTracks,
                transitions = request.transitions,
            )
            transformer.start(composition, output.path)
            val progressJob = launch {
                val holder = ProgressHolder()
                while (isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        send(ExportStatus.Progress(holder.progress * ENCODE_PERCENT / 100))
                    }
                    delay(PROGRESS_INTERVAL_MILLIS)
                }
            }
            done.await()
            progressJob.cancel()
            val uri = withContext(Dispatchers.IO) { saveToGallery(output, request.name) }
            send(ExportStatus.Progress(100))
            send(ExportStatus.Saved(uri))
        } finally {
            if (!done.isCompleted) transformer.cancel()
            withContext(Dispatchers.IO + NonCancellable) { output.delete() }
        }
    }.flowOn(Dispatchers.Main)

    /**
     * Copies the file into Movies/magic of the shared storage.
     *
     * The item stays pending until the copy ends, so the gallery does not show a half-written video.
     *
     * @return The content URI of the saved video.
     */
    private fun saveToGallery(file: File, name: String): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, exportFileName(name))
            put(MediaStore.Video.Media.MIME_TYPE, MimeTypes.VIDEO_MP4)
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$GALLERY_FOLDER")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = checkNotNull(resolver.insert(collection, values)) { "Cannot add the video to the media store" }
        try {
            val stream = checkNotNull(resolver.openOutputStream(uri)) { "Cannot open the gallery item" }
            stream.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri.toString()
    }

    private companion object {
        /** Folder in the cache directory for the encoder output. */
        const val EXPORT_DIR = "export"

        /** Folder in Movies for the saved videos. */
        const val GALLERY_FOLDER = "magic"

        /** Part of the progress for the encoder. The copy to the gallery is the rest. */
        const val ENCODE_PERCENT = 97

        /** Interval of the progress updates. */
        const val PROGRESS_INTERVAL_MILLIS = 200L
    }
}
