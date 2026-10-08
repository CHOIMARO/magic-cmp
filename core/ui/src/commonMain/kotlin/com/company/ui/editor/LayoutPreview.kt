package com.company.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.StripeBox
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.LayoutType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * One video area of a layout. Positions are in percent of the stage.
 *
 * @property left Left edge.
 * @property top Top edge.
 * @property width Width.
 * @property height Height.
 * @property cornerRadius Corner radius in dp.
 * @property ring True to draw a white frame around the area.
 */
@Immutable
data class LayoutCell(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val cornerRadius: Float = 0f,
    val ring: Boolean = false,
)

/**
 * Text label on a layout, for example "전" and "후".
 *
 * @property text Label text.
 * @property left Left edge in percent.
 * @property top Top edge in percent.
 */
@Immutable
data class LayoutLabel(val text: String, val left: Float, val top: Float)

/**
 * Geometry of a [LayoutType].
 *
 * @property cells Video areas.
 * @property labels Text labels.
 * @property background Stage color behind the cells, or null for the default color.
 */
@Immutable
data class LayoutSpec(
    val cells: List<LayoutCell>,
    val labels: List<LayoutLabel> = emptyList(),
    val background: Color? = null,
)

/** Geometry of the layout. */
val LayoutType.spec: LayoutSpec
    get() = when (this) {
        LayoutType.FULL -> LayoutSpec(listOf(LayoutCell(0f, 0f, 100f, 100f)))
        LayoutType.SPLIT_VERTICAL -> LayoutSpec(listOf(LayoutCell(0f, 0f, 100f, 49.6f), LayoutCell(0f, 50.4f, 100f, 49.6f)))
        LayoutType.SPLIT_HORIZONTAL -> LayoutSpec(listOf(LayoutCell(0f, 0f, 49.6f, 100f), LayoutCell(50.4f, 0f, 49.6f, 100f)))
        LayoutType.PICTURE_IN_PICTURE -> LayoutSpec(
            listOf(LayoutCell(0f, 0f, 100f, 100f), LayoutCell(55f, 5f, 39f, 28f, cornerRadius = 10f, ring = true))
        )
        LayoutType.BEFORE_AFTER -> LayoutSpec(
            cells = listOf(LayoutCell(0f, 0f, 49.6f, 100f), LayoutCell(50.4f, 0f, 49.6f, 100f)),
            labels = listOf(LayoutLabel("전", 4f, 3f), LayoutLabel("후", 54.4f, 3f)),
        )
        LayoutType.GRID_THREE -> LayoutSpec(
            listOf(LayoutCell(0f, 0f, 100f, 33f), LayoutCell(0f, 33.5f, 100f, 33f), LayoutCell(0f, 67f, 100f, 33f))
        )
        LayoutType.POLAROID -> LayoutSpec(
            cells = listOf(LayoutCell(7f, 7f, 86f, 64f, cornerRadius = 4f)),
            background = Color(0xFFEFE9DF),
        )
        LayoutType.CINEMA -> LayoutSpec(listOf(LayoutCell(0f, 20f, 100f, 60f)), background = Color.Black)
    }

/**
 * Draws the cells and labels of a layout. Fill the parent with this composable.
 *
 * @param spec Layout geometry.
 * @param cellHue Hue of the placeholder stripes for each cell index.
 * @param modifier Modifier for the stage.
 * @param defaultBackground Stage color when [LayoutSpec.background] is null.
 * @param colorFilter Optional video filter for the cells.
 * @param stripeOffset Stripe shift for each cell index. Use it to animate playback.
 * @param labelFontSize Font size of the labels in sp.
 * @param labelPadding Horizontal and vertical padding of the labels.
 * @param labelRadius Corner radius of the labels.
 */
