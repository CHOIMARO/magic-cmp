package com.company.feature.editor.editor

import androidx.compose.runtime.Composable

/**
 * Plays an audio address while it is in the composition, for example the preview of a dubbing. Android uses Media3,
 * and iOS uses AVPlayer. A new [url] starts again from the beginning.
 *
 * @param url Address of the audio. The address can have a signature, so the player adds no header.
 * @param onEnded Called when the audio played to the end.
 * @param onError Called when the audio cannot play, for example when the address is no longer valid.
 */
@Composable
internal expect fun StreamAudioPlayer(url: String, onEnded: () -> Unit, onError: () -> Unit)
