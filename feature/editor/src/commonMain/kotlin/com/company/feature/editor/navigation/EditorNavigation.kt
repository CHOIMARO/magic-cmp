package com.company.feature.editor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.company.feature.editor.editor.EditorRoute
import com.company.feature.editor.export.ExportRoute
import com.company.ui.navigation.onlyWhenResumed
import kotlinx.serialization.Serializable

/** Route of the editor screen. */
@Serializable
object EditorRoute

/** Route of the export screen. */
@Serializable
object ExportRoute

/**
 * Adds the editor screen to the graph.
 *
 * @param onBack Leaves the editor.
 * @param onAddClips Opens the video picker in append mode.
 * @param onExport Opens the export screen.
 */
fun NavGraphBuilder.editorScreen(onBack: () -> Unit, onAddClips: () -> Unit, onExport: () -> Unit) {
    composable<EditorRoute> { entry ->
        EditorRoute(
            onBack = entry.onlyWhenResumed(onBack),
            onAddClips = entry.onlyWhenResumed(onAddClips),
            onExport = entry.onlyWhenResumed(onExport),
        )
    }
}

/**
 * Adds the export screen to the graph.
 *
 * @param onBack Goes back to the editor.
 * @param onGoHome Navigates to the home tab.
 */
fun NavGraphBuilder.exportScreen(onBack: () -> Unit, onGoHome: () -> Unit) {
    composable<ExportRoute> { entry ->
        ExportRoute(onBack = entry.onlyWhenResumed(onBack), onGoHome = entry.onlyWhenResumed(onGoHome))
    }
}
