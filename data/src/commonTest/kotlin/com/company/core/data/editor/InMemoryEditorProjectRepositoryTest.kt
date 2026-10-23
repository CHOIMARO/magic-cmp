package com.company.core.data.editor

import com.company.core.domain.model.editor.EditorProject
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
    fun start_clearsHistory() {
        val repository = InMemoryEditorProjectRepository()
        repository.commit { it.copy(name = "x") }
        repository.start(EditorProject(name = "new"))
        assertFalse(repository.canUndo.value)
    }
}
