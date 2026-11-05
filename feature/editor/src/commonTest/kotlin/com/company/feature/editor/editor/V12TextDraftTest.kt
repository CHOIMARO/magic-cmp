package com.company.feature.editor.editor

import com.company.domain.model.editor.BackgroundShape
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Decorations before the user types (D76). */
class V12TextDraftTest {

    private val red = Decoration(color = 0xFF0000)

    @Test
    fun decorated_emptyInput_changesThePendingDecoration() {
        val draft = TextDraft(id = null, text = "")
            .decorated(DecorationChange.Color(0xFF0000))
            .decorated(DecorationChange.Bold(true))
        assertEquals(Decoration(color = 0xFF0000, bold = true), draft.pendingDecoration)
        assertTrue(draft.decorations.isEmpty())
        assertEquals(setOf(0xFF0000), draft.decorationValues { it.color })
        assertTrue(draft.hasDecorations)

        // 아무 꾸미기도 남지 않으면 미리 건 꾸미기도 없다.
        val cleared = draft.decorated(DecorationChange.Color(null)).decorated(DecorationChange.Bold(false))
        assertNull(cleared.pendingDecoration)
    }

    @Test
    fun decorated_emptyInput_usesTheBoxRulesOfTheCharacters() {
        // 배경이 없으면 박스 모양·테두리는 바뀌지 않는다.
        val noBackground = TextDraft(id = null, text = "").decorated(DecorationChange.BackgroundBorder(true))
        assertNull(noBackground.pendingDecoration)

        val boxed = TextDraft(id = null, text = "")
            .decorated(DecorationChange.Background(0x000000))
            .decorated(DecorationChange.BackgroundShape(BackgroundShape.SQUARE))
            .decorated(DecorationChange.BackgroundBorder(true))
        assertEquals(Decoration(background = 0x000000, backgroundShape = BackgroundShape.SQUARE, backgroundBorder = true), boxed.pendingDecoration)

        // 배경을 없애면 박스 모양과 테두리도 처음으로 돌아간다.
        assertNull(boxed.decorated(DecorationChange.Background(null)).pendingDecoration)
    }

    @Test
    fun decorated_emptyInputWithATemplate_dropsTheTemplateAndKeepsItsFont() {
        val draft = TextDraft(id = null, text = "", style = TextStyleType.IMPACT).decorated(DecorationChange.Color(0xFF0000))
        assertNull(draft.style)
        assertEquals(TextFontType.BLACK_HAN_SANS, draft.font)
        assertTrue(draft.fontFromTemplate)
        assertEquals(red, draft.pendingDecoration)
    }

    @Test
    fun withText_firstCharactersTakeThePendingDecoration() {
        val draft = TextDraft(id = null, text = "", pendingDecoration = red).withText("ab", composing = false)
        assertEquals(listOf(DecorationRange(0, 2, red)), draft.decorations)
        assertNull(draft.pendingDecoration)
    }

    @Test
    fun withText_composingTheFirstLetter_keepsItsDecoration() {
        // 빈 입력에서 'ㅇ'을 치고 '아'로 조합해도 미리 건 꾸미기가 남는다.
        val typed = TextDraft(id = null, text = "", pendingDecoration = red).withText("ㅇ", composing = true)
        val composed = typed.withText("아", composing = true)
        assertEquals(listOf(DecorationRange(0, 1, red)), composed.decorations)
        // 조합이 아니면 맨 앞 글자를 바꾼 글자는 꾸미기를 받지 않는다.
        assertTrue(typed.withText("b", composing = false).decorations.isEmpty())
    }

    @Test
    fun withText_removingAllCharacters_keepsTheDecorationOfTheFirstCharacter() {
        val draft = TextDraft(id = null, text = "ab", decorations = listOf(DecorationRange(0, 1, red)))
        val emptied = draft.withText("", composing = false)
        assertEquals(red, emptied.pendingDecoration)
        assertTrue(emptied.decorations.isEmpty())
        // 첫 글자에 꾸미기가 없으면 미리 건 꾸미기도 없다.
        val plain = TextDraft(id = null, text = "ab", decorations = listOf(DecorationRange(1, 2, red))).withText("", composing = false)
        assertNull(plain.pendingDecoration)
    }

    @Test
    fun blankDecoration_usesThePendingDecorationOrTheFirstSpace() {
        assertEquals(red, TextDraft(id = null, text = "", pendingDecoration = red).blankDecoration)
        assertEquals(red, TextDraft(id = null, text = "  ", decorations = listOf(DecorationRange(0, 2, red))).blankDecoration)
        assertNull(TextDraft(id = null, text = "").blankDecoration)
        assertEquals(listOf(DecorationRange(0, 2, red)), red.fullRange(2))
        val none: Decoration? = null
        assertTrue(none.fullRange(2).isEmpty())
    }
}
