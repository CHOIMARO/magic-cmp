package com.company.data.remote

import com.company.data.editor.MediaPersister
import com.company.data.media.MediaMetadata
import com.company.data.media.MediaMetadataReader
import com.company.domain.model.editor.DubbingContent
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.MY_VOICE_ID
import com.company.domain.model.editor.MediaType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Dubbings from the server: voices, job, state requests, and download (app-api-guide 3). */
class RemoteDubbingRepositoryTest {
    private val sessionStore = SessionStore(FakeSecureStore()).apply { signIn(AuthTokens("a1", "r1")) }
    private val requests = mutableListOf<HttpRequestData>()
    private val downloads = mutableListOf<HttpRequestData>()
    private var jobStates = mutableListOf("PENDING", "PROCESSING", "COMPLETED")
    private var server: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        when (request.route) {
            "GET /dubbing-options" -> json(OPTIONS)
            "POST /dubbing-jobs" -> json(job("PENDING"), HttpStatusCode.Accepted)
            "GET /dubbing-jobs/7" -> json(job(jobStates.removeFirstOrNull() ?: "COMPLETED"))
            else -> error(request.route)
        }
    }

    private val client: HttpClient = createMagicHttpClient(
        sessionStore = sessionStore,
        baseUrl = "http://api.test",
        engine = MockEngine { request ->
            requests += request
            server(request)
        },
    )
    private val downloadClient = HttpClient(
        MockEngine { request ->
            downloads += request
            respond(ByteArray(AUDIO_BYTES), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "audio/mpeg"))
        },
    )
    private val media = AudioMediaPersister()
    private val repository = RemoteDubbingRepository(
        client = client,
        mediaPersister = media,
        metadataReader = FakeMetadataReader(),
        downloadClient = downloadClient,
        pollMillis = 1,
        timeoutMillis = 2_000,
        io = Dispatchers.Default,
    )
    private val content = DubbingContent(text = "안녕하세요", voiceId = "SEOHYEON", speed = DubbingSpeed.FAST)

    private fun runApiTest(block: suspend () -> Unit) = runTest { withContext(Dispatchers.Default) { block() } }

    private val HttpRequestData.route get() = "${method.value} ${url.encodedPath}"
    private val HttpRequestData.text get() = (body as? TextContent)?.text

    @Test
    fun loadVoices_usesTheVoicesOfTheServer() = runApiTest {
        repository.loadVoices()

        val voices = repository.voices.value
        assertEquals(listOf("SEOHYEON", "JINWOO"), voices.map { it.id })
        assertEquals("서현", voices[0].name)
        assertEquals("아나운서·대화에 어울리는 여성 목소리", voices[0].description)
    }

    @Test
    fun loadVoices_failure_keepsTheList() = runApiTest {
        repository.loadVoices()
        server = { json("""{"code":"INTERNAL_ERROR","message":"오류"}""", HttpStatusCode.InternalServerError) }

        repository.loadVoices()

        assertEquals(2, repository.voices.value.size)
    }

    @Test
    fun preview_makesJob_thenAsksUntilComplete() = runApiTest {
        val url = repository.preview(content)

        assertEquals(AUDIO_URL, url)
        assertEquals("""{"text":"안녕하세요","presetVoice":"SEOHYEON","speed":1.2}""", requests[0].text)
        assertEquals(listOf("POST /dubbing-jobs", "GET /dubbing-jobs/7", "GET /dubbing-jobs/7", "GET /dubbing-jobs/7"), requests.map { it.route })
    }

    @Test
    fun insertAfterPreview_usesTheSameJob_withANewAddress() = runApiTest {
        repository.preview(content)
        requests.clear()

        val audio = repository.synthesize(content)

        // 같은 내용은 새로 만들지 않는다. 주소는 10분만 유효하므로 받기 직전에 다시 조회한다.
        assertEquals(listOf("GET /dubbing-jobs/7"), requests.map { it.route })
        assertEquals("saved/audio-$AUDIO_BYTES.mp3", audio.audioUri)
        assertEquals(2.5, audio.length)
        // presigned URL에는 토큰을 붙이지 않는다(3-5).
        assertNull(downloads.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun otherContent_makesANewJob() = runApiTest {
        repository.preview(content)
        jobStates = mutableListOf("COMPLETED")
        requests.clear()

        repository.preview(content.copy(speed = DubbingSpeed.SLOW))

        assertEquals("POST /dubbing-jobs", requests.first().route)
    }

    @Test
    fun failedJob_fails() = runApiTest {
        jobStates = mutableListOf("PROCESSING", "FAILED")

        assertFailsWith<DubbingFailedException> { repository.synthesize(content) }

        assertTrue(downloads.isEmpty())
    }

    @Test
    fun slowJob_failsAfterTheWait() = runApiTest {
        val slow = RemoteDubbingRepository(client, media, FakeMetadataReader(), downloadClient, pollMillis = 5, timeoutMillis = 50, io = Dispatchers.Default)
        server = { request -> if (request.route == "POST /dubbing-jobs") json(job("PENDING")) else json(job("PROCESSING")) }

        // 시간 초과도 보통의 실패로 알린다. 취소로 알리면 화면이 실패 안내를 보여 주지 못한다.
        assertFailsWith<DubbingFailedException> { slow.preview(content) }
    }

    @Test
    fun myVoice_isNotSentToTheServer() = runApiTest {
        assertFailsWith<UnsupportedOperationException> { repository.preview(content.copy(voiceId = MY_VOICE_ID)) }

        assertTrue(requests.isEmpty())
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    private fun job(status: String) = """{"id":7,"status":"$status","text":"안녕하세요","presetVoice":"SEOHYEON",""" +
        """"customVoiceId":null,"speed":1.2,"audioUrl":${if (status == "COMPLETED") "\"$AUDIO_URL\"" else "null"},""" +
        """"createdAt":"2026-11-03T10:00:00.1","completedAt":null}"""

    /** Keeps the audio files by size, like the media folder of the app. */
    private class AudioMediaPersister : MediaPersister {
        override fun persist(uri: String): String = uri
        override fun saveAudio(bytes: ByteArray, extension: String): String = "saved/audio-${bytes.size}.$extension"
        override fun restore(uri: String): String = uri
        override fun isCopy(uri: String): Boolean = true
        override fun release(uri: String) = Unit
        override fun sizeOf(uri: String): Long = 0
        override fun freeBytes(): Long = Long.MAX_VALUE
        override fun cleanUp(keep: Set<String>) = Unit
    }

    /** Gives a length of 2.5 seconds for each file. */
    private class FakeMetadataReader : MediaMetadataReader {
        override suspend fun read(uri: String): MediaMetadata = MediaMetadata(name = "audio", type = MediaType.VIDEO, duration = 2.5)
    }

    private companion object {
        const val AUDIO_URL = "https://bucket.s3.test/dubbing/7.mp3?X-Amz-Signature=abc"
        const val AUDIO_BYTES = 1_000
        const val OPTIONS = """{"voices":[{"id":"SEOHYEON","name":"서현","description":"아나운서·대화에 어울리는 여성 목소리"},""" +
            """{"id":"JINWOO","name":"진우","description":"광고·다큐에 어울리는 남성 목소리"}],""" +
            """"speed":{"min":0.5,"max":2.0,"default":1.0,"presets":[{"name":"보통","value":1.0}]},"maxTextLength":200}"""
    }
}
