package com.company.feature.editor.editor

import com.company.core.domain.model.editor.AudioTrack
import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.PushDirection
import com.company.core.domain.model.editor.SoundEffectClip
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.TransitionType
import com.company.core.media.CoverColor
import com.company.core.media.openingLook
import com.company.core.media.transitionLook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class V7TimelineTest {

    private fun clip(id: String, length: Double) = Clip(id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, thumbnailHue = 0)

    private val project = EditorProject(
        name = "p",
        clips = listOf(clip("a", 4.0), clip("b", 4.0)),
        texts = listOf(TextOverlay(id = "t", text = "t", start = 6.0, length = 1.0, style = TextStyleType.BOLD, x = 50.0, y = 50.0)),
        captions = listOf(CaptionSegment("k1", "하나", start = 0.0, length = 2.0), CaptionSegment("k2", "둘", start = 5.0, length = 1.0)),
        soundEffects = listOf(SoundEffectClip(id = "s", effectId = "s1", name = "띠링", start = 1.0, length = 0.5)),
    )

    @Test
    fun captionEdge_snapsToTheClipBoundary_andStopsAtTheNextCaption() {
        val target = TimelineDragTarget.CaptionEdge("k1", TrimEdge.END)
        val edges = project.timelineSnapEdges(target, originStart = 0.0, originEnd = 2.0, delta = 1.9, playhead = 0.0)
        assertEquals(0.1, assertNotNull(snapOffset(edges, threshold = 0.2)), 1e-9)
        val trimmed = project.dragTo(target, originStart = 0.0, originEnd = 2.0, delta = 9.0, layer = null)
        assertEquals(5.0, trimmed.captions.first { it.id == "k1" }.end)
        assertEquals(3.0, trimmed.draggedDistance(target, originStart = 0.0, originEnd = 2.0))
    }

    @Test
    fun audioMove_snapsItsStartToATextStart_andStaysInTheProject() {
        val target = TimelineDragTarget.AudioMove(AudioTrack.SOUND_EFFECT, "s")
        val edges = project.timelineSnapEdges(target, originStart = 1.0, originEnd = 1.5, delta = 4.95, playhead = 0.0)
        assertEquals(0.05, assertNotNull(snapOffset(edges, threshold = 0.2)), 1e-9)
        val moved = project.dragTo(target, originStart = 1.0, originEnd = 1.5, delta = 20.0, layer = null)
        assertEquals(7.5, moved.soundEffects.single().start)
        assertNull(snapOffset(project.timelineSnapEdges(target, 1.0, 1.5, delta = 1.5, playhead = 0.0), threshold = 0.2))
    }

    @Test
    fun transitionLook_endsOnTheNextScene() {
        TransitionType.entries.forEach { type ->
            val start = transitionLook(type, 0f)
            val end = transitionLook(type, 1f)
            assertEquals(1f, end.to.alpha, 1e-4f, type.name)
            assertEquals(0f, end.coverAlpha, 1e-4f, type.name)
            assertEquals(0f, start.coverAlpha, 1e-4f, type.name)
        }
        assertEquals(1f, transitionLook(TransitionType.FADE, 0.5f).coverAlpha, 1e-4f)
        assertEquals(CoverColor.WHITE, transitionLook(TransitionType.FLASH, 0.5f).cover)
    }

    @Test
    fun pushTransition_comesFromTheGivenSide() {
        val fromLeft = transitionLook(TransitionType.PUSH, 0f, PushDirection.LEFT)
        assertEquals(-1f, fromLeft.to.translateX, 1e-4f)
        val fromBottom = transitionLook(TransitionType.PUSH, 0f, PushDirection.DOWN)
        assertEquals(1f, fromBottom.to.translateY, 1e-4f)
        assertEquals(0f, transitionLook(TransitionType.PUSH, 1f, PushDirection.DOWN).to.translateY, 1e-4f)
    }

    @Test
    fun openingLook_startsFromBlack() {
        val fade = openingLook(TransitionType.FADE, 0f)
        assertTrue(fade.fromIsBlack)
        assertEquals(1f, fade.coverAlpha, 1e-4f)
        assertEquals(0f, openingLook(TransitionType.FADE, 1f).coverAlpha, 1e-4f)
        assertEquals(0f, openingLook(TransitionType.ZOOM, 0f).to.alpha, 1e-4f)
    }
}
