package com.company.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import kotlinx.coroutines.delay

/**
 * State of the app-wide toast.
 *
 * The toast stays on screen after navigation, so one screen can show a message for the next screen.
 */
@Stable
class MagicToastState {
    /**
     * Current message.
     *
     * @property text Text of the message.
     * @property durationMillis Time on screen.
     * @property id Unique key. A new key restarts the timer, also for the same text.
     */
    data class Message(val text: String, val durationMillis: Long, val id: Long)

    /** Current message. Null hides the toast. */
    var message: Message? by mutableStateOf(null)
        private set

    private var nextId = 0L

    /**
     * Shows a message.
     *
     * @param text Text of the message.
     * @param durationMillis Time on screen.
     */
    fun show(text: String, durationMillis: Long = 1600L) {
        message = Message(text = text, durationMillis = durationMillis, id = nextId++)
    }

    /**
     * Hides the message if it is still the message with [id].
     *
     * @param id Key of the message to hide.
     */
    fun dismiss(id: Long) {
        if (message?.id == id) message = null
    }
}

/** App-wide toast. The app root provides it. */
val LocalMagicToast = staticCompositionLocalOf { MagicToastState() }

/** Creates and remembers a [MagicToastState]. */
@Composable
fun rememberMagicToastState(): MagicToastState = remember { MagicToastState() }

/**
 * Shows the toast of [state] at the top of the screen.
 *
 * @param state Toast state.
 * @param modifier Modifier for the toast area.
 */
@Composable
fun MagicToastHost(state: MagicToastState, modifier: Modifier = Modifier) {
    val message = state.message
    LaunchedEffect(message?.id) {
        val current = message ?: return@LaunchedEffect
        delay(current.durationMillis)
        state.dismiss(current.id)
    }

    // 닫히는 동안 마지막 문구를 유지한다. 화면 상태가 아니므로 Snapshot 상태로 두지 않는다.
    val lastText = remember { arrayOf("") }
    if (message != null) lastText[0] = message.text

    Box(modifier = modifier.fillMaxWidth().padding(top = 62.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(visible = message != null, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Text(
                text = lastText[0],
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MagicColors.OnSurface.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                color = MagicColors.Background,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Preview(name = "Toast", widthDp = 360)
@Composable
private fun MagicToastPreview() {
    // 실행형 프리뷰에서도 바로 사라지지 않도록 표시 시간을 길게 둔다.
    val state = remember { MagicToastState().apply { show("3개를 붙였어요", durationMillis = 60_000L) } }
    MagicPreview(modifier = Modifier.height(120.dp)) {
        MagicToastHost(state = state)
    }
}
