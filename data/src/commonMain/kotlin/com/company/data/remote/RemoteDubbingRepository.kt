package com.company.data.remote

import com.company.data.editor.MediaPersister
import com.company.data.media.MediaMetadataReader
import com.company.data.remote.dto.CreateDubbingJobRequest
import com.company.data.remote.dto.DubbingJobResponse
import com.company.data.remote.dto.DubbingJobStatus
import com.company.data.remote.dto.DubbingOptionsResponse
import com.company.domain.model.editor.DubbingAudio
import com.company.domain.model.editor.DubbingContent
import com.company.domain.model.editor.DubbingVoice
import com.company.domain.model.account.ServerException
import com.company.domain.model.editor.MY_VOICE_ID
import com.company.domain.repository.DubbingRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.CoroutineContext

/** The server did not make the audio. The user can try again with a new request. */
internal class DubbingFailedException(message: String) : Exception(message)

/**
 * Dubbings from the API server (app-api-guide 3).
 *
 * The server takes a request at once and makes the audio later. The repository asks for the state every second, for
 * 60 seconds at most. The address of a finished audio is valid for 10 minutes, so the repository asks for a new
 * address right before each use.
 *
 * The server makes the audio of one content only one time. An insert after a preview uses the job of the preview,
 * also when the preview stopped before the job was finished.
 *
 * @param client HTTP client of the API server.
 * @param mediaPersister Keeps the audio files in the app storage.
 * @param metadataReader Reads the length of an audio file.
 * @param downloadClient HTTP client for the audio files. It has no Auth plugin, because the address has a signature.
 * @param pollMillis Time between two state requests.
 * @param timeoutMillis Longest wait for the audio.
 * @param io Context of the file work.
 */
