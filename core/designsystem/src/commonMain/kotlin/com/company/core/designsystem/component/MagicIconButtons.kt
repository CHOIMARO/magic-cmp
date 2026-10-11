package com.company.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview

/**
 * Background color for the default, pressed, and disabled states, with the press animation.
 *
 * @param enabled False for the disabled state.
 * @param pressed True while pressed.
 * @param default Color in the default state.
 * @param pressedColor Color while pressed.
 * @param disabled Color in the disabled state.
 */
@Composable
internal fun stateColor(enabled: Boolean, pressed: Boolean, default: Color, pressedColor: Color, disabled: Color): Color {
    val color by animateColorAsState(
        targetValue = when {
            !enabled -> disabled
            pressed -> pressedColor
            else -> default
        },
        animationSpec = tween(PRESS_MILLIS),
    )
    return color
}

// =====================================================================
// Icon button
// =====================================================================

/** Kind of an icon button. */
enum class MagicIconButtonStyle {
    /** No background, for example "뒤로" and "되돌리기". */
    PLAIN,

    /** Dark round background, for example "닫기". */
    SURFACE,
}

/**
 * Round icon button of the Magic Buttons spec.
 *
 * @param icon Icon to show.
 * @param contentDescription Description for accessibility.
 * @param onClick Action on click.
 * @param modifier Modifier for the button.
 * @param style Kind of the button.
 * @param enabled Set to false for the disabled state.
 * @param size Size of the touch circle.
 * @param iconSize Size of the icon.
 */
@Composable
fun MagicIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MagicIconButtonStyle = MagicIconButtonStyle.PLAIN,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val surface = style == MagicIconButtonStyle.SURFACE
    val container = stateColor(
        enabled = enabled,
        pressed = pressed,
        default = if (surface) MagicColors.Surface else Color.Transparent,
        pressedColor = if (surface) MagicColors.SurfaceHighest else MagicColors.OnSurface.copy(alpha = 0.12f),
        disabled = if (surface) MagicColors.SurfaceDisabled else Color.Transparent,
    )
    Box(
        modifier = modifier
            .size(size)
            .pressClickable(enabled = enabled, pressedScale = PRESSED_SCALE_ICON, interactionSource = interactionSource, onClick = onClick)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MagicColors.OnSurface else MagicColors.OnSurface.copy(alpha = 0.25f),
            modifier = Modifier.size(iconSize),
        )
    }
}

// =====================================================================
// Editor tool
// =====================================================================

/** Kind of an editor tool. */
enum class MagicToolStyle {
    /** Normal tool on a dark circle. */
    DEFAULT,

    /** Destructive tool, for example "삭제". */
    DANGER,

    /** Confirm tool on a light circle, for example "완료". */
    CONFIRM,
}

/**
 * Editor tool of the Magic Buttons spec: a 46 dp circle with a label. The touch area is 70 dp high.
 *
 * @param icon Icon in the circle.
 * @param label Label under the circle.
 * @param onClick Action on click.
 * @param modifier Modifier for the tool. Give it a width.
 * @param style Kind of the tool.
 * @param enabled Set to false for the disabled state.
 */
@Composable
fun MagicToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MagicToolStyle = MagicToolStyle.DEFAULT,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val circle = stateColor(
        enabled = enabled,
        pressed = pressed,
        default = if (style == MagicToolStyle.CONFIRM) MagicColors.OnSurface else MagicColors.Surface,
        pressedColor = if (style == MagicToolStyle.CONFIRM) MagicColors.LightPressed else MagicColors.SurfaceHighest,
        disabled = MagicColors.SurfaceDisabled,
    )
    val disabledColor = MagicColors.OnSurface.copy(alpha = 0.25f)
    val iconColor = when {
        !enabled -> disabledColor
        style == MagicToolStyle.CONFIRM -> MagicColors.Background
        style == MagicToolStyle.DANGER -> MagicColors.DangerIcon
        else -> MagicColors.OnSurface
    }
    val labelColor = when {
        !enabled -> disabledColor
        style == MagicToolStyle.DANGER -> MagicColors.Danger
        else -> MagicColors.OnSurface80
    }
    Column(
        modifier = modifier
            .height(70.dp)
            .pressClickable(enabled = enabled, pressedScale = PRESSED_SCALE_ICON, interactionSource = interactionSource, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
        )
    }
}

