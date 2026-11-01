package com.company.data.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.ClipTransition
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.Dubbing
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.MusicRange
import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.SoundEffectClip
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextLoop
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.TransitionType
import com.company.domain.model.editor.WorkThumbnail
import com.company.domain.usecase.editor.CheckMediaStorageUseCase
import com.company.domain.usecase.editor.LeaveEditorUseCase
import com.company.domain.usecase.editor.LeaveResult
import com.company.domain.usecase.editor.OpenWorkUseCase
import com.company.domain.usecase.editor.SaveWorkUseCase
import com.company.domain.usecase.editor.StartNewProjectUseCase
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers

/** Works of "최근 작업" and "내 영상", kept on the device (F15). */
class LocalMyVideoRepositoryTest {

    private val store = MemoryWorkFileStore()
    private val persister = FakeMediaPersister()
    private val repository = newRepository()
    private val projects = InMemoryEditorProjectRepository()
    private val clip = Clip(id = "c", name = "c", sourceDuration = 5.0, inPoint = 0.0, outPoint = 5.0, thumbnailHue = 80)

    /** A repository that reads the same files, as after an app restart. */
    private fun newRepository() = LocalMyVideoRepository(store, persister, Dispatchers.Unconfined)

    /** Runs a suspend function that never suspends, without a coroutine library. */
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return assertNotNull(result).getOrThrow()
    }

    private fun startNew(project: EditorProject = EditorProject(name = "새 영상", clips = listOf(clip))) =
        runSuspend { StartNewProjectUseCase(projects, repository)(project) }

    @Test
    fun firstStart_hasNoWorks() {
        // 목록에는 사용자가 만든 작업만 있다. 샘플 작업은 넣지 않는다.
        assertTrue(repository.works.value.isEmpty())
        assertTrue(runSuspend { repository.workNames() }.isEmpty())
    }

    @Test
    fun sampleWorksOfAnOlderVersion_areLeftOut() {
        startNew()
        val mine = repository.works.value.first()
        // 예전 버전은 처음 켤 때 샘플 작업을 목록에 넣었다.
        val sample = "{\"id\":\"s1\",\"name\":\"바닷가 주말\",\"duration\":18.0,\"thumbnailHue\":35,\"modifiedAt\":1,\"sample\":true},"
        store.files["works.json"] = store.files.getValue("works.json").replace("\"works\":[", "\"works\":[$sample")

        val restarted = newRepository()
        assertEquals(listOf(mine.id), restarted.works.value.map { it.id })
        // 다음에 목록을 쓰면 파일에서도 빠진다.
        runSuspend { restarted.markExported(mine.id) }
        assertFalse(store.files.getValue("works.json").contains("바닷가 주말"))
    }

    @Test
    fun newProject_isSavedAtOnce_asTheFirstWork() {
        startNew()
        val work = repository.works.value.first()
        assertEquals("새 영상", work.name)
        assertEquals(5.0, work.duration)
        assertEquals(80, work.thumbnailHue)
        assertEquals(work.id, projects.project.value.workId)
        assertEquals(projects.project.value, runSuspend { repository.loadWork(work.id) })
    }

    @Test
    fun newWorks_withTheSameBaseName_getNumbers() {
        startNew(EditorProject(name = "새 작업", clips = listOf(clip)))
        startNew(EditorProject(name = "새 작업", clips = listOf(clip)))
        assertEquals("새 작업 2", projects.project.value.name)
        // 다시 켜도 저장된 이름을 보고 번호를 고른다.
        runSuspend { StartNewProjectUseCase(projects, newRepository())(EditorProject(name = "새 작업", clips = listOf(clip))) }
        assertEquals("새 작업 3", projects.project.value.name)
        assertEquals(listOf("새 작업 2", "새 작업"), repository.works.value.take(2).map { it.name })
    }

    @Test
    fun cardThumbnail_isTheFirstFrameOfTheFirstClip() {
        // 원본 파일이 없는 클립은 썸네일이 없고, 카드는 색 띠를 보여 준다.
        startNew(EditorProject(name = "새 작업", clips = listOf(clip)))
        assertNull(repository.works.value.first().thumbnail)

        val video = clip.copy(sourceUri = "picked/a.mp4", inPoint = 1.5)
        startNew(EditorProject(name = "새 작업", clips = listOf(video, clip.copy(id = "d", sourceUri = "picked/b.mp4"))))
        // 복사가 끝나면 첫 클립 복사본의 시작 지점을 가리킨다(D52).
        assertEquals(WorkThumbnail("saved/a.mp4", MediaType.VIDEO, 1.5), repository.works.value.first().thumbnail)
        // 첫 클립의 시작 지점을 자르면 다음 저장에서 바뀐다.
        projects.commit { project -> project.copy(clips = project.clips.map { if (it.id == "c") it.copy(inPoint = 2.0) else it }) }
        assertTrue(runSuspend { SaveWorkUseCase(projects, repository)() })
        assertEquals(2.0, repository.works.value.first().thumbnail?.time)
        // 다시 켜면 지금 위치의 복사본을 가리킨다.
        assertEquals(WorkThumbnail("current/a.mp4", MediaType.VIDEO, 2.0), newRepository().works.value.first().thumbnail)
    }

    @Test
    fun saveWork_onlyAChangeMovesTheWorkToTheTop() {
        startNew(EditorProject(name = "첫째", clips = listOf(clip)))
        runSuspend { repository.markExported(repository.works.value.first().id) }
        startNew(EditorProject(name = "둘째", clips = listOf(clip)))
        val older = repository.works.value[1]
        // 열어 보기만 하면 순서와 시각이 그대로다.
        assertTrue(runSuspend { OpenWorkUseCase(projects, repository)(older.id) })
        assertFalse(projects.canUndo.value)
        assertFalse(runSuspend { SaveWorkUseCase(projects, repository)() })
        assertEquals(older, repository.works.value[1])
        // 고치면 맨 앞으로 오고 길이와 시각이 바뀐다. 내보낸 적 있음 표시는 남는다.
        projects.commit { it.copy(clips = listOf(clip.copy(outPoint = 2.0, thumbnailHue = 150))) }
        assertTrue(runSuspend { SaveWorkUseCase(projects, repository)() })
        val saved = repository.works.value.first()
        assertEquals(older.id, saved.id)
        assertEquals(2.0, saved.duration)
        assertEquals(150, saved.thumbnailHue)
        assertTrue(saved.modifiedAt >= older.modifiedAt)
        assertTrue(saved.exported)
        // 다시 열면 마지막 저장 상태다.
        assertEquals(projects.project.value, runSuspend { repository.loadWork(older.id) })
    }

    @Test
    fun emptyProject_isNotSaved_andLeavingRemovesTheWork() {
        startNew()
        val id = assertNotNull(projects.project.value.workId)
        projects.commit { it.copy(clips = emptyList()) }
        assertFalse(runSuspend { SaveWorkUseCase(projects, repository)() })
        assertEquals(LeaveResult.REMOVED_EMPTY, runSuspend { LeaveEditorUseCase(projects, repository)() })
        assertTrue(repository.works.value.none { it.id == id })
        assertNull(runSuspend { repository.loadWork(id) })
        assertNull(store.files["work-$id.json"])
    }

    @Test
    fun leaving_savesTheLastChange() {
        startNew()
        projects.commit { it.copy(name = "바뀐 이름") }
        assertEquals(LeaveResult.KEPT, runSuspend { LeaveEditorUseCase(projects, repository)() })
        assertEquals("바뀐 이름", repository.works.value.first().name)
    }

    @Test
    fun markExported_keepsTheOrderAndTheTime() {
        startNew(EditorProject(name = "첫째", clips = listOf(clip)))
        startNew(EditorProject(name = "둘째", clips = listOf(clip)))
        val older = repository.works.value[1]
        runSuspend { repository.markExported(older.id) }
        assertEquals(older.copy(exported = true), repository.works.value[1])
        assertEquals(older.copy(exported = true), newRepository().works.value[1])
    }

    @Test
    fun deleteWork_removesTheWork() {
        startNew()
        val first = repository.works.value.first()
        runSuspend { repository.deleteWork(first.id) }
        assertTrue(repository.works.value.none { it.id == first.id })
        assertFalse(runSuspend { OpenWorkUseCase(projects, repository)(first.id) })
        assertTrue(newRepository().works.value.none { it.id == first.id })
    }

    // =====================================================================
    // Device storage
    // =====================================================================

    @Test
    fun restart_readsTheSameWorksAndProjects() {
        val project = fullProject()
        startNew(project)
        startNew()
        val before = repository.works.value

        val restarted = newRepository()
        assertEquals(before, restarted.works.value)
        val id = before[1].id
        assertEquals(project.copy(workId = id), runSuspend { restarted.loadWork(id) })
    }

    @Test
    fun firstStart_writesNoFile_untilAChange() {
        assertTrue(store.files.isEmpty())
        startNew()
        assertEquals(setOf("works.json", "work-${projects.project.value.workId}.json"), store.files.keys)
    }

    @Test
    fun brokenListFile_givesNoWorks() {
        store.files["works.json"] = "{ broken"
        assertTrue(newRepository().works.value.isEmpty())
    }

    @Test
    fun failedWrite_keepsTheWork_andTheNextSaveWritesIt() {
        store.failWrites = true
        startNew()
        val id = assertNotNull(projects.project.value.workId)
        assertEquals(id, repository.works.value.first().id)
        assertTrue(store.files.isEmpty())
        // 바뀐 것이 없어도 아직 파일에 없으므로 다시 쓴다. 쓰지 못하면 편집기에 알린다.
        assertFailsWith<IllegalStateException> { runSuspend { SaveWorkUseCase(projects, repository)() } }
        store.failWrites = false
        assertTrue(runSuspend { SaveWorkUseCase(projects, repository)() })
        assertEquals(projects.project.value, runSuspend { newRepository().loadWork(id) })
    }

    @Test
    fun failedDelete_keepsTheWork() {
        startNew()
        val id = assertNotNull(projects.project.value.workId)
        store.failWrites = true
        assertFailsWith<IllegalStateException> { runSuspend { repository.deleteWork(id) } }
        assertEquals(id, repository.works.value.first().id)
    }

    @Test
    fun sourceFiles_areCopied_andDeleteRemovesTheCopy() {
        val video = clip.copy(sourceUri = "picked/a.mp4")
        startNew(EditorProject(name = "새 영상", clips = listOf(video, video.copy(id = "d"))))
        val id = assertNotNull(projects.project.value.workId)
        // 같은 원본은 한 번만 복사하고, 복사가 끝나면 저장한 프로젝트가 복사본을 쓴다(D50).
        assertEquals(listOf("picked/a.mp4"), persister.persisted)
        assertTrue(store.files.getValue("work-$id.json").contains("saved/a.mp4"))
        // 편집기는 고른 위치를 계속 쓰지만, 바뀐 것이 없으면 다시 저장하지 않는다.
        assertFalse(runSuspend { SaveWorkUseCase(projects, repository)() })

        val restarted = newRepository()
        val loaded = assertNotNull(runSuspend { restarted.loadWork(id) })
        assertEquals(listOf("current/a.mp4", "current/a.mp4"), loaded.clips.map { it.sourceUri })

        runSuspend { restarted.deleteWork(id) }
        assertEquals(listOf("current/a.mp4"), persister.released)
    }

    @Test
    fun sourceFileOfAnOldWork_isCopiedWhenTheUserPicksItAgain() {
        // 복사하지 못한 예전 작업은 원본 위치를 가리킨다.
        persister.copyFails = true
        startNew(EditorProject(name = "예전 작업", clips = listOf(clip.copy(sourceUri = "picked/a.mp4"))))
        val oldId = assertNotNull(projects.project.value.workId)
        persister.copyFails = false

        val restarted = newRepository()
        runSuspend { StartNewProjectUseCase(projects, restarted)(EditorProject(name = "새 영상", clips = listOf(clip.copy(sourceUri = "picked/a.mp4")))) }
        val newId = assertNotNull(projects.project.value.workId)
        // 원본을 이미 복사한 것으로 보지 않고 다시 복사한다. 같은 원본을 쓰는 예전 작업도 복사본을 쓴다.
        assertEquals(listOf("picked/a.mp4", "picked/a.mp4"), persister.persisted)
        assertTrue(store.files.getValue("work-$newId.json").contains("saved/a.mp4"))
        assertTrue(store.files.getValue("work-$oldId.json").contains("saved/a.mp4"))
    }

    // =====================================================================
    // Storage space (D51)
    // =====================================================================

    @Test
    fun storageBytes_countTheCopiesAndTheProjectFile() {
        persister.sizes += mapOf("saved/a.mp4" to 5_000_000L, "current/a.mp4" to 5_000_000L)
        startNew(EditorProject(name = "새 영상", clips = listOf(clip.copy(sourceUri = "picked/a.mp4"))))
        val bytes = repository.works.value.first().storageBytes
        assertTrue(bytes in 5_000_001..5_010_000, "bytes = $bytes")
        // 다시 켜면 파일에서 다시 센다.
        assertTrue(newRepository().works.value.first().storageBytes in 5_000_001..5_010_000)
    }

    @Test
    fun checkMediaStorage_keepsTheMargin_andCountsOnlyNewFiles() {
        persister.free = 600_000_000L
        persister.sizes += mapOf("picked/b.mp4" to 50_000_000L, "picked/c.mp4" to 150_000_000L)
        val check = CheckMediaStorageUseCase(repository)
        assertTrue(runSuspend { check(listOf("picked/b.mp4")) })
        // 600MB - 150MB = 450MB는 여유 500MB보다 작다.
        assertFalse(runSuspend { check(listOf("picked/c.mp4")) })
        // 이미 복사한 파일은 공간이 더 필요하지 않다.
        startNew(EditorProject(name = "새 영상", clips = listOf(clip.copy(sourceUri = "picked/b.mp4"))))
        assertEquals(0L, runSuspend { repository.bytesToCopy(listOf("picked/b.mp4")) })
        assertEquals(150_000_000L, runSuspend { repository.bytesToCopy(listOf("picked/b.mp4", "picked/c.mp4", "picked/c.mp4")) })
    }

    @Test
    fun startup_removesTheFilesThatNoWorkUses() {
        startNew(EditorProject(name = "새 영상", clips = listOf(clip.copy(sourceUri = "picked/a.mp4"))))
        val id = assertNotNull(projects.project.value.workId)
        store.files["work-gone.json"] = "{}"
        store.files["works.json.part"] = "{"

        newRepository()
        assertEquals(setOf("works.json", "work-$id.json"), store.files.keys)
        assertEquals(setOf("current/a.mp4"), persister.kept)
    }

    @Test
    fun unreadableProject_keepsAllCopies() {
        startNew(EditorProject(name = "새 영상", clips = listOf(clip.copy(sourceUri = "picked/a.mp4"))))
        val id = assertNotNull(projects.project.value.workId)
        store.files["work-$id.json"] = "{ broken"

        newRepository()
        // 모르는 복사본을 지우지 않도록 복사본 정리를 건너뛴다. 목록에 있는 프로젝트 파일도 남는다.
        assertNull(persister.kept)
        assertTrue("work-$id.json" in store.files)
    }

    /** A project with each kind of element, so that the test finds a field that the file does not keep. */
    private fun fullProject(): EditorProject {
        val decoration = DecorationRange(0, 2, Decoration(color = 0xFF00FF, bold = true, outline = true, shadow = true, background = 0x112233))
        return EditorProject(
            name = "전체",
            clips = listOf(
                clip.copy(speed = 1.5, sourceUri = "file:///a.mp4", volume = 40),
                clip.copy(id = "p", type = MediaType.PHOTO, inPoint = 0.25, outPoint = 3.75),
            ),
            layout = LayoutType.SPLIT_VERTICAL,
            captionStyle = CaptionStyleType.BOX,
            texts = listOf(
                TextOverlay(
                    id = "t", text = "안녕", start = 0.1, length = 2.2, style = TextStyleType.NEON, x = 12.5, y = 80.0,
                    layer = 1, font = TextFontType.JUA, scale = 1.3, rotation = -12.0, sizeStep = 4,
                    decorations = listOf(decoration), entrance = TextEntrance.TYPING, entranceMillis = 700,
                    exit = TextExit.FLY_UP, exitMillis = 400, loop = TextLoop.WAVE,
                )
            ),
            musicId = "m1",
            captions = listOf(
                CaptionSegment(id = "s1", text = "말", start = 0.0, length = 1.0, decorations = listOf(decoration), clipId = "c", sourceStart = 0.0, sourceEnd = 1.0),
                CaptionSegment(id = "s2", text = "글", start = 9.0, length = 2.0),
            ),
            captionPosition = 62.0,
            musicRange = MusicRange(1.0, 7.5),
            musicFade = false,
            musicVolume = 30,
            dubbings = listOf(
                Dubbing(
                    id = "d1", text = "더빙", voiceId = "v", speed = DubbingSpeed.FAST, start = 1.0, length = 2.0,
                    layer = 1, volume = 70, captionId = "s2", rate = 1.2, audioUri = "file:///v.m4a",
                )
            ),
            soundEffects = listOf(SoundEffectClip(id = "e1", effectId = "pop", name = "뿅", start = 2.0, length = 0.5, layer = 2, volume = 90)),
            transitions = mapOf("c" to ClipTransition(TransitionType.PUSH, PushDirection.UP)),
            openingTransition = ClipTransition(TransitionType.FADE),
        )
    }
}

