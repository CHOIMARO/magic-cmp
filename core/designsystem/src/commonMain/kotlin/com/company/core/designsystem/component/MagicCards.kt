package com.company.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview

// =====================================================================
// Action card
// =====================================================================

/**
 * Colors of an [MagicActionCard] in the default and pressed states.
 *
 * @property container Background in the default state.
 * @property pressedContainer Background while pressed.
 * @property content Title and arrow color.
 */
enum class MagicActionCardStyle(val container: Color, val pressedContainer: Color, val content: Color) {
    /** Main start action, for example "새 영상 만들기". */
    PRIMARY(MagicColors.Accent, MagicColors.AccentPressed, MagicColors.OnAccent),

    /** Other start action, for example "촬영". */
    SECONDARY(MagicColors.Lime, MagicColors.LimePressed, MagicColors.OnLime),
}

/**
 * Large action card of the Magic Buttons spec. It is 188 dp high and shows an icon in a dark circle,
 * an arrow, and a large title.
 *
 * @param title Title of the card. Use "\n" to break the line.
 * @param icon Icon in the dark circle.
 * @param onClick Action on click.
 * @param modifier Modifier for the card. Give it a width.
 * @param style Colors of the card.
 * @param enabled Set to false for the disabled state.
 */
@Composable
fun MagicActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MagicActionCardStyle = MagicActionCardStyle.PRIMARY,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val container = stateColor(enabled, pressed, style.container, style.pressedContainer, MagicColors.Surface)
    val disabledContent = MagicColors.OnSurface.copy(alpha = 0.3f)
    val content = if (enabled) style.content else disabledContent
    Column(
        modifier = modifier
            .height(188.dp)
            .pressClickable(enabled = enabled, interactionSource = interactionSource, onClick = onClick)
            .clip(RoundedCornerShape(26.dp))
            .background(container)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (enabled) MagicColors.Background else MagicColors.SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MagicColors.OnSurface else disabledContent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Icon(Icons.Rounded.NorthEast, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
        }
        Text(
            text = title,
            color = content,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 1.15.em,
            letterSpacing = (-0.04).em,
            softWrap = false,
        )
    }
}

// =====================================================================
// Video card
// =====================================================================

/** Black fade at the top and the bottom of a [MagicVideoCard]. It keeps the white text readable. */
private val VideoCardScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.35f),
    0.3f to Color.Transparent,
    0.48f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.62f),
)

/** [VideoCardScrim] while the card is pressed. It is one step darker. */
private val VideoCardPressedScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.5f),
    0.3f to Color.Black.copy(alpha = 0.18f),
    0.48f to Color.Black.copy(alpha = 0.18f),
    1f to Color.Black.copy(alpha = 0.72f),
)

/** Removes all color. The disabled video card uses it. */
private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Video card of the Magic Buttons spec, for example a recent draft. It is 4:5 and shows the length
 * at the top and the title at the bottom.
 *
 * @param title Name of the video.
 * @param duration Length of the video, for example "0:18".
 * @param hue Hue of the placeholder thumbnail.
 * @param onClick Action on click.
 * @param modifier Modifier for the card. Give it a width.
 * @param enabled Set to false for the disabled state.
 * @param caption Short line above the title, for example "어제", or null.
 */
@Composable
fun MagicVideoCard(
    title: String,
    duration: String,
    hue: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    caption: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    StripeBox(
        hue = hue,
        lightness = 0.46f,
        shape = RoundedCornerShape(22.dp),
        colorFilter = if (enabled) null else Grayscale,
        modifier = modifier
            .aspectRatio(4f / 5f)
            .then(if (enabled) Modifier else Modifier.alpha(0.35f))
            .pressClickable(enabled = enabled, interactionSource = interactionSource, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(if (pressed && enabled) VideoCardPressedScrim else VideoCardScrim)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            val light = Color.White.copy(alpha = 0.88f)
            Text(
                text = duration,
                color = light,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            )
            Icon(Icons.Rounded.NorthEast, contentDescription = null, tint = light, modifier = Modifier.size(18.dp))
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (caption != null) {
                Text(text = caption, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 1.2.em,
                letterSpacing = (-0.03).em,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                // 제목 줄 길이를 고르게 나눈다(CSS text-wrap: balance).
                style = LocalTextStyle.current.copy(lineBreak = LineBreak.Heading),
            )
        }
    }
}

@Preview(name = "Cards", widthDp = 390)
@Composable
private fun MagicCardsPreview() {
    MagicPreview {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MagicActionCard(title = "새 영상\n만들기", icon = Icons.Rounded.Add, onClick = {}, modifier = Modifier.weight(1.3f))
                MagicActionCard(
                    title = "촬영",
                    icon = Icons.Rounded.PhotoCamera,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    style = MagicActionCardStyle.SECONDARY,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MagicVideoCard(title = "바닷가 주말", duration = "0:18", hue = 35f, onClick = {}, modifier = Modifier.width(168.dp))
                MagicVideoCard(
                    title = "작업실 투어",
                    duration = "0:42",
                    hue = 200f,
                    onClick = {},
                    modifier = Modifier.width(168.dp),
                    enabled = false,
                )
            }
        }
    }
}