// =====================================================================
// Chip and bottom tab
// =====================================================================

/**
 * Selectable chip of the Magic Buttons spec, for example a template kind filter.
 *
 * @param label Label of the chip.
 * @param selected True when the chip is selected.
 * @param onClick Action on click.
 * @param modifier Modifier for the chip.
 * @param enabled Set to false for the disabled state.
 */
@Composable
fun MagicChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SelectablePill(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        height = 40.dp,
        horizontalPadding = 18.dp,
        fontSize = 14.sp,
        pressedScale = PRESSED_SCALE,
    )
}

/**
 * Bottom tab of the Magic Buttons spec: a pill with a label and no icon. A tab is always enabled.
 *
 * @param label Label of the tab.
 * @param selected True when the tab is open.
 * @param onClick Action on click.
 * @param modifier Modifier for the tab.
 */
@Composable
fun MagicTabPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectablePill(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = true,
        height = 48.dp,
        horizontalPadding = 20.dp,
        fontSize = 15.sp,
        pressedScale = PRESSED_SCALE_TAB,
    )
}

/** Light pill when selected, dark pill when not selected. The chip and the bottom tab share it. */
@Composable
private fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    height: Dp,
    horizontalPadding: Dp,
    fontSize: TextUnit,
    pressedScale: Float,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val container = if (selected) {
        stateColor(enabled, pressed, MagicColors.OnSurface, MagicColors.LightPressed, MagicColors.SwitchTrackOff)
    } else {
        stateColor(enabled, pressed, MagicColors.Surface, MagicColors.SurfaceHighest, MagicColors.SurfaceDisabled)
    }
    val content = when {
        selected && enabled -> MagicColors.Background
        selected -> MagicColors.OnSurface50
        enabled -> MagicColors.OnSurface.copy(alpha = 0.75f)
        else -> MagicColors.OnSurface.copy(alpha = 0.25f)
    }
    Box(
        modifier = modifier
            .height(height)
            .pressClickable(enabled = enabled, pressedScale = pressedScale, interactionSource = interactionSource, onClick = onClick)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = fontSize, fontWeight = FontWeight.Bold, color = content, maxLines = 1, softWrap = false)
    }
}

@Preview(name = "Icon buttons", widthDp = 360)
@Composable
private fun MagicIconButtonsPreview() {
    MagicPreview {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MagicIconButton(icon = Icons.Rounded.Close, contentDescription = "닫기", onClick = {}, style = MagicIconButtonStyle.SURFACE)
                MagicIconButton(icon = Icons.AutoMirrored.Rounded.Undo, contentDescription = "되돌리기", onClick = {})
                MagicIconButton(icon = Icons.AutoMirrored.Rounded.Undo, contentDescription = "되돌리기", onClick = {}, enabled = false)
                MagicIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "더 보기",
                    onClick = {},
                    style = MagicIconButtonStyle.SURFACE,
                    size = 40.dp,
                    iconSize = 20.dp,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MagicToolButton(icon = Icons.Outlined.Palette, label = "필터", onClick = {})
                MagicToolButton(icon = Icons.Outlined.ContentCut, label = "나누기", onClick = {})
                MagicToolButton(icon = Icons.Outlined.Delete, label = "삭제", onClick = {}, style = MagicToolStyle.DANGER)
                MagicToolButton(icon = Icons.Rounded.Check, label = "완료", onClick = {}, style = MagicToolStyle.CONFIRM)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MagicChip(label = "전체", selected = true, onClick = {})
                MagicChip(label = "레이아웃", selected = false, onClick = {})
                MagicChip(label = "15초", selected = false, onClick = {}, enabled = false)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MagicTabPill(label = "홈", selected = true, onClick = {})
                MagicTabPill(label = "템플릿", selected = false, onClick = {})
            }
        }
    }
}
