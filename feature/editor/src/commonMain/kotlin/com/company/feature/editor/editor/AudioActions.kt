package com.company.feature.editor.editor

import com.company.domain.model.editor.AudioTrack
import com.company.domain.model.editor.DubbingContent
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.MAX_DUBBING_CHARACTERS
import com.company.domain.model.editor.MY_VOICE_ID
import com.company.domain.model.editor.MusicRange
import com.company.domain.model.editor.MusicSettings
import com.company.domain.model.editor.addDubbing
import com.company.domain.model.editor.addSoundEffect
import com.company.domain.model.editor.defaultMusicRange
import com.company.domain.model.editor.duplicateAudioItem
import com.company.domain.model.editor.fitDubbingToCaption
import com.company.domain.model.editor.moveMusicRange
import com.company.domain.model.editor.placedCaptions
import com.company.domain.model.editor.removeAudioItem
import com.company.domain.model.editor.replaceDubbing
import com.company.domain.model.editor.setAudioVolume
import com.company.domain.model.editor.setClipVolume
import com.company.domain.model.editor.setMusic
import com.company.domain.repository.DubbingRepository
import com.company.feature.editor.resources.*
import com.company.ui.text.uiText
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Number of effects in the recent effects. */
private const val RECENT_EFFECT_COUNT = 8

/**
 * Music sheet (F10), dubbing sheet (F05), sound effect sheet (F07), the volume sheet, and the edits of the dubbing
 * and sound effect bars.
 *
 * @param host Editor state and project.
 * @param dubbingRepository Makes the audio of a dubbing.
 */
internal class AudioActions(private val host: EditorHost, private val dubbingRepository: DubbingRepository) {
    private var dubbingJob: Job? = null
    private var previewJob: Job? = null

    /** Time of the last volume change. A quick series of changes is one undo step. */
    private var lastVolumeChange: TimeMark? = null

    // =====================================================================
    // Music
    // =====================================================================

    /** Opens the music sheet with the music of the project. */
    fun openMusic() {
        host.pause()
        val state = host.state
        val project = state.project
        val track = state.musicTracks.find { it.id == project.musicId }
        val range = track?.let { project.musicRange ?: defaultMusicRange(it.durationSeconds.toDouble(), project.totalDuration) }
        val draft = MusicDraft(musicId = track?.id, range = range, fade = project.musicFade, volume = project.musicVolume)
        host.reduce { copy(sheet = EditorSheet.MUSIC, musicDraft = draft) }
    }

    /**
     * Selects a track and plays its preview. A tap on the selected track stops or starts the preview.
     *
     * @param musicId ID of the track, or null for no music.
     */
    fun tapMusic(musicId: String?) {
        val state = host.state
        val draft = state.musicDraft ?: return
        if (draft.musicId == musicId) {
            if (musicId != null) host.reduce { copy(musicDraft = draft.copy(previewing = !draft.previewing, previewStart = draft.previewStart + 1)) }
            return
        }
        val track = state.musicTracks.find { it.id == musicId }
        val project = state.project
        val range = track?.let {
            if (project.musicId == it.id && project.musicRange != null) project.musicRange else defaultMusicRange(it.durationSeconds.toDouble(), project.totalDuration)
        }
        host.reduce { copy(musicDraft = draft.copy(musicId = track?.id, range = range, previewing = track != null, previewStart = draft.previewStart + 1)) }
    }

    /**
     * Moves the edges of the part of the track. The preview plays the part from its start again.
     *
     * @param origin Part when the drag started.
     * @param delta Movement in track seconds.
     * @param moveStart True to move the start.
     * @param moveEnd True to move the end.
     */
    fun moveMusicRange(origin: MusicRange, delta: Double, moveStart: Boolean, moveEnd: Boolean) {
        val state = host.state
        val draft = state.musicDraft ?: return
        val track = state.musicTracks.find { it.id == draft.musicId } ?: return
        val range = moveMusicRange(track.durationSeconds.toDouble(), origin, delta, moveStart, moveEnd)
        host.reduce { copy(musicDraft = draft.copy(range = range, previewing = true, previewStart = draft.previewStart + 1)) }
    }

    /** Applies the music of the sheet, or removes the music, and closes the sheet. */
    fun applyMusic() {
        val state = host.state
        val draft = state.musicDraft ?: return
        val had = state.project.musicId != null
        if (draft.musicId == null && !had) return
        host.projects.commit { it.setMusic(MusicSettings(draft.musicId, draft.range, draft.fade, draft.volume)) }
        host.reduce { copy(sheet = null, musicDraft = null) }
        host.toast(
            when {
                draft.musicId == null -> uiText(Res.string.toast_music_removed)
                had -> uiText(Res.string.toast_music_changed)
                else -> uiText(Res.string.toast_music_added)
            },
        )
    }

