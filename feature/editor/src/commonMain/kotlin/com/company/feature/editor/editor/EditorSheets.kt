package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.SheetHeader
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.component.selectionRing
import com.company.core.designsystem.component.sheetIme
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.MusicRange
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.TransitionSlot
import com.company.feature.editor.preview.EditorPreviewData
import com.company.feature.editor.resources.*
import com.company.ui.editor.OverlayText
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.TextFrame
import com.company.ui.editor.TextLook
import com.company.ui.editor.displayName
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/** Horizontal padding of the editor sheets. */
internal val SheetPadding = 18.dp

/** Bottom padding of the editor sheets. */
internal val SheetBottomPadding = 22.dp

/** Bottom padding of the editor sheets while the keyboard is open. The free space above the keyboard is small. */
internal val SheetKeyboardBottomPadding = 10.dp

/**
 * Tells if the keyboard is open. Call it in the sheet content, because the sheet gives the keyboard insets
 * ([WindowInsets.sheetIme]).
 *
 * @return State that is true while the keyboard is open or moves.
 */
@Composable
internal fun rememberKeyboardOpen(): State<Boolean> {
    val keyboard = WindowInsets.sheetIme
    val density = LocalDensity.current
    return remember(keyboard, density) { derivedStateOf { keyboard.getBottom(density) > 0 } }
}

private val SpeedOptions = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

/**
 * Content of the open editor sheet.
 *
 * @param sheet Sheet to show.
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param onClose Closes the sheet with the close animation.
 */
@Composable
internal fun ColumnScope.EditorSheetContent(
    sheet: EditorSheet,
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    onClose: () -> Unit,
) {
    // 글자 시트는 제목 자리에 입력창을 두므로 머리글을 직접 그린다.
    val title = sheet.title(state)
    if (title != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetHeader(title = title, onClose = onClose)
            if (sheet == EditorSheet.LAYOUT) {
                LayoutIntro(state, onIntent)
            } else {
                sheet.intro(state)?.let { Text(text = it, fontSize = 13.sp, color = MagicColors.OnSurface60) }
            }
        }
    }
    when (sheet) {
        EditorSheet.LAYOUT -> LayoutSheet(state, onIntent)
        EditorSheet.TEXT -> TextSheet(state, onIntent, onClose)
        EditorSheet.MUSIC -> MusicSheet(state, onIntent)
        EditorSheet.SPEED -> SpeedSheet(state, onIntent)
        EditorSheet.CAPTIONS -> CaptionSheet(state, onIntent)
        EditorSheet.ANIMATION -> AnimationSheet(state, onIntent)
        EditorSheet.PASTE -> PasteSheet(state, onIntent)
        EditorSheet.SOUND_EFFECT -> SoundEffectSheet(state, onIntent)
        EditorSheet.DUBBING -> DubbingSheet(state, onIntent)
        EditorSheet.TRANSITION -> TransitionSheet(state, onIntent)
        EditorSheet.VOLUME -> VolumeSheet(state, onIntent)
    }
}

/** Title of the sheet, or null for the text sheet. The text sheet shows its text field in place of the title. */
@Composable
private fun EditorSheet.title(state: EditorState): String? = when (this) {
    EditorSheet.TEXT -> null
    EditorSheet.MUSIC -> stringResource(Res.string.menu_music)
    EditorSheet.SPEED -> stringResource(Res.string.menu_speed)
    EditorSheet.CAPTIONS -> stringResource(if (state.captionScript != null) Res.string.sheet_title_caption_script else Res.string.menu_caption)
    EditorSheet.LAYOUT -> stringResource(Res.string.menu_layout)
    EditorSheet.ANIMATION -> stringResource(Res.string.menu_animation)
    EditorSheet.PASTE -> stringResource(Res.string.menu_paste)
    EditorSheet.SOUND_EFFECT -> stringResource(Res.string.menu_sound_effect)
    EditorSheet.DUBBING -> stringResource(Res.string.menu_dubbing)
    EditorSheet.TRANSITION -> stringResource(Res.string.sheet_title_transition)
    EditorSheet.VOLUME -> stringResource(Res.string.menu_volume)
}

