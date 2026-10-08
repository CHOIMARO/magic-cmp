package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.clipAt
import com.company.ui.editor.CaptionText
import com.company.ui.editor.LayoutCells
import com.company.ui.editor.OverlayText
import com.company.ui.editor.centeredAt
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime
import com.company.ui.editor.look
import com.company.ui.editor.spec
import com.company.ui.editor.toColorFilter
import kotlin.math.ceil

/**
 * 9:16 preview of the project at the playhead.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the stage. Give it a size.
 */
@Composable
internal fun EditorStage(state: EditorState, onIntent: (EditorIntent) -> Unit, modifier: Modifier = Modifier) {
    val project = state.project
    val current = project.clipAt(state.time)
    val filter = remember(project.filter) { project.filter.toColorFilter() }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MagicColors.Surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onIntent(EditorIntent.TogglePlay) },
            ),
    ) {
        val stageWidth = constraints.maxWidth.toFloat()
        val stageHeight = constraints.maxHeight.toFloat()

        if (current != null) {
            LayoutCells(
                spec = project.layout.spec,
                cellHue = { index -> project.clips[(current.index + index) % project.clips.size].thumbnailHue },
                modifier = Modifier.fillMaxSize(),
                defaultBackground = Color(0xFF111111),
                colorFilter = filter,
                stripeOffset = { index -> (state.time * 26 + index * 40).dp },
                labelFontSize = 13f,
                labelPadding = 8.dp to 3.dp,
                labelRadius = 6.dp,
            )
            Text(
                text = "${current.clip.name} · ${formatTime(current.localTime)}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
            )
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.VideoLibrary, contentDescription = null, tint = MagicColors.OnSurface60, modifier = Modifier.size(34.dp))
                Text(text = "아직 영상이 없어요", fontSize = 14.sp, color = MagicColors.OnSurface60)
            }
        }

        val vibe = project.vibe
        if (vibe != null && vibe != Vibe.NONE && project.layout == LayoutType.FULL) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(start = 7.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MagicColors.AccentSparkle, modifier = Modifier.size(14.dp))
                Text(text = vibe.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        project.texts.filter { state.time >= it.start && state.time < it.start + it.length }.forEach { overlay ->
            key(overlay.id) {
                DraggableOverlay(
                    overlay = overlay,
                    stageWidth = stageWidth,
                    stageHeight = stageHeight,
                    onIntent = onIntent,
                )
            }
        }

        CaptionLayer(state = state, stageWidth = stageWidth, stageHeight = stageHeight)

        if (state.watermark) {
            Text(
                text = "magic",
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 8.dp),
                style = TextStyle(
                    fontFamily = MagicTheme.fonts.serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 2f), 8f),
                ),
            )
        }

        if (!state.playing && current != null && state.sheet == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 48.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "재생", modifier = Modifier.size(32.dp))
            }
        }
    }
}

/** Text overlay that the user can drag. A tap opens the text sheet. */
@Composable
private fun DraggableOverlay(
    overlay: TextOverlay,
    stageWidth: Float,
    stageHeight: Float,
    onIntent: (EditorIntent) -> Unit,
) {
    val latest by rememberUpdatedState(overlay)
    OverlayText(
        text = overlay.text,
        look = overlay.style.look(),
        modifier = Modifier
            .centeredAt(xPercent = overlay.x.toFloat(), yPercent = overlay.y.toFloat())
            .pointerInput(overlay.id) {
                var x = 0.0
                var y = 0.0
                detectDragGestures(
                    onDragStart = {
                        x = latest.x
                        y = latest.y
                        onIntent(EditorIntent.StartTextDrag)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        x += amount.x / stageWidth * 100
                        y += amount.y / stageHeight * 100
                        onIntent(EditorIntent.MoveText(overlay.id, x.coerceIn(12.0, 88.0), y.coerceIn(6.0, 94.0)))
                    },
                )
            }
            .pointerInput(overlay.id) {
                detectTapGestures(onTap = { onIntent(EditorIntent.OpenTextEditor(overlay.id)) })
            },
    )
}

/** Shows the caption at the playhead. The spoken words use the highlight color. */
@Composable
private fun CaptionLayer(state: EditorState, stageWidth: Float, stageHeight: Float) {
    val project = state.project
    val segment = project.captions.find { state.time >= it.start && state.time < it.start + it.length } ?: return
    val words = segment.text.split(" ")
    val spokenCount = maxOf(1, ceil((state.time - segment.start) / segment.length * words.size).toInt())
    val look = project.captionStyle.look()
    val density = LocalDensity.current
    val sideMargin = with(density) { (stageWidth * 0.06f).toDp() }
    val bottomMargin = with(density) { (stageHeight * look.bottomPercent / 100f).toDp() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        CaptionText(
            spoken = words.take(spokenCount).joinToString(" "),
            upcoming = words.drop(spokenCount).joinToString(" "),
            look = look,
            maxLines = 2,
            modifier = Modifier.padding(start = sideMargin, end = sideMargin, bottom = bottomMargin),
        )
    }
}
