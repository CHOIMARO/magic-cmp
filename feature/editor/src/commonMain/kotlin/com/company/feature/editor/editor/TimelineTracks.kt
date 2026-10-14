package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Start
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.oklch
import com.company.core.domain.model.editor.AudioTrack
import com.company.core.domain.model.editor.TrackItem
import com.company.core.domain.model.editor.TransitionSlot
import com.company.core.domain.model.editor.TransitionType
import com.company.core.domain.model.editor.clipBoundaries
import com.company.core.domain.model.editor.dubbingOverflow
import com.company.core.domain.model.editor.layerCount
import com.company.core.domain.model.editor.placedCaptions
import kotlinx.coroutines.withTimeoutOrNull

/** Hue of the caption bars. */
private const val CAPTION_HUE = 95f

/** Hue of the dubbing bars. */
private const val DUBBING_HUE = 235f

/** Space between two rows of the dubbing and sound effect tracks. */
private val AudioRowSpacing = 4.dp

/** Size of a transition button on the clip track. */
private val TransitionButtonSize = 24.dp

/** Width of each ring around a transition button. */
private val TransitionButtonRing = 2.dp

/** Distance from the first clip to the transition button of the video start. */
private val OpeningButtonOffset = 34.dp

/** Color of the overflow stripes of a dubbing that is longer than its caption. */
private val OverflowStripe = Color(0xFFFB9890)

// =====================================================================
// Transition buttons
// =====================================================================

/**
 * Small buttons at the clip boundaries and before the first clip. A button opens the transition sheet of its place,
 * and its icon shows the transition there (F08).
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param secondWidth Width of one timeline second.
 */