    // =====================================================================
    // Dubbing
    // =====================================================================

    /**
     * Opens the dubbing sheet.
     *
     * A new dubbing takes the text of [captionId], or of the caption at the playhead. A dubbing to make again keeps
     * its text, voice, and speed.
     *
     * @param captionId Caption to read, or null.
     * @param editId Dubbing to make again, or null for a new dubbing.
     */
    fun openDubbing(captionId: String?, editId: String?) {
        host.pause()
        val state = host.state
        // 서버 목소리를 아직 받지 못했으면 다시 받는다. 받으면 초안이 첫 목소리를 고른다.
        if (state.voices.isEmpty()) host.scope.launch { dubbingRepository.loadVoices() }
        val defaultVoice = state.voices.firstOrNull()?.id.orEmpty()
        val draft = editId?.let { id ->
            state.project.dubbings.find { it.id == id }?.let {
                // 예전 버전의 목소리(서버에 없는 ID)로 만든 더빙은 서버의 첫 목소리로 다시 만든다.
                val voiceId = it.voiceId.takeIf { voice -> voice == MY_VOICE_ID || state.voices.any { v -> v.id == voice } } ?: defaultVoice
                DubbingDraft(text = it.text, voiceId = voiceId, speed = it.speed, captionId = it.captionId, editId = it.id)
            }
        } ?: run {
            val captions = state.project.placedCaptions
            val caption = captionId?.let { id -> captions.find { it.id == id } }
                ?: captions.find { state.time >= it.start && state.time < it.end }
            DubbingDraft(
                text = caption?.text.orEmpty().take(MAX_DUBBING_CHARACTERS),
                // 내 목소리는 서버에서 아직 쓸 수 없으므로(Typecast 유료 플랜 전) 기본으로 고르지 않는다.
                voiceId = defaultVoice,
                speed = DubbingSpeed.NORMAL,
                captionId = caption?.id,
                editId = null,
            )
        }
        host.reduce { copy(sheet = EditorSheet.DUBBING, dubbingDraft = draft) }
    }

    /**
     * Changes the text of the dubbing. The text keeps [MAX_DUBBING_CHARACTERS] characters at most.
     *
     * @param text New text.
     */
    fun changeDubbingText(text: String) {
        changeDubbingDraft { copy(text = text.take(MAX_DUBBING_CHARACTERS)) }
    }

    /**
     * Changes the text, the voice, or the speed of the dubbing. The change stops the preview.
     *
     * @param change Change of the draft.
     */
    fun changeDubbingDraft(change: DubbingDraft.() -> DubbingDraft) {
        previewJob?.cancel()
        host.reduce { copy(dubbingDraft = dubbingDraft?.change()?.copy(previewing = false, previewUrl = null)) }
    }

