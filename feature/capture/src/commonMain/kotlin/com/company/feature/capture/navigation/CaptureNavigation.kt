package com.company.feature.capture.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.company.domain.model.editor.EditorTemplate
import com.company.feature.capture.camera.CameraRoute
import com.company.feature.capture.gallery.GalleryArgs
import com.company.feature.capture.gallery.GalleryRoute
import com.company.ui.editor.GalleryMode
import com.company.ui.navigation.onlyWhenResumed
import kotlinx.serialization.Serializable

/**
 * Route of the video picker.
 *
 * @property mode Name of a [GalleryMode].
 * @property templateKey Key of an [EditorTemplate], or null.
 */
@Serializable
data class GalleryRoute(val mode: String, val templateKey: String? = null)

/** Route of the camera screen. */
@Serializable
object CameraRoute

/**
 * Opens the video picker.
 *
 * @param mode Purpose of the picker.
 * @param template Template to apply to the new project, or null.
 */
fun NavController.navigateToGallery(mode: GalleryMode, template: EditorTemplate? = null) {
    navigate(GalleryRoute(mode = mode.name, templateKey = template?.key))
}

/** Opens the camera screen. */
fun NavController.navigateToCamera() {
    navigate(CameraRoute)
}

/**
 * Adds the video picker to the graph.
 *
 * @param onClose Goes back to the previous screen.
 * @param onOpenEditor Opens the editor with the new project.
 * @param onReturnToEditor Goes back to the editor after clips were added.
 */
fun NavGraphBuilder.galleryScreen(
    onClose: () -> Unit,
    onOpenEditor: () -> Unit,
    onReturnToEditor: () -> Unit,
) {
    composable<GalleryRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<GalleryRoute>()
        GalleryRoute(
            args = GalleryArgs(
                mode = GalleryMode.entries.find { it.name == route.mode } ?: GalleryMode.CREATE,
                template = EditorTemplate.fromKey(route.templateKey),
            ),
            onClose = backStackEntry.onlyWhenResumed(onClose),
            onOpenEditor = backStackEntry.onlyWhenResumed(onOpenEditor),
            onReturnToEditor = backStackEntry.onlyWhenResumed(onReturnToEditor),
        )
    }
}

/**
 * Adds the camera screen to the graph.
 *
 * @param onClose Goes back to the previous screen.
 * @param onOpenEditor Opens the editor with the new project.
 */
fun NavGraphBuilder.cameraScreen(onClose: () -> Unit, onOpenEditor: () -> Unit) {
    composable<CameraRoute> { entry ->
        CameraRoute(
            onClose = entry.onlyWhenResumed(onClose),
            onOpenEditor = entry.onlyWhenResumed(onOpenEditor),
        )
    }
}
