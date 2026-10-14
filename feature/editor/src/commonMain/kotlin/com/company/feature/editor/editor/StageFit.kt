package com.company.feature.editor.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/** Free space between the fitted stage and the edges of the free screen part. */
private val StageFitGap = 10.dp

/** The fitted stage is never smaller than this part of its size. */
private const val STAGE_MIN_SCALE = 0.3f

/**
 * Scale and vertical move of the stage drawing.
 *
 * @property scale Scale around the stage center.
 * @property translationY Vertical move in pixels. A negative value moves the stage up.
 */
internal data class StageTransform(val scale: Float, val translationY: Float) {
    companion object {
        /** No change. */
        val None = StageTransform(scale = 1f, translationY = 0f)
    }
}

/**
 * Finds the transform that puts the stage between [top] and [bottom]. All values are pixels in the same space.
 *
 * A stage that ends above [bottom] does not change. Another stage gets as small as necessary, but not smaller than
 * [minScale]. Then it moves the shortest distance into the free part. If [minScale] stops it, it touches [top].
 *
 * @param stageTop Top of the stage before the fit.
 * @param stageHeight Height of the stage before the fit.
 * @param top Top of the free part.
 * @param bottom Bottom of the free part.
 * @param minScale Smallest scale.
 */
internal fun fitStage(stageTop: Float, stageHeight: Float, top: Float, bottom: Float, minScale: Float): StageTransform {
    if (stageHeight <= 0f || stageTop + stageHeight <= bottom) return StageTransform.None
    val scale = ((bottom - top) / stageHeight).coerceIn(minScale, 1f)
    val half = stageHeight * scale / 2f
    val center = stageTop + stageHeight / 2f
    val fitted = center.coerceAtMost(bottom - half).coerceAtLeast(top + half)
    return StageTransform(scale = scale, translationY = fitted - center)
}

/**
 * Fits the stage into the screen part above a preview sheet, so that the sheet does not cover the stage.
 *
 * It changes only the drawing of the stage. The layout does not change, so the video surface keeps its size.
 * The sheet reports its position on each frame, so the stage follows the sheet and the keyboard.
 */
@Stable
internal class StageFit {
    /** Height in pixels from the top of the preview sheet to the window bottom. It is 0 when no preview sheet is open. */
    var coveredHeight by mutableIntStateOf(0)

    private var windowHeight by mutableFloatStateOf(0f)
    private var areaTop by mutableFloatStateOf(0f)
    private var stageTop by mutableFloatStateOf(0f)
    private var stageHeight by mutableFloatStateOf(0f)

    /**
     * Keeps the top of the stage area. The stage does not go above it.
     *
     * @param coordinates Coordinates of the stage area.
     */
    fun updateArea(coordinates: LayoutCoordinates) {
        areaTop = coordinates.positionInRoot().y
        windowHeight = coordinates.findRootCoordinates().size.height.toFloat()
    }

    /**
     * Keeps the position and the height of the stage before the fit.
     *
     * @param coordinates Coordinates of the stage, outside the fit layer.
     */
    fun updateStage(coordinates: LayoutCoordinates) {
        stageTop = coordinates.positionInRoot().y
        stageHeight = coordinates.size.height.toFloat()
    }

    /**
     * Sets the scale and the move of the stage layer.
     *
     * @param layer Layer of the stage.
     */
    fun applyTo(layer: GraphicsLayerScope) {
        val covered = coveredHeight
        val transform = if (covered <= 0) {
            StageTransform.None
        } else {
            val gap = with(layer) { StageFitGap.toPx() }
            fitStage(
                stageTop = stageTop,
                stageHeight = stageHeight,
                top = areaTop + gap,
                bottom = windowHeight - covered - gap,
                minScale = STAGE_MIN_SCALE,
            )
        }
        layer.scaleX = transform.scale
        layer.scaleY = transform.scale
        layer.translationY = transform.translationY
    }
}
