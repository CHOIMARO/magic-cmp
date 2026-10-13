package com.company.feature.editor.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.company.core.designsystem.component.DurationBadge
import com.company.core.designsystem.component.PRESSED_SCALE_ICON
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.AudioTrack
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.MediaType
import com.company.core.domain.model.editor.MusicRange
import com.company.core.domain.model.editor.clipBoundaries
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.textLayerCount
import com.company.ui.editor.formatTime
import com.company.ui.media.rememberFilmstripFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Width of one second on the timeline at the default zoom. */
private val PixelsPerSecond = 50.dp

/** Smallest timeline zoom. A long project fits on the screen. */
private const val MIN_ZOOM = 0.2f

/**
 * Largest timeline zoom. One filmstrip frame then shows one frame of a 30 fps video.
 * It is the filmstrip frame width (36 dp) times 30, divided by [PixelsPerSecond] (50 dp).
 */
private const val MAX_ZOOM = 36f * 30f / 50f

/** Filmstrip frames that show this many source seconds or less are read exactly, not from a near key frame. */
private const val EXACT_FRAME_SECONDS = 0.5

/** Extra space after the last clip. */
private val TrailingSpace = 60.dp

/** Width of the fade at the right edge. The add button sits on it. */
private val AddButtonFadeWidth = 92.dp

/** Space before the clip track, so the left trim handle of the first clip stays inside the track. */
internal val HandleInset = 16.dp

/** Time of the color change of a text bar that cannot stay at its place. */
private const val BAR_COLOR_MILLIS = 100

/** Time in which the timeline moves back after a drag near the project start. */
private const val DRAG_SHIFT_RETURN_MILLIS = 200

/** Width of one frame in the clip filmstrip. A 9:16 frame at the clip height is about this wide. */
private val FilmstripFrameWidth = 36.dp

/** Frames that load beyond each side of the visible part of a clip. They make the scrolling smooth. */
private const val FILMSTRIP_EXTRA_FRAMES = 2

/** The last filmstrip frame is read this many seconds before the end of the source, so the time stays inside the file. */
private const val FILMSTRIP_END_MARGIN = 0.05

/** Width of the area at the left end of the timeline that scrolls the timeline during a timeline drag. */
private val EdgeScrollLeftZone = 48.dp

/** Width of a trim handle. */
internal val TrimHandleWidth = 16.dp

/** Height of the whole timeline. The tracks under the clip track scroll up and down when they do not fit. */
private val TimelineHeight = 184.dp

/** Space above the clip track and under the last track. */
private val TimelinePadding = 6.dp

/** Space between two tracks or two text rows. */
internal val TrackSpacing = 6.dp

/** Height of one text row. */
internal val TextTrackHeight = 22.dp

/** Height of the clip track. */
internal val ClipTrackHeight = 62.dp

/** Gap after each clip block and text bar, so that neighbors look apart. */
internal val TimelineItemGap = 3.dp

/** Part of a trim handle that lies on the clip. The rest is outside the clip. */
internal val TrimHandleOverlap = 2.dp

/** A dragged edge snaps to the playhead, and a text start to another text start, within this distance. */
private val SnapDistance = 12.dp

/** Fastest edge scrolling during a timeline drag, as a distance per second. It is the same at all zoom levels. */
private val EdgeScrollMaxSpeed = 300.dp

