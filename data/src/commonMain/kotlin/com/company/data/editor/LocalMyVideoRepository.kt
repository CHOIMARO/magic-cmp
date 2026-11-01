package com.company.data.editor

import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.VideoSummary
import com.company.domain.model.editor.WorkThumbnail
import com.company.domain.model.editor.newEditorId
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

/** File of the work list. */
private const val INDEX_FILE = "works.json"

/** File of the project of a work. */
private fun projectFile(id: String) = "work-$id.json"

/** Format of the work files. A newer app version can add fields. */
private val WorkJson = Json { ignoreUnknownKeys = true }

/**
 * Keeps the works and their projects on the device (F15). The work with the most recent change is first.
 *
 * The work list is one JSON file, and each project is one JSON file. The picked photos and videos are copied into the
 * app storage in the background, and the saved project then uses the copies (D50). Until a copy is ready, the project
 * uses the picked file. The list loads in the background when the app starts. Then the files that no work uses go
 * away (D51).
 *
 * The memory has the latest state. When a file write fails, the next save of the work writes the files again.
 *
 * At the first start, the list is empty. The list has only the works that the user made. Older app versions put
 * sample works in the list; the repository leaves out the sample works that the user did not change.
 *
 * @param store Files of the works.
 * @param mediaPersister Copies the source files.
 * @param io Context of the file work. Tests use a context that does not change the thread.
 */
