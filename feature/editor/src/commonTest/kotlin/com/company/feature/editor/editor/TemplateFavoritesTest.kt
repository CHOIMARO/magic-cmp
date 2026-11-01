package com.company.feature.editor.editor

import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateFavoritesTest {

    @Test
    fun templatesWithFavoritesFirst_putsTheNewestFavoriteFirst_andKeepsTheOtherOrder() {
        val favorites = listOf(TextStyleType.THUMB_PIXEL, TextStyleType.NEON)

        assertEquals(
            listOf(TextStyleType.NEON, TextStyleType.IMPACT, TextStyleType.MARKER, TextStyleType.MINIMAL),
            templatesWithFavoritesFirst(favorites, thumbnail = false),
        )
        assertEquals(
            listOf(
                TextStyleType.THUMB_PIXEL,
                TextStyleType.THUMB_FIRE,
                TextStyleType.THUMB_BUBBLE,
                TextStyleType.THUMB_HAND,
                TextStyleType.THUMB_THIN,
                TextStyleType.THUMB_BOLD,
            ),
            templatesWithFavoritesFirst(favorites, thumbnail = true),
        )
    }

    @Test
    fun templatesWithFavoritesFirst_withoutFavorites_keepsTheNormalOrder() {
        assertEquals(TextStyleType.entries.filterNot { it.thumbnail }, templatesWithFavoritesFirst(emptyList(), thumbnail = false))
        assertEquals(TextStyleType.entries.filter { it.thumbnail }, templatesWithFavoritesFirst(emptyList(), thumbnail = true))
    }

    @Test
    fun templatesWithFavoritesFirst_showsEachTemplateOnce() {
        val order = templatesWithFavoritesFirst(listOf(TextStyleType.THUMB_HAND, TextStyleType.THUMB_HAND), thumbnail = true)

        assertEquals(6, order.size)
        assertEquals(TextStyleType.THUMB_HAND, order.first())
    }
}
