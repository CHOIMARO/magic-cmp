package com.company.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.theme.MagicColors

/**
 * Draws a selection ring outside the element, like CSS `box-shadow: 0 0 0 2px bg, 0 0 0 4px accent`.
 *
 * The ring has a 2 dp gap and a 2 dp line. Do not clip the parent, or the ring is cut off.
 *
 * @param selected True to draw the ring.
 * @param cornerRadius Corner radius of the element.
 * @param color Color of the ring.
 */
fun Modifier.selectionRing(selected: Boolean, cornerRadius: Dp, color: Color = MagicColors.Accent): Modifier =
    if (!selected) {
        this
    } else {
        drawWithContent {
            drawContent()
            val gap = 2.dp.toPx()
            val line = 2.dp.toPx()
            val inset = gap + line / 2
            drawRoundRect(
                color = color,
                topLeft = Offset(-inset, -inset),
                size = Size(size.width + inset * 2, size.height + inset * 2),
                cornerRadius = CornerRadius(cornerRadius.toPx() + inset),
                style = Stroke(width = line),
            )
        }
    }

/**
 * Makes the element wider than its parent by [bleed] on each side.
 *
 * Use it for a horizontal scroll row that must reach the screen edges inside a padded sheet.
 *
 * @param bleed Extra width on each side. Use the horizontal padding of the parent.
 */
fun Modifier.bleedHorizontal(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = bleed.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        )
    )
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-bleed.roundToPx(), 0)
    }
}
