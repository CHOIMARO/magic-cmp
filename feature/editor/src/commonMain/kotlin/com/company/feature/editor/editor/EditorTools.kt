package com.company.feature.editor.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicToolButton
import com.company.core.designsystem.component.MagicToolStyle
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.canChangeSpeed
import com.company.core.domain.model.editor.dubbingOverflow
import kotlin.math.roundToInt

/** Width of one tool in a scrolling tool row. */
private val ScrollToolWidth = 70.dp

/** Smallest width of one tool in the selection tool row. */
private val SelectionToolMinWidth = 68.dp

/** Height of a tool row. It is the touch height of a tool. */
private val ToolRowHeight = 70.dp

/** Width of the "완료" tool. */
private val DoneToolWidth = 64.dp

/**
 * Tool row under the timeline.
 *
 * Without a selection it shows the eight editing tools in a row that scrolls to the side (D22). With a selected clip,
 * text, caption, dubbing, or sound effect, it shows the actions of the item and the "완료" tool.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun EditorTools(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val clip = state.selectedClip
    val text = state.selectedText
    val caption = state.selectedCaption
    val dubbing = state.selectedDubbing
    val effect = state.selectedSoundEffect
    val hasClipboard = state.clipboard != null
    Column(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 12.dp)) {
        when {
            clip != null -> Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Tool(Icons.Outlined.ContentCut, "나누기") { onIntent(EditorIntent.SplitClip) }
                if (clip.canChangeSpeed) {
                    Tool(Icons.Outlined.Speed, "속도") { onIntent(EditorIntent.OpenSheet(EditorSheet.SPEED)) }
                    Tool(Icons.AutoMirrored.Outlined.VolumeUp, "볼륨") { onIntent(EditorIntent.OpenVolume(VolumeTarget(VolumeKind.CLIP, clip.id))) }
                }
                Tool(Icons.Outlined.ContentCopy, "복제") { onIntent(EditorIntent.DuplicateClip) }
                Tool(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveClip) }
                DoneTool(onIntent)
            }
            text != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.Outlined.Edit, "고치기") { onIntent(EditorIntent.OpenTextEditor(text.id)) },
                ToolSpec(Icons.Outlined.Animation, "애니메이션") { onIntent(EditorIntent.OpenAnimationSheet) },
                ToolSpec(Icons.Outlined.CopyAll, "복사") { onIntent(EditorIntent.CopySelected) },
                if (hasClipboard) ToolSpec(Icons.Outlined.ContentPaste, "붙여넣기") { onIntent(EditorIntent.OpenPaste) } else null,
                ToolSpec(Icons.Outlined.ContentCopy, "복제") { onIntent(EditorIntent.DuplicateText) },
                ToolSpec(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveText) },
            ))
            caption != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.Outlined.Edit, "고치기") { onIntent(EditorIntent.OpenCaptionEditor(caption.id)) },
                ToolSpec(Icons.Outlined.CopyAll, "복사") { onIntent(EditorIntent.CopySelected) },
                ToolSpec(Icons.Outlined.RecordVoiceOver, "더빙") { onIntent(EditorIntent.OpenDubbing(captionId = caption.id)) },
                ToolSpec(Icons.Outlined.ContentCut, "나누기") { onIntent(EditorIntent.SplitCaption) },
                ToolSpec(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveTrackItem) },
            ))
            dubbing != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.AutoMirrored.Outlined.VolumeUp, "볼륨") { onIntent(EditorIntent.OpenVolume(VolumeTarget(VolumeKind.DUBBING, dubbing.id))) },
                ToolSpec(Icons.Outlined.Refresh, "다시 만들기") { onIntent(EditorIntent.OpenDubbing(editId = dubbing.id)) },
                ToolSpec(Icons.Outlined.ContentCopy, "복제") { onIntent(EditorIntent.DuplicateTrackItem) },
                ToolSpec(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveTrackItem) },
            ))
            effect != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.AutoMirrored.Outlined.VolumeUp, "볼륨") { onIntent(EditorIntent.OpenVolume(VolumeTarget(VolumeKind.SOUND_EFFECT, effect.id))) },
                ToolSpec(Icons.Outlined.ContentCopy, "복제") { onIntent(EditorIntent.DuplicateTrackItem) },
                ToolSpec(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveTrackItem) },
            ))
            else -> ScrollingTools {
                if (hasClipboard) ScrollTool(Icons.Outlined.ContentPaste, "붙여넣기", MagicToolStyle.HIGHLIGHT) { onIntent(EditorIntent.OpenPaste) }
                ScrollTool(Icons.Outlined.Title, "글자") { onIntent(EditorIntent.OpenSheet(EditorSheet.TEXT)) }
                ScrollTool(Icons.Outlined.Subtitles, "자막") { onIntent(EditorIntent.OpenSheet(EditorSheet.CAPTIONS)) }
                ScrollTool(Icons.Outlined.MusicNote, "음악") { onIntent(EditorIntent.OpenSheet(EditorSheet.MUSIC)) }
                ScrollTool(Icons.Outlined.GraphicEq, "효과음") { onIntent(EditorIntent.OpenSheet(EditorSheet.SOUND_EFFECT)) }
                ScrollTool(Icons.Outlined.RecordVoiceOver, "더빙") { onIntent(EditorIntent.OpenSheet(EditorSheet.DUBBING)) }
                ScrollTool(Icons.Outlined.SwapHoriz, "전환") { onIntent(EditorIntent.OpenNearestTransition) }
                ScrollTool(Icons.Outlined.Palette, "필터") { onIntent(EditorIntent.OpenSheet(EditorSheet.FILTER)) }
                ScrollTool(Icons.Outlined.Dashboard, "레이아웃") { onIntent(EditorIntent.OpenSheet(EditorSheet.LAYOUT)) }
            }
        }
        val overflow = dubbing?.let { state.project.dubbingOverflow(it) } ?: 0.0
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 16.dp).padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = when {
                    clip != null -> "보라색 손잡이를 끌어 길이를 조절해요"
                    text != null -> "손잡이로 나오는 시간을 조절하고, 길게 눌러 옮겨요"
                    caption != null -> "손잡이로 시간을 맞추고, 두 번 누르면 문구를 고쳐요"
                    dubbing != null && overflow > OVERFLOW_TOLERANCE -> "자막 구간보다 ${(overflow * 10).roundToInt() / 10.0}초 길어요 ·"
                    dubbing != null || effect != null -> "선택한 막대를 끌어서 옮겨요"
                    state.project.clips.size <= 1 -> "오른쪽 + 를 눌러 영상·사진을 더 붙여요"
                    else -> "클립을 누르면 자르고 다듬을 수 있어요"
                },
                textAlign = TextAlign.Center,
                fontSize = 11.5.sp,
                color = MagicColors.OnSurface45,
            )
            if (dubbing != null && overflow > OVERFLOW_TOLERANCE) {
                Text(
                    text = "구간에 맞추기",
                    modifier = Modifier.pressClickable { onIntent(EditorIntent.FitDubbing) },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MagicColors.AccentLabel,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

/** A dubbing longer than its caption by more than this, in seconds, shows the fit link. */
private const val OVERFLOW_TOLERANCE = 0.05

