package com.company.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Width of a phone screen preview, in dp. */
const val PHONE_PREVIEW_WIDTH = 390

/** Height of a phone screen preview, in dp. */
const val PHONE_PREVIEW_HEIGHT = 844

/**
 * Frame for a `@Preview`. It applies [MagicTheme] and paints the app background.
 *
 * @param modifier Modifier for the frame.
 * @param content Content to preview.
 */
@Composable
fun MagicPreview(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    MagicTheme {
        Box(modifier = modifier.background(MagicColors.Background)) {
            content()
        }
    }
}
