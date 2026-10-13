package com.company.ui.editor

import androidx.compose.ui.graphics.Color
import com.company.core.domain.model.editor.Decoration
import com.company.core.domain.model.editor.DecorationRange
import com.company.core.domain.model.editor.TextEntrance
import com.company.core.domain.model.editor.TextExit
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextV7RulesTest {

    @Test
    fun sizeStep_multipliesTheFontSize_andChangesTheLineLength() {
        val normal = TextStyleType.BOLD.look()
        val large = TextStyleType.BOLD.look(sizeStep = 5)
        assertEquals(normal.fontSize * 1.5f, large.fontSize, 1e-4f)
        assertTrue(large.maxCharsPerLine < normal.maxCharsPerLine)
        assertEquals(normal.fontSize * 0.6f, TextStyleType.BOLD.look(sizeStep = 1).fontSize, 1e-4f)
    }

    @Test
    fun boldAndOutlineDecorations_makeTheLettersWider() {
        val decorations = listOf(DecorationRange(0, 2, Decoration(bold = true, outline = true)))
        val decorated = TextStyleType.BOLD.look(decorations = decorations)
        assertEquals(1.1f, decorated.widthFactor, 1e-4f)
        assertTrue(decorated.maxCharsPerLine <= TextStyleType.BOLD.look().maxCharsPerLine)
    }

    @Test
    fun withChoices_usesThePickedAnimations_andShortensThemForAShortText() {
        val style = TextStyleType.BOLD.look().animation
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
            id = "t", text = "a", start = 0.0, length = 3.0, style = TextStyleType.BOLD, x = 50.0, y = 50.0,
            exit = TextExit.FLY_UP, exitMillis = 600,
        )
        assertEquals(TextExit.FLY_UP, overlay.look().animation.exit)
        assertEquals(600, overlay.look().animation.exitMillis)
    }

    @Test
    fun autoTextColor_picksTheHigherContrast() {
        assertEquals(Color(0xFF111111), autoTextColor(0xFFFFFF))
        assertEquals(Color.White, autoTextColor(0x1A1A1A))
        assertEquals(Color(0xFF111111), autoTextColor(0xFDFFB6))
    }
}