/** Light "완료" tool at the end of the row. It clears the selection and the clipboard. */
@Composable
private fun DoneTool(onIntent: (EditorIntent) -> Unit) {
    MagicToolButton(
        icon = Icons.Rounded.Check,
        label = "완료",
        onClick = { onIntent(EditorIntent.ClearSelection) },
        modifier = Modifier.width(DoneToolWidth),
        style = MagicToolStyle.CONFIRM,
    )
}

@Composable
private fun RowScope.Tool(
    icon: ImageVector,
    label: String,
    style: MagicToolStyle = MagicToolStyle.DEFAULT,
    onClick: () -> Unit,
) {
    MagicToolButton(icon = icon, label = label, onClick = onClick, modifier = Modifier.weight(1f), style = style)
}

/**
 * One tool of the selection tool row.
 *
 * @property icon Icon in the circle.
 * @property label Label under the circle.
 * @property style Kind of the tool.
 * @property onClick Action on click.
 */
private class ToolSpec(
    val icon: ImageVector,
    val label: String,
    val style: MagicToolStyle = MagicToolStyle.DEFAULT,
    val onClick: () -> Unit,
)

/**
 * Tools of a selected text, caption, dubbing, or sound effect. They share the width, and they scroll to the side
 * when they do not fit. The "완료" tool stays at the end.
 */
@Composable
private fun SelectionTools(onIntent: (EditorIntent) -> Unit, tools: List<ToolSpec>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val toolWidth = maxOf(SelectionToolMinWidth, maxWidth / tools.size.coerceAtLeast(1))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.Top) {
                tools.forEach { tool ->
                    MagicToolButton(icon = tool.icon, label = tool.label, onClick = tool.onClick, modifier = Modifier.width(toolWidth), style = tool.style)
                }
            }
        }
        DoneTool(onIntent)
    }
}

/** Editing tools in a row that scrolls to the side. The edges fade when more tools are there. */
@Composable
private fun ScrollingTools(tools: @Composable RowScope.() -> Unit) {
    val scroll = rememberScrollState()
    // 높이를 도구 높이로 정한다. 정하지 않으면 페이드의 fillMaxHeight가 화면에 남은 높이를 모두 차지해서
    // 미리보기 칸의 높이가 0이 된다.
    Box(modifier = Modifier.fillMaxWidth().height(ToolRowHeight)) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scroll), verticalAlignment = Alignment.Top, content = tools)
        EdgeFade(scroll = scroll, start = true, modifier = Modifier.align(Alignment.CenterStart).width(36.dp))
        EdgeFade(scroll = scroll, start = false, modifier = Modifier.align(Alignment.CenterEnd).width(48.dp))
    }
}

@Composable
private fun ScrollTool(icon: ImageVector, label: String, style: MagicToolStyle = MagicToolStyle.DEFAULT, onClick: () -> Unit) {
    MagicToolButton(icon = icon, label = label, onClick = onClick, modifier = Modifier.width(ScrollToolWidth), style = style)
}

/** Dark fade at one edge of a scrolling row. It shows only when more content is past that edge. */
@Composable
private fun EdgeFade(scroll: ScrollState, start: Boolean, modifier: Modifier) {
    val visible = if (start) scroll.value > 2 else scroll.value < scroll.maxValue - 2
    val colors = listOf(MagicColors.Background, MagicColors.Background.copy(alpha = 0f))
    Box(
        modifier = modifier
            .fillMaxHeight()
            .alpha(if (visible) 1f else 0f)
            .background(Brush.horizontalGradient(if (start) colors else colors.reversed())),
    )
}
