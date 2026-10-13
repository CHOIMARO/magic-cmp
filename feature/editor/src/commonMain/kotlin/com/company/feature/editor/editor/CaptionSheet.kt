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
import com.company.ui.editor.CaptionText
import com.company.ui.editor.displayName
import com.company.ui.editor.look

/** Words of the sample caption in the caption style options. */
private val CaptionSampleWords = listOf("자막", "예시")

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
            title = if (progress != null) "만드는 중 $progress%" else "말소리로 만들기",
            note = "말하는 소리를 알아들어요",
            progress = (progress ?: 0) / 100f,
            enabled = !running,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(EditorIntent.GenerateCaptions) },
        )
        CaptionSourceButton(
            icon = Icons.Outlined.EditNote,
            title = "글로 만들기",
            note = "한 줄이 자막 하나가 돼요",
            progress = 0f,
            enabled = !running,
            modifier = Modifier.weight(1f),
            onClick = { onIntent(EditorIntent.OpenCaptionScript) },
        )
    }
    Text(
        text = "스타일 · 모든 자막에 바로 적용돼요",
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
                text = "재생선에 자막 추가",
                onClick = { onIntent(EditorIntent.AddCaptionAtPlayhead) },
                modifier = Modifier.weight(1f),
                style = MagicButtonStyle.SECONDARY,
                leadingIcon = Icons.Rounded.Add,
                height = 52.dp,
            )
            MagicButton(
                text = "모두 지우기",
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
                        text = "한 줄이 자막 하나가 돼요\n다른 곳에서 쓴 대본을 붙여 넣어도 돼요",
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
                lines.isEmpty() -> "줄바꿈으로 자막을 나눠요"
                over -> "${lines.size}줄 · 구간보다 ${lines.size - clipCount}줄 많아요"
                else -> "${lines.size}줄"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (over) WarningColor else MagicColors.OnSurface70,
        )
        Text(text = "영상 구간 ${clipCount}개", fontSize = 13.sp, color = MagicColors.OnSurface45)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MagicButton(
            text = "뒤로",
            onClick = { onIntent(EditorIntent.CloseCaptionScript) },
            style = MagicButtonStyle.SECONDARY,
            colors = MagicButtonDefaults.colors(MagicButtonStyle.SECONDARY),
            height = MagicButtonStyle.PRIMARY.height,
        )
        MagicButton(
            text = "자막 넣기",
            onClick = { onIntent(EditorIntent.InsertCaptionScript) },
            modifier = Modifier.weight(1f),
            enabled = lines.isNotEmpty(),
        )
    }
}
