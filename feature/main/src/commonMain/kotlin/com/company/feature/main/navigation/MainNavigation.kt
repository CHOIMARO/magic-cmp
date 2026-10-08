package com.company.feature.main.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.company.core.domain.model.editor.EditorTemplate
import com.company.feature.main.MainRoute
import com.company.ui.editor.GalleryMode
import kotlinx.serialization.Serializable

/** Route of the main tab screen. */
@Serializable
object MainRoute

/**
 * Adds the main tab screen to the graph.
 *
 * @param onOpenGallery Opens the video picker.
 * @param onOpenCamera Opens the camera.
 * @param onOpenEditor Opens the editor with the current project.
 */
fun NavGraphBuilder.mainScreen(
    onOpenGallery: (GalleryMode, EditorTemplate?) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    composable<MainRoute> {
        MainRoute(
            onOpenGallery = onOpenGallery,
            onOpenCamera = onOpenCamera,
            onOpenEditor = onOpenEditor,
        )
    }
}
