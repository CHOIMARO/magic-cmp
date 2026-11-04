package com.company.core.media

import com.company.domain.model.editor.Dubbing
import com.company.domain.model.editor.MAX_VOLUME

/**
 * Audio file that plays on the timeline of the video, for example the audio of a dubbing. The preview player and
 * the export mix it into the sound of the clips.
 *
 * @property uri Location of the audio file.
 * @property start Start on the timeline in seconds.
 * @property length Length on the timeline in seconds.
 * @property rate Play rate. A value above 1 plays faster. The audio file is [length] × [rate] seconds long.
 * @property volume Volume from 0 to 100.
 * @property layer Row of the item. Items in one row do not overlap in time.
 */
data class TimelineAudio(
    val uri: String,
    val start: Double,
    val length: Double,
    val rate: Double,
    val volume: Int,
    val layer: Int,
) {
    /** Part of the audio file that plays, in seconds from the start of the file. */
    val sourceLength: Double
        get() = length * rate

    /** Volume from 0 to 1. */
    val gain: Float
        get() = volume.coerceIn(0, MAX_VOLUME).toFloat() / MAX_VOLUME
}

/** Dubbings that have an audio file, as timeline audio. A dubbing without a file stays silent. */
fun List<Dubbing>.timelineAudio(): List<TimelineAudio> = mapNotNull { dubbing ->
    val uri = dubbing.audioUri ?: return@mapNotNull null
    if (dubbing.length <= 0.0) return@mapNotNull null
    TimelineAudio(
        uri = uri,
        start = dubbing.start,
        length = dubbing.length,
        rate = dubbing.rate,
        volume = dubbing.volume,
        layer = dubbing.layer,
    )
}

/** Items in rows. Each row is in time order. The rows are in the order of their layer. */
fun List<TimelineAudio>.byLayer(): List<List<TimelineAudio>> =
    groupBy { it.layer }.entries.sortedBy { it.key }.map { (_, row) -> row.sortedBy { it.start } }
