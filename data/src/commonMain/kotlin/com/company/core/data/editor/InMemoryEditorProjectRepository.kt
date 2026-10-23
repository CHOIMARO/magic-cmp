package com.company.core.data.editor

import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.repository.EditorProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Keeps the current project and its undo history in memory. */
class InMemoryEditorProjectRepository : EditorProjectRepository {
    private val _project = MutableStateFlow(EditorProject(name = ""))
    override val project: StateFlow<EditorProject> = _project.asStateFlow()

    private val history = ArrayDeque<EditorProject>()
    private val _canUndo = MutableStateFlow(false)
    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    override fun start(project: EditorProject) {
        history.clear()
        _canUndo.value = false
        _project.value = project
    }

    override fun commit(transform: (EditorProject) -> EditorProject) {
        saveCheckpoint()
        _project.update(transform)
    }

    override fun saveCheckpoint() {
        history.addLast(_project.value)
        while (history.size > MAX_HISTORY) history.removeFirst()
        _canUndo.value = true
    }

    override fun update(transform: (EditorProject) -> EditorProject) {
        _project.update(transform)
    }

    override fun undo(): Boolean {
        val previous = history.removeLastOrNull() ?: return false
        _project.value = previous
        _canUndo.value = history.isNotEmpty()
        return true
    }

    private companion object {
        /** Number of undo steps to keep. */
        const val MAX_HISTORY = 40
    }
}
