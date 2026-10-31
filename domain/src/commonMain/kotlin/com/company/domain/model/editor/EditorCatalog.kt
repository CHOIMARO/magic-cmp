package com.company.domain.model.editor

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

/**
 * Look of a text overlay. The order is the order of the template lists. A new text uses [IMPACT].
 *
 * SHOUT, FLAME, SPARK, and DASH are the thumbnail looks: heavy letters with an outline and a solid 3D shadow.
 */
enum class TextStyleType { IMPACT, NEON, MARKER, MINIMAL, SHOUT, FLAME, SPARK, DASH }

/**
 * Font of a text overlay. The downloadable fonts use the SIL Open Font License 1.1. The license notices of the fonts
 * in the app are in the `licenses` folder of core:ui.
 *
 * @property downloadable True when the app does not include the font file. The app downloads the file the first
 *   time that the user picks the font.
 * @property isNew True to show a "NEW" mark in the font list.
 */
enum class TextFontType(val downloadable: Boolean = false, val isNew: Boolean = false) {
    /** Pretendard. Most text styles use it. */
    PRETENDARD,

    /** Gowun Batang (고운바탕), a serif font. The serif styles use it. */
    GOWUN_BATANG,

    /** Nanum Pen Script (나눔손글씨 펜), a handwriting font. */
    NANUM_PEN,

    /** Black Han Sans (검은고딕), a heavy display font. */
    BLACK_HAN_SANS,

    /** Do Hyeon (도현), a retro display font. */
    DO_HYEON,

    /** Jua (주아), a round display font. */
    JUA,

    /** BM Kkubulim (꾸불림체), a wavy handwriting font of Woowa Brothers. */
    KKUBULIM(isNew = true),

    /** Griun Myoeun Heullim (그리운 묘은흘림체), a cursive handwriting font. */
    MYOEUN_HEULLIM(isNew = true),

    /** Griun Gellyroll (그리운 겔리롤), a gel pen handwriting font. */
    GELLYROLL(isNew = true),

    /** Mona12 Text KR (Mona12), a 12-pixel bitmap style font. */
    MONA12(isNew = true),

    /** Neujoh (느좋체), a narrow gothic font that comes from Pretendard. */
    NEUJOH(isNew = true),

    /** OkDanDan (Ok단단체), a heavy display font. */
    OK_DANDAN(isNew = true),

    /** Strong Army Bold (강한육군체), a narrow heavy font of the Republic of Korea Army. */
    STRONG_ARMY(isNew = true),

    /** Sandoll Kukdetopokki Bold (국대떡볶이체), a heavy round brush font. */
    KUKDETOPOKKI(isNew = true),

    /** Esamanru Bold (이사만루체), a heavy gothic font of Gong Games. */
    ESAMANRU(isNew = true),

    /** OK Gung (읏궁체), a heavy brush font of OK Holdings. */
    OK_GUNG(isNew = true),

    /** Gaegu (개구쟁이), a playful handwriting font. */
    GAEGU(downloadable = true, isNew = true),

    /** Dongle (동글), a narrow round font. */
    DONGLE(downloadable = true, isNew = true),

    /** Gugi (구기), a heavy display font. */
    GUGI(downloadable = true, isNew = true),

    /** Song Myung (송명), a serif font. */
    SONG_MYUNG(downloadable = true),
}

/** How a text comes on screen. */
enum class TextEntrance {
    /** No animation. The text shows at once ("없음"). */
    NONE,

    /** Grows from 60% with a small overshoot ("팝"). */
    POP,

    /** Moves up into place ("밀어 올리기"). */
    SLIDE_UP,

    /** Fades in ("페이드"). */
    FADE,

    /** Fades in and moves up a short distance ("떠오르기"). */
    FADE_UP,

    /** Blinks a few times ("깜빡임"). */
    FLICKER,

    /** The background draws from left to right ("형광펜 긋기"). */
    MARKER_SWEEP,

    /** The text shows from left to right, as if a pen writes it ("써 내려가기"). */
    WRITE,

    /** Shrinks from a large size into place, as a stamp ("도장 찍기"). */
    STAMP,

    /** Drops from above and bounces into place ("통통"). */
    BOUNCE,
}

/** How a text goes off screen. */
enum class TextExit {
    /** No animation. The text goes away at once ("없음"). */
    NONE,

    /** Fades out ("페이드"). It is the default. */
    FADE,

    /** Fades out and moves down ("밀어 내리기"). */
    SLIDE_DOWN,

    /** Fades out and becomes smaller ("작아지며"). */
    SHRINK,

    /** Fades out and moves up fast ("위로 날아가기"). */
    FLY_UP,

    /** Blinks a few times and goes away ("깜빡임"). */
    FLICKER,
}

/**
 * Transition between two clips, or at the start of the video.
 *
 * @property canOpen True when the video start can use the transition.
 */
enum class TransitionType(val canOpen: Boolean) {
    /** The next scene shows through the previous scene ("디졸브"). */
    DISSOLVE(canOpen = false),

    /** The scene goes to black, and the next scene comes from black ("페이드"). */
    FADE(canOpen = true),

    /** The next scene pushes the previous scene out ("밀기"). */
    PUSH(canOpen = false),

    /** The scene zooms in, and the next scene zooms out into place ("확대"). */
    ZOOM(canOpen = true),

    /** The scene blurs, and the next scene becomes sharp ("흐림"). */
    BLUR(canOpen = true),

    /** A white flash changes the scene ("플래시"). */
    FLASH(canOpen = true),

    /** The scene shakes and breaks up while it changes ("글리치"). */
    GLITCH(canOpen = false),
}

/** Side that the next scene comes from in a [TransitionType.PUSH] transition. */
enum class PushDirection {
    /** LEFT : From the left edge. */
    LEFT,

    /** RIGHT : From the right edge. It is the default. */
    RIGHT,

    /** UP : From the top edge. */
    UP,

    /** DOWN : From the bottom edge. */
    DOWN,
}

/**
 * Reading speed of a dubbing.
 *
 * @property factor Speed factor. 1 is the normal speed.
 */
enum class DubbingSpeed(val factor: Double) {
    /** SLOW : Slow reading ("느리게"). */
    SLOW(0.8),

    /** NORMAL : Normal reading ("보통"). */
    NORMAL(1.0),

    /** FAST : Fast reading ("빠르게"). */
    FAST(1.25),
}

/** Category of a sound effect. */
enum class SoundEffectCategory {
    /** HIGHLIGHT : Sounds that make a moment stand out ("강조"). */
    HIGHLIGHT,

    /** REACTION : Claps, laughs, and cheers ("리액션"). */
    REACTION,

    /** TRANSITION : Whooshes for a scene change ("전환"). */
    TRANSITION,

    /** TENSION : Sounds of surprise and tension ("놀람·긴장"). */
    TENSION,

    /** COMIC : Sounds of a funny failure ("코믹·실패"). */
    COMIC,

    /** DAILY : Sounds of daily objects ("일상·사물"). */
    DAILY,
}

/** Look of the automatic captions. */
enum class CaptionStyleType { BASIC, POP, BOX, MINIMAL, SERIF, BIG }

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
