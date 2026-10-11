package com.company.feature.editor.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.TextOverlay
import com.company.ui.editor.CaptionText
import com.company.ui.editor.LayoutCells
import com.company.ui.editor.TextFrame
import com.company.ui.editor.frameAt
import com.company.ui.editor.splitWords
import com.company.ui.editor.spec
import com.company.ui.editor.look
import kotlin.math.ceil

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
 * @param colorFilter Filter for the placeholder stripes.
 * @param stripeOffset Stripe shift for each cell index.
 * @param cellContent Content of each cell with its clip, or null to draw placeholder stripes.
 */
@Composable
internal fun StageLayoutCells(
    project: EditorProject,
    clipIndex: Int,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    stripeOffset: (Int) -> Dp = { 0.dp },
    cellContent: (@Composable BoxScope.(index: Int, clip: Clip) -> Unit)? = null,
) {
    LayoutCells(
        spec = project.layout.spec,
        cellHue = { index -> project.cellClip(clipIndex, index).thumbnailHue },
        modifier = modifier,
        defaultBackground = StageBackground,
        colorFilter = colorFilter,
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

/** Number of words that the speaker already said at [time]. It is at least 1. The last of them is the current word. */
internal fun CaptionSegment.spokenWordCount(time: Double): Int {
    val words = text.splitWords().size
    return maxOf(1, ceil((time - start) / length * words).toInt())
}

/** Animation state of the text at [time]. */
internal fun TextOverlay.frameAt(time: Double): TextFrame = style.look().animation.frameAt(time - start, start + length - time)

/** Animation state of the caption at [time]. */
internal fun CaptionSegment.frameAt(time: Double, style: CaptionStyleType): TextFrame =
    style.look().animation.frameAt(time - start, start + length - time)

/**
 * Shows the caption at [time] with the highlight of the caption style.
 *
 * @param project Project with the captions and the caption style.
 * @param time Playhead position in seconds.
 * @param animate True to show the entrance and the exit. False shows the caption without them.
 */
@Composable
internal fun StageCaption(project: EditorProject, time: Double, animate: Boolean) {
    val segment = project.captions.visibleAt(time) ?: return
    val look = project.captionStyle.look()
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        val sideMargin = maxWidth * 0.06f
        CaptionText(
            words = segment.text.splitWords(),
            currentWord = segment.spokenWordCount(time) - 1,
            look = look,
            frame = if (animate) segment.frameAt(time, project.captionStyle) else TextFrame.Static,
            modifier = Modifier.padding(start = sideMargin, end = sideMargin, bottom = maxHeight * (look.bottomPercent / 100f)),
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
