package com.company.data.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryEditorProjectRepositoryTest {

    @Test
    fun commitAndUndo_restoresPreviousProject() {
        val repository = InMemoryEditorProjectRepository()
        repository.start(EditorProject(name = "a"))
        assertFalse(repository.canUndo.value)

        repository.commit { it.copy(name = "b") }
        assertTrue(repository.canUndo.value)

        assertTrue(repository.undo())
        assertEquals("a", repository.project.value.name)
        assertFalse(repository.canUndo.value)
        assertFalse(repository.undo())
    }

    @Test
    fun checkpointAndUpdate_makeOneUndoStepForADrag() {
        val repository = InMemoryEditorProjectRepository()
        repository.start(EditorProject(name = "start"))

        repository.saveCheckpoint()
        repeat(5) { index -> repository.update { it.copy(name = "drag $index") } }

        assertTrue(repository.undo())
        assertEquals("start", repository.project.value.name)
        assertFalse(repository.canUndo.value)
    }

    @Test
    fun rename_addsNoUndoStep_andAnUndoKeepsTheNewName() {
        val repository = InMemoryEditorProjectRepository()
        val clip = Clip(id = "c", name = "c", sourceDuration = 5.0, inPoint = 0.0, outPoint = 5.0, thumbnailHue = 0)
        repository.start(EditorProject(name = "새 작업"))
        repository.commit { it.copy(clips = listOf(clip)) }

        repository.rename("바닷가")
        assertEquals("바닷가", repository.project.value.name)
        // 이름 바꾸기 전의 편집을 되돌려도 이름은 그대로다(D55).
        assertTrue(repository.undo())
        assertEquals(EditorProject(name = "바닷가"), repository.project.value)
        assertFalse(repository.canUndo.value)
    }

    @Test
    fun start_clearsHistory() {
        val repository = InMemoryEditorProjectRepository()
        repository.commit { it.copy(name = "x") }
        repository.start(EditorProject(name = "new"))
        assertFalse(repository.canUndo.value)
    }
}