    /**
     * Plays the preview of the dubbing. The server makes the audio first. A later insert of the same content uses
     * the same audio, so the server does not make it two times.
     */
    fun startDubbingPreview() {
        val draft = host.state.dubbingDraft ?: return
        val content = draft.content() ?: return
        if (draft.busy || draft.previewing) return
        host.reduce { copy(dubbingDraft = draft.copy(previewing = true, previewUrl = null)) }
        previewJob?.cancel()
        previewJob = host.scope.launch {
            try {
                val url = dubbingRepository.preview(content)
                host.reduce { copy(dubbingDraft = dubbingDraft?.takeIf { it.previewing }?.copy(previewUrl = url) ?: dubbingDraft) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failDubbingPreview()
            }
        }
    }

    /** Stops the preview of the dubbing. */
    fun stopDubbingPreview() {
        previewJob?.cancel()
        host.reduce { copy(dubbingDraft = dubbingDraft?.copy(previewing = false, previewUrl = null)) }
    }

    /** Stops the preview after a failure and tells the user. */
    fun failDubbingPreview() {
        stopDubbingPreview()
        host.toast(uiText(Res.string.toast_dubbing_failed))
    }

    /** Makes the audio, then adds the dubbing or replaces the old dubbing, and closes the sheet. */
    fun insertDubbing() {
        val draft = host.state.dubbingDraft ?: return
        val content = draft.content() ?: return
        if (draft.busy) return
        previewJob?.cancel()
        host.reduce { copy(dubbingDraft = draft.copy(busy = true, previewing = false, previewUrl = null)) }
        dubbingJob = host.scope.launch {
            try {
                val audio = dubbingRepository.synthesize(content)
                val editId = draft.editId
                if (editId != null) {
                    host.projects.commit { it.replaceDubbing(editId, content, audio) }
                    host.reduce { copy(sheet = null, dubbingDraft = null) }
                    host.toast(uiText(Res.string.toast_dubbing_remade))
                } else {
                    val time = host.state.time
                    var newId = ""
                    host.projects.commit { project ->
                        project.addDubbing(content, audio, time, draft.captionId).let { (changed, id) ->
                            newId = id
                            changed
                        }
                    }
                    host.reduce {
                        copy(sheet = null, dubbingDraft = null, selectedTrackItem = TrackSelection.Dubbing(newId), selectedClipId = null, selectedTextId = null)
                    }
                    host.toast(uiText(Res.string.toast_dubbing_added))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                host.reduce { copy(dubbingDraft = dubbingDraft?.copy(busy = false)) }
                host.toast(uiText(Res.string.toast_dubbing_failed))
            }
        }
    }

    /** Plays the selected dubbing faster so that it ends with its caption (D12). */
    fun fitDubbing() {
        val dubbing = host.state.selectedDubbing ?: return
        host.projects.commit { it.fitDubbingToCaption(dubbing.id) }
        host.toast(uiText(Res.string.toast_dubbing_fitted))
    }

    /** Stops the production of a dubbing and its preview. Closing the sheet calls it. */
    fun stop() {
        dubbingJob?.cancel()
        dubbingJob = null
        previewJob?.cancel()
        previewJob = null
    }

    /** Content to send to the server, or null when the text is empty or no voice is selected. */
    private fun DubbingDraft.content(): DubbingContent? {
        if (text.isBlank() || voiceId.isEmpty()) return null
        return DubbingContent(text = text.trim(), voiceId = voiceId, speed = speed)
    }

    // =====================================================================
    // Sound effects and volume
    // =====================================================================

    /**
     * Adds a sound effect at the playhead, selects it, and closes the sheet.
     *
     * @param effectId ID of the effect.
     */
    fun insertSoundEffect(effectId: String) {
        val state = host.state
        val effect = state.soundEffects.find { it.id == effectId } ?: return
        val time = state.time
        var newId = ""
        host.projects.commit { project ->
            project.addSoundEffect(effect, time).let { (changed, id) ->
                newId = id
                changed
            }
        }
        host.reduce {
            copy(
                sheet = null,
                selectedTrackItem = TrackSelection.SoundEffect(newId),
                selectedClipId = null,
                selectedTextId = null,
                recentSoundEffects = (listOf(effect.id) + recentSoundEffects.filter { it != effect.id }).take(RECENT_EFFECT_COUNT),
            )
        }
        host.toast(uiText(Res.string.toast_effect_added, effect.name))
    }

    /**
     * Changes the volume of the item of the volume sheet. The first change of a quick series keeps an undo step.
     *
     * @param volume New volume from 0 to 100.
     */
    fun changeVolume(volume: Int) {
        val target = host.state.volumeTarget ?: return
        val fresh = lastVolumeChange?.let { it.elapsedNow().inWholeMilliseconds > SLIDER_STEP_MILLIS } ?: true
        lastVolumeChange = TimeSource.Monotonic.markNow()
        if (fresh) host.projects.saveCheckpoint()
        host.projects.update {
            when (target.kind) {
                VolumeKind.CLIP -> it.setClipVolume(target.id, volume)
                VolumeKind.DUBBING -> it.setAudioVolume(AudioTrack.DUBBING, target.id, volume)
                VolumeKind.SOUND_EFFECT -> it.setAudioVolume(AudioTrack.SOUND_EFFECT, target.id, volume)
            }
        }
    }

    /** Puts a copy of the selected dubbing or sound effect after it, and selects the copy. */
    fun duplicateSelected() {
        val selection = host.state.selectedTrackItem ?: return
        val track = selection.audioTrack() ?: return
        val (project, copyId) = host.state.project.duplicateAudioItem(track, selection.id) ?: return
        host.projects.commit { project }
        host.reduce {
            copy(selectedTrackItem = if (track == AudioTrack.DUBBING) TrackSelection.Dubbing(copyId) else TrackSelection.SoundEffect(copyId))
        }
        host.toast(uiText(Res.string.toast_duplicated))
    }

    /** Removes the selected dubbing or sound effect. */
    fun removeSelected() {
        val selection = host.state.selectedTrackItem ?: return
        val track = selection.audioTrack() ?: return
        host.projects.commit { it.removeAudioItem(track, selection.id) }
    }
}

/** Audio track of the selected bar, or null for a caption. */
internal fun TrackSelection.audioTrack(): AudioTrack? = when (this) {
    is TrackSelection.Caption -> null
    is TrackSelection.Dubbing -> AudioTrack.DUBBING
    is TrackSelection.SoundEffect -> AudioTrack.SOUND_EFFECT
}
