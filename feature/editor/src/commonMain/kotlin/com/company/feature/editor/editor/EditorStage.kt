package com.company.feature.editor.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.MAX_TEXT_SCALE
import com.company.core.domain.model.editor.MIN_TEXT_SCALE
import com.company.core.domain.model.editor.MediaType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.clipAt
import com.company.ui.editor.OverlayText
import com.company.ui.editor.TextFrame
import com.company.ui.editor.centeredAt
import com.company.ui.editor.formatTime
import com.company.ui.editor.look
import com.company.ui.editor.toColorFilter
import com.company.ui.media.MediaThumbnail
import com.company.ui.media.rememberExactVideoFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 9:16 preview of the project at the playhead.
 *
 * With [video], the first layout cell shows the video of the clip at the playhead. The other cells
 * show a thumbnail of the next clips. Without [video], all cells show placeholder stripes.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the stage. Give it a size.
 * @param video Video surface of the preview player, or null to show placeholders.
 */
@Composable
internal fun EditorStage(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier,
    video: (@Composable (Modifier) -> Unit)? = null,
) {
    val project = state.project
    val current = project.clipAt(state.time)
    val filter = remember(project.filter) { project.filter.toColorFilter() }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MagicColors.Surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onIntent(EditorIntent.TogglePlay) },
            ),
    ) {
        val stageWidth = constraints.maxWidth.toFloat()
        val stageHeight = constraints.maxHeight.toFloat()
        val screenDensity = LocalDensity.current

        // 레이아웃·글자·자막·워터마크는 기준 무대(StageCanvas) 위에 그린다. 저장할 때도 같은 코드로 그린다.
        if (current != null) {
            StageCanvas(modifier = Modifier.fillMaxSize()) {
                if (video != null) {
                    StageLayoutCells(project = project, clipIndex = current.index, modifier = Modifier.fillMaxSize()) { index, clip ->
                        if (index == 0) {
                            // iOS 영상 뷰는 LocalDensity로 화면 좌표를 계산하므로, 영상은 화면의 실제 배율로 그린다.
                            CompositionLocalProvider(LocalDensity provides screenDensity) {
                                video(Modifier.matchParentSize())
                            }
                        } else {
                            // 칸마다 영상을 합성하기 전까지, 다음 클립은 시작 화면으로 보여 준다.
                            MediaThumbnail(
                                uri = clip.sourceUri,
                                type = clip.type,
                                hue = clip.thumbnailHue,
                                modifier = Modifier.matchParentSize(),
                                time = clip.inPoint,
                                colorFilter = filter,
                            )
                        }
                    }
                } else {
                    StageLayoutCells(
                        project = project,
                        clipIndex = current.index,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = filter,
                        stripeOffset = { index -> (state.time * 26 + index * 40).dp },
                    )
                }
            }
            if (video == null) {
                Text(
                    text = "${current.clip.name} · ${formatTime(current.localTime)}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = MagicColors.OnSurface60, modifier = Modifier.size(34.dp))
                Text(text = "아직 영상이 없어요", fontSize = 14.sp, color = MagicColors.OnSurface60)
            }
        }

        // 손잡이를 끄는 동안 자르는 위치의 프레임을 무대 전체에 보여 준다. 글자와 자막은 그 위에 그린다.
        state.trimEdgeFrame()?.let { (uri, time) ->
            TrimEdgeFrame(uri = uri, time = time, colorFilter = filter, modifier = Modifier.fillMaxSize())
        }

        // 선택한 글자가 무대에 보이면, 무대 어디에서든 두 손가락으로 그 글자의 크기와 각도를 바꾼다.
        val transformTarget by rememberUpdatedState(project.texts.find { it.id == state.selectedTextId && it.isVisibleAt(state.time) })
        StageCanvas(modifier = Modifier.fillMaxSize().pinchToTransformText(target = { transformTarget }, onIntent = onIntent)) {
            project.texts.filter { it.isVisibleAt(state.time) }.forEach { overlay ->
                key(overlay.id) {
                    DraggableOverlay(
                        overlay = overlay,
                        frame = if (state.playing) overlay.frameAt(state.time) else TextFrame.Static,
                        selected = overlay.id == state.selectedTextId && !state.playing,
                        stageWidth = stageWidth,
                        stageHeight = stageHeight,
                        onIntent = onIntent,
                    )
                }
            }
            // 등장·퇴장 애니메이션은 재생할 때만 보여 준다. 멈춘 화면에서는 글자를 고치기 쉽게 그대로 보여 준다.
            StageCaption(project = project, time = state.time, animate = state.playing)
            if (state.watermark) StageWatermark()
        }

        if (!state.playing && current != null && state.sheet == null && state.activeDrag == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 48.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "재생", modifier = Modifier.size(32.dp))
            }
        }
    }
}