internal class RemoteDubbingRepository(
    private val client: HttpClient,
    private val mediaPersister: MediaPersister,
    private val metadataReader: MediaMetadataReader,
    private val downloadClient: HttpClient = defaultDownloadClient(),
    private val pollMillis: Long = POLL_MILLIS,
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
    private val io: CoroutineContext = Dispatchers.IO,
) : DubbingRepository {
    private val _voices = MutableStateFlow<List<DubbingVoice>>(emptyList())
    override val voices: StateFlow<List<DubbingVoice>> = _voices.asStateFlow()

    private val mutex = Mutex()

    /** Content and job of the last finished audio. A preview and then an insert of the same content use one job. */
    private var lastJob: Pair<DubbingContent, Long>? = null

    /**
     * Content and job that the server makes now. A request that stopped (for example a preview, when the user taps
     * the insert button) keeps it, so the next request of the same content waits for this job.
     */
    private var pendingJob: Pair<DubbingContent, Long>? = null

    override suspend fun loadVoices() {
        try {
            val options: DubbingOptionsResponse = client.get("dubbing-options").body()
            if (options.voices.isEmpty()) return
            _voices.value = options.voices.mapIndexed { index, voice ->
                DubbingVoice(id = voice.id, name = voice.name, description = voice.description, hue = VoiceHues[index % VoiceHues.size])
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 다음에 더빙 시트를 열 때 다시 받는다.
        }
    }

    override suspend fun preview(content: DubbingContent): String = finishedJob(content).audioUrl()

    override suspend fun synthesize(content: DubbingContent): DubbingAudio {
        val url = finishedJob(content).audioUrl()
        val response: HttpResponse = downloadClient.get(url)
        if (!response.status.isSuccess()) throw DubbingFailedException("Audio download failed: ${response.status}")
        val bytes = response.body<ByteArray>()
        if (bytes.isEmpty()) throw DubbingFailedException("The audio file is empty")
        val uri = withContext(io) { mediaPersister.saveAudio(bytes, AUDIO_EXTENSION) }
        val length = metadataReader.read(uri)?.duration?.takeIf { it > 0 } ?: estimatedSeconds(bytes.size)
        return DubbingAudio(length = length, audioUri = uri)
    }

    /**
     * Gives a finished job of the content with a fresh audio address.
     *
     * The job of the last finished content is used again. A job of the same content that the server makes now is
     * waited for. Otherwise the repository makes a new job and waits for it.
     */
    private suspend fun finishedJob(content: DubbingContent): DubbingJobResponse = mutex.withLock {
        // 내 목소리는 Typecast 유료 플랜 전까지 서버에서 쓸 수 없다(app-api-guide 3-8).
        if (content.voiceId == MY_VOICE_ID) throw UnsupportedOperationException("The server cannot use the own voice yet")
        lastJob?.takeIf { it.first == content }?.let { (_, id) ->
            // 주소는 10분만 유효하므로 쓰기 직전에 다시 조회한다(3-5).
            val job = getJob(id)
            if (job.status == DubbingJobStatus.COMPLETED && job.audioUrl != null) return@withLock job
        }
        // 미리 듣기를 만드는 중에 더빙 넣기를 누르면, 새로 만들지 않고 그 작업을 이어서 기다린다.
        val id = pendingJob?.takeIf { it.first == content }?.second ?: createJob(content)
        val finished = try {
            withTimeoutOrNull(timeoutMillis) { waitForJob(id) }
                ?: throw DubbingFailedException("The audio is not ready after ${timeoutMillis / 1000} seconds")
        } catch (e: DubbingFailedException) {
            // 서버가 실패했거나 너무 오래 걸린 작업은 버린다. 다시 시도하면 새 작업을 만든다(3-4).
            pendingJob = null
            throw e
        } catch (e: ServerException) {
            // 서버에 없는 작업(404 DUBBING_JOB_NOT_FOUND)은 버린다. 네트워크 오류면 다음에 같은 작업을 이어서 기다린다.
            if (e.status == HttpStatusCode.NotFound.value) pendingJob = null
            throw e
        }
        pendingJob = null
        lastJob = content to finished.id
        finished
    }

    /**
     * Asks the server for a new job and keeps it as [pendingJob]. The request runs to its end, also when the caller
     * stops waiting, so that the app knows each job that the server takes.
     */
    private suspend fun createJob(content: DubbingContent): Long = withContext(NonCancellable) {
        val request = CreateDubbingJobRequest(text = content.text, presetVoice = content.voiceId, speed = content.speed.factor)
        val created: DubbingJobResponse = client.post("dubbing-jobs") { setBody(request) }.body()
        pendingJob = content to created.id
        created.id
    }

    /** Asks for the state of a job every [pollMillis] until it is finished. */
    private suspend fun waitForJob(id: Long): DubbingJobResponse {
        while (true) {
            delay(pollMillis)
            val job = getJob(id)
            when (job.status) {
                DubbingJobStatus.COMPLETED -> if (job.audioUrl != null) return job
                DubbingJobStatus.FAILED -> throw DubbingFailedException("The server could not make the audio")
            }
        }
    }

    private suspend fun getJob(id: Long): DubbingJobResponse = client.get("dubbing-jobs/$id").body()

    private fun DubbingJobResponse.audioUrl(): String = audioUrl ?: throw DubbingFailedException("The job has no audio")

    private companion object {
        const val POLL_MILLIS = 1_000L
        const val TIMEOUT_MILLIS = 60_000L
        const val AUDIO_EXTENSION = "mp3"

        /** Bit rate of the audio of the server: mp3 at 320 kbps. It estimates the length when the file is not readable. */
        const val AUDIO_BYTES_PER_SECOND = 320_000 / 8

        /** Hues of the voice avatars, in the order of the voices. The server gives no colors. */
        val VoiceHues = listOf(330, 260, 200, 150, 70)

        fun estimatedSeconds(bytes: Int): Double = bytes.toDouble() / AUDIO_BYTES_PER_SECOND

        fun defaultDownloadClient() = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }
}
