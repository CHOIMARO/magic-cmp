package com.company.feature.main.splash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplashTimelineTest {

    private val end = SplashTimeline.TotalMillis.toFloat()

    @Test
    fun pen_startsHiddenAtTheLeftOfTheWord() {
        val pen = SplashTimeline.penAt(0f)
        assertEquals(-0.02f, pen.xFraction, 1e-4f)
        assertEquals(0.78f, pen.yFraction, 1e-4f)
        assertEquals(0f, pen.alpha)
    }

    @Test
    fun pen_landsAtTheTopRightOfTheWord_withTheStarSize() {
        val pen = SplashTimeline.penAt(end)
        assertEquals(1f, pen.xFraction, 1e-4f)
        assertEquals(SplashTimeline.LandOffsetDp, pen.xOffsetDp, 1e-4f)
        assertEquals(SplashTimeline.LandYFraction, pen.yFraction, 1e-4f)
        assertEquals(0f, pen.rotation, 1e-3f)
        assertEquals(1f, pen.scale, 1e-3f)
        assertEquals(1f, pen.alpha, 1e-4f)
    }

    @Test
    fun pen_growsLargerThanItsFinalSize_whenItLands() {
        val largest = (1300..1700).maxOf { SplashTimeline.penAt(it.toFloat()).scale }
        assertTrue(largest > 1.3f)
    }

    @Test
    fun word_isHiddenAtTheStart_andFullyShownAtTheEnd() {
        val (_, clearStart) = SplashTimeline.revealEdges(SplashTimeline.revealAt(0f))
        assertTrue(clearStart <= 0f)
        val (opaqueEnd, _) = SplashTimeline.revealEdges(SplashTimeline.revealAt(end))
        assertTrue(opaqueEnd >= 1f)
    }

    @Test
    fun splash_staysOpaque_untilItFadesOutAtTheEnd() {
        assertEquals(1f, SplashTimeline.overlayAlphaAt(1800f))
        assertEquals(0f, SplashTimeline.overlayAlphaAt(end), 1e-4f)
    }

    @Test
    fun flash_andSparks_areGoneWhenTheSplashEnds() {
        assertEquals(0f, SplashTimeline.flashAt(end).alpha, 1e-4f)
        assertEquals(18, SplashTimeline.Sparks.size)
        SplashTimeline.Sparks.forEach { spark ->
            assertTrue(spark.sizeDp in 3f..7f)
            assertTrue(spark.delay + spark.duration < end)
            assertEquals(0f, SplashTimeline.sparkAt(spark, end).alpha, 1e-4f)
        }
    }

    @Test
    fun keyframes_useTheEasingOfTheStartKeyframe() {
        val frames = listOf(Keyframe(0f, 0f), Keyframe(0.5f, 10f), Keyframe(1f, 20f))
        assertEquals(5f, frames.valueAt(0.25f), 1e-4f)
        assertEquals(15f, frames.valueAt(0.75f), 1e-4f)
        assertEquals(0f, frames.valueAt(-1f))
        assertEquals(20f, frames.valueAt(2f))
    }
}
