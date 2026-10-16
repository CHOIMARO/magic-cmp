package com.company.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.theme.MagicColors
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Smallest free space above the sheet. The sheet is never taller than the screen minus this space. */
private val SheetTopClearance = 56.dp

/** A downward drag longer than this closes the sheet when the user releases it. */
private val SwipeDismissDistanceThreshold = 100.dp

/** A fling faster than this per second closes or restores the sheet. It is the M3 anchoredDraggable default. */
private val SwipeDismissVelocityThreshold = 125.dp

/** Longest wait for the keyboard to close during the exit. Some devices do not send the IME inset updates. */
private const val ImeExitTimeoutMs = 600L

/** Longest wait for the scrim to fade out after the exit. */
private const val ScrimFadeTimeoutMs = 500L

/** Longest wait for the keyboard to start closing before the hide animation starts. */
private const val ImeStartTimeoutMs = 150L

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Keyboard insets of the content of the open [MagicModalBottomSheet], or null outside a sheet. */
private val LocalSheetKeyboardInsets = staticCompositionLocalOf<WindowInsets?> { null }

/**
 * Keyboard insets for the content of a [MagicModalBottomSheet]. Read them in the sheet content instead of
 * [WindowInsets.ime], so that Android and iOS get the same keyboard height. Outside a sheet they are [WindowInsets.ime].
 */
val WindowInsets.Companion.sheetIme: WindowInsets
    @Composable get() = LocalSheetKeyboardInsets.current ?: WindowInsets.ime

