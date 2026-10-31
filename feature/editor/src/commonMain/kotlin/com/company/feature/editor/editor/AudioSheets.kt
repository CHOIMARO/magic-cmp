package com.company.feature.editor.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonLoading
import com.company.core.designsystem.component.MagicChip
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.designsystem.theme.oklch
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.MAX_DUBBING_CHARACTERS
import com.company.domain.model.editor.MAX_VOLUME
import com.company.domain.model.editor.MY_VOICE_ID
import com.company.domain.model.editor.MyVoiceStatus
import com.company.domain.model.editor.SoundEffect
import com.company.domain.model.editor.SoundEffectCategory
import com.company.feature.editor.resources.*
import com.company.ui.editor.displayName
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Length of a voice sample preview, in milliseconds. */
private const val VOICE_SAMPLE_MILLIS = 1600L

/** Extra time of a sound effect preview, in milliseconds. */
private const val EFFECT_PREVIEW_EXTRA_MILLIS = 150L

/** Color of the caption hint in the dubbing field. */
private val CaptionHintColor = oklch(0.88f, 0.1f, 95f)

/** Warning color of the character counter. */
private val CounterWarningColor = oklch(0.84f, 0.12f, 70f)

/** Hue of the sound effect bars and icons. */
internal const val SOUND_EFFECT_HUE = 45f

/**
 * Estimated length of a dubbing in seconds: 0.16 seconds for each character without spaces, divided by the speed.
 * The preview of the dubbing sheet uses it. It is the same estimate as the fake text to speech service.
 */
private fun estimatedDubbingSeconds(text: String, speed: DubbingSpeed): Double =
    maxOf(0.6, text.count { !it.isWhitespace() } * 0.16 / speed.factor)

// =====================================================================
// Dubbing
// =====================================================================

/**
 * Dubbing sheet (F05): text, voice, speed, preview, and the insert button. The user's own voice is first in the list.
 *
 * The app has no audio yet, so the previews only show their progress.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun DubbingSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val draft = state.dubbingDraft ?: return
    var samplePlaying by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(samplePlaying) {
        if (samplePlaying != null) {
            delay(VOICE_SAMPLE_MILLIS)
            samplePlaying = null
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = draft.text,
            onValueChange = { onIntent(EditorIntent.ChangeDubbingText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MagicColors.Background)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 26.dp),
            textStyle = TextStyle(color = MagicColors.OnSurface, fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, fontFamily = MagicTheme.fonts.sans),
            cursorBrush = SolidColor(MagicColors.Accent),
            decorationBox = { inner ->
                Box {
                    if (draft.text.isEmpty()) Text(text = stringResource(Res.string.dubbing_hint), fontSize = 15.sp, color = MagicColors.OnSurface40)
                    inner()
                }
            },
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = if (draft.captionId != null) stringResource(Res.string.dubbing_from_caption) else "", fontSize = 11.5.sp, color = CaptionHintColor)
            val count = draft.text.length
            Text(
                text = if (count >= MAX_DUBBING_CHARACTERS) stringResource(Res.string.dubbing_max_chars, MAX_DUBBING_CHARACTERS) else "$count/$MAX_DUBBING_CHARACTERS",
                fontSize = 11.5.sp,
                color = if (count >= MAX_DUBBING_CHARACTERS) CounterWarningColor else MagicColors.OnSurface40,
            )
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = 176.dp).bleedHorizontal(8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MyVoiceRow(status = state.myVoice, selected = draft.voiceId == MY_VOICE_ID, playing = samplePlaying == MY_VOICE_ID, onIntent = onIntent) {
            samplePlaying = if (samplePlaying == MY_VOICE_ID) null else MY_VOICE_ID
        }
        state.voices.forEach { voice ->
            VoiceRow(
                name = voice.name,
                description = voice.description,
                avatar = { VoiceAvatar(color = oklch(0.82f, 0.1f, voice.hue.toFloat())) { Text(text = voice.name.take(1), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MagicColors.OnAccent) } },
                selected = draft.voiceId == voice.id,
                playing = samplePlaying == voice.id,
                onPick = { onIntent(EditorIntent.PickDubbingVoice(voice.id)) },
                onPlay = { samplePlaying = if (samplePlaying == voice.id) null else voice.id },
            )
        }
    }
    SegmentedControl(
        options = DubbingSpeed.entries.map { SegmentOption(it.displayName) },
        selectedIndex = draft.speed.ordinal,
        onSelect = { onIntent(EditorIntent.PickDubbingSpeed(DubbingSpeed.entries[it])) },
        containerColor = MagicColors.Background,
        itemHeight = 40.dp,
    )
    val canInsert = draft.text.isNotBlank() && !draft.busy
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DubbingPreviewButton(
            seconds = estimatedDubbingSeconds(draft.text, draft.speed),
            enabled = canInsert,
            key = draft.text to draft.speed,
        )
        MagicButton(
            text = stringResource(if (draft.editId != null) Res.string.dubbing_replace else Res.string.dubbing_insert),
            onClick = { onIntent(EditorIntent.InsertDubbing) },
            modifier = Modifier.weight(1f),
            enabled = canInsert || draft.busy,
            loading = if (draft.busy) MagicButtonLoading.Spinner(stringResource(Res.string.dubbing_making)) else null,
        )
    }
}

/**
 * Row of the user's own voice. A ready voice is a normal voice row. Otherwise the row opens the screen that makes
 * the voice, or shows the progress.
 */
