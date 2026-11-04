package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun StreamAudioPlayer(url: String, onEnded: () -> Unit, onError: () -> Unit) {
    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnError by rememberUpdatedState(onError)
    DisposableEffect(url) {
        val address = NSURL.URLWithString(url)
        if (address == null) {
            currentOnError()
            return@DisposableEffect onDispose {}
        }
        // 무음 스위치가 켜져 있어도 미리 듣기 소리가 나오게 한다.
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)
        val item = AVPlayerItem(uRL = address)
        val player = AVPlayer(playerItem = item)
        val center = NSNotificationCenter.defaultCenter
        val observers = listOf(
            center.addObserverForName(AVPlayerItemDidPlayToEndTimeNotification, `object` = item, queue = NSOperationQueue.mainQueue) { _ ->
                currentOnEnded()
            },
            center.addObserverForName(AVPlayerItemFailedToPlayToEndTimeNotification, `object` = item, queue = NSOperationQueue.mainQueue) { _ ->
                currentOnError()
            },
        )
        player.play()
        onDispose {
            player.pause()
            observers.forEach { center.removeObserver(it) }
        }
    }
}
