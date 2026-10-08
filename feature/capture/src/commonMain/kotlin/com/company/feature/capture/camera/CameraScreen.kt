package com.company.feature.capture.camera

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.PRESSED_SCALE_ICON
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.ui.editor.formatTime

private val ShutterEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Camera screen. It only draws [state] and sends user actions out.
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 * @param onClose Action of the close button.
 */
@Composable
internal fun CameraScreen(state: CameraState, onIntent: (CameraIntent) -> Unit, onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        StripeBox(
            hue = (state.segments.lastOrNull()?.hue ?: 200).toFloat(),
            lightness = 0.42f,
            stripeOffset = (state.recordedSeconds * 18).dp,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "카메라 화면",
            modifier = Modifier
                .align(BiasAlignment(0f, -0.12f))
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.06.em,
            color = Color.White.copy(alpha = 0.8f),
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            SegmentProgress(state = state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundIconButton(Icons.Rounded.Close, contentDescription = "닫기", onClick = onClose)
                Text(
                    text = "${formatTime(state.recordedSeconds)} / ${formatTime(state.maxSeconds.toDouble())}",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                RoundIconButton(Icons.Rounded.FlipCameraAndroid, contentDescription = "카메라 전환", onClick = {})
            }
        }

        CameraControls(state = state, onIntent = onIntent, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SegmentProgress(state: CameraState) {
    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.25f)),
    ) {
        val trackWidth = maxWidth
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            state.segments.forEach { segment ->
                val fraction = (segment.duration / state.maxSeconds).toFloat()
                Box(modifier = Modifier.fillMaxHeight().width(trackWidth * fraction).background(MagicColors.Accent))
            }
        }
    }
}

@Composable
private fun CameraControls(state: CameraState, onIntent: (CameraIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            .navigationBarsPadding()
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(15, 30, 60).forEach { seconds ->
                val selected = state.maxSeconds == seconds
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .pressClickable { onIntent(CameraIntent.SelectMaxDuration(seconds)) }
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.Transparent)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${seconds}초",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.Black else Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (state.canUndoOrContinue) {
                    OverlayPillButton(
                        text = "지우기",
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        onClick = { onIntent(CameraIntent.RemoveLastSegment) },
                    )
                }
            }
            Shutter(recording = state.recording, onClick = { onIntent(CameraIntent.ToggleRecord) })
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (state.canUndoOrContinue) {
                    MagicButton(
                        text = "다음",
                        onClick = { onIntent(CameraIntent.Done) },
                        style = MagicButtonStyle.PRIMARY_SMALL,
                    )
                }
            }
        }

        Text(
            text = when {
                state.recording -> "촬영 중 · 누르면 잠깐 멈춰요"
                state.segments.isNotEmpty() -> "${state.segments.size}개 찍었어요 · 이어 찍거나 다음으로"
                else -> "누르면 촬영, 한 번 더 누르면 멈춰요"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun Shutter(recording: Boolean, onClick: () -> Unit) {
    val size by animateDpAsState(if (recording) 30.dp else 60.dp, tween(200, easing = ShutterEasing))
    val radius by animateDpAsState(if (recording) 8.dp else 30.dp, tween(200, easing = ShutterEasing))
    Box(
        modifier = Modifier
            .size(82.dp)
            .pressClickable(onClick = onClick)
            .border(5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(MagicColors.Record))
    }
}

/** Translucent black background of the camera overlay buttons and its pressed color. */
private val OverlayContainer = Color.Black.copy(alpha = 0.35f)
private val OverlayContainerPressed = Color.Black.copy(alpha = 0.55f)

/**
 * Icon button on the camera preview.
 *
 * The spec has no button for a camera overlay. The button keeps the overlay look and uses the
 * spec press behavior: a darker background and a scale of 94 %.
 */
@Composable
private fun RoundIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressClickable(pressedScale = PRESSED_SCALE_ICON, interactionSource = interactionSource, onClick = onClick)
            .clip(CircleShape)
            .background(if (pressed) OverlayContainerPressed else OverlayContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

/** Pill button with an icon on the camera preview, for example "지우기". */
@Composable
private fun OverlayPillButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .height(44.dp)
            .pressClickable(interactionSource = interactionSource, onClick = onClick)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = if (pressed) 0.6f else 0.45f))
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
