package com.company.feature.editor.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.company.domain.model.editor.transitionWindows
import kotlin.math.abs
import kotlinx.coroutines.delay

/** Wait after a clip or transition change before the player rebuilds its content. A trim drag changes the clips many times. */
private const val CONTENT_DELAY_MILLIS = 120L

/** Smallest position difference, in seconds, that makes the player seek. */
private const val SEEK_TOLERANCE = 0.001

/**
 * Creates the preview player for the project and keeps it in step with the editor state.
 *
 * When all clips have a source file, the platform player plays them. Other projects use
 * [ClockPreviewPlayer], and the stage shows placeholders.
 *
 * @param state Editor state.
 * @param onIntent Called with the playback progress and the end of the playback.
 * @return The video player, or null when the stage must show placeholders.
 */
@Composable
internal fun rememberEditorPlayback(state: EditorState, onIntent: (EditorIntent) -> Unit): VideoPreviewPlayer? {
    val clips = state.project.clips
    val transitions = remember(state.project.clips, state.project.transitions, state.project.openingTransition) {
        state.project.transitionWindows()
    }
    val hasFiles = clips.isNotEmpty() && clips.all { it.sourceUri != null }
    val videoPlayer = if (hasFiles) rememberVideoPreviewPlayer() else null
    val player: PreviewPlayer = videoPlayer ?: remember { ClockPreviewPlayer() }
    val latestState by rememberUpdatedState(state)
    val latestOnIntent by rememberUpdatedState(onIntent)
    val loaded = remember(player) { mutableStateOf(false) }

    // 클립과 전환이 바뀌면 재생할 내용을 다시 만든다. 연속으로 바뀌면 마지막 변경만 반영한다.
    LaunchedEffect(player, clips, transitions) {
        if (loaded.value) delay(CONTENT_DELAY_MILLIS)
        player.setClips(clips, transitions, latestState.time)
        loaded.value = true
        if (latestState.playing) player.play()
    }

    // 재생하는 동안 매 프레임 재생 위치를 ViewModel에 보낸다.
    LaunchedEffect(player, state.playing) {
        if (!state.playing) {
            player.pause()
            return@LaunchedEffect
        }
        val start = latestState.time
        if (abs(player.currentTime - start) > SEEK_TOLERANCE) player.seekTo(start)
        player.play()
        while (true) {
            withFrameNanos { }
            if (player.ended) {
                latestOnIntent(EditorIntent.PlaybackEnded)
                break
            }
            latestOnIntent(EditorIntent.PlaybackProgress(player.currentTime))
        }
    }

    // 멈춘 동안 재생 위치가 바뀌면(타임라인 스크롤, 클립 삭제 등) 그 위치의 화면을 보여 준다.
    LaunchedEffect(player, state.time) {
        if (!latestState.playing && abs(player.currentTime - state.time) > SEEK_TOLERANCE) {
            player.seekTo(state.time)
        }
    }

    // 앱이 화면에서 가려지면 재생을 멈춘다.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { latestOnIntent(EditorIntent.Pause) }

    return videoPlayer
}
