package com.company.ui.editor

import androidx.compose.runtime.Composable
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.SoundEffectCategory
import com.company.domain.model.editor.TemplateKind
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextLoop
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.TransitionType
import com.company.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Default text of a new title overlay. A ViewModel reads it with a StringResolver. */
val DefaultTitleText: StringResource
    get() = Res.string.default_title_text

/** Display name of the layout. */
val LayoutType.displayName: String
    @Composable get() = stringResource(
        when (this) {
            LayoutType.FULL -> Res.string.layout_full
            LayoutType.SPLIT_VERTICAL -> Res.string.layout_split_vertical
            LayoutType.SPLIT_HORIZONTAL -> Res.string.layout_split_horizontal
            LayoutType.PICTURE_IN_PICTURE -> Res.string.layout_picture_in_picture
            LayoutType.BEFORE_AFTER -> Res.string.layout_before_after
            LayoutType.GRID_THREE -> Res.string.layout_grid_three
            LayoutType.POLAROID -> Res.string.layout_polaroid
            LayoutType.CINEMA -> Res.string.layout_cinema
        }
    )

/** Short note under the layout name, for example "클립 2개". */
val LayoutType.subLabel: String
    @Composable get() = if (this == LayoutType.FULL) {
        stringResource(Res.string.layout_sub_default)
    } else {
        stringResource(Res.string.layout_sub_clips, clipCount)
    }

/** Description of the layout in the template sheet. */
val LayoutType.description: String
    @Composable get() = stringResource(
        when (this) {
            LayoutType.FULL -> Res.string.layout_desc_full
            LayoutType.SPLIT_VERTICAL -> Res.string.layout_desc_split_vertical
            LayoutType.SPLIT_HORIZONTAL -> Res.string.layout_desc_split_horizontal
            LayoutType.PICTURE_IN_PICTURE -> Res.string.layout_desc_picture_in_picture
            LayoutType.BEFORE_AFTER -> Res.string.layout_desc_before_after
            LayoutType.GRID_THREE -> Res.string.layout_desc_grid_three
            LayoutType.POLAROID -> Res.string.layout_desc_polaroid
            LayoutType.CINEMA -> Res.string.layout_desc_cinema
        }
    )

/** Display name of the text style. */
val TextStyleType.displayName: String
    @Composable get() = stringResource(
        when (this) {
            TextStyleType.IMPACT -> Res.string.text_style_impact
            TextStyleType.NEON -> Res.string.text_style_neon
            TextStyleType.MARKER -> Res.string.text_style_marker
            TextStyleType.MINIMAL -> Res.string.text_style_minimal
            TextStyleType.SHOUT -> Res.string.text_style_shout
            TextStyleType.FLAME -> Res.string.text_style_flame
            TextStyleType.SPARK -> Res.string.text_style_spark
            TextStyleType.DASH -> Res.string.text_style_dash
        }
    )

/** Display name of the font. */
val TextFontType.displayName: String
    @Composable get() = stringResource(
        when (this) {
            TextFontType.PRETENDARD -> Res.string.font_pretendard
            TextFontType.GOWUN_BATANG -> Res.string.font_gowun_batang
            TextFontType.NANUM_PEN -> Res.string.font_nanum_pen
            TextFontType.BLACK_HAN_SANS -> Res.string.font_black_han_sans
            TextFontType.DO_HYEON -> Res.string.font_do_hyeon
            TextFontType.JUA -> Res.string.font_jua
            TextFontType.KKUBULIM -> Res.string.font_kkubulim
            TextFontType.MYOEUN_HEULLIM -> Res.string.font_myoeun_heullim
            TextFontType.GELLYROLL -> Res.string.font_gellyroll
            TextFontType.MONA12 -> Res.string.font_mona12
            TextFontType.NEUJOH -> Res.string.font_neujoh
            TextFontType.OK_DANDAN -> Res.string.font_ok_dandan
            TextFontType.STRONG_ARMY -> Res.string.font_strong_army
            TextFontType.KUKDETOPOKKI -> Res.string.font_kukdetopokki
            TextFontType.ESAMANRU -> Res.string.font_esamanru
            TextFontType.OK_GUNG -> Res.string.font_ok_gung
            TextFontType.GAEGU -> Res.string.font_gaegu
            TextFontType.DONGLE -> Res.string.font_dongle
            TextFontType.GUGI -> Res.string.font_gugi
            TextFontType.SONG_MYUNG -> Res.string.font_song_myung
        }
    )

/** Display name of the entrance animation. */
val TextEntrance.displayName: String
    @Composable get() = stringResource(
        when (this) {
            TextEntrance.NONE -> Res.string.entrance_none
            TextEntrance.TYPING -> Res.string.entrance_typing
            TextEntrance.POP -> Res.string.entrance_pop
            TextEntrance.SLIDE_UP -> Res.string.entrance_slide_up
            TextEntrance.FADE -> Res.string.entrance_fade
            TextEntrance.FADE_UP -> Res.string.entrance_fade_up
            TextEntrance.FLICKER -> Res.string.entrance_flicker
            TextEntrance.MARKER_SWEEP -> Res.string.entrance_marker_sweep
            TextEntrance.WRITE -> Res.string.entrance_write
            TextEntrance.STAMP -> Res.string.entrance_stamp
            TextEntrance.BOUNCE -> Res.string.entrance_bounce
        }
    )

