package com.company.ui.editor


import androidx.compose.ui.text.style.TextAlign
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextStyleType
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
    fun font_replacesFontOfStyleAndChangesLineLength() {
        assertEquals(TextFontType.GOWUN_BATANG, TextStyleType.SERIF.look().font)
        assertEquals(TextFontType.GOWUN_BATANG, CaptionStyleType.SERIF.look().font)
        assertEquals(TextFontType.JUA, TextStyleType.SERIF.look(font = TextFontType.JUA).font)
        // 나눔손글씨 펜은 글자 폭이 좁아서, 같은 크기에서 한 줄에 더 많은 글자가 들어간다.
        val pen = TextStyleType.BOLD.look(font = TextFontType.NANUM_PEN)
        assertTrue(pen.maxCharsPerLine > TextStyleType.BOLD.look().maxCharsPerLine)
    }

    /** Checks that the frame is the look without animation. Float math can leave a very small offset. */
    private fun assertStatic(frame: TextFrame, message: String) {
        assertEquals(1f, frame.alpha, 1e-4f, message)
        assertEquals(1f, frame.scale, 1e-4f, message)
        assertEquals(0f, frame.offsetY, 1e-3f, message)
        assertEquals(1f, frame.backgroundReveal, 1e-4f, message)
        assertEquals(1f, frame.textReveal, 1e-4f, message)
    }

    /** True when no part of the text shows. */
    private val TextFrame.hidden: Boolean
        get() = alpha == 0f || textReveal == 0f

    @Test
    fun previewLoop_holdsOneSecondAndPausesShortly() {
        TextStyleType.entries.forEach { style ->
            val animation = style.look().animation
            val entranceEnd = animation.entranceMillis.toLong()
            val exitEnd = entranceEnd + 1000L + animation.exitMillis
            // 처음에는 보이지 않는다.
            assertTrue(animation.previewFrameAt(0L).hidden, style.name)
            // 등장이 끝나면 1초 동안 정지 상태다.
            assertStatic(animation.previewFrameAt(entranceEnd), style.name)
            assertStatic(animation.previewFrameAt(entranceEnd + 1000L), style.name)
            // 퇴장 뒤 0.3초 동안 보이지 않고, 그 다음에 처음부터 다시 재생한다.
            assertTrue(animation.previewFrameAt(exitEnd + 100L).hidden, style.name)
            assertEquals(animation.previewFrameAt(100L), animation.previewFrameAt(exitEnd + 300L + 100L), style.name)
        }
        // 퇴장 중에는 반쯤 보인다.
        val fading = TextStyleType.BOLD.look().animation.previewFrameAt(350L + 1000L + 100L).alpha
        assertTrue(fading > 0f && fading < 1f)
    }

    @Test
    fun newEntrances_startAwayAndEndInPlace() {
        fun start(entrance: TextEntrance) = TextAnimation(entrance, entranceMillis = 500, exitMillis = 200).frameAt(0.0, 3.0)
        // 떠오르기는 아래에서 투명하게 시작한다.
        assertEquals(0f, start(TextEntrance.FADE_UP).alpha)
        assertTrue(start(TextEntrance.FADE_UP).offsetY > 0f)
        // 써 내려가기는 보이는 폭 0에서 시작한다.
        assertEquals(0f, start(TextEntrance.WRITE).textReveal)
        // 도장 찍기는 크게 시작한다.
        assertEquals(1.9f, start(TextEntrance.STAMP).scale, 1e-4f)
        // 통통은 위에서 떨어진다.
        assertTrue(start(TextEntrance.BOUNCE).offsetY < 0f)
        listOf(TextEntrance.FADE_UP, TextEntrance.WRITE, TextEntrance.STAMP, TextEntrance.BOUNCE).forEach { entrance ->
            assertStatic(TextAnimation(entrance, entranceMillis = 500, exitMillis = 200).frameAt(0.5, 2.5), entrance.name)
        }
    }

    @Test
    fun minimal_usesNarrowBoxAndLeftAlignment() {
        val minimal = TextStyleType.MINIMAL.look()
        assertEquals(TextAlign.Start, minimal.textAlign)
        assertEquals(0.7f, minimal.boxWidthRatio)
        assertEquals(-4f, TextStyleType.IMPACT.look().rotation)
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
