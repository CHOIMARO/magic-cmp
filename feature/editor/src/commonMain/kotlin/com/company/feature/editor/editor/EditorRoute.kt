package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [EditorViewModel] to [EditorScreen] and sends navigation events out.
 *
 * @param onBack Leaves the editor.
 * @param onAddClips Opens the video picker in append mode.
 * @param onExport Opens the export screen.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun EditorRoute(
    onBack: () -> Unit,
    onAddClips: () -> Unit,
    onExport: () -> Unit,
    viewModel: EditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EditorSideEffect.ShowToast -> toast.show(effect.message)
                EditorSideEffect.OpenGalleryToAppend -> onAddClips()
                EditorSideEffect.OpenExport -> onExport()
                EditorSideEffect.Close -> onBack()
            }
        }
    }

    EditorScreen(state = state, onIntent = { viewModel.postIntent(it) })
}
