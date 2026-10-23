package com.company.data.font

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/** Keeps the font files in the Application Support folder of the app. The system does not remove them. */
@OptIn(ExperimentalForeignApi::class)
internal class IosFontFileStore : FontFileStore {
    private val folder: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        val path = "$base/$FONT_DIR"
        NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        path
    }

    override fun path(fileName: String): String = "$folder/${fileName.replace('/', '_')}"

    override fun exists(fileName: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path(fileName))

    @OptIn(BetaInteropApi::class)
    override fun write(fileName: String, bytes: ByteArray) {
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        // atomically로 써서 쓰는 중에 앱이 꺼져도 반쯤 쓴 파일이 남지 않게 한다.
        check(data.writeToFile(path(fileName), atomically = true)) { "Cannot save the font file $fileName" }
    }

    private companion object {
        const val FONT_DIR = "fonts"
    }
}
