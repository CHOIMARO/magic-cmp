package com.company.ui.editor

import androidx.compose.ui.graphics.Color
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextLoop
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextV7RulesTest {

    @Test
    fun sizeStep_multipliesTheFontSize_andChangesTheLineLength() {
        val normal = TextStyleType.NEON.look()
        val large = TextStyleType.NEON.look(sizeStep = 5)
        assertEquals(normal.fontSize * 1.5f, large.fontSize, 1e-4f)
        assertTrue(large.maxCharsPerLine < normal.maxCharsPerLine)
        assertEquals(normal.fontSize * 0.6f, TextStyleType.NEON.look(sizeStep = 1).fontSize, 1e-4f)
    }

    @Test
    fun boldAndOutlineDecorations_makeTheLettersWider() {
        val decorations = listOf(DecorationRange(0, 2, Decoration(bold = true, outline = true)))
        val decorated = TextStyleType.NEON.look(decorations = decorations)
        assertEquals(1.1f, decorated.widthFactor, 1e-4f)
        assertTrue(decorated.maxCharsPerLine <= TextStyleType.NEON.look().maxCharsPerLine)
    }

    @Test
    fun withChoices_usesThePickedAnimations_andShortensThemForAShortText() {
        val style = TextStyleType.NEON.look().animation
        val picked = style.withChoices(TextEntrance.FADE, 800, TextExit.SHRINK, 400, lengthSeconds = 3.0)
        assertEquals(TextEntrance.FADE, picked.entrance)
        assertEquals(800, picked.entranceMillis)
        assertEquals(TextExit.SHRINK, picked.exit)
        val short = style.withChoices(null, 800, null, 200, lengthSeconds = 0.5)
        assertEquals(500, short.entranceMillis + short.exitMillis)
        val none = style.withChoices(TextEntrance.NONE, null, TextExit.NONE, null, lengthSeconds = 3.0)
        assertEquals(TextFrame.Static, none.frameAt(0.0, 3.0))
        assertEquals(TextFrame.Static, none.frameAt(2.99, 0.01))
    }

    @Test
    fun typingEntrance_showsTheCharactersInOrder_andTakesOneSecondByDefault() {
        val style = TextStyleType.NEON.look().animation
        val typing = style.withChoices(TextEntrance.TYPING, null, null, null, lengthSeconds = 3.0)
        assertEquals(1000, typing.entranceMillis)
        assertEquals(0f, typing.frameAt(0.0, 3.0).charReveal, 1e-4f)
        assertEquals(0.5f, typing.frameAt(0.5, 2.5).charReveal, 1e-4f)
        assertEquals(1f, typing.frameAt(0.5, 2.5).alpha, 1e-4f)
        assertEquals(TextFrame.Static, typing.frameAt(1.5, 1.5))
        // 사용자가 고른 길이가 기본 길이보다 먼저다.
        assertEquals(600, style.withChoices(TextEntrance.TYPING, 600, null, null, lengthSeconds = 3.0).entranceMillis)
        // 다른 등장은 스타일의 길이를 그대로 쓴다.
        assertEquals(700, style.withChoices(TextEntrance.FADE, null, null, null, lengthSeconds = 3.0).entranceMillis)
    }

    @Test
    fun loop_movesTheTextDuringTheWholeTime() {
        val style = TextStyleType.NEON.look().animation
        val wave = style.withChoices(null, null, null, null, lengthSeconds = 3.0, loop = TextLoop.WAVE)
        val middle = wave.frameAt(1.5, 1.5)
        assertEquals(TextLoop.WAVE, middle.loop)
        assertEquals(1.5f, middle.loopTime, 1e-4f)
        assertEquals(listOf(0.0..3.0), wave.animatedRanges(3.0))
        // 반복이 없으면 등장과 퇴장 사이에는 모양이 바뀌지 않는다.
        val still = style.withChoices(null, null, null, null, lengthSeconds = 3.0)
        assertEquals(TextFrame.Static, still.frameAt(1.5, 1.5))
        assertEquals(2, still.animatedRanges(3.0).size)
    }

    @Test
    fun overlayLook_appliesTheLoop() {
        val overlay = TextOverlay(
            id = "t", text = "a", start = 0.0, length = 3.0, style = TextStyleType.NEON, x = 50.0, y = 50.0,
            loop = TextLoop.SHAKE,
        )
        assertEquals(TextLoop.SHAKE, overlay.look().animation.loop)
        assertEquals(TextLoop.NONE, overlay.copy(loop = null).look().animation.loop)
    }

    @Test
    fun exits_endInvisible() {
        TextExit.entries.filter { it != TextExit.NONE }.forEach { exit ->
            val animation = TextAnimation(TextEntrance.NONE, 0, 300, exit)
            assertEquals(0f, animation.frameAt(3.0, 0.0).alpha, 1e-4f, exit.name)
            assertEquals(1f, animation.frameAt(1.0, 2.0).alpha, 1e-4f, exit.name)
        }
        assertTrue(TextAnimation(TextEntrance.NONE, 0, 300, TextExit.FLY_UP).frameAt(3.0, 0.1).offsetY < 0f)
        assertTrue(TextAnimation(TextEntrance.NONE, 0, 300, TextExit.SHRINK).frameAt(3.0, 0.1).scale < 1f)
    }

    @Test
    fun overlayLook_appliesTheAnimationChoices() {
        val overlay = TextOverlay(
            id = "t", text = "a", start = 0.0, length = 3.0, style = TextStyleType.NEON, x = 50.0, y = 50.0,
            exit = TextExit.FLY_UP, exitMillis = 600,
        )
        assertEquals(TextExit.FLY_UP, overlay.look().animation.exit)
        assertEquals(600, overlay.look().animation.exitMillis)
    }

    @Test
    fun everyFont_hasALineLimit_andNarrowFontsFitMoreLetters() {
        TextFontType.entries.forEach { font ->
            assertTrue(TextStyleType.NEON.look(font = font).maxCharsPerLine >= 1, font.name)
        }
        val pretendard = TextStyleType.NEON.look().maxCharsPerLine
        assertTrue(TextStyleType.NEON.look(font = TextFontType.NEUJOH).maxCharsPerLine > pretendard)
        assertTrue(TextStyleType.NEON.look(font = TextFontType.MONA12).maxCharsPerLine < pretendard)
    }

    @Test
    fun autoTextColor_picksTheHigherContrast() {
        assertEquals(Color(0xFF111111), autoTextColor(0xFFFFFF))
        assertEquals(Color.White, autoTextColor(0x1A1A1A))
        assertEquals(Color(0xFF111111), autoTextColor(0xFDFFB6))
    }
}
