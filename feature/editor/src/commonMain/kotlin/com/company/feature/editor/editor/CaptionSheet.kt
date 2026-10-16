package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonDefaults
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.scriptLines
import com.company.feature.editor.resources.*
import com.company.ui.editor.CaptionText
import com.company.ui.editor.displayName
import com.company.ui.editor.look
import com.company.ui.editor.splitWords
import org.jetbrains.compose.resources.stringResource

/** Words of the sample caption in the caption style options. */
private val CaptionSampleWords: List<String>
    @Composable get() = stringResource(Res.string.caption_sheet_sample).splitWords()

/** Warning color of a script with more lines than clips. */
private val WarningColor = com.company.core.designsystem.theme.oklch(0.84f, 0.12f, 70f)

/**
 * Caption sheet (F04): captions from speech or from a typed script, the caption style, and the caption track tools.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun CaptionSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val script = state.captionScript
    if (script != null) {
        CaptionScriptPage(script = script, clipCount = state.project.clips.size, onIntent = onIntent)
        return
    }
    val progress = state.captionProgress
    val running = progress != null
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CaptionSourceButton(
            icon = Icons.Rounded.Mic,
            title = if (progress != null) stringResource(Res.string.caption_from_speech_progress, progress) else stringResource(Res.string.caption_from_speech),
            note = stringResource(Res.string.caption_from_speech_note),
            progress = (progress ?: 0) / 100f,
            enabled = !running,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(EditorIntent.GenerateCaptions) },
        )
        CaptionSourceButton(
            icon = Icons.Outlined.EditNote,
            title = stringResource(Res.string.caption_from_script),
            note = stringResource(Res.string.caption_from_script_note),
            progress = 0f,
            enabled = !running,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(EditorIntent.OpenCaptionScript) },
        )
    }
    Text(
        text = stringResource(Res.string.caption_style_title),
        modifier = Modifier.padding(bottom = 0.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MagicColors.OnSurface70,
    )
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
                    .height(56.dp)
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
    if (state.project.captions.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MagicButton(
                text = stringResource(Res.string.caption_add_at_playhead),
                onClick = { onIntent(EditorIntent.AddCaptionAtPlayhead) },
                modifier = Modifier.weight(1f),
                style = MagicButtonStyle.SECONDARY,
                leadingIcon = Icons.Rounded.Add,
                height = 52.dp,
            )
            MagicButton(
                text = stringResource(Res.string.caption_clear_all),
                onClick = { onIntent(EditorIntent.RemoveCaptions) },
                style = MagicButtonStyle.DANGER,
                enabled = !running,
                height = 52.dp,
            )
        }
    }
}

/**
 * One of the two caption sources. The speech source fills from the left while the captions are made.
 *
 * @param icon Icon at the top.
 * @param title Name of the source.
 * @param note Short note under the name.
 * @param progress Part of the button that is filled, from 0 to 1.
 * @param enabled Set to false while the captions are made.
 * @param modifier Modifier for the button.
 * @param onClick Action on click.
 */
@Composable
private fun CaptionSourceButton(
    icon: ImageVector,
    title: String,
    note: String,
    progress: Float,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(88.dp)
            .pressClickable(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(MagicColors.SurfaceHigh),
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(MagicColors.Accent.copy(alpha = 0.22f)))
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, tint = MagicColors.AccentSoft, modifier = Modifier.size(22.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = note, fontSize = 12.sp, color = MagicColors.OnSurface55)
        }
    }
}

/**
 * Script field of the caption sheet. Each line is one caption, and the n-th line goes to the n-th clip.
 *
 * @param script Text of the field.
 * @param clipCount Number of clips.
 * @param onIntent Called with each user action.
 */
@Composable
private fun CaptionScriptPage(script: String, clipCount: Int, onIntent: (EditorIntent) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val lines = scriptLines(script)
    BasicTextField(
        value = script,
        onValueChange = { onIntent(EditorIntent.ChangeCaptionScript(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MagicColors.Background)
            .padding(14.dp)
            .focusRequester(focusRequester),
        textStyle = TextStyle(color = MagicColors.OnSurface, fontSize = 15.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, fontFamily = MagicTheme.fonts.sans),
        cursorBrush = SolidColor(MagicColors.Accent),
        decorationBox = { inner ->
            Box {
                if (script.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.caption_script_hint),
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = MagicColors.OnSurface40,
                    )
                }
                inner()
            }
        },
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        val over = lines.size > clipCount
        Text(
            text = when {
                lines.isEmpty() -> stringResource(Res.string.caption_script_empty)
                over -> stringResource(Res.string.caption_script_over, lines.size, lines.size - clipCount)
                else -> stringResource(Res.string.caption_script_lines, lines.size)
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (over) WarningColor else MagicColors.OnSurface70,
        )
        Text(text = stringResource(Res.string.caption_script_clips, clipCount), fontSize = 13.sp, color = MagicColors.OnSurface45)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MagicButton(
            text = stringResource(Res.string.back),
            onClick = { onIntent(EditorIntent.CloseCaptionScript) },
            style = MagicButtonStyle.SECONDARY,
            colors = MagicButtonDefaults.colors(MagicButtonStyle.SECONDARY),
            height = MagicButtonStyle.PRIMARY.height,
        )
        MagicButton(
            text = stringResource(Res.string.caption_script_insert),
            onClick = { onIntent(EditorIntent.InsertCaptionScript) },
            modifier = Modifier.weight(1f),
            enabled = lines.isNotEmpty(),
        )
    }
}
