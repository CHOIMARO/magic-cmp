package com.company.domain.repository

import com.company.domain.model.editor.TextFontType
import kotlinx.coroutines.flow.StateFlow

/**
 * Font files on the device.
 *
 * @property regular Path of the regular weight file.
 * @property bold Path of the bold weight file, or null when the font has no bold weight.
 */
data class FontFiles(val regular: String, val bold: String?)

/** Downloads the fonts that the app does not include, and keeps them on the device. */
interface FontRepository {
    /** Downloaded fonts and their files. */
    val downloadedFonts: StateFlow<Map<TextFontType, FontFiles>>

    /**
     * Downloads a font. A font that is on the device returns at once.
     *
     * @param font Font with [TextFontType.downloadable].
     * @return The files of the font.
     * @throws Exception when the download fails.
     */
    suspend fun download(font: TextFontType): FontFiles
}
