package com.company.domain.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.ThumbnailTemplate
import com.company.domain.model.editor.addText
import com.company.domain.model.editor.addThumbnail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThumbnailTemplatesTest {

    private val project = EditorProject(
        name = "p",
        clips = listOf(Clip("a", "a.mp4", sourceDuration = 10.0, inPoint = 0.0, outPoint = 10.0, thumbnailHue = 0)),
    )

    @Test
    fun designHasSixThumbnails_andEachLineHasItsOwnLook() {
        assertEquals(6, ThumbnailTemplate.entries.size)
        assertEquals(listOf(4, 3, 3, 5, 4, 4), ThumbnailTemplate.entries.map { it.lines.size })
        val lineStyles = ThumbnailTemplate.entries.flatMap { template -> template.lines.map { it.style } }
        assertEquals(TextStyleType.entries.filter { it.thumbnail }, lineStyles)
        assertEquals(lineStyles.size, lineStyles.toSet().size)
    }

    @Test
    fun accents_markTheSameWordsAsTheDesign() {
        val bubble = ThumbnailTemplate.BUBBLE.lines
        assertEquals(listOf(DecorationRange(0, 2, Decoration(sizeScale = 0.68f))), bubble[0].accents)
        assertEquals(listOf(DecorationRange(3, 5, Decoration(sizeScale = 0.65f))), bubble[1].accents)
        assertEquals(listOf(DecorationRange(5, 7, Decoration(color = 0xFFF09C))), ThumbnailTemplate.BOLD.lines[1].accents)
    }

    @Test
    fun addThumbnail_putsEachLineOnItsOwnRow_withTheAccents() {
        val result = project.addThumbnail(ThumbnailTemplate.BUBBLE, start = 0.0)
        assertEquals(listOf("월 260", "1억 모은", "가계부작성법"), result.texts.map { it.text })
        assertEquals(listOf(0, 1, 2), result.texts.map { it.layer })
        assertEquals(ThumbnailTemplate.BUBBLE.lines.map { it.accents }, result.texts.map { it.decorations })
        assertTrue(result.texts.all { it.length == 3.0 })
    }

    @Test
    fun addThumbnail_replacesTheText_andStartsAtItsStart() {
        val withText = project.addText("hi", TextStyleType.NEON, time = 5.0)
        val text = withText.texts.single()
        val result = withText.addThumbnail(ThumbnailTemplate.HAND, start = text.start, replaceTextId = text.id)
        assertNull(result.texts.find { it.id == text.id })
        assertEquals(3, result.texts.size)
        assertTrue(result.texts.all { it.start == 5.0 && it.length == 3.0 })
    }

    @Test
    fun addThumbnail_nearTheVideoEnd_staysInsideTheVideo() {
        val result = project.addThumbnail(ThumbnailTemplate.THIN, start = 8.5)
        assertTrue(result.texts.all { it.length == 1.5 })
        assertEquals(10.0, result.totalDuration)
    }

    @Test
    fun lineOf_findsTheLineOfALook() {
        assertEquals("서울", ThumbnailTemplate.lineOf(TextStyleType.THUMB_FIRE_2)?.text)
        assertNull(ThumbnailTemplate.lineOf(TextStyleType.NEON))
    }
}