/** Short guide under the title, or null. */
@Composable
private fun EditorSheet.intro(state: EditorState): String? = when (this) {
    EditorSheet.TRANSITION -> transitionIntro(state)
    EditorSheet.VOLUME -> when (state.volumeTarget?.kind) {
        VolumeKind.CLIP -> stringResource(Res.string.sheet_intro_volume_clip)
        VolumeKind.DUBBING -> stringResource(Res.string.sheet_intro_volume_dubbing)
        VolumeKind.SOUND_EFFECT -> stringResource(Res.string.sheet_intro_volume_effect)
        null -> null
    }
    else -> null
}

// =====================================================================
// Layout, speed
// =====================================================================

/**
 * Guide of the layout sheet. A reset button goes back to the full layout when the project uses an other layout.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
private fun LayoutIntro(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    // 버튼이 나타나거나 사라져도 줄 높이가 바뀌지 않게 버튼 높이를 확보한다.
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.sheet_intro_layout),
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MagicColors.OnSurface60,
        )
        if (state.project.layout != LayoutType.FULL) {
            SmallPillButton(label = stringResource(Res.string.layout_reset), onClick = { onIntent(EditorIntent.ResetLayout) })
        }
    }
}

@Composable
private fun LayoutSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bleedHorizontal(SheetPadding)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SheetPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LayoutType.entries.forEach { layout ->
            val selected = state.project.layout == layout
            Column(
                modifier = Modifier.width(76.dp).pressClickable { onIntent(EditorIntent.PickLayout(layout)) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .aspectRatio(9f / 16f)
                        .selectionRing(selected, 10.dp)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    TemplatePreview(
                        template = EditorTemplate.Layout(layout),
                        scale = 0.29f,
                        modifier = Modifier.fillMaxSize(),
                        labelPadding = 3.dp to 0.dp,
                    )
                }
                Text(
                    text = layout.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MagicColors.OnSurface else MagicColors.OnSurface60,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun SpeedSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val clip = state.selectedClip
    SegmentedControl(
        options = SpeedOptions.map { SegmentOption(stringResource(Res.string.speed_factor, it.toSpeedLabel())) },
        selectedIndex = SpeedOptions.indexOf(clip?.speed),
        onSelect = { onIntent(EditorIntent.PickSpeed(SpeedOptions[it])) },
        containerColor = MagicColors.Background,
        itemHeight = 46.dp,
        spacing = 6.dp,
    )
    if (clip != null) {
        Text(
            text = stringResource(Res.string.speed_clip_length, ((clip.duration * 10).roundToInt() / 10.0).toString()),
            fontSize = 13.sp,
            color = MagicColors.OnSurface55,
        )
    }
}

// =====================================================================
// Shared parts of the sheets
// =====================================================================

/**
 * Slider of a sheet with the accent color.
 *
 * @param value Current value.
 * @param range Range of the value.
 * @param onChange Called with the new value while the thumb moves.
 * @param modifier Modifier for the slider.
 * @param steps Number of steps between the ends, or 0 for a continuous slider.
 * @param enabled Set to false for the disabled state.
 */
@Composable
internal fun SheetSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        valueRange = range,
        steps = steps,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = MagicColors.Accent,
            activeTrackColor = MagicColors.Accent,
            inactiveTrackColor = MagicColors.SurfaceHighest,
            activeTickColor = MagicColors.OnAccent.copy(alpha = 0.4f),
            inactiveTickColor = MagicColors.OnSurface40,
        ),
    )
}

/**
 * Returns the time since the animations started, in milliseconds. The cards of the sheets use it to play their
 * animations in a loop. The time changes on each frame while [running] is true. When [running] becomes true again,
 * the time starts again from 0.
 */
@Composable
internal fun rememberTextPreviewClock(running: Boolean): LongState {
    val clock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        clock.longValue = 0L
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { clock.longValue = it - start }
        }
    }
    return clock
}

/**
 * Sample text of a card. It shows [text] with [look] at the animation state [frame]. When the text is too large for
 * the card, it becomes smaller to fit.
 *
 * @param text Text to show.
 * @param look Look of the style at the card scale.
 * @param frame Animation state of the text.
 * @param scale Size factor of the card. The animation moves are made smaller in the same proportion.
 */
@Composable
internal fun BoxScope.StyleSample(text: String, look: TextLook, frame: TextFrame, scale: Float) {
    OverlayText(
        text = text,
        look = look,
        modifier = Modifier.matchParentSize().fitInside(SampleInset),
        // 글자를 줄여 보여 주므로, 움직이는 거리도 같은 비율로 줄인다.
        frame = frame.copy(offsetY = frame.offsetY * scale),
    )
}

