package com.company.feature.capture.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Tells if the device can show the system picker inside the app screen.
 *
 * iOS can always show it. Android needs Android 14 and SDK Extensions 15 or later.
 */
internal expect fun isEmbeddedMediaPickerSupported(): Boolean

/**
 * Sends requests from the app UI to the [EmbeddedMediaPicker].
 *
 * The embedded picker keeps the selection. Thus the app asks the picker to remove an item,
 * and the picker then reports the new selection.
 */
internal class EmbeddedMediaPickerController {
    private val requests = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** URIs that the picker must deselect. */
    val deselectRequests: Flow<String> = requests

    /**
     * Asks the picker to deselect an item.
     *
     * @param uri URI that the picker reported for the item.
     */
    fun deselect(uri: String) {
        requests.tryEmit(uri)
    }
}

/**
 * System picker inside the app screen. It shows photos and videos. The OS draws it, and the app needs no permission.
 *
 * Call it only when [isEmbeddedMediaPickerSupported] is true.
 *
 * @param controller Sends deselect requests from the app UI to the picker.
 * @param onSelectionChange Called with the URIs of the picked items in pick order, each time the selection changes.
 * @param onSelectionComplete Called when the user taps the done action of the picker.
 * @param modifier Modifier for the picker.
 */
@Composable
internal expect fun EmbeddedMediaPicker(
    controller: EmbeddedMediaPickerController,
    onSelectionChange: (List<String>) -> Unit,
    onSelectionComplete: () -> Unit,
    modifier: Modifier = Modifier,
)

/**
 * Remembers an action that opens the full-screen system picker for photos and videos.
 *
 * Use it when [isEmbeddedMediaPickerSupported] is false. The picker does not keep the pick order.
 *
 * @param onResult Called with the URIs of the picked items. The list is empty when the user closes the picker.
 * @return Action that opens the picker.
 */
@Composable
internal expect fun rememberMediaPickerLauncher(onResult: (List<String>) -> Unit): () -> Unit
