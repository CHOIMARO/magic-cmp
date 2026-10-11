package com.company.core.domain.model.editor

/**
 * Area of the output frame that shows the video. Values are fractions of the frame size, from 0 to 1.
 *
 * @property left Left edge.
 * @property top Top edge.
 * @property width Width.
 * @property height Height.
 */
data class FrameArea(val left: Float, val top: Float, val width: Float, val height: Float) {
    /** True when the video fills the whole frame. */
    val isFullFrame: Boolean
        get() = this == FULL

    companion object {
        /** The whole frame. */
        val FULL = FrameArea(left = 0f, top = 0f, width = 1f, height = 1f)
    }
}

/**
 * Image on top of the video for a time range of the export.
 *
 * The image has the size of the output frame. It holds the texts, the captions, and the watermark,
 * or the layout background. The area of the video is transparent.
 *
 * @property start Start time on the timeline, in seconds.
 * @property end End time on the timeline, in seconds.
 * @property png PNG data of the image.
 */
class ExportOverlay(val start: Double, val end: Double, val png: ByteArray)

/**
 * Everything that the encoder needs to make the video file.
 *
 * @property name Name of the project. The file name uses it.
 * @property clips Clips in play order. All clips must have a source file.
 * @property filter Color filter for the video.
 * @property width Width of the output frame in pixels.
 * @property height Height of the output frame in pixels.
 * @property videoArea Area of the frame that shows the video.
 * @property overlayTracks Images on top of the video. Each track holds images in time order, and a time
 * without an image shows nothing of that track. The first track is at the bottom.
 */
data class ExportRequest(
    val name: String,
    val clips: List<Clip>,
    val filter: FilterType,
    val width: Int,
    val height: Int,
    val videoArea: FrameArea = FrameArea.FULL,
    val overlayTracks: List<List<ExportOverlay>> = emptyList(),
)

/** State of a running export. */
sealed interface ExportStatus {
    /** Progress(percent) : The export runs. [percent] goes from 0 to 100. */
    data class Progress(val percent: Int) : ExportStatus

    /** Saved(uri) : The video is in the gallery. [uri] identifies the saved video. */
    data class Saved(val uri: String) : ExportStatus
}

/** The user did not let the app add videos to the gallery. */
class GalleryAccessDeniedException : Exception("The app cannot add videos to the gallery")
