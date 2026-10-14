package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicIconButtonStyle
import com.company.core.designsystem.component.MagicSpinner
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.SheetHeader
import com.company.core.designsystem.component.SheetHeaderButtonHeight
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.Decoration
import com.company.core.domain.model.editor.SIZE_STEP_COUNT
import com.company.core.domain.model.editor.TextFontType
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.valuesIn
import com.company.ui.editor.TextFrame
import com.company.ui.editor.displayName
import com.company.ui.editor.fontFamily
import com.company.ui.editor.isAvailable
import com.company.ui.editor.look
import com.company.ui.editor.previewFrameAt
import com.company.ui.editor.rgbColor

/** Size factor of the sample text in a style card. */
internal const val STYLE_SAMPLE_SCALE = 0.66f

/** Text in the cards of the text sheet while the text field is empty. */
private const val SAMPLE_TEXT = "가나다"

/** Height of each tab in the text sheet, in dp. */
private const val TEXT_TAB_HEIGHT = 38

/** Height of the scrolling part under the tabs. */
private val TextSheetBodyHeight = 220.dp

/** Width of a card in the strips above the keyboard. It is near the width of a card in the template grid. */
private val StripCardWidth = 76.dp

/** Height of each tab in the small tab row above the keyboard, in dp. */
private const val TYPING_TAB_HEIGHT = 24

/** Width of the small tab row above the keyboard. */
private val TypingTabsWidth = 210.dp

/** Diameters of the five size step circles in the decoration strip. */
private val StripSizeStepDiameters = listOf(9.dp, 12.dp, 15.dp, 19.dp, 23.dp)

/** Main colors of the color and background palettes: pastel colors and white and black. */
internal val DecorationPalette: List<Int> = listOf(
    0xFFFFFF, 0x1A1A1A, 0xFFB3BA, 0xFFD6A5, 0xFDFFB6, 0xCAFFBF, 0x9BF6FF, 0xA0C4FF, 0xBDB2FF, 0xFFC6FF, 0xF1E4D3, 0xC7CEEA,
)

/** Diameters of the five size step circles. */
private val SizeStepDiameters = listOf(14.dp, 19.dp, 24.dp, 30.dp, 36.dp)

/**
 * Text sheet. It opens without the keyboard, so the user sees the stage while the user picks a style.
 *
 * - Without the keyboard, the sheet shows the text field row, the tabs, and the tab page.
 * - With the keyboard, the keyboard goes over the tab page. Only the text field row, a small tab row, and a strip of
 *   the open tab stay above the keyboard. When the keyboard closes, the sheet shows the full tab page again.
 *
 * Both states show the same tab. A caption draft shows only the decoration tab, so it has no tab row.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param onClose Closes the sheet.
 */
