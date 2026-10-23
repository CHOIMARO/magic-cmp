package com.company.data.font

import android.content.Context
import java.io.File

/**
 * Keeps the font files in the private files folder of the app.
 *
 * @param context Context for the files folder.
 */
internal class AndroidFontFileStore(context: Context) : FontFileStore {
    private val folder = File(context.filesDir, FONT_DIR)

    override fun path(fileName: String): String = file(fileName).path

    override fun exists(fileName: String): Boolean = file(fileName).let { it.isFile && it.length() > 0 }

    override fun write(fileName: String, bytes: ByteArray) {
        val target = file(fileName)
        target.parentFile?.mkdirs()
        // 다 쓴 뒤에 이름을 바꿔서, 쓰는 중에 앱이 꺼져도 반쯤 쓴 파일을 글꼴로 읽지 않게 한다.
        val temp = File(target.parentFile, "${target.name}.part")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Cannot save the font file $fileName")
        }
    }

    // 원격 경로의 폴더 구분자는 파일 이름에 넣지 않는다.
    private fun file(fileName: String): File = File(folder, fileName.replace('/', '_'))

    private companion object {
        const val FONT_DIR = "fonts"
    }
}
