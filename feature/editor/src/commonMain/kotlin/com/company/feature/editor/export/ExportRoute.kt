package com.company.feature.editor.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [ExportViewModel] to [ExportScreen] and sends navigation events out.
 *
 * @param onBack Goes back to the editor.
 * @param onGoHome Navigates to the home tab.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun ExportRoute(
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: ExportViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                ExportSideEffect.Close -> onBack()
                ExportSideEffect.GoHome -> onGoHome()
            }
        }
    }

    ExportScreen(state = state, onIntent = { viewModel.postIntent(it) })
}
