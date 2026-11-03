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
 * Text template: the look of a text overlay. A new text has no template.
 *
 * The first 4 entries are the text templates, in the order of the template lists. The other entries are the lines of
 * the thumbnail templates. Each line of a [ThumbnailTemplate] is one text with its own look. The lines follow the same
 * rules as the text templates (9.3, D61).
 *
 * @property font Default font of the template. A font that the user picks replaces it.
 * @property thumbnail True for a line of a thumbnail template.
 */
enum class TextStyleType(val font: TextFontType, val thumbnail: Boolean = false) {
    /** IMPACT : Yellow heavy letters with a black outline ("예능"). */
    IMPACT(TextFontType.BLACK_HAN_SANS),

    /** NEON : Pink letters with a glow ("네온"). */
    NEON(TextFontType.PRETENDARD),

    /** MARKER : Black letters on a yellow band ("형광펜"). */
    MARKER(TextFontType.PRETENDARD),

    /** MINIMAL : Small thin letters with wide spaces ("미니멀"). */
    MINIMAL(TextFontType.PRETENDARD),

    // ----- "불꽃·핑크 입체" -----

    /** THUMB_FIRE_1 : White letters with a brown outline, an orange glow, and a flame behind them. */
    THUMB_FIRE_1(TextFontType.KUKDETOPOKKI, thumbnail = true),

    /** THUMB_FIRE_2 : Large pink letters with a white outline and a solid shadow. */
    THUMB_FIRE_2(TextFontType.GELLYROLL, thumbnail = true),

    /** THUMB_FIRE_3 : Large pink letters with a white outline, a solid shadow, and a pink star. */
    THUMB_FIRE_3(TextFontType.GELLYROLL, thumbnail = true),

    /** THUMB_FIRE_4 : Small black letters on a white rounded box. */
    THUMB_FIRE_4(TextFontType.GAEGU, thumbnail = true),

    // ----- "옐로 버블" -----

    /** THUMB_BUBBLE_1 : Large yellow letters with a dark outline, a white glow, and two stars. */
    THUMB_BUBBLE_1(TextFontType.GELLYROLL, thumbnail = true),

    /** THUMB_BUBBLE_2 : Large yellow letters with a dark outline and a white glow. */
    THUMB_BUBBLE_2(TextFontType.GELLYROLL, thumbnail = true),

    /** THUMB_BUBBLE_3 : Pink letters with a white outline, a dark glow, and a star. */
    THUMB_BUBBLE_3(TextFontType.GELLYROLL, thumbnail = true),

    // ----- "손글씨 풀스크린" -----

    /** THUMB_HAND_1 : Tall yellow handwriting with a black outline, turned to the left. */
    THUMB_HAND_1(TextFontType.KKUBULIM, thumbnail = true),

    /** THUMB_HAND_2 : Tall yellow handwriting with a black outline, turned a little to the right. */
    THUMB_HAND_2(TextFontType.KKUBULIM, thumbnail = true),

    /** THUMB_HAND_3 : Tall yellow handwriting with a black outline, turned to the right. */
    THUMB_HAND_3(TextFontType.KKUBULIM, thumbnail = true),

    // ----- "얇은 고딕 미니멀" -----

    /** THUMB_THIN_1 : Small green letters on a light green box, with a white burst. */
    THUMB_THIN_1(TextFontType.PRETENDARD, thumbnail = true),

    /** THUMB_THIN_2 : White letters with a soft shadow. */
    THUMB_THIN_2(TextFontType.PRETENDARD, thumbnail = true),

    /** THUMB_THIN_3 : Large thin white letters with a soft shadow. */
    THUMB_THIN_3(TextFontType.PRETENDARD, thumbnail = true),

    /** THUMB_THIN_4 : Small white letters with a soft shadow. */
    THUMB_THIN_4(TextFontType.PRETENDARD, thumbnail = true),

    /** THUMB_THIN_5 : Small white slanted letters with a soft shadow and a white bar. */
    THUMB_THIN_5(TextFontType.PRETENDARD, thumbnail = true),

