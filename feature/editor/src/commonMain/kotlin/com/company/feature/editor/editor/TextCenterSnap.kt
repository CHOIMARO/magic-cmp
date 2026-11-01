package com.company.feature.editor.editor

import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Center of the stage on each axis, in percent. */
internal const val STAGE_CENTER_PERCENT = 50.0

/** Distance from a center line of the stage that snaps a dragged text to the line (v10). */
internal val TextSnapDistance = 6.dp

/**
 * Finds the center lines of the stage that a text center is near.
 *
 * @param x Horizontal position of the text center, in percent of the stage width.
 * @param y Vertical position of the text center, in percent of the stage height.
 * @param snapX Snap distance, in percent of the stage width.
 * @param snapY Snap distance, in percent of the stage height.
 */
internal fun stageCenterGuide(x: Double, y: Double, snapX: Double, snapY: Double): TextSnapGuide =
    TextSnapGuide(
        vertical = abs(x - STAGE_CENTER_PERCENT) < snapX,
        horizontal = abs(y - STAGE_CENTER_PERCENT) < snapY,
    )
