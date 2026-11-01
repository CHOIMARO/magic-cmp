package com.company.feature.main

import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.TextStyleType

/**
 * Templates that the template tab shows (S02): the layouts and the thumbnails. The text and the caption templates are
 * only in the text sheet and the caption sheet of the editor.
 */
internal object TemplateCatalog {
    /** All templates in grid order. The full-screen layout is the default, so it is not a template. */
    val All: List<EditorTemplate> =
        LayoutType.entries.filter { it != LayoutType.FULL }.map { EditorTemplate.Layout(it) } +
            TextStyleType.entries.filter { it.thumbnail }.map { EditorTemplate.Thumbnail(it) }

    /** Templates with a "NEW" badge. The first thumbnail is new, as in the design. */
    val New: Set<EditorTemplate> = setOf(
        EditorTemplate.Layout(LayoutType.BEFORE_AFTER),
        EditorTemplate.Layout(LayoutType.POLAROID),
        EditorTemplate.Thumbnail(TextStyleType.THUMB_FIRE),
    )
}