internal class LocalMyVideoRepository(
    private val store: WorkFileStore,
    private val mediaPersister: MediaPersister,
    private val io: CoroutineContext = Dispatchers.IO,
) : MyVideoRepository {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + io)
    private val _works = MutableStateFlow<List<VideoSummary>>(emptyList())
    override val works: StateFlow<List<VideoSummary>> = _works.asStateFlow()

    /** IDs of the works with changes that are not in the files yet. */
    private val unsaved = mutableSetOf<String>()

    /** Last saved project of each work that the app read or wrote. */
    private val projects = mutableMapOf<String, EditorProject>()

    /** Source files that the app tried to copy, with the location to save. A failed copy keeps the source location. */
    private val copies = mutableMapOf<String, String>()

    /** Source files that a background copy reads now. */
    private val copying = mutableSetOf<String>()
    private var loaded = false

    init {
        scope.launch { mutex.withLock { ensureLoaded() } }
    }

    override suspend fun workNames(): List<String> = mutex.withLock {
        ensureLoaded()
        _works.value.map { it.name }
    }

    override suspend fun addWork(project: EditorProject): VideoSummary = withCopies { pending ->
        ensureLoaded()
        val id = newEditorId()
        val saved = useCopies(project.copy(workId = id), pending)
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

    override suspend fun saveWork(id: String, project: EditorProject): Boolean = withCopies { pending ->
        ensureLoaded()
        val work = _works.value.find { it.id == id } ?: return@withCopies false
        val saved = useCopies(project.copy(workId = id), pending)
        if (id !in unsaved && loadProject(id) == saved) return@withCopies false
        projects[id] = saved
        unsaved += id
        val changed = saved.summary(id, now()).copy(exported = work.exported, storageBytes = work.storageBytes)
        _works.value = listOf(changed) + _works.value.filterNot { it.id == id }
        writeFiles(id, saved)
        true
    }

    override suspend fun loadWork(id: String): EditorProject? = mutex.withLock {
        ensureLoaded()
        if (_works.value.none { it.id == id }) return@withLock null
        loadProject(id)
    }

    override suspend fun deleteWork(id: String) {
        mutex.withLock {
            ensureLoaded()
            if (_works.value.none { it.id == id }) return@withLock
            val media = loadProject(id)?.mediaUris().orEmpty()
            val remaining = _works.value.filterNot { it.id == id }
            // 목록 파일을 먼저 쓴다. 쓰지 못하면 작업을 그대로 둔다.
            writeIndex(remaining)
            _works.value = remaining
            projects.remove(id)
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

    override suspend fun bytesToCopy(uris: List<String>): Long {
        val needed = mutex.withLock {
            ensureLoaded()
            uris.distinct().filterNot { it in copies || it in copying }
        }
        return withContext(io) { needed.sumOf { mediaPersister.sizeOf(it) } }
    }

    override suspend fun freeBytes(): Long = withContext(io) { mediaPersister.freeBytes() }

    // =====================================================================
    // Files
    // =====================================================================

    /**
     * Reads the work list and the projects once. Without a list file, the list is empty. A list file that the app
     * cannot read gives no works. The sample works of older app versions stay out. After a good read, the files that
     * no work uses go away (D51).
     */
    private suspend fun ensureLoaded() {
        if (loaded) return
        val index = readIndex()
        // 예전 버전이 넣은 샘플 작업은 사용자가 만든 것이 아니므로 목록에서 뺀다. 다음에 목록을 쓸 때 파일에서도 빠진다.
        val entries = index.entries.filterNot { it.sample }
        var allProjectsRead = true
        val works = entries.map { entry ->
            val work = entry.toDomain()
            val project = readProject(entry.id)?.copy(workId = entry.id)?.let { restoreMedia(it) }
            if (project == null) {
                allProjectsRead = false
                return@map work
            }
            projects[entry.id] = project
            work.copy(storageBytes = storageBytes(project, encode(project)), thumbnail = project.thumbnail())
        }
        if (index.trusted) removeUnusedFiles(entries.map { it.id }.toSet(), withCopies = allProjectsRead)
        _works.value = works
        loaded = true
    }

    /** Reads the work list. [StoredIndex.trusted] is false without a list file or when the app cannot read it. */
    private suspend fun readIndex(): StoredIndex = try {
        val text = withContext(io) { store.read(INDEX_FILE) }
        if (text == null) {
            StoredIndex(emptyList(), trusted = false)
        } else {
            StoredIndex(WorkJson.decodeFromString<WorkIndexDto>(text).works, trusted = true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        StoredIndex(emptyList(), trusted = false)
    }

    /**
     * Writes the project file, then the list file with the new size of the work. The project file comes first, so that
     * the list never points to a missing project. The card also gets the first frame of the saved project, so that it
     * uses the copy when the copy is ready.
     */
    private suspend fun writeFiles(id: String, project: EditorProject) {
        val text = encode(project)
        withContext(io) { store.write(projectFile(id), text) }
        val bytes = storageBytes(project, text)
        _works.value = _works.value.map { if (it.id == id) it.copy(storageBytes = bytes, thumbnail = project.thumbnail()) else it }
        writeIndex(_works.value)
        unsaved -= id
    }

    private suspend fun writeIndex(works: List<VideoSummary>) {
        val index = WorkIndexDto(works = works.map { it.toDto() })
        withContext(io) { store.write(INDEX_FILE, WorkJson.encodeToString(index)) }
    }

    /** Last saved project of a work, or null when the app cannot read it. */
    private suspend fun loadProject(id: String): EditorProject? {
        projects[id]?.let { return it }
        val project = readProject(id)?.copy(workId = id)?.let { restoreMedia(it) } ?: return null
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

    /**
     * Removes the files that no work uses: project files that the list does not name, files that a write did not
     * finish, and copies that no project uses. When a project cannot be read, all copies stay, because the copies of
     * that project are not known.
     */
    private suspend fun removeUnusedFiles(ids: Set<String>, withCopies: Boolean) {
        val keepFiles = ids.mapTo(mutableSetOf(INDEX_FILE)) { projectFile(it) }
        val keepCopies = projects.values.flatMapTo(mutableSetOf()) { it.mediaUris() }
        try {
            withContext(io) {
                store.list().filterNot { it in keepFiles }.forEach { store.delete(it) }
                if (withCopies) mediaPersister.cleanUp(keepCopies)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 지우지 못한 파일은 다음 시작 때 다시 지운다.
        }
    }

    /** Bytes of a work: its project file and the files of its clips (D51). */
    private suspend fun storageBytes(project: EditorProject, projectText: String): Long = withContext(io) {
        projectText.encodeToByteArray().size + project.mediaUris().sumOf { mediaPersister.sizeOf(it) }
    }

    // =====================================================================
    // Source files
    // =====================================================================

    /**
     * Runs [block] under the lock, and then starts the copies that [block] asked for. The copies run in the
     * background, so that a large video does not hold the picker or the editor.
     */
    private suspend fun <T> withCopies(block: suspend (MutableList<String>) -> T): T {
        val pending = mutableListOf<String>()
        try {
            return mutex.withLock { block(pending) }
        } finally {
            pending.forEach { uri -> scope.launch { copyInBackground(uri) } }
        }
    }

    /**
     * The project with the copies of its source files. A source file without a copy keeps its location, and
     * [pending] gets it, so that a copy starts.
     */
    private fun useCopies(project: EditorProject, pending: MutableList<String>): EditorProject {
        project.mediaUris().filterNot { it in copies || it in copying }.forEach { uri ->
            copying += uri
            pending += uri
        }
        return project.mapMedia { copies[it] ?: it }
    }

    /**
     * Copies one source file, and then changes the saved projects that use it to the copy. When no project uses the
     * file any more, the copy goes away, and the next save copies the file again.
     */
    private suspend fun copyInBackground(uri: String) {
        val copy = try {
            withContext(io) { mediaPersister.persist(uri) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uri
        }
        mutex.withLock {
            copying -= uri
            val users = projects.filterValues { uri in it.mediaUris() }
            if (copy != uri && users.isEmpty()) {
                withContext(io) { mediaPersister.release(copy) }
                return@withLock
            }
            copies[uri] = copy
            if (copy == uri) return@withLock
            users.forEach { (id, project) ->
                val updated = project.mapMedia { if (it == uri) copy else it }
                projects[id] = updated
                try {
                    writeFiles(id, updated)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    unsaved += id
                }
            }
        }
    }

    /**
     * The project with the current locations of its copies. A clip that still points to a source file keeps it, and
     * the next save of that file makes a copy.
     */
    private suspend fun restoreMedia(project: EditorProject): EditorProject {
        val restored = withContext(io) { project.mediaUris().associateWith { mediaPersister.restore(it) } }
        // 복사본만 복사된 것으로 본다. 원본을 넣으면 같은 항목을 다시 골라도 복사하지 않는다.
        restored.values.filter { mediaPersister.isCopy(it) }.forEach { copies[it] = it }
        return project.mapMedia { restored[it] ?: it }
    }

    /** Removes the copies that no other saved work uses. */
    private suspend fun releaseUnused(media: Set<String>) {
        if (media.isEmpty()) return
        val used = _works.value.flatMap { loadProject(it.id)?.mediaUris().orEmpty() }.toSet()
        val unused = media - used
        if (unused.isEmpty()) return
        withContext(io) { unused.forEach { mediaPersister.release(it) } }
        copies.entries.removeAll { it.value in unused }
    }
}

/**
 * Work list that the app read from the device.
 *
 * @property entries Works in the list.
 * @property trusted True when the app read a list file without an error. Only then the unused files can go away.
 */
private class StoredIndex(val entries: List<WorkSummaryDto>, val trusted: Boolean)

/** Text of the project file of a project. */
private fun encode(project: EditorProject): String = WorkJson.encodeToString(ProjectFileDto(project = project.toDto()))

/** Current time in milliseconds since 1970-01-01 UTC. */
@OptIn(ExperimentalTime::class)
private fun now(): Long = Clock.System.now().toEpochMilliseconds()

/** Summary of the project as the work [id], changed at [time]. A project without clips has a black card. */
private fun EditorProject.summary(id: String, time: Long): VideoSummary = VideoSummary(
    id = id,
    name = name,
    duration = totalDuration,
    thumbnailHue = clips.firstOrNull()?.thumbnailHue,
    modifiedAt = time,
    thumbnail = thumbnail(),
)

/** First frame of the project for its card (D52), or null when the first clip has no source file. */
private fun EditorProject.thumbnail(): WorkThumbnail? {
    val first = clips.firstOrNull() ?: return null
    val uri = first.sourceUri ?: return null
    return WorkThumbnail(uri = uri, type = first.type, time = first.inPoint)
}

/** Locations of the source files of the clips. */
private fun EditorProject.mediaUris(): Set<String> = clips.mapNotNull { it.sourceUri }.toSet()

/** The project with each source location changed by [transform]. */
private fun EditorProject.mapMedia(transform: (String) -> String): EditorProject =
    if (clips.none { it.sourceUri != null }) this
    else copy(clips = clips.map { clip -> clip.sourceUri?.let { clip.copy(sourceUri = transform(it)) } ?: clip })
