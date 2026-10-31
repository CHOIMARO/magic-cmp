package com.company.feature.editor.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.PRESSED_SCALE_ICON
import com.company.core.designsystem.component.SystemBackHandler
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.fullscreen_close
import com.company.feature.editor.resources.pause
import com.company.feature.editor.resources.play
import com.company.ui.editor.formatTime
import org.jetbrains.compose.resources.stringResource

/** Background of the floating buttons and the control bar. It is dark and lets the video show through. */
private val ControlBackground = Color(0xFF141318)

/** Time of the fade when the controls show or hide. */
private const val CONTROLS_FADE_MILLIS = 200

/** Time of the size change of the seek knob. */
private const val KNOB_MILLIS = 120

/**
 * Full-screen preview (S11-8). The 9:16 stage fills the screen, and the rest is black.
 *
 * A tap on the screen hides or shows the floating controls. It does not play or pause. The controls are a close
 * button, a play button, the time, and a seek bar. The system back action closes the preview.
 *
 * @param state Editor state. [EditorState.fullScreen] must not be null.
 * @param onIntent Called with each user action.
 * @param video Video surface of the preview player, or null to show placeholders on the stage.
 */
@Composable
internal fun FullScreenPreview(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    video: (@Composable (Modifier) -> Unit)?,
) {
    val fullScreen = state.fullScreen ?: return
    SystemBackHandler(enabled = true) { onIntent(EditorIntent.CloseFullScreen) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onIntent(EditorIntent.ToggleFullScreenControls) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        EditorStage(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.aspectRatio(9f / 16f),
            video = video,
            fullScreen = true,
        )
        AnimatedVisibility(
            visible = fullScreen.controlsVisible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(CONTROLS_FADE_MILLIS)),
            exit = fadeOut(tween(CONTROLS_FADE_MILLIS)),
        ) {
            FullScreenControls(state = state, seeking = fullScreen.seeking, onIntent = onIntent)
        }
    }
}

/** Floating controls on top of the video, with dark fades at the top and the bottom. */
@Composable
private fun FullScreenControls(state: EditorState, seeking: Boolean, onIntent: (EditorIntent) -> Unit) {
    val total = state.totalDuration
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
        )
        FloatingButton(
            icon = Icons.Rounded.Close,
            contentDescription = stringResource(Res.string.fullscreen_close),
            onClick = { onIntent(EditorIntent.CloseFullScreen) },
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 20.dp)
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(ControlBackground.copy(alpha = 0.62f))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // 막대를 만지면 제어를 다시 1.5초 동안 보여 준다. 버튼과 재생바보다 먼저 받는다.
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        onIntent(EditorIntent.ShowFullScreenControls)
                        // 막대의 빈 곳을 눌러도 뒤의 화면 누르기(제어 숨기기)로 넘기지 않는다.
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val atEnd = !state.playing && total > 0.0 && state.time >= total - 0.05
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .pressClickable(pressedScale = PRESSED_SCALE_ICON) { onIntent(EditorIntent.TogglePlay) }
                    .clip(CircleShape)
                    .background(MagicColors.OnSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        state.playing -> Icons.Rounded.Pause
                        atEnd -> Icons.Rounded.Replay
                        else -> Icons.Rounded.PlayArrow
                    },
                    contentDescription = stringResource(if (state.playing) Res.string.pause else Res.string.play),
                    tint = MagicColors.Background,
                    modifier = Modifier.size(28.dp),
                )
            }
            TimeLabel(text = formatTime(state.time), color = MagicColors.OnSurface)
            SeekBar(
                progress = if (total > 0.0) (state.time / total).toFloat().coerceIn(0f, 1f) else 0f,
                seeking = seeking,
                total = total,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
            TimeLabel(text = formatTime(total), color = MagicColors.OnSurface.copy(alpha = 0.65f), textAlign = TextAlign.End)
        }
    }
}

/** Time in the control bar, with digits of the same width. */
@Composable
private fun TimeLabel(text: String, color: Color, textAlign: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        modifier = Modifier.widthIn(min = 32.dp),
        color = color,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = textAlign,
        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
    )
}

/**
 * Seek bar of the full-screen preview. A touch moves the preview to that place, and a drag moves it with the finger.
 * The knob grows while the finger is on the bar.
 *
 * @param progress Played part from 0 to 1.
 * @param seeking True while the finger is on the bar.
 * @param total Length of the project in seconds.
 */
@Composable
private fun SeekBar(progress: Float, seeking: Boolean, total: Double, onIntent: (EditorIntent) -> Unit, modifier: Modifier) {
    val knob by animateDpAsState(if (seeking) 20.dp else 14.dp, tween(KNOB_MILLIS))
    BoxWithConstraints(
        modifier = modifier
            .height(44.dp)
            .pointerInput(total) {
                fun timeAt(x: Float): Double = (x / size.width).coerceIn(0f, 1f) * total
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onIntent(EditorIntent.StartFullScreenSeek(timeAt(down.position.x)))
                    try {
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            onIntent(EditorIntent.MoveFullScreenSeek(timeAt(change.position.x)))
                        }
                    } finally {
                        onIntent(EditorIntent.EndFullScreenSeek)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val track = RoundedCornerShape(2.dp)
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(track).background(MagicColors.OnSurface.copy(alpha = 0.28f)))
        Box(modifier = Modifier.fillMaxWidth(progress).height(4.dp).clip(track).background(MagicColors.OnSurface))
        Box(
            modifier = Modifier
                .offset(x = maxWidth * progress - knob / 2)
                .size(knob)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Round button on the video, for example the close button. */
@Composable
private fun FloatingButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .pressClickable(pressedScale = PRESSED_SCALE_ICON, onClick = onClick)
            .clip(CircleShape)
            .background(ControlBackground.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = MagicColors.OnSurface, modifier = Modifier.size(24.dp))
    }
}
