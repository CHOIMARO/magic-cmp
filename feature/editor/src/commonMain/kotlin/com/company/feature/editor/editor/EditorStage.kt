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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.clipAt
import com.company.ui.editor.OverlayText
import com.company.ui.editor.TextFrame
import com.company.ui.editor.centeredAt
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime
import com.company.ui.editor.look
import com.company.ui.editor.toColorFilter
import com.company.ui.media.MediaThumbnail

/**
 * 9:16 preview of the project at the playhead.
 *
 * With [video], the first layout cell shows the video of the clip at the playhead. The other cells
 * show a thumbnail of the next clips. Without [video], all cells show placeholder stripes.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 * @param modifier Modifier for the stage. Give it a size.
 * @param video Video surface of the preview player, or null to show placeholders.
 */
@Composable
internal fun EditorStage(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    modifier: Modifier = Modifier,
    video: (@Composable (Modifier) -> Unit)? = null,
) {
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
        val screenDensity = LocalDensity.current

        // 레이아웃·글자·자막·워터마크는 기준 무대(StageCanvas) 위에 그린다. 저장할 때도 같은 코드로 그린다.
        if (current != null) {
            StageCanvas(modifier = Modifier.fillMaxSize()) {
                if (video != null) {
                    StageLayoutCells(project = project, clipIndex = current.index, modifier = Modifier.fillMaxSize()) { index, clip ->
                        if (index == 0) {
                            // iOS 영상 뷰는 LocalDensity로 화면 좌표를 계산하므로, 영상은 화면의 실제 배율로 그린다.
                            CompositionLocalProvider(LocalDensity provides screenDensity) {
                                video(Modifier.matchParentSize())
                            }
                        } else {
                            // 칸마다 영상을 합성하기 전까지, 다음 클립은 시작 화면으로 보여 준다.
                            MediaThumbnail(
                                uri = clip.sourceUri,
                                type = clip.type,
                                hue = clip.thumbnailHue,
                                modifier = Modifier.matchParentSize(),
                                time = clip.inPoint,
                                colorFilter = filter,
                            )
                        }
                    }
                } else {
                    StageLayoutCells(
                        project = project,
                        clipIndex = current.index,
                        modifier = Modifier.fillMaxSize(),
                        colorFilter = filter,
                        stripeOffset = { index -> (state.time * 26 + index * 40).dp },
                    )
                }
            }
            if (video == null) {
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
            }
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

        StageCanvas(modifier = Modifier.fillMaxSize()) {
            project.texts.filter { it.isVisibleAt(state.time) }.forEach { overlay ->
                key(overlay.id) {
                    DraggableOverlay(
                        overlay = overlay,
                        frame = if (state.playing) overlay.frameAt(state.time) else TextFrame.Static,
                        stageWidth = stageWidth,
                        stageHeight = stageHeight,
                        onIntent = onIntent,
                    )
                }
            }
            // 등장·퇴장 애니메이션은 재생할 때만 보여 준다. 멈춘 화면에서는 글자를 고치기 쉽게 그대로 보여 준다.
            StageCaption(project = project, time = state.time, animate = state.playing)
            if (state.watermark) StageWatermark()
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
    frame: TextFrame,
    stageWidth: Float,
    stageHeight: Float,
    onIntent: (EditorIntent) -> Unit,
) {
    val latest by rememberUpdatedState(overlay)
    OverlayText(
        text = overlay.text,
        look = overlay.style.look(),
        frame = frame,
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
