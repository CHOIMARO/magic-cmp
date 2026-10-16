package com.company.feature.editor.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import com.company.ui.text.resolve
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [MyVoiceViewModel] to [MyVoiceScreen] and sends navigation events out.
 *
 * @param onClose Leaves the screen.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun MyVoiceRoute(onClose: () -> Unit, viewModel: MyVoiceViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current
    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is MyVoiceSideEffect.ShowToast -> toast.show(effect.message.resolve())
                MyVoiceSideEffect.Close -> onClose()
            }
        }
    }
    MyVoiceScreen(state = state, onIntent = { viewModel.postIntent(it) })
}