@Composable
internal fun TransitionButtons(state: EditorState, onIntent: (EditorIntent) -> Unit, secondWidth: Dp) {
    val project = state.project
    if (project.clips.isEmpty()) return
    val boundaries = project.clipBoundaries()
    // 버튼은 어두운 테두리로 클립과 떨어져 보이고, 시트가 열린 버튼은 바깥에 보라색 테두리가 있다.
    val outer = TransitionButtonSize + TransitionButtonRing * 4
    val top = (ClipTrackHeight - outer) / 2
    project.clips.forEachIndexed { index, clip ->
        val slot = if (index == 0) TransitionSlot.Opening else TransitionSlot.Boundary(clip.id)
        val transition = if (index == 0) project.openingTransition else project.transitions[clip.id]
        val center = if (index == 0) {
            HandleInset - OpeningButtonOffset + TransitionButtonSize / 2
        } else {
            HandleInset + secondWidth * boundaries[index].toFloat() - TimelineItemGap / 2
        }
        val open = state.sheet == EditorSheet.TRANSITION && state.transitionSlot == slot
        Box(
            modifier = Modifier
                .offset(x = center - outer / 2, y = top)
                .zIndex(4f)
                .size(outer)
                .pressClickable(pressedScale = 0.88f) { onIntent(EditorIntent.OpenTransition(slot)) }
                .clip(RoundedCornerShape(12.dp))
                .background(if (open) MagicColors.Accent else Color.Transparent)
                .padding(TransitionButtonRing)
                .clip(RoundedCornerShape(10.dp))
                .background(MagicColors.Background)
                .padding(TransitionButtonRing)
                .clip(RoundedCornerShape(8.dp))
                .background(if (transition != null) MagicColors.Accent else MagicColors.OnSurface),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when {
                transition == null -> if (index == 0) Icons.Rounded.Start else Icons.Rounded.Add
                transition.type == TransitionType.PUSH -> transition.direction.icon
                else -> transition.type.icon
            }
            Icon(
                icon,
                contentDescription = if (index == 0) "시작 전환" else "${index}번째 클립 뒤 전환",
                tint = MagicColors.OnAccent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Icon of a transition type on the timeline. */
private val TransitionType.icon: ImageVector
    get() = when (this) {
        TransitionType.DISSOLVE -> Icons.Rounded.Gradient
        TransitionType.FADE -> Icons.Rounded.Contrast
        TransitionType.PUSH -> Icons.Rounded.Add
        TransitionType.ZOOM -> Icons.Rounded.ZoomIn
        TransitionType.BLUR -> Icons.Rounded.BlurOn
        TransitionType.FLASH -> Icons.Rounded.FlashOn
        TransitionType.GLITCH -> Icons.Rounded.BrokenImage
    }

// =====================================================================
// Caption track
// =====================================================================

/** Shortest part of an item before the project end that the timeline still shows, in seconds. */
private const val MIN_SHOWN_SECONDS = 0.05

/**
 * Length of the part of an item before the project end. The timeline cuts an item at the project end, like the music
 * bar, because the timeline does not scroll past the end. The stored time does not change.
 *
 * @param total Length of the project.
 * @return The shown length, or null when the item starts at the end or later.
 */
internal fun TrackItem.shownLength(total: Double): Double? = (minOf(end, total) - start).takeIf { it >= MIN_SHOWN_SECONDS }

/**
 * Caption track (F04): one bar for each caption. A tap selects a bar, and a double tap opens the caption in the text
 * sheet. The selected bar shows handles that change its start and end. A drag on a bar scrolls the timeline (D28).
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param edgeScroller Scrolls the timeline while a handle is at the end of the timeline.
 * @param snapSeconds Largest distance in seconds that snaps a handle.
 */
@Composable
internal fun CaptionTrack(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    pixelsPerSecond: Float,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
) {
    val captions = state.project.placedCaptions
    if (captions.isEmpty()) return
    val secondWidth = with(LocalDensity.current) { pixelsPerSecond.toDp() }
    val handleColor = oklch(0.84f, 0.11f, CAPTION_HUE)
    Box(modifier = Modifier.fillMaxWidth().height(TextTrackHeight)) {
        captions.forEach { caption ->
            key(caption.id) {
                val left = HandleInset + secondWidth * caption.start.toFloat()
                val width = maxOf(6.dp, secondWidth * caption.length.toFloat() - TimelineItemGap)
                val selected = (state.selectedTrackItem as? TrackSelection.Caption)?.id == caption.id
                TrackBar(
                    hue = CAPTION_HUE,
                    icon = Icons.Rounded.Subtitles,
                    label = caption.text,
                    selected = selected,
                    modifier = Modifier.offset(x = left).width(width).zIndex(if (selected) 2f else 1f).tapGestures(
                        onTap = { onIntent(EditorIntent.SelectTrackItem(TrackSelection.Caption(caption.id))) },
                        onDoubleTap = { onIntent(EditorIntent.OpenCaptionEditor(caption.id)) },
                    ),
                )
                if (selected) {
                    TrimHandle(
                        target = TimelineDragTarget.CaptionEdge(caption.id, TrimEdge.START),
                        side = TrimEdge.START,
                        height = TextTrackHeight,
                        modifier = Modifier.offset(x = left - (TrimHandleWidth - TrimHandleOverlap)).zIndex(3f),
                        onIntent = onIntent,
                        pixelsPerSecond = pixelsPerSecond,
                        edgeScroller = edgeScroller,
                        snapSeconds = snapSeconds,
                        color = handleColor,
                    )
                    TrimHandle(
                        target = TimelineDragTarget.CaptionEdge(caption.id, TrimEdge.END),
                        side = TrimEdge.END,
                        height = TextTrackHeight,
                        modifier = Modifier.offset(x = left + width - TrimHandleOverlap).zIndex(3f),
                        onIntent = onIntent,
                        pixelsPerSecond = pixelsPerSecond,
                        edgeScroller = edgeScroller,
                        snapSeconds = snapSeconds,
                        color = handleColor,
                    )
                }
            }
        }
    }
}

// =====================================================================
// Dubbing and sound effect tracks
// =====================================================================

/**
 * Dubbing track (F05) or sound effect track (F07): bars in rows. A tap selects a bar. A drag of the selected bar
 * moves it in time, and it snaps to the playhead, the clip boundaries, and the text starts. A drag on another bar
 * scrolls the timeline. A dubbing that is longer than its caption shows red stripes at its end.
 *
 * @param state Editor state.
 * @param track Track to show.
 * @param onIntent Called with each user action.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param edgeScroller Scrolls the timeline while a bar is at the end of the timeline.
 * @param snapSeconds Largest distance in seconds that snaps a bar.
 */
@Composable
internal fun AudioTrackRows(
    state: EditorState,
    track: AudioTrack,
    onIntent: (EditorIntent) -> Unit,
    pixelsPerSecond: Float,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
) {
    val project = state.project
    val items: List<TrackItem> = if (track == AudioTrack.DUBBING) project.dubbings else project.soundEffects
    if (items.isEmpty()) return
    val secondWidth = with(LocalDensity.current) { pixelsPerSecond.toDp() }
    val rowPitch = TextTrackHeight + AudioRowSpacing
    val hue = if (track == AudioTrack.DUBBING) DUBBING_HUE else SOUND_EFFECT_HUE
    Box(modifier = Modifier.fillMaxWidth().height(rowPitch * items.layerCount - AudioRowSpacing)) {
        items.forEach { item ->
            val shownLength = item.shownLength(project.totalDuration)
            // 프로젝트 끝 뒤에서 시작하는 항목은 그리지 않는다.
            if (shownLength != null) key(item.id) {
                val selection = if (track == AudioTrack.DUBBING) TrackSelection.Dubbing(item.id) else TrackSelection.SoundEffect(item.id)
                val selected = state.selectedTrackItem == selection
                val width = maxOf(6.dp, secondWidth * shownLength.toFloat() - TimelineItemGap)
                val dubbing = project.dubbings.find { it.id == item.id }
                val overflow = dubbing?.let { project.dubbingOverflow(it) } ?: 0.0
                val label = dubbing?.text ?: project.soundEffects.find { it.id == item.id }?.name.orEmpty()
                val holder = remember { LayoutHolder() }
                TrackBar(
                    hue = hue,
                    icon = if (track == AudioTrack.DUBBING) Icons.Rounded.RecordVoiceOver else Icons.Rounded.GraphicEq,
                    label = label,
                    selected = selected,
                    overflowWidth = if (overflow > 0.05) minOf(width, secondWidth * overflow.toFloat()) else 0.dp,
                    modifier = Modifier
                        .offset(x = HandleInset + secondWidth * item.start.toFloat(), y = rowPitch * item.layer)
                        .width(width)
                        .zIndex(if (selected) 2f else 1f)
                        .audioBarGestures(
                            bar = holder,
                            selected = selected,
                            target = TimelineDragTarget.AudioMove(track, item.id),
                            pixelsPerSecond = pixelsPerSecond,
                            edgeScroller = edgeScroller,
                            snapSeconds = snapSeconds,
                            onIntent = onIntent,
                            onTap = { onIntent(EditorIntent.SelectTrackItem(selection)) },
                        ),
                )
            }
        }
    }
}

/**
 * Bar of one item on the caption, dubbing, or sound effect track.
 *
 * @param hue Hue of the track.
 * @param icon Icon of the track.
 * @param label Text of the item.
 * @param selected True to show the selection look.
 * @param modifier Modifier for the bar. Give it the position, the width, and the gestures.
 * @param overflowWidth Width of the red stripes at the end, or 0.
 */
@Composable
private fun TrackBar(
    hue: Float,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier,
    overflowWidth: Dp = 0.dp,
) {
    val shape = if (selected) RoundedCornerShape(6.dp) else CircleShape
    val tone = oklch(0.84f, 0.11f, hue)
    Box(
        modifier = modifier
            .height(TextTrackHeight)
            .clip(shape)
            .background(tone.copy(alpha = if (selected) 0.38f else 0.2f))
            .then(if (selected) Modifier.border(2.dp, tone, shape) else Modifier),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val content = oklch(0.92f, 0.07f, hue)
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }
        if (overflowWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(overflowWidth)
                    .fillMaxHeight()
                    .drawBehind {
                        val stripe = 4.dp.toPx()
                        drawRect(OverflowStripe.copy(alpha = 0.18f))
                        var x = -size.height
                        while (x < size.width) {
                            drawLine(OverflowStripe.copy(alpha = 0.6f), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = stripe)
                            x += stripe * 2
                        }
                    },
            )
        }
    }
}

/**
 * Tap and double tap on a bar. A drag does not count as a tap, so the timeline can scroll from the bar.
 *
 * @param onTap Called on a tap.
 * @param onDoubleTap Called on a second tap within the double tap time.
 */
private fun Modifier.tapGestures(onTap: () -> Unit, onDoubleTap: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown()
        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
        if (up.isConsumed) return@awaitEachGesture
        onTap()
        val second = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) { awaitFirstDown() } ?: return@awaitEachGesture
        if (waitForUpOrCancellation() != null) onDoubleTap()
        second.consume()
    }
}