@Composable
internal fun TextSheet(state: EditorState, onIntent: (EditorIntent) -> Unit, onClose: () -> Unit) {
    // 넣기·저장·삭제로 시트가 닫히면 ViewModel이 초안을 비운다. 닫힘 애니메이션 동안에는 마지막 초안을 그려서
    // 시트 높이가 줄지 않게 한다. 높이가 줄면 시트가 키보드와 함께 내려가지 못하고 먼저 내려앉는다.
    val lastDraft = remember { LastTextDraft() }
    state.textDraft?.let { lastDraft.value = it }
    val draft = lastDraft.value ?: return
    val keyboardOpen by rememberKeyboardOpen()
    // 키보드 없이 그린 탭 영역(위 간격 포함)의 높이. 키보드가 열리는 동안 시트 윗변을 이 높이에 맞춰 둔다.
    var tabAreaHeight by remember { mutableIntStateOf(0) }

    // 머리글 아래 간격을 상태마다 다르게 두려고 머리글과 아래 영역을 한 열에 묶는다.
    Column {
        SheetHeader(
            onClose = onClose,
            action = {
                MagicButton(
                    text = if (draft.id != null || draft.isCaption) "저장" else "넣기",
                    onClick = { onIntent(EditorIntent.SaveText) },
                    style = MagicButtonStyle.PRIMARY_SMALL,
                    height = SheetHeaderButtonHeight,
                )
            },
        ) {
            TextDraftField(draft = draft, onIntent = onIntent, modifier = Modifier.weight(1f))
        }
        // 입력한 글자를 카드에 그대로 보여 준다. 비어 있으면 예시 글자를 쓴다.
        val sampleText = draft.text.trim().ifEmpty { SAMPLE_TEXT }
        if (keyboardOpen) {
            val keyboardAboveBars = WindowInsets.ime.exclude(WindowInsets.systemBars.union(WindowInsets.displayCutout))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .keepSheetTop(
                        keyboard = keyboardAboveBars,
                        height = { tabAreaHeight },
                        extra = SheetBottomPadding - SheetKeyboardBottomPadding,
                    )
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!draft.isCaption) TypingTabs(draft = draft, onIntent = onIntent)
                when (draft.tab) {
                    TextSheetTab.TEMPLATE -> TemplateStrip(draft = draft, text = sampleText, onIntent = onIntent)
                    TextSheetTab.FONT -> FontStrip(draft = draft, text = sampleText, downloading = state.fontDownloading, onIntent = onIntent)
                    TextSheetTab.DECORATION -> DecorationStrip(draft = draft, recentColors = state.recentColors, onIntent = onIntent)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .onSizeChanged { size -> tabAreaHeight = size.height }
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextSheetTabs(draft = draft, onIntent = onIntent)
                Box(modifier = Modifier.fillMaxWidth().height(TextSheetBodyHeight).bleedHorizontal(SheetPadding)) {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = SheetPadding)) {
                        when (draft.tab) {
                            TextSheetTab.TEMPLATE -> TextTemplatePage(draft = draft, text = sampleText, onIntent = onIntent)
                            TextSheetTab.FONT -> TextFontPage(draft = draft, downloading = state.fontDownloading, onIntent = onIntent)
                            TextSheetTab.DECORATION -> DecorationPage(draft = draft, recentColors = state.recentColors, onIntent = onIntent)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tabs of the text sheet, and the delete button of a saved text or a caption.
 *
 * @param draft Draft of the sheet.
 * @param onIntent Called with each user action.
 */
@Composable
private fun TextSheetTabs(draft: TextDraft, onIntent: (EditorIntent) -> Unit) {
    val tabs = if (draft.isCaption) listOf(TextSheetTab.DECORATION) else TextSheetTab.entries
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        SegmentedControl(
            options = tabs.map { SegmentOption(it.label) },
            selectedIndex = tabs.indexOf(draft.tab).coerceAtLeast(0),
            onSelect = { onIntent(EditorIntent.SelectTextTab(tabs[it])) },
            modifier = Modifier.weight(1f),
            containerColor = MagicColors.Background,
            itemHeight = TEXT_TAB_HEIGHT.dp,
        )
        // 삭제는 실수로 누르지 않도록 저장 버튼과 다른 줄에 둔다. 스크롤해도 보이도록 탭 옆에 고정한다.
        if (draft.id != null || draft.isCaption) {
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
}

/**
 * Small tab row above the keyboard. It changes the same tab as the tabs without the keyboard.
 *
 * @param draft Draft of the sheet.
 * @param onIntent Called with each user action.
 */
@Composable
private fun TypingTabs(draft: TextDraft, onIntent: (EditorIntent) -> Unit) {
    val tabs = TextSheetTab.entries
    SegmentedControl(
        options = tabs.map { SegmentOption(it.label) },
        selectedIndex = tabs.indexOf(draft.tab),
        onSelect = { onIntent(EditorIntent.SelectTextTab(tabs[it])) },
        modifier = Modifier.width(TypingTabsWidth),
        containerColor = MagicColors.Background,
        itemHeight = TYPING_TAB_HEIGHT.dp,
        containerRadius = 12.dp,
        itemRadius = 9.dp,
        fontSize = 12.5.sp,
    )
}

/**
 * Keeps the sheet top in place while the keyboard rises over the tab area. After that, the content moves up with
 * the keyboard. The M3 sheet moves up by the full keyboard height, so the free space under the content gets smaller
 * by the same distance.
 *
 * @param keyboard Keyboard height above the system bars.
 * @param height Height of the tab area without the keyboard, in pixels.
 * @param extra Distance that the sheet bottom padding gets smaller while the keyboard is open. The free space gets
 *   larger by this distance.
 */
private fun Modifier.keepSheetTop(keyboard: WindowInsets, height: () -> Int, extra: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val keptHeight = maxOf(placeable.height, height() + extra.roundToPx() - keyboard.getBottom(this))
    layout(placeable.width, constraints.constrainHeight(keptHeight)) { placeable.place(0, 0) }
}

/** Label of the tab. */
private val TextSheetTab.label: String
    get() = when (this) {
        TextSheetTab.TEMPLATE -> "템플릿"
        TextSheetTab.FONT -> "글꼴"
        TextSheetTab.DECORATION -> "꾸미기"
    }

/**
 * Text field of the text sheet. The keyboard shows only when the user taps the field. The field sends the selected
 * characters with each change, so the decorations can change only them.
 *
 * @param draft Draft of the sheet.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the field.
 */
@Composable
private fun TextDraftField(draft: TextDraft, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    var field by remember { mutableStateOf(TextFieldValue(draft.text, TextRange(draft.text.length))) }
    BasicTextField(
        value = field,
        onValueChange = { value ->
            field = value
            onIntent(EditorIntent.ChangeDraftText(value.text, value.selection.min, value.selection.max))
        },
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MagicColors.Background),
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
                if (field.text.isEmpty()) {
                    Text(
                        text = if (draft.isCaption) "자막 문구를 입력하세요" else "글자를 입력하세요",
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

// =====================================================================
// Template and font tabs
// =====================================================================

/**
 * Template tab. Each card plays the animation of its style in a loop.
 *
 * @param draft Text that the user types.
 * @param text Text in the cards.
 * @param onIntent Called with each user action.
 */
@Composable
private fun TextTemplatePage(draft: TextDraft, text: String, onIntent: (EditorIntent) -> Unit) {
    val clock = rememberTextPreviewClock(running = true)
    FixedGrid(items = TextStyleType.entries, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { style ->
        TemplateCard(style = style, draft = draft, text = text, clock = clock, onIntent = onIntent)
    }
}

/**
 * One row of template cards above the keyboard. The user can change the template while the user types.
 *
 * @param draft Text that the user types.
 * @param text Text in the cards.
 * @param onIntent Called with each user action.
 */
@Composable
private fun TemplateStrip(draft: TextDraft, text: String, onIntent: (EditorIntent) -> Unit) {
    val clock = rememberTextPreviewClock(running = true)
    val styles = TextStyleType.entries
    StripRow(state = rememberStripState(selectedIndex = styles.indexOf(draft.style))) {
        items(styles) { style ->
            TemplateCard(
                style = style,
                draft = draft,
                text = text,
                clock = clock,
                onIntent = onIntent,
                modifier = Modifier.width(StripCardWidth),
            )
        }
    }
}

/**
 * One row of font cards above the keyboard. Each card shows the text in the style of the draft with that font.
 * A font that the app must download shows a download mark, and a progress mark while it downloads.
 *
 * @param draft Text that the user types.
 * @param text Text in the cards.
 * @param downloading Font that downloads now, or null.
 * @param onIntent Called with each user action.
 */
@Composable
private fun FontStrip(draft: TextDraft, text: String, downloading: TextFontType?, onIntent: (EditorIntent) -> Unit) {
    // 글꼴을 고르지 않았으면 스타일의 글꼴이 선택된 것으로 보인다.
    val selectedFont = draft.font ?: draft.style.look().font
    val fonts = TextFontType.entries
    StripRow(state = rememberStripState(selectedIndex = fonts.indexOf(selectedFont))) {
        items(fonts) { font ->
            TextSheetCard(
                label = font.displayName,
                selected = selectedFont == font,
                onClick = { onIntent(EditorIntent.ChangeDraftFont(font)) },
                modifier = Modifier.width(StripCardWidth),
            ) {
                val look = draft.style.look(STYLE_SAMPLE_SCALE, font)
                StyleSample(text = text, look = look, frame = TextFrame.Static, scale = STYLE_SAMPLE_SCALE)
                if (font.isNew) NewBadge(modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
                val available = font.isAvailable()
                if (downloading == font || !available) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MagicColors.BadgeScrim),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (downloading == font) {
                            MagicSpinner(color = MagicColors.Accent, trackColor = MagicColors.SurfaceHighest, size = 14.dp, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Download, contentDescription = "내려받기", tint = MagicColors.OnSurface, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row of cards that scrolls to the side. It goes to the sheet edges.
 *
 * @param state Scroll state of the row.
 * @param content Cards of the row.
 */
@Composable
private fun StripRow(state: LazyListState, content: LazyListScope.() -> Unit) {
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().bleedHorizontal(SheetPadding),
        contentPadding = PaddingValues(horizontal = SheetPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * Scroll state of a strip. The strip starts with the selected card and the card before it.
 *
 * @param selectedIndex Index of the selected card, or -1.
 */
@Composable
private fun rememberStripState(selectedIndex: Int): LazyListState =
    rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0))

/**
 * Card of one template. It plays the animation of the style in a loop.
 *
 * @param style Style of the card.
 * @param draft Text that the user types.
 * @param text Text in the card.
 * @param clock Clock of the card animations.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the card.
 */
@Composable
private fun TemplateCard(
    style: TextStyleType,
    draft: TextDraft,
    text: String,
    clock: LongState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextSheetCard(
        label = style.displayName,
        selected = draft.style == style,
        onClick = { onIntent(EditorIntent.ChangeDraftStyle(style)) },
        modifier = modifier,
    ) {
        val look = style.look(STYLE_SAMPLE_SCALE, draft.font)
        StyleSample(text = text, look = look, frame = look.animation.previewFrameAt(clock.longValue), scale = STYLE_SAMPLE_SCALE)
    }
}

/**
 * Font tab: one row for each font, with the name in that font (D02). The text is not in the rows; the stage shows
 * it. A font that the app must download shows a download mark, and a progress mark while it downloads.
 *
 * @param draft Text that the user types.
 * @param downloading Font that downloads now, or null.
 * @param onIntent Called with each user action.
 */
@Composable
private fun TextFontPage(draft: TextDraft, downloading: TextFontType?, onIntent: (EditorIntent) -> Unit) {
    // 글꼴을 고르지 않았으면 스타일의 글꼴이 선택된 것으로 보인다.
    val selectedFont = draft.font ?: draft.style.look().font
    Column(modifier = Modifier.padding(horizontal = 0.dp)) {
        TextFontType.entries.forEach { font ->
            val selected = selectedFont == font
            val available = font.isAvailable()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .pressClickable(pressedScale = 0.99f) { onIntent(EditorIntent.ChangeDraftFont(font)) }
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) MagicColors.SurfaceHigh else Color.Transparent)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = font.displayName,
                    modifier = Modifier.weight(1f),
                    fontFamily = font.fontFamily(),
                    fontSize = when (font) {
                        TextFontType.DONGLE -> 28.sp
                        TextFontType.NANUM_PEN -> 24.sp
                        else -> 18.sp
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (font.isNew) NewBadge()
                Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    when {
                        downloading == font -> MagicSpinner(color = MagicColors.Accent, trackColor = MagicColors.SurfaceHighest)
                        selected -> Icon(Icons.Rounded.Check, contentDescription = "선택됨", tint = MagicColors.Accent, modifier = Modifier.size(20.dp))
                        !available -> Icon(Icons.Rounded.Download, contentDescription = "내려받기", tint = MagicColors.OnSurface45, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** "NEW" mark of a new font. */
@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        text = "NEW",
        modifier = modifier
            .clip(CircleShape)
            .background(MagicColors.Accent.copy(alpha = 0.2f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MagicColors.AccentLabel,
        letterSpacing = 0.04.sp,
    )
}

/**
 * Card of the text sheet: a striped preview box with a name under it.
 *
 * @param label Name under the preview.
 * @param selected True to draw the selection border.
 * @param onClick Action on click.
 * @param modifier Modifier for the card.
 * @param preview Content of the preview box.
 */
@Composable
internal fun TextSheetCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier.pressClickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// =====================================================================
// Decoration tab
// =====================================================================

/**
 * Decoration tab (F01, F03): size steps, color, bold, outline, shadow, and background. The decorations change the
 * characters that the user selected in the text field, or all characters.
 *
 * @param draft Draft of the sheet.
 * @param recentColors Colors from the color picker, newest first.
 * @param onIntent Called with each user action.
 */
@Composable
private fun DecorationPage(draft: TextDraft, recentColors: List<Int>, onIntent: (EditorIntent) -> Unit) {
    val length = draft.text.length
    val backgrounds = draft.decorationValues { it.background }
    val colors = draft.decorationValues { it.color }
    // 배경이 있는 글자는 글자색을 자동으로 정하므로 색상을 고를 수 없다 (D05).
    val colorOff = length > 0 && backgrounds.any { it != null }
    var customColorOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    length == 0 -> "글자를 먼저 입력하세요"
                    draft.hasSelection -> "‘${draft.text.substring(draft.selectionStart, draft.selectionEnd)}’에만 적용해요"
                    else -> "글자 전체에 적용해요 · 입력창에서 글자를 고르면 그 부분만 꾸며요"
                },
                modifier = Modifier.weight(1f),
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MagicColors.OnSurface60,
            )
            if (draft.decorations.isNotEmpty()) {
                SmallPillButton(label = "꾸미기 초기화", onClick = { onIntent(EditorIntent.ResetDraftDecorations) })
            }
        }
        if (!draft.isCaption) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "크기", modifier = Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SizeSteps(draft = draft, boxSize = 44.dp, diameters = SizeStepDiameters, onIntent = onIntent)
                }
            }
        }
        Column(modifier = Modifier.alpha(if (colorOff) 0.4f else 1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (colorOff) "색상 · 배경이 있는 글자는 글자색이 자동이에요" else "색상",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MagicColors.OnSurface70,
            )
            SwatchRow {
                TextColorSwatches(
                    colors = colors,
                    recentColors = recentColors,
                    enabled = !colorOff,
                    customEnabled = !colorOff && length > 0,
                    onCustom = { customColorOpen = true },
                    onIntent = onIntent,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DecorationToggles(draft = draft, onIntent = onIntent, modifier = Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "배경", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
            SwatchRow {
                BackgroundSwatches(backgrounds = backgrounds, hasText = length > 0, onIntent = onIntent)
            }
        }
    }
    if (customColorOpen) {
        CustomColorDialog(
            initial = colors.singleOrNull() ?: 0xFFFFFF,
            onPick = { rgb ->
                customColorOpen = false
                onIntent(EditorIntent.PickCustomColor(rgb))
            },
            onDismiss = { customColorOpen = false },
        )
    }
}

/**
 * Decoration controls above the keyboard, in two rows that scroll to the side. The first row has the size steps,
 * bold, outline, and shadow. The second row has the text colors and the background colors. The user can select
 * characters in the text field and change only them, while the keyboard stays open.
 *
 * @param draft Draft of the sheet.
 * @param recentColors Colors from the color picker, newest first.
 * @param onIntent Called with each user action.
 */
@Composable
private fun DecorationStrip(draft: TextDraft, recentColors: List<Int>, onIntent: (EditorIntent) -> Unit) {
    val length = draft.text.length
    val backgrounds = draft.decorationValues { it.background }
    val colors = draft.decorationValues { it.color }
    // 배경이 있는 글자는 글자색을 자동으로 정하므로 색상을 고를 수 없다 (D05).
    val colorOff = length > 0 && backgrounds.any { it != null }
    var customColorOpen by remember { mutableStateOf(false) }

    Column {
        SwatchRow {
            if (!draft.isCaption) {
                Row { SizeSteps(draft = draft, boxSize = 26.dp, diameters = StripSizeStepDiameters, onIntent = onIntent) }
                StripDivider()
            }
            DecorationToggles(draft = draft, onIntent = onIntent, compact = true)
        }
        SwatchRow {
            Row(
                modifier = Modifier.alpha(if (colorOff) 0.4f else 1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StripLabel(text = "색")
                TextColorSwatches(
                    colors = colors,
                    recentColors = recentColors,
                    enabled = !colorOff,
                    customEnabled = !colorOff && length > 0,
                    onCustom = { customColorOpen = true },
                    onIntent = onIntent,
                )
            }
            StripDivider()
            StripLabel(text = "배경")
            BackgroundSwatches(backgrounds = backgrounds, hasText = length > 0, onIntent = onIntent)
        }
    }
    if (customColorOpen) {
        CustomColorDialog(
            initial = colors.singleOrNull() ?: 0xFFFFFF,
            onPick = { rgb ->
                customColorOpen = false
                onIntent(EditorIntent.PickCustomColor(rgb))
            },
            onDismiss = { customColorOpen = false },
        )
    }
}

/** Decoration values of the characters that the decorations change. */
private fun <T> TextDraft.decorationValues(read: (Decoration) -> T): Set<T> =
    decorations.valuesIn(text.length, decorationRange.first, decorationRange.last + 1, read)

/**
 * Five size step circles. The selected step is filled with the accent color.
 *
 * @param draft Draft of the sheet.
 * @param boxSize Touch size of each circle.
 * @param diameters Diameters of the five circles.
 * @param onIntent Called with each user action.
 */
@Composable
private fun SizeSteps(draft: TextDraft, boxSize: Dp, diameters: List<Dp>, onIntent: (EditorIntent) -> Unit) {
    (1..SIZE_STEP_COUNT).forEach { step ->
        val selected = draft.sizeStep == step
        Box(
            modifier = Modifier.size(boxSize).pressClickable { onIntent(EditorIntent.ChangeDraftSize(step)) },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(diameters[step - 1])
                    .clip(CircleShape)
                    .background(if (selected) MagicColors.Accent else Color.Transparent)
                    .border(2.dp, if (selected) MagicColors.Accent else MagicColors.OnSurface45, CircleShape),
            )
        }
    }
}

/**
 * Bold, outline, and shadow toggles.
 *
 * @param draft Draft of the sheet.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for each toggle.
 * @param compact True for the small toggles above the keyboard.
 */
@Composable
private fun DecorationToggles(draft: TextDraft, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier, compact: Boolean = false) {
    DecorationToggle(Icons.Rounded.FormatBold, "굵게", draft.decorationValues { it.bold }, modifier, compact) {
        onIntent(EditorIntent.DecorateDraft(DecorationChange.Bold(it)))
    }
    DecorationToggle(Icons.Rounded.BorderColor, "테두리", draft.decorationValues { it.outline }, modifier, compact) {
        onIntent(EditorIntent.DecorateDraft(DecorationChange.Outline(it)))
    }
    DecorationToggle(Icons.Rounded.Gradient, "그림자", draft.decorationValues { it.shadow }, modifier, compact) {
        onIntent(EditorIntent.DecorateDraft(DecorationChange.Shadow(it)))
    }
}

/**
 * Button of the color picker, then the recent colors and the palette for the text color.
 *
 * @param colors Text colors in the range.
 * @param recentColors Colors from the color picker, newest first.
 * @param enabled Set to false when the text color is automatic.
 * @param customEnabled Set to false to turn off the color picker button.
 * @param onCustom Opens the color picker.
 * @param onIntent Called with each user action.
 */
@Composable
private fun TextColorSwatches(
    colors: Set<Int?>,
    recentColors: List<Int>,
    enabled: Boolean,
    customEnabled: Boolean,
    onCustom: () -> Unit,
    onIntent: (EditorIntent) -> Unit,
) {
    CustomColorButton(enabled = customEnabled, onClick = onCustom)
    (recentColors.filter { it !in DecorationPalette } + DecorationPalette).forEach { rgb ->
        ColorSwatch(
            color = rgbColor(rgb),
            selected = colors.singleOrNull() == rgb,
            shape = CircleShape,
            enabled = enabled,
            onClick = { onIntent(EditorIntent.DecorateDraft(DecorationChange.Color(rgb))) },
        )
    }
}

/**
 * "No background" swatch, then the palette for the background.
 *
 * @param backgrounds Backgrounds in the range.
 * @param hasText True when the draft has text.
 * @param onIntent Called with each user action.
 */
@Composable
private fun BackgroundSwatches(backgrounds: Set<Int?>, hasText: Boolean, onIntent: (EditorIntent) -> Unit) {
    ColorSwatch(
        color = MagicColors.SurfaceHigh,
        selected = backgrounds.singleOrNull() == null && hasText,
        shape = RoundedCornerShape(10.dp),
        icon = Icons.Rounded.Block,
        onClick = { onIntent(EditorIntent.DecorateDraft(DecorationChange.Background(null))) },
    )
    DecorationPalette.forEach { rgb ->
        ColorSwatch(
            color = rgbColor(rgb),
            selected = backgrounds.singleOrNull() == rgb,
            shape = RoundedCornerShape(10.dp),
            onClick = { onIntent(EditorIntent.DecorateDraft(DecorationChange.Background(rgb))) },
        )
    }
}

/** Small label in front of a group in a strip, for example "색". */
@Composable
private fun StripLabel(text: String) {
    Text(text = text, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface60)
}

/** Thin line between two groups in a strip. */
@Composable
private fun StripDivider() {
    Box(modifier = Modifier.size(width = 1.dp, height = 20.dp).background(MagicColors.OnSurface.copy(alpha = 0.14f)))
}

/** Row of color swatches that scrolls to the side. It goes to the sheet edges. */
@Composable
private fun SwatchRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bleedHorizontal(SheetPadding)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SheetPadding, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/**
 * Color swatch. The selected swatch has an accent ring.
 *
 * @param color Color of the swatch.
 * @param selected True to show the ring.
 * @param shape Shape of the swatch.
 * @param enabled Set to false for the disabled state.
 * @param icon Optional icon on the swatch.
 * @param onClick Action on click.
 */
@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .pressClickable(enabled = enabled, onClick = onClick)
            .then(if (selected) Modifier.border(2.dp, MagicColors.Accent, shape).padding(4.dp) else Modifier)
            .clip(shape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, contentDescription = "배경 없음", tint = MagicColors.OnSurface60, modifier = Modifier.size(18.dp))
    }
}

/** Button that opens the color picker. It has a rainbow ring and a plus mark. */
@Composable
private fun CustomColorButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .pressClickable(enabled = enabled, onClick = onClick)
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(Color(0xFFFF9A9A), Color(0xFFFFE08A), Color(0xFF9AF0B0), Color(0xFF8AD0FF), Color(0xFFC49AFF), Color(0xFFFF9A9A)))),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(MagicColors.Surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, contentDescription = "직접 고르기", modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Toggle of one decoration. A range with different values shows "섞임", and a tap turns the whole range on.
 *
 * @param icon Icon of the toggle.
 * @param label Name of the decoration.
 * @param values Values in the range.
 * @param modifier Modifier for the toggle.
 * @param compact True for the small toggle above the keyboard. It has no icon.
 * @param onChange Called with the new value.
 */
@Composable
private fun DecorationToggle(
    icon: ImageVector,
    label: String,
    values: Set<Boolean>,
    modifier: Modifier,
    compact: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val mixed = values.size > 1
    val on = values == setOf(true)
    Row(
        modifier = modifier
            .height(if (compact) 34.dp else 44.dp)
            .pressClickable { onChange(mixed || !on) }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    on -> MagicColors.OnSurface
                    mixed -> MagicColors.Accent.copy(alpha = 0.2f)
                    else -> MagicColors.SurfaceHigh
                },
            )
            .then(if (compact) Modifier.padding(horizontal = 12.dp) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (on) MagicColors.Background else MagicColors.OnSurface
        if (!compact) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(19.dp))
        Text(
            text = if (mixed) "$label 섞임" else label,
            fontSize = if (compact) 12.5.sp else 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
        )
    }
}

/**
 * Small dark pill button of a sheet, for example "꾸미기 초기화".
 *
 * @param label Label of the button.
 * @param onClick Action on click.
 * @param enabled Set to false for the disabled state.
 */
@Composable
internal fun SmallPillButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .pressClickable(enabled = enabled, onClick = onClick)
            .clip(CircleShape)
            .background(MagicColors.SurfaceHighest)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MagicColors.OnSurface else MagicColors.OnSurface.copy(alpha = 0.3f),
        )
    }
}

/** Last text draft of the open text sheet. It is not a screen state, so it is not a snapshot state. */
private class LastTextDraft {
    var value: TextDraft? = null
}
