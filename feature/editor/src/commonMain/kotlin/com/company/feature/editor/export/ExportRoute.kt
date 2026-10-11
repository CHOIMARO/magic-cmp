package com.company.feature.editor.export

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [ExportViewModel] to [ExportScreen] and sends navigation events out.
 * It also draws the overlay images of the export when the ViewModel asks for them.
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
    val toast = LocalMagicToast.current
    val onIntent: (ExportIntent) -> Unit = remember(viewModel) { { viewModel.postIntent(it) } }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ExportSideEffect.ShowToast -> toast.show(effect.message)
                ExportSideEffect.Close -> onBack()
                ExportSideEffect.GoHome -> onGoHome()
            }
        }
    }

    Box {
        ExportScreen(state = state, onIntent = onIntent)
        // 글자·자막·레이아웃 이미지는 편집 화면과 같은 코드로 그려야 해서 화면 쪽에서 만든다.
        ExportOverlayRenderer(
            request = state.overlayRequest,
            onProgress = { onIntent(ExportIntent.OverlaysProgress(it)) },
            onRendered = { onIntent(ExportIntent.OverlaysReady(it)) },
        )
    }
}