/** Smallest space between the sample text and the edge of its card. */
private val SampleInset = 6.dp

/**
 * Measures the element at its own size, and places it in the center of the parent. When the element is larger than
 * the parent less [inset] on each side, it becomes smaller to fit. It never becomes larger.
 *
 * @param inset Smallest space between the element and each edge of the parent.
 */
internal fun Modifier.fitInside(inset: Dp): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints())
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val space = inset.roundToPx() * 2
    val fit = minOf(
        1f,
        (width - space).toFloat() / maxOf(1, placeable.width),
        (height - space).toFloat() / maxOf(1, placeable.height),
    ).coerceAtLeast(0f)
    layout(width, height) {
        placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
            scaleX = fit
            scaleY = fit
        }
    }
}

private val PreviewSheetState = EditorState(
    project = EditorPreviewData.project,
    musicTracks = EditorPreviewData.musicTracks,
    voices = EditorPreviewData.voices,
    soundEffects = EditorPreviewData.soundEffects,
)

// 시트는 별도 창(MagicModalBottomSheet)으로 열려 정적 프리뷰에 그려지지 않는다. 시트 표면과 콘텐츠만 보여 준다.
// 글자 시트는 세로로 스크롤되므로, 실제 시트처럼 높이에 상한을 둔다.
@Composable
private fun EditorSheetPreview(sheet: EditorSheet, state: EditorState = PreviewSheetState) {
    MagicPreview {
        Column(
            modifier = Modifier
                .heightIn(max = PHONE_PREVIEW_HEIGHT.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MagicColors.Surface)
                .padding(start = SheetPadding, end = SheetPadding, top = 24.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EditorSheetContent(sheet = sheet, state = state, onIntent = {}, onClose = {})
        }
    }
}

@Preview(name = "시트 - 레이아웃", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun LayoutSheetPreview() = EditorSheetPreview(EditorSheet.LAYOUT)

@Preview(name = "시트 - 글자", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun TextSheetPreview() = EditorSheetPreview(
    EditorSheet.TEXT,
    PreviewSheetState.copy(textDraft = TextDraft(id = null, text = "오늘의 기록", style = TextStyleType.IMPACT)),
)

@Preview(name = "시트 - 글자 꾸미기", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun TextDecorationSheetPreview() = EditorSheetPreview(
    EditorSheet.TEXT,
    PreviewSheetState.copy(
        textDraft = TextDraft(id = "t1", text = "바닷가 주말", style = TextStyleType.IMPACT, tab = TextSheetTab.DECORATION),
    ),
)

@Preview(name = "시트 - 음악", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun MusicSheetPreview() = EditorSheetPreview(
    EditorSheet.MUSIC,
    PreviewSheetState.copy(
        musicDraft = MusicDraft(musicId = "m5", range = MusicRange(4.0, 12.0), fade = true, volume = 80),
    ),
)


@Preview(name = "시트 - 속도", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun SpeedSheetPreview() = EditorSheetPreview(EditorSheet.SPEED, PreviewSheetState.copy(selectedClipId = "c2"))

@Preview(name = "시트 - 자막", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun CaptionSheetPreview() = EditorSheetPreview(EditorSheet.CAPTIONS)

@Preview(name = "시트 - 글로 자막 만들기", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun CaptionScriptSheetPreview() = EditorSheetPreview(EditorSheet.CAPTIONS, PreviewSheetState.copy(captionScript = "첫 줄\n둘째 줄"))

@Preview(name = "시트 - 더빙", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun DubbingSheetPreview() = EditorSheetPreview(
    EditorSheet.DUBBING,
    PreviewSheetState.copy(
        dubbingDraft = DubbingDraft(text = "자 오늘은", voiceId = "v1", speed = DubbingSpeed.NORMAL, captionId = "k1", editId = null),
    ),
)

@Preview(name = "시트 - 효과음", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun SoundEffectSheetPreview() = EditorSheetPreview(EditorSheet.SOUND_EFFECT)

@Preview(name = "시트 - 화면 전환", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun TransitionSheetPreview() = EditorSheetPreview(
    EditorSheet.TRANSITION,
    PreviewSheetState.copy(transitionSlot = TransitionSlot.Boundary("c2")),
)
