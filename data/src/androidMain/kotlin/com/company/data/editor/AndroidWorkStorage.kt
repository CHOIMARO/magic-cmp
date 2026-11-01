package com.company.data.editor

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/**
 * Keeps the work files in the private files folder of the app.
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

    private companion object {
        const val WORK_DIR = "works"
    }
}

/**
 * Keeps the read access to the photos and videos from the photo picker after the app restarts.
 *
 * @param context Context for the content resolver.
 */
internal class AndroidMediaPersister(context: Context) : MediaPersister {
    private val resolver = context.contentResolver

    override fun persist(uri: String): String {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != ContentResolver.SCHEME_CONTENT) return uri
        // 오래 유지할 수 없는 URI는 권한을 받지 못한다. 그래도 앱이 켜져 있는 동안은 열 수 있으니 저장은 계속한다.
        runCatching { resolver.takePersistableUriPermission(parsed, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        return uri
    }

    override fun restore(uri: String): String = uri

    override fun release(uri: String) {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != ContentResolver.SCHEME_CONTENT) return
        runCatching { resolver.releasePersistableUriPermission(parsed, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}
