package com.company.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.domain.model.editor.EditorTemplate
import com.company.ui.editor.GalleryMode
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [MainViewModel] to [MainScreen] and sends navigation events out.
 *
 * @param onOpenGallery Opens the video picker.
 * @param onOpenCamera Opens the camera.
 * @param onOpenEditor Opens the editor with the current project.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun MainRoute(
    onOpenGallery: (GalleryMode, EditorTemplate?) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenEditor: () -> Unit,
    viewModel: MainViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is MainSideEffect.OpenGallery -> onOpenGallery(effect.mode, effect.template)
                MainSideEffect.OpenCamera -> onOpenCamera()
                MainSideEffect.OpenEditor -> onOpenEditor()
            }
        }
    }

    MainScreen(state = state, onIntent = { viewModel.postIntent(it) })
}
