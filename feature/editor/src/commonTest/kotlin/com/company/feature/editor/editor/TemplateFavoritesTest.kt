package com.company.feature.editor.editor

import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateFavoritesTest {

    @Test
    fun templatesWithFavoritesFirst_putsTheNewestFavoriteFirst_andKeepsTheOtherOrder() {
        val order = templatesWithFavoritesFirst(listOf(TextStyleType.MINIMAL, TextStyleType.NEON))

        assertEquals(listOf(TextStyleType.MINIMAL, TextStyleType.NEON, TextStyleType.IMPACT, TextStyleType.MARKER), order)
    }

    @Test
    fun templatesWithFavoritesFirst_withoutFavorites_keepsTheNormalOrder() {
        assertEquals(
            listOf(TextStyleType.IMPACT, TextStyleType.NEON, TextStyleType.MARKER, TextStyleType.MINIMAL),
            templatesWithFavoritesFirst(emptyList()),
        )
    }

    @Test
    fun templatesWithFavoritesFirst_showsEachTemplateOnce_andNoThumbnailLines() {
        val order = templatesWithFavoritesFirst(listOf(TextStyleType.NEON, TextStyleType.NEON, TextStyleType.THUMB_FIRE_1))

        assertEquals(4, order.size)
        assertEquals(TextStyleType.NEON, order.first())
    }
}
