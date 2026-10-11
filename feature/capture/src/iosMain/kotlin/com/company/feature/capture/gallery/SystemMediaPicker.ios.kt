package com.company.feature.capture.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerCapabilitiesSearch
import platform.PhotosUI.PHPickerCapabilitiesSelectionActions
import platform.PhotosUI.PHPickerCapabilitiesStagingArea
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationAssetRepresentationModeCurrent
import platform.PhotosUI.PHPickerConfigurationSelectionContinuousAndOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.NSDirectionalRectEdgeAll
import platform.UIKit.UIUserInterfaceStyle
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.darwin.NSObject

/** Folder in the temporary directory for the copies of the picked photos and videos. */
private const val PICKED_FOLDER = "picked"

private const val DEFAULT_NAME = "media"

internal actual fun isEmbeddedMediaPickerSupported(): Boolean = true

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun EmbeddedMediaPicker(
    controller: EmbeddedMediaPickerController,
    onSelectionChange: (List<String>) -> Unit,
    onSelectionComplete: () -> Unit,
    modifier: Modifier,
) {
    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val scope = rememberCoroutineScope()
    // PHPickerViewController는 delegate를 약하게 참조하므로 컴포지션에서 강하게 붙잡아 둔다.
    val delegate = remember { MediaPickerDelegate(scope) { currentOnSelectionChange(it) } }
    LaunchedEffect(delegate, controller) {
        controller.deselectRequests.collect { delegate.deselect(it) }
    }
    UIKitViewController(
        factory = {
            PHPickerViewController(configuration = pickerConfiguration()).apply {
                this.delegate = delegate
                delegate.picker = this
                overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
            }
        },
        modifier = modifier,
        // 피커는 다른 프로세스가 그리는 뷰라서, Compose가 터치를 붙잡았다가 넘기면 받지 못한다.
        // 그래서 피커를 Compose 위층에 두고 터치를 바로 넘긴다. 피커 위에는 앱 UI를 겹치지 않는다.
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
            placedAsOverlay = true,
        ),
    )
}

// iOS는 항상 임베디드 피커를 쓰므로 전체 화면 피커를 열 일이 없다.
@Composable
internal actual fun rememberMediaPickerLauncher(onResult: (List<String>) -> Unit): () -> Unit = remember { {} }

private fun pickerConfiguration(): PHPickerConfiguration =
    PHPickerConfiguration(photoLibrary = PHPhotoLibrary.sharedPhotoLibrary()).apply {
        filter = PHPickerFilter.anyFilterMatchingSubfilters(listOf(PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter))
        selectionLimit = 0
        selection = PHPickerConfigurationSelectionContinuousAndOrdered
        // 앱이 [다음] 버튼을 따로 두므로 피커의 취소·추가 버튼과 검색을 숨긴다.
        disabledCapabilities = PHPickerCapabilitiesSearch or PHPickerCapabilitiesStagingArea or PHPickerCapabilitiesSelectionActions
        edgesWithoutContentMargins = NSDirectionalRectEdgeAll
        // 원본 형식을 그대로 받아 변환 시간을 없앤다.
        preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent
    }

/**
 * Receives the selection from the picker and copies each new photo or video into the app storage.
 *
 * The picker gives a temporary file that it deletes after the callback, so the app copies it.
 *
 * @param scope Scope for the copy work.
 * @param onSelectionChange Called with the file URLs of the copied items in pick order.
 */
private class MediaPickerDelegate(
    private val scope: CoroutineScope,
    private val onSelectionChange: (List<String>) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    private val copiedUris = mutableMapOf<String, String>()
    private val copying = mutableSetOf<String>()
    private var selection: List<String> = emptyList()

    /** Picker that uses this delegate. [deselect] removes the check mark in it. */
    var picker: PHPickerViewController? = null

    /**
     * Removes an item from the selection and from the picker.
     *
     * @param uri File URL of the copied item.
     */
    fun deselect(uri: String) {
        val assetId = copiedUris.entries.find { it.value == uri }?.key ?: return
        selection = selection - assetId
        emitSelection()
        picker?.deselectAssetsWithIdentifiers(listOf(assetId))
    }

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        selection = results.mapNotNull { it.assetIdentifier }
        emitSelection()
        results.forEach { result ->
            val assetId = result.assetIdentifier ?: return@forEach
            if (assetId in copiedUris || !copying.add(assetId)) return@forEach
            scope.launch {
                copyToAppStorage(result)?.let { copiedUris[assetId] = it }
                copying.remove(assetId)
                emitSelection()
            }
        }
    }

    private fun emitSelection() = onSelectionChange(selection.mapNotNull { copiedUris[it] })
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun copyToAppStorage(result: PHPickerResult): String? = suspendCancellableCoroutine { continuation ->
    val provider = result.itemProvider
    val name = provider.suggestedName ?: DEFAULT_NAME
    // 영상을 먼저 확인한다. 라이브 포토는 영상이 아니라 사진으로 받는다.
    val typeId = when {
        provider.hasItemConformingToTypeIdentifier(UTTypeMovie.identifier) -> UTTypeMovie.identifier
        provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier) -> UTTypeImage.identifier
        else -> null
    }
    if (typeId == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }
    provider.loadFileRepresentationForTypeIdentifier(typeId) { source, _ ->
        val copied = source?.let { file ->
            val folder = NSURL.fileURLWithPath(NSTemporaryDirectory())
                .URLByAppendingPathComponent("$PICKED_FOLDER/${NSUUID().UUIDString}")
            val target = folder?.URLByAppendingPathComponent("$name.${file.pathExtension.orEmpty()}")
            val fileManager = NSFileManager.defaultManager
            if (folder != null && target != null &&
                fileManager.createDirectoryAtURL(folder, withIntermediateDirectories = true, attributes = null, error = null) &&
                fileManager.copyItemAtURL(file, toURL = target, error = null)
            ) target.absoluteString else null
        }
        continuation.resume(copied)
    }
}
