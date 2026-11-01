package com.company.feature.main

import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.ThumbnailTemplate

/** Templates that the template tab shows, in the order of the design. */
internal object TemplateCatalog {
    /**
     * All templates in grid order: layouts, text templates, caption styles, and thumbnails. The full-screen layout is
     * the default, so it is not a template.
     */
    val All: List<EditorTemplate> =
        LayoutType.entries.filter { it != LayoutType.FULL }.map { EditorTemplate.Layout(it) } +
            TextStyleType.entries.filterNot { it.thumbnail }.map { EditorTemplate.Text(it) } +
            CaptionStyleType.entries.map { EditorTemplate.Caption(it) } +
            ThumbnailTemplate.entries.map { EditorTemplate.Thumbnail(it) }

    /** Templates with a "NEW" badge, as in the design. */
    val New: Set<EditorTemplate> = setOf(
        EditorTemplate.Layout(LayoutType.BEFORE_AFTER),
        EditorTemplate.Layout(LayoutType.POLAROID),
        EditorTemplate.Text(TextStyleType.NEON),
        EditorTemplate.Caption(CaptionStyleType.BIG),
        EditorTemplate.Thumbnail(ThumbnailTemplate.FIRE),
    )
}
