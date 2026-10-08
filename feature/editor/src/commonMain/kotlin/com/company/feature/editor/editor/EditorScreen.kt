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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicBottomSheet
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.theme.MagicColors
import com.company.ui.editor.formatTime

/**
 * Editor screen. It only draws [state] and sends user actions to [onIntent].
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun EditorScreen(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            EditorTopBar(state = state, onIntent = onIntent)
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                EditorStage(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxHeight()
                        .heightIn(max = 470.dp)
                        .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true),
                )
            }
            PlaybackRow(state = state, onIntent = onIntent)
            EditorTimeline(state = state, onIntent = onIntent)
            EditorTools(hasSelection = state.selectedClip != null, onIntent = onIntent)
        }

        MagicBottomSheet(
            value = state.sheet,
            onDismiss = { onIntent(EditorIntent.CloseSheet) },
            scrimAlpha = 0.3f,
        ) { sheet ->
            EditorSheetContent(sheet = sheet, state = state, onIntent = onIntent)
        }
    }
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
                append(formatTime(state.time))
                withStyle(SpanStyle(color = MagicColors.OnSurface40)) { append(" / ${formatTime(state.totalDuration)}") }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
