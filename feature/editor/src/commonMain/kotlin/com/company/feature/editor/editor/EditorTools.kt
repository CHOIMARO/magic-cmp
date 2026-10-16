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
import com.company.feature.editor.resources.*
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/** Width of one tool in a scrolling tool row. */
private val ScrollToolWidth = 70.dp

/** Smallest width of one tool in the selection tool row. */
private val SelectionToolMinWidth = 68.dp

/** Height of a tool row. It is the touch height of a tool. */
private val ToolRowHeight = 70.dp

/** Width of the stringResource(Res.string.menu_done) tool. */
private val DoneToolWidth = 64.dp

/**
 * Tool row under the timeline.
 *
 * Without a selection it shows the eight editing tools in a row that scrolls to the side (D22). With a selected clip,
 * text, caption, dubbing, or sound effect, it shows the actions of the item and the stringResource(Res.string.menu_done) tool.
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
                Tool(Icons.Outlined.ContentCut, stringResource(Res.string.menu_split)) { onIntent(EditorIntent.SplitClip) }
                if (clip.canChangeSpeed) {
                    Tool(Icons.Outlined.Speed, stringResource(Res.string.menu_speed)) { onIntent(EditorIntent.OpenSheet(EditorSheet.SPEED)) }
                    Tool(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(Res.string.menu_volume)) { onIntent(EditorIntent.OpenVolume(VolumeTarget(VolumeKind.CLIP, clip.id))) }
                }
                Tool(Icons.Outlined.ContentCopy, stringResource(Res.string.duplicate)) { onIntent(EditorIntent.DuplicateClip) }
                Tool(Icons.Outlined.Delete, stringResource(Res.string.delete), MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveClip) }
                DoneTool(onIntent)
            }
            text != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.Outlined.Edit, stringResource(Res.string.menu_edit)) { onIntent(EditorIntent.OpenTextEditor(text.id)) },
                ToolSpec(Icons.Outlined.Animation, stringResource(Res.string.menu_animation)) { onIntent(EditorIntent.OpenAnimationSheet) },
                ToolSpec(Icons.Outlined.CopyAll, stringResource(Res.string.menu_copy)) { onIntent(EditorIntent.CopySelected) },
                if (hasClipboard) ToolSpec(Icons.Outlined.ContentPaste, stringResource(Res.string.menu_paste)) { onIntent(EditorIntent.OpenPaste) } else null,
                ToolSpec(Icons.Outlined.ContentCopy, stringResource(Res.string.duplicate)) { onIntent(EditorIntent.DuplicateText) },
                ToolSpec(Icons.Outlined.Delete, stringResource(Res.string.delete), MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveText) },
            ))
            caption != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.Outlined.Edit, stringResource(Res.string.menu_edit)) { onIntent(EditorIntent.OpenCaptionEditor(caption.id)) },
                ToolSpec(Icons.Outlined.CopyAll, stringResource(Res.string.menu_copy)) { onIntent(EditorIntent.CopySelected) },
                ToolSpec(Icons.Outlined.RecordVoiceOver, stringResource(Res.string.menu_dubbing)) { onIntent(EditorIntent.OpenDubbing(captionId = caption.id)) },
                ToolSpec(Icons.Outlined.ContentCut, stringResource(Res.string.menu_split)) { onIntent(EditorIntent.SplitCaption) },
                ToolSpec(Icons.Outlined.Delete, stringResource(Res.string.delete), MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveTrackItem) },
            ))
            dubbing != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(Res.string.menu_volume)) { onIntent(EditorIntent.OpenVolume(VolumeTarget(VolumeKind.DUBBING, dubbing.id))) },
                ToolSpec(Icons.Outlined.Refresh, stringResource(Res.string.remake)) { onIntent(EditorIntent.OpenDubbing(editId = dubbing.id)) },
                ToolSpec(Icons.Outlined.ContentCopy, stringResource(Res.string.duplicate)) { onIntent(EditorIntent.DuplicateTrackItem) },
                ToolSpec(Icons.Outlined.Delete, stringResource(Res.string.delete), MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveTrackItem) },
            ))
            effect != null -> SelectionTools(onIntent, listOfNotNull(
                ToolSpec(Icons.AutoMirrored.Outlined.VolumeUp, stringResource(Res.string.menu_volume)) { onIntent(EditorIntent.OpenVolume(VolumeTarget(VolumeKind.SOUND_EFFECT, effect.id))) },
                ToolSpec(Icons.Outlined.ContentCopy, stringResource(Res.string.duplicate)) { onIntent(EditorIntent.DuplicateTrackItem) },
                ToolSpec(Icons.Outlined.Delete, stringResource(Res.string.delete), MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveTrackItem) },
            ))
            else -> ScrollingTools {
                if (hasClipboard) ScrollTool(Icons.Outlined.ContentPaste, stringResource(Res.string.menu_paste), MagicToolStyle.HIGHLIGHT) { onIntent(EditorIntent.OpenPaste) }
                ScrollTool(Icons.Outlined.Title, stringResource(Res.string.menu_text)) { onIntent(EditorIntent.OpenSheet(EditorSheet.TEXT)) }
                ScrollTool(Icons.Outlined.Subtitles, stringResource(Res.string.menu_caption)) { onIntent(EditorIntent.OpenSheet(EditorSheet.CAPTIONS)) }
                ScrollTool(Icons.Outlined.MusicNote, stringResource(Res.string.menu_music)) { onIntent(EditorIntent.OpenSheet(EditorSheet.MUSIC)) }
                ScrollTool(Icons.Outlined.GraphicEq, stringResource(Res.string.menu_sound_effect)) { onIntent(EditorIntent.OpenSheet(EditorSheet.SOUND_EFFECT)) }
                ScrollTool(Icons.Outlined.RecordVoiceOver, stringResource(Res.string.menu_dubbing)) { onIntent(EditorIntent.OpenSheet(EditorSheet.DUBBING)) }
                ScrollTool(Icons.Outlined.SwapHoriz, stringResource(Res.string.menu_transition)) { onIntent(EditorIntent.OpenNearestTransition) }
                ScrollTool(Icons.Outlined.Palette, stringResource(Res.string.menu_filter)) { onIntent(EditorIntent.OpenSheet(EditorSheet.FILTER)) }
                ScrollTool(Icons.Outlined.Dashboard, stringResource(Res.string.menu_layout)) { onIntent(EditorIntent.OpenSheet(EditorSheet.LAYOUT)) }
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
                    clip != null -> stringResource(Res.string.tools_hint_clip)
                    text != null -> stringResource(Res.string.tools_hint_text)
                    caption != null -> stringResource(Res.string.tools_hint_caption)
                    dubbing != null && overflow > OVERFLOW_TOLERANCE -> stringResource(Res.string.tools_hint_dubbing_overflow, ((overflow * 10).roundToInt() / 10.0).toString())
                    dubbing != null || effect != null -> stringResource(Res.string.tools_hint_track)
                    state.project.clips.size <= 1 -> stringResource(Res.string.tools_hint_add_more)
                    else -> stringResource(Res.string.tools_hint_default)
                },
                textAlign = TextAlign.Center,
                fontSize = 11.5.sp,
                color = MagicColors.OnSurface45,
            )
            if (dubbing != null && overflow > OVERFLOW_TOLERANCE) {
                Text(
                    text = stringResource(Res.string.tools_fit_dubbing),
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

/** Light stringResource(Res.string.menu_done) tool at the end of the row. It clears the selection and the clipboard. */
@Composable
private fun DoneTool(onIntent: (EditorIntent) -> Unit) {
    MagicToolButton(
        icon = Icons.Rounded.Check,
        label = stringResource(Res.string.menu_done),
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
 * when they do not fit. The stringResource(Res.string.menu_done) tool stays at the end.
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
