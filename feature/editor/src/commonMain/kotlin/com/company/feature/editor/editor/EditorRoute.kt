package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import com.company.ui.text.resolve
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects [EditorViewModel] to [EditorScreen] and sends navigation events out.
 * It also owns the preview player, because the player is a platform resource of the screen.
 *
 * @param onBack Leaves the editor.
 * @param onAddClips Opens the video picker in append mode.
 * @param onExport Opens the export screen.
 * @param onOpenMyVoice Opens the screen of the user's own voice.
 * @param viewModel ViewModel of the screen.
 */
@Composable
internal fun EditorRoute(
    onBack: () -> Unit,
    onAddClips: () -> Unit,
    onExport: () -> Unit,
    onOpenMyVoice: () -> Unit,
    viewModel: EditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val toast = LocalMagicToast.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is EditorSideEffect.ShowToast -> toast.show(effect.message.resolve())
                EditorSideEffect.OpenGalleryToAppend -> onAddClips()
                EditorSideEffect.OpenExport -> onExport()
                EditorSideEffect.OpenMyVoice -> onOpenMyVoice()
                EditorSideEffect.Close -> onBack()
                EditorSideEffect.Snapped -> haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
    }

    val onIntent: (EditorIntent) -> Unit = remember(viewModel) { { viewModel.postIntent(it) } }
    val player = rememberEditorPlayback(state = state, onIntent = onIntent)
    val video: (@Composable (Modifier) -> Unit)? = remember(player) {
        player?.let { { modifier -> it.VideoSurface(modifier) } }
    }
    EditorScreen(state = state, onIntent = onIntent, video = video)
}
