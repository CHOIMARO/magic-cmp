package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonLoading
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicIconButtonStyle
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.SheetHeader
import com.company.core.designsystem.component.SheetHeaderButtonHeight
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.component.selectionRing
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.clipAt
import com.company.feature.editor.preview.EditorPreviewData
import com.company.ui.editor.CaptionText
import com.company.ui.editor.OverlayText
import com.company.ui.editor.TextLook
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime
import com.company.ui.editor.look
import com.company.ui.editor.previewFrameAt
import com.company.ui.editor.toColorFilter
import kotlin.math.roundToInt

private val SheetPadding = 18.dp
private val SpeedOptions = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

/** Words of the sample caption in the caption style options. */
private val CaptionSampleWords = listOf("자막", "예시")

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
    val intro = when (sheet) {
        EditorSheet.LAYOUT -> "화면을 나누거나 액자처럼 꾸며요"
        EditorSheet.CAPTIONS -> if (state.project.captions.isNotEmpty()) {
            "스타일을 누르면 모든 자막에 바로 적용돼요"
        } else {
            "말소리를 알아듣고 자막을 자동으로 넣어요"
        }
        else -> null
    }
    // 글자 시트는 제목 자리에 입력창을 두므로 머리글을 직접 그린다.
    val title = sheet.title()
    if (title != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetHeader(title = title, onClose = onClose)
            if (intro != null) Text(text = intro, fontSize = 13.sp, color = MagicColors.OnSurface60)
        }
    }
    when (sheet) {
        EditorSheet.LAYOUT -> LayoutSheet(state, onIntent)
        EditorSheet.TEXT -> TextSheet(state, onIntent, onClose)
        EditorSheet.MUSIC -> MusicSheet(state, onIntent)
        EditorSheet.FILTER -> FilterSheet(state, onIntent)
        EditorSheet.SPEED -> SpeedSheet(state, onIntent)
        EditorSheet.CAPTIONS -> CaptionSheet(state, onIntent)
    }
}

/** Title of the sheet, or null for the text sheet. The text sheet shows its text field in place of the title. */
private fun EditorSheet.title(): String? = when (this) {
    EditorSheet.TEXT -> null
    EditorSheet.MUSIC -> "음악"
    EditorSheet.FILTER -> "필터"
    EditorSheet.SPEED -> "속도"
    EditorSheet.CAPTIONS -> "자막"
    EditorSheet.LAYOUT -> "레이아웃"
}

/** Hue of the clip at the playhead. The filter previews use it. */
private fun EditorState.currentHue(): Int = project.clipAt(time)?.clip?.thumbnailHue ?: 35

// =====================================================================
// Layout, filter
// =====================================================================

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
private fun FilterSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val hue = state.currentHue().toFloat()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bleedHorizontal(SheetPadding)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SheetPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterType.entries.forEach { filter ->
            val selected = state.project.filter == filter
            Column(
                modifier = Modifier.pressClickable { onIntent(EditorIntent.PickFilter(filter)) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StripeBox(
                    hue = hue,
                    lightness = 0.56f,
                    shape = RoundedCornerShape(14.dp),
                    colorFilter = remember(filter) { filter.toColorFilter() },
                    modifier = Modifier.size(width = 70.dp, height = 96.dp).selectionRing(selected, 14.dp),
                )
                Text(
                    text = filter.displayName,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MagicColors.OnSurface else MagicColors.OnSurface55,
                )
            }
        }
    }
}

// =====================================================================
// Text
// =====================================================================

/** Tab of the text sheet. */
private enum class TextSheetTab(val label: String) {
    /** Text styles. */
    TEMPLATE("템플릿"),

    /** Fonts. */
    FONT("글꼴"),
}

