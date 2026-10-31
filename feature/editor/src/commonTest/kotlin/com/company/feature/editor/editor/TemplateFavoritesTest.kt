package com.company.feature.editor.editor

import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateFavoritesTest {

    @Test
    fun templatesWithFavoritesFirst_putsTheNewestFavoriteFirst_andKeepsTheOtherOrder() {
        val order = templatesWithFavoritesFirst(listOf(TextStyleType.DASH, TextStyleType.NEON))

        assertEquals(
            listOf(
                TextStyleType.DASH,
                TextStyleType.NEON,
                TextStyleType.IMPACT,
                TextStyleType.MARKER,
                TextStyleType.MINIMAL,
                TextStyleType.SHOUT,
                TextStyleType.FLAME,
                TextStyleType.SPARK,
            ),
            order,
        )
    }

    @Test
    fun templatesWithFavoritesFirst_withoutFavorites_keepsTheNormalOrder() {
        assertEquals(TextStyleType.entries, templatesWithFavoritesFirst(emptyList()))
    }

    @Test
    fun templatesWithFavoritesFirst_showsEachTemplateOnce() {
        val order = templatesWithFavoritesFirst(listOf(TextStyleType.SHOUT, TextStyleType.SHOUT))

        assertEquals(TextStyleType.entries.size, order.size)
        assertEquals(TextStyleType.SHOUT, order.first())
    }
}
