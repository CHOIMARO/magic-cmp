package com.company.core.domain.repository

import com.company.core.domain.model.editor.EditorSettings
import kotlinx.coroutines.flow.StateFlow

/** Holds the editor settings of the user. */
interface EditorSettingsRepository {
    /** Current settings. */
    val settings: StateFlow<EditorSettings>

    /**
     * Changes the settings.
     *
     * @param transform Change to apply.
     */
    fun update(transform: (EditorSettings) -> EditorSettings)
}
