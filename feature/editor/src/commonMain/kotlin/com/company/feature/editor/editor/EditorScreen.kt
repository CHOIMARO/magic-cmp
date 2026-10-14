package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicModalBottomSheet
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.feature.editor.preview.EditorPreviewData
import com.company.ui.editor.formatTime
import com.company.ui.editor.formatTimeTenths

/**
 * Editor screen. It only draws [state] and sends user actions to [onIntent].
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 * @param video Video surface of the preview player, or null to show placeholders on the stage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditorScreen(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    video: (@Composable (Modifier) -> Unit)? = null,
) {
    val stageFit = remember { StageFit() }
    Box(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            EditorTopBar(state = state, onIntent = onIntent)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned(stageFit::updateArea)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                EditorStage(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxHeight()
                        .heightIn(max = 400.dp)
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                        // 미리보기 시트가 열려 있으면 무대를 시트 위 빈 곳에 맞춰 줄여 그린다.
                        .onGloballyPositioned(stageFit::updateStage)
                        .graphicsLayer { stageFit.applyTo(this) },
                    video = video,
                )
            }
            PlaybackRow(state = state, onIntent = onIntent)
            EditorTimeline(state = state, onIntent = onIntent)
            EditorTools(state = state, onIntent = onIntent)
        }

        // 미리보기가 보이도록 메인 시트보다 옅은 scrim을 쓴다. 미리보기 시트는 무대에서 바로 확인하도록 scrim이 없다.
        val lastSheet = remember { LastSheet() }
        state.sheet?.let { lastSheet.value = it }
        val previewSheet = lastSheet.value in PreviewSheets
        MagicModalBottomSheet(
            target = state.sheet,
            onDismissRequest = { onIntent(EditorIntent.CloseSheet) },
            showDragHandle = true,
            scrimColor = if (previewSheet) Color.Transparent else Color.Black.copy(alpha = 0.3f),
            onCoveredHeightChange = { height -> stageFit.coveredHeight = if (lastSheet.value in PreviewSheets) height else 0 },
        ) { sheet, requestDismiss ->
            val keyboardOpen by rememberKeyboardOpen()
            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 16.dp,
                    bottom = if (keyboardOpen) SheetKeyboardBottomPadding else SheetBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                EditorSheetContent(sheet = sheet, state = state, onIntent = onIntent, onClose = requestDismiss)
            }
        }
        state.dialog?.let { dialog ->
            EditorConfirmDialog(
                title = dialog.title,
                message = dialog.message,
                confirmLabel = dialog.confirmLabel,
                onConfirm = { onIntent(EditorIntent.ConfirmDialog) },
                onDismiss = { onIntent(EditorIntent.DismissDialog) },
            )
        }
    }
}

/** Sheets that show their result on the stage. They have no scrim, and the stage gets smaller to stay above them. */
private val PreviewSheets = setOf(EditorSheet.TEXT, EditorSheet.ANIMATION)

/** Last open sheet. The sheet keeps its scrim during the close animation. */
private class LastSheet {
    var value: EditorSheet? = null
}

@Composable
private fun EditorTopBar(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagicIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "뒤로",
            onClick = { onIntent(EditorIntent.Back) },
            iconSize = 26.dp,
        )
        Text(
            text = state.project.name,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        MagicIconButton(
            icon = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = "되돌리기",
            onClick = { onIntent(EditorIntent.Undo) },
            enabled = state.canUndo,
        )
        MagicButton(
            text = "완성",
            onClick = { onIntent(EditorIntent.Export) },
            style = MagicButtonStyle.PRIMARY_SMALL,
        )
    }
}

@Composable
private fun PlaybackRow(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagicIconButton(
            icon = if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (state.playing) "일시정지" else "재생",
            onClick = { onIntent(EditorIntent.TogglePlay) },
            size = 40.dp,
            iconSize = 30.dp,
        )
        Text(
            text = buildAnnotatedString {
                // 타임라인을 끄는 동안에는 0.1초 단위로 보여 준다.
                append(if (state.scrubbing) formatTimeTenths(state.time) else formatTime(state.time))
                withStyle(SpanStyle(color = MagicColors.OnSurface40)) { append(" / ${formatTime(state.totalDuration)}") }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EditorScreenPreview(state: EditorState) {
    MagicPreview {
        EditorScreen(state = state, onIntent = {})
    }
}

@Preview(name = "편집기", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun EditorDefaultPreview() = EditorScreenPreview(
    EditorState(project = EditorPreviewData.project, canUndo = true, time = 1.0, musicTracks = EditorPreviewData.musicTracks),
)

@Preview(name = "편집기 - 사진 클립 선택", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun EditorPhotoSelectedPreview() = EditorScreenPreview(
    EditorState(project = EditorPreviewData.project, time = 5.2, selectedClipId = "c3", musicTracks = EditorPreviewData.musicTracks),
)

@Preview(name = "편집기 - 글자 선택", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun EditorTextSelectedPreview() = EditorScreenPreview(
    EditorState(project = EditorPreviewData.project, time = 1.0, selectedTextId = "t1", musicTracks = EditorPreviewData.musicTracks),
)