@Composable
private fun TextSheet(state: EditorState, onIntent: (EditorIntent) -> Unit, onClose: () -> Unit) {
    // 넣기·저장·삭제로 시트가 닫히면 ViewModel이 초안을 비운다. 닫힘 애니메이션 동안에는 마지막 초안을 그려서
    // 시트 높이가 줄지 않게 한다. 높이가 줄면 시트가 키보드와 함께 내려가지 못하고 먼저 내려앉는다.
    val lastDraft = remember { LastTextDraft() }
    state.textDraft?.let { lastDraft.value = it }
    val draft = lastDraft.value ?: return
    var tab by remember { mutableStateOf(TextSheetTab.TEMPLATE) }

    // 입력창, 넣기·저장 버튼, 탭은 키보드에 가리지 않도록 위에 고정한다.
    SheetHeader(
        onClose = onClose,
        action = {
            MagicButton(
                text = if (draft.id != null) "저장" else "넣기",
                onClick = { onIntent(EditorIntent.SaveText) },
                style = MagicButtonStyle.PRIMARY_SMALL,
                height = SheetHeaderButtonHeight,
            )
        },
    ) {
        TextDraftField(text = draft.text, onIntent = onIntent, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SegmentedControl(
            options = TextSheetTab.entries.map { SegmentOption(it.label) },
            selectedIndex = tab.ordinal,
            onSelect = { tab = TextSheetTab.entries[it] },
            modifier = Modifier.weight(1f),
            containerColor = MagicColors.Background,
            itemHeight = TEXT_TAB_HEIGHT.dp,
        )
        // 삭제는 실수로 누르지 않도록 저장 버튼과 다른 줄에 둔다. 스크롤해도 보이도록 탭 옆에 고정한다.
        if (draft.id != null) {
            MagicIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "삭제",
                onClick = { onIntent(EditorIntent.DeleteText) },
                style = MagicIconButtonStyle.DANGER,
                // 탭 높이(항목 높이 + 위아래 여백 4dp)와 맞춘다.
                size = (TEXT_TAB_HEIGHT + 8).dp,
                iconSize = 22.dp,
            )
        }
    }
    // 입력한 글자를 카드에 그대로 보여 준다. 비어 있으면 예시 글자를 쓴다.
    val sampleText = draft.text.trim().ifEmpty { SAMPLE_TEXT }
    // 키보드가 시트 아래쪽을 가리면 스크롤해서 고른다.
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TabPages(selectedIndex = tab.ordinal) {
            TextTemplatePage(draft = draft, text = sampleText, onIntent = onIntent, animate = tab == TextSheetTab.TEMPLATE)
            TextFontPage(draft = draft, text = sampleText, onIntent = onIntent, animate = tab == TextSheetTab.FONT)
        }
    }
}

/**
 * Text field of the text sheet. It gets the focus when the sheet opens, so the keyboard shows.
 *
 * @param text Current input.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the field.
 */
@Composable
private fun TextDraftField(text: String, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = text,
        onValueChange = { onIntent(EditorIntent.ChangeDraftText(it)) },
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MagicColors.Background)
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = TextStyle(
            color = MagicColors.OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MagicTheme.fonts.sans,
        ),
        cursorBrush = SolidColor(MagicColors.Accent),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    Text(
                        text = "글자를 입력하세요",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MagicColors.OnSurface40,
                        maxLines = 1,
                    )
                }
                innerTextField()
            }
        },
    )
}

/**
 * Shows only the page at [selectedIndex]. The height is the height of the tallest page, so the sheet keeps its
 * height when the user changes the tab. Each page must have one root layout.
 *
 * @param selectedIndex Index of the page to show.
 * @param modifier Modifier for the pages.
 * @param pages Pages in tab order.
 */
@Composable
private fun TabPages(selectedIndex: Int, modifier: Modifier = Modifier, pages: @Composable () -> Unit) {
    Layout(content = pages, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        // 고른 페이지만 놓는다. 놓지 않은 페이지는 그리지 않고 터치도 받지 않는다.
        layout(constraints.maxWidth, placeables.maxOf { it.height }) {
            placeables[selectedIndex].place(0, 0)
        }
    }
}

/**
 * Template tab of the text sheet. Each card plays the animation of its style in a loop.
 *
 * @param draft Text that the user types.
 * @param text Text in the cards.
 * @param onIntent Called with each user action.
 * @param animate True to play the animations. Use false while the tab is hidden.
 */
