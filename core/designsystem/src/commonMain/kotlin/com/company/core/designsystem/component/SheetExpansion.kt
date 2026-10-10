package com.company.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/**
 * Expansion state of an expandable sheet. The user drags it up to make it taller.
 *
 * The sheet opens at the base (collapsed) height. An upward drag adds up to [maxExpansionPx] to the content,
 * until the sheet reaches its maximum height. A downward drag first collapses the sheet and then closes it.
 * On release, the sheet settles to collapsed or expanded, by the fling direction or by the half point.
 *
 * [MagicModalBottomSheet] takes this state as `expansionState` and handles the gestures. The content puts
 * [expandableHeight] on the area that grows. Make a new state each time the sheet opens, so that it opens collapsed.
 */
@Stable
class SheetExpansionState internal constructor() {

    private val expansionState = mutableFloatStateOf(0f)

    /** Added height in pixels. 0 means the base (collapsed) height. */
    val expansionPx: Float
        get() = expansionState.floatValue

    /**
     * Largest added height in pixels. [expandableHeight] updates it at each measure.
     * It is 0 on a small screen where the base height already reaches the maximum sheet height.
     */
    var maxExpansionPx: Float by mutableFloatStateOf(0f)
        internal set

    /** Sets the added height in the range 0 to [maxExpansionPx]. */
    internal fun setExpansion(valuePx: Float) {
        expansionState.floatValue = valuePx.coerceIn(0f, maxExpansionPx.coerceAtLeast(0f))
    }

    /** True when the sheet stopped between collapsed and expanded, so it needs a settle animation. */
    internal val isSettling: Boolean
        get() = expansionPx > 0f && expansionPx < maxExpansionPx
}

/** Makes the expansion state of an expandable sheet. The sheet starts collapsed each time it opens. */
@Composable
fun rememberSheetExpansionState(): SheetExpansionState = remember { SheetExpansionState() }

/**
 * Sets the height of the area that grows in an expandable sheet.
 *
 * The height is [baseHeight] plus the added height of [state], and it stays inside the maximum height that the
 * parent allows. Use it with `weight(1f, fill = false)` in the parent `Column`. The height is read only in the
 * layout step, so a drag or a settle measures again without a recomposition.
 *
 * @param baseHeight Height when the sheet is collapsed.
 * @param state Expansion state. Null keeps the height at [baseHeight].
 */
fun Modifier.expandableHeight(baseHeight: Dp, state: SheetExpansionState?): Modifier =
    layout { measurable, constraints ->
        val basePx = baseHeight.roundToPx()
        if (state != null) {
            state.maxExpansionPx = if (constraints.hasBoundedHeight) {
                (constraints.maxHeight - basePx).coerceAtLeast(0).toFloat()
            } else {
                0f
            }
        }
        val targetHeight = basePx + (state?.expansionPx ?: 0f).roundToInt()
        val height = targetHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
