package com.company.core.media

import com.company.core.domain.model.editor.ClipTransition
import com.company.core.domain.model.editor.PushDirection
import com.company.core.domain.model.editor.TransitionType
import com.company.core.domain.model.editor.TransitionWindow
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransitionTimelineTest {

    /** Two clips of 2 seconds. The boundary is at 2 seconds. */
    private val clipStartsUs = listOf(0L, 2_000_000L)

    private fun boundary(type: TransitionType, direction: PushDirection = PushDirection.RIGHT) = TransitionWindow(
        transition = ClipTransition(type, direction),
        start = 1.75,
        end = 2.25,
        boundary = 2.0,
        fromClip = 0,
        toClip = 1,
    )

    private fun opening(type: TransitionType, end: Double = 0.5) =
        TransitionWindow(ClipTransition(type), start = 0.0, end = end, boundary = 0.0, fromClip = null, toClip = 0)

    /** Time on the timeline in microseconds at [progress] of [window]. */
    private fun timeUs(window: TransitionWindow, progress: Double): Long =
        ((window.start + (window.end - window.start) * progress) * 1_000_000).toLong()

    /**
     * Part of the output that comes from each scene, when both scenes fill the frame.
     *
     * @return The part of the live scene and the part of the still scene.
     */
    private fun TransitionFrame.weights(): Pair<Float, Float> {
        val stillAlpha = still?.look?.alpha ?: 0f
        return live.alpha * (1f - stillAlpha) to stillAlpha
    }

    private fun assertNear(expected: Float, actual: Float, message: String? = null) {
        assertTrue(abs(expected - actual) < 1e-4f, "${message.orEmpty()} expected $expected but was $actual")
    }

    @Test
    fun noTransitionsUseNoLayers() {
        val timeline = TransitionTimeline(emptyList(), clipStartsUs)

        assertEquals(TransitionLayers(), timeline.layers)
        assertNull(timeline.frameAt(2_000_000L))
    }

    @Test
    fun timeOutsideTheWindowHasNoFrame() {
        val timeline = TransitionTimeline(listOf(boundary(TransitionType.DISSOLVE)), clipStartsUs)

        assertNull(timeline.frameAt(1_000_000L))
        assertNull(timeline.frameAt(2_250_000L))
        assertNotNull(timeline.frameAt(1_750_000L))
    }

    @Test
    fun dissolveBeforeTheBoundaryPutsTheNextFirstFrameOnTop() {
        val window = boundary(TransitionType.DISSOLVE)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        val frame = assertNotNull(timeline.frameAt(timeUs(window, 0.25)))
        val look = transitionLook(TransitionType.DISSOLVE, 0.25f)

        val still = assertNotNull(frame.still)
        assertEquals(StillRef(clipIndex = 1, edge = StillEdge.FIRST), still.ref)
        assertNear(look.to.alpha, still.look.alpha)
        assertNear(1f, frame.live.alpha)
    }

    @Test
    fun dissolveAfterTheBoundaryMatchesTheFormula() {
        val window = boundary(TransitionType.DISSOLVE)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        val frame = assertNotNull(timeline.frameAt(timeUs(window, 0.75)))
        val look = transitionLook(TransitionType.DISSOLVE, 0.75f)
        val (liveWeight, stillWeight) = frame.weights()

        assertEquals(StillRef(clipIndex = 0, edge = StillEdge.LAST), frame.still?.ref)
        assertNear(look.to.alpha, liveWeight, "next scene")
        assertNear((1f - look.to.alpha) * look.from.alpha, stillWeight, "previous scene")
    }

    @Test
    fun zoomAfterTheBoundaryMatchesTheFormula() {
        val window = boundary(TransitionType.ZOOM)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        for (progress in listOf(0.5, 0.6, 0.8, 0.95)) {
            val frame = assertNotNull(timeline.frameAt(timeUs(window, progress)))
            val look = transitionLook(TransitionType.ZOOM, window.progressAt(timeUs(window, progress) / 1e6).toFloat())
            val (liveWeight, stillWeight) = frame.weights()

            assertNear(look.to.alpha, liveWeight, "next scene at $progress")
            assertNear((1f - look.to.alpha) * look.from.alpha, stillWeight, "previous scene at $progress")
            assertNear(look.from.scale, assertNotNull(frame.still).look.scale, "still scale at $progress")
            assertNear(look.to.scale, frame.live.scale, "live scale at $progress")
        }
    }

    @Test
    fun pushAfterTheBoundaryKeepsThePreviousSceneOpaque() {
        val window = boundary(TransitionType.PUSH, PushDirection.LEFT)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        val time = timeUs(window, 0.7)
        val frame = assertNotNull(timeline.frameAt(time))
        val look = transitionLook(TransitionType.PUSH, window.progressAt(time / 1e6).toFloat(), PushDirection.LEFT)

        val still = assertNotNull(frame.still)
        assertEquals(StillRef(clipIndex = 0, edge = StillEdge.LAST), still.ref)
        assertNear(1f, still.look.alpha)
        assertNear(look.from.translateX, still.look.translateX)
        assertNear(look.to.translateX, frame.live.translateX)
        assertNear(1f, frame.live.alpha)
    }

    @Test
    fun fadeNeedsNoStill() {
        val window = boundary(TransitionType.FADE)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        val before = assertNotNull(timeline.frameAt(timeUs(window, 0.4)))
        val after = assertNotNull(timeline.frameAt(timeUs(window, 0.6)))

        assertNull(before.still)
        assertNull(after.still)
        assertTrue(before.coverAlpha > 0f)
        assertEquals(TransitionLayers(cover = true), timeline.layers)
    }

    @Test
    fun glitchShakesTheLiveFrameOnly() {
        val timeline = TransitionTimeline(listOf(boundary(TransitionType.GLITCH)), clipStartsUs)

        assertEquals(TransitionLayers(shape = true, color = true), timeline.layers)
        assertTrue(timeline.stillsOf(0).isEmpty())
    }

    @Test
    fun dissolveUsesOnlyTheStillLayer() {
        val timeline = TransitionTimeline(listOf(boundary(TransitionType.DISSOLVE)), clipStartsUs)

        assertEquals(TransitionLayers(still = true), timeline.layers)
        assertEquals(
            setOf(StillRef(1, StillEdge.FIRST), StillRef(0, StillEdge.LAST)),
            timeline.stillsOf(0),
        )
    }

    @Test
    fun blurStillsKeepTheBoundaryBlur() {
        val timeline = TransitionTimeline(listOf(boundary(TransitionType.BLUR)), clipStartsUs)
        val boundaryLook = transitionLook(TransitionType.BLUR, 0.5f)

        assertTrue(timeline.layers.blur)
        assertTrue(timeline.layers.still)
        assertNear(boundaryLook.to.blur, timeline.stillBlur(StillRef(1, StillEdge.FIRST)))
        assertNear(boundaryLook.from.blur, timeline.stillBlur(StillRef(0, StillEdge.LAST)))
    }

    @Test
    fun theMediaClipStartDecidesTheLiveClip() {
        val window = boundary(TransitionType.DISSOLVE)
        // Media3가 다음 클립을 경계보다 조금 늦게 시작하면, 그 전 프레임은 아직 앞 클립이다.
        val timeline = TransitionTimeline(listOf(window), listOf(0L, 2_000_400L))

        val frame = assertNotNull(timeline.frameAt(2_000_200L))

        assertEquals(StillEdge.FIRST, frame.still?.ref?.edge)
    }

    @Test
    fun openingShowsTheFirstClipLookWithoutAStill() {
        val window = opening(TransitionType.ZOOM)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        val frame = assertNotNull(timeline.frameAt(timeUs(window, 0.5)))
        val look = openingLook(TransitionType.ZOOM, 0.5f)

        assertNull(frame.still)
        assertNear(look.to.alpha, frame.live.alpha)
        assertNear(look.to.scale, frame.live.scale)
        assertEquals(TransitionLayers(shape = true, color = true), timeline.layers)
    }

    @Test
    fun theLaterTransitionWinsWhenTwoOverlap() {
        // 첫 클립이 짧으면 시작 전환과 첫 경계 전환이 겹칠 수 있다.
        val opening = opening(TransitionType.FADE, end = 1.9)
        val timeline = TransitionTimeline(listOf(opening, boundary(TransitionType.DISSOLVE)), clipStartsUs)

        val frame = assertNotNull(timeline.frameAt(1_800_000L))

        assertEquals(1, timeline.windowIndexAt(1_800_000L))
        assertEquals(StillEdge.FIRST, frame.still?.ref?.edge)
    }

    @Test
    fun windowsWithMissingClipsAreIgnored() {
        val window = boundary(TransitionType.DISSOLVE).copy(toClip = 5)
        val timeline = TransitionTimeline(listOf(window), clipStartsUs)

        assertFalse(timeline.layers.still)
        assertNull(timeline.frameAt(2_000_000L))
    }

    @Test
    fun nextWindowIsTheFirstOneAfterTheTime() {
        val timeline = TransitionTimeline(listOf(boundary(TransitionType.DISSOLVE)), clipStartsUs)

        assertEquals(0, timeline.nextWindowIndexAfter(0L))
        assertEquals(-1, timeline.nextWindowIndexAfter(1_800_000L))
    }
}