@Composable
private fun TextTemplatePage(draft: TextDraft, text: String, onIntent: (EditorIntent) -> Unit, animate: Boolean) {
    val clock = rememberTextPreviewClock(running = animate)
    FixedGrid(items = TextStyleType.entries, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { style ->
        TextSheetCard(
            label = style.displayName,
            selected = draft.style == style,
            onClick = { onIntent(EditorIntent.ChangeDraftStyle(style)) },
        ) {
            StyleSample(
                text = text,
                look = style.look(STYLE_SAMPLE_SCALE, draft.font),
                elapsedMillis = { clock.longValue },
            )
        }
    }
}

/**
 * Font tab of the text sheet. Each card shows the selected style with one font, and plays the animation of the
 * style in a loop. All cards use the same style, so they play together.
 *
 * @param draft Text that the user types.
 * @param text Text in the cards.
 * @param onIntent Called with each user action.
 * @param animate True to play the animations. Use false while the tab is hidden.
 */
@Composable
private fun TextFontPage(draft: TextDraft, text: String, onIntent: (EditorIntent) -> Unit, animate: Boolean) {
    val clock = rememberTextPreviewClock(running = animate)
    // 글꼴을 고르지 않았으면 스타일의 글꼴이 선택된 것으로 보인다.
    val selectedFont = draft.font ?: draft.style.look().font
    FixedGrid(items = TextFontType.entries, columns = 3, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { font ->
        TextSheetCard(
            label = font.displayName,
            selected = selectedFont == font,
            onClick = { onIntent(EditorIntent.ChangeDraftFont(font)) },
        ) {
            StyleSample(
                text = text,
                look = draft.style.look(STYLE_SAMPLE_SCALE, font),
                elapsedMillis = { clock.longValue },
            )
        }
    }
}

/**
 * Card of the text sheet: a striped preview box with a name under it.
 *
 * @param label Name under the preview.
 * @param selected True to draw the selection border.
 * @param onClick Action on click.
 * @param preview Content of the preview box.
 */
@Composable
private fun TextSheetCard(label: String, selected: Boolean, onClick: () -> Unit, preview: @Composable BoxScope.() -> Unit) {
    Column(modifier = Modifier.pressClickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = RoundedCornerShape(14.dp)
        StripeBox(
            hue = 250f,
            lightness = 0.42f,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .then(if (selected) Modifier.border(2.dp, MagicColors.Accent, shape) else Modifier),
            content = preview,
        )
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MagicColors.OnSurface else MagicColors.OnSurface60,
            maxLines = 1,
        )
    }
}

// =====================================================================
// Music and speed
// =====================================================================

@Composable
private fun MusicSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        MusicRow(
            title = "음악 없음",
            subtitle = "원본 소리만 · —",
            hue = null,
            selected = state.project.musicId == null,
            onClick = { onIntent(EditorIntent.PickMusic(null)) },
        )
        state.musicTracks.forEach { track ->
            MusicRow(
                title = track.title,
                subtitle = "${track.mood} · ${formatTime(track.durationSeconds.toDouble())}",
                hue = track.thumbnailHue,
                selected = state.project.musicId == track.id,
                onClick = { onIntent(EditorIntent.PickMusic(track.id)) },
            )
        }
    }
}

@Composable
private fun MusicRow(title: String, subtitle: String, hue: Int?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickable(pressedScale = 0.99f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MagicColors.SurfaceHigh else Color.Transparent)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hue != null) {
            StripeBox(hue = hue.toFloat(), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(46.dp))
        } else {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(MagicColors.SurfaceHigh))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, modifier = Modifier.padding(top = 2.dp), fontSize = 12.sp, color = MagicColors.OnSurface55)
        }
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(if (selected) MagicColors.Accent else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SpeedSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val clip = state.selectedClip
    SegmentedControl(
        options = SpeedOptions.map { SegmentOption("${it.toSpeedLabel()}배") },
        selectedIndex = SpeedOptions.indexOf(clip?.speed),
        onSelect = { onIntent(EditorIntent.PickSpeed(SpeedOptions[it])) },
        containerColor = MagicColors.Background,
        itemHeight = 46.dp,
        spacing = 6.dp,
    )
    if (clip != null) {
        Text(
            text = "이 클립 길이: ${(clip.duration * 10).roundToInt() / 10.0}초",
            fontSize = 13.sp,
            color = MagicColors.OnSurface55,
        )
    }
}

// =====================================================================
// Captions
// =====================================================================

