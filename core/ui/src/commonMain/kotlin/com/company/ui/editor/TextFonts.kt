package com.company.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.repository.FontFiles
import com.company.ui.resources.Res
import com.company.ui.resources.black_han_sans
import com.company.ui.resources.do_hyeon
import com.company.ui.resources.gowun_batang_bold
import com.company.ui.resources.jua
import com.company.ui.resources.nanum_pen_script
import org.jetbrains.compose.resources.Font

/**
 * Font files that the app downloaded. The app root provides them, so the stage and the export can draw the
 * downloaded fonts.
 */
val LocalDownloadedFonts = compositionLocalOf<Map<TextFontType, FontFiles>> { emptyMap() }

/**
 * Returns the font family of an overlay font.
 *
 * Pretendard comes from [MagicTheme]. The fonts of the app come from the font files of this module, and a file loads
 * only when a text uses it. A downloadable font comes from its downloaded files. When the files are not on the
 * device, the text uses Pretendard.
 */
@Composable
fun TextFontType.fontFamily(): FontFamily = when (this) {
    TextFontType.PRETENDARD -> MagicTheme.fonts.sans
    TextFontType.GOWUN_BATANG -> FontFamily(Font(Res.font.gowun_batang_bold, weight = FontWeight.Bold))
    TextFontType.NANUM_PEN -> FontFamily(Font(Res.font.nanum_pen_script))
    TextFontType.BLACK_HAN_SANS -> FontFamily(Font(Res.font.black_han_sans))
    TextFontType.DO_HYEON -> FontFamily(Font(Res.font.do_hyeon))
    TextFontType.JUA -> FontFamily(Font(Res.font.jua))
    TextFontType.GAEGU, TextFontType.DONGLE, TextFontType.GUGI, TextFontType.SONG_MYUNG -> downloadedFontFamily(this)
}

/** True when the font can draw text now: it is in the app, or its files are on the device. */
@Composable
fun TextFontType.isAvailable(): Boolean = !downloadable || LocalDownloadedFonts.current.containsKey(this)

@Composable
private fun downloadedFontFamily(font: TextFontType): FontFamily {
    val files = LocalDownloadedFonts.current[font]
    val family = remember(files) {
        files?.let {
            runCatching {
                FontFamily(listOfNotNull(fileFont(it.regular, FontWeight.Normal), it.bold?.let { bold -> fileFont(bold, FontWeight.Bold) }))
            }.getOrNull()
        }
    }
    return family ?: MagicTheme.fonts.sans
}

/**
 * Makes a font from a font file on the device.
 *
 * @param path Path of the file.
 * @param weight Weight of the file.
 */
internal expect fun fileFont(path: String, weight: FontWeight): Font
