package com.company.feature.capture.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.core.designsystem.component.LocalMagicToast
import com.company.core.domain.model.editor.EditorTemplate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Connects [GalleryViewModel] to [GalleryScreen] and sends navigation events out.
 *
 * On a device without the embedded picker, it opens the full-screen system picker when the screen opens.
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
    val useEmbeddedPicker = remember { isEmbeddedMediaPickerSupported() }
    val openSystemPicker = rememberMediaPickerLauncher { uris -> viewModel.postIntent(GalleryIntent.MediaAdded(uris)) }

    if (!useEmbeddedPicker) {
        // 화면을 다시 그리거나 회전해도 피커를 한 번만 자동으로 연다.
        var pickerOpened by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!pickerOpened) {
                pickerOpened = true
                openSystemPicker()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is GallerySideEffect.OpenMagic -> onOpenMagic(effect.template)
                GallerySideEffect.OpenEditor -> onOpenEditor()
                is GallerySideEffect.ReturnToEditor -> {
                    toast.show(effect.message)
                    onReturnToEditor()
                }
                GallerySideEffect.Close -> onClose()
            }
        }
    }

    GalleryScreen(
        state = state,
        useEmbeddedPicker = useEmbeddedPicker,
        onIntent = { viewModel.postIntent(it) },
        onOpenSystemPicker = openSystemPicker,
        onClose = onClose,
    )
}