/** Time before the end of a clip that shows its last frame. It is one frame at 30 fps. */
private const val LAST_FRAME_OFFSET = 1.0 / 30

/**
 * Finds the frame at the trim edge that the user drags.
 *
 * @return The source file and the source time of the frame, or null when no trim drag runs or the clip is not a video file.
 */
private fun EditorState.trimEdgeFrame(): Pair<String, Double>? {
    val trim = activeDrag as? TimelineDragTarget.ClipEdge ?: return null
    val clip = project.clips.find { it.id == trim.clipId } ?: return null
    val uri = clip.sourceUri ?: return null
    if (clip.type != MediaType.VIDEO) return null
    val time = when (trim.edge) {
        TrimEdge.START -> clip.inPoint
        TrimEdge.END -> (clip.outPoint - LAST_FRAME_OFFSET).coerceAtLeast(clip.inPoint)
    }
    return uri to time
}

/** Exact video frame at the trim edge. Until the first frame loads, the stage below stays visible. */
@Composable
private fun TrimEdgeFrame(uri: String, time: Double, colorFilter: ColorFilter?, modifier: Modifier) {
    val image = rememberExactVideoFrame(uri, time) ?: return
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        colorFilter = colorFilter,
    )
}

/** Space between the selected text and its frame. */
private val SelectionFramePadding = 6.dp

/** Visible size of the handle at the corner of the selection frame. */
private val TransformHandleSize = 18.dp

/** Touch size of the handle. It is larger than the visible handle. */
private val TransformHandleTouchSize = 40.dp

/** An angle this near to 0, 90, 180, or 270 degrees snaps to it, so a text easily goes back to straight. */
private const val ROTATION_SNAP_DEGREES = 5f

/**
 * Text overlay that the user can drag. A tap selects it, and a double tap opens the text sheet.
 * The selected text shows a frame with a handle that changes its size and its angle.
 *
 * @param overlay Text overlay to show.
 * @param frame Animation state of the text.
 * @param selected True to show the selection frame and the handle.
 * @param stageWidth Width of the stage in pixels.
 * @param stageHeight Height of the stage in pixels.
 * @param onIntent Called with each user action.
 */
