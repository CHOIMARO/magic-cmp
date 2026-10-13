package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.CopiedElement
import com.company.core.domain.model.editor.MAX_ANIMATION_MILLIS
import com.company.core.domain.model.editor.MIN_ANIMATION_MILLIS
import com.company.core.domain.model.editor.PasteTarget
import com.company.core.domain.model.editor.TextEntrance
import com.company.core.domain.model.editor.TextExit
import com.company.core.domain.model.editor.hasCustomAnimation
import com.company.core.domain.model.editor.pasteOptions
import com.company.ui.editor.DEFAULT_TITLE_EXIT_MILLIS
import com.company.ui.editor.TextAnimation
import com.company.ui.editor.displayName
import com.company.ui.editor.frameAt
import com.company.ui.editor.look
import kotlin.math.roundToInt

/** Time that a card shows the text before its exit, in milliseconds. */
private const val EXIT_HOLD_MILLIS = 900L

/** Time without text after an exit, in milliseconds. */
private const val EXIT_PAUSE_MILLIS = 400L

/** Shortest entrance length of a card loop, in milliseconds. A very short entrance is hard to see. */
private const val MIN_CARD_ENTRANCE_MILLIS = 300

// =====================================================================
// Animation sheet
// =====================================================================

/**
 * Animation sheet (F09): entrance and exit tabs, a card for each animation with the selected text in a loop, and the
 * length. A pick plays the animation once on the stage.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun AnimationSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val text = state.selectedText ?: return
    val styleAnimation = text.style.look().animation
    val entranceTab = state.animationTab == AnimationTab.ENTRANCE
    val currentEntrance = text.entrance ?: styleAnimation.entrance
    val currentExit = text.exit ?: TextExit.FADE
    val millis = if (entranceTab) text.entranceMillis ?: styleAnimation.entranceMillis else text.exitMillis ?: DEFAULT_TITLE_EXIT_MILLIS
    val none = if (entranceTab) currentEntrance == TextEntrance.NONE else currentExit == TextExit.NONE

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedControl(
            options = listOf(SegmentOption("등장"), SegmentOption("퇴장")),
            selectedIndex = state.animationTab.ordinal,
            onSelect = { onIntent(EditorIntent.SelectAnimationTab(AnimationTab.entries[it])) },
            modifier = Modifier.weight(1f),
            containerColor = MagicColors.Background,
            itemHeight = 38.dp,
        )
        Box(
            modifier = Modifier
                .height(46.dp)
                .pressClickable(enabled = text.hasCustomAnimation) { onIntent(EditorIntent.ResetAnimation) }
                .clip(CircleShape)
                .background(MagicColors.SurfaceHighest)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "기본값",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (text.hasCustomAnimation) MagicColors.OnSurface else MagicColors.OnSurface.copy(alpha = 0.3f),
            )
        }
    }
    val clock = rememberTextPreviewClock(running = true)
    // 카드에서는 글자 크기 단계를 기본으로 두고 작게 보여 준다.
    val look = text.style.look(scale = STYLE_SAMPLE_SCALE, font = text.font)
    val cardMillis = maxOf(MIN_CARD_ENTRANCE_MILLIS, millis)
    Column(modifier = Modifier.fillMaxWidth().height(196.dp).verticalScroll(rememberScrollState())) {
        if (entranceTab) {
            FixedGrid(items = TextEntrance.entries, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { entrance ->
                TextSheetCard(label = entrance.displayName, selected = currentEntrance == entrance, onClick = { onIntent(EditorIntent.PickEntrance(entrance)) }) {
                    val time = clock.longValue % (cardMillis + 1500)
                    val animation = TextAnimation(entrance, if (entrance == TextEntrance.NONE) 0 else cardMillis, DEFAULT_TITLE_EXIT_MILLIS)
                    val frame = animation.frameAt(time / 1000.0, (cardMillis + 1200 - time) / 1000.0)
                    StyleSample(text = text.text, look = look, frame = frame, scale = STYLE_SAMPLE_SCALE)
                }
            }
        } else {
            FixedGrid(items = TextExit.entries, columns = 4, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { exit ->
                TextSheetCard(label = exit.displayName, selected = currentExit == exit, onClick = { onIntent(EditorIntent.PickExit(exit)) }) {
                    val time = clock.longValue % (EXIT_HOLD_MILLIS + cardMillis + EXIT_PAUSE_MILLIS)
                    val animation = TextAnimation(TextEntrance.NONE, 0, if (exit == TextExit.NONE) 0 else cardMillis, exit)
                    val frame = animation.frameAt(1.0, (EXIT_HOLD_MILLIS + cardMillis - time) / 1000.0)
                    StyleSample(text = text.text, look = look, frame = frame, scale = STYLE_SAMPLE_SCALE)
                }
            }
        }
    }
    Row(
        modifier = Modifier.alpha(if (none) 0.4f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "길이", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
        val steps = (millis / 100f).roundToInt().coerceIn(MIN_ANIMATION_MILLIS / 100, MAX_ANIMATION_MILLIS / 100)
        SheetSlider(
            value = steps.toFloat(),
            range = (MIN_ANIMATION_MILLIS / 100f)..(MAX_ANIMATION_MILLIS / 100f),
            steps = (MAX_ANIMATION_MILLIS - MIN_ANIMATION_MILLIS) / 100 - 1,
            enabled = !none,
            onChange = { value ->
                val next = value.roundToInt() * 100
                if (next != steps * 100) onIntent(EditorIntent.ChangeAnimationLength(next))
            },
            modifier = Modifier.weight(1f),
        )
        Text(text = "${steps / 10.0}초", modifier = Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

// =====================================================================
// Paste sheet
// =====================================================================

/**
 * Paste sheet (F12, S11-7): the copied element, and the three targets.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun PasteSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val clipboard = state.clipboard ?: return
    val options = state.project.pasteOptions(clipboard, state.time) ?: return
    val element = clipboard.element
    val (icon, label) = when (element) {
        is CopiedElement.Text -> Icons.Rounded.Title to "글자 ‘${element.overlay.text}’"
        is CopiedElement.Caption -> Icons.Rounded.Subtitles to "자막 ‘${element.text}’"
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MagicColors.Background).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MagicColors.AccentSoft, modifier = Modifier.size(20.dp))
        Text(text = label, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = if (clipboard.coversClip) "구간 전체" else "${(clipboard.length * 10).roundToInt() / 10.0}초",
            fontSize = 12.sp,
            color = MagicColors.OnSurface50,
        )
    }
    val clipCount = state.project.clips.size
    val sourceInProject = state.project.clips.any { it.id == clipboard.sourceClipId }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PasteOption(
            icon = Icons.AutoMirrored.Rounded.ArrowForward,
            title = "다음 구간",
            subtitle = options.next.firstOrNull()?.let { "${it + 1}번째 구간 · 다시 누르면 그다음 구간에 이어 붙어요" } ?: "다음 구간이 없어요",
            enabled = options.next.isNotEmpty(),
            // 다음 구간이 없을 때 누르면 그 이유를 토스트로 알린다.
            clickableWhenDisabled = true,
        ) { onIntent(EditorIntent.Paste(PasteTarget.NEXT)) }
        PasteOption(
            icon = Icons.Rounded.MyLocation,
            title = "지금 구간",
            subtitle = "${options.currentIndex + 1}번째 구간 · 재생선이 있는 곳",
            enabled = options.current.isNotEmpty(),
        ) { onIntent(EditorIntent.Paste(PasteTarget.CURRENT)) }
        PasteOption(
            icon = Icons.Rounded.SelectAll,
            title = "모든 구간",
            subtitle = if (clipCount < 2) "구간이 하나뿐이에요" else "${if (sourceInProject) "원본 구간을 뺀 " else ""}${options.all.size}개 구간",
            enabled = options.all.isNotEmpty(),
        ) { onIntent(EditorIntent.Paste(PasteTarget.ALL)) }
    }
    Text(
        text = "복사한 것과 같은 위치·같은 크기·같은 꾸미기로 들어가요",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 12.sp,
        color = MagicColors.OnSurface45,
    )
}

/** One paste target. A target that cannot paste is faded. */
@Composable
private fun PasteOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    clickableWhenDisabled: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .pressClickable(enabled = enabled || clickableWhenDisabled, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(MagicColors.SurfaceHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MagicColors.AccentSoft, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, modifier = Modifier.padding(top = 2.dp), fontSize = 12.5.sp, lineHeight = 17.sp, color = MagicColors.OnSurface55)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MagicColors.OnSurface40, modifier = Modifier.size(20.dp))
    }
}
