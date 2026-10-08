package com.company.feature.main

import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextStyleType

/** Templates that the template tab and the home tab show. */
internal object TemplateCatalog {
    /** All templates in grid order. The full-screen layout is the default, so it is not a template. */
    val All: List<EditorTemplate> =
        LayoutType.entries.filter { it != LayoutType.FULL }.map { EditorTemplate.Layout(it) } +
            TextStyleType.entries.map { EditorTemplate.Text(it) } +
            CaptionStyleType.entries.map { EditorTemplate.Caption(it) }

    /** Shortcuts on the home tab. */
    val Home: List<EditorTemplate> = listOf(
        EditorTemplate.Layout(LayoutType.BEFORE_AFTER),
        EditorTemplate.Caption(CaptionStyleType.POP),
        EditorTemplate.Layout(LayoutType.SPLIT_VERTICAL),
        EditorTemplate.Text(TextStyleType.SERIF),
        EditorTemplate.Layout(LayoutType.POLAROID),
    )

    /** Templates with a "NEW" badge. */
    val New: Set<EditorTemplate> = setOf(
        EditorTemplate.Layout(LayoutType.BEFORE_AFTER),
        EditorTemplate.Layout(LayoutType.POLAROID),
        EditorTemplate.Text(TextStyleType.NEON),
        EditorTemplate.Caption(CaptionStyleType.BIG),
    )
}