/**
 * Scrolling timeline with the clip track at the top, and the text rows and the music track under it.
 * A fixed white line marks the playhead.
 *
 * Scrolling to the side moves the playhead. While the preview plays, the timeline follows the playhead.
 * The clip track stays at the top. The text rows and the music track scroll up and down when they do not fit.
 * The add button stays at the right edge on top of a fade.
 *
 * Two fingers zoom the timeline in and out around the playhead. A larger zoom shows more filmstrip frames
 * and makes trims finer.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun EditorTimeline(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val density = LocalDensity.current
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    // 확대·축소에 따라 바뀌는 1초의 폭.
    val secondWidth = PixelsPerSecond * zoom
    val pixelsPerSecond = with(density) { secondWidth.toPx() }
    val latestPixelsPerSecond by rememberUpdatedState(pixelsPerSecond)
    val scrollState = rememberScrollState()
    val layerScrollState = rememberScrollState()
    val snapSeconds = with(density) { SnapDistance.toPx() } / pixelsPerSecond.toDouble()
    val total = state.totalDuration
    val latestState by rememberUpdatedState(state)
    val latestOnIntent by rememberUpdatedState(onIntent)
    val scope = rememberCoroutineScope()
    val edgeScroller = remember(scope, density) {
        with(density) {
            TimelineEdgeScroller(
                scope = scope,
                leftZone = EdgeScrollLeftZone.toPx(),
                // 오른쪽 끝은 추가 버튼과 페이드가 덮는다. 손잡이는 그 앞에서 멈추고, 손가락이 넘어가면 스크롤한다.
                rightStop = AddButtonFadeWidth.toPx(),
                maxSpeed = EdgeScrollMaxSpeed.toPx(),
            ) { pixels -> latestOnIntent(EditorIntent.ScrollTimelineDrag(pixels / latestPixelsPerSecond.toDouble())) }
        }
    }

    val haptic = LocalHapticFeedback.current
    // 사용자가 스크롤하면 재생 위치를 옮긴다. 재생 중에는 무시한다.
    // 재생선이 클립 경계를 지나면 짧게 진동한다. 경계에 붙지는 않는다 (D26).
    LaunchedEffect(scrollState, pixelsPerSecond) {
        snapshotFlow { scrollState.value }.collect { value ->
            if (scrollState.isScrollInProgress && !latestState.playing && latestState.activeDrag == null) {
                val before = latestState.time
                val after = value / pixelsPerSecond.toDouble()
                val boundaries = latestState.project.clipBoundaries()
                if (boundaries.drop(1).dropLast(1).any { (before - it) * (after - it) < 0 }) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                onIntent(EditorIntent.Scrub(after))
            }
        }
    }
    // 끌기를 멈추면 현재 시간을 다시 초 단위로 보여 준다.
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling && latestState.scrubbing) onIntent(EditorIntent.ScrubEnded)
        }
    }
    // 재생 위치가 코드로 바뀌면(재생, 삭제, 앞 손잡이 끌기 등) 스크롤을 맞춘다.
    LaunchedEffect(state.time, total) {
        if (!scrollState.isScrollInProgress) {
            val target = (state.time * pixelsPerSecond).roundToInt()
            if (abs(scrollState.value - target) > 1) scrollState.scrollTo(target)
        }
    }
    // 확대·축소는 손가락 사이에서 스크롤 위치를 바로 맞춘다. 내용이 커지기 전이라 끝에서 모자란 스크롤은
    // 다음 프레임에 그려진 내용 크기로 다시 맞춘다.
    LaunchedEffect(pixelsPerSecond) {
        withFrameNanos { }
        if (!scrollState.isScrollInProgress) {
            val target = (latestState.time * pixelsPerSecond).roundToInt()
            if (abs(scrollState.value - target) > 1) scrollState.scrollTo(target)
        }
    }
    // 타임라인을 끄는 동안 재생 위치를 0초보다 앞으로 옮길 수 없으면, 남은 만큼 타임라인을 오른쪽으로 민다.
    // 끄는 동안에는 바로 따라가고, 손을 떼면 제자리로 돌아온다.
    val dragShiftPx by animateFloatAsState(
        targetValue = (state.dragShift * pixelsPerSecond).toFloat(),
        animationSpec = if (state.activeDrag != null) snap() else tween(DRAG_SHIFT_RETURN_MILLIS),
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(TimelineHeight)
            .onGloballyPositioned { edgeScroller.viewport = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onIntent(EditorIntent.Pause)
                }
            }
            .pointerInput(Unit) {
                // 두 손가락 확대·축소. 스크롤과 막대·손잡이보다 먼저(Initial) 받아서, 두 손가락일 때는 그쪽으로 넘기지 않는다.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            val newZoom = (zoom * event.calculateZoom()).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            if (newZoom != zoom) {
                                // 재생선이 가리키는 시간이 그대로 있도록 스크롤도 같은 비율로 바꾼다.
                                val ratio = newZoom / zoom
                                zoom = newZoom
                                scrollState.dispatchRawDelta(scrollState.value * (ratio - 1f))
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        val playheadX = (maxWidth / 2 - 8.dp).coerceAtLeast(HandleInset)
        val contentWidth = secondWidth * total.toFloat() + TrailingSpace
        val playheadXPx = with(density) { playheadX.toPx() }
        val viewportWidthPx = constraints.maxWidth.toFloat()
        // 클립이 시작하는 타임라인 시간을 받아, 화면에 보이는 부분을 클립 왼쪽 기준 픽셀 범위로 돌려준다.
        val visibleRange: (Double) -> ClosedFloatingPointRange<Float> = { clipStart ->
            val clipLeft = playheadXPx + clipStart.toFloat() * pixelsPerSecond + dragShiftPx
            (scrollState.value - clipLeft)..(scrollState.value + viewportWidthPx - clipLeft)
        }
        Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
            Box(
                modifier = Modifier
                    .width(contentWidth + playheadX * 2)
                    .fillMaxHeight()
                    .graphicsLayer { translationX = dragShiftPx },
            ) {
                // 왼쪽 손잡이가 트랙 밖으로 나가지 않게 트랙을 HandleInset만큼 앞에서 시작한다.
                Column(
                    modifier = Modifier
                        .offset(x = playheadX - HandleInset)
                        .width(contentWidth + HandleInset)
                        .fillMaxHeight()
                        .padding(top = TimelinePadding),
                ) {
                    ClipTrack(
                        state = state,
                        onIntent = onIntent,
                        pixelsPerSecond = pixelsPerSecond,
                        visibleRange = visibleRange,
                        edgeScroller = edgeScroller,
                        snapSeconds = snapSeconds,
                    )
                    // 영상 트랙은 위에 고정하고, 그 아래 글자 줄과 음악 트랙은 넘치면 위아래로 스크롤한다.
                    Column(
                        modifier = Modifier
                            .padding(top = TrackSpacing)
                            .weight(1f)
                            .verticalScroll(layerScrollState)
                            .padding(bottom = TimelinePadding),
                        verticalArrangement = Arrangement.spacedBy(TrackSpacing),
                    ) {
                        TextTrack(
                            state = state,
                            onIntent = onIntent,
                            pixelsPerSecond = pixelsPerSecond,
                            edgeScroller = edgeScroller,
                            snapSeconds = snapSeconds,
                        )
                        CaptionTrack(
                            state = state,
                            onIntent = onIntent,
                            pixelsPerSecond = pixelsPerSecond,
                            edgeScroller = edgeScroller,
                            snapSeconds = snapSeconds,
                        )
                        AudioTrackRows(
                            state = state,
                            track = AudioTrack.DUBBING,
                            onIntent = onIntent,
                            pixelsPerSecond = pixelsPerSecond,
                            edgeScroller = edgeScroller,
                            snapSeconds = snapSeconds,
                        )
                        AudioTrackRows(
                            state = state,
                            track = AudioTrack.SOUND_EFFECT,
                            onIntent = onIntent,
                            pixelsPerSecond = pixelsPerSecond,
                            edgeScroller = edgeScroller,
                            snapSeconds = snapSeconds,
                        )
                        MusicTrackRow(state = state, onIntent = onIntent, secondWidth = secondWidth)
                    }
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
        // 가장자리를 어둡게 덮어 클립이 버튼 밑으로 사라지는 것처럼 보이게 한다. 터치는 아래 타임라인으로 간다.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(AddButtonFadeWidth)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(0f to MagicColors.Background.copy(alpha = 0f), 0.48f to MagicColors.Background)),
        )
        // 맨 위 클립 트랙과 같은 높이에 둔다.
        AddClipButton(
            onClick = { onIntent(EditorIntent.AddClips) },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = TimelinePadding, end = 12.dp),
        )
    }
}

/**
 * Text overlays as bars at their time, in the rows that the overlays keep. The first row is at the top.
 * A tap selects a bar, and a long press moves it in time. The selected bar shows handles that change
 * when the text starts and ends.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param edgeScroller Scrolls the timeline while a bar or a handle is at the end of the timeline.
 * @param snapSeconds Largest distance in seconds that snaps a dragged edge.
 */
