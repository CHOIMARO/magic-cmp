package com.company.feature.capture.gallery

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.photopicker.compose.EmbeddedPhotoPicker
import androidx.photopicker.compose.ExperimentalPhotoPickerComposeApi
import androidx.photopicker.compose.rememberEmbeddedPhotoPickerState
import com.company.core.designsystem.theme.MagicColors

/** SDK Extensions version that adds the embedded photo picker to Android 14. */
private const val EMBEDDED_PICKER_EXTENSION = 15

private val MEDIA_MIME_TYPES = listOf("image/*", "video/*")

internal actual fun isEmbeddedMediaPickerSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= EMBEDDED_PICKER_EXTENSION

@OptIn(ExperimentalPhotoPickerComposeApi::class)
@Composable
internal actual fun EmbeddedMediaPicker(
    onSelectionChange: (List<String>) -> Unit,
    onSelectionComplete: () -> Unit,
    modifier: Modifier,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) < EMBEDDED_PICKER_EXTENSION
    ) return

    val currentOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val currentOnSelectionComplete by rememberUpdatedState(onSelectionComplete)
    // 피커는 고른 항목과 해제한 항목만 알려 준다. 누른 순서는 앱이 직접 기록한다.
    val pickedUris = remember { mutableStateListOf<Uri>() }
    val state = rememberEmbeddedPhotoPickerState(
        initialExpandedValue = true,
        onUriPermissionGranted = { uris ->
            pickedUris.addAll(uris.filterNot { it in pickedUris })
            currentOnSelectionChange(pickedUris.map(Uri::toString))
        },
        onUriPermissionRevoked = { uris ->
            pickedUris.removeAll(uris)
            currentOnSelectionChange(pickedUris.map(Uri::toString))
        },
        onSelectionComplete = { currentOnSelectionComplete() },
    )
    val featureInfo = remember {
        EmbeddedPhotoPickerFeatureInfo.Builder()
            .setMimeTypes(MEDIA_MIME_TYPES)
            .setOrderedSelection(true)
            // API 문서는 @ColorLong이지만, 피커는 ARGB 정수 값을 읽는다. Color.pack() 값을 주면 강조색이 무시된다.
            .setAccentColor(MagicColors.Accent.toArgb().toLong())
            .setThemeNightMode(Configuration.UI_MODE_NIGHT_YES)
            .build()
    }
    EmbeddedPhotoPicker(state = state, modifier = modifier, embeddedPhotoPickerFeatureInfo = featureInfo)
}

@Composable
internal actual fun rememberMediaPickerLauncher(onResult: (List<String>) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        currentOnResult(uris.map(Uri::toString))
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
    }
}
