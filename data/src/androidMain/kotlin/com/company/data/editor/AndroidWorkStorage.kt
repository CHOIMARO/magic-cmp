package com.company.data.editor

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

/**
 * Keeps the work files in the private files folder of the app. The backup rules of the app leave the folder out.
 *
 * @param context Context for the files folder.
 */
internal class AndroidWorkFileStore(context: Context) : WorkFileStore {
    private val folder = File(context.filesDir, WORK_DIR)

    override fun read(fileName: String): String? = File(folder, fileName).takeIf { it.isFile }?.readText()

    override fun write(fileName: String, text: String) {
        folder.mkdirs()
        val target = File(folder, fileName)
        // 다 쓴 뒤에 이름을 바꿔서, 쓰는 중에 앱이 꺼져도 이전 파일이 그대로 남게 한다.
        val temp = File(folder, "$fileName.part")
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Cannot save the work file $fileName")
        }
    }

    override fun delete(fileName: String) {
        File(folder, fileName).delete()
    }

    override fun list(): List<String> = folder.list()?.toList().orEmpty()

    private companion object {
        const val WORK_DIR = "works"
    }
}

/**
 * Copies the photos and videos from the photo picker into the private files folder of the app (D50). The access that
 * the picker gives can end when the app stops, but the copies stay. The backup rules of the app leave the folder out.
 *
 * @param context Context for the content resolver and the files folder.
 */
internal class AndroidMediaPersister(private val context: Context) : MediaPersister {
    private val resolver = context.contentResolver
    private val folder = File(context.filesDir, MEDIA_DIR)

    override fun persist(uri: String): String {
        val source = Uri.parse(uri)
        if (source.scheme != ContentResolver.SCHEME_CONTENT) return uri
        folder.mkdirs()
        val extension = resolver.getType(source)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val target = File(folder, UUID.randomUUID().toString() + extension?.let { ".$it" }.orEmpty())
        // 다 복사한 뒤에 이름을 바꿔서, 복사하다 앱이 꺼져도 반쯤 복사한 파일을 원본으로 쓰지 않게 한다.
        val temp = File(folder, "${target.name}.part")
        return try {
            checkNotNull(resolver.openInputStream(source)) { "Cannot open $uri" }.use { input ->
                temp.outputStream().use { output -> input.copyTo(output, COPY_BUFFER_BYTES) }
            }
            check(temp.renameTo(target)) { "Cannot save the copy of $uri" }
            Uri.fromFile(target).toString()
        } catch (e: Exception) {
            temp.delete()
            uri
        }
    }

    override fun saveAudio(bytes: ByteArray, extension: String): String {
        folder.mkdirs()
        val target = File(folder, "${UUID.randomUUID()}.$extension")
        // 다 쓴 뒤에 이름을 바꿔서, 쓰다 앱이 꺼져도 반쯤 쓴 파일을 쓰지 않게 한다.
        val temp = File(folder, "${target.name}.part")
        try {
            temp.writeBytes(bytes)
            check(temp.renameTo(target)) { "Cannot save the audio file" }
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
        return Uri.fromFile(target).toString()
    }

    override fun restore(uri: String): String = uri

    override fun isCopy(uri: String): Boolean = copyFile(uri) != null

    override fun release(uri: String) {
        copyFile(uri)?.delete()
    }

    override fun sizeOf(uri: String): Long {
        val parsed = Uri.parse(uri)
        return try {
            when (parsed.scheme) {
                ContentResolver.SCHEME_FILE -> parsed.path?.let { File(it).length() } ?: 0
                ContentResolver.SCHEME_CONTENT -> contentSize(parsed)
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    override fun freeBytes(): Long = try {
        val storage = context.getSystemService(StorageManager::class.java)
        // 시스템이 다른 앱의 캐시를 비워서 만들 수 있는 공간까지 센다.
        storage.getAllocatableBytes(storage.getUuidForPath(context.filesDir))
    } catch (e: Exception) {
        context.filesDir.usableSpace.takeIf { it > 0 } ?: Long.MAX_VALUE
    }

    override fun cleanUp(keep: Set<String>) {
        val keepPaths = keep.mapNotNull { copyFile(it)?.path }.toSet()
        folder.listFiles()?.filterNot { it.path in keepPaths }?.forEach { it.delete() }
    }

    /** Size that the content provider tells, or the length of the file that it opens. */
    private fun contentSize(uri: Uri): Long {
        val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
        return size ?: resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }?.takeIf { it >= 0 } ?: 0
    }

    /** The copy at [uri], or null when [uri] is not a copy in the media folder. */
    private fun copyFile(uri: String): File? {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != ContentResolver.SCHEME_FILE) return null
        val file = File(parsed.path ?: return null)
        return file.takeIf { it.parentFile == folder }
    }

    private companion object {
        const val MEDIA_DIR = "media"
        const val COPY_BUFFER_BYTES = 256 * 1024
    }
}
