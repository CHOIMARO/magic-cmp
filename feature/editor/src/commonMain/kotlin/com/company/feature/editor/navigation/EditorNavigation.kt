package com.company.feature.editor.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.company.feature.editor.editor.EditorRoute
import com.company.feature.editor.export.ExportRoute
import com.company.ui.navigation.onlyWhenResumed
import kotlinx.serialization.Serializable

/** Route of the editor screen. */
@Serializable
object EditorRoute

/**
 * Route of the export screen.
 *
 * @property fromList True when the work menu of the tab opened the screen without the editor (S13).
 */
@Serializable
data class ExportRoute(val fromList: Boolean = false)

/** Route of the screen of the user's own voice. */
@Serializable
object MyVoiceRoute

/**
 * Adds the editor screen to the graph.
 *
 * @param onBack Leaves the editor.
 * @param onAddClips Opens the video picker in append mode.
 * @param onExport Opens the export screen.
 * @param onOpenMyVoice Opens the screen of the user's own voice.
 */
fun NavGraphBuilder.editorScreen(onBack: () -> Unit, onAddClips: () -> Unit, onExport: () -> Unit, onOpenMyVoice: () -> Unit) {
    composable<EditorRoute> { entry ->
        EditorRoute(
            onBack = entry.onlyWhenResumed(onBack),
            onAddClips = entry.onlyWhenResumed(onAddClips),
            onExport = entry.onlyWhenResumed(onExport),
            onOpenMyVoice = entry.onlyWhenResumed(onOpenMyVoice),
        )
    }
}

/**
 * Adds the screen of the user's own voice to the graph. The main tab and the dubbing sheet open it.
 *
 * @param onClose Leaves the screen.
 */
fun NavGraphBuilder.myVoiceScreen(onClose: () -> Unit) {
    composable<MyVoiceRoute> { entry ->
        com.company.feature.editor.voice.MyVoiceRoute(onClose = entry.onlyWhenResumed(onClose))
    }
}

/**
 * Adds the export screen to the graph.
 *
 * @param onBack Goes back to the screen before, the editor or the tab.
 * @param onOpenEditor Opens the editor in place of the export screen. "더 편집하기" uses it when the work menu opened
 *   the screen. Otherwise "더 편집하기" goes back to the editor.
 * @param onGoHome Navigates to the home tab.
 */
fun NavGraphBuilder.exportScreen(onBack: () -> Unit, onOpenEditor: () -> Unit, onGoHome: () -> Unit) {
    composable<ExportRoute> { entry ->
        val fromList = entry.toRoute<ExportRoute>().fromList
        ExportRoute(
            onBack = entry.onlyWhenResumed(onBack),
            onEditMore = entry.onlyWhenResumed(if (fromList) onOpenEditor else onBack),
            onGoHome = entry.onlyWhenResumed(onGoHome),
        )
    }
}
