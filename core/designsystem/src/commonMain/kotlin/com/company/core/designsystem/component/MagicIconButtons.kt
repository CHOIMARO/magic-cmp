package com.company.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors

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
private fun stateColor(enabled: Boolean, pressed: Boolean, default: Color, pressedColor: Color, disabled: Color): Color {
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

    /** Highlighted "매직" tool on an accent circle. */
    MAGIC,

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
        default = when (style) {
            MagicToolStyle.MAGIC -> MagicColors.Accent
            MagicToolStyle.CONFIRM -> MagicColors.OnSurface
            else -> MagicColors.Surface
        },
        pressedColor = when (style) {
            MagicToolStyle.MAGIC -> MagicColors.AccentPressed
            MagicToolStyle.CONFIRM -> MagicColors.LightPressed
            else -> MagicColors.SurfaceHighest
        },
        disabled = MagicColors.SurfaceDisabled,
    )
    val disabledColor = MagicColors.OnSurface.copy(alpha = 0.25f)
    val iconColor = when {
        !enabled -> disabledColor
        style == MagicToolStyle.MAGIC -> MagicColors.OnAccent
        style == MagicToolStyle.CONFIRM -> MagicColors.Background
        style == MagicToolStyle.DANGER -> MagicColors.DangerIcon
        else -> MagicColors.OnSurface
    }
    val labelColor = when {
        !enabled -> disabledColor
        style == MagicToolStyle.MAGIC -> MagicColors.AccentLabel
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
            fontWeight = if (style == MagicToolStyle.MAGIC && enabled) FontWeight.Bold else FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
        )
    }
}

// =====================================================================
// Create button
// =====================================================================

/**
 * Create button ("+") in the center of the bottom navigation.
 *
 * @param onClick Action on click.
 * @param modifier Modifier for the button.
 * @param enabled Set to false for the disabled state.
 */
@Composable
fun MagicCreateButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val container = stateColor(enabled, pressed, MagicColors.Accent, MagicColors.AccentPressed, MagicColors.SurfaceHigh)
    Box(
        modifier = modifier
            .size(width = 56.dp, height = 42.dp)
            .pressClickable(enabled = enabled, pressedScale = PRESSED_SCALE_ICON, interactionSource = interactionSource, onClick = onClick)
            .clip(RoundedCornerShape(15.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "만들기",
            tint = if (enabled) MagicColors.OnAccent else MagicColors.OnSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(28.dp),
        )
    }
}

// =====================================================================
// Chip
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
            .height(38.dp)
            .pressClickable(enabled = enabled, interactionSource = interactionSource, onClick = onClick)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}