@Composable
private fun TextTrack(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    pixelsPerSecond: Float,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
) {
    val secondWidth = with(LocalDensity.current) { pixelsPerSecond.toDp() }
    val rowPitch = TextTrackHeight + TrackSpacing
    // 줄마다 따로 그리지 않고 한 상자에 줄 높이만큼 내려 그린다. 줄이 바뀌어도 막대를 다시 만들지 않는다.
    Box(modifier = Modifier.fillMaxWidth().height(rowPitch * state.project.textLayerCount - TrackSpacing)) {
        state.project.texts.forEach { overlay ->
            key(overlay.id) {
                val top = rowPitch * overlay.layer
                val left = HandleInset + secondWidth * overlay.start.toFloat()
                val width = maxOf(8.dp, secondWidth * overlay.length.toFloat() - TimelineItemGap)
                val selected = overlay.id == state.selectedTextId
                val moving = (state.activeDrag as? TimelineDragTarget.TextMove)?.textId == overlay.id
                TextBar(
                    overlay = overlay,
                    selected = selected,
                    blocked = moving && state.dragBlocked,
                    pixelsPerSecond = pixelsPerSecond,
                    onIntent = onIntent,
                    edgeScroller = edgeScroller,
                    snapSeconds = snapSeconds,
                    modifier = Modifier.offset(x = left, y = top).width(width).zIndex(if (selected) 2f else 1f),
                )
                if (selected) {
                    TrimHandle(
                        target = TimelineDragTarget.TextEdge(overlay.id, TrimEdge.START),
                        side = TrimEdge.START,
                        height = TextTrackHeight,
                        modifier = Modifier.offset(x = left - (TrimHandleWidth - TrimHandleOverlap), y = top).zIndex(3f),
                        onIntent = onIntent,
                        pixelsPerSecond = pixelsPerSecond,
                        edgeScroller = edgeScroller,
                        snapSeconds = snapSeconds,
                    )
                    TrimHandle(
                        target = TimelineDragTarget.TextEdge(overlay.id, TrimEdge.END),
                        side = TrimEdge.END,
                        height = TextTrackHeight,
                        modifier = Modifier.offset(x = left + width - TrimHandleOverlap, y = top).zIndex(3f),
                        onIntent = onIntent,
                        pixelsPerSecond = pixelsPerSecond,
                        edgeScroller = edgeScroller,
                        snapSeconds = snapSeconds,
                    )
                }
            }
        }
    }
}

