package com.company.data.font

import com.company.domain.model.editor.TextFontType
import com.company.domain.repository.FontFiles
import com.company.domain.repository.FontRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Stores font files on the device. */
internal interface FontFileStore {
    /**
     * Path of a font file.
     *
     * @param fileName Name of the file.
     */
    fun path(fileName: String): String

    /**
     * True when the file is on the device.
     *
     * @param fileName Name of the file.
     */
    fun exists(fileName: String): Boolean

    /**
     * Writes a file. A file with the same name is replaced.
     *
     * @param fileName Name of the file.
     * @param bytes Content of the file.
     */
    fun write(fileName: String, bytes: ByteArray)
}

/**
 * Source of a downloadable font.
 *
 * @property regular File name of the regular weight.
 * @property bold File name of the bold weight, or null.
 */
private data class FontSource(val regular: String, val bold: String?)

/**
 * Downloads the fonts from the Google Fonts repository on GitHub. All fonts there use the SIL Open Font License 1.1.
 *
 * The list of fonts is fixed in the app. A font server can replace it later (PRD D03).
 *
 * @param store Stores the font files.
 */
internal class RemoteFontRepository(private val store: FontFileStore) : FontRepository {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = DOWNLOAD_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
    }
    private val mutex = Mutex()

    private val _downloadedFonts = MutableStateFlow(
        Sources.mapNotNull { (font, source) -> source.toFilesIfStored()?.let { font to it } }.toMap(),
    )
    override val downloadedFonts: StateFlow<Map<TextFontType, FontFiles>> = _downloadedFonts.asStateFlow()

    override suspend fun download(font: TextFontType): FontFiles = mutex.withLock {
        _downloadedFonts.value[font]?.let { return it }
        val source = requireNotNull(Sources[font]) { "The app cannot download $font" }
        withContext(Dispatchers.IO) {
            listOfNotNull(source.regular, source.bold).forEach { fileName ->
                if (!store.exists(fileName)) store.write(fileName, fetch(fileName))
            }
        }
        val files = checkNotNull(source.toFilesIfStored())
        _downloadedFonts.update { it + (font to files) }
        files
    }

    private suspend fun fetch(fileName: String): ByteArray {
        val response: HttpResponse = client.get(BASE_URL + fileName)
        check(response.status.isSuccess()) { "Font download failed: ${response.status}" }
        return response.body<ByteArray>().also { check(it.isNotEmpty()) { "Font file is empty" } }
    }

    private fun FontSource.toFilesIfStored(): FontFiles? {
        if (!store.exists(regular) || (bold != null && !store.exists(bold))) return null
        return FontFiles(regular = store.path(regular), bold = bold?.let(store::path))
    }

    private companion object {
        /** Folder of the Google Fonts repository. */
        const val BASE_URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/"

        const val DOWNLOAD_TIMEOUT_MILLIS = 60_000L
        const val CONNECT_TIMEOUT_MILLIS = 15_000L

        /** Files of each downloadable font. The paths are relative to [BASE_URL]. */
        val Sources: Map<TextFontType, FontSource> = mapOf(
            TextFontType.GAEGU to FontSource("gaegu/Gaegu-Regular.ttf", "gaegu/Gaegu-Bold.ttf"),
            TextFontType.DONGLE to FontSource("dongle/Dongle-Regular.ttf", "dongle/Dongle-Bold.ttf"),
            TextFontType.GUGI to FontSource("gugi/Gugi-Regular.ttf", null),
            TextFontType.SONG_MYUNG to FontSource("songmyung/SongMyung-Regular.ttf", null),
        )
    }
}
