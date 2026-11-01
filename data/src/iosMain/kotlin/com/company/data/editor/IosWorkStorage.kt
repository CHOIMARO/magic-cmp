package com.company.data.editor

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/** Application Support folder of the app. The system does not remove the files in it. */
@OptIn(ExperimentalForeignApi::class)
private fun supportFolder(name: String): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val path = "$base/$name"
    NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    return path
}

/** Keeps the work files in the Application Support folder of the app. */
@OptIn(ExperimentalForeignApi::class)
internal class IosWorkFileStore : WorkFileStore {
    private val folder: String by lazy { supportFolder(WORK_DIR) }

    override fun read(fileName: String): String? =
        NSString.stringWithContentsOfFile("$folder/$fileName", encoding = NSUTF8StringEncoding, error = null)

    @OptIn(BetaInteropApi::class)
    override fun write(fileName: String, text: String) {
        // atomically로 써서 쓰는 중에 앱이 꺼져도 이전 파일이 그대로 남게 한다.
        val written = NSString.create(string = text)
            .writeToFile("$folder/$fileName", atomically = true, encoding = NSUTF8StringEncoding, error = null)
        check(written) { "Cannot save the work file $fileName" }
    }

    override fun delete(fileName: String) {
        NSFileManager.defaultManager.removeItemAtPath("$folder/$fileName", error = null)
    }

    private companion object {
        const val WORK_DIR = "works"
    }
}

/**
 * Moves the picked photos and videos out of the temporary folder, so that the system does not remove them.
 * The files go to the Application Support folder with the same relative path.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosMediaPersister : MediaPersister {
    private val folder: String by lazy { supportFolder(MEDIA_DIR) }

    override fun persist(uri: String): String {
        val source = NSURL.URLWithString(uri)?.takeIf { it.isFileURL() }?.path ?: return uri
        val temp = NSTemporaryDirectory().trimEnd('/') + "/"
        // 임시 폴더 밖의 파일은 시스템이 지우지 않으므로 그대로 둔다.
        if (!source.startsWith(temp)) return uri
        val target = "$folder/${source.removePrefix(temp)}"
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(target)) {
            fileManager.createDirectoryAtPath(target.substringBeforeLast('/'), withIntermediateDirectories = true, attributes = null, error = null)
            if (!fileManager.copyItemAtPath(source, toPath = target, error = null)) return uri
        }
        return NSURL.fileURLWithPath(target).absoluteString ?: uri
    }

    override fun restore(uri: String): String {
        val relative = savedRelativePath(uri) ?: return uri
        // 앱을 업데이트하면 앱 폴더 경로가 바뀔 수 있으므로 지금의 폴더로 다시 만든다.
        return NSURL.fileURLWithPath("$folder/$relative").absoluteString ?: uri
    }

    override fun release(uri: String) {
        val relative = savedRelativePath(uri) ?: return
        NSFileManager.defaultManager.removeItemAtPath("$folder/$relative", error = null)
    }

    /** Path of a saved source file in the media folder, or null for a file outside of it. */
    private fun savedRelativePath(uri: String): String? {
        val path = NSURL.URLWithString(uri)?.takeIf { it.isFileURL() }?.path ?: return null
        val marker = "/$SUPPORT_DIR/$MEDIA_DIR/"
        return path.substringAfter(marker, missingDelimiterValue = "").ifEmpty { null }
    }

    private companion object {
        const val MEDIA_DIR = "media"
        const val SUPPORT_DIR = "Application Support"
    }
}
