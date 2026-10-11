package com.company.feature.editor.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.StripeBox
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.ExportOverlay
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.clipAt
import com.company.feature.editor.editor.StageCanvas
import com.company.feature.editor.editor.StageCaption
import com.company.feature.editor.editor.StageLayoutCells
import com.company.feature.editor.editor.StageWatermark
import com.company.feature.editor.editor.isVisibleAt
import com.company.ui.editor.OverlayText
import com.company.ui.editor.centeredAt
import com.company.ui.editor.look
import com.company.ui.editor.toColorFilter
import com.company.ui.media.MediaThumbnailLoader
import com.company.ui.media.rememberMediaThumbnailLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Encodes the image to PNG data.
 *
 * @return The PNG data.
 */
internal expect fun ImageBitmap.encodeToPng(): ByteArray

/**
 * Draws the overlay images of an export off screen, with the same code as the editor stage.
 *
 * The images have the size of the output frame. Nothing shows on the screen. The renderer needs
 * the screen to stay visible, because Compose draws only while the app shows frames.
 *
 * @param request What to draw, or null when nothing must be drawn.
 * @param onProgress Called with the part of the images that are ready, from 0 to 1.
 * @param onRendered Called with the images in time order.
 */
@Composable
internal fun ExportOverlayRenderer(
    request: OverlayRequest?,
    onProgress: (Float) -> Unit,
    onRendered: (List<ExportOverlay>) -> Unit,
) {
    if (request == null) return
    val loader = rememberMediaThumbnailLoader()
    val layer = rememberGraphicsLayer()
    val latestOnProgress by rememberUpdatedState(onProgress)
    val latestOnRendered by rememberUpdatedState(onRendered)
    var frameTime by remember(request) { mutableStateOf<Double?>(null) }
    var thumbnails by remember(request) { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    Box(modifier = Modifier.size(0.dp).wrapContentSize(align = Alignment.TopStart, unbounded = true)) {
        val time = frameTime
        if (time != null) {
            Box(
                modifier = Modifier
                    .exactPixelSize(request.width, request.height)
                    // 레이어에 기록만 하고 화면에는 그리지 않는다.
                    .drawWithContent { layer.record { this@drawWithContent.drawContent() } },
            ) {
                ExportOverlayFrame(
                    project = request.project,
                    time = time,
                    watermark = request.watermark,
                    thumbnails = thumbnails,
                )
            }
        }
    }

    LaunchedEffect(request) {
        if (request.project.layout != LayoutType.FULL) thumbnails = loader.loadCellThumbnails(request.project.clips)
        val spans = request.project.overlaySpans(request.watermark)
        val overlays = coroutineScope {
            spans.mapIndexed { index, span ->
                frameTime = span.time
                // 첫 프레임에서 새 시간으로 다시 그리고, 다음 프레임이 시작될 때 기록이 끝나 있다.
                withFrameNanos { }
                withFrameNanos { }
                val image = layer.toImageBitmap()
                latestOnProgress((index + 1f) / spans.size)
                async(Dispatchers.Default) { ExportOverlay(start = span.start, end = span.end, png = image.encodeToPng()) }
            }.awaitAll()
        }
        latestOnRendered(overlays)
    }
}

/**
 * Overlay of the export at [time]: layout background, other cells, texts, caption, and watermark.
 *
 * The first layout cell is transparent, because the video shows there.
 */
@Composable
private fun ExportOverlayFrame(
    project: EditorProject,
    time: Double,
    watermark: Boolean,
    thumbnails: Map<String, ImageBitmap>,
) {
    val filter = remember(project.filter) { project.filter.toColorFilter() }
    StageCanvas(modifier = Modifier.fillMaxSize()) {
        if (project.layout != LayoutType.FULL) {
            StageLayoutCells(
                project = project,
                clipIndex = project.clipAt(time)?.index ?: 0,
                modifier = Modifier.fillMaxSize(),
            ) { index, clip ->
                if (index == 0) {
                    Box(modifier = Modifier.matchParentSize().drawBehind { drawRect(Color.Black, blendMode = BlendMode.Clear) })
                } else {
                    CellThumbnail(clip = clip, image = thumbnails[clip.id], filter = filter, modifier = Modifier.matchParentSize())
                }
            }
        }
        project.texts.filter { it.isVisibleAt(time) }.forEach { overlay ->
            OverlayText(
                text = overlay.text,
                look = overlay.style.look(),
                modifier = Modifier.centeredAt(xPercent = overlay.x.toFloat(), yPercent = overlay.y.toFloat()),
            )
        }
        StageCaption(project = project, time = time)
        if (watermark) StageWatermark()
    }
}

/** Thumbnail of a clip in a layout cell. It looks like the cell of the editor stage. */
@Composable
private fun CellThumbnail(clip: Clip, image: ImageBitmap?, filter: ColorFilter?, modifier: Modifier) {
    StripeBox(hue = clip.thumbnailHue.toFloat(), modifier = modifier, colorFilter = filter) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                colorFilter = filter,
            )
        }
    }
}

/** Loads the start frame of each clip, as the editor stage shows it in the other cells. */
private suspend fun MediaThumbnailLoader.loadCellThumbnails(clips: List<Clip>): Map<String, ImageBitmap> =
    clips.mapNotNull { clip ->
        val uri = clip.sourceUri ?: return@mapNotNull null
        load(uri, clip.type, clip.inPoint)?.let { clip.id to it }
    }.toMap()

/** Gives the content an exact pixel size, whatever the screen density is. */
private fun Modifier.exactPixelSize(width: Int, height: Int): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { placeable.place(0, 0) }
}