@Composable
private fun DraggableOverlay(
    overlay: TextOverlay,
    frame: TextFrame,
    selected: Boolean,
    stageWidth: Float,
    stageHeight: Float,
    onIntent: (EditorIntent) -> Unit,
) {
    val latest by rememberUpdatedState(overlay)
    var textSize by remember { mutableStateOf(IntSize.Zero) }
    val scale = overlay.scale.toFloat()
    val rotation = overlay.rotation.toFloat()
    Box(modifier = Modifier.centeredAt(xPercent = overlay.x.toFloat(), yPercent = overlay.y.toFloat())) {
        OverlayText(
            text = overlay.text,
            look = overlay.style.look(font = overlay.font),
            frame = frame,
            modifier = Modifier
                .onSizeChanged { textSize = it }
                // 글자 상자를 통째로 키우고 돌린다. 손가락이 닿는 영역도 함께 바뀌도록 손가락 처리보다 먼저 둔다.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .pointerInput(overlay.id) {
                    var x = 0.0
                    var y = 0.0
                    detectDragGestures(
                        onDragStart = {
                            x = latest.x
                            y = latest.y
                            onIntent(EditorIntent.StartTextDrag)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            val moved = amount.toStageDistance(latest)
                            x += moved.x / stageWidth * 100
                            y += moved.y / stageHeight * 100
                            onIntent(EditorIntent.MoveText(overlay.id, x.coerceIn(12.0, 88.0), y.coerceIn(6.0, 94.0)))
                        },
                    )
                }
                .pointerInput(overlay.id) {
                    detectTapGestures(
                        onTap = { onIntent(EditorIntent.SelectText(overlay.id)) },
                        onDoubleTap = { onIntent(EditorIntent.OpenTextEditor(overlay.id)) },
                    )
                },
        )
        if (selected) {
            TextTransformFrame(
                textSize = textSize,
                scale = scale,
                rotation = rotation,
                onTransformStart = { onIntent(EditorIntent.StartTextTransform) },
                onTransform = { newScale, newRotation ->
                    onIntent(EditorIntent.TransformText(overlay.id, newScale.toDouble(), newRotation.toDouble()))
                },
            )
        }
    }
}

/**
 * Converts a distance inside the text box to a distance on the stage. The text box has the size factor and the
 * angle of [overlay], so a finger movement comes in smaller and turned.
 */
private fun Offset.toStageDistance(overlay: TextOverlay): Offset {
    val radians = overlay.rotation * PI / 180
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    val scale = overlay.scale.toFloat()
    return Offset((x * cos - y * sin) * scale, (x * sin + y * cos) * scale)
}

/** Turns the point clockwise by [degrees] around the origin. */
private fun Offset.rotatedBy(degrees: Float): Offset {
    val radians = degrees * PI / 180
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    return Offset(x * cos - y * sin, x * sin + y * cos)
}

/** Angle of the point from the origin, in degrees. A larger angle is more clockwise on the screen. */
private fun Offset.angleDegrees(): Float = (atan2(y, x) * 180 / PI).toFloat()

/** Snaps an angle near 0, 90, 180, or 270 degrees to that angle. */
private fun snapRotation(degrees: Float): Float {
    val nearest = (degrees / 90f).roundToInt() * 90f
    return if (abs(degrees - nearest) <= ROTATION_SNAP_DEGREES) nearest else degrees
}

/**
 * Frame around the selected text, with a handle at the bottom right corner.
 *
 * A drag of the handle away from the center of the text makes the text larger, and toward the center makes it
 * smaller. A drag around the center turns the text. The frame and the handle keep their size at all text sizes.
 *
 * @param textSize Size of the text box before the size factor.
 * @param scale Size factor of the text.
 * @param rotation Angle of the text in degrees.
 * @param onTransformStart Called when a drag of the handle starts.
 * @param onTransform Called with the new size factor and the new angle while the handle moves.
 */
