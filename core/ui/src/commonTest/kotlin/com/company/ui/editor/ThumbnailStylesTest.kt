package com.company.ui.editor

import androidx.compose.ui.graphics.Color
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThumbnailStylesTest {

    private val thumbnailStyles = TextStyleType.entries.filter { it.thumbnail }

    @Test
    fun thereAreSixThumbnails_afterTheFourTextTemplates() {
        assertEquals(6, thumbnailStyles.size)
        assertEquals(listOf(TextStyleType.IMPACT, TextStyleType.NEON, TextStyleType.MARKER, TextStyleType.MINIMAL), TextStyleType.entries.filterNot { it.thumbnail })
    }

    @Test
    fun thumbnails_showFromTheFirstFrame_andFadeOut() {
        thumbnailStyles.forEach { style ->
            val animation = style.look().animation
            assertEquals(TextEntrance.NONE, animation.entrance, style.name)
            assertEquals(200, animation.exitMillis, style.name)
            assertEquals(1f, animation.frameAt(elapsed = 0.0, remaining = 3.0).alpha, style.name)
        }
    }

    @Test
    fun thumbnails_useTheFontsOfTheDesign() {
        assertEquals(TextFontType.JUA, TextStyleType.THUMB_FIRE.look().font)
        assertEquals(TextFontType.JUA, TextStyleType.THUMB_BUBBLE.look().font)
        assertEquals(TextFontType.DOKDO, TextStyleType.THUMB_HAND.look().font)
        assertEquals(TextFontType.PRETENDARD, TextStyleType.THUMB_THIN.look().font)
        assertEquals(TextFontType.BLACK_HAN_SANS, TextStyleType.THUMB_BOLD.look().font)
        assertEquals(TextFontType.GAEGU, TextStyleType.THUMB_PIXEL.look().font)
        TextStyleType.entries.forEach { assertEquals(it.font, it.look().font, it.name) }
    }

    @Test
    fun thumbnailSizes_comeFromThe1080Canvas() {
        // 1080 캔버스의 168px은 무대 폭 264.375dp에서 41.125dp다.
        assertEquals(41.125f, TextStyleType.THUMB_FIRE.look().fontSize, 1e-3f)
        assertEquals(75.885f, TextStyleType.THUMB_HAND.look().fontSize, 1e-2f)
    }

    @Test
    fun scaleAndSizeStep_keepTheEffectsInProportionToTheFont() {
        // 테두리와 입체 그림자는 글자 크기의 비율이다. 크기를 바꿔도 값이 그대로여야 글자와 함께 커진다.
        val normal = TextStyleType.THUMB_FIRE.look()
        val small = TextStyleType.THUMB_FIRE.look(scale = 0.5f, sizeStep = 1)
        assertEquals(normal.outlineEm, small.outlineEm)
        assertEquals(normal.extrude, small.extrude)
        assertTrue(small.fontSize < normal.fontSize)
        assertEquals(TextStyleType.THUMB_BUBBLE.look().stickers, TextStyleType.THUMB_BUBBLE.look(scale = 0.5f, sizeStep = 5).stickers)
    }

    @Test
    fun onlyBubbleAndPixel_haveStickers() {
        assertEquals(setOf(TextStyleType.THUMB_BUBBLE, TextStyleType.THUMB_PIXEL), thumbnailStyles.filter { it.look().stickers.isNotEmpty() }.toSet())
        assertTrue(TextStyleType.THUMB_BUBBLE.look().stickers.all { it.shape == StickerShape.STAR })
        assertTrue(TextStyleType.THUMB_PIXEL.look().stickers.all { it.shape == StickerShape.SPARK })
    }

    @Test
    fun hand_isTallAndTurned_andBold_hasAYellowLastWord() {
        val hand = TextStyleType.THUMB_HAND.look()
        assertEquals(1.5f, hand.scaleY)
        assertEquals(3f, hand.rotation)
        assertEquals(Color(0xFFFFF09C), TextStyleType.THUMB_BOLD.look().lastWordColor)
        (TextStyleType.entries - TextStyleType.THUMB_BOLD).forEach { assertNull(it.look().lastWordColor, it.name) }
    }

    @Test
    fun pickedFont_keepsTheEffects() {
        val look = TextStyleType.THUMB_BUBBLE.look(font = TextFontType.PRETENDARD)
        assertEquals(TextFontType.PRETENDARD, look.font)
        assertNotNull(look.glow)
        assertTrue(look.outline)
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
        // 크기 단계는 템플릿이 없어도 같은 배율을 쓴다.
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
