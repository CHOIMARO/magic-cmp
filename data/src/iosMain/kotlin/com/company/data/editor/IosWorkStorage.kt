package com.company.data.editor

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSURLVolumeAvailableCapacityForImportantUsageKey
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

/**
 * Application Support folder of the app. The system does not remove the files in it. The folder is left out of the
 * iCloud backup, because the copies of the videos are large (D51).
 */
@OptIn(ExperimentalForeignApi::class)
private fun supportFolder(name: String): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
    val path = "$base/$name"
    NSFileManager.defaultManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    NSURL.fileURLWithPath(path).setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = null)
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

    override fun list(): List<String> =
        NSFileManager.defaultManager.contentsOfDirectoryAtPath(folder, error = null)?.filterIsInstance<String>().orEmpty()

    private companion object {
        const val WORK_DIR = "works"
    }
}

/**
 * Copies the picked photos and videos out of the temporary folder, so that the system does not remove them (D50).
 * The copies go to the Application Support folder with the same relative path.
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

    override fun sizeOf(uri: String): Long {
        val path = NSURL.URLWithString(uri)?.takeIf { it.isFileURL() }?.path ?: return 0
        val size = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)?.get(NSFileSize) as? NSNumber
        return size?.longLongValue ?: 0
    }

    override fun freeBytes(): Long {
        // 사용자가 시작한 저장이므로, 시스템이 지울 수 있는 공간까지 센다.
        val important = NSURL.fileURLWithPath(folder)
            .resourceValuesForKeys(listOf(NSURLVolumeAvailableCapacityForImportantUsageKey), error = null)
            ?.get(NSURLVolumeAvailableCapacityForImportantUsageKey) as? NSNumber
        val free = important ?: NSFileManager.defaultManager.attributesOfFileSystemForPath(folder, error = null)?.get(NSFileSystemFreeSize) as? NSNumber
        return free?.longLongValue?.takeIf { it > 0 } ?: Long.MAX_VALUE
    }

    override fun cleanUp(keep: Set<String>) {
        val fileManager = NSFileManager.defaultManager
        val keepPaths = keep.mapNotNull { savedRelativePath(it) }.map { "$folder/$it" }.toSet()
        val entries = fileManager.subpathsOfDirectoryAtPath(folder, error = null)?.filterIsInstance<String>().orEmpty().map { "$folder/$it" }
        val (folders, files) = entries.partition { isDirectory(it) }
        files.filterNot { it in keepPaths }.forEach { fileManager.removeItemAtPath(it, error = null) }
        // 고를 때마다 폴더가 하나씩 생기므로, 비게 된 폴더도 깊은 것부터 지운다.
        folders.sortedByDescending { it.length }.forEach { path ->
            if (fileManager.contentsOfDirectoryAtPath(path, error = null)?.isEmpty() == true) fileManager.removeItemAtPath(path, error = null)
        }
    }

    /** Path of a copy in the media folder, or null for a file outside of it. */
    private fun savedRelativePath(uri: String): String? {
        val path = NSURL.URLWithString(uri)?.takeIf { it.isFileURL() }?.path ?: return null
        val marker = "/$SUPPORT_DIR/$MEDIA_DIR/"
        return path.substringAfter(marker, missingDelimiterValue = "").ifEmpty { null }
    }

    private fun isDirectory(path: String): Boolean =
        NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)?.get(NSFileType) == NSFileTypeDirectory

    private companion object {
        const val MEDIA_DIR = "media"
        const val SUPPORT_DIR = "Application Support"
    }
}
