package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.company.core.designsystem.component.DurationBadge
import com.company.core.designsystem.component.PRESSED_SCALE_ICON
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.Clip
import kotlin.math.abs
import kotlin.math.roundToInt

/** Width of one second on the timeline. */
private val PixelsPerSecond = 50.dp

/** Extra space after the last clip for the add button. */
private val TrailingSpace = 60.dp

/** Space before the clip track, so the left trim handle of the first clip stays inside the track. */
private val HandleInset = 16.dp

/**
 * Scrolling timeline with the text, clip, and music tracks. A fixed white line marks the playhead.
 *
 * Scrolling moves the playhead. While the preview plays, the timeline follows the playhead.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun EditorTimeline(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val density = LocalDensity.current
    val pixelsPerSecond = with(density) { PixelsPerSecond.toPx() }
    val scrollState = rememberScrollState()
    val total = state.totalDuration
    val latestState by rememberUpdatedState(state)

    // 사용자가 스크롤하면 재생 위치를 옮긴다. 재생 중에는 무시한다.
    LaunchedEffect(scrollState, pixelsPerSecond) {
        snapshotFlow { scrollState.value }.collect { value ->
            if (scrollState.isScrollInProgress && !latestState.playing) {
                onIntent(EditorIntent.Scrub(value / pixelsPerSecond.toDouble()))
            }
        }
    }
    // 재생 위치가 코드로 바뀌면(재생, 삭제 등) 스크롤을 맞춘다.
    LaunchedEffect(state.time, total) {
        if (!scrollState.isScrollInProgress) {
            val target = (state.time * pixelsPerSecond).roundToInt()
            if (abs(scrollState.value - target) > 1) scrollState.scrollTo(target)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onIntent(EditorIntent.Pause)
                }
            },
    ) {
        val playheadX = (maxWidth / 2 - 8.dp).coerceAtLeast(HandleInset)
        val contentWidth = PixelsPerSecond * total.toFloat() + TrailingSpace
        Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
            Box(modifier = Modifier.width(contentWidth + playheadX * 2).fillMaxHeight()) {
                // 왼쪽 손잡이가 트랙 밖으로 나가지 않게 트랙을 HandleInset만큼 앞에서 시작한다.
                Column(
                    modifier = Modifier
                        .offset(x = playheadX - HandleInset)
                        .width(contentWidth + HandleInset)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TextTrack(state = state, onIntent = onIntent)
                    ClipTrack(state = state, onIntent = onIntent, pixelsPerSecond = pixelsPerSecond)
                    MusicTrackRow(state = state, onIntent = onIntent, width = PixelsPerSecond * total.toFloat())
                }
            }
        }
        Box(
            modifier = Modifier
                .offset(x = playheadX - 1.dp)
                .padding(top = 4.dp, bottom = 6.dp)
                .width(2.dp)
                .fillMaxHeight()
                .shadow(4.dp, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White),
        )
    }
}

@Composable
private fun TextTrack(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(22.dp).padding(start = HandleInset)) {
        state.project.texts.forEach { overlay ->
            Row(
                modifier = Modifier
                    .offset(x = PixelsPerSecond * overlay.start.toFloat())
                    .width(maxOf(40.dp, PixelsPerSecond * overlay.length.toFloat() - 3.dp))
                    .height(22.dp)
                    .pressClickable { onIntent(EditorIntent.OpenTextEditor(overlay.id)) }
                    .clip(CircleShape)
                    .background(MagicColors.Accent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Title, contentDescription = null, tint = MagicColors.AccentPale, modifier = Modifier.size(13.dp))
                Text(
                    text = overlay.text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MagicColors.AccentPale,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun ClipTrack(state: EditorState, onIntent: (EditorIntent) -> Unit, pixelsPerSecond: Float) {
    Box(modifier = Modifier.fillMaxWidth().height(62.dp)) {
        var start = 0.0
        state.project.clips.forEach { clip ->
            val left = HandleInset + PixelsPerSecond * start.toFloat()
            val width = maxOf(8.dp, PixelsPerSecond * clip.duration.toFloat() - 3.dp)
            val selected = clip.id == state.selectedClipId
            ClipBlock(
                clip = clip,
                selected = selected,
                modifier = Modifier.offset(x = left).width(width).zIndex(if (selected) 2f else 1f),
                onClick = { onIntent(EditorIntent.SelectClip(clip.id)) },
            )
            if (selected) {
                TrimHandle(
                    clip = clip,
                    edge = TrimEdge.START,
                    modifier = Modifier.offset(x = left - 14.dp).zIndex(3f),
                    onIntent = onIntent,
                    pixelsPerSecond = pixelsPerSecond,
                )
                TrimHandle(
                    clip = clip,
                    edge = TrimEdge.END,
                    modifier = Modifier.offset(x = left + width - 2.dp).zIndex(3f),
                    onIntent = onIntent,
                    pixelsPerSecond = pixelsPerSecond,
                )
            }
            start += clip.duration
        }
        AddClipButton(
            onClick = { onIntent(EditorIntent.AddClips) },
            modifier = Modifier.offset(x = HandleInset + PixelsPerSecond * state.totalDuration.toFloat() + 8.dp, y = 9.dp),
        )
    }
}

@Composable
private fun ClipBlock(clip: Clip, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    StripeBox(
        hue = clip.thumbnailHue.toFloat(),
        shape = shape,
        modifier = modifier
            .height(62.dp)
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .then(if (selected) Modifier.border(3.dp, MagicColors.Accent, shape) else Modifier),
    ) {
        DurationBadge(
            text = "${(clip.duration * 10).roundToInt() / 10.0}초",
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 5.dp),
        )
        if (clip.speed != 1.0) {
            Text(
                text = "${clip.speed.toSpeedLabel()}배",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MagicColors.Accent)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                color = MagicColors.OnAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Square "+" button after the last clip. It uses the icon button press behavior of the spec. */