/**
 * Modal bottom sheet of the app. It wraps the M3 [ModalBottomSheet].
 *
 * The sheet turns off the M3 sheet gestures and does the swipe-to-close itself. Only a finger drag moves the sheet.
 * A list fling does not move it, so the sheet does not bounce at the end of a list.
 * - When the content is at the top, a downward drag moves the sheet down. On release, the sheet closes
 *   or goes back by distance and speed.
 * - A drag that started as a list scroll does not move the sheet. The user must start a new drag.
 * - An upward drag first brings a lowered sheet back, before the list scrolls.
 *
 * Only a sheet with a handle closes by a swipe or a tap on the scrim ([gestureDismissEnabled] follows
 * [showDragHandle]). A sheet with a close button closes only by the button. All close paths call [onDismissRequest]
 * after the hide animation ends, so the caller can clean up or navigate after the animation.
 *
 * Connect the close and confirm buttons of the content to `requestDismiss`. When the keyboard is open,
 * the sheet then moves down together with the keyboard and disappears at the same time.
 *
 * Use the `target` overload when the ViewModel state owns the sheet.
 *
 * @param onDismissRequest Called after the sheet closed and the hide animation ended.
 * @param modifier Modifier for the sheet container.
 * @param sheetState State of the sheet. Pass it to control the hide animation from outside.
 * @param showDragHandle True to show the handle at the top of the sheet.
 * @param gestureDismissEnabled True to close the sheet by a swipe or a tap on the scrim. Default is [showDragHandle].
 * @param backDismissEnabled False to block the system back action, for example while the app saves.
 * @param onBackRequest Called on the system back action instead of a close, for example to go to the previous step.
 * @param expansionState State of an expandable sheet. Null means the sheet does not expand.
 * @param containerColor Background of the sheet.
 * @param contentColor Default color of the content.
 * @param scrimColor Color of the scrim behind the sheet.
 * @param shape Shape of the sheet.
 * @param onCoveredHeightChange Called each time the sheet moves, with the height in pixels from the sheet top to the
 *   window bottom. The height includes the keyboard under the sheet. The last call gives 0, when the sheet closes.
 * @param content Content of the sheet. `requestDismiss` closes the sheet with the animation and then calls
 *   [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    showDragHandle: Boolean = false,
    gestureDismissEnabled: Boolean = showDragHandle,
    backDismissEnabled: Boolean = true,
    onBackRequest: (() -> Unit)? = null,
    expansionState: SheetExpansionState? = null,
    containerColor: Color = MagicColors.Surface,
    contentColor: Color = MagicColors.OnSurface,
    scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    shape: Shape = SheetShape,
    onCoveredHeightChange: ((Int) -> Unit)? = null,
    content: @Composable ColumnScope.(requestDismiss: () -> Unit) -> Unit,
) {
    val exitController = rememberSheetExitController(sheetState)
    val requestDismiss = rememberSheetDismissRequest(exitController, onDismissRequest)

    MagicModalBottomSheetContainer(
        exitController = exitController,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        showDragHandle = showDragHandle,
        gestureDismissEnabled = gestureDismissEnabled,
        backDismissEnabled = backDismissEnabled,
        onBackRequest = onBackRequest,
        expansionState = expansionState,
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = scrimColor,
        shape = shape,
        onCoveredHeightChange = onCoveredHeightChange,
    ) {
        content(requestDismiss)
    }
}

/**
 * [MagicModalBottomSheet] for a sheet that the ViewModel state shows and hides.
 *
 * When [target] becomes null, the sheet does not disappear at once. It stays until the exit animation ends.
 * Pass the state as it is. Do not wrap the call in `target?.let { }`, or the exit animation cannot run.
 *
 * Content buttons close the sheet with `requestDismiss`. The ViewModel can also set the state to null.
 * Both paths run the same exit only one time.
 *
 * @param target Item to show in the sheet. Null means the sheet is closed.
 * @param content Content of the sheet. `target` is the last item, also during the exit. `requestDismiss` closes
 *   the sheet with the animation and then calls [onDismissRequest].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> MagicModalBottomSheet(
    target: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    showDragHandle: Boolean = false,
    gestureDismissEnabled: Boolean = showDragHandle,
    backDismissEnabled: Boolean = true,
    onBackRequest: (() -> Unit)? = null,
    expansionState: SheetExpansionState? = null,
    containerColor: Color = MagicColors.Surface,
    contentColor: Color = MagicColors.OnSurface,
    scrimColor: Color = Color.Black.copy(alpha = 0.4f),
    shape: Shape = SheetShape,
    onCoveredHeightChange: ((Int) -> Unit)? = null,
    content: @Composable ColumnScope.(target: T, requestDismiss: () -> Unit) -> Unit,
) {
    val exitController = rememberSheetExitController(sheetState)
    val requestDismiss = rememberSheetDismissRequest(exitController, onDismissRequest)
    // 퇴장이 끝날 때까지 유지하는 마지막 대상. 상위 상태가 null이 되어도 이 값이 남아 시트가 살아 있다.
    var lastTarget by remember { mutableStateOf(target) }

    LaunchedEffect(target) {
        if (target == null) {
            if (lastTarget != null) {
                exitController.runExit()
                lastTarget = null
            }
        } else {
            // 다시 열릴 때 이전 퇴장 상태(변위·완료 표시)를 지운다.
            exitController.reset()
            lastTarget = target
        }
    }

    // 열림은 즉시 반영하고(프레임 지연 없음), 닫힘만 퇴장 완료까지 마지막 대상을 그린다.
    val shownTarget = target ?: lastTarget
    if (shownTarget != null) {
        MagicModalBottomSheetContainer(
            exitController = exitController,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            showDragHandle = showDragHandle,
            gestureDismissEnabled = gestureDismissEnabled,
            backDismissEnabled = backDismissEnabled,
            onBackRequest = onBackRequest,
            expansionState = expansionState,
            containerColor = containerColor,
            contentColor = contentColor,
            scrimColor = scrimColor,
            shape = shape,
            onCoveredHeightChange = onCoveredHeightChange,
        ) {
            content(shownTarget, requestDismiss)
        }
    }
}

/** Close request for content buttons. It runs the exit one time and then calls [onDismissRequest]. */
@Composable
private fun rememberSheetDismissRequest(
    exitController: SheetExitController,
    onDismissRequest: () -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    return remember(scope, exitController) {
        {
            scope.launch {
                exitController.runExit()
                currentOnDismissRequest()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberSheetExitController(sheetState: SheetState): SheetExitController {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    return remember(scope, sheetState, density) {
        SheetExitController(scope = scope, sheetState = sheetState, density = density)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MagicModalBottomSheetContainer(
    exitController: SheetExitController,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetState: SheetState,
    showDragHandle: Boolean,
    gestureDismissEnabled: Boolean,
    backDismissEnabled: Boolean,
    onBackRequest: (() -> Unit)?,
    expansionState: SheetExpansionState?,
    containerColor: Color,
    contentColor: Color,
    scrimColor: Color,
    shape: Shape,
    onCoveredHeightChange: ((Int) -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val currentGestureDismissEnabled by rememberUpdatedState(gestureDismissEnabled)
    val density = LocalDensity.current

    val swipeController = remember(sheetState, density, expansionState) {
        SheetSwipeDismissController(
            scope = scope,
            sheetState = sheetState,
            dismissDistancePx = with(density) { SwipeDismissDistanceThreshold.toPx() },
            dismissVelocityPx = with(density) { SwipeDismissVelocityThreshold.toPx() },
            isSwipeEnabled = { currentGestureDismissEnabled },
            onDismissed = { currentOnDismissRequest() },
            expansion = expansionState,
        )
    }
    // 확장형 시트는 제스처 닫기가 꺼져 있어도 끌어서 키우고 접을 수 있어야 한다.
    val dragEnabled = gestureDismissEnabled || expansionState != null
    // 시트 밖(화면)의 키보드 인셋. iOS에서는 시트 안의 키보드 인셋이 0이므로 이 값을 시트 콘텐츠에 넘긴다.
    val windowImeInsets = WindowInsets.ime

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier
            // 모든 시트 공통 최대 높이 규칙: 화면 상단에서 최소 56dp(상태바 인셋이 더 크면 상태바만큼)를 비워 둔다.
            // 시트가 짧으면 차이가 없고, 콘텐츠가 길 때만 최대 높이 제한으로 작동한다.
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets(top = SheetTopClearance)))
            // 드래그 변위와 퇴장 변위를 시트 전체(핸들 + 콘텐츠)에 반영한다. 퇴장 변위는 레이아웃 단계에서
            // IME 인셋을 읽어 계산하므로 리컴포지션 없이 키보드와 프레임 단위로 맞춰 움직인다.
            .offset {
                val offsetPx = swipeController.offsetPx + exitController.exitOffsetPx()
                IntOffset(x = 0, y = offsetPx.roundToInt())
            },
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = scrimColor,
        // Android는 M3가 키보드 인셋을 먼저 소비해서, 키보드가 열리면 시트 아래 시스템 바 여백이 없어진다.
        // iOS는 키보드가 열려도 홈 인디케이터 여백이 남으므로, 키보드 높이만큼 빼서 Android와 맞춘다.
        contentWindowInsets = if (SheetWindowHasKeyboardInsets) {
            { BottomSheetDefaults.windowInsets }
        } else {
            { BottomSheetDefaults.windowInsets.exclude(windowImeInsets) }
        },
        // 시트 밖(scrim) 터치 닫기는 제스처 닫기와 한 묶음으로 손잡이 시트에서만 허용한다.
        // 뒤로가기 닫기는 아래 SystemBackHandler가 동적으로 제어한다. M3는 다이얼로그를 만들 때의
        // shouldDismissOnBackPress만 반영하고 이후 변경은 무시하므로 true로 고정한다.
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = gestureDismissEnabled,
        ),
        dragHandle = if (showDragHandle) {
            {
                SwipeDismissDragHandle(
                    enabled = dragEnabled,
                    onDrag = { delta -> swipeController.dragBy(delta) },
                    onDragStopped = { velocity -> swipeController.endDrag(velocity) },
                    onHeightChange = { height -> exitController.dragHandleHeightPx = height },
                )
            }
        } else {
            null
        },
    ) {
        // Android에서 시트는 별도 다이얼로그 윈도우이므로 IME 인셋과 키보드도 이 윈도우의 것을 써야 한다.
        // 시트 밖(화면 윈도우)의 키보드 컨트롤러로는 시트 안 입력창의 키보드를 내릴 수 없다.
        // iOS에서는 시트 안의 IME 인셋이 0이므로 시트 밖의 IME 인셋을 쓴다.
        val dialogImeInsets = if (SheetWindowHasKeyboardInsets) WindowInsets.ime else windowImeInsets
        val dialogFocusManager = LocalFocusManager.current
        val dialogKeyboardController = LocalSoftwareKeyboardController.current
        SideEffect {
            exitController.imeInsets = dialogImeInsets
            exitController.hideKeyboard = {
                dialogFocusManager.clearFocus(force = true)
                dialogKeyboardController?.hide()
            }
        }

        // 시트의 기본 뒤로가기(닫기)보다 나중에 등록되어, 활성일 때 먼저 처리된다.
        // - onBackRequest가 있으면 시트를 닫지 않고 이전 단계로 돌아간다.
        // - backDismissEnabled가 false면 뒤로가기를 소비해서 닫힘을 막는다.
        SystemBackHandler(enabled = onBackRequest != null || !backDismissEnabled) {
            onBackRequest?.invoke()
        }

        val currentOnCoveredHeightChange by rememberUpdatedState(onCoveredHeightChange)
        DisposableEffect(Unit) {
            onDispose { currentOnCoveredHeightChange?.invoke(0) }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 퇴장 이동 거리(시트가 화면 밖으로 나가는 데 필요한 거리)로 사용한다.
                .onSizeChanged { size -> exitController.contentHeightPx = size.height }
                // 시트 윗변(손잡이 포함)부터 창 아래까지의 높이를 알린다. 열림·끌기·퇴장·키보드 이동이 모두 위치에 반영된다.
                .onGloballyPositioned { coordinates ->
                    val listener = currentOnCoveredHeightChange ?: return@onGloballyPositioned
                    val sheetTop = coordinates.positionInRoot().y - exitController.dragHandleHeightPx
                    val windowHeight = coordinates.findRootCoordinates().size.height
                    listener((windowHeight - sheetTop).roundToInt().coerceAtLeast(0))
                }
                .nestedScroll(swipeController)
                // 스크롤 컨테이너가 없는 콘텐츠용. 자식 스크롤이 제스처를 소비하면 이 드래그는 동작하지 않는다.
                .draggable(
                    orientation = Orientation.Vertical,
                    enabled = dragEnabled,
                    state = rememberDraggableState { delta -> swipeController.dragBy(delta) },
                    onDragStopped = { velocity -> swipeController.endDrag(velocity) },
                ),
        ) {
            CompositionLocalProvider(LocalSheetKeyboardInsets provides dialogImeInsets) {
                content()
            }
        }
    }
}

/**
 * Moves the sheet out together with the closing keyboard.
 *
 * The keyboard animation has no known length, so the exit follows the progress of the IME inset, not the time.
 * The progress is `1 - current / start` of the IME bottom inset of the sheet window. When the keyboard is
 * fully closed, the sheet is fully out of the screen.
 *
 * The travel distance is the content height plus the handle height. The M3 `imePadding` already moves the sheet
 * down by the keyboard height while the keyboard closes.
 *
 * The M3 scrim fades out only when `targetValue == Hidden`, so the exit also runs `hide()`. The moving anchor stops
 * `hide()` from making much progress, but this controller makes the visible movement.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class SheetExitController(
    private val scope: CoroutineScope,
    private val sheetState: SheetState,
    private val density: Density,
) {
    /** IME insets of the sheet window. The sheet content sets it. */
    var imeInsets: WindowInsets? = null

    /** Closes the keyboard of the sheet window. The sheet content sets it. */
    var hideKeyboard: () -> Unit = {}

    /** Height of the sheet content in pixels. */
    var contentHeightPx by mutableIntStateOf(0)

    /** Height of the handle in pixels. The handle is above the content, so it adds to the travel distance. */
    var dragHandleHeightPx by mutableIntStateOf(0)

    /** True during the exit. The layout step reads it, so it is a state. */
    private var isExiting by mutableStateOf(false)

    /** IME height in pixels when the exit started. It is the denominator of the progress. */
    private var imeStartPx = 0

    /** Running or completed exit. The button path and the state path share it. */
    private var runner: Deferred<Unit>? = null

    /** Exit offset in pixels. The layout step reads it, so it updates each frame without a recomposition. */
    fun exitOffsetPx(): Float {
        if (!isExiting) return 0f
        val startPx = imeStartPx
        val insets = imeInsets
        if (startPx <= 0 || insets == null) return 0f
        val progress = (1f - insets.getBottom(density).toFloat() / startPx).coerceIn(0f, 1f)
        return progress * (contentHeightPx + dragHandleHeightPx)
    }

    /** Clears the previous exit when the sheet opens again. */
    fun reset() {
        runner = null
        isExiting = false
        imeStartPx = 0
    }

    /** Runs the exit only one time. If an exit runs or ended, it only waits for the end. */
    suspend fun runExit() {
        val existing = runner
        if (existing != null) {
            existing.join()
            return
        }
        val started = scope.async { exit() }
        runner = started
        started.join()
    }

    private suspend fun exit() {
        val insets = imeInsets
        val travelPx = contentHeightPx + dragHandleHeightPx
        val startPx = insets?.getBottom(density) ?: 0
        if (insets == null || startPx <= 0 || travelPx <= 0) {
            // 키보드가 없으면 맞출 대상도 없다. 기존 슬라이드 다운 그대로 닫는다.
            runCatching { sheetState.hide() }
            return
        }
        imeStartPx = startPx
        isExiting = true
        // 콘텐츠가 이미 키보드를 내렸더라도 한 번 더 요청한다(진행 중이면 무시된다).
        hideKeyboard()
        // 키보드가 실제로 내려가기 시작한 뒤에 hide를 시작한다. 먼저 시작하면 키보드가 아직 움직이지 않는
        // 첫 프레임들에 앵커가 고정되어 있어서, 시트만 한 박자 먼저 내려간다.
        withTimeoutOrNull(ImeStartTimeoutMs) {
            snapshotFlow { insets.getBottom(density) }.first { bottom -> bottom < startPx }
        }
        val hideJob = scope.launch { runCatching { sheetState.hide() } }
        withTimeoutOrNull(ImeExitTimeoutMs) {
            snapshotFlow { insets.getBottom(density) }.first { bottom -> bottom <= 0 }
        }
        // scrim이 다 걷힌 뒤에 윈도우를 해체해서, 마지막 프레임에 딤이 튀지 않게 한다.
        withTimeoutOrNull(ScrimFadeTimeoutMs) { hideJob.join() }
    }
}

/**
 * Keeps the swipe-to-close offset of the sheet, and the expansion of an expandable sheet.
 *
 * It moves the sheet only by the downward finger drag that the content did not use. On release ([endDrag]),
 * it closes the sheet or moves it back by distance and speed. It ignores the rest of a list fling.
 *
 * The sheet takes over only when the gesture started at the top of the content. A scroll that reaches the top
 * during the gesture does not move the sheet until the user starts a new drag.
 *
 * With [expansion], an upward drag first brings a lowered sheet back and then expands the sheet.
 * A downward drag first collapses the sheet and then moves it down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class SheetSwipeDismissController(
    private val scope: CoroutineScope,
    private val sheetState: SheetState,
    private val dismissDistancePx: Float,
    private val dismissVelocityPx: Float,
    private val isSwipeEnabled: () -> Boolean,
    private val onDismissed: () -> Unit,
    private val expansion: SheetExpansionState?,
) : NestedScrollConnection {

    private val offsetState = mutableFloatStateOf(0f)
    private var settleJob: Job? = null
    private var expansionSettleJob: Job? = null

    /**
     * True when the content used a downward delta in this gesture (a list that is not at the top scrolled).
     * Then the sheet does not take over in the same gesture. [onPreFling] clears it at the end of the gesture.
     */
    private var isHandoffBlocked = false

    /** Downward offset of the sheet in pixels. */
    val offsetPx: Float
        get() = offsetState.floatValue

    /** Applies a drag delta to the sheet and returns the used amount. */
    fun dragBy(deltaPx: Float): Float {
        settleJob?.cancel()
        expansionSettleJob?.cancel()
        return if (deltaPx < 0f) dragUp(deltaPx) else dragDown(deltaPx)
    }

    private fun dragUp(deltaPx: Float): Float {
        var remaining = deltaPx
        val offset = offsetState.floatValue
        if (offset > 0f) {
            val newOffset = (offset + remaining).coerceAtLeast(0f)
            remaining -= newOffset - offset
            offsetState.floatValue = newOffset
        }
        if (remaining < 0f && expansion != null) {
            val before = expansion.expansionPx
            expansion.setExpansion(before - remaining)
            remaining += expansion.expansionPx - before
        }
        return deltaPx - remaining
    }

    private fun dragDown(deltaPx: Float): Float {
        var remaining = deltaPx
        if (expansion != null && expansion.expansionPx > 0f) {
            val before = expansion.expansionPx
            expansion.setExpansion(before - remaining)
            remaining -= before - expansion.expansionPx
        }
        if (remaining > 0f && isSwipeEnabled()) {
            offsetState.floatValue += remaining
            remaining = 0f
        }
        return deltaPx - remaining
    }

    /**
     * Decides the end of a drag. A lowered sheet closes when the speed or the distance is over the limit.
     * Otherwise it springs back. An expandable sheet stopped in the middle settles to collapsed or expanded.
     */
    fun endDrag(velocityY: Float) {
        if (offsetState.floatValue <= 0f) {
            settleExpansion(velocityY)
            return
        }
        val shouldDismiss = isSwipeEnabled() && when {
            velocityY >= dismissVelocityPx -> true
            velocityY <= -dismissVelocityPx -> false
            else -> offsetState.floatValue >= dismissDistancePx
        }
        if (shouldDismiss) {
            // 변위를 유지한 채 hide가 진행되므로 현재 위치에서 이어서 내려간다.
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                offsetState.floatValue = 0f
                onDismissed()
            }
        } else {
            settleJob = scope.launch {
                animate(
                    initialValue = offsetState.floatValue,
                    targetValue = 0f,
                    initialVelocity = velocityY,
                ) { value, _ -> offsetState.floatValue = value }
            }
        }
    }

    /** Settles an expandable sheet stopped in the middle, by the fling direction or by the half point. */
    private fun settleExpansion(velocityY: Float) {
        val expansion = expansion ?: return
        if (!expansion.isSettling) return
        val current = expansion.expansionPx
        val max = expansion.maxExpansionPx
        val target = when {
            velocityY <= -dismissVelocityPx -> max
            velocityY >= dismissVelocityPx -> 0f
            else -> if (current >= max / 2f) max else 0f
        }
        expansionSettleJob = scope.launch {
            animate(
                initialValue = current,
                targetValue = target,
                // 펼침 높이는 위로 갈수록 커지므로 드래그 속도와 부호가 반대다.
                initialVelocity = -velocityY,
            ) { value, _ -> expansion.setExpansion(value) }
        }
    }

    // =====================================================================
    // Nested scroll: hand-off between the scroll content and the sheet offset
    // =====================================================================

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        // 위로 끌면 리스트 스크롤보다 먼저 내려간 시트를 되돌리고, 확장형 시트는 이어서 펼친다.
        if (available.y < 0f) {
            return Offset(x = 0f, y = dragBy(available.y))
        }
        return Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        // 콘텐츠가 아래 방향 델타를 소비했다면(최상단이 아닌 리스트가 스크롤됨) 이번 제스처의 핸드오프를 막는다.
        if (consumed.y > 0f) {
            isHandoffBlocked = true
        }
        // 리스트 최상단이라 소비되지 못한 아래 방향 드래그를 접힘 → 시트 변위로 넘긴다.
        // 제스처 시작부터 최상단이었던 경우에만 넘긴다.
        if (available.y > 0f && !isHandoffBlocked) {
            return Offset(x = 0f, y = dragBy(available.y))
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // 손을 뗀 시점이다. 다음 제스처를 위해 핸드오프 차단을 푼다.
        isHandoffBlocked = false
        // 시트가 내려가 있거나 펼침 중간에 멈춰 있으면, 리스트 플링 대신 시트 정착 판정에 속도를 쓴다.
        if (offsetState.floatValue > 0f || expansion?.isSettling == true) {
            endDrag(available.y)
            return available
        }
        return Velocity.Zero
    }
}

/**
 * Handle area at the top of the sheet. A downward drag here moves the sheet. The whole band is the touch target.
 *
 * @param onHeightChange Called with the band height in pixels. The exit adds it to the travel distance.
 */
@Composable
private fun SwipeDismissDragHandle(
    enabled: Boolean,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onHeightChange: (Int) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size -> onHeightChange(size.height) }
            .draggable(
                orientation = Orientation.Vertical,
                enabled = enabled,
                state = rememberDraggableState { delta -> onDrag(delta) },
                onDragStopped = { velocity -> onDragStopped(velocity) },
            )
            .padding(top = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        SheetHandle()
    }
}