/** Display name of the loop animation. */
val TextLoop.displayName: String
    @Composable get() = stringResource(
        when (this) {
            TextLoop.NONE -> Res.string.loop_none
            TextLoop.WAVE -> Res.string.loop_wave
            TextLoop.SHAKE -> Res.string.loop_shake
            TextLoop.FIRE -> Res.string.loop_fire
        }
    )

/** Display name of the exit animation. */
val TextExit.displayName: String
    @Composable get() = stringResource(
        when (this) {
            TextExit.NONE -> Res.string.exit_none
            TextExit.FADE -> Res.string.exit_fade
            TextExit.SLIDE_DOWN -> Res.string.exit_slide_down
            TextExit.SHRINK -> Res.string.exit_shrink
            TextExit.FLY_UP -> Res.string.exit_fly_up
            TextExit.FLICKER -> Res.string.exit_flicker
        }
    )

/** Display name of the transition. */
val TransitionType.displayName: String
    @Composable get() = stringResource(
        when (this) {
            TransitionType.DISSOLVE -> Res.string.transition_dissolve
            TransitionType.FADE -> Res.string.transition_fade
            TransitionType.PUSH -> Res.string.transition_push
            TransitionType.ZOOM -> Res.string.transition_zoom
            TransitionType.BLUR -> Res.string.transition_blur
            TransitionType.FLASH -> Res.string.transition_flash
            TransitionType.GLITCH -> Res.string.transition_glitch
            TransitionType.SPIN -> Res.string.transition_spin
        }
    )

/** Display name of the side that a push transition comes from. */
val PushDirection.displayName: String
    @Composable get() = stringResource(
        when (this) {
            PushDirection.LEFT -> Res.string.push_from_left
            PushDirection.RIGHT -> Res.string.push_from_right
            PushDirection.UP -> Res.string.push_from_up
            PushDirection.DOWN -> Res.string.push_from_down
        }
    )

/** Display name of the sound effect category. */
val SoundEffectCategory.displayName: String
    @Composable get() = stringResource(
        when (this) {
            SoundEffectCategory.HIGHLIGHT -> Res.string.sound_category_highlight
            SoundEffectCategory.REACTION -> Res.string.sound_category_reaction
            SoundEffectCategory.TRANSITION -> Res.string.sound_category_transition
            SoundEffectCategory.TENSION -> Res.string.sound_category_tension
            SoundEffectCategory.COMIC -> Res.string.sound_category_comic
            SoundEffectCategory.DAILY -> Res.string.sound_category_daily
        }
    )

/** Display name of the dubbing speed. */
val DubbingSpeed.displayName: String
    @Composable get() = stringResource(
        when (this) {
            DubbingSpeed.SLOW -> Res.string.dubbing_speed_slow
            DubbingSpeed.NORMAL -> Res.string.dubbing_speed_normal
            DubbingSpeed.FAST -> Res.string.dubbing_speed_fast
        }
    )

/** Display name of the caption style. */
val CaptionStyleType.displayName: String
    @Composable get() = stringResource(
        when (this) {
            CaptionStyleType.BASIC -> Res.string.caption_style_basic
            CaptionStyleType.POP -> Res.string.caption_style_pop
            CaptionStyleType.BOX -> Res.string.caption_style_box
            CaptionStyleType.MINIMAL -> Res.string.caption_style_minimal
            CaptionStyleType.SERIF -> Res.string.caption_style_serif
            CaptionStyleType.BIG -> Res.string.caption_style_big
        }
    )

/** Label of the template kind in the template sheet. */
val TemplateKind.sheetLabel: String
    @Composable get() = stringResource(
        when (this) {
            TemplateKind.LAYOUT -> Res.string.template_kind_layout
            TemplateKind.TEXT -> Res.string.template_kind_text
            TemplateKind.CAPTION -> Res.string.template_kind_caption
        }
    )

/** Display name of the template. */
val EditorTemplate.displayName: String
    @Composable get() = when (this) {
        is EditorTemplate.Layout -> layout.displayName
        is EditorTemplate.Text -> style.displayName
        is EditorTemplate.Caption -> style.displayName
    }

/** Short note under the template name in the template grid. */
val EditorTemplate.subLabel: String
    @Composable get() = when (this) {
        is EditorTemplate.Layout -> stringResource(Res.string.template_sub_layout, layout.subLabel)
        is EditorTemplate.Text -> stringResource(Res.string.template_sub_text)
        is EditorTemplate.Caption -> stringResource(Res.string.template_sub_caption)
    }

/** Description of the template in the template sheet. */
val EditorTemplate.description: String
    @Composable get() = when (this) {
        is EditorTemplate.Layout -> layout.description
        is EditorTemplate.Text -> stringResource(Res.string.template_desc_text)
        is EditorTemplate.Caption -> stringResource(Res.string.template_desc_caption)
    }

/**
 * Formats seconds as "m:ss.d" with tenths of a second. The timeline shows it while the user drags it.
 *
 * @param seconds Time in seconds. Negative values show as 0:00.0.
 */
fun formatTimeTenths(seconds: Double): String {
    val tenths = (seconds.coerceAtLeast(0.0) * 10).toLong()
    val minutes = tenths / 600
    val rest = (tenths / 10) % 60
    return "$minutes:${rest.toString().padStart(2, '0')}.${tenths % 10}"
}

/**
 * Formats seconds as "m:ss".
 *
 * @param seconds Time in seconds. Negative values show as 0:00.
 */
fun formatTime(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toInt()
    val minutes = total / 60
    val rest = total % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}
