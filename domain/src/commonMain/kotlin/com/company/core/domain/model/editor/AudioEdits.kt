package com.company.core.domain.model.editor

// =====================================================================
// Dubbings (F05, F06), sound effects (F07), and music (F10)
// =====================================================================

/** Largest number of characters in one dubbing. */
const val MAX_DUBBING_CHARACTERS = 200

/** Shortest length of a part of a music track, in seconds. */
const val MIN_MUSIC_RANGE = 3.0

/** Shortest length of a new music part, in seconds. A shorter project still gets this length. */
private const val MIN_DEFAULT_MUSIC_RANGE = 3.0

/**
 * Settings of the background music.
 *
 * @property musicId ID of the track, or null for no music.
 * @property range Part of the track to play.
 * @property fade True to fade the music in and out.
 * @property volume Volume from 0 to 100.
 */
data class MusicSettings(val musicId: String?, val range: MusicRange?, val fade: Boolean, val volume: Int)

/** Current music settings of the project. */
val EditorProject.musicSettings: MusicSettings
    get() = MusicSettings(musicId = musicId, range = musicRange, fade = musicFade, volume = musicVolume)

/**
 * Changes the background music.
 *
 * @param settings New settings. A null music ID removes the music, and the other settings stay.
 */
fun EditorProject.setMusic(settings: MusicSettings): EditorProject =
    if (settings.musicId == null) {
        copy(musicId = null)
    } else {
        copy(
            musicId = settings.musicId,
            musicRange = settings.range,
            musicFade = settings.fade,
            musicVolume = settings.volume.coerceIn(0, MAX_VOLUME),
        )
    }

/**
 * Part of a track that a new music choice uses: from the start of the track, as long as the project.
 *
 * @param trackSeconds Length of the track.
 * @param projectSeconds Length of the project.
 */
fun defaultMusicRange(trackSeconds: Double, projectSeconds: Double): MusicRange =
    MusicRange(0.0, minOf(trackSeconds, maxOf(MIN_DEFAULT_MUSIC_RANGE, projectSeconds)))

/**
 * Moves one edge or the whole part of a music track. The part stays inside the track and is at least
 * [MIN_MUSIC_RANGE] long.
 *
 * @param trackSeconds Length of the track.
 * @param origin Part when the drag started.
 * @param delta Movement in seconds of the track.
 * @param moveStart True to move the start.
 * @param moveEnd True to move the end. With [moveStart], the whole part moves and its length stays.
 */
fun moveMusicRange(trackSeconds: Double, origin: MusicRange, delta: Double, moveStart: Boolean, moveEnd: Boolean): MusicRange =
    when {
        moveStart && moveEnd -> {
            val start = (origin.start + delta).coerceIn(0.0, maxOf(0.0, trackSeconds - origin.length))
            MusicRange(start, start + origin.length)
        }
        moveStart -> MusicRange((origin.start + delta).coerceIn(0.0, maxOf(0.0, origin.end - MIN_MUSIC_RANGE)), origin.end)
        moveEnd -> MusicRange(origin.start, (origin.end + delta).coerceIn(minOf(trackSeconds, origin.start + MIN_MUSIC_RANGE), trackSeconds))
        else -> origin
    }

// =====================================================================
// Dubbings
// =====================================================================

/**
 * Text, voice, and speed of a dubbing.
 *
 * @property text Text that the voice reads.
 * @property voiceId ID of the voice.
 * @property speed Reading speed.
 */
data class DubbingContent(val text: String, val voiceId: String, val speed: DubbingSpeed)

/**
 * Audio that a voice made from a text.
 *
 * @property length Length of the audio in seconds at the normal play rate.
 * @property audioUri Location of the audio file, or null when no file exists.
 */
data class DubbingAudio(val length: Double, val audioUri: String?)

