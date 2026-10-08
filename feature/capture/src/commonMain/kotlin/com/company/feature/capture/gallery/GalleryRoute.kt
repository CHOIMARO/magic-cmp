package com.company.feature.capture.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import com.company.core.domain.model.editor.EditorTemplate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Connects [GalleryViewModel] to [GalleryScreen] and sends navigation events out.
 *
 * @param args Arguments of the picker.
 * @param onClose Goes back to the previous screen.
 * @param onOpenMagic Opens the magic screen with the template.
 * @param onOpenEditor Opens the editor with the new project.
 * @param onReturnToEditor Goes back to the editor after clips were added.
 */
@Composable
internal fun GalleryRoute(
    args: GalleryArgs,
    onClose: () -> Unit,
    onOpenMagic: (EditorTemplate?) -> Unit,
    onOpenEditor: () -> Unit,
    onReturnToEditor: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel { parametersOf(args) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GallerySideEffect.OpenMagic -> onOpenMagic(effect.template)
                GallerySideEffect.OpenEditor -> onOpenEditor()
                is GallerySideEffect.ReturnToEditor -> {
                    toast.show(effect.message)
                    onReturnToEditor()
                }
            }
        }
    }

    GalleryScreen(state = state, onIntent = { viewModel.postIntent(it) }, onClose = onClose)
}
