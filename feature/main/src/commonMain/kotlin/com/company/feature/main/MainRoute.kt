package com.company.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.domain.model.editor.EditorTemplate
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [MainViewModel] to [MainScreen] and sends navigation events out.
 *
 * @param onOpenGallery Opens the photo and video picker for a new project with the template, or with no template.
 * @param onOpenCamera Opens the camera.
 * @param onOpenEditor Opens the editor with the current project.
 * @param onOpenMyVoice Opens the screen of the user's own voice.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun MainRoute(
    onOpenGallery: (EditorTemplate?) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenMyVoice: () -> Unit,
    viewModel: MainViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is MainSideEffect.OpenGallery -> onOpenGallery(effect.template)
                MainSideEffect.OpenCamera -> onOpenCamera()
                MainSideEffect.OpenEditor -> onOpenEditor()
                MainSideEffect.OpenMyVoice -> onOpenMyVoice()
            }
        }
    }

    MainScreen(state = state, onIntent = { viewModel.postIntent(it) })
}
