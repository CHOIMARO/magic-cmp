package com.company.feature.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.company.domain.model.editor.EditorTemplate
import com.company.feature.main.MainRoute
import com.company.ui.navigation.onlyWhenResumed
import kotlinx.serialization.Serializable

/** Route of the main tab screen. */
@Serializable
object MainRoute

/**
 * Adds the main tab screen to the graph.
 *
 * @param onOpenGallery Opens the photo and video picker for a new project with the template, or with no template.
 * @param onOpenCamera Opens the camera.
 * @param onOpenEditor Opens the editor with the current project.
 * @param onOpenExport Opens the export screen of the current project, without the editor (S13).
 * @param onOpenMyVoice Opens the screen of the user's own voice.
 * @param onOpenAccount Opens the account management screen (S15).
 */
fun NavGraphBuilder.mainScreen(
    onOpenGallery: (EditorTemplate?) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenMyVoice: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    composable<MainRoute> { entry ->
        MainRoute(
            onOpenGallery = entry.onlyWhenResumed(onOpenGallery),
            onOpenCamera = entry.onlyWhenResumed(onOpenCamera),
            onOpenEditor = entry.onlyWhenResumed(onOpenEditor),
            onOpenExport = entry.onlyWhenResumed(onOpenExport),
            onOpenMyVoice = entry.onlyWhenResumed(onOpenMyVoice),
            onOpenAccount = entry.onlyWhenResumed(onOpenAccount),
        )
    }
}