/**
 * Adds a dubbing. It starts at the caption that the text came from, or at [time]. When it overlaps another dubbing,
 * it goes to the first free row.
 *
 * @param content Text, voice, and speed.
 * @param audio Audio of the dubbing.
 * @param time Playhead position.
 * @param captionId ID of the caption that the text came from, or null.
 * @return The changed project and the ID of the new dubbing.
 */
fun EditorProject.addDubbing(content: DubbingContent, audio: DubbingAudio, time: Double, captionId: String?): Pair<EditorProject, String> {
    val caption = captionId?.let { id -> placedCaptions.find { it.id == id } }
    val start = caption?.start ?: time.coerceIn(0.0, maxOf(0.0, totalDuration - 0.5))
    val dubbing = Dubbing(
        id = newEditorId(),
        text = content.text,
        voiceId = content.voiceId,
        speed = content.speed,
        start = start,
        length = audio.length,
        layer = dubbings.firstFreeLayer(start, start + audio.length),
        captionId = caption?.id,
        audioUri = audio.audioUri,
    )
    return copy(dubbings = dubbings + dubbing) to dubbing.id
}

/**
 * Makes a dubbing again with a new text, voice, or speed. The start stays, and the play rate goes back to 1.
 *
 * @param dubbingId ID of the dubbing.
 * @param content New text, voice, and speed.
 * @param audio New audio.
 */
fun EditorProject.replaceDubbing(dubbingId: String, content: DubbingContent, audio: DubbingAudio): EditorProject =
    copy(
        dubbings = dubbings.map {
            if (it.id == dubbingId) {
                it.copy(text = content.text, voiceId = content.voiceId, speed = content.speed, length = audio.length, rate = 1.0, audioUri = audio.audioUri)
            } else {
                it
            }
        }.relayer(dubbingId) { item, layer -> item.copy(layer = layer) },
    )

/**
 * Seconds that a dubbing is longer than its caption. It is 0 when the dubbing has no caption or fits.
 *
 * @param dubbing The dubbing.
 */
fun EditorProject.dubbingOverflow(dubbing: Dubbing): Double {
    val caption = dubbing.captionId?.let { id -> placedCaptions.find { it.id == id } } ?: return 0.0
    return (dubbing.length - caption.length).coerceAtLeast(0.0)
}

/**
 * Plays a dubbing faster so that it ends with its caption.
 *
 * @param dubbingId ID of the dubbing.
 */
fun EditorProject.fitDubbingToCaption(dubbingId: String): EditorProject {
    val dubbing = dubbings.find { it.id == dubbingId } ?: return this
    val caption = dubbing.captionId?.let { id -> placedCaptions.find { it.id == id } } ?: return this
    if (caption.length <= 0.0) return this
    return copy(
        dubbings = dubbings.map {
            if (it.id == dubbingId) it.copy(rate = it.rate * it.length / caption.length, length = caption.length) else it
        },
    )
}

// =====================================================================
// Sound effects
// =====================================================================

/**
 * Adds a sound effect at [time]. When it overlaps another effect, it goes to the first free row.
 *
 * @param effect Effect from the catalog.
 * @param time Playhead position.
 * @return The changed project and the ID of the new effect.
 */
fun EditorProject.addSoundEffect(effect: SoundEffect, time: Double): Pair<EditorProject, String> {
    val start = time.coerceIn(0.0, maxOf(0.0, totalDuration - 0.05))
    val clip = SoundEffectClip(
        id = newEditorId(),
        effectId = effect.id,
        name = effect.name,
        start = start,
        length = effect.length,
        layer = soundEffects.firstFreeLayer(start, start + effect.length),
    )
    return copy(soundEffects = soundEffects + clip) to clip.id
}

// =====================================================================
// Shared edits of dubbings and sound effects
// =====================================================================

/** Track of audio items with rows. */
enum class AudioTrack {
    /** DUBBING : The dubbing track. */
    DUBBING,

    /** SOUND_EFFECT : The sound effect track. */
    SOUND_EFFECT,
}