@Composable
private fun BoxScope.TextTransformFrame(
    textSize: IntSize,
    scale: Float,
    rotation: Float,
    onTransformStart: () -> Unit,
    onTransform: (scale: Float, rotation: Float) -> Unit,
) {
    val latestScale by rememberUpdatedState(scale)
    val latestRotation by rememberUpdatedState(rotation)
    val framePadding = with(LocalDensity.current) { SelectionFramePadding.toPx() }
    val center = Offset(textSize.width / 2f, textSize.height / 2f)
    // 글자 가운데에서 본 프레임 오른쪽 아래 모서리. 키운 상자 바깥에 여백을 더하고, 글자 각도만큼 돌린다.
    val corner = Offset(textSize.width * scale / 2f + framePadding, textSize.height * scale / 2f + framePadding)
    val handleCenter = center + corner.rotatedBy(rotation)
    val frame = remember { LayoutHolder() }
    val handle = remember { LayoutHolder() }
    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { frame.coordinates = it }
            .drawBehind {
                rotate(degrees = rotation, pivot = center) {
                    drawRoundRect(
                        color = MagicColors.Accent,
                        topLeft = center - corner,
                        size = Size(corner.x * 2, corner.y * 2),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            },
    )
    Box(
        modifier = Modifier
            .offset {
                val half = TransformHandleTouchSize.toPx() / 2f
                IntOffset((handleCenter.x - half).roundToInt(), (handleCenter.y - half).roundToInt())
            }
            .size(TransformHandleTouchSize)
            .onGloballyPositioned { handle.coordinates = it }
            .pointerInput(Unit) {
                // 손잡이는 끄는 동안 함께 움직이므로, 화면 기준 위치로 글자 가운데에서 손가락까지의 방향과 거리를 잰다.
                fun fromCenter(position: Offset): Offset {
                    val handleCoordinates = handle.coordinates ?: return Offset.Zero
                    val frameCoordinates = frame.coordinates ?: return Offset.Zero
                    return handleCoordinates.localToRoot(position) - frameCoordinates.localToRoot(center)
                }
                var startScale = 1f
                var startRotation = 0f
                var start = Offset.Zero
                detectDragGestures(
                    onDragStart = { position ->
                        startScale = latestScale
                        startRotation = latestRotation
                        start = fromCenter(position)
                        onTransformStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val now = fromCenter(change.position)
                        if (start.getDistance() > 0f && now.getDistance() > 0f) {
                            val newScale = startScale * now.getDistance() / start.getDistance()
                            val newRotation = startRotation + now.angleDegrees() - start.angleDegrees()
                            onTransform(
                                newScale.coerceIn(MIN_TEXT_SCALE.toFloat(), MAX_TEXT_SCALE.toFloat()),
                                snapRotation(newRotation),
                            )
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TransformHandleSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, MagicColors.Accent, CircleShape),
        )
    }
}

/**
 * Changes the size and the angle of [target] when two fingers pinch or twist anywhere on the stage.
 *
 * The gesture takes the touches before the texts and the stage get them. So during the gesture, no text moves and
 * the preview does not start to play. With one finger, the touches go on to the texts and the stage.
 *
 * @param target Returns the selected text on the stage, or null when no selected text is on the stage.
 * @param onIntent Called with each user action.
 */
private fun Modifier.pinchToTransformText(target: () -> TextOverlay?, onIntent: (EditorIntent) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var pinchScale: Float? = null
            // 붙기 전의 각도를 따로 모은다. 붙은 각도에서 계속 더하면 붙은 자리를 벗어나지 못한다.
            var freeRotation = 0f
            var changed = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val text = target()
                if (text != null && (pinchScale != null || event.changes.count { it.pressed } >= 2)) {
                    val startScale = pinchScale ?: text.scale.toFloat()
                    if (pinchScale == null) freeRotation = text.rotation.toFloat()
                    val zoom = event.calculateZoom()
                    val turn = event.calculateRotation()
                    val newScale = (startScale * zoom).coerceIn(MIN_TEXT_SCALE.toFloat(), MAX_TEXT_SCALE.toFloat())
                    freeRotation += turn
                    if (zoom != 1f || turn != 0f) {
                        // 처음 바뀔 때 되돌리기 단계를 남긴다. 한 번의 핀치가 한 단계다.
                        if (!changed) {
                            changed = true
                            onIntent(EditorIntent.StartTextTransform)
                        }
                        onIntent(EditorIntent.TransformText(text.id, newScale.toDouble(), snapRotation(freeRotation).toDouble()))
                    }
                    pinchScale = newScale
                    // 핀치가 시작되면 손가락을 모두 뗄 때까지 글자 옮기기와 재생 전환이 반응하지 않게 한다.
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
