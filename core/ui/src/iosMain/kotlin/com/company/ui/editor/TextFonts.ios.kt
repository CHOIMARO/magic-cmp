package com.company.ui.editor

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual fun fileFont(path: String, weight: FontWeight): Font {
    val data = checkNotNull(NSData.dataWithContentsOfFile(path)) { "Cannot read the font file $path" }
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return androidx.compose.ui.text.platform.Font(identity = path, data = bytes, weight = weight)
}
