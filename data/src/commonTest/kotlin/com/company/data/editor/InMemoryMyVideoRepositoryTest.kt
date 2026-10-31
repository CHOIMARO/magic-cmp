package com.company.data.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.usecase.editor.StartNewProjectUseCase
import com.company.domain.usecase.editor.UpdateDraftUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryMyVideoRepositoryTest {

    private val repository = InMemoryMyVideoRepository(FakeCaptionRepository())

    @Test
    fun samples_showTheNewestFirst() {
        assertEquals(listOf("바닷가 주말", "말차 레시피", "작업실 투어", "러닝 크루"), repository.drafts.value.map { it.name })
        assertEquals(listOf("벚꽃 산책", "첫 출근 브이로그", "고양이 낮잠"), repository.completedVideos.value.map { it.name })
        val times = repository.drafts.value.map { it.createdAt }
        assertEquals(times.sortedDescending(), times)
        assertEquals(repository.drafts.value.size, repository.drafts.value.map { it.id }.toSet().size)
    }

    @Test
    fun addDraft_putsTheDraftFirst_withTheCurrentTime() {
        val newest = repository.drafts.value.first()
        val draft = repository.addDraft("새 영상", 6.5, 120)
        assertEquals(draft, repository.drafts.value.first())
        assertTrue(draft.createdAt >= newest.createdAt)
        assertTrue(repository.drafts.value.drop(1).none { it.id == draft.id })
    }

    @Test
    fun updateDraft_changesOnlyThatDraft_andKeepsItsPlace() {
        val draft = repository.addDraft("새 영상", 6.5, 120)
        repository.addDraft("새 촬영", 3.0, 40)
        repository.updateDraft(draft.id, 9.0, 200)
        assertEquals(draft.copy(duration = 9.0, thumbnailHue = 200), repository.drafts.value[1])
        // 색이 없으면 원래 색을 지킨다. 없는 ID는 아무것도 바꾸지 않는다.
        repository.updateDraft(draft.id, 4.0, null)
        assertEquals(200, repository.drafts.value[1].thumbnailHue)
        val before = repository.drafts.value
        repository.updateDraft("unknown", 1.0, 1)
        assertEquals(before, repository.drafts.value)
    }

    @Test
    fun addCompletedVideo_putsTheVideoFirst() {
        repository.addCompletedVideo("바닷가 주말", 18.0, 35)
        assertEquals("바닷가 주말", repository.completedVideos.value.first().name)
        assertEquals(4, repository.completedVideos.value.size)
    }

    @Test
    fun newProject_becomesADraft_andTheDraftFollowsTheProjectWhenTheUserLeaves() {
        val projects = InMemoryEditorProjectRepository()
        val clip = Clip(id = "c", name = "c", sourceDuration = 5.0, inPoint = 0.0, outPoint = 5.0, thumbnailHue = 80)
        StartNewProjectUseCase(projects, repository)(EditorProject(name = "새 영상", clips = listOf(clip)))

        val draft = repository.drafts.value.first()
        assertEquals("새 영상", draft.name)
        assertEquals(5.0, draft.duration)
        assertEquals(80, draft.thumbnailHue)
        assertEquals(draft.id, assertNotNull(projects.project.value.draftId))

        projects.commit { it.copy(clips = listOf(clip.copy(outPoint = 2.0, thumbnailHue = 150))) }
        UpdateDraftUseCase(projects, repository)()
        assertEquals(2.0, repository.drafts.value.first().duration)
        assertEquals(150, repository.drafts.value.first().thumbnailHue)
    }

    @Test
    fun openedDraft_doesNotChangeTheList() {
        val projects = InMemoryEditorProjectRepository()
        projects.start(EditorProject(name = "바닷가 주말"))
        assertNull(projects.project.value.draftId)
        val before = repository.drafts.value
        UpdateDraftUseCase(projects, repository)()
        assertEquals(before, repository.drafts.value)
    }
}
