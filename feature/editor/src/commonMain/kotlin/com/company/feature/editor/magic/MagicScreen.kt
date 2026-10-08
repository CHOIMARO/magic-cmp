package com.company.feature.editor.magic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.FilterType
import com.company.ui.editor.toColorFilter

/**
 * Magic screen: animated cards and the list of steps.
 *
 * @param state Screen state.
 */
@Composable
internal fun MagicScreen(state: MagicState) {
    Column(
        modifier = Modifier.fillMaxSize().background(MagicColors.Background).padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically),
    ) {
        CardStack(state = state)
        Text(
            text = "magic 거는 중…",
            fontFamily = MagicTheme.fonts.serif,
            fontStyle = FontStyle.Italic,
            fontSize = 34.sp,
        )
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MagicViewModel.STEPS.forEachIndexed { index, label ->
                StepRow(label = label, done = state.step > index, running = state.step == index)
            }
        }
    }
}

@Composable
private fun CardStack(state: MagicState) {
    val cardShape = RoundedCornerShape(18.dp)
    Box(modifier = Modifier.size(width = 132.dp, height = 200.dp)) {
        StripeBox(
            hue = state.firstHue.toFloat(), lightness = 0.55f, shape = cardShape,
            modifier = Modifier.matchParentSize().offset(x = (-18).dp).rotate(-8f).alpha(0.5f),
        )
        StripeBox(
            hue = state.secondHue.toFloat(), lightness = 0.55f, shape = cardShape,
            modifier = Modifier.matchParentSize().offset(x = 18.dp).rotate(7f).alpha(0.6f),
        )
        StripeBox(
            hue = state.firstHue.toFloat(), lightness = 0.55f, shape = cardShape,
            colorFilter = if (state.step >= 4) FilterType.WARM.toColorFilter() else null,
            modifier = Modifier
                .matchParentSize()
                .shadow(elevation = 30.dp, shape = cardShape, spotColor = Color(0xFF9260DA), ambientColor = Color(0xFF9260DA)),
        )
        Sparkle(size = 30.dp, color = MagicColors.AccentSparkle, delayMillis = 0, modifier = Modifier.align(Alignment.TopEnd).offset(x = 16.dp, y = (-14).dp))
        Sparkle(size = 20.dp, color = MagicColors.Sparkle, delayMillis = 500, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-18).dp, y = (-24).dp))
    }
}

@Composable
private fun Sparkle(size: Dp, color: Color, delayMillis: Int, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis),
        ),
    )
    Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = color,
        modifier = modifier
            .size(size)
            .alpha(0.35f + 0.65f * progress)
            .scale(0.85f + 0.25f * progress),
    )
}

@Composable
private fun StepRow(label: String, done: Boolean, running: Boolean) {
    val alpha by animateFloatAsState(if (done || running) 1f else 0.35f, tween(300))
    Row(
        modifier = Modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> MagicColors.Accent
                        running -> MagicColors.Accent.copy(alpha = 0.35f)
                        else -> MagicColors.SurfaceHigh
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Rounded.Check, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(17.dp))
                running -> CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MagicColors.OnAccent,
                    strokeWidth = 2.dp,
                )
            }
        }
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
