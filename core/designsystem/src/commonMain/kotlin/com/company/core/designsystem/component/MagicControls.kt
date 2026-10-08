package com.company.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors

/**
 * On/off switch of the settings rows.
 *
 * The row that holds the switch handles the click. The switch only shows the state.
 * While the row is pressed, the knob grows from 22 dp to 26 dp and the track gets darker.
 *
 * @param checked True when the switch is on.
 * @param modifier Modifier for the switch.
 * @param pressed True while the row is pressed.
 * @param enabled Set to false for the disabled state.
 */
@Composable
fun MagicSwitch(checked: Boolean, modifier: Modifier = Modifier, pressed: Boolean = false, enabled: Boolean = true) {
    val isPressed = pressed && enabled
    val track by animateColorAsState(
        targetValue = when {
            !enabled && checked -> MagicColors.Accent.copy(alpha = 0.3f)
            !enabled -> MagicColors.SurfaceHigh
            isPressed && checked -> MagicColors.AccentPressed
            isPressed -> MagicColors.SwitchTrackOffPressed
            checked -> MagicColors.Accent
            else -> MagicColors.SwitchTrackOff
        },
        animationSpec = tween(200),
    )
    val knob by animateColorAsState(
        targetValue = when {
            !enabled && checked -> MagicColors.OnAccent.copy(alpha = 0.6f)
            !enabled -> MagicColors.OnSurface.copy(alpha = 0.3f)
            checked -> MagicColors.OnAccent
            else -> MagicColors.OnSurface
        },
        animationSpec = tween(200),
    )
    val knobSize by animateDpAsState(if (isPressed) 26.dp else 22.dp, tween(PRESS_MILLIS))
    val knobX by animateDpAsState(
        targetValue = when {
            isPressed && checked -> 21.dp
            isPressed -> 1.dp
            checked -> 23.dp
            else -> 3.dp
        },
        animationSpec = tween(200),
    )
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(track),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX)
                .size(knobSize)
                .clip(CircleShape)
                .background(knob)
        )
    }
}

/**
 * One option of a [SegmentedControl].
 *
 * @property label Main label.
 * @property subLabel Optional small label under [label].
 */
data class SegmentOption(val label: String, val subLabel: String? = null)

/**
 * Row of equal-width options in a rounded container. One option is selected.
 *
 * @param options Options to show.
 * @param selectedIndex Index of the selected option.
 * @param onSelect Called with the index of the tapped option.
 * @param modifier Modifier for the container.
 * @param containerColor Background of the container.
 * @param selectedColor Background of the selected option.
 * @param selectedContentColor Text color of the selected option.
 * @param contentColor Text color of the other options.
 * @param itemHeight Height of each option.
 * @param spacing Space between the options.
 * @param containerRadius Corner radius of the container.
 * @param itemRadius Corner radius of each option.
 * @param fontSize Size of [SegmentOption.label].
 */
@Composable
fun SegmentedControl(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MagicColors.Surface,
    selectedColor: Color = MagicColors.OnSurface,
    selectedContentColor: Color = MagicColors.Background,
    contentColor: Color = MagicColors.OnSurface.copy(alpha = 0.75f),
    itemHeight: Dp = 42.dp,
    spacing: Dp = 4.dp,
    containerRadius: Dp = 16.dp,
    itemRadius: Dp = 12.dp,
    fontSize: TextUnit = 14.sp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(containerRadius))
            .background(containerColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            // 칩과 같은 눌림 색: 선택된 칸은 한 단계 어둡게, 나머지는 회색 면을 보여준다.
            val background by animateColorAsState(
                targetValue = when {
                    selected && pressed -> MagicColors.LightPressed
                    selected -> selectedColor
                    pressed -> MagicColors.SurfaceHighest
                    else -> Color.Transparent
                },
                animationSpec = tween(PRESS_MILLIS),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(itemHeight)
                    .pressClickable(interactionSource = interactionSource) { onSelect(index) }
                    .clip(RoundedCornerShape(itemRadius))
                    .background(background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val color = if (selected) selectedContentColor else contentColor
                Text(text = option.label, color = color, fontSize = fontSize, fontWeight = FontWeight.Bold)
                if (option.subLabel != null) {
                    Text(
                        text = option.subLabel,
                        color = color.copy(alpha = color.alpha * 0.7f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * Small dark badge with a duration, for example "0:18".
 *
 * @param text Text of the badge.
 * @param modifier Modifier for the badge.
 * @param fontSize Size of the text.
 */
@Composable
fun DurationBadge(text: String, modifier: Modifier = Modifier, fontSize: TextUnit = 10.sp) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MagicColors.BadgeScrim)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}
