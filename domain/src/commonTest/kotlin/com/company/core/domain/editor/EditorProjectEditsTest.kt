package com.company.core.domain.editor

import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.MAX_TEXT_SCALE
import com.company.core.domain.model.editor.MIN_TEXT_LENGTH
import com.company.core.domain.model.editor.MIN_TEXT_SCALE
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.addText
import com.company.core.domain.model.editor.applyTemplate
import com.company.core.domain.model.editor.clipAt
import com.company.core.domain.model.editor.compactTextLayers
import com.company.core.domain.model.editor.cutTextAtEnd
import com.company.core.domain.model.editor.duplicateClip
import com.company.core.domain.model.editor.duplicateText
import com.company.core.domain.model.editor.moveText
import com.company.core.domain.model.editor.moveTextTime
import com.company.core.domain.model.editor.overlapsOtherText
import com.company.core.domain.model.editor.removeText
import com.company.core.domain.model.editor.splitAt
import com.company.core.domain.model.editor.textLayerCount
import com.company.core.domain.model.editor.transformText
import com.company.core.domain.model.editor.trimClipEnd
import com.company.core.domain.model.editor.trimClipStart
import com.company.core.domain.model.editor.trimTextEnd
import com.company.core.domain.model.editor.trimTextStart
import com.company.core.domain.model.editor.updateText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorProjectEditsTest {

    private fun clip(id: String, length: Double, speed: Double = 1.0) = Clip(
        id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, speed = speed, thumbnailHue = 0,
    )

    private val project = EditorProject(name = "p", clips = listOf(clip("a", 4.0), clip("b", 2.0, speed = 2.0)))

    @Test
    fun cutTextAtEnd_cutsOnlyATextThatGoesPastTheEnd() {
        // 프로젝트 길이는 5초다.
        val withTexts = project.copy(
            texts = listOf(
                TextOverlay("past", "hi", 3.0, 4.0, TextStyleType.BOLD, 50.0, 50.0),
                TextOverlay("inside", "hi", 0.0, 2.0, TextStyleType.BOLD, 50.0, 50.0),
                TextOverlay("after", "hi", 6.0, 1.0, TextStyleType.BOLD, 50.0, 50.0, layer = 1),
            ),
        )
        assertEquals(2.0, withTexts.cutTextAtEnd("past").texts.first { it.id == "past" }.length, 1e-9)
        assertEquals(withTexts, withTexts.cutTextAtEnd("inside"))
        assertEquals(withTexts, withTexts.cutTextAtEnd("after"))
    }

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
    fun trimText_keepsOtherEdgeAndMinimumLength() {
        val withText = project.addText("hi", TextStyleType.BOLD, time = 1.0)
        val id = withText.texts.single().id

        val shorter = withText.trimTextStart(id, 2.0).texts.single()
        assertEquals(2.0, shorter.start)
        assertEquals(4.0, shorter.start + shorter.length)
        val shortest = withText.trimTextStart(id, 9.0).texts.single()
        assertEquals(MIN_TEXT_LENGTH, shortest.length)
        assertEquals(0.0, withText.trimTextStart(id, -2.0).texts.single().start)

        val longer = withText.trimTextEnd(id, 99.0).texts.single()
        assertEquals(1.0, longer.start)
        assertEquals(project.totalDuration, longer.start + longer.length)
        assertEquals(MIN_TEXT_LENGTH, withText.trimTextEnd(id, 0.0).texts.single().length)
    }

    @Test
    fun moveTextTime_keepsLengthInsideProject() {
        val withText = project.addText("hi", TextStyleType.BOLD, time = 1.0)
        val id = withText.texts.single().id

        assertEquals(2.0, withText.moveTextTime(id, 2.0).texts.single().start)
        assertEquals(2.0, withText.moveTextTime(id, 9.0).texts.single().start)
        assertEquals(0.0, withText.moveTextTime(id, -1.0).texts.single().start)
        assertEquals(3.0, withText.moveTextTime(id, 9.0).texts.single().length)
    }

    @Test
    fun duplicateText_putsCopyAfterOriginalInsideProject() {
        val withText = project.addText("hi", TextStyleType.BOLD, time = 0.0)
        val id = withText.texts.single().id

        val (duplicated, copyId) = assertNotNull(withText.duplicateText(id))
        assertEquals(listOf(id, copyId), duplicated.texts.map { it.id })
        // 원본(0~3초) 바로 뒤는 3~6초지만, 프로젝트가 5초라서 2~5초로 당긴다. 원본과 겹치므로 다음 줄에 들어간다.
        assertEquals(2.0, duplicated.texts.last().start)
        assertEquals(1, duplicated.texts.last().layer)
        assertNull(withText.duplicateText("missing"))
    }

    @Test
    fun textFont_staysWithTextAndItsCopy() {
        val withText = project.addText("hi", TextStyleType.BOLD, time = 0.0, font = TextFontType.JUA)
        val id = withText.texts.single().id
        assertEquals(TextFontType.JUA, withText.texts.single().font)
        assertEquals(TextFontType.JUA, assertNotNull(withText.duplicateText(id)).first.texts.last().font)
        assertEquals(TextFontType.DO_HYEON, withText.updateText(id, "hi", TextStyleType.BOX, TextFontType.DO_HYEON).texts.single().font)
        // null이면 스타일의 글꼴을 쓴다.
        assertNull(withText.updateText(id, "hi", TextStyleType.BOX, font = null).texts.single().font)
    }

    /** Project of 5 seconds with texts at 0~3 seconds (row 0), 1~4 seconds (row 1), and 3~5 seconds (row 0). */
    private fun layeredProject(): EditorProject = project
        .addText("a", TextStyleType.BOLD, time = 0.0)
        .addText("b", TextStyleType.BOLD, time = 1.0)
        .addText("c", TextStyleType.BOLD, time = 3.0)

    private fun EditorProject.text(name: String) = texts.single { it.text == name }

    @Test
    fun addText_usesFirstRowWithoutOverlap() {
        val texts = layeredProject()
        assertEquals(0, texts.text("a").layer)
        // 0~3초와 겹치므로 새 줄.
        assertEquals(1, texts.text("b").layer)
        // 3초에 끝나는 글자와 닿기만 하므로 첫 줄.
        assertEquals(0, texts.text("c").layer)
        // 2~5초는 두 줄 모두와 겹치므로 새 줄.
        assertEquals(2, texts.addText("d", TextStyleType.BOLD, time = 2.0).text("d").layer)
        assertEquals(2, texts.textLayerCount)
    }

    @Test
    fun trimText_stopsAtNeighborInSameRow() {
        val texts = layeredProject()
        assertEquals(3.0, texts.trimTextEnd(texts.text("a").id, 4.5).text("a").end)
        assertEquals(3.0, texts.trimTextStart(texts.text("c").id, 1.0).text("c").start)
        // 다른 줄의 글자는 막지 않는다.
        assertEquals(0.0, texts.trimTextStart(texts.text("b").id, 0.0).text("b").start)
    }

    @Test
    fun moveTextTime_toOtherRow_reportsOverlap() {
        val texts = layeredProject()
        val c = texts.text("c").id
        // c(3~5초)를 b가 있는 두 번째 줄로 옮기면 b(1~4초)와 겹친다.
        val onB = texts.moveTextTime(c, 3.0, layer = 1)
        assertEquals(1, onB.text("c").layer)
        assertTrue(onB.overlapsOtherText(c))
        // b가 3초에 끝나면 c(3~5초)는 b와 닿기만 하므로 놓을 수 있다.
        val shortB = texts.trimTextEnd(texts.text("b").id, 3.0)
        assertFalse(shortB.moveTextTime(c, 3.0, layer = 1).overlapsOtherText(c))
        // 새 줄은 비어 있으므로 놓을 수 있다.
        assertFalse(texts.moveTextTime(c, 3.0, layer = 2).overlapsOtherText(c))
        assertEquals(0, texts.moveTextTime(c, 3.0, layer = -1).text("c").layer)
    }

    @Test
    fun compactTextLayers_removesRowLeftEmptyByMove() {
        val texts = layeredProject()
        val b = texts.text("b").id
        // b가 혼자 있던 두 번째 줄을 떠나 세 번째 줄로 가면, 정리 후 두 번째 줄이 된다.
        val moved = texts.moveTextTime(b, 1.0, layer = 2).compactTextLayers()
        assertEquals(1, moved.text("b").layer)
        assertEquals(2, moved.textLayerCount)
    }

    @Test
    fun removeText_movesRowsBelowEmptyRowUp() {
        val texts = layeredProject().addText("d", TextStyleType.BOLD, time = 2.0)
        val removed = texts.removeText(texts.text("b").id)
        assertEquals(1, removed.text("d").layer)
        assertEquals(2, removed.textLayerCount)
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
    fun transformText_keepsScaleInRangeAndNormalizesAngle() {
        val withText = project.addText("hi", TextStyleType.BOLD, time = 0.0)
        val id = withText.texts.single().id
        assertEquals(1.0, withText.texts.single().scale)
        assertEquals(0.0, withText.texts.single().rotation)
        val changed = withText.transformText(id, scale = 1.8, rotation = 30.0).texts.single()
        assertEquals(1.8, changed.scale)
        assertEquals(30.0, changed.rotation)
        assertEquals(MAX_TEXT_SCALE, withText.transformText(id, scale = 10.0, rotation = 0.0).texts.single().scale)
        assertEquals(MIN_TEXT_SCALE, withText.transformText(id, scale = 0.1, rotation = 0.0).texts.single().scale)
        assertEquals(-170.0, withText.transformText(id, scale = 1.0, rotation = 190.0).texts.single().rotation)
        assertEquals(170.0, withText.transformText(id, scale = 1.0, rotation = -190.0).texts.single().rotation)
        assertEquals(-180.0, withText.transformText(id, scale = 1.0, rotation = 180.0).texts.single().rotation)
    }

    @Test
    fun transformText_staysWithTextAndItsCopy() {
        val withText = project.addText("hi", TextStyleType.BOLD, time = 0.0)
        val id = withText.texts.single().id
        val changed = withText.transformText(id, scale = 1.8, rotation = -15.0)
        // 글자를 고치거나 복제해도 크기와 각도는 그대로다.
        val updated = changed.updateText(id, "hello", TextStyleType.BOX).texts.single()
        assertEquals(1.8, updated.scale)
        assertEquals(-15.0, updated.rotation)
        val copy = assertNotNull(changed.duplicateText(id)).first.texts.last()
        assertEquals(1.8, copy.scale)
        assertEquals(-15.0, copy.rotation)
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
