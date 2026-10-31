package com.company.feature.editor.editor

import com.company.domain.model.editor.SoundEffect
import com.company.domain.model.editor.SoundEffectCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class V8SheetTest {

    private val catalog = listOf(
        SoundEffect(id = "s1", category = SoundEffectCategory.HIGHLIGHT, name = "띠링", length = 0.6),
        SoundEffect(id = "s4", category = SoundEffectCategory.REACTION, name = "박수", length = 2.4),
        SoundEffect(id = "s7", category = SoundEffectCategory.TRANSITION, name = "휙", length = 0.4),
    )

    @Test
    fun soundEffectsOf_allTab_showsTheWholeCatalogInOrder() {
        assertEquals(catalog, soundEffectsOf(SoundEffectTab.All, catalog, recent = listOf("s7")))
    }

    @Test
    fun soundEffectsOf_recentAndCategoryTabs_filterTheCatalog() {
        val recent = soundEffectsOf(SoundEffectTab.Recent, catalog, recent = listOf("s7", "gone", "s1"))
        assertEquals(listOf("s7", "s1"), recent.map { it.id })

        val reaction = soundEffectsOf(SoundEffectTab.Category(SoundEffectCategory.REACTION), catalog, recent = emptyList())
        assertEquals(listOf("s4"), reaction.map { it.id })
    }

    @Test
    fun transitionPreviewProgress_holdsTheScenes_andRepeats() {
        assertEquals(0f, transitionPreviewProgress(clockMillis = 400, opening = false))
        assertEquals(0.5f, transitionPreviewProgress(clockMillis = 800, opening = false), 1e-4f)
        assertEquals(1f, transitionPreviewProgress(clockMillis = 1500, opening = false))
        // 1.7초마다 처음부터 다시 보여 준다.
        assertEquals(0.5f, transitionPreviewProgress(clockMillis = 1700 + 800, opening = false), 1e-4f)
        // 시작 전환은 더 일찍 시작하고 더 오래 걸린다.
        assertEquals(0.5f, transitionPreviewProgress(clockMillis = 650, opening = true), 1e-4f)
    }
}
