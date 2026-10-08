package com.company.feature.editor.magic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Connects [MagicViewModel] to [MagicScreen] and sends navigation events out.
 *
 * @param args Arguments of the screen.
 * @param onOpenEditor Opens the editor with the decorated project.
 */
@Composable
internal fun MagicRoute(
    args: MagicArgs,
    onOpenEditor: () -> Unit,
    viewModel: MagicViewModel = koinViewModel { parametersOf(args) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is MagicSideEffect.OpenEditor -> {
                    toast.show(effect.message, durationMillis = 2600)
                    onOpenEditor()
                }
            }
        }
    }

    MagicScreen(state = state)
}
