package com.company.feature.editor.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.company.core.domain.model.editor.EditorTemplate
import com.company.feature.editor.editor.EditorRoute
import com.company.feature.editor.export.ExportRoute
import com.company.feature.editor.magic.MagicArgs
import com.company.feature.editor.magic.MagicRoute
import kotlinx.serialization.Serializable

/**
 * Route of the magic screen.
 *
 * @property templateKey Key of an [EditorTemplate] to apply after the magic, or null.
 */
@Serializable
data class MagicRoute(val templateKey: String? = null)

/** Route of the editor screen. */
@Serializable
object EditorRoute

/** Route of the export screen. */
@Serializable
object ExportRoute

/**
 * Opens the magic screen. It decorates the clips of the current project.
 *
 * @param template Template to apply after the magic, or null.
 */
fun NavController.navigateToMagic(template: EditorTemplate?) {
    navigate(MagicRoute(templateKey = template?.key))
}

/**
 * Adds the magic screen to the graph.
 *
 * @param onOpenEditor Opens the editor with the decorated project.
 */
fun NavGraphBuilder.magicScreen(onOpenEditor: () -> Unit) {
    composable<MagicRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<MagicRoute>()
        MagicRoute(args = MagicArgs(template = EditorTemplate.fromKey(route.templateKey)), onOpenEditor = onOpenEditor)
    }
}

/**
 * Adds the editor screen to the graph.
 *
 * @param onBack Leaves the editor.
 * @param onAddClips Opens the video picker in append mode.
 * @param onExport Opens the export screen.
 */
fun NavGraphBuilder.editorScreen(onBack: () -> Unit, onAddClips: () -> Unit, onExport: () -> Unit) {
    composable<EditorRoute> {
        EditorRoute(onBack = onBack, onAddClips = onAddClips, onExport = onExport)
    }
}

/**
 * Adds the export screen to the graph.
 *
 * @param onBack Goes back to the editor.
 * @param onGoHome Navigates to the home tab.
 */
fun NavGraphBuilder.exportScreen(onBack: () -> Unit, onGoHome: () -> Unit) {
    composable<ExportRoute> {
        ExportRoute(onBack = onBack, onGoHome = onGoHome)
    }
}
