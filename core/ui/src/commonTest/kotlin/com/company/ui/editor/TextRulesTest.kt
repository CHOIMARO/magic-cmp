package com.company.ui.editor

import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextRulesTest {

    @Test
    fun shortTitle_staysOnOneLine() {
        val lines = TextStyleType.BOLD.look().breakLines("바닷가 주말".splitWords())
        assertEquals(listOf(listOf("바닷가", "주말")), lines.lines)
        assertEquals(1f, lines.fontScale)
    }

    @Test
    fun longTitle_breaksIntoLinesOfSimilarLength() {
        // 굵게는 한 줄에 8자, 최대 2줄이다.
        val look = TextStyleType.BOLD.look()
        assertEquals(8, look.maxCharsPerLine)
        val lines = look.breakLines("처음 가본 동네 빵집 투어 브이로그".splitWords())
        assertEquals(listOf(listOf("처음", "가본", "동네"), listOf("빵집", "투어", "브이로그")), lines.lines)
        assertEquals(1f, lines.fontScale)
    }

    @Test
    fun tooLongTitle_getsSmallerToSeventyPercent() {
        // 24자는 2줄 16자를 넘어서 글자를 줄인다. 16/24는 70%보다 작으므로 70%에서 멈춘다.
        val lines = TextStyleType.BOLD.look().breakLines("가나다라 마바사아 자차카타 파하가나 다라마바 사아자차".splitWords())
        assertEquals(0.7f, lines.fontScale)
        assertEquals(2, lines.lines.size)
    }

    @Test
    fun tag_staysOnOneLine() {
        val lines = TextStyleType.TAG.look().breakLines("아주 긴 라벨 문구를 한 줄로 보여 줘요".splitWords())
        assertEquals(1, lines.lines.size)
    }

    @Test
    fun bigCaption_allowsThreeLines() {
        assertEquals(3, CaptionStyleType.BIG.look().maxLines)
        assertTrue(CaptionStyleType.BIG.look().revealByWord)
    }

    @Test
    fun pop_startsSmallAndInvisibleAndEndsStatic() {
        val animation = TextStyleType.BOLD.look().animation
        val start = animation.frameAt(elapsed = 0.0, remaining = 3.0)
        assertEquals(0f, start.alpha)
        assertEquals(0.6f, start.scale, 1e-4f)
        val middle = animation.frameAt(elapsed = 1.0, remaining = 2.0)
        assertEquals(1f, middle.alpha)
        assertEquals(1f, middle.scale, 1e-4f)
    }

    @Test
    fun exit_fadesOutAtTheEnd() {
        val animation = TextStyleType.BOLD.look().animation
        assertEquals(0f, animation.frameAt(elapsed = 3.0, remaining = 0.0).alpha)
        val half = animation.frameAt(elapsed = 3.0, remaining = 0.1).alpha
        assertTrue(half > 0f && half < 1f)
    }

    @Test
    fun markerSweep_drawsTheBackgroundFromTheLeft() {
        val animation = TextStyleType.MARKER.look().animation
        assertEquals(0f, animation.frameAt(elapsed = 0.0, remaining = 3.0).backgroundReveal)
        assertEquals(1f, animation.frameAt(elapsed = 1.0, remaining = 2.0).backgroundReveal)
    }
}