/**
 * Gestures of a dubbing or sound effect bar. A tap selects the bar. When the bar is selected, a drag moves it, and
 * the timeline scrolls at its ends. When the bar is not selected, a drag scrolls the timeline.
 *
 * @param bar Keeps the layout of the bar for the edge scrolling.
 */
private fun Modifier.audioBarGestures(
    bar: LayoutHolder,
    selected: Boolean,
    target: TimelineDragTarget,
    pixelsPerSecond: Float,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
    onIntent: (EditorIntent) -> Unit,
    onTap: () -> Unit,
): Modifier = onGloballyPositioned { bar.coordinates = it }.pointerInput(target, selected, pixelsPerSecond, snapSeconds) {
    awaitEachGesture {
        val down = awaitFirstDown()
        if (!selected) {
            // 고르지 않은 막대는 끌면 타임라인이 스크롤하고, 떼면 선택한다.
            val up = waitForUpOrCancellation()
            if (up != null && !up.isConsumed) onTap()
            return@awaitEachGesture
        }
        var overshootStart = 0f
        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
            change.consume()
            overshootStart = over
        }
        if (slop == null) {
            // 끌지 않고 떼면 선택을 푼다.
            if (currentEvent.changes.all { it.changedToUp() && !it.isConsumed }) onTap()
            return@awaitEachGesture
        }
        onIntent(EditorIntent.StartTimelineDrag(target, snapSeconds))
        bar.coordinates?.let { edgeScroller.begin(it, slop.position) }
        var dragged = overshootStart
        try {
            horizontalDrag(slop.id) { change ->
                dragged += change.positionChange().x
                change.consume()
                val overshoot = bar.coordinates?.let { edgeScroller.follow(it, change.position, stopOffset = 0f) } ?: 0f
                onIntent(EditorIntent.MoveTimelineDrag((dragged - overshoot) / pixelsPerSecond.toDouble()))
            }
        } finally {
            edgeScroller.stop()
            onIntent(EditorIntent.EndTimelineDrag)
        }
    }
}