/** Items of the track. */
fun EditorProject.itemsOf(track: AudioTrack): List<TrackItem> = when (track) {
    AudioTrack.DUBBING -> dubbings
    AudioTrack.SOUND_EFFECT -> soundEffects
}

/**
 * Moves an audio item in time. The length stays, and the whole item stays inside the project. The row does not
 * change. Call [finishAudioMove] when the drag ends.
 *
 * @param track Track of the item.
 * @param id ID of the item.
 * @param start New start time.
 */
fun EditorProject.moveAudioItem(track: AudioTrack, id: String, start: Double): EditorProject {
    val total = totalDuration
    fun place(item: TrackItem) = start.coerceIn(0.0, maxOf(0.0, total - minOf(item.length, total)))
    return when (track) {
        AudioTrack.DUBBING -> copy(dubbings = dubbings.map { if (it.id == id) it.copy(start = place(it)) else it })
        AudioTrack.SOUND_EFFECT -> copy(soundEffects = soundEffects.map { if (it.id == id) it.copy(start = place(it)) else it })
    }
}

/**
 * Puts a moved audio item in a free row when it overlaps another item, and removes the empty rows.
 *
 * @param track Track of the item.
 * @param id ID of the item.
 */
fun EditorProject.finishAudioMove(track: AudioTrack, id: String): EditorProject = when (track) {
    AudioTrack.DUBBING -> copy(dubbings = dubbings.relayer(id) { item, layer -> item.copy(layer = layer) })
    AudioTrack.SOUND_EFFECT -> copy(soundEffects = soundEffects.relayer(id) { item, layer -> item.copy(layer = layer) })
}

/**
 * Puts a copy of an audio item right after it. Near the project end, the copy moves back to stay inside the project.
 * The copy goes to the first free row. A dubbing copy has no caption.
 *
 * @param track Track of the item.
 * @param id ID of the item.
 * @return The changed project and the ID of the copy, or null when the item does not exist.
 */
fun EditorProject.duplicateAudioItem(track: AudioTrack, id: String): Pair<EditorProject, String>? {
    val item = itemsOf(track).find { it.id == id } ?: return null
    val start = item.end.coerceAtMost(totalDuration - item.length).coerceAtLeast(0.0)
    val copyId = newEditorId()
    val layer = itemsOf(track).firstFreeLayer(start, start + item.length)
    val project = when (track) {
        AudioTrack.DUBBING -> copy(dubbings = dubbings + (item as Dubbing).copy(id = copyId, start = start, layer = layer, captionId = null))
        AudioTrack.SOUND_EFFECT -> copy(soundEffects = soundEffects + (item as SoundEffectClip).copy(id = copyId, start = start, layer = layer))
    }
    return project to copyId
}

/**
 * Removes an audio item and the empty rows.
 *
 * @param track Track of the item.
 * @param id ID of the item.
 */
fun EditorProject.removeAudioItem(track: AudioTrack, id: String): EditorProject = when (track) {
    AudioTrack.DUBBING -> copy(dubbings = dubbings.filterNot { it.id == id }.compactLayers { item, layer -> item.copy(layer = layer) })
    AudioTrack.SOUND_EFFECT ->
        copy(soundEffects = soundEffects.filterNot { it.id == id }.compactLayers { item, layer -> item.copy(layer = layer) })
}

/**
 * Changes the volume of an audio item.
 *
 * @param track Track of the item.
 * @param id ID of the item.
 * @param volume New volume. The function keeps it from 0 to [MAX_VOLUME].
 */
fun EditorProject.setAudioVolume(track: AudioTrack, id: String, volume: Int): EditorProject {
    val value = volume.coerceIn(0, MAX_VOLUME)
    return when (track) {
        AudioTrack.DUBBING -> copy(dubbings = dubbings.map { if (it.id == id) it.copy(volume = value) else it })
        AudioTrack.SOUND_EFFECT -> copy(soundEffects = soundEffects.map { if (it.id == id) it.copy(volume = value) else it })
    }
}
