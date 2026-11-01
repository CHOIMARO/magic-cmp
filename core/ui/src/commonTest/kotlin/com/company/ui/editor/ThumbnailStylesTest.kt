package com.company.ui.editor

import androidx.compose.ui.graphics.Color
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.ThumbnailTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThumbnailStylesTest {

    private val lines = TextStyleType.entries.filter { it.thumbnail }

    @Test
    fun thereAreFourTextTemplates_andTheLinesOfSixThumbnails() {
        assertEquals(listOf(TextStyleType.IMPACT, TextStyleType.NEON, TextStyleType.MARKER, TextStyleType.MINIMAL), TextStyleType.entries.filterNot { it.thumbnail })
        assertEquals(23, lines.size)
    }

    @Test
    fun lines_showFromTheFirstFrame_andFadeOut() {
        lines.forEach { style ->
            val animation = style.look().animation
            assertEquals(TextEntrance.NONE, animation.entrance, style.name)
            assertEquals(200, animation.exitMillis, style.name)
            assertEquals(1f, animation.frameAt(elapsed = 0.0, remaining = 3.0).alpha, style.name)
        }
    }

    @Test
    fun lines_useTheFontsOfTheDesign() {
        assertEquals(TextFontType.KUKDETOPOKKI, TextStyleType.THUMB_FIRE_1.look().font)
        assertEquals(TextFontType.GELLYROLL, TextStyleType.THUMB_BUBBLE_1.look().font)
        assertEquals(TextFontType.KKUBULIM, TextStyleType.THUMB_HAND_2.look().font)
        assertEquals(TextFontType.MONA12, TextStyleType.THUMB_PIXEL_4.look().font)
        TextStyleType.entries.forEach { assertEquals(it.font, it.look().font, it.name) }
    }

    @Test
    fun sizes_comeFromThe1080Canvas() {
        // 1080 캔버스의 168px은 무대 폭 264.375dp에서 41.125dp다.
        assertEquals(41.125f, TextStyleType.THUMB_FIRE_2.look().fontSize, 1e-3f)
        // 테두리 16px은 바깥쪽 절반 8px만 보인다.
        assertEquals(8f / 168, TextStyleType.THUMB_FIRE_2.look().outlineEm, 1e-5f)
    }

    @Test
    fun theTextOfTheDesign_fitsOnOneLine() {
        ThumbnailTemplate.entries.flatMap { it.lines }.forEach { line ->
            val look = line.style.look(decorations = line.accents)
            assertEquals(1, look.breakLines(line.text.splitWords()).lines.size, line.text)
        }
    }

    @Test
    fun outlinedLines_drawTheirShadowsUnderTheOutline_inEm() {
        val fire = TextStyleType.THUMB_FIRE_2.look()
        assertTrue(fire.shadowsUnderOutline)
        val shadow = fire.shadows.single()
        assertTrue(shadow.em)
        assertEquals(0f, shadow.blur)
        assertEquals(10f / 168, shadow.offsetX, 1e-5f)
        // 테두리가 없는 줄의 그림자는 글자 위에 그린다.
        assertTrue(!TextStyleType.THUMB_THIN_2.look().shadowsUnderOutline)
    }

    @Test
    fun boxesStickersAndTransforms_followTheDesign() {
        val fire4 = TextStyleType.THUMB_FIRE_4.look()
        assertEquals(Color.White, fire4.background)
        assertEquals(1f, fire4.lineHeightEm)
        assertEquals(listOf(StickerShape.FLAME, StickerShape.STAR, StickerShape.STAR), TextStyleType.THUMB_FIRE_1.look().stickers.map { it.shape })
        assertEquals(listOf(StickerShape.BURST), TextStyleType.THUMB_THIN_1.look().stickers.map { it.shape })
        assertEquals(listOf(StickerShape.BAR), TextStyleType.THUMB_THIN_5.look().stickers.map { it.shape })
        assertTrue(TextStyleType.THUMB_THIN_5.look().italic)
        val hand = TextStyleType.THUMB_HAND_1.look()
        assertEquals(1.45f, hand.scaleY)
        assertEquals(-4f, hand.rotation)
        assertNotNull(TextStyleType.THUMB_BUBBLE_1.look().glow)
    }

    @Test
    fun scaleAndSizeStep_keepTheEffectsInProportionToTheFont() {
        val normal = TextStyleType.THUMB_PIXEL_2.look()
        val small = TextStyleType.THUMB_PIXEL_2.look(scale = 0.5f, sizeStep = 1)
        assertEquals(normal.outlineEm, small.outlineEm)
        assertEquals(normal.stickers, small.stickers)
        assertTrue(small.fontSize < normal.fontSize)
    }

    @Test
    fun noTemplate_isWhitePretendardAt80px_withoutAnEntrance() {
        val plain = (null as TextStyleType?).look()
        assertEquals(TextFontType.PRETENDARD, plain.font)
        assertEquals(Color.White, plain.color)
        assertEquals(80 * 264.375f / 1080, plain.fontSize, 1e-3f)
        assertTrue(plain.shadows.isEmpty())
        assertEquals(TextEntrance.NONE, plain.animation.entrance)
        assertEquals(200, plain.animation.exitMillis)
        assertEquals(plain.fontSize * 1.5f, (null as TextStyleType?).look(sizeStep = 5).fontSize, 1e-3f)
    }

    @Test
    fun letterBounds_areInsideTheLine() {
        TextFontType.entries.forEach { font ->
            val (top, bottom) = font.letterBoundsEm
            assertTrue(top > 0f && bottom >= 0f && top + bottom <= 1.25f, font.name)
        }
    }
}
