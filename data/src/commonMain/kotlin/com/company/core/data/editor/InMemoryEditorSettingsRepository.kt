package com.company.core.data.editor

import com.company.core.domain.model.editor.EditorSettings
import com.company.core.domain.repository.EditorSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Keeps the editor settings in memory. The settings reset when the app restarts. */
class InMemoryEditorSettingsRepository : EditorSettingsRepository {
    private val _settings = MutableStateFlow(EditorSettings())
    override val settings: StateFlow<EditorSettings> = _settings.asStateFlow()

    override fun update(transform: (EditorSettings) -> EditorSettings) {
        _settings.update(transform)
    }
}
