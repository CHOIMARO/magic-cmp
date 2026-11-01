package com.company.data.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.VideoSummary
import com.company.domain.model.editor.newEditorId
import com.company.domain.repository.CaptionRepository
import com.company.domain.repository.MyVideoRepository
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** One day in milliseconds. */
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Time between two sample entries of the same day, in milliseconds. It keeps the sample order stable. */
private const val SAMPLE_STEP_MILLIS = 10L * 60 * 1000

/** File of the work list. */
private const val INDEX_FILE = "works.json"

/** File of the project of a work. */
private fun projectFile(id: String) = "work-$id.json"

/** Format of the work files. A newer app version can add fields. */
private val WorkJson = Json { ignoreUnknownKeys = true }

/**
 * Keeps the works and their projects on the device (F15). The work with the most recent change is first.
 *
 * The work list is one JSON file, and each project is one JSON file. The source photos and videos stay where they
 * are; [MediaPersister] keeps the access to them. The list loads in the background when the app starts.
 *
 * The memory has the latest state. When a file write fails, the next save of the work writes the files again.
 *
 * At the first start, the list has the sample works. A sample work opens the sample project, as in the prototype, and
 * its first change saves it as a normal work.
 *
 * @param store Files of the works.
 * @param mediaPersister Keeps the access to the source files.
 * @param captionRepository Makes the captions of the sample project.
 * @param io Context of the file work. Tests use a context that does not change the thread.
 */
