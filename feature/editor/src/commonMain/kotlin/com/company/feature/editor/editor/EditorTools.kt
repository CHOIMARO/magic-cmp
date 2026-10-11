package com.company.feature.editor.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicToolButton
import com.company.core.designsystem.component.MagicToolStyle
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.canChangeSpeed

/**
 * Tool row under the timeline.
 *
 * Without a selection it shows the five editing tools. With a selected clip it shows the clip actions.
 * With a selected text overlay it shows the text actions.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun EditorTools(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val clip = state.selectedClip
    val text = state.selectedText
    Column(modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, top = 2.dp, bottom = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            when {
                clip != null -> {
                    Tool(Icons.Outlined.ContentCut, "나누기") { onIntent(EditorIntent.SplitClip) }
                    if (clip.canChangeSpeed) {
                        Tool(Icons.Outlined.Speed, "속도") { onIntent(EditorIntent.OpenSheet(EditorSheet.SPEED)) }
                    }
                    Tool(Icons.Outlined.ContentCopy, "복제") { onIntent(EditorIntent.DuplicateClip) }
                    Tool(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveClip) }
                    DoneTool(onIntent)
                }
                text != null -> {
                    Tool(Icons.Outlined.Edit, "고치기") { onIntent(EditorIntent.OpenTextEditor(text.id)) }
                    Tool(Icons.Outlined.ContentCopy, "복제") { onIntent(EditorIntent.DuplicateText) }
                    Tool(Icons.Outlined.Delete, "삭제", MagicToolStyle.DANGER) { onIntent(EditorIntent.RemoveText) }
                    DoneTool(onIntent)
                }
                else -> {
                    Tool(Icons.Outlined.Dashboard, "레이아웃") { onIntent(EditorIntent.OpenSheet(EditorSheet.LAYOUT)) }
                    Tool(Icons.Outlined.Title, "글자") { onIntent(EditorIntent.OpenSheet(EditorSheet.TEXT)) }
                    Tool(Icons.Outlined.Subtitles, "자막") { onIntent(EditorIntent.OpenSheet(EditorSheet.CAPTIONS)) }
                    Tool(Icons.Outlined.MusicNote, "음악") { onIntent(EditorIntent.OpenSheet(EditorSheet.MUSIC)) }
                    Tool(Icons.Outlined.Palette, "필터") { onIntent(EditorIntent.OpenSheet(EditorSheet.FILTER)) }
                }
            }
        }
        Text(
            text = when {
                clip != null -> "보라색 손잡이를 끌어 길이를 조절해요"
                text != null -> "손잡이로 나오는 시간을 조절하고, 길게 눌러 옮겨요"
                state.project.clips.size <= 1 -> "오른쪽 + 를 눌러 영상·사진을 더 붙여요"
                else -> "클립을 누르면 자르고 다듬을 수 있어요"
            },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.5.sp,
            color = MagicColors.OnSurface45,
        )
    }
}

/** Light "완료" tool at the end of the row. It clears the selection. */
@Composable
private fun DoneTool(onIntent: (EditorIntent) -> Unit) {
    MagicToolButton(
        icon = Icons.Rounded.Check,
        label = "완료",
        onClick = { onIntent(EditorIntent.ClearSelection) },
        modifier = Modifier.width(64.dp),
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
