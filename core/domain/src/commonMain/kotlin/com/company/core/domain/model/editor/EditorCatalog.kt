package com.company.core.domain.model.editor

/**
 * Screen layout of the clips.
 *
 * @property clipCount Number of clips that fit the layout best.
 */
enum class LayoutType(val clipCount: Int) {
    /** One clip fills the screen. */
    FULL(1),

    /** Two clips, one above the other. */
    SPLIT_VERTICAL(2),

    /** Two clips side by side. */
    SPLIT_HORIZONTAL(2),

    /** One small clip on top of a full-screen clip. */
    PICTURE_IN_PICTURE(2),

    /** Two clips side by side with "before" and "after" labels. */
    BEFORE_AFTER(2),

    /** Three clips stacked vertically. */
    GRID_THREE(3),

    /** One clip in a photo frame with space for text. */
    POLAROID(1),

    /** One clip with black bars at the top and bottom. */
    CINEMA(1),
}

/** Look of a text overlay. */
enum class TextStyleType { BOLD, BOX, OUTLINE, SERIF, NEON, MARKER, TAG }

/** Look of the automatic captions. */
enum class CaptionStyleType { BASIC, POP, BOX, MINIMAL, SERIF, BIG }

/** Color filter for the video. */
enum class FilterType { NONE, WARM, FADE, COOL, PUNCH, CINE, MONO }

/**
 * Preset that changes the filter, the music, the text style, and the caption style together.
 *
 * @property filter Filter of the preset.
 * @property musicId Music of the preset. Null keeps the current music.
 * @property textStyle Text style of the preset. Null keeps the current styles.
 * @property captionStyle Caption style of the preset. Null keeps the current style.
 */
enum class Vibe(
    val filter: FilterType,
    val musicId: String?,
    val textStyle: TextStyleType?,
    val captionStyle: CaptionStyleType?,
) {
    /** Warm colors with acoustic music. */
    WARM(FilterType.WARM, "m5", TextStyleType.BOX, CaptionStyleType.POP),

    /** Faded film look with lo-fi music. */
    DREAMY(FilterType.FADE, "m4", TextStyleType.SERIF, CaptionStyleType.SERIF),

    /** Strong colors with upbeat music. */
    HYPE(FilterType.PUNCH, "m3", TextStyleType.BOLD, CaptionStyleType.BIG),

    /** Movie look with dreamy music. */
    CINEMATIC(FilterType.CINE, "m1", TextStyleType.OUTLINE, CaptionStyleType.MINIMAL),

    /** Cool colors with wave sounds. */
    FRESH(FilterType.COOL, "m2", TextStyleType.MARKER, CaptionStyleType.BOX),

    /** Original video without effects. */
    NONE(FilterType.NONE, null, null, null),
}

/** Kind of an [EditorTemplate]. */
enum class TemplateKind { LAYOUT, TEXT, CAPTION }

/**
 * Template that the user picks in the template tab.
 *
 * A template changes one part of the project: the layout, the text style, or the caption style.
 */
sealed interface EditorTemplate {
    /** Kind of the template. */
    val kind: TemplateKind

    /** Stable key, for example "LAYOUT:POLAROID". Navigation arguments use it. */
    val key: String

    /**
     * Layout template.
     *
     * @property layout Layout to apply.
     */
    data class Layout(val layout: LayoutType) : EditorTemplate {
        override val kind = TemplateKind.LAYOUT
        override val key = "${kind.name}:${layout.name}"
    }

    /**
     * Text template.
     *
     * @property style Text style to apply.
     */
    data class Text(val style: TextStyleType) : EditorTemplate {
        override val kind = TemplateKind.TEXT
        override val key = "${kind.name}:${style.name}"
    }

    /**
     * Caption template.
     *
     * @property style Caption style to apply.
     */
    data class Caption(val style: CaptionStyleType) : EditorTemplate {
        override val kind = TemplateKind.CAPTION
        override val key = "${kind.name}:${style.name}"
    }

    companion object {
        /**
         * Reads a template from its [key].
         *
         * @param key Key that [EditorTemplate.key] made.
         * @return The template, or null if the key is not valid.
         */
        fun fromKey(key: String?): EditorTemplate? {
            val parts = key?.split(":") ?: return null
            if (parts.size != 2) return null
            val (kind, id) = parts
            return when (kind) {
                TemplateKind.LAYOUT.name -> LayoutType.entries.find { it.name == id }?.let(::Layout)
                TemplateKind.TEXT.name -> TextStyleType.entries.find { it.name == id }?.let(::Text)
                TemplateKind.CAPTION.name -> CaptionStyleType.entries.find { it.name == id }?.let(::Caption)
                else -> null
            }
        }
    }
}
