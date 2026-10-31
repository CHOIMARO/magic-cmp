package com.company.feature.editor.editor

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.currentWordIndex
import com.company.domain.model.editor.placedCaptions
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.stage_caption_placeholder
import com.company.ui.editor.CaptionText
import com.company.ui.editor.LayoutCells
import com.company.ui.editor.TextFrame
import com.company.ui.editor.frameAt
import com.company.ui.editor.look
import com.company.ui.editor.spec
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource

/** Background of the stage outside the layout cells. */
internal val StageBackground = Color(0xFF111111)

/** Height of the reference stage. The editor stage has this height on most phones. */
internal val StageReferenceHeight = 470.dp

/** Width of the reference stage. The stage is 9:16. */
internal val StageReferenceWidth = StageReferenceHeight * 9f / 16f

/**
 * Draws [content] on the reference stage and scales it to fill the bounds.
 *
 * Inside, dp and sp values are for the reference stage, and the system font size has no effect.
 * So texts keep the same size relative to the video on every phone and in the saved video.
 *
 * @param modifier Modifier for the stage. Give it a 9:16 size.
 * @param content Content in reference stage units.
 */
@Composable
internal fun StageCanvas(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier) {
        val pixelsPerDp = constraints.maxWidth / StageReferenceWidth.value
        CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, fontScale = 1f)) {
            Box(modifier = Modifier.requiredSize(StageReferenceWidth, StageReferenceHeight), content = content)
        }
    }
}

/** Clip that a layout cell shows when the playhead is in the clip at [clipIndex]. The cells show the next clips in turn. */
internal fun EditorProject.cellClip(clipIndex: Int, cell: Int): Clip = clips[(clipIndex + cell) % clips.size]

/**
 * Draws the layout cells of the project for the clip at [clipIndex].
 *
 * @param project Project with the layout and the clips.
 * @param clipIndex Index of the clip at the playhead.
 * @param modifier Modifier for the cells. Fill the stage with it.
 * @param stripeOffset Stripe shift for each cell index.
 * @param cellContent Content of each cell with its clip, or null to draw placeholder stripes.
 */
@Composable
internal fun StageLayoutCells(
    project: EditorProject,
    clipIndex: Int,
    modifier: Modifier = Modifier,
    stripeOffset: (Int) -> Dp = { 0.dp },
    cellContent: (@Composable BoxScope.(index: Int, clip: Clip) -> Unit)? = null,
) {
    LayoutCells(
        spec = project.layout.spec,
        cellHue = { index -> project.cellClip(clipIndex, index).thumbnailHue },
        modifier = modifier,
        defaultBackground = StageBackground,
        stripeOffset = stripeOffset,
        labelFontSize = 13f,
        labelPadding = 8.dp to 3.dp,
        labelRadius = 6.dp,
        cellContent = cellContent?.let { content -> { index -> content(index, project.cellClip(clipIndex, index)) } },
    )
}

/** True when the text is on screen at [time]. */
internal fun TextOverlay.isVisibleAt(time: Double): Boolean = time >= start && time < start + length

/** Caption on screen at [time], or null. */
internal fun List<CaptionSegment>.visibleAt(time: Double): CaptionSegment? =
    find { time >= it.start && time < it.start + it.length }

/** Animation state of the text at [time]. It uses the animations that the user picked. */
internal fun TextOverlay.frameAt(time: Double): TextFrame = look().animation.frameAt(time - start, start + length - time)

/** Animation state of the caption at [time]. */
internal fun CaptionSegment.frameAt(time: Double, style: CaptionStyleType): TextFrame =
    style.look().animation.frameAt(time - start, start + length - time)

/**
 * Caption that the text sheet changes now. The stage shows it at all times while the sheet is open.
 *
 * @property id ID of the caption.
 * @property text Text in the text field.
 * @property decorations Decorations of the text.
 */
internal data class CaptionPreview(val id: String, val text: String, val decorations: List<DecorationRange>)

/**
 * Drag of the caption on the editor stage. A drag up or down moves all captions together. A tap selects the caption.
 *
 * @property moving True while the user drags a caption. The caption then has a dashed frame.
 * @property selectedId ID of the selected caption, or null. The selected caption has a dashed frame.
 * @property onTap Called with the caption ID after a tap without a drag.
 * @property onStart Called when the finger moved far enough to start the drag.
 * @property onMove Called with the height from the bottom of the screen that the finger gives, in percent.
 * @property onEnd Called when the drag ends.
 */
