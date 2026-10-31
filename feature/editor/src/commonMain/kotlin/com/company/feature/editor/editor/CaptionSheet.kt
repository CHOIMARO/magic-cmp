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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonDefaults
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.scriptLines
import com.company.domain.model.editor.scriptStart
import com.company.feature.editor.resources.*
import com.company.ui.editor.CaptionText
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTimeTenths
import com.company.ui.editor.look
import com.company.ui.editor.splitWords
import org.jetbrains.compose.resources.stringResource

/** Words of the sample caption in the caption style options. */
private val CaptionSampleWords: List<String>
    @Composable get() = stringResource(Res.string.caption_sheet_sample).splitWords()

/** Warning color of a script with more lines than clips. */
private val WarningColor = com.company.core.designsystem.theme.oklch(0.84f, 0.12f, 70f)

/** A playhead at this time or earlier, in seconds, counts as the start of the video. */
private const val PLAYHEAD_START = 1e-6

/**
 * Caption sheet (F04): captions from speech or from a typed script, the start of the new captions, the caption style,
 * and the caption height reset.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun CaptionSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val script = state.captionScript
    if (script != null) {
        // 재생선부터 만들 때는 재생선이 있는 구간부터 남은 구간만 셈다.
        val fromPlayhead = state.captionFromPlayhead && state.time > PLAYHEAD_START
        val firstClip = if (fromPlayhead) state.project.scriptStart(state.time).clipIndex else 0
        CaptionScriptPage(
            script = script,
            clipCount = state.project.clips.size - firstClip,
            fromPlayhead = fromPlayhead,
            onIntent = onIntent,
        )
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
    PlayheadCheckRow(
        checked = state.captionFromPlayhead,
        time = state.time,
        enabled = !running,
        onToggle = { onIntent(EditorIntent.ToggleCaptionFromPlayhead) },
    )
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.caption_style_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MagicColors.OnSurface70,
        )
        if (state.project.captionPosition != null) {
            SmallPillButton(label = stringResource(Res.string.caption_position_reset), onClick = { onIntent(EditorIntent.ResetCaptionPosition) })
        }
    }
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
        MagicButton(
            text = stringResource(Res.string.caption_clear_all),
            onClick = { onIntent(EditorIntent.RemoveCaptions) },
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.DANGER,
            enabled = !running,
            height = 52.dp,
        )
    }
}

/**
 * Check row that makes the new captions start at the playhead. Without the check, they start at 0:00.
 *
 * @param checked True when the captions start at the playhead.
 * @param time Playhead position. The row shows it at the right.
 * @param enabled Set to false while captions are made.
 * @param onToggle Action on click.
 */
@Composable
private fun PlayheadCheckRow(checked: Boolean, time: Double, enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressClickable(enabled = enabled, pressedScale = 0.98f, onClick = onToggle)
            .semantics {
                role = Role.Checkbox
                toggleableState = ToggleableState(checked)
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MagicColors.Background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val boxShape = RoundedCornerShape(7.dp)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(boxShape)
                .background(if (checked) MagicColors.Accent else Color.Transparent)
                .border(2.dp, if (checked) MagicColors.Accent else MagicColors.OnSurface40, boxShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MagicColors.OnAccent,
                modifier = Modifier.size(17.dp).alpha(if (checked) 1f else 0f),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(Res.string.caption_add_at_playhead), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(if (checked) Res.string.caption_from_playhead_on else Res.string.caption_from_playhead_off),
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                lineHeight = 1.4.em,
                color = MagicColors.OnSurface55,
            )
        }
        Text(
            text = formatTimeTenths(time),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (checked) MagicColors.AccentLabel else MagicColors.OnSurface40,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
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
 * @param clipCount Number of clips that can get a line.
 * @param fromPlayhead True when the lines start at the playhead. [clipCount] then counts from the clip at the playhead.
 * @param onIntent Called with each user action.
 */
@Composable
private fun CaptionScriptPage(script: String, clipCount: Int, fromPlayhead: Boolean, onIntent: (EditorIntent) -> Unit) {
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
        Text(
            text = stringResource(if (fromPlayhead) Res.string.caption_script_clips_after else Res.string.caption_script_clips, clipCount),
            fontSize = 13.sp,
            color = MagicColors.OnSurface45,
        )
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