@Composable
private fun MyVoiceRow(status: MyVoiceStatus, selected: Boolean, playing: Boolean, onIntent: (EditorIntent) -> Unit, onPlay: () -> Unit) {
    if (status == MyVoiceStatus.Ready) {
        VoiceRow(
            name = stringResource(Res.string.voice_mine),
            description = stringResource(Res.string.voice_mine_description),
            avatar = { VoiceAvatar(color = MagicColors.Accent) { Icon(Icons.Rounded.Person, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(20.dp)) } },
            selected = selected,
            playing = playing,
            onPick = { onIntent(EditorIntent.PickDubbingVoice(MY_VOICE_ID)) },
            onPlay = onPlay,
        )
        return
    }
    val making = status as? MyVoiceStatus.Making
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .pressClickable(pressedScale = 0.99f) { onIntent(EditorIntent.OpenMyVoice) }
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, MagicColors.OnSurface.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceAvatar(color = MagicColors.SurfaceHighest) {
            Icon(if (making != null) Icons.Rounded.HourglassTop else Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = stringResource(if (making != null) Res.string.voice_mine_making else Res.string.voice_mine_make), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (making != null) stringResource(Res.string.voice_mine_progress, making.percent) else stringResource(Res.string.voice_mine_make_hint),
                modifier = Modifier.padding(top = 1.dp),
                fontSize = 12.sp,
                color = MagicColors.OnSurface55,
            )
        }
    }
}

/**
 * One voice of the dubbing sheet.
 *
 * @param name Name of the voice.
 * @param description Short description.
 * @param avatar Round avatar.
 * @param selected True for the selected voice.
 * @param playing True while the sample plays.
 * @param onPick Selects the voice.
 * @param onPlay Starts or stops the sample.
 */
@Composable
private fun VoiceRow(
    name: String,
    description: String,
    avatar: @Composable () -> Unit,
    selected: Boolean,
    playing: Boolean,
    onPick: () -> Unit,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .pressClickable(pressedScale = 0.99f, onClick = onPick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MagicColors.SurfaceHigh else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        avatar()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, modifier = Modifier.padding(top = 1.dp), fontSize = 12.sp, color = MagicColors.OnSurface55)
        }
        Box(modifier = Modifier.size(40.dp).pressClickable(onClick = onPlay), contentAlignment = Alignment.Center) {
            Icon(
                if (playing) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                contentDescription = stringResource(Res.string.voice_sample_listen),
                tint = if (playing) MagicColors.Accent else MagicColors.OnSurface.copy(alpha = 0.75f),
                modifier = Modifier.size(28.dp),
            )
        }
        CheckCircle(selected = selected, size = 24.dp)
    }
}

/** Round avatar of a voice. */
@Composable
private fun VoiceAvatar(color: Color, content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { content() }
}

/**
 * stringResource(Res.string.listen_preview) button of the dubbing sheet. It fills from the left while the preview plays.
 *
 * @param seconds Estimated length of the dubbing.
 * @param enabled Set to false when the text is empty.
 * @param key Text and speed. A change stops the preview.
 */
@Composable
private fun DubbingPreviewButton(seconds: Double, enabled: Boolean, key: Any) {
    var playing by remember(key) { mutableStateOf(false) }
    var progress by remember(key) { mutableFloatStateOf(0f) }
    LaunchedEffect(playing, key) {
        progress = 0f
        if (!playing) return@LaunchedEffect
        val start = withFrameMillis { it }
        while (progress < 1f) {
            withFrameMillis { progress = ((it - start) / (seconds * 1000)).toFloat().coerceAtMost(1f) }
        }
        playing = false
    }
    Box(
        modifier = Modifier
            .height(56.dp)
            .pressClickable(enabled = enabled) { playing = !playing }
            .clip(CircleShape)
            .background(MagicColors.SurfaceHighest),
    ) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(MagicColors.OnSurface.copy(alpha = 0.12f)))
        Row(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val content = if (enabled) MagicColors.OnSurface else MagicColors.OnSurface.copy(alpha = 0.35f)
            Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Text(text = stringResource(Res.string.listen_preview), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = content)
        }
    }
}

// =====================================================================
// Sound effects
// =====================================================================

/** Tabs of the sound effect sheet, in the order of the chips. */
private val SoundEffectTabs: List<SoundEffectTab> =
    listOf(SoundEffectTab.All, SoundEffectTab.Recent) + SoundEffectCategory.entries.map { SoundEffectTab.Category(it) }

/**
 * Effects that a tab of the sound effect sheet shows.
 *
 * @param tab Tab of the sheet.
 * @param catalog All effects of the catalog.
 * @param recent IDs of the effects that the user added, newest first.
 */
