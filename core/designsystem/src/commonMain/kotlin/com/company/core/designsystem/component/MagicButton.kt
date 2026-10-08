package com.company.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import kotlin.math.roundToInt

/** Press animation time of the Magic Buttons spec: the button reacts in 0.1 seconds. */
internal const val PRESS_MILLIS = 100

/** Scale of a pressed text button. */
const val PRESSED_SCALE = 0.97f

/** Scale of a pressed icon button or tool. */
const val PRESSED_SCALE_ICON = 0.94f

/**
 * Makes an element clickable without a ripple. The element shrinks a little while it is pressed.
 *
 * The design has no ripple effect. This modifier gives touch feedback in the same style.
 *
 * @param enabled Set to false to ignore clicks.
 * @param pressedScale Scale of the element while it is pressed.
 * @param interactionSource Optional source to read the pressed state outside the modifier.
 * @param onClick Action on click.
 */
fun Modifier.pressClickable(
    enabled: Boolean = true,
    pressedScale: Float = PRESSED_SCALE,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) pressedScale else 1f, tween(PRESS_MILLIS))
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

// =====================================================================
// Text buttons
// =====================================================================

/**
 * Kind of a text button in the Magic Buttons spec.
 *
 * Use one [PRIMARY] or [PRIMARY_SMALL] button on a screen. Use the other kinds for the other actions.
 *
 * @property height Height of the button.
 * @property horizontalPadding Space at the left and right of the label.
 * @property fontSize Size of the label.
 * @property fontWeight Weight of the label.
 */
enum class MagicButtonStyle(
    val height: Dp,
    val horizontalPadding: Dp,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
) {
    /** Main action, large. For example "영상 고르기" and "저장하기". */
    PRIMARY(56.dp, 26.dp, 16.sp, FontWeight.Bold),

    /** Main action, small. For example "완성" and "다음". */
    PRIMARY_SMALL(40.dp, 18.dp, 15.sp, FontWeight.Bold),

    /** Other action. For example "더 편집하기" and "취소". */
    SECONDARY(54.dp, 24.dp, 15.sp, FontWeight.SemiBold),

    /** Light action with a border. For example "프로필 수정". */
    OUTLINE(38.dp, 14.dp, 13.sp, FontWeight.SemiBold),

    /** Destructive action. For example "삭제", "자막 지우기", and "로그아웃". */
    DANGER(48.dp, 20.dp, 15.sp, FontWeight.SemiBold),

    /** Action without a background. For example "전체 보기" and "화질 바꾸기". */
    TEXT(40.dp, 12.dp, 13.sp, FontWeight.SemiBold),
}

/**
 * Colors of a text button in all states.
 *
 * @property container Background in the default state.
 * @property content Label and icon color in the default state.
 * @property border Border color. Transparent means no border.
 * @property pressedContainer Background while pressed. It is one step darker than [container].
 * @property pressedContent Label color while pressed.
 * @property disabledContainer Background when disabled.
 * @property disabledContent Label color when disabled.
 * @property disabledBorder Border color when disabled.
 * @property progressFill Fill of the progress loading state.
 * @property sweep Light of the magic loading state.
 * @property sparkle Sparkle and spinner color of the loading states.
 * @property spinnerTrack Track color of the spinner.
 */
@Immutable
data class MagicButtonColors(
    val container: Color,
    val content: Color,
    val border: Color = Color.Transparent,
    val pressedContainer: Color,
    val pressedContent: Color = content,
    val disabledContainer: Color,
    val disabledContent: Color,
    val disabledBorder: Color = Color.Transparent,
    val progressFill: Color,
    val sweep: Color,
    val sparkle: Color,
    val spinnerTrack: Color,
)

/** Default colors of the text buttons. */
object MagicButtonDefaults {
    private val LightLoading = MagicButtonColors(
        container = MagicColors.Surface,
        content = MagicColors.OnSurface,
        pressedContainer = MagicColors.SurfaceHighest,
        disabledContainer = MagicColors.SurfaceDisabled,
        disabledContent = MagicColors.OnSurface.copy(alpha = 0.3f),
        progressFill = MagicColors.Accent.copy(alpha = 0.22f),
        sweep = MagicColors.Accent.copy(alpha = 0.22f),
        sparkle = MagicColors.Accent,
        spinnerTrack = MagicColors.OnSurface.copy(alpha = 0.18f),
    )

    /**
     * Returns the colors of a button kind.
     *
     * @param style Kind of the button.
     */
    fun colors(style: MagicButtonStyle): MagicButtonColors = when (style) {
        MagicButtonStyle.PRIMARY, MagicButtonStyle.PRIMARY_SMALL -> MagicButtonColors(
            container = MagicColors.Accent,
            content = MagicColors.OnAccent,
            pressedContainer = MagicColors.AccentPressed,
            disabledContainer = MagicColors.SurfaceHigh,
            disabledContent = MagicColors.OnSurface.copy(alpha = 0.35f),
            progressFill = MagicColors.OnAccent.copy(alpha = 0.18f),
            sweep = Color.White.copy(alpha = 0.55f),
            sparkle = MagicColors.OnAccent,
            spinnerTrack = MagicColors.OnAccent.copy(alpha = 0.2f),
        )
        MagicButtonStyle.SECONDARY -> LightLoading
        MagicButtonStyle.OUTLINE -> LightLoading.copy(
            container = Color.Transparent,
            border = MagicColors.OnSurface.copy(alpha = 0.2f),
            pressedContainer = MagicColors.OnSurface.copy(alpha = 0.1f),
            disabledContainer = Color.Transparent,
            disabledBorder = MagicColors.OnSurface.copy(alpha = 0.08f),
        )
        MagicButtonStyle.DANGER -> LightLoading.copy(
            container = MagicColors.SurfaceHighest,
            content = MagicColors.Danger,
            pressedContainer = MagicColors.DangerPressed,
            disabledContainer = MagicColors.DangerDisabled,
        )
        MagicButtonStyle.TEXT -> LightLoading.copy(
            container = Color.Transparent,
            content = MagicColors.OnSurface.copy(alpha = 0.65f),
            pressedContainer = MagicColors.OnSurface.copy(alpha = 0.08f),
            pressedContent = MagicColors.OnSurface,
            disabledContainer = Color.Transparent,
            disabledContent = MagicColors.OnSurface.copy(alpha = 0.25f),
        )
    }

