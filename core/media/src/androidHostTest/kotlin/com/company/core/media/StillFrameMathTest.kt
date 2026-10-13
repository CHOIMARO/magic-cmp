package com.company.core.media

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StillFrameMathTest {

    private val portrait = 1080f / 1920f

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(abs(expected - actual) < tolerance, "expected $expected but was $actual")
    }

    @Test
    fun landscapeVideoKeepsTheCenterOfTheFrame() {
        val (width, height) = stillCropSize(1920, 1080, frameAspect = portrait, areaAspect = portrait)

        assertNear(607.5f, width)
        assertNear(1080f, height)
    }

    @Test
    fun wideAreaCutsTheFramePartAgain() {
        // 9:16 프레임으로 먼저 자르고, 그 안에서 칸 비율로 다시 자른다.
        val (width, height) = stillCropSize(1920, 1080, frameAspect = portrait, areaAspect = 1080f / 960f)

        assertNear(607.5f, width)
        assertNear(540f, height)
    }

    @Test
    fun stillHasTheAreaSize() {
        assertEquals(PixelSize(1080, 1920), stillBitmapSize(1080, 1920, blurRadius = 0f))
        assertEquals(PixelSize(540, 960), stillBitmapSize(540, 960, blurRadius = 0f))
    }

    @Test
    fun largeAreaGetsASmallerStill() {
        assertEquals(PixelSize(1080, 1920), stillBitmapSize(2160, 3840, blurRadius = 0f))
    }

    @Test
    fun strongBlurGetsASmallStill() {
        val size = stillBitmapSize(1080, 1920, blurRadius = 40f)

        assertEquals(108, size.width)
        assertEquals(192, size.height)
    }

    @Test
    fun boxRadiiMakeTheGaussianDeviation() {
        for (sigma in listOf(1f, 2.5f, 4f, 8f)) {
            val variance = gaussianBoxRadii(sigma).sumOf { radius -> ((2 * radius + 1) * (2 * radius + 1) - 1) / 12.0 }

            assertNear(sigma, sqrt(variance).toFloat(), tolerance = 0.5f)
        }
    }

    @Test
    fun zeroBlurHasNoRadius() {
        assertTrue(gaussianBoxRadii(0f).all { it == 0 })
    }

    @Test
    fun boxBlurKeepsAPlainImage() {
        val color = 0xFF336699.toInt()
        val pixels = IntArray(20 * 10) { color }

        boxBlur(pixels, 20, 10, intArrayOf(2, 2, 3))

        assertTrue(pixels.all { it == color })
    }

    @Test
    fun boxBlurSpreadsABrightPixel() {
        val black = 0xFF000000.toInt()
        val pixels = IntArray(9 * 9) { black }
        pixels[4 * 9 + 4] = 0xFFFFFFFF.toInt()

        boxBlur(pixels, 9, 9, intArrayOf(1))

        val red = pixels.map { (it shr 16) and 0xFF }
        assertEquals(28, red[4 * 9 + 4])
        assertEquals(28, red[3 * 9 + 3])
        assertEquals(0, red[2 * 9 + 2])
        assertTrue(pixels.all { it ushr 24 == 0xFF })
    }

    @Test
    fun zeroRadiusChangesNothing() {
        val pixels = IntArray(16) { 0xFF000000.toInt() or it }
        val copy = pixels.copyOf()

        boxBlur(pixels, 4, 4, intArrayOf(0, 0, 0))

        assertContentEquals(copy, pixels)
    }
}
