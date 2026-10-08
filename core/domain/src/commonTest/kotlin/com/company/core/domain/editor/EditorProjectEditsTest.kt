package com.company.core.domain.editor

import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.addText
import com.company.core.domain.model.editor.applyTemplate
import com.company.core.domain.model.editor.applyVibe
import com.company.core.domain.model.editor.clipAt
import com.company.core.domain.model.editor.duplicateClip
import com.company.core.domain.model.editor.moveText
import com.company.core.domain.model.editor.splitAt
import com.company.core.domain.model.editor.trimClipEnd
import com.company.core.domain.model.editor.trimClipStart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorProjectEditsTest {

    private fun clip(id: String, length: Double, speed: Double = 1.0) = Clip(
        id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, speed = speed, thumbnailHue = 0,
    )

    private val project = EditorProject(name = "p", clips = listOf(clip("a", 4.0), clip("b", 2.0, speed = 2.0)))

    @Test
    fun totalDuration_usesClipSpeed() {
        assertEquals(5.0, project.totalDuration)
    }

    @Test
    fun clipAt_returnsSourcePositionWithSpeed() {
        val position = assertNotNull(project.clipAt(4.5))
        assertEquals("b", position.clip.id)
        assertEquals(1.0, position.localTime)
    }

    @Test
    fun clipAt_afterEnd_returnsLastClip() {
        assertEquals("b", project.clipAt(99.0)?.clip?.id)
    }

    @Test
    fun splitAt_insideClip_makesTwoClips() {
        val split = assertNotNull(project.splitAt(1.5))
        assertEquals(3, split.clips.size)
        assertEquals(1.5, split.clips[0].outPoint)
        assertEquals(1.5, split.clips[1].inPoint)
        assertEquals(project.totalDuration, split.totalDuration)
    }

    @Test
    fun splitAt_nearClipEdge_returnsNull() {
        assertNull(project.splitAt(0.1))
        assertNull(project.splitAt(3.9))
    }

    @Test
    fun trim_keepsMinimumLengthAndSourceRange() {
        assertEquals(3.5, project.trimClipStart("a", 10.0).clips[0].inPoint)
        assertEquals(0.0, project.trimClipStart("a", -3.0).clips[0].inPoint)
        assertEquals(4.0, project.trimClipEnd("a", 10.0).clips[0].outPoint)
        assertEquals(0.5, project.trimClipEnd("a", 0.0).clips[0].outPoint)
    }

    @Test
    fun duplicateClip_putsCopyAfterOriginal() {
        val (duplicated, copyId) = assertNotNull(project.duplicateClip("a"))
        assertEquals(listOf("a", copyId, "b"), duplicated.clips.map { it.id })
    }

    @Test
    fun applyVibe_changesStylesAndKeepsMusicForNone() {
        val withText = project.copy(musicId = "m9").addText("hi", TextStyleType.BOLD, time = 0.0)
        val warm = withText.applyVibe(Vibe.WARM)
        assertEquals(FilterType.WARM, warm.filter)
        assertEquals("m5", warm.musicId)
        assertEquals(TextStyleType.BOX, warm.texts.single().style)

        val none = warm.applyVibe(Vibe.NONE)
        assertEquals(FilterType.NONE, none.filter)
        assertEquals("m5", none.musicId)
        assertEquals(TextStyleType.BOX, none.texts.single().style)
    }

    @Test
    fun addText_limitsLengthToProject() {
        val added = project.addText("hi", TextStyleType.BOLD, time = 4.6).texts.single()
        assertEquals(4.0, added.start)
        assertEquals(1.0, added.length)
    }

    @Test
    fun moveText_staysInsideSafeArea() {
        val overlay = TextOverlay("t", "hi", 0.0, 1.0, TextStyleType.BOLD, 50.0, 50.0)
        val moved = project.copy(texts = listOf(overlay)).moveText("t", 0.0, 100.0).texts.single()
        assertEquals(12.0, moved.x)
        assertEquals(94.0, moved.y)
    }

    @Test
    fun applyTemplate_textAddsDefaultTitleWhenNoText() {
        val result = project.applyTemplate(EditorTemplate.Text(TextStyleType.NEON), "title", emptyList())
        assertEquals("title", result.texts.single().text)
        assertEquals(TextStyleType.NEON, result.texts.single().style)
    }

    @Test
    fun applyTemplate_captionKeepsExistingCaptions() {
        val existing = listOf(CaptionSegment("c", "old", 0.0, 1.0))
        val fallback = listOf(CaptionSegment("n", "new", 0.0, 1.0))
        val result = project.copy(captions = existing)
            .applyTemplate(EditorTemplate.Caption(CaptionStyleType.BIG), "title", fallback)
        assertEquals(CaptionStyleType.BIG, result.captionStyle)
        assertEquals(existing, result.captions)
    }

    @Test
    fun templateKey_roundTrips() {
        val templates = listOf(
            EditorTemplate.Layout(LayoutType.POLAROID),
            EditorTemplate.Text(TextStyleType.TAG),
            EditorTemplate.Caption(CaptionStyleType.MINIMAL),
        )
        templates.forEach { assertEquals(it, EditorTemplate.fromKey(it.key)) }
        assertNull(EditorTemplate.fromKey("LAYOUT:UNKNOWN"))
        assertNull(EditorTemplate.fromKey(null))
        assertTrue(templates.map { it.key }.toSet().size == templates.size)
    }
}