internal class LocalMyVideoRepository(
    private val store: WorkFileStore,
    private val mediaPersister: MediaPersister,
    private val captionRepository: CaptionRepository,
    private val io: CoroutineContext = Dispatchers.IO,
) : MyVideoRepository {
    private val mutex = Mutex()
    private val _works = MutableStateFlow<List<VideoSummary>>(emptyList())
    override val works: StateFlow<List<VideoSummary>> = _works.asStateFlow()

    /** IDs of the sample works that still open the sample project. */
    private val samples = mutableSetOf<String>()

    /** IDs of the works with changes that are not in the files yet. */
    private val unsaved = mutableSetOf<String>()

    /** Last saved project of each work that the app read or wrote. */
    private val projects = mutableMapOf<String, EditorProject>()

    /** Source locations that already have a lasting access, with the location to save. */
    private val persistedUris = mutableMapOf<String, String>()
    private var loaded = false

    init {
        CoroutineScope(SupervisorJob() + io).launch { mutex.withLock { ensureLoaded() } }
    }

    override suspend fun addWork(project: EditorProject): VideoSummary = mutex.withLock {
        ensureLoaded()
        val id = newEditorId()
        val saved = persistMedia(project.copy(workId = id))
        val work = saved.summary(id, now())
        projects[id] = saved
        unsaved += id
        _works.value = listOf(work) + _works.value
        // 파일에 쓰지 못해도 편집은 계속한다. 다음 저장에서 다시 쓴다.
        try {
            writeFiles(id, saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            unsaved += id
        }
        work
    }

    override suspend fun saveWork(id: String, project: EditorProject): Boolean = mutex.withLock {
        ensureLoaded()
        val work = _works.value.find { it.id == id } ?: return@withLock false
        val saved = persistMedia(project.copy(workId = id))
        if (id !in unsaved && loadProject(id, work) == saved) return@withLock false
        projects[id] = saved
        samples -= id
        unsaved += id
        val changed = saved.summary(id, now()).copy(exported = work.exported)
        _works.value = listOf(changed) + _works.value.filterNot { it.id == id }
        writeFiles(id, saved)
        true
    }

    override suspend fun loadWork(id: String): EditorProject? = mutex.withLock {
        ensureLoaded()
        val work = _works.value.find { it.id == id } ?: return@withLock null
        loadProject(id, work)
    }

    override suspend fun deleteWork(id: String) {
        mutex.withLock {
            ensureLoaded()
            val work = _works.value.find { it.id == id } ?: return@withLock
            val media = if (id in samples) emptySet() else loadProject(id, work)?.mediaUris().orEmpty()
            val remaining = _works.value.filterNot { it.id == id }
            // 목록 파일을 먼저 쓴다. 쓰지 못하면 작업을 그대로 둔다.
            writeIndex(remaining)
            _works.value = remaining
            projects.remove(id)
            samples -= id
            unsaved -= id
            withContext(io) { store.delete(projectFile(id)) }
            releaseUnused(media)
        }
    }

    override suspend fun markExported(id: String) {
        mutex.withLock {
            ensureLoaded()
            if (_works.value.none { it.id == id && !it.exported }) return@withLock
            _works.value = _works.value.map { if (it.id == id) it.copy(exported = true) else it }
            writeIndex(_works.value)
        }
    }

    // =====================================================================
    // Files
    // =====================================================================

    /**
     * Reads the work list once. Without a list file, the list has the sample works.
     * A list file that the app cannot read gives no works.
     */
    private suspend fun ensureLoaded() {
        if (loaded) return
        val entries = try {
            val text = withContext(io) { store.read(INDEX_FILE) }
            if (text == null) {
                EditorSampleData.Works.toSampleEntries(now())
            } else {
                WorkJson.decodeFromString<WorkIndexDto>(text).works
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        samples += entries.filter { it.sample }.map { it.id }
        _works.value = entries.map { it.toDomain() }
        loaded = true
    }

    /** Writes the project file, then the list file. The project file comes first, so that the list never points to a missing project. */
    private suspend fun writeFiles(id: String, project: EditorProject) {
        val text = WorkJson.encodeToString(ProjectFileDto(project = project.toDto()))
        withContext(io) { store.write(projectFile(id), text) }
        writeIndex(_works.value)
        unsaved -= id
    }

    private suspend fun writeIndex(works: List<VideoSummary>) {
        val index = WorkIndexDto(works = works.map { it.toDto(sample = it.id in samples) })
        withContext(io) { store.write(INDEX_FILE, WorkJson.encodeToString(index)) }
    }

    /** Last saved project of a work. A sample work that the user did not change gets the sample project. */
    private suspend fun loadProject(id: String, work: VideoSummary): EditorProject? {
        projects[id]?.let { return it }
        val project = if (id in samples) {
            sampleProject(work.name).copy(workId = id)
        } else {
            readProject(id)?.copy(workId = id)?.let { restoreMedia(it) } ?: return null
        }
        projects[id] = project
        return project
    }

    /** Reads a project file. A missing file or a file that the app cannot read gives null. */
    private suspend fun readProject(id: String): EditorProject? = try {
        withContext(io) { store.read(projectFile(id)) }?.let { WorkJson.decodeFromString<ProjectFileDto>(it).project.toDomain() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    // =====================================================================
    // Source files
    // =====================================================================

    /** The project with lasting access to its source files. A location that changes gets the new location. */
    private suspend fun persistMedia(project: EditorProject): EditorProject {
        val uris = project.mediaUris().filterNot { it in persistedUris }
        if (uris.isNotEmpty()) persistedUris += withContext(io) { uris.associateWith { mediaPersister.persist(it) } }
        return project.mapMedia { persistedUris[it] ?: it }
    }

    /** The project with the current locations of its saved source files. */
    private suspend fun restoreMedia(project: EditorProject): EditorProject {
        val restored = withContext(io) { project.mediaUris().associateWith { mediaPersister.restore(it) } }
        restored.values.forEach { persistedUris[it] = it }
        return project.mapMedia { restored[it] ?: it }
    }

    /** Gives up the access to the source files that no other saved work uses. */
    private suspend fun releaseUnused(media: Set<String>) {
        if (media.isEmpty()) return
        val used = _works.value.filterNot { it.id in samples }.flatMap { loadProject(it.id, it)?.mediaUris().orEmpty() }.toSet()
        val unused = media - used
        if (unused.isEmpty()) return
        withContext(io) { unused.forEach { mediaPersister.release(it) } }
        persistedUris.entries.removeAll { it.value in unused }
    }

    /** Sample project of a sample work. */
    private suspend fun sampleProject(name: String): EditorProject {
        val clips = EditorSampleData.SampleClipIndexes.map { index ->
            val source = EditorSampleData.SampleVideos[index]
            val long = source.duration > 4
            Clip(
                id = newEditorId(),
                name = source.name,
                sourceDuration = source.duration,
                inPoint = if (long) 0.4 else 0.0,
                outPoint = if (long) minOf(source.duration, 3.8) else source.duration,
                thumbnailHue = source.hue,
            )
        }
        val project = EditorProject(
            name = name,
            clips = clips,
            texts = listOf(
                TextOverlay(
                    id = newEditorId(),
                    text = EditorSampleData.SampleTitle,
                    start = 0.2,
                    length = 2.8,
                    style = TextStyleType.IMPACT,
                    x = 50.0,
                    y = 24.0,
                )
            ),
            musicId = EditorSampleData.SampleMusicId,
        )
        return project.copy(captions = captionRepository.generateCaptions(project.totalDuration))
    }
}

/** Current time in milliseconds since 1970-01-01 UTC. */
@OptIn(ExperimentalTime::class)
private fun now(): Long = Clock.System.now().toEpochMilliseconds()

/** Summary of the project as the work [id], changed at [time]. A project without clips has a black card. */
private fun EditorProject.summary(id: String, time: Long): VideoSummary =
    VideoSummary(id = id, name = name, duration = totalDuration, thumbnailHue = clips.firstOrNull()?.thumbnailHue, modifiedAt = time)

/** Locations of the source files of the clips. */
private fun EditorProject.mediaUris(): Set<String> = clips.mapNotNull { it.sourceUri }.toSet()

/** The project with each source location changed by [transform]. */
private fun EditorProject.mapMedia(transform: (String) -> String): EditorProject =
    if (clips.none { it.sourceUri != null }) this
    else copy(clips = clips.map { clip -> clip.sourceUri?.let { clip.copy(sourceUri = transform(it)) } ?: clip })

/** Makes the list entries of the sample works, with the most recent change first. */
private fun List<EditorSampleData.SampleSummary>.toSampleEntries(now: Long): List<WorkSummaryDto> =
    mapIndexed { index, sample ->
        WorkSummaryDto(
            id = newEditorId(),
            name = sample.name,
            duration = sample.duration,
            thumbnailHue = sample.thumbnailHue,
            modifiedAt = now - sample.daysAgo * DAY_MILLIS - index * SAMPLE_STEP_MILLIS,
            exported = sample.exported,
            sample = true,
        )
    }.sortedByDescending { it.modifiedAt }
