package com.company.ui.editor

import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThumbnailStylesTest {

    private val thumbnailStyles = listOf(TextStyleType.SHOUT, TextStyleType.FLAME, TextStyleType.SPARK, TextStyleType.DASH)

    @Test
    fun thumbnailStyles_haveAnOutlineInEm_andASolid3dShadow() {
        thumbnailStyles.forEach { style ->
            val look = style.look()
            assertTrue(look.outline, style.name)
            assertTrue(look.outlineEm > 0f, style.name)
            val extrude = assertNotNull(look.extrude, style.name)
            // 입체 그림자는 아래쪽으로 나온다.
            assertTrue(extrude.y > 0f, style.name)
            assertTrue(look.shadows.isEmpty(), style.name)
        }
    }

    @Test
    fun gradientStops_startAtZero_endAtOne_andGoUp() {
        thumbnailStyles.flatMap { listOfNotNull(it.look().fill, it.look().firstWordFill) }.forEach { fill ->
            val positions = fill.stops.map { it.first }
            assertEquals(0f, positions.first())
            assertEquals(1f, positions.last())
            assertEquals(positions.sorted(), positions)
        }
    }

    @Test
    fun twoColorStyles_paintTheFirstWordDifferently() {
        assertNull(TextStyleType.SHOUT.look().fill)
        assertNull(TextStyleType.FLAME.look().firstWordFill)
        assertNotNull(TextStyleType.SPARK.look().firstWordFill)
        assertNotNull(TextStyleType.DASH.look().firstWordFill)
    }

    @Test
    fun dash_leansToTheRight_andOthersStandStraight() {
        assertTrue(TextStyleType.DASH.look().skewX < 0f)
        (TextStyleType.entries - TextStyleType.DASH).forEach { assertEquals(0f, it.look().skewX, it.name) }
    }

    @Test
    fun scaleAndSizeStep_keepTheEffectsInProportionToTheFont() {
        // 테두리와 그림자는 글자 크기의 비율이다. 크기를 바꿔도 값이 그대로여야 글자와 함께 커진다.
        val normal = TextStyleType.FLAME.look()
        val small = TextStyleType.FLAME.look(scale = 0.5f, sizeStep = 1)
        assertEquals(normal.outlineEm, small.outlineEm)
        assertEquals(normal.extrude, small.extrude)
        assertTrue(small.fontSize < normal.fontSize)
    }

    @Test
    fun thumbnailStyles_useTheFontsOfTheReference() {
        assertEquals(TextFontType.STRONG_ARMY, TextStyleType.SHOUT.look().font)
        assertEquals(TextFontType.OK_GUNG, TextStyleType.FLAME.look().font)
        assertEquals(TextFontType.KUKDETOPOKKI, TextStyleType.SPARK.look().font)
        assertEquals(TextFontType.ESAMANRU, TextStyleType.DASH.look().font)
    }

    @Test
    fun pickedFont_keepsTheEffects() {
        val look = TextStyleType.SPARK.look(font = TextFontType.PRETENDARD)
        assertEquals(TextFontType.PRETENDARD, look.font)
        assertNotNull(look.fill)
        assertNotNull(look.glow)
    }

    @Test
    fun letterBounds_areInsideTheLine() {
        TextFontType.entries.forEach { font ->
            val (top, bottom) = font.letterBoundsEm
            assertTrue(top > 0f && bottom >= 0f && top + bottom <= 1.25f, font.name)
        }
    }
}
