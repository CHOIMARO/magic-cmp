package com.company.domain.editor

import com.company.domain.model.editor.BackgroundShape
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.TextContent
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.decorate
import com.company.domain.model.editor.drawnFont
import com.company.domain.model.editor.withBackground
import com.company.domain.model.editor.withBackgroundBorder
import com.company.domain.model.editor.withBackgroundShape
import com.company.domain.model.editor.withFont
import com.company.domain.model.editor.withTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextTemplatesTest {

    private val plain = TextContent(text = "오늘의 기록")

    @Test
    fun newText_hasNoTemplate_andUsesPretendard() {
        assertNull(plain.style)
        assertEquals(TextFontType.PRETENDARD, plain.drawnFont)
        assertEquals(3, plain.sizeStep)
    }

    @Test
    fun decoratingATemplate_removesIt_andKeepsItsFont() {
        val impact = plain.withTemplate(TextStyleType.IMPACT).copy(sizeStep = 5)
        val decorated = impact.decorate(0, 2) { it.copy(bold = true) }

        assertNull(decorated.style)
        // 템플릿 글꼴을 남기되, 직접 고른 글꼴이 아니라고 표시한다 (D61).
        assertEquals(TextFontType.BLACK_HAN_SANS, decorated.font)
        assertTrue(decorated.fontFromTemplate)
        assertEquals(listOf(DecorationRange(0, 2, Decoration(bold = true))), decorated.decorations)
        assertEquals(5, decorated.sizeStep)
    }

    @Test
    fun decoratingATemplate_keepsAPickedFont() {
        val picked = plain.withFont(TextFontType.JUA).withTemplate(TextStyleType.NEON)
        val decorated = picked.decorate(0, 1) { it.copy(color = 0xFF0000) }

        assertEquals(TextFontType.JUA, decorated.font)
        assertFalse(decorated.fontFromTemplate)
    }

    @Test
    fun aChangeWithoutADecoration_keepsTheTemplate() {
        val neon = plain.withTemplate(TextStyleType.NEON)
        assertEquals(neon, neon.decorate(0, plain.text.length) { it.withBackground(null) })
        assertEquals(neon, neon.decorate(0, plain.text.length) { it.withBackgroundShape(BackgroundShape.SQUARE) })
    }

    @Test
    fun pickingATemplate_removesTheDecorations_andTheFontThatATemplateLeft() {
        val left = plain.withTemplate(TextStyleType.IMPACT).decorate(0, 1) { it.copy(outline = true) }
        val thumbnail = left.withTemplate(TextStyleType.THUMB_HAND)

        assertEquals(TextStyleType.THUMB_HAND, thumbnail.style)
        assertTrue(thumbnail.decorations.isEmpty())
        assertNull(thumbnail.font)
        assertFalse(thumbnail.fontFromTemplate)
        assertEquals(TextFontType.DOKDO, thumbnail.drawnFont)
    }

    @Test
    fun pickingATemplate_keepsAPickedFont_andTheSizeStep() {
        val picked = plain.withFont(TextFontType.GOWUN_BATANG).copy(sizeStep = 2)
            .decorate(0, 1) { it.copy(shadow = true) }
            .withTemplate(TextStyleType.THUMB_FIRE)

        assertEquals(TextFontType.GOWUN_BATANG, picked.drawnFont)
        assertEquals(2, picked.sizeStep)
    }

    @Test
    fun resettingTheTemplate_goesBackToNoTemplate() {
        val reset = plain.withTemplate(TextStyleType.MARKER).withTemplate(null)
        assertNull(reset.style)
        assertEquals(TextFontType.PRETENDARD, reset.drawnFont)
    }

    @Test
    fun boxShapeAndBorder_changeOnlyCharactersWithABackground() {
        val half = plain.decorate(0, 3) { it.withBackground(0xFFD6A5) }
        val square = half.decorate(0, plain.text.length) { it.withBackgroundShape(BackgroundShape.SQUARE).withBackgroundBorder(true) }

        assertEquals(
            listOf(DecorationRange(0, 3, Decoration(background = 0xFFD6A5, backgroundShape = BackgroundShape.SQUARE, backgroundBorder = true))),
            square.decorations,
        )
        // 배경을 없애면 모양과 테두리도 기본값으로 돌아가서 꾸미기가 남지 않는다.
        assertTrue(square.decorate(0, plain.text.length) { it.withBackground(null) }.decorations.isEmpty())
    }
}
