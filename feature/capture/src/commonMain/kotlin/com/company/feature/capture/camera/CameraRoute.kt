package com.company.feature.capture.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [CameraViewModel] to [CameraScreen] and sends navigation events out.
 *
 * @param onClose Goes back to the previous screen.
 * @param onOpenMagic Opens the magic screen with the new project.
 * @param onOpenEditor Opens the editor with the new project.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun CameraRoute(
    onClose: () -> Unit,
    onOpenMagic: () -> Unit,
    onOpenEditor: () -> Unit,
    viewModel: CameraViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                CameraSideEffect.OpenMagic -> onOpenMagic()
                CameraSideEffect.OpenEditor -> onOpenEditor()
            }
        }
    }

    CameraScreen(state = state, onIntent = { viewModel.postIntent(it) }, onClose = onClose)
}
