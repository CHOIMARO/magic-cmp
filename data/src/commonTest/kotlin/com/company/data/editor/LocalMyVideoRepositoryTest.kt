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
    private fun newRepository() = LocalMyVideoRepository(store, persister, FakeCaptionRepository(), Dispatchers.Unconfined)

    /** Runs a suspend function that never suspends, without a coroutine library. */
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return assertNotNull(result).getOrThrow()
    }

    private fun startNew(project: EditorProject = EditorProject(name = "새 영상", clips = listOf(clip))) =
        runSuspend { StartNewProjectUseCase(projects, repository)(project) }

    @Test
    fun samples_showTheMostRecentChangeFirst_inOneList() {
        assertEquals(
            listOf("바닷가 주말", "말차 레시피", "벚꽃 산책", "작업실 투어", "첫 출근 브이로그", "러닝 크루", "고양이 낮잠"),
            repository.works.value.map { it.name },
        )
        val times = repository.works.value.map { it.modifiedAt }
        assertEquals(times.sortedDescending(), times)
        assertEquals(4, repository.works.value.count { it.exported })
        assertEquals(repository.works.value.size, repository.works.value.map { it.id }.toSet().size)
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
    fun saveWork_onlyAChangeMovesTheWorkToTheTop() {
        val third = repository.works.value[2]
        // 열어 보기만 하면 순서와 시각이 그대로다.
        assertTrue(runSuspend { OpenWorkUseCase(projects, repository)(third.id) })
        assertFalse(projects.canUndo.value)
        assertFalse(runSuspend { SaveWorkUseCase(projects, repository)() })
        assertEquals(third, repository.works.value[2])
        // 고치면 맨 앞으로 오고 길이와 시각이 바뀐다. 내보낸 적 있음 표시는 남는다.
        projects.commit { it.copy(clips = listOf(clip.copy(outPoint = 2.0, thumbnailHue = 150))) }
        assertTrue(runSuspend { SaveWorkUseCase(projects, repository)() })
        val saved = repository.works.value.first()
        assertEquals(third.id, saved.id)
        // 샘플의 글자와 자막은 남아 있으므로 영상 길이는 가장 늦게 끝나는 요소까지다(F16).
        assertEquals(projects.project.value.totalDuration, saved.duration)
        assertEquals(150, saved.thumbnailHue)
        assertTrue(saved.modifiedAt >= third.modifiedAt)
        assertEquals(third.exported, saved.exported)
        // 다시 열면 마지막 저장 상태다.
        assertEquals(projects.project.value, runSuspend { repository.loadWork(third.id) })
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
        val index = repository.works.value.indexOfLast { !it.exported }
        val work = repository.works.value[index]
        runSuspend { repository.markExported(work.id) }
        assertEquals(work.copy(exported = true), repository.works.value[index])
        assertEquals(work.copy(exported = true), newRepository().works.value[index])
    }

    @Test
    fun deleteWork_removesTheWork() {
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
        val sample = repository.works.value[3]
        val before = repository.works.value

        val restarted = newRepository()
        assertEquals(before, restarted.works.value)
        val id = before.first().id
        assertEquals(project.copy(workId = id), runSuspend { restarted.loadWork(id) })
        // 고치지 않은 샘플은 다시 켜도 샘플 프로젝트를 연다.
        assertEquals(sample.name, runSuspend { restarted.loadWork(sample.id) }?.name)
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
    fun sourceFiles_keepTheirAccess_andDeleteGivesItUp() {
        val video = clip.copy(sourceUri = "picked/a.mp4")
        startNew(EditorProject(name = "새 영상", clips = listOf(video, video.copy(id = "d"))))
        val id = assertNotNull(projects.project.value.workId)
        assertEquals(listOf("picked/a.mp4"), persister.persisted)
        // 편집기는 고른 위치를 계속 쓰지만, 바뀐 것이 없으면 다시 저장하지 않는다.
        assertFalse(runSuspend { SaveWorkUseCase(projects, repository)() })

        val restarted = newRepository()
        val loaded = assertNotNull(runSuspend { restarted.loadWork(id) })
        assertEquals(listOf("current/a.mp4", "current/a.mp4"), loaded.clips.map { it.sourceUri })

        runSuspend { restarted.deleteWork(id) }
        assertEquals(listOf("current/a.mp4"), persister.released)
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
}

/** Moves "picked/" files to "saved/", and finds them at "current/" after a restart. */
private class FakeMediaPersister : MediaPersister {
    val persisted = mutableListOf<String>()
    val released = mutableListOf<String>()

    override fun persist(uri: String): String {
        persisted += uri
        return uri.replace("picked/", "saved/")
    }

    override fun restore(uri: String): String = uri.replace("saved/", "current/")

    override fun release(uri: String) {
        released += uri
    }
}
