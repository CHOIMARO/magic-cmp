package com.company.feature.main

import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.TextStyleType

/** Templates that the template tab shows. */
internal object TemplateCatalog {
    /** All templates in grid order. The full-screen layout is the default, so it is not a template. */
    val All: List<EditorTemplate> =
        LayoutType.entries.filter { it != LayoutType.FULL }.map { EditorTemplate.Layout(it) } +
            TextStyleType.entries.map { EditorTemplate.Text(it) } +
            CaptionStyleType.entries.map { EditorTemplate.Caption(it) }

    /** Templates with a "NEW" badge. */
    val New: Set<EditorTemplate> = setOf(
        EditorTemplate.Layout(LayoutType.BEFORE_AFTER),
        EditorTemplate.Layout(LayoutType.POLAROID),
        EditorTemplate.Text(TextStyleType.NEON),
        EditorTemplate.Text(TextStyleType.SHOUT),
        EditorTemplate.Text(TextStyleType.FLAME),
        EditorTemplate.Text(TextStyleType.SPARK),
        EditorTemplate.Text(TextStyleType.DASH),
        EditorTemplate.Caption(CaptionStyleType.BIG),
    )
}
