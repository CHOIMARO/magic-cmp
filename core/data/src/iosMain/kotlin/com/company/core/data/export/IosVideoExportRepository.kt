package com.company.core.data.export

import com.company.core.domain.model.editor.ExportRequest
import com.company.core.domain.model.editor.ExportStatus
import com.company.core.domain.model.editor.GalleryAccessDeniedException
import com.company.core.domain.repository.VideoExportRepository
import com.company.core.media.buildMediaComposition
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAssetExportPresetHighestQuality
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVAssetExportSessionStatusCompleted
import platform.AVFoundation.AVAudioTimePitchAlgorithmSpectral
import platform.AVFoundation.AVFileTypeMPEG4
import platform.AVFoundation.audioTimePitchAlgorithm
import platform.AVFoundation.audioMix
import platform.AVFoundation.videoComposition
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary

/**
 * Encodes the project with [AVAssetExportSession] and saves the file to the photo library.
 *
 * The session uses the same composition as the preview player. The app asks only for the
 * add-only access to the photo library.
 */
internal class IosVideoExportRepository : VideoExportRepository {

    @OptIn(ExperimentalForeignApi::class)
    override fun export(request: ExportRequest): Flow<ExportStatus> = channelFlow {
        if (!requestAddAccess()) throw GalleryAccessDeniedException()
        val composition = buildMediaComposition(
            clips = request.clips,
            filter = request.filter,
            frameWidth = request.width,
            frameHeight = request.height,
            videoArea = request.videoArea,
            overlayTracks = request.overlayTracks,
            transitions = request.transitions,
        ) ?: error("Cannot build the composition")
        val output = NSURL.fileURLWithPath(NSTemporaryDirectory() + exportFileName(request.name))
        val session = AVAssetExportSession(asset = composition.asset, presetName = AVAssetExportPresetHighestQuality).apply {
            outputURL = output
            outputFileType = AVFileTypeMPEG4
            videoComposition = composition.videoComposition
            composition.audioMix?.let { audioMix = it }
            // 배속을 바꿔도 소리의 높이는 그대로 둔다.
            audioTimePitchAlgorithm = AVAudioTimePitchAlgorithmSpectral
            shouldOptimizeForNetworkUse = true
        }
        val done = CompletableDeferred<Unit>()
        try {
            // 완료 콜백은 AVFoundation 스레드에서 오므로 CompletableDeferred로만 알린다.
            session.exportAsynchronouslyWithCompletionHandler { done.complete(Unit) }
            val progressJob = launch {
                while (isActive) {
                    send(ExportStatus.Progress((session.progress * ENCODE_PERCENT).roundToInt()))
                    delay(PROGRESS_INTERVAL_MILLIS)
                }
            }
            done.await()
            progressJob.cancel()
            check(session.status == AVAssetExportSessionStatusCompleted) {
                "Export failed: ${session.error?.localizedDescription}"
            }
            val identifier = saveToPhotoLibrary(output)
            send(ExportStatus.Progress(100))
            send(ExportStatus.Saved(identifier))
        } finally {
            if (!done.isCompleted) session.cancelExport()
            NSFileManager.defaultManager.removeItemAtURL(output, error = null)
        }
    }

    /** Asks for the add-only access. Returns true when the app can add videos. */
    private suspend fun requestAddAccess(): Boolean = suspendCancellableCoroutine { continuation ->
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
            continuation.resume(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited)
        }
    }

    /**
     * Adds the video file to the photo library.
     *
     * @return The local identifier of the new asset.
     */
    private suspend fun saveToPhotoLibrary(file: NSURL): String = suspendCancellableCoroutine { continuation ->
        var identifier: String? = null
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            changeBlock = {
                identifier = PHAssetChangeRequest.creationRequestForAssetFromVideoAtFileURL(file)
                    ?.placeholderForCreatedAsset
                    ?.localIdentifier
            },
            completionHandler = { success, error ->
                if (success) {
                    continuation.resume(identifier.orEmpty())
                } else {
                    continuation.resumeWithException(IllegalStateException("Cannot save the video: ${error?.localizedDescription}"))
                }
            },
        )
    }

    private companion object {
        /** Part of the progress for the encoder. The save to the photo library is the rest. */
        const val ENCODE_PERCENT = 97

        /** Interval of the progress updates. */
        const val PROGRESS_INTERVAL_MILLIS = 200L
    }
}
