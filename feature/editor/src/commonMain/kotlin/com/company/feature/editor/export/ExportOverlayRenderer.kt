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
import com.company.core.domain.model.editor.clipAt
import com.company.feature.editor.editor.StageCanvas
import com.company.feature.editor.editor.StageCaption
import com.company.feature.editor.editor.StageLayoutCells
import com.company.feature.editor.editor.StageWatermark
import com.company.feature.editor.editor.frameAt
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

/** Layer of the overlay images. */
private enum class OverlayLayer {
    /** Layout background and the other cells. It changes only at clip boundaries. */
    LAYOUT,

    /** Texts, captions, and the watermark. It changes often during the animations. */
    TEXT,
}

/**
 * One overlay image to draw.
 *
 * @property layer Layer of the image.
 * @property time Time on the timeline in seconds.
 */
private data class OverlayFrame(val layer: OverlayLayer, val time: Double)

/**
 * Draws the overlay images of an export off screen, with the same code as the editor stage.
 *
 * The images have the size of the output frame. The layout layer and the text layer are separate
 * tracks, so the often changing text images stay small. Nothing shows on the screen. The renderer
 * needs the screen to stay visible, because Compose draws only while the app shows frames.
 *
 * @param request What to draw, or null when nothing must be drawn.
 * @param onProgress Called with the part of the images that are ready, from 0 to 1.
 * @param onRendered Called with the image tracks. The layout track is first.
 */
@Composable
internal fun ExportOverlayRenderer(
    request: OverlayRequest?,
    onProgress: (Float) -> Unit,
    onRendered: (List<List<ExportOverlay>>) -> Unit,
) {
    if (request == null) return
    val loader = rememberMediaThumbnailLoader()
    val layer = rememberGraphicsLayer()
    val latestOnProgress by rememberUpdatedState(onProgress)
    val latestOnRendered by rememberUpdatedState(onRendered)
    var frame by remember(request) { mutableStateOf<OverlayFrame?>(null) }
    var thumbnails by remember(request) { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }

    Box(modifier = Modifier.size(0.dp).wrapContentSize(align = Alignment.TopStart, unbounded = true)) {
        val current = frame
        if (current != null) {
            Box(
                modifier = Modifier
                    .exactPixelSize(request.width, request.height)
                    // 레이어에 기록만 하고 화면에는 그리지 않는다.
                    .drawWithContent { layer.record { this@drawWithContent.drawContent() } },
            ) {
                when (current.layer) {
                    OverlayLayer.LAYOUT -> ExportLayoutFrame(project = request.project, time = current.time, thumbnails = thumbnails)
                    OverlayLayer.TEXT -> ExportTextFrame(project = request.project, time = current.time, watermark = request.watermark)
                }
            }
        }
    }

    LaunchedEffect(request) {
        val layoutSpans = request.project.layoutSpans()
        val textSpans = request.project.textSpans(request.watermark)
        if (layoutSpans.isNotEmpty()) thumbnails = loader.loadCellThumbnails(request.project.clips)
        val total = layoutSpans.size + textSpans.size
        var done = 0
        val tracks = coroutineScope {
            listOf(OverlayLayer.LAYOUT to layoutSpans, OverlayLayer.TEXT to textSpans).map { (overlayLayer, spans) ->
                spans.map { span ->
                    frame = OverlayFrame(overlayLayer, span.time)
                    // 첫 프레임에서 새 시간으로 다시 그리고, 다음 프레임이 시작될 때 기록이 끝나 있다.
                    withFrameNanos { }
                    withFrameNanos { }
                    val image = layer.toImageBitmap()
                    latestOnProgress(++done / total.toFloat())
                    async(Dispatchers.Default) { ExportOverlay(start = span.start, end = span.end, png = image.encodeToPng()) }
                }
            }.map { it.awaitAll() }
        }
        latestOnRendered(tracks)
    }
}

/**
 * Layout layer of the export at [time]: layout background, labels, and the other cells.
 *
 * The first layout cell is transparent, because the video shows there.
 */
@Composable
private fun ExportLayoutFrame(project: EditorProject, time: Double, thumbnails: Map<String, ImageBitmap>) {
    val filter = remember(project.filter) { project.filter.toColorFilter() }
    StageCanvas(modifier = Modifier.fillMaxSize()) {
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
}

/** Text layer of the export at [time]: texts, caption, and watermark with their animations. */
@Composable
private fun ExportTextFrame(project: EditorProject, time: Double, watermark: Boolean) {
    StageCanvas(modifier = Modifier.fillMaxSize()) {
        project.texts.filter { it.isVisibleAt(time) }.forEach { overlay ->
            OverlayText(
                text = overlay.text,
                look = overlay.style.look(),
                frame = overlay.frameAt(time),
                modifier = Modifier.centeredAt(xPercent = overlay.x.toFloat(), yPercent = overlay.y.toFloat()),
            )
        }
        StageCaption(project = project, time = time, animate = true)
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
