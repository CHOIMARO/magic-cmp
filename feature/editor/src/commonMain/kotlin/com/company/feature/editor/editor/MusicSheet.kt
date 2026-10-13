package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicChip
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.MusicRange
import com.company.core.domain.model.editor.MusicTrack
import com.company.ui.editor.formatTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/** Number of bars of the waveform. */
private const val WAVE_BARS = 56

/** Width of a range handle on the waveform. */
private val WaveHandleWidth = 14.dp

/**
 * Music sheet (F10): pick a track, listen to it, pick the part to use, and set the volume and the fade. The change
 * applies only with the button at the bottom.
 *
 * The app has no music files yet, so the preview only moves a line over the waveform.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun MusicSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val draft = state.musicDraft ?: return
    val track = state.musicTracks.find { it.id == draft.musicId }
    val moods = remember(state.musicTracks) { state.musicTracks.map { it.mood }.distinct() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bleedHorizontal(SheetPadding)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = SheetPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MagicChip(label = "전체", selected = draft.mood == null, onClick = { onIntent(EditorIntent.PickMusicMood(null)) })
        moods.forEach { mood ->
            MagicChip(label = mood, selected = draft.mood == mood, onClick = { onIntent(EditorIntent.PickMusicMood(mood)) })
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (track != null) 180.dp else 300.dp)
            .bleedHorizontal(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MusicRow(title = "음악 없음", subtitle = "원본 소리만", hue = null, selected = draft.musicId == null, playing = false) {
            onIntent(EditorIntent.TapMusicTrack(null))
        }
        state.musicTracks.filter { draft.mood == null || it.mood == draft.mood }.forEach { item ->
            val selected = draft.musicId == item.id
            val playing = selected && draft.previewing
            MusicRow(
                title = item.title,
                subtitle = when {
                    playing -> "미리 듣는 중 · 다시 누르면 멈춰요"
                    else -> "${item.mood} · ${formatTime(item.durationSeconds.toDouble())}" + if (state.project.musicId == item.id) " · 사용 중" else ""
                },
                hue = item.thumbnailHue,
                selected = selected,
                playing = playing,
            ) { onIntent(EditorIntent.TapMusicTrack(item.id)) }
        }
    }
    val range = draft.range
    if (track != null && range != null) {
        MusicRangeSection(track = track, range = range, draft = draft, projectSeconds = state.totalDuration, onIntent = onIntent)
    }
    val hasMusic = state.project.musicId != null
    MagicButton(
        text = when {
            draft.musicId == null -> if (hasMusic) "음악 빼기" else "음악을 골라 주세요"
            hasMusic -> "이 구간으로 바꾸기"
            else -> "이 구간 넣기"
        },
        onClick = { onIntent(EditorIntent.ApplyMusic) },
        modifier = Modifier.fillMaxWidth(),
        enabled = draft.musicId != null || hasMusic,
    )
}

/**
 * One row of the music list. The cover shows a play or a pause mark.
 *
 * @param title Title of the track.
 * @param subtitle Mood and length, or the preview state.
 * @param hue Hue of the cover, or null for "음악 없음".
 * @param selected True for the selected row.
 * @param playing True while the preview plays.
 * @param onClick Action on click.
 */
@Composable
private fun MusicRow(title: String, subtitle: String, hue: Int?, selected: Boolean, playing: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .pressClickable(pressedScale = 0.99f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MagicColors.SurfaceHigh else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = when {
            hue == null -> Icons.Rounded.MusicOff
            playing -> Icons.Rounded.Pause
            else -> Icons.Rounded.PlayArrow
        }
        if (hue != null) {
            StripeBox(hue = hue.toFloat(), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(46.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp).align(Alignment.Center))
            }
        } else {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(MagicColors.SurfaceHigh), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, modifier = Modifier.padding(top = 2.dp), fontSize = 12.sp, color = MagicColors.OnSurface55)
        }
        CheckCircle(selected = selected, size = 26.dp)
    }
}

/**
 * Round check mark of a selected row.
 *
 * @param selected True to show the mark.
 * @param size Size of the circle.
 */
@Composable
internal fun CheckCircle(selected: Boolean, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(if (selected) MagicColors.Accent else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(size * 0.65f))
    }
}

/**
 * Part of the track to use: a waveform with two handles, the volume, and the fade.
 *
 * @param track Selected track.
 * @param range Selected part.
 * @param draft Music draft.
 * @param projectSeconds Length of the project.
 * @param onIntent Called with each user action.
 */