internal fun soundEffectsOf(tab: SoundEffectTab, catalog: List<SoundEffect>, recent: List<String>): List<SoundEffect> = when (tab) {
    SoundEffectTab.All -> catalog
    SoundEffectTab.Recent -> recent.mapNotNull { id -> catalog.find { it.id == id } }
    is SoundEffectTab.Category -> catalog.filter { it.category == tab.category }
}

/**
 * Sound effect sheet (F07): tabs (all, recent, and the categories), the effects of the tab, a preview, and the insert
 * button of each effect.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun SoundEffectSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    var playing by remember { mutableStateOf<String?>(null) }
    val tab = state.soundEffectTab
    Row(
        modifier = Modifier.fillMaxWidth().bleedHorizontal(SheetPadding).horizontalScroll(rememberScrollState()).padding(horizontal = SheetPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SoundEffectTabs.forEach { item ->
            val label = when (item) {
                SoundEffectTab.All -> stringResource(Res.string.all)
                SoundEffectTab.Recent -> stringResource(Res.string.sound_recent)
                is SoundEffectTab.Category -> item.category.displayName
            }
            MagicChip(label = label, selected = tab == item, onClick = {
                playing = null
                onIntent(EditorIntent.PickSoundEffectTab(item))
            })
        }
    }
    val effects = soundEffectsOf(tab, state.soundEffects, state.recentSoundEffects)
    Column(
        modifier = Modifier.fillMaxWidth().height(236.dp).bleedHorizontal(8.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (effects.isEmpty()) {
            Text(
                text = stringResource(Res.string.sound_empty_recent),
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                fontSize = 14.sp,
                color = MagicColors.OnSurface50,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        effects.forEach { effect ->
            SoundEffectRow(
                effect = effect,
                playing = playing == effect.id,
                onPlay = { playing = if (playing == effect.id) null else effect.id },
                onEnd = { if (playing == effect.id) playing = null },
                onAdd = { onIntent(EditorIntent.InsertSoundEffect(effect.id)) },
            )
        }
    }
    Text(
        text = stringResource(Res.string.sound_hint),
        modifier = Modifier.fillMaxWidth(),
        fontSize = 12.sp,
        color = MagicColors.OnSurface45,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun SoundEffectRow(effect: SoundEffect, playing: Boolean, onPlay: () -> Unit, onEnd: () -> Unit, onAdd: () -> Unit) {
    LaunchedEffect(playing) {
        if (playing) {
            delay((effect.length * 1000).toLong() + EFFECT_PREVIEW_EXTRA_MILLIS)
            onEnd()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressClickable(pressedScale = 0.99f, onClick = onPlay)
            .clip(RoundedCornerShape(16.dp))
            .background(if (playing) MagicColors.SurfaceHigh else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(oklch(0.84f, 0.11f, SOUND_EFFECT_HUE, alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = oklch(0.9f, 0.08f, SOUND_EFFECT_HUE),
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = effect.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (playing) stringResource(Res.string.sound_playing) else stringResource(Res.string.sound_meta, ((effect.length * 10).roundToInt() / 10.0).toString(), effect.category.displayName),
                modifier = Modifier.padding(top = 1.dp),
                fontSize = 12.sp,
                color = MagicColors.OnSurface55,
            )
        }
        Box(
            modifier = Modifier
                .height(36.dp)
                .pressClickable(onClick = onAdd)
                .clip(CircleShape)
                .background(MagicColors.OnSurface)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(Res.string.insert), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MagicColors.Background)
        }
    }
}

// =====================================================================
// Volume
// =====================================================================

/**
 * Volume sheet of a clip, a dubbing, or a sound effect. A quick series of changes is one undo step.
 *
 * @param state Editor state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun VolumeSheet(state: EditorState, onIntent: (EditorIntent) -> Unit) {
    val target = state.volumeTarget ?: return
    val project = state.project
    val volume = when (target.kind) {
        VolumeKind.CLIP -> project.clips.find { it.id == target.id }?.volume
        VolumeKind.DUBBING -> project.dubbings.find { it.id == target.id }?.volume
        VolumeKind.SOUND_EFFECT -> project.soundEffects.find { it.id == target.id }?.volume
    } ?: MAX_VOLUME
    Row(
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(volumeIcon(volume), contentDescription = null, tint = MagicColors.OnSurface70, modifier = Modifier.size(24.dp))
        SheetSlider(
            value = volume.toFloat(),
            range = 0f..100f,
            onChange = { onIntent(EditorIntent.ChangeVolume(it.roundToInt())) },
            modifier = Modifier.weight(1f),
        )
        Text(text = "$volume%", modifier = Modifier.width(48.dp), fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Icon of a volume level. */
private fun volumeIcon(volume: Int): ImageVector = when {
    volume == 0 -> Icons.AutoMirrored.Rounded.VolumeOff
    volume < 50 -> Icons.AutoMirrored.Rounded.VolumeDown
    else -> Icons.AutoMirrored.Rounded.VolumeUp
}