    // ----- "볼드 고딕 하이라이트" -----

    /** THUMB_BOLD_1 : Small black letters on a yellow box. */
    THUMB_BOLD_1(TextFontType.PRETENDARD, thumbnail = true),

    /** THUMB_BOLD_2 : Large white heavy letters with a dark outline and a soft shadow. */
    THUMB_BOLD_2(TextFontType.BLACK_HAN_SANS, thumbnail = true),

    /** THUMB_BOLD_3 : Large white heavy letters with a dark outline and a soft shadow. */
    THUMB_BOLD_3(TextFontType.BLACK_HAN_SANS, thumbnail = true),

    /** THUMB_BOLD_4 : Large yellow heavy letters with a dark outline and a soft shadow. */
    THUMB_BOLD_4(TextFontType.BLACK_HAN_SANS, thumbnail = true),

    // ----- "픽셀 손글씨" -----

    /** THUMB_PIXEL_1 : Small black letters on a light blue box. */
    THUMB_PIXEL_1(TextFontType.PRETENDARD, thumbnail = true),

    /** THUMB_PIXEL_2 : Cream pixel letters with a navy outline and two sparkles. */
    THUMB_PIXEL_2(TextFontType.MONA12, thumbnail = true),

    /** THUMB_PIXEL_3 : Cream pixel letters with a navy outline and two sparkles. */
    THUMB_PIXEL_3(TextFontType.MONA12, thumbnail = true),

    /** THUMB_PIXEL_4 : Blue pixel letters with a navy outline and two sparkles. */
    THUMB_PIXEL_4(TextFontType.MONA12, thumbnail = true),
}

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

    /** Dokdo (독도), a rough handwriting font. A thumbnail template uses it. */
    DOKDO(downloadable = true, isNew = true),
}

/** How a text comes on screen. */
enum class TextEntrance {
    /** No animation. The text shows at once ("없음"). */
    NONE,

    /** The letters show one by one from the first letter ("타이핑"). The length is 1 second when the user picks no length. */
    TYPING,

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

/** Movement of a text that repeats while the text is on screen (F09 loop tab). */
enum class TextLoop {
    /** NONE : No movement ("없음"). */
    NONE,

    /** WAVE : The letters go up and down one after another, as a wave ("물결치기"). */
    WAVE,

    /** SHAKE : The whole text shakes a little ("떨리기"). */
    SHAKE,

    /** FIRE : An orange glow that flickers goes up from the letters ("불꽃"). */
    FIRE,
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

    /** The next scene grows from the center and turns one and a half times ("빙글빙글"). */
    SPIN(canOpen = true),
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
 * @property factor Speed factor. 1 is the normal speed. The values are the speed presets of the server.
 */
enum class DubbingSpeed(val factor: Double) {
    /** SLOW : Slow reading ("느리게"). */
    SLOW(0.8),

    /** NORMAL : Normal reading ("보통"). */
    NORMAL(1.0),

    /** FAST : Fast reading ("빠르게"). */
    FAST(1.2),
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

/** Kind of an [EditorTemplate]. The order is the order of the categories in the template tab. */
enum class TemplateKind {
    /** LAYOUT : A layout of the clips ("레이아웃"). */
    LAYOUT,

    /** THUMBNAIL : A cover title of several lines ("썸네일"). */
    THUMBNAIL,
}

/**
 * Template that the user picks in the template tab (S02).
 *
 * A layout template changes the layout. A thumbnail template adds its title lines. The text and the caption templates
 * are not in the template tab: the user picks them in the text sheet and the caption sheet of the editor.
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
     * Thumbnail template.
     *
     * @property template Thumbnail with the lines to add.
     */
    data class Thumbnail(val template: ThumbnailTemplate) : EditorTemplate {
        override val kind = TemplateKind.THUMBNAIL
        override val key = "${kind.name}:${template.name}"
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
                TemplateKind.THUMBNAIL.name -> ThumbnailTemplate.entries.find { it.name == id }?.let(::Thumbnail)
                else -> null
            }
        }
    }
}