@Composable
fun LayoutCells(
    spec: LayoutSpec,
    cellHue: (Int) -> Int,
    modifier: Modifier = Modifier,
    defaultBackground: Color = Color(0xFF222222),
    colorFilter: ColorFilter? = null,
    stripeOffset: (Int) -> Dp = { 0.dp },
    labelFontSize: Float = 13f,
    labelPadding: Pair<Dp, Dp> = 4.dp to 1.dp,
    labelRadius: Dp = 3.dp,
) {
    BoxWithConstraints(modifier = modifier.background(spec.background ?: defaultBackground)) {
        val stageWidth = maxWidth
        val stageHeight = maxHeight
        spec.cells.forEachIndexed { index, cell ->
            val shape = RoundedCornerShape(cell.cornerRadius.dp)
            StripeBox(
                hue = cellHue(index).toFloat(),
                modifier = Modifier
                    .offset(x = stageWidth * (cell.left / 100f), y = stageHeight * (cell.top / 100f))
                    .size(width = stageWidth * (cell.width / 100f), height = stageHeight * (cell.height / 100f))
                    .then(if (cell.ring) Modifier.border(2.dp, Color.White, shape) else Modifier),
                shape = shape,
                colorFilter = colorFilter,
                stripeOffset = stripeOffset(index),
            )
        }
        spec.labels.forEach { label ->
            Text(
                text = label.text,
                modifier = Modifier
                    .offset(x = stageWidth * (label.left / 100f), y = stageHeight * (label.top / 100f))
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(labelRadius))
                    .padding(horizontal = labelPadding.first, vertical = labelPadding.second),
                fontSize = labelFontSize.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

/** Hues of the sample cells in a layout preview. */
private val LayoutPreviewHues = listOf(35, 200, 150)

/** Hue of the sample cell in a text preview. */
private const val TEXT_PREVIEW_HUE = 250

/** Hue of the sample cell in a caption preview. */
private const val CAPTION_PREVIEW_HUE = 160

/**
 * Small 9:16 preview of a template, as in the home and template tabs.
 *
 * @param template Template to preview.
 * @param scale Size factor for text and labels.
 * @param modifier Modifier for the preview. Give it a size or an aspect ratio.
 * @param showSample True to show the sample title or caption.
 * @param labelPadding Horizontal and vertical padding of the layout labels.
 * @param captionRadius Corner radius of the sample caption.
 */
@Composable
fun TemplatePreview(
    template: EditorTemplate,
    scale: Float,
    modifier: Modifier = Modifier,
    showSample: Boolean = true,
    labelPadding: Pair<Dp, Dp> = 4.dp to 1.dp,
    captionRadius: Float = 4f,
) {
    val layout = (template as? EditorTemplate.Layout)?.layout ?: LayoutType.FULL
    val hues = when (template) {
        is EditorTemplate.Layout -> LayoutPreviewHues
        is EditorTemplate.Text -> listOf(TEXT_PREVIEW_HUE)
        is EditorTemplate.Caption -> listOf(CAPTION_PREVIEW_HUE)
    }
    Box(modifier = modifier) {
        LayoutCells(
            spec = layout.spec,
            cellHue = { hues[it % hues.size] },
            modifier = Modifier.fillMaxSize(),
            labelFontSize = max(8f, 13f * scale),
            labelPadding = labelPadding,
        )
        if (!showSample) return@Box
        when (template) {
            is EditorTemplate.Text -> OverlayText(
                text = DEFAULT_TITLE_TEXT,
                look = template.style.look(scale),
                modifier = Modifier.centeredAt(xPercent = 50f, yPercent = 40f),
            )
            is EditorTemplate.Caption -> {
                val look = template.style.look(scale)
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    CaptionText(
                        spoken = "자막이",
                        upcoming = "이렇게 나와요",
                        look = look.copy(cornerRadius = captionRadius),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = maxHeight * (look.bottomPercent / 100f))
                            .wrapContentSize(unbounded = true),
                    )
                }
            }
            is EditorTemplate.Layout -> Unit
        }
    }
}

/**
 * Places the element so that its center is at a percent position of the parent.
 *
 * The parent must fill the stage. The element can be wider than the parent.
 *
 * @param xPercent Horizontal center in percent.
 * @param yPercent Vertical center in percent.
 */
fun Modifier.centeredAt(xPercent: Float, yPercent: Float): Modifier = this.layout { measurable, constraints ->
    // 디자인의 white-space: nowrap처럼 줄바꿈 없이 잰다.
    val placeable = measurable.measure(Constraints())
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    layout(width, height) {
        val x = (width * xPercent / 100f - placeable.width / 2f).roundToInt()
        val y = (height * yPercent / 100f - placeable.height / 2f).roundToInt()
        placeable.place(x, y)
    }
}