@Composable
private fun AddClipButton(onClick: () -> Unit, modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(44.dp)
            .pressClickable(pressedScale = PRESSED_SCALE_ICON, interactionSource = interactionSource, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) MagicColors.SurfaceHighest else MagicColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "클립 추가", modifier = Modifier.size(24.dp))
    }
}

/** Purple handle that trims one edge of the selected clip. */
@Composable
private fun TrimHandle(
    clip: Clip,
    edge: TrimEdge,
    modifier: Modifier,
    onIntent: (EditorIntent) -> Unit,
    pixelsPerSecond: Float,
) {
    val shape = if (edge == TrimEdge.START) {
        RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp)
    } else {
        RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp)
    }
    Box(
        modifier = modifier
            .size(width = 16.dp, height = 62.dp)
            .clip(shape)
            .background(MagicColors.Accent)
            .pointerInput(clip.id, edge) {
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragged = 0f
                        onIntent(EditorIntent.StartTrim(clip.id))
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                        onIntent(EditorIntent.Trim(clip.id, edge, dragged / pixelsPerSecond.toDouble()))
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(width = 2.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(MagicColors.OnAccent))
    }
}

@Composable
private fun MusicTrackRow(state: EditorState, onIntent: (EditorIntent) -> Unit, width: Dp) {
    Box(modifier = Modifier.fillMaxWidth().height(20.dp).padding(start = HandleInset)) {
        val track = state.musicTracks.find { it.id == state.project.musicId } ?: return@Box
        Row(
            modifier = Modifier
                .width(width)
                .height(20.dp)
                .pressClickable(pressedScale = 0.99f) { onIntent(EditorIntent.OpenSheet(EditorSheet.MUSIC)) }
                .clip(CircleShape)
                .background(MagicColors.MusicContainer)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MagicColors.Music, modifier = Modifier.size(13.dp))
            Text(text = track.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.Music, maxLines = 1)
        }
    }
}

/** Formats a speed like the design: 0.5, 1, 1.5, 2, 3. */
internal fun Double.toSpeedLabel(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