/** Work files in memory. */
private class MemoryWorkFileStore : WorkFileStore {
    val files = mutableMapOf<String, String>()

    /** When true, each write fails, as on a full storage. */
    var failWrites = false

    override fun read(fileName: String): String? = files[fileName]

    override fun write(fileName: String, text: String) {
        check(!failWrites) { "Storage is full" }
        files[fileName] = text
    }

    override fun delete(fileName: String) {
        files.remove(fileName)
    }

    override fun list(): List<String> = files.keys.toList()
}

/** Copies "picked/" files to "saved/", and finds them at "current/" after a restart. */
private class FakeMediaPersister : MediaPersister {
    val persisted = mutableListOf<String>()
    val released = mutableListOf<String>()

    /** Size of each file. Other files have the size 0. */
    val sizes = mutableMapOf<String, Long>()

    /** Free bytes on the device. */
    var free = Long.MAX_VALUE

    /** Copies that the last clean-up kept, or null before a clean-up. */
    var kept: Set<String>? = null

    /** When true, each copy fails, and the work keeps the source location. */
    var copyFails = false

    override fun isCopy(uri: String): Boolean = uri.startsWith("saved/") || uri.startsWith("current/")

    override fun sizeOf(uri: String): Long = sizes[uri] ?: 0

    override fun freeBytes(): Long = free

    override fun cleanUp(keep: Set<String>) {
        kept = keep
    }

    override fun persist(uri: String): String {
        persisted += uri
        return if (copyFails) uri else uri.replace("picked/", "saved/")
    }

    override fun restore(uri: String): String = uri.replace("saved/", "current/")

    override fun release(uri: String) {
        released += uri
    }
}
