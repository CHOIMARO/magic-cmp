package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/** Height of the scrolling part under the tabs. The stage above the sheet stays visible with the keyboard. */
private val TextSheetBodyHeight = 220.dp

/** Main colors of the color and background palettes: pastel colors and white and black. */
internal val DecorationPalette: List<Int> = listOf(
    0xFFFFFF, 0x1A1A1A, 0xFFB3BA, 0xFFD6A5, 0xFDFFB6, 0xCAFFBF, 0x9BF6FF, 0xA0C4FF, 0xBDB2FF, 0xFFC6FF, 0xF1E4D3, 0xC7CEEA,
)

/** Diameters of the five size step circles. */
private val SizeStepDiameters = listOf(14.dp, 19.dp, 24.dp, 30.dp, 36.dp)

/**
 * Text sheet. The text field, the save button, and the tabs stay at the top so that the keyboard does not cover them.
 * A caption draft shows only the decoration tab.
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
    // 입력한 글자를 카드에 그대로 보여 준다. 비어 있으면 예시 글자를 쓴다.
    val sampleText = draft.text.trim().ifEmpty { SAMPLE_TEXT }
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

/** Label of the tab. */
private val TextSheetTab.label: String
    get() = when (this) {
        TextSheetTab.TEMPLATE -> "템플릿"
        TextSheetTab.FONT -> "글꼴"
        TextSheetTab.DECORATION -> "꾸미기"
    }

/**
 * Text field of the text sheet. It gets the focus when the sheet opens, so the keyboard shows. It sends the selected
 * characters with each change, so the decorations can change only them.
 *
 * @param draft Draft of the sheet.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the field.
 */
@Composable
private fun TextDraftField(draft: TextDraft, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue(draft.text, TextRange(draft.text.length))) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    BasicTextField(
        value = field,
        onValueChange = { value ->
            field = value
            onIntent(EditorIntent.ChangeDraftText(value.text, value.selection.min, value.selection.max))
        },
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
        TextSheetCard(
            label = style.displayName,
            selected = draft.style == style,
            onClick = { onIntent(EditorIntent.ChangeDraftStyle(style)) },
        ) {
            val look = style.look(STYLE_SAMPLE_SCALE, draft.font)
            StyleSample(text = text, look = look, frame = look.animation.previewFrameAt(clock.longValue), scale = STYLE_SAMPLE_SCALE)
        }
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
private fun NewBadge() {
    Text(
        text = "NEW",
        modifier = Modifier
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
 * @param preview Content of the preview box.
 */
@Composable
internal fun TextSheetCard(label: String, selected: Boolean, onClick: () -> Unit, preview: @Composable BoxScope.() -> Unit) {
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
    val range = draft.decorationRange
    fun <T> values(read: (Decoration) -> T): Set<T> = draft.decorations.valuesIn(length, range.first, range.last + 1, read)
    val backgrounds = values { it.background }
    val colors = values { it.color }
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
                    (1..SIZE_STEP_COUNT).forEach { step ->
                        val selected = draft.sizeStep == step
                        Box(
                            modifier = Modifier.size(44.dp).pressClickable { onIntent(EditorIntent.ChangeDraftSize(step)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(SizeStepDiameters[step - 1])
                                    .clip(CircleShape)
                                    .background(if (selected) MagicColors.Accent else Color.Transparent)
                                    .border(2.dp, if (selected) MagicColors.Accent else MagicColors.OnSurface45, CircleShape),
                            )
                        }
                    }
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
                CustomColorButton(enabled = !colorOff && length > 0, onClick = { customColorOpen = true })
                (recentColors.filter { it !in DecorationPalette } + DecorationPalette).forEach { rgb ->
                    ColorSwatch(
                        color = rgbColor(rgb),
                        selected = colors.singleOrNull() == rgb,
                        shape = CircleShape,
                        enabled = !colorOff,
                        onClick = { onIntent(EditorIntent.DecorateDraft(DecorationChange.Color(rgb))) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DecorationToggle(Icons.Rounded.FormatBold, "굵게", values { it.bold }, Modifier.weight(1f)) { onIntent(EditorIntent.DecorateDraft(DecorationChange.Bold(it))) }
            DecorationToggle(Icons.Rounded.BorderColor, "테두리", values { it.outline }, Modifier.weight(1f)) { onIntent(EditorIntent.DecorateDraft(DecorationChange.Outline(it))) }
            DecorationToggle(Icons.Rounded.Gradient, "그림자", values { it.shadow }, Modifier.weight(1f)) { onIntent(EditorIntent.DecorateDraft(DecorationChange.Shadow(it))) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "배경", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
            SwatchRow {
                ColorSwatch(
                    color = MagicColors.SurfaceHigh,
                    selected = backgrounds.singleOrNull() == null && length > 0,
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
 * @param onChange Called with the new value.
 */
@Composable
private fun DecorationToggle(icon: ImageVector, label: String, values: Set<Boolean>, modifier: Modifier, onChange: (Boolean) -> Unit) {
    val mixed = values.size > 1
    val on = values == setOf(true)
    Row(
        modifier = modifier
            .height(44.dp)
            .pressClickable { onChange(mixed || !on) }
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    on -> MagicColors.OnSurface
                    mixed -> MagicColors.Accent.copy(alpha = 0.2f)
                    else -> MagicColors.SurfaceHigh
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (on) MagicColors.Background else MagicColors.OnSurface
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(19.dp))
        Text(text = if (mixed) "$label 섞임" else label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = content, maxLines = 1)
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