    /** Light button on the dark background, for example "홈으로". It uses the chip colors. */
    val InverseColors = MagicButtonColors(
        container = MagicColors.OnSurface,
        content = MagicColors.Background,
        pressedContainer = MagicColors.LightPressed,
        disabledContainer = MagicColors.SwitchTrackOff,
        disabledContent = MagicColors.OnSurface.copy(alpha = 0.5f),
        progressFill = MagicColors.Background.copy(alpha = 0.18f),
        sweep = Color.White.copy(alpha = 0.55f),
        sparkle = MagicColors.Background,
        spinnerTrack = MagicColors.Background.copy(alpha = 0.2f),
    )
}

/**
 * Loading state of a button. The button keeps its size and colors and shows "~하는 중".
 *
 * Do not show a loading state for work that ends in 0.4 seconds.
 */
sealed interface MagicButtonLoading {
    /** Label of the loading state, for example "저장하는 중". */
    val label: String

    /**
     * Spinner(label) : Use it when the time is not known.
     *
     * @property label Label next to the spinner.
     */
    data class Spinner(override val label: String) : MagicButtonLoading

    /**
     * Progress(label, fraction) : Use it when the remaining amount is known and the work takes more than 2 seconds.
     *
     * @property label Label before the percent, for example "저장하는 중".
     * @property fraction Progress from 0 to 1.
     */
    data class Progress(override val label: String, val fraction: Float) : MagicButtonLoading

    /**
     * Magic(label) : Use it only while magic decorates the video.
     *
     * @property label Label next to the sparkle, for example "magic 거는 중".
     */
    data class Magic(override val label: String) : MagicButtonLoading
}

/**
 * Pill button of the Magic Buttons spec with the default, pressed, disabled, and loading states.
 *
 * @param text Label of the button.
 * @param onClick Action on click. The button ignores clicks while it is disabled or loading.
 * @param modifier Modifier for the button.
 * @param style Kind of the button.
 * @param colors Colors of all states.
 * @param leadingIcon Optional icon before the label.
 * @param enabled Set to false for the disabled state. Tell the reason in [text].
 * @param loading Loading state, or null.
 * @param height Height of the button. Change it only to match another button in the same row.
 */
@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MagicButtonStyle = MagicButtonStyle.PRIMARY,
    colors: MagicButtonColors = MagicButtonDefaults.colors(style),
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: MagicButtonLoading? = null,
    height: Dp = style.height,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val clickable = enabled && loading == null
    val isPressed = pressed && clickable

    val container by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledContainer
            isPressed -> colors.pressedContainer
            else -> colors.container
        },
        animationSpec = tween(PRESS_MILLIS),
    )
    val content by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledContent
            isPressed -> colors.pressedContent
            else -> colors.content
        },
        animationSpec = tween(PRESS_MILLIS),
    )
    val border = if (enabled) colors.border else colors.disabledBorder

    Box(
        modifier = modifier
            .height(height)
            .pressClickable(enabled = clickable, interactionSource = interactionSource, onClick = onClick)
            .clip(CircleShape)
            .background(container)
            .then(if (border != Color.Transparent) Modifier.border(1.dp, border, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when (loading) {
            null -> ButtonLabel(text = text, icon = leadingIcon, color = content, style = style)
            is MagicButtonLoading.Spinner -> Row(
                modifier = Modifier.padding(horizontal = style.horizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MagicSpinner(color = colors.sparkle, trackColor = colors.spinnerTrack)
                ButtonText(text = loading.label, color = content, style = style)
            }
            is MagicButtonLoading.Progress -> {
                val fraction = loading.fraction.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(colors.progressFill)
                )
                ButtonText(text = "${loading.label} ${(fraction * 100).roundToInt()}%", color = content, style = style)
            }
            is MagicButtonLoading.Magic -> {
                MagicSweep(color = colors.sweep)
                Row(
                    modifier = Modifier.padding(horizontal = style.horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MagicTwinkle(color = colors.sparkle)
                    ButtonText(text = loading.label, color = content, style = style)
                }
            }
        }
    }
}

@Composable
private fun ButtonLabel(text: String, icon: ImageVector?, color: Color, style: MagicButtonStyle) {
    Row(
        modifier = Modifier.padding(horizontal = style.horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        ButtonText(text = text, color = color, style = style)
    }
}

@Composable
private fun ButtonText(text: String, color: Color, style: MagicButtonStyle) {
    Text(
        text = text,
        color = color,
        fontSize = style.fontSize,
        fontWeight = style.fontWeight,
        maxLines = 1,
        // 진행률 숫자가 바뀌어도 글자 폭이 흔들리지 않게 고정폭 숫자를 쓴다.
        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
    )
}
