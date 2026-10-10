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
import com.company.ui.navigation.onlyWhenResumed
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
        // 매직 처리가 끝나면 앱이 직접 편집기로 이동한다. 사용자 입력이 아니므로 onlyWhenResumed로 감싸지 않는다.
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