@Composable
private fun MusicRangeSection(track: MusicTrack, range: MusicRange, draft: MusicDraft, projectSeconds: Double, onIntent: (EditorIntent) -> Unit) {
    val seconds = track.durationSeconds.toDouble()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "쓸 구간", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
            Text(
                text = "${formatTime(range.start)} ~ ${formatTime(range.end)} · ${range.length.roundToInt()}초",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Waveform(track = track, range = range, previewing = draft.previewing, previewStart = draft.previewStart, onIntent = onIntent)
        Text(
            text = when {
                range.length > projectSeconds + 0.05 -> "영상보다 길어서 영상 끝에서 잘려요"
                range.length < projectSeconds - 0.05 -> "구간이 끝나면 음악 없이 이어져요"
                else -> "영상 길이와 딱 맞아요"
            },
            fontSize = 12.sp,
            color = MagicColors.OnSurface50,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                if (draft.volume == 0) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = MagicColors.OnSurface70,
                modifier = Modifier.size(22.dp),
            )
            SheetSlider(
                value = draft.volume.toFloat(),
                range = 0f..100f,
                onChange = { onIntent(EditorIntent.ChangeMusicVolume(it.roundToInt())) },
                modifier = Modifier.weight(1f),
            )
            Text(text = "${draft.volume}%", modifier = Modifier.width(40.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .pressClickable { onIntent(EditorIntent.ToggleMusicFade) }
                    .clip(CircleShape)
                    .background(if (draft.fade) MagicColors.OnSurface else MagicColors.SurfaceHigh)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val content = if (draft.fade) MagicColors.Background else MagicColors.OnSurface70
                Icon(if (draft.fade) Icons.Rounded.Check else Icons.Rounded.Add, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                Text(text = "페이드", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = content)
            }
        }
    }
}

/**
 * Waveform of the track with the selected part between two handles. A drag of a handle moves one edge, and a drag
 * of the part moves the whole part. While the preview plays, a white line moves through the part.
 */
@Composable
private fun Waveform(track: MusicTrack, range: MusicRange, previewing: Boolean, previewStart: Int, onIntent: (EditorIntent) -> Unit) {
    val seconds = track.durationSeconds.toDouble()
    val latestRange by rememberUpdatedState(range)
    val clock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(previewing, previewStart) {
        clock.longValue = 0L
        if (!previewing) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (true) withFrameMillis { clock.longValue = it - start }
    }
    val heights = remember(track.thumbnailHue) {
        List(WAVE_BARS) { index ->
            val hue = track.thumbnailHue.toFloat()
            0.16f + 0.84f * abs(sin(index * 0.9f + hue) * sin(index * 0.37f + hue * 0.1f))
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        // 손잡이가 파형 밖으로 나오도록 파형은 손잡이 폭만큼 안쪽에 그린다.
        val waveWidth = maxWidth - WaveHandleWidth * 2
        val waveWidthPx = with(LocalDensity.current) { waveWidth.toPx() }
        val startFraction = (range.start / seconds).toFloat()
        val endFraction = (range.end / seconds).toFloat()

        /** Drag that moves the edges from the part at the drag start. */
        fun Modifier.rangeDrag(moveStart: Boolean, moveEnd: Boolean): Modifier = pointerInput(moveStart, moveEnd, seconds) {
            var origin = latestRange
            var dragged = 0f
            detectHorizontalDragGestures(
                onDragStart = {
                    origin = latestRange
                    dragged = 0f
                },
            ) { change, amount ->
                change.consume()
                dragged += amount
                onIntent(EditorIntent.MoveMusicRange(origin, dragged / waveWidthPx * seconds, moveStart, moveEnd))
            }
        }

        Box(
            modifier = Modifier
                .offset(x = WaveHandleWidth)
                .width(waveWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(MagicColors.Background)
                .padding(vertical = 8.dp)
                .drawBehind {
                    val gap = 2.dp.toPx()
                    val barWidth = (size.width - gap * (WAVE_BARS - 1)) / WAVE_BARS
                    heights.forEachIndexed { index, height ->
                        val time = (index + 0.5) / WAVE_BARS * seconds
                        val inside = time >= range.start && time <= range.end
                        val barHeight = size.height * height
                        drawRoundRect(
                            color = MagicColors.OnSurface.copy(alpha = if (inside) 0.85f else 0.2f),
                            topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    }
                },
        )
        // 고른 구간: 끌면 길이를 그대로 두고 통째로 옮긴다.
        Box(
            modifier = Modifier
                .offset(x = WaveHandleWidth + waveWidth * startFraction)
                .width(waveWidth * (endFraction - startFraction))
                .fillMaxHeight()
                .background(MagicColors.Accent.copy(alpha = 0.12f))
                .drawBehind {
                    val line = 2.dp.toPx()
                    drawRect(MagicColors.Accent, size = Size(size.width, line))
                    drawRect(MagicColors.Accent, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                }
                .rangeDrag(moveStart = true, moveEnd = true),
        )
        RangeHandle(modifier = Modifier.offset(x = waveWidth * startFraction).rangeDrag(moveStart = true, moveEnd = false), start = true)
        RangeHandle(modifier = Modifier.offset(x = WaveHandleWidth + waveWidth * endFraction).rangeDrag(moveStart = false, moveEnd = true), start = false)
        if (previewing) {
            val position = range.start + (clock.longValue / 1000.0) % maxOf(0.1, range.length)
            Box(
                modifier = Modifier
                    .offset(x = WaveHandleWidth + waveWidth * (position / seconds).toFloat() - 1.dp)
                    .padding(vertical = 4.dp)
                    .width(2.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(Color.White),
            )
        }
    }
}

/** Purple handle at one edge of the selected music part. Give it the drag in [modifier]. */
@Composable
private fun RangeHandle(modifier: Modifier, start: Boolean) {
    val shape = if (start) RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp) else RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
    Box(
        modifier = modifier.size(width = WaveHandleWidth, height = 60.dp).clip(shape).background(MagicColors.Accent),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(width = 2.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(MagicColors.OnAccent))
    }
}