/**
 * Bar of one text overlay on the text track.
 *
 * A tap selects the bar, and a double tap opens the text sheet. A long press starts a move: the bar follows the
 * finger in time and between the rows.
 * When the bar overlaps another text in its row, it turns red, and it goes back when the finger lifts.
 *
 * @param overlay Text overlay to show.
 * @param selected True to show the selection look.
 * @param blocked True to show that the moved bar cannot stay at its place.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param onIntent Called with each user action.
 * @param edgeScroller Scrolls the timeline while the bar is at the end of the timeline.
 * @param snapSeconds Largest distance in seconds that snaps the bar.
 * @param modifier Modifier for the bar. Give it the position and the width.
 */
@Composable
private fun TextBar(
    overlay: TextOverlay,
    selected: Boolean,
    blocked: Boolean,
    pixelsPerSecond: Float,
    onIntent: (EditorIntent) -> Unit,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
    modifier: Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val latestSnapSeconds by rememberUpdatedState(snapSeconds)
    val latestSelected by rememberUpdatedState(selected)
    val bar = remember { LayoutHolder() }
    val shape = if (selected) RoundedCornerShape(6.dp) else CircleShape
    val tone = if (blocked) MagicColors.Danger else MagicColors.Accent
    val background by animateColorAsState(tone.copy(alpha = if (selected) 0.35f else 0.2f), tween(BAR_COLOR_MILLIS))
    val border by animateColorAsState(tone, tween(BAR_COLOR_MILLIS))
    Row(
        modifier = modifier
            .height(TextTrackHeight)
            .onGloballyPositioned { bar.coordinates = it }
            .pointerInput(overlay.id) {
                val rowPitch = (TextTrackHeight + TrackSpacing).toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress == null) {
                        // 길게 누르기 전에 손을 떼면 탭이다. 타임라인 스크롤이 가져간 터치는 탭이 아니다.
                        if (currentEvent.changes.all { it.changedToUp() && !it.isConsumed }) {
                            onIntent(EditorIntent.SelectText(overlay.id))
                            // 두 번 누르기 시간 안에 한 번 더 누르면 글자 시트를 연다.
                            val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) { awaitFirstDown() }
                            if (secondDown != null && waitForUpOrCancellation() != null) {
                                // 첫 탭이 선택을 풀었으면 다시 선택한다.
                                if (!latestSelected) onIntent(EditorIntent.SelectText(overlay.id))
                                onIntent(EditorIntent.OpenTextEditor(overlay.id))
                            }
                        }
                        return@awaitEachGesture
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(EditorIntent.StartTimelineDrag(TimelineDragTarget.TextMove(overlay.id), latestSnapSeconds))
                    bar.coordinates?.let { edgeScroller.begin(it, longPress.position) }
                    var draggedX = 0f
                    var draggedY = 0f
                    try {
                        drag(longPress.id) { change ->
                            val amount = change.positionChange()
                            draggedX += amount.x
                            draggedY += amount.y
                            change.consume()
                            // 손가락이 오른쪽 멈춤 선을 넘은 만큼은 막대를 움직이지 않는다. 그만큼은 타임라인이 스크롤한다.
                            val overshoot = bar.coordinates?.let { edgeScroller.follow(it, change.position, stopOffset = 0f) } ?: 0f
                            // 위아래로 한 줄 높이의 절반을 넘게 움직이면 다음 줄로 간다.
                            val rowDelta = (draggedY / rowPitch).roundToInt()
                            onIntent(EditorIntent.MoveTimelineDrag((draggedX - overshoot) / pixelsPerSecond.toDouble(), rowDelta))
                        }
                    } finally {
                        // 끄는 도중에 막대가 사라져도 옮기기와 스크롤을 끝낸다.
                        edgeScroller.stop()
                        onIntent(EditorIntent.EndTimelineDrag)
                    }
                }
            }
            .clip(shape)
            .background(background)
            .then(if (selected || blocked) Modifier.border(2.dp, border, shape) else Modifier)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (blocked) MagicColors.OnSurface else MagicColors.AccentPale
        Icon(Icons.Rounded.Title, contentDescription = null, tint = content, modifier = Modifier.size(13.dp))
        Text(
            text = overlay.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

/**
 * Clip blocks in play order. The selected clip shows the trim handles.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param visibleRange Returns the visible part of a clip, in pixels from its left edge. It takes the start time of the clip.
 * @param edgeScroller Scrolls the timeline while a trim handle is at the end of the timeline.
 * @param snapSeconds Largest distance in seconds that snaps the clip start to the playhead.
 */
@Composable
private fun ClipTrack(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    pixelsPerSecond: Float,
    visibleRange: (Double) -> ClosedFloatingPointRange<Float>,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
) {
    val secondWidth = with(LocalDensity.current) { pixelsPerSecond.toDp() }
    Box(modifier = Modifier.fillMaxWidth().height(ClipTrackHeight)) {
        var start = 0.0
        state.project.clips.forEach { clip ->
            val left = HandleInset + secondWidth * start.toFloat()
            val width = maxOf(8.dp, secondWidth * clip.duration.toFloat() - TimelineItemGap)
            val selected = clip.id == state.selectedClipId
            val clipStart = start
            ClipBlock(
                clip = clip,
                selected = selected,
                pixelsPerSecond = pixelsPerSecond,
                visibleRange = { visibleRange(clipStart) },
                modifier = Modifier.offset(x = left).width(width).zIndex(if (selected) 2f else 1f),
                onClick = { onIntent(EditorIntent.SelectClip(clip.id)) },
            )
            if (selected) {
                TrimHandle(
                    target = TimelineDragTarget.ClipEdge(clip.id, TrimEdge.START),
                    side = TrimEdge.START,
                    height = ClipTrackHeight,
                    modifier = Modifier.offset(x = left - (TrimHandleWidth - TrimHandleOverlap)).zIndex(3f),
                    onIntent = onIntent,
                    pixelsPerSecond = pixelsPerSecond,
                    edgeScroller = edgeScroller,
                    snapSeconds = snapSeconds,
                )
                TrimHandle(
                    target = TimelineDragTarget.ClipEdge(clip.id, TrimEdge.END),
                    side = TrimEdge.END,
                    height = ClipTrackHeight,
                    modifier = Modifier.offset(x = left + width - TrimHandleOverlap).zIndex(3f),
                    onIntent = onIntent,
                    pixelsPerSecond = pixelsPerSecond,
                    edgeScroller = edgeScroller,
                    snapSeconds = snapSeconds,
                )
            }
            start += clip.duration
        }
        // 클립을 고르거나 끄는 동안에는 손잡이를 가리지 않도록 전환 버튼을 숨긴다.
        if (state.selectedClipId == null && state.activeDrag == null) {
            TransitionButtons(state = state, onIntent = onIntent, secondWidth = secondWidth)
        }
    }
}

/**
 * One clip on the timeline: a filmstrip of its frames with the length and the speed.
 *
 * @param clip Clip to show.
 * @param selected True to draw the selection border.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param visibleRange Returns the visible part of the clip, in pixels from its left edge.
 * @param modifier Modifier for the block. Give it the position and the width.
 * @param onClick Action on click.
 */
@Composable
private fun ClipBlock(
    clip: Clip,
    selected: Boolean,
    pixelsPerSecond: Float,
    visibleRange: () -> ClosedFloatingPointRange<Float>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    // 프레임을 읽기 전과 원본 파일이 없는 샘플 클립에는 줄무늬를 보여 준다.
    StripeBox(
        hue = clip.thumbnailHue.toFloat(),
        shape = shape,
        modifier = modifier
            .height(ClipTrackHeight)
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .then(if (selected) Modifier.border(3.dp, MagicColors.Accent, shape) else Modifier),
    ) {
        clip.sourceUri?.let { uri ->
            ClipFilmstrip(
                clip = clip,
                uri = uri,
                pixelsPerSecond = pixelsPerSecond,
                visibleRange = visibleRange,
                modifier = Modifier.matchParentSize(),
            )
        }
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

/**
 * Frames of the clip source side by side, like a film.
 *
 * Each frame stays at its position in the source video. Thus a trim drag moves the frames under the edge,
 * and the user sees where the clip starts and ends. Only the frames near the visible part load.
 *
 * @param clip Clip to show.
 * @param uri Location of the source file.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param visibleRange Returns the visible part of the clip, in pixels from its left edge.
 * @param modifier Modifier for the filmstrip. Give it the size of the clip.
 */
@Composable
private fun ClipFilmstrip(
    clip: Clip,
    uri: String,
    pixelsPerSecond: Float,
    visibleRange: () -> ClosedFloatingPointRange<Float>,
    modifier: Modifier,
) {
    val frameWidth = with(LocalDensity.current) { FilmstripFrameWidth.toPx() }
    // 한 칸에 들어가는 원본 길이. 빠르게 재생하는 클립은 한 칸에 더 긴 원본이 들어간다.
    val frameSeconds = frameWidth / pixelsPerSecond * clip.speed
    // 원본 0초는 클립 왼쪽 끝보다 이만큼 왼쪽에 있다.
    val sourceOffset = (clip.inPoint / clip.speed * pixelsPerSecond).toFloat()
    val frameCount = ceil(clip.sourceDuration / frameSeconds).toInt()
    val latestVisibleRange by rememberUpdatedState(visibleRange)
    // 스크롤할 때마다 다시 그리지 않도록, 보이는 칸 번호가 바뀔 때만 다시 그린다.
    val frames by remember(frameWidth, sourceOffset, frameCount) {
        derivedStateOf {
            val range = latestVisibleRange()
            val first = floor((range.start + sourceOffset) / frameWidth).toInt() - FILMSTRIP_EXTRA_FRAMES
            val last = floor((range.endInclusive + sourceOffset) / frameWidth).toInt() + FILMSTRIP_EXTRA_FRAMES
            first.coerceAtLeast(0)..last.coerceAtMost(frameCount - 1)
        }
    }
    // 사진은 모든 칸이 같은 그림이므로 한 번만 읽는다.
    val photo = if (clip.type == MediaType.PHOTO) rememberFilmstripFrame(uri, clip.type, time = 0.0) else null
    Box(modifier = modifier) {
        for (index in frames) {
            key(index) {
                val time = filmstripFrameTime(index, frameSeconds).coerceAtMost(clip.sourceDuration - FILMSTRIP_END_MARGIN)
                // 확대해서 칸 사이가 가까우면 가까운 키 프레임 대신 그 시간의 프레임을 읽는다.
                val image = if (clip.type == MediaType.PHOTO) {
                    photo
                } else {
                    // 읽는 동안에는 두 칸 거리 안에서 이미 읽은 가장 가까운 프레임을 보여 준다.
                    rememberFilmstripFrame(
                        uri = uri,
                        type = clip.type,
                        time = time,
                        exact = frameSeconds <= EXACT_FRAME_SECONDS,
                        nearestWithin = frameSeconds * 2,
                    )
                }
                if (image != null) {
                    // 칸 경계를 정수 픽셀로 맞춰 칸 사이에 틈이 생기지 않게 한다.
                    val left = (index * frameWidth - sourceOffset).roundToInt()
                    val right = ((index + 1) * frameWidth - sourceOffset).roundToInt()
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.frameBounds(left = left, width = right - left),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

/**
 * Source time of the frame that a filmstrip frame shows.
 *
 * The time is on a grid of a power of two seconds (…, 0.25, 0.5, 1, 2 seconds). Thus the frames that a zoom
 * already loaded come back at a near zoom. The grid step is not longer than one filmstrip frame, so neighbor
 * frames do not show the same frame.
 *
 * @param index Position of the filmstrip frame from the source start.
 * @param frameSeconds Source seconds in one filmstrip frame.
 * @return The source time on the grid, nearest to the center of the filmstrip frame.
 */
internal fun filmstripFrameTime(index: Int, frameSeconds: Double): Double {
    val grid = 2.0.pow(floor(log2(frameSeconds)))
    // 반을 넘으면 올린다. round()는 x.5를 짝수 쪽으로 보내서, 칸 길이가 격자와 같을 때 이웃한 칸이 같은 시간이 된다.
    return floor((index + 0.5) * frameSeconds / grid + 0.5) * grid
}

/** Places the element at [left] pixels from the left edge of the parent, [width] pixels wide and as high as the parent. */
private fun Modifier.frameBounds(left: Int, width: Int): Modifier =
    offset { IntOffset(left, 0) }.layout { measurable, constraints ->
        val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
        layout(constraints.constrainWidth(width), placeable.height) { placeable.place(0, 0) }
    }

/** Light "+ 추가" button at the right edge of the timeline. It uses the icon button press behavior of the spec. */
@Composable
private fun AddClipButton(onClick: () -> Unit, modifier: Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = modifier
            .size(width = 56.dp, height = 62.dp)
            .pressClickable(pressedScale = PRESSED_SCALE_ICON, interactionSource = interactionSource, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) MagicColors.LightPressed else MagicColors.OnSurface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = MagicColors.Background, modifier = Modifier.size(24.dp))
        Text(text = "추가", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MagicColors.Background)
    }
}

/**
 * Handle that moves one edge of the selected clip, text overlay, or caption.
 *
 * @param target Part of the timeline that the handle drags.
 * @param side Side of the item that the handle is on.
 * @param height Height of the handle. Use the height of its track.
 * @param modifier Modifier for the handle. Give it the position.
 * @param onIntent Called with each user action.
 * @param pixelsPerSecond Width of one timeline second in pixels.
 * @param edgeScroller Scrolls the timeline while the handle is at the end of the timeline.
 * @param snapSeconds Largest distance in seconds that snaps the edge.
 * @param color Color of the handle.
 */
@Composable
internal fun TrimHandle(
    target: TimelineDragTarget,
    side: TrimEdge,
    height: Dp,
    modifier: Modifier,
    onIntent: (EditorIntent) -> Unit,
    pixelsPerSecond: Float,
    edgeScroller: TimelineEdgeScroller,
    snapSeconds: Double,
    color: Color = MagicColors.Accent,
) {
    val latestSnapSeconds by rememberUpdatedState(snapSeconds)
    val shape = if (side == TrimEdge.START) {
        RoundedCornerShape(topStart = 9.dp, bottomStart = 9.dp)
    } else {
        RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp)
    }
    val handle = remember { LayoutHolder() }
    Box(
        modifier = modifier
            .size(width = TrimHandleWidth, height = height)
            .onGloballyPositioned { handle.coordinates = it }
            .clip(shape)
            .background(color)
            .pointerInput(target) {
                var dragged = 0f
                var stopOffset = 0f
                var dragging = false
                val endDrag = {
                    dragging = false
                    edgeScroller.stop()
                    onIntent(EditorIntent.EndTimelineDrag)
                }
                try {
                    detectHorizontalDragGestures(
                        onDragStart = { start ->
                            dragged = 0f
                            // 손가락에서 손잡이 오른쪽 끝까지의 거리. 손잡이가 멈춤 선을 넘지 않게 한다.
                            stopOffset = TrimHandleWidth.toPx() - start.x
                            dragging = true
                            handle.coordinates?.let { edgeScroller.begin(it, start) }
                            onIntent(EditorIntent.StartTimelineDrag(target, latestSnapSeconds))
                        },
                        onDragEnd = endDrag,
                        onDragCancel = endDrag,
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragged += amount
                            // 손가락이 멈춤 선을 넘은 만큼은 손잡이를 움직이지 않는다. 그만큼은 타임라인이 스크롤한다.
                            val overshoot = handle.coordinates?.let { edgeScroller.follow(it, change.position, stopOffset) } ?: 0f
                            onIntent(EditorIntent.MoveTimelineDrag((dragged - overshoot) / pixelsPerSecond.toDouble()))
                        },
                    )
                } finally {
                    // 끄는 도중에 손잡이가 사라져도 끌기와 스크롤을 끝낸다.
                    if (dragging) endDrag()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = (height - 10.dp).coerceAtMost(18.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(MagicColors.OnAccent)
        )
    }
}

/**
 * Music bar of the timeline. It shows the title and the part of the track, and it is as long as the part or the
 * project, whichever is shorter. A tap opens the music sheet.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param secondWidth Width of one timeline second.
 */
@Composable
private fun MusicTrackRow(state: EditorState, onIntent: (EditorIntent) -> Unit, secondWidth: Dp) {
    Box(modifier = Modifier.fillMaxWidth().height(20.dp).padding(start = HandleInset)) {
        val project = state.project
        val track = state.musicTracks.find { it.id == project.musicId } ?: return@Box
        val range = project.musicRange ?: MusicRange(0.0, track.durationSeconds.toDouble())
        val seconds = minOf(range.length, project.totalDuration)
        Row(
            modifier = Modifier
                .width(maxOf(8.dp, secondWidth * seconds.toFloat() - TimelineItemGap))
                .height(20.dp)
                .pressClickable(pressedScale = 0.99f) { onIntent(EditorIntent.OpenSheet(EditorSheet.MUSIC)) }
                .clip(CircleShape)
                .background(MagicColors.MusicContainer)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MagicColors.Music, modifier = Modifier.size(13.dp))
            Text(
                text = "${track.title} · ${formatTime(range.start)}~${formatTime(range.end)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MagicColors.Music,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** Formats a speed like the design: 0.5, 1, 1.5, 2, 3. */
internal fun Double.toSpeedLabel(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

/** Keeps the layout of an element for code outside the composition, for example a gesture. */
internal class LayoutHolder {
    var coordinates: LayoutCoordinates? = null
}

/**
 * Scrolls the timeline while the finger that drags a trim handle or a text bar is near the left or the right end of the timeline.
 *
 * At the left end, the scrolling starts when the finger goes into [leftZone]. At the right end, the dragged element
 * stops [rightStop] before the end, so it does not go under the add button. When the finger goes past that stop,
 * the timeline scrolls. The scrolling goes faster when the finger goes nearer to the end. It stops when the finger
 * goes back or the drag ends.
 *
 * When a drag starts inside one of these areas, the start position is the limit instead. Thus the handle does not jump,
 * and the timeline scrolls only when the finger goes farther to the end.
 *
 * @param scope Scope for the scroll loop.
 * @param leftZone Width of the scroll area at the left end, in pixels.
 * @param rightStop Distance from the right end to the stop position of the dragged element, in pixels.
 * @param maxSpeed Fastest scroll speed in pixels per second.
 * @param onScroll Called each frame with the pixels to scroll. A negative value scrolls toward the project start.
 */
internal class TimelineEdgeScroller(
    private val scope: CoroutineScope,
    private val leftZone: Float,
    private val rightStop: Float,
    private val maxSpeed: Float,
    private val onScroll: (Double) -> Unit,
) {
    /** Visible area of the timeline. The finger position is measured in it. */
    var viewport: LayoutCoordinates? = null

    /** Scroll speed in pixels per second. A negative value scrolls toward the project start. */
    private var speed = 0f
    private var job: Job? = null

    /** Finger position in [viewport] when the current drag started. */
    private var startX = 0f

    /**
     * Keeps the finger position at the start of a drag.
     *
     * @param handle Layout of the dragged handle.
     * @param position Finger position in [handle].
     */
    fun begin(handle: LayoutCoordinates, position: Offset) {
        startX = timelineX(handle, position) ?: return
    }

    /**
     * Converts a position in an element to a horizontal position in [viewport].
     *
     * @param element Layout of the element.
     * @param position Position in [element].
     * @return The position in pixels, or null when a layout is not attached.
     */
    fun timelineX(element: LayoutCoordinates, position: Offset): Float? {
        val area = viewport?.takeIf { it.isAttached && element.isAttached } ?: return null
        return area.localPositionOf(element, position).x
    }

    /**
     * Sets the scroll speed from the finger position, and starts or stops the scrolling.
     *
     * @param handle Layout of the dragged element.
     * @param position Finger position in [handle].
     * @param stopOffset Distance from the finger to the part of the element that stops at the right stop, in pixels.
     *   Use 0 to stop the finger position itself.
     * @return Pixels that the finger went past the right stop. The element must not move by this part.
     */
    fun follow(handle: LayoutCoordinates, position: Offset, stopOffset: Float): Float {
        val area = viewport
        if (area == null || !area.isAttached || !handle.isAttached) {
            stop()
            return 0f
        }
        val x = area.localPositionOf(handle, position).x
        val width = area.size.width
        // 끄는 요소가 멈춤 선에 닿을 때의 손가락 위치. 멈춤 선 너머에서 잡았으면 잡은 위치다.
        val fingerStop = maxOf(width - rightStop - stopOffset, startX)
        val overshoot = (x - fingerStop).coerceAtLeast(0f)
        // 왼쪽 영역 안에서 잡았으면 잡은 위치보다 더 왼쪽으로 갈 때만 스크롤한다.
        val leftEdge = minOf(leftZone, startX)
        speed = when {
            x < leftEdge -> -maxSpeed * ((leftEdge - x) / leftEdge.coerceAtLeast(1f)).coerceAtMost(1f)
            overshoot > 0f -> maxSpeed * (overshoot / (width - fingerStop).coerceAtLeast(1f)).coerceAtMost(1f)
            else -> 0f
        }
        when {
            speed == 0f -> stop()
            job == null -> job = scope.launch {
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    onScroll(speed * (now - last) / 1_000_000_000.0)
                    last = now
                }
            }
        }
        return overshoot
    }

    /** Stops the scrolling. */
    fun stop() {
        speed = 0f
        job?.cancel()
        job = null
    }
}
