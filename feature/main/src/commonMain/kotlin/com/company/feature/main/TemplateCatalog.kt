package com.company.feature.main

import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.ThumbnailTemplate

/**
 * Templates that the template tab shows, in the order of the design: the layouts and the thumbnails. The text and the
 * caption templates are only in the text sheet and the caption sheet of the editor.
 */
internal object TemplateCatalog {
    /** All templates in grid order. The full-screen layout is the default, so it is not a template. */
    val All: List<EditorTemplate> =
        LayoutType.entries.filter { it != LayoutType.FULL }.map { EditorTemplate.Layout(it) } +
            ThumbnailTemplate.entries.map { EditorTemplate.Thumbnail(it) }

    /** Templates with a "NEW" badge, as in the design. */
    val New: Set<EditorTemplate> = setOf(
        EditorTemplate.Layout(LayoutType.BEFORE_AFTER),
        EditorTemplate.Layout(LayoutType.POLAROID),
        EditorTemplate.Thumbnail(ThumbnailTemplate.FIRE),
    )
}
