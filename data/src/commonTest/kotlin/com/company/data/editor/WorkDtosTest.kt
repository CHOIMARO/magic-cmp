package com.company.data.editor

import com.company.domain.model.editor.BackgroundShape
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class WorkDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun read(text: String): TextOverlay =
        json.decodeFromString<ProjectFileDto>("""{"project":{"name":"p","texts":[$text]}}""").project.toDomain().texts.single()

    @Test
    fun oldFile_keepsAKnownTemplate() {
        val text = read("""{"id":"t","text":"a","start":0,"length":1,"style":"NEON","x":50,"y":40}""")
        assertEquals(TextStyleType.NEON, text.style)
        assertNull(text.font)
        assertFalse(text.fontFromTemplate)
    }

    @Test
    fun oldFile_withARemovedThumbnail_readsAsNoTemplate_andKeepsItsFont() {
        val text = read("""{"id":"t","text":"a","start":0,"length":1,"style":"SHOUT","x":50,"y":40}""")
        assertNull(text.style)
        assertEquals(TextFontType.STRONG_ARMY, text.font)
        assertTrue(text.fontFromTemplate)
        // 직접 고른 글꼴이 있으면 그 글꼴을 쓴다.
        val picked = read("""{"id":"t","text":"a","start":0,"length":1,"style":"DASH","x":50,"y":40,"font":"JUA"}""")
        assertEquals(TextFontType.JUA, picked.font)
        assertFalse(picked.fontFromTemplate)
    }

    @Test
    fun oldDecoration_usesTheDefaultBox() {
        val text = read(
            """{"id":"t","text":"ab","start":0,"length":1,"style":"NEON","x":50,"y":40,""" +
                """"decorations":[{"start":0,"end":2,"decoration":{"background":16777215}}]}""",
        )
        val decoration = text.decorations.single().decoration
        assertEquals(BackgroundShape.ROUNDED, decoration.backgroundShape)
        assertFalse(decoration.backgroundBorder)
    }

    @Test
    fun newFields_roundTrip() {
        val text = TextOverlay(
            id = "t",
            text = "ab",
            start = 0.0,
            length = 1.0,
            style = null,
            x = 50.0,
            y = 40.0,
            font = TextFontType.DOKDO,
            fontFromTemplate = true,
            decorations = listOf(DecorationRange(0, 2, Decoration(background = 0xFFD6A5, backgroundShape = BackgroundShape.SQUARE, backgroundBorder = true))),
        )
        // 썸네일 줄의 모습과 크기 강조도 그대로 읽는다.
        val line = text.copy(
            id = "u",
            style = TextStyleType.THUMB_BUBBLE_2,
            decorations = listOf(DecorationRange(0, 1, Decoration(sizeScale = 0.65f))),
        )
        val project = EditorProject(name = "p", texts = listOf(text, line))
        val encoded = json.encodeToString(ProjectFileDto(project = project.toDto()))
        assertEquals(project, json.decodeFromString<ProjectFileDto>(encoded).project.toDomain())
    }
}
