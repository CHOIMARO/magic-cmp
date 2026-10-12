package com.company.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.TextFontType
import com.company.ui.resources.Res
import com.company.ui.resources.black_han_sans
import com.company.ui.resources.do_hyeon
import com.company.ui.resources.gowun_batang_bold
import com.company.ui.resources.jua
import com.company.ui.resources.nanum_pen_script
import org.jetbrains.compose.resources.Font

/**
 * Returns the font family of an overlay font.
 *
 * Pretendard comes from [MagicTheme]. The other fonts come from the font files of this module.
 * A font file loads only when a text uses it.
 */
@Composable
fun TextFontType.fontFamily(): FontFamily = when (this) {
    TextFontType.PRETENDARD -> MagicTheme.fonts.sans
    TextFontType.GOWUN_BATANG -> FontFamily(Font(Res.font.gowun_batang_bold, weight = FontWeight.Bold))
    TextFontType.NANUM_PEN -> FontFamily(Font(Res.font.nanum_pen_script))
    TextFontType.BLACK_HAN_SANS -> FontFamily(Font(Res.font.black_han_sans))
    TextFontType.DO_HYEON -> FontFamily(Font(Res.font.do_hyeon))
    TextFontType.JUA -> FontFamily(Font(Res.font.jua))
}
