package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
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
import com.company.core.designsystem.component.MagicDialog
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicInputDialog
import com.company.core.designsystem.component.MagicModalBottomSheet
import com.company.core.designsystem.component.SystemBackHandler
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.domain.model.editor.MAX_WORK_NAME_LENGTH
import com.company.domain.model.editor.workNameOf
import com.company.feature.editor.preview.EditorPreviewData
import com.company.feature.editor.resources.*
import com.company.ui.editor.formatTime
import com.company.ui.editor.formatTimeTenths
import com.company.ui.text.asString
import org.jetbrains.compose.resources.stringResource

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
    // 시스템 뒤로가기와 iOS 가장자리 스와이프도 뒤로 버튼처럼 나가기 전에 묻는다. 열린 시트와 팝업은 뒤로가기를 먼저 받는다.
    SystemBackHandler(enabled = true) { onIntent(EditorIntent.Back) }
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
                // 전체 화면일 때는 무대를 전체 화면 쪽에만 그린다. 영상 화면은 한 번에 한 곳에만 둘 수 있다.
                if (state.fullScreen == null) {
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
            }
            PlaybackRow(state = state, onIntent = onIntent)
            EditorTimeline(state = state, onIntent = onIntent)
            EditorTools(state = state, onIntent = onIntent)
        }
        if (state.fullScreen != null) {
            FullScreenPreview(state = state, onIntent = onIntent, video = video)
        }

        // 미리보기가 보이도록 메인 시트보다 옅은 scrim을 쓴다. 미리보기 시트는 무대에서 바로 확인하도록 scrim이 없다.
        val lastSheet = remember { LastSheet() }
        state.sheet?.let { lastSheet.value = it }
        val previewSheet = lastSheet.value in PreviewSheets
        // 손잡이가 없는 시트는 끌기나 바깥 터치로 닫히지 않는다. 고치던 내용을 실수로 잃지 않도록 버튼으로만 닫는다.
        MagicModalBottomSheet(
            target = state.sheet,
            onDismissRequest = { onIntent(EditorIntent.CloseSheet) },
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
            MagicDialog(
                title = dialog.title.asString(),
                message = dialog.message.asString(),
                confirmLabel = dialog.confirmLabel.asString(),
                onConfirm = { onIntent(EditorIntent.ConfirmDialog) },
                cancelLabel = stringResource(Res.string.cancel),
                onCancel = { onIntent(EditorIntent.DismissDialog) },
                onDismissRequest = { onIntent(EditorIntent.DismissDialog) },
            )
        }
        if (state.renaming) {
            MagicInputDialog(
                title = stringResource(Res.string.rename_title),
                initialText = state.project.name,
                maxLength = MAX_WORK_NAME_LENGTH,
                confirmLabel = stringResource(Res.string.save),
                cancelLabel = stringResource(Res.string.cancel),
                onConfirm = { onIntent(EditorIntent.ConfirmRename(it)) },
                onDismissRequest = { onIntent(EditorIntent.DismissRename) },
                inputDescription = stringResource(Res.string.rename_input),
                canConfirm = { workNameOf(it) != null },
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
            contentDescription = stringResource(Res.string.back),
            onClick = { onIntent(EditorIntent.Back) },
            iconSize = 26.dp,
        )
        ProjectNameButton(name = state.project.name, onClick = { onIntent(EditorIntent.OpenRename) }, modifier = Modifier.weight(1f))
        MagicIconButton(
            icon = Icons.AutoMirrored.Rounded.Undo,
            contentDescription = stringResource(Res.string.editor_undo),
            onClick = { onIntent(EditorIntent.Undo) },
            enabled = state.canUndo,
        )
        // 요소가 하나도 없으면 볼 것도, 저장할 것도 없다. 글자만 있는 영상도 만들 수 있다(F16).
        val hasContent = !state.project.isEmpty
        MagicIconButton(
            icon = Icons.Rounded.Fullscreen,
            contentDescription = stringResource(Res.string.fullscreen_open),
            onClick = { onIntent(EditorIntent.OpenFullScreen) },
            enabled = hasContent,
            iconSize = 26.dp,
        )
        MagicButton(
            text = stringResource(Res.string.editor_finish),
            onClick = { onIntent(EditorIntent.Export) },
            style = MagicButtonStyle.PRIMARY_SMALL,
            enabled = hasContent,
        )
    }
}

/**
 * Name of the project with a pencil icon. A tap opens the rename popup (D54). A long name gets an ellipsis, and the
 * icon stays visible.
 */
@Composable
private fun ProjectNameButton(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val renameLabel = stringResource(Res.string.rename_title)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer { alpha = if (pressed) 0.6f else 1f }
            .clickable(interactionSource = interactionSource, indication = null, onClickLabel = renameLabel, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Rounded.Edit, contentDescription = renameLabel, tint = MagicColors.OnSurface50, modifier = Modifier.size(16.dp))
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
            contentDescription = stringResource(if (state.playing) Res.string.pause else Res.string.play),
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

@Preview(name = "편집기 - 전체 화면", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun EditorFullScreenPreview() = EditorScreenPreview(
    EditorState(project = EditorPreviewData.project, time = 2.4, musicTracks = EditorPreviewData.musicTracks, fullScreen = FullScreenState()),
)