internal class CaptionMove(
    val moving: Boolean,
    val selectedId: String?,
    val onTap: (String) -> Unit,
    val onStart: () -> Unit,
    val onMove: (Double) -> Unit,
    val onEnd: () -> Unit,
)

/** Finger distance before a caption drag starts. */
private val CaptionMoveSlop = 6.dp

/**
 * Shows the caption at [time] with the highlight of the caption style and its decorations.
 *
 * The caption is at [EditorProject.captionPosition], or at the height of the caption style.
 *
 * @param project Project with the captions and the caption style.
 * @param time Playhead position in seconds.
 * @param animate True to show the entrance and the exit. False shows the caption without them.
 * @param preview Caption that the text sheet changes, or null. It shows in place of the caption at [time].
 * @param move Drag of the caption on the editor stage, or null when the caption does not take touches.
 */
@Composable
internal fun StageCaption(
    project: EditorProject,
    time: Double,
    animate: Boolean,
    preview: CaptionPreview? = null,
    move: CaptionMove? = null,
) {
    val captions = project.placedCaptions
    val placed = preview?.let { p -> captions.find { it.id == p.id } } ?: captions.visibleAt(time) ?: return
    val captionPlaceholder = stringResource(Res.string.stage_caption_placeholder)
    val segment = if (preview != null && preview.id == placed.id) {
        // 비운 입력은 '자막'으로 보여 준다.
        placed.copy(text = preview.text.ifBlank { captionPlaceholder }, decorations = if (preview.text.isBlank()) emptyList() else preview.decorations)
    } else {
        placed
    }
    val look = project.captionStyle.look(decorations = segment.decorations)
    val position = project.captionPosition?.toFloat() ?: look.bottomPercent
    // 끌기 중에 자막 위치가 바뀌어 다시 그려도 듣고 있던 손가락을 놓치지 않게, 최신 값을 참조한다.
    val latestMove by rememberUpdatedState(move)
    val latestPosition by rememberUpdatedState(position)
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val sideMargin = maxWidth * 0.06f
        val stageHeight = constraints.maxHeight.toFloat()
        val outlined = move != null && (move.moving || move.selectedId == segment.id)
        CaptionText(
            text = segment.text,
            decorations = segment.decorations,
            // 고치는 동안에는 모든 단어를 보여 준다.
            currentWord = if (preview != null) Int.MAX_VALUE else segment.currentWordIndex(time),
            look = look,
            frame = if (animate && preview == null) segment.frameAt(time, project.captionStyle) else TextFrame.Static,
            modifier = Modifier
                .padding(start = sideMargin, end = sideMargin, bottom = maxHeight * (position / 100f))
                .drawWithContent {
                    drawContent()
                    if (outlined) {
                        val inset = 4.dp.toPx()
                        drawRoundRect(
                            color = MagicColors.Accent,
                            topLeft = Offset(-inset, -inset),
                            size = Size(size.width + inset * 2, size.height + inset * 2),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                            ),
                        )
                    }
                }
                .then(
                    if (move == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(segment.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                // 누른 자막은 무대 누르기(재생·일시정지)로 넘기지 않는다.
                                down.consume()
                                val start = latestPosition.toDouble()
                                var dy = 0f
                                var moving = false
                                var released = false
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    change.consume()
                                    if (!change.pressed) {
                                        released = true
                                        break
                                    }
                                    dy += change.positionChange().y
                                    if (!moving && abs(dy) > CaptionMoveSlop.toPx()) {
                                        moving = true
                                        latestMove?.onStart?.invoke()
                                    }
                                    if (moving) latestMove?.onMove?.invoke(start - dy / stageHeight * 100.0)
                                }
                                if (moving) latestMove?.onEnd?.invoke() else if (released) latestMove?.onTap?.invoke(segment.id)
                            }
                        }
                    },
                ),
        )
    }
}

/** Shows the "magic" watermark at the bottom right of the stage. */
@Composable
internal fun BoxScope.StageWatermark() {
    val density = LocalDensity.current
    Text(
        text = "magic",
        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 8.dp),
        style = TextStyle(
            fontFamily = MagicTheme.fonts.serif,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.75f),
            shadow = with(density) { Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 0.75.dp.toPx()), 3.dp.toPx()) },
        ),
    )
}
