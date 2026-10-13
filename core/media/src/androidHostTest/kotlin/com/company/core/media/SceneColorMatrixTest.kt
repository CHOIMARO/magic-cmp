package com.company.core.media

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class SceneColorMatrixTest {

    /** Applies a column-major 4x4 Media3 color matrix to (r, g, b, 1). */
    private fun FloatArray.apply(r: Float, g: Float, b: Float): List<Float> =
        (0 until 3).map { row -> this[row] * r + this[4 + row] * g + this[8 + row] * b + this[12 + row] }

    private fun assertColor(expected: List<Float>, actual: List<Float>) {
        assertTrue(expected.indices.all { abs(expected[it] - actual[it]) < 1e-3f }, "expected $expected but was $actual")
    }

    @Test
    fun plainLookKeepsTheColors() {
        assertColor(listOf(0.2f, 0.5f, 0.9f), sceneColorMatrix(SceneLook()).apply(0.2f, 0.5f, 0.9f))
    }

    @Test
    fun alphaDarkensTowardBlack() {
        assertColor(listOf(0.1f, 0.25f, 0.45f), sceneColorMatrix(SceneLook(alpha = 0.5f)).apply(0.2f, 0.5f, 0.9f))
    }

    @Test
    fun fullHueTurnKeepsTheColors() {
        assertColor(listOf(0.2f, 0.5f, 0.9f), sceneColorMatrix(SceneLook(hueDegrees = 360f)).apply(0.2f, 0.5f, 0.9f))
    }

    @Test
    fun zeroSaturationMakesGray() {
        val gray = sceneColorMatrix(SceneLook(saturation = 0f)).apply(0.2f, 0.5f, 0.9f)

        assertColor(List(3) { gray[0] }, gray)
    }

    @Test
    fun hueTurnKeepsGray() {
        assertColor(listOf(0.5f, 0.5f, 0.5f), sceneColorMatrix(SceneLook(hueDegrees = 120f)).apply(0.5f, 0.5f, 0.5f))
    }

    @Test
    fun whiteCoverMixesTowardWhite() {
        assertColor(listOf(0.6f, 0.75f, 0.95f), coverColorMatrix(CoverColor.WHITE, 0.5f).apply(0.2f, 0.5f, 0.9f))
        assertColor(listOf(1f, 1f, 1f), coverColorMatrix(CoverColor.WHITE, 1f).apply(0.2f, 0.5f, 0.9f))
    }

    @Test
    fun blackCoverMixesTowardBlack() {
        assertColor(listOf(0.15f, 0.375f, 0.675f), coverColorMatrix(CoverColor.BLACK, 0.25f).apply(0.2f, 0.5f, 0.9f))
    }

    @Test
    fun noCoverKeepsTheColors() {
        assertColor(listOf(0.2f, 0.5f, 0.9f), coverColorMatrix(CoverColor.WHITE, 0f).apply(0.2f, 0.5f, 0.9f))
    }
}