@Composable
private fun CaptionSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    FixedGrid(items = CaptionStyleType.entries, columns = 3, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { style ->
        val selected = state.project.captionStyle == style
        Column(
            modifier = Modifier.pressClickable { onIntent(EditorIntent.PickCaptionStyle(style)) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shape = RoundedCornerShape(14.dp)
            StripeBox(
                hue = 160f,
                lightness = 0.42f,
                shape = shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .then(if (selected) Modifier.border(2.dp, MagicColors.Accent, shape) else Modifier),
            ) {
                // 정지 화면이라 마지막 단어를 지금 말하는 단어로 보여 준다.
                CaptionText(
                    words = CaptionSampleWords,
                    currentWord = CaptionSampleWords.lastIndex,
                    look = style.look(0.72f).copy(cornerRadius = 4f),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Text(
                text = style.displayName,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MagicColors.OnSurface else MagicColors.OnSurface60,
            )
        }
    }

    // 자막을 만드는 동안 버튼은 크기와 색을 유지하고 진행률만 보여준다. "자막 지우기"는 비활성이 된다.
    val progress = state.captionProgress
    val hasCaptions = state.project.captions.isNotEmpty()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (hasCaptions) {
            MagicButton(
                text = "자막 지우기",
                onClick = { onIntent(EditorIntent.RemoveCaptions) },
                modifier = Modifier.weight(1f),
                style = MagicButtonStyle.DANGER,
                enabled = progress == null,
                height = MagicButtonStyle.PRIMARY.height,
            )
        }
        MagicButton(
            text = if (hasCaptions) "다시 만들기" else "자막 만들기",
            onClick = { onIntent(EditorIntent.GenerateCaptions) },
            modifier = Modifier.weight(1f),
            style = MagicButtonStyle.PRIMARY,
            loading = progress?.let { MagicButtonLoading.Progress(label = "자막 만드는 중", fraction = it / 100f) },
        )
    }
}

/** Size factor of the sample text in a style card. */
private const val STYLE_SAMPLE_SCALE = 0.66f

/** Text in the cards of the text sheet while the text field is empty. */
private const val SAMPLE_TEXT = "가나다"

/** Height of each tab in the text sheet, in dp. */
private const val TEXT_TAB_HEIGHT = 38

/** Smallest space between the sample text and the edge of its card. */
private val SampleInset = 6.dp

/**
 * Returns the time since the animations started, in milliseconds. The cards of the text sheet use it to play their
 * animations in a loop. The time changes on each frame while [running] is true. When [running] becomes true again,
 * the time starts again from 0.
 */
@Composable
private fun rememberTextPreviewClock(running: Boolean): LongState {
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
 * Sample text of a card in the text sheet. It plays the entrance and the exit of [look] in a loop.
 *
 * The text breaks into lines as on the stage. When it is too large for the card, it becomes smaller to fit.
 * Only this function reads [elapsedMillis]. Thus only the sample, not the full sheet, changes on each frame.
 *
 * @param text Text to show.
 * @param look Look of the style at the card scale.
 * @param elapsedMillis Time since the animations started.
 */
@Composable
private fun BoxScope.StyleSample(text: String, look: TextLook, elapsedMillis: () -> Long) {
    val frame = look.animation.previewFrameAt(elapsedMillis())
    OverlayText(
        text = text,
        look = look,
        modifier = Modifier.matchParentSize().fitInside(SampleInset),
        // 글자를 줄여 보여 주므로, 밀어 올리는 거리도 같은 비율로 줄인다.
        frame = frame.copy(offsetY = frame.offsetY * STYLE_SAMPLE_SCALE),
    )
}

/**
 * Measures the element at its own size, and places it in the center of the parent. When the element is larger than
 * the parent less [inset] on each side, it becomes smaller to fit. It never becomes larger.
 *
 * @param inset Smallest space between the element and each edge of the parent.
 */
private fun Modifier.fitInside(inset: Dp): Modifier = this.layout { measurable, constraints ->
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

/** Last text draft of the open text sheet. It is not a screen state, so it is not a snapshot state. */
private class LastTextDraft {
    var value: TextDraft? = null
}

private val PreviewSheetState = EditorState(project = EditorPreviewData.project, musicTracks = EditorPreviewData.musicTracks)

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
    PreviewSheetState.copy(textDraft = TextDraft(id = null, text = "오늘의 기록", style = TextStyleType.BOLD)),
)

@Preview(name = "시트 - 음악", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun MusicSheetPreview() = EditorSheetPreview(EditorSheet.MUSIC)

@Preview(name = "시트 - 필터", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun FilterSheetPreview() = EditorSheetPreview(EditorSheet.FILTER)

@Preview(name = "시트 - 속도", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun SpeedSheetPreview() = EditorSheetPreview(EditorSheet.SPEED, PreviewSheetState.copy(selectedClipId = "c2"))

@Preview(name = "시트 - 자막", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun CaptionSheetPreview() = EditorSheetPreview(EditorSheet.CAPTIONS)

@Preview(name = "시트 - 자막 만드는 중", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun CaptionSheetProgressPreview() = EditorSheetPreview(
    EditorSheet.CAPTIONS,
    PreviewSheetState.copy(project = EditorPreviewData.project.copy(captions = emptyList()), captionProgress = 40),
)
