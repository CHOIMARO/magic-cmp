package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonLoading
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.SheetHeader
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.component.selectionRing
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.clipAt
import com.company.ui.editor.CaptionText
import com.company.ui.editor.LookText
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime
import com.company.ui.editor.look
import com.company.ui.editor.toColorFilter
import kotlin.math.roundToInt

private val SheetPadding = 18.dp
private val SpeedOptions = listOf(0.5, 1.0, 1.5, 2.0, 3.0)

/**
 * Content of the open editor sheet.
 *
 * @param sheet Sheet to show.
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun ColumnScope.EditorSheetContent(sheet: EditorSheet, state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val close = { onIntent(EditorIntent.CloseSheet) }
    val intro = when (sheet) {
        EditorSheet.VIBE -> "필터·음악·글자·자막 스타일이 한 번에 바뀌어요"
        EditorSheet.LAYOUT -> "화면을 나누거나 액자처럼 꾸며요"
        EditorSheet.CAPTIONS -> if (state.project.captions.isNotEmpty()) {
            "스타일을 누르면 모든 자막에 바로 적용돼요"
        } else {
            "말소리를 알아듣고 자막을 자동으로 넣어요"
        }
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SheetHeader(title = sheet.title(state), onClose = close)
        if (intro != null) Text(text = intro, fontSize = 13.sp, color = MagicColors.OnSurface60)
    }
    when (sheet) {
        EditorSheet.VIBE -> VibeSheet(state, onIntent)
        EditorSheet.LAYOUT -> LayoutSheet(state, onIntent)
        EditorSheet.TEXT -> TextSheet(state, onIntent)
        EditorSheet.MUSIC -> MusicSheet(state, onIntent)
        EditorSheet.FILTER -> FilterSheet(state, onIntent)
        EditorSheet.SPEED -> SpeedSheet(state, onIntent)
        EditorSheet.CAPTIONS -> CaptionSheet(state, onIntent)
    }
}

private fun EditorSheet.title(state: EditorState): String = when (this) {
    EditorSheet.TEXT -> if (state.textDraft?.id != null) "글자 고치기" else "글자 넣기"
    EditorSheet.MUSIC -> "음악"
    EditorSheet.FILTER -> "필터"
    EditorSheet.SPEED -> "속도"
    EditorSheet.CAPTIONS -> "자막"
    EditorSheet.VIBE -> "분위기 고르기"
    EditorSheet.LAYOUT -> "레이아웃"
}

/** Hue of the clip at the playhead. The filter and vibe previews use it. */
private fun EditorState.currentHue(): Int = project.clipAt(time)?.clip?.thumbnailHue ?: 35

// =====================================================================
// Vibe, layout, filter
// =====================================================================

@Composable
private fun VibeSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val hue = state.currentHue().toFloat()
    FixedGrid(items = Vibe.entries, columns = 3, horizontalSpacing = 10.dp, verticalSpacing = 10.dp) { vibe ->
        val selected = state.project.vibe == vibe
        val music = state.musicTracks.find { it.id == vibe.musicId }
        Column(
            modifier = Modifier.pressClickable { onIntent(EditorIntent.PickVibe(vibe)) },
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            StripeBox(
                hue = hue,
                lightness = 0.56f,
                shape = RoundedCornerShape(14.dp),
                colorFilter = remember(vibe) { vibe.filter.toColorFilter() },
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).selectionRing(selected, 14.dp),
            )
            Column(modifier = Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = vibe.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MagicColors.AccentLabel else MagicColors.OnSurface,
                )
                Text(
                    text = music?.let { "♪ ${it.title}" } ?: "효과 없음",
                    fontSize = 11.sp,
                    color = MagicColors.OnSurface50,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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

@Composable
private fun TextSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val draft = state.textDraft ?: return
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        value = draft.text,
        onValueChange = { onIntent(EditorIntent.ChangeDraftText(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MagicColors.Background)
            .focusRequester(focusRequester),
        singleLine = true,
        textStyle = TextStyle(
            color = MagicColors.OnSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MagicTheme.fonts.sans,
        ),
        cursorBrush = SolidColor(MagicColors.Accent),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                if (draft.text.isEmpty()) {
                    Text(text = "글자를 입력하세요", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface40)
                }
                innerTextField()
            }
        },
    )
    FixedGrid(items = TextStyleType.entries, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { style ->
        val selected = draft.style == style
        Column(
            modifier = Modifier.pressClickable { onIntent(EditorIntent.ChangeDraftStyle(style)) },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val shape = RoundedCornerShape(14.dp)
            StripeBox(
                hue = 250f,
                lightness = 0.42f,
                shape = shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .then(if (selected) Modifier.border(2.dp, MagicColors.Accent, shape) else Modifier),
            ) {
                LookText(
                    text = AnnotatedString("가나다"),
                    look = style.look(0.66f),
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
                maxLines = 1,
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        if (draft.id != null) {
            MagicButton(
                text = "삭제",
                onClick = { onIntent(EditorIntent.DeleteText) },
                style = MagicButtonStyle.DANGER,
            )
        }
        // 같은 줄의 "삭제" 버튼과 높이를 맞춘다.
        MagicButton(
            text = if (draft.id != null) "저장" else "넣기",
            onClick = { onIntent(EditorIntent.SaveText) },
            style = MagicButtonStyle.PRIMARY,
            height = MagicButtonStyle.DANGER.height,
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
                CaptionText(
                    spoken = "자막",
                    upcoming = "예시",
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
