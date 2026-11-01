package com.company.data.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Works of "최근 작업" and "내 영상" (F15). */
class InMemoryMyVideoRepositoryTest {

    private val repository = InMemoryMyVideoRepository(FakeCaptionRepository())
    private val projects = InMemoryEditorProjectRepository()
    private val clip = Clip(id = "c", name = "c", sourceDuration = 5.0, inPoint = 0.0, outPoint = 5.0, thumbnailHue = 80)

    /** Runs a suspend function that never suspends, without a coroutine library. */
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return assertNotNull(result).getOrThrow()
    }

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
        runSuspend { StartNewProjectUseCase(projects, repository)(EditorProject(name = "새 영상", clips = listOf(clip))) }
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
        runSuspend { StartNewProjectUseCase(projects, repository)(EditorProject(name = "새 영상", clips = listOf(clip))) }
        val id = assertNotNull(projects.project.value.workId)
        projects.commit { it.copy(clips = emptyList()) }
        assertFalse(runSuspend { SaveWorkUseCase(projects, repository)() })
        assertEquals(LeaveResult.REMOVED_EMPTY, runSuspend { LeaveEditorUseCase(projects, repository)() })
        assertTrue(repository.works.value.none { it.id == id })
        assertNull(runSuspend { repository.loadWork(id) })
    }

    @Test
    fun leaving_savesTheLastChange() {
        runSuspend { StartNewProjectUseCase(projects, repository)(EditorProject(name = "새 영상", clips = listOf(clip))) }
        projects.commit { it.copy(name = "바뀐 이름") }
        assertEquals(LeaveResult.KEPT, runSuspend { LeaveEditorUseCase(projects, repository)() })
        assertEquals("바뀐 이름", repository.works.value.first().name)
    }

    @Test
    fun markExported_keepsTheOrderAndTheTime() {
        val last = repository.works.value.last()
        runSuspend { repository.markExported(last.id) }
        assertEquals(last.copy(exported = true), repository.works.value.last())
    }

    @Test
    fun deleteWork_removesTheWork() {
        val first = repository.works.value.first()
        runSuspend { repository.deleteWork(first.id) }
        assertTrue(repository.works.value.none { it.id == first.id })
        assertFalse(runSuspend { OpenWorkUseCase(projects, repository)(first.id) })
    }
}
