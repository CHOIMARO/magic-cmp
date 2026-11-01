package com.company.data.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.CaptionStyleType
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.ClipTransition
import com.company.domain.model.editor.DEFAULT_MUSIC_VOLUME
import com.company.domain.model.editor.DEFAULT_SIZE_STEP
import com.company.domain.model.editor.Decoration
import com.company.domain.model.editor.DecorationRange
import com.company.domain.model.editor.Dubbing
import com.company.domain.model.editor.DubbingSpeed
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.MAX_VOLUME
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.MusicRange
import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.SoundEffectClip
import com.company.domain.model.editor.TextEntrance
import com.company.domain.model.editor.TextExit
import com.company.domain.model.editor.TextFontType
import com.company.domain.model.editor.TextLoop
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.TransitionType
import com.company.domain.model.editor.VideoSummary
import kotlinx.serialization.Serializable

// =====================================================================
// File format of the works (F15). The domain models stay free of the file format.
// Change WORK_FILE_VERSION when a change breaks the reading of old files.
// =====================================================================

/** Version of the work files. */
internal const val WORK_FILE_VERSION = 1

/**
 * List of the works.
 *
 * @property version File version. See [WORK_FILE_VERSION].
 * @property works Works, the work with the most recent change first.
 */
@Serializable
internal data class WorkIndexDto(val version: Int = WORK_FILE_VERSION, val works: List<WorkSummaryDto> = emptyList())

/**
 * One work in the list.
 *
 * @property sample True for a sample work. It opens the sample project until the user changes it.
 */
@Serializable
internal data class WorkSummaryDto(
    val id: String,
    val name: String,
    val duration: Double,
    val thumbnailHue: Int? = null,
    val modifiedAt: Long,
    val exported: Boolean = false,
    val sample: Boolean = false,
)

/**
 * Project file of one work.
 *
 * @property version File version. See [WORK_FILE_VERSION].
 * @property project The project.
 */
@Serializable
internal data class ProjectFileDto(val version: Int = WORK_FILE_VERSION, val project: ProjectDto)

@Serializable
internal data class ProjectDto(
    val name: String,
    val clips: List<ClipDto> = emptyList(),
    val layout: LayoutType = LayoutType.FULL,
    val captionStyle: CaptionStyleType = CaptionStyleType.POP,
    val texts: List<TextDto> = emptyList(),
    val musicId: String? = null,
    val captions: List<CaptionDto> = emptyList(),
    val captionPosition: Double? = null,
    val musicRange: MusicRangeDto? = null,
    val musicFade: Boolean = true,
    val musicVolume: Int = DEFAULT_MUSIC_VOLUME,
    val dubbings: List<DubbingDto> = emptyList(),
    val soundEffects: List<SoundEffectDto> = emptyList(),
    val transitions: Map<String, TransitionDto> = emptyMap(),
    val openingTransition: TransitionDto? = null,
    val workId: String? = null,
)

@Serializable
internal data class ClipDto(
    val id: String,
    val name: String,
    val sourceDuration: Double,
    val inPoint: Double,
    val outPoint: Double,
    val speed: Double = 1.0,
    val thumbnailHue: Int,
    val sourceUri: String? = null,
    val type: MediaType = MediaType.VIDEO,
    val volume: Int = MAX_VOLUME,
)

@Serializable
internal data class TextDto(
    val id: String,
    val text: String,
    val start: Double,
    val length: Double,
    val style: TextStyleType,
    val x: Double,
    val y: Double,
    val layer: Int = 0,
    val font: TextFontType? = null,
    val scale: Double = 1.0,
    val rotation: Double = 0.0,
    val sizeStep: Int = DEFAULT_SIZE_STEP,
    val decorations: List<DecorationRangeDto> = emptyList(),
    val entrance: TextEntrance? = null,
    val entranceMillis: Int? = null,
    val exit: TextExit? = null,
    val exitMillis: Int? = null,
    val loop: TextLoop? = null,
)

@Serializable
internal data class DecorationRangeDto(val start: Int, val end: Int, val decoration: DecorationDto)

@Serializable
internal data class DecorationDto(
    val color: Int? = null,
    val bold: Boolean = false,
    val outline: Boolean = false,
    val shadow: Boolean = false,
    val background: Int? = null,
)

@Serializable
internal data class CaptionDto(
    val id: String,
    val text: String,
    val start: Double,
    val length: Double,
    val decorations: List<DecorationRangeDto> = emptyList(),
    val clipId: String? = null,
    val sourceStart: Double? = null,
    val sourceEnd: Double? = null,
)

@Serializable
internal data class MusicRangeDto(val start: Double, val end: Double)

@Serializable
internal data class DubbingDto(
    val id: String,
    val text: String,
    val voiceId: String,
    val speed: DubbingSpeed,
    val start: Double,
    val length: Double,
    val layer: Int = 0,
    val volume: Int = MAX_VOLUME,
    val captionId: String? = null,
    val rate: Double = 1.0,
    val audioUri: String? = null,
)

@Serializable
internal data class SoundEffectDto(
    val id: String,
    val effectId: String,
    val name: String,
    val start: Double,
    val length: Double,
    val layer: Int = 0,
    val volume: Int = MAX_VOLUME,
)

@Serializable
internal data class TransitionDto(val type: TransitionType, val direction: PushDirection = PushDirection.RIGHT)

// =====================================================================
// Conversions
// =====================================================================

internal fun WorkSummaryDto.toDomain(): VideoSummary =
    VideoSummary(id = id, name = name, duration = duration, thumbnailHue = thumbnailHue, modifiedAt = modifiedAt, exported = exported)

internal fun VideoSummary.toDto(sample: Boolean): WorkSummaryDto = WorkSummaryDto(
    id = id, name = name, duration = duration, thumbnailHue = thumbnailHue, modifiedAt = modifiedAt, exported = exported, sample = sample,
)

internal fun EditorProject.toDto(): ProjectDto = ProjectDto(
    name = name,
    clips = clips.map { it.toDto() },
    layout = layout,
    captionStyle = captionStyle,
    texts = texts.map { it.toDto() },
    musicId = musicId,
    captions = captions.map { it.toDto() },
    captionPosition = captionPosition,
    musicRange = musicRange?.let { MusicRangeDto(it.start, it.end) },
    musicFade = musicFade,
    musicVolume = musicVolume,
    dubbings = dubbings.map { it.toDto() },
    soundEffects = soundEffects.map { it.toDto() },
    transitions = transitions.mapValues { (_, transition) -> TransitionDto(transition.type, transition.direction) },
    openingTransition = openingTransition?.let { TransitionDto(it.type, it.direction) },
    workId = workId,
)

internal fun ProjectDto.toDomain(): EditorProject = EditorProject(
    name = name,
    clips = clips.map { it.toDomain() },
    layout = layout,
    captionStyle = captionStyle,
    texts = texts.map { it.toDomain() },
    musicId = musicId,
    captions = captions.map { it.toDomain() },
    captionPosition = captionPosition,
    musicRange = musicRange?.let { MusicRange(it.start, it.end) },
    musicFade = musicFade,
    musicVolume = musicVolume,
    dubbings = dubbings.map { it.toDomain() },
    soundEffects = soundEffects.map { it.toDomain() },
    transitions = transitions.mapValues { (_, transition) -> ClipTransition(transition.type, transition.direction) },
    openingTransition = openingTransition?.let { ClipTransition(it.type, it.direction) },
    workId = workId,
)

private fun Clip.toDto() = ClipDto(id, name, sourceDuration, inPoint, outPoint, speed, thumbnailHue, sourceUri, type, volume)

private fun ClipDto.toDomain() = Clip(id, name, sourceDuration, inPoint, outPoint, speed, thumbnailHue, sourceUri, type, volume)

private fun TextOverlay.toDto() = TextDto(
    id, text, start, length, style, x, y, layer, font, scale, rotation, sizeStep, decorations.map { it.toDto() },
    entrance, entranceMillis, exit, exitMillis, loop,
)

private fun TextDto.toDomain() = TextOverlay(
    id, text, start, length, style, x, y, layer, font, scale, rotation, sizeStep, decorations.map { it.toDomain() },
    entrance, entranceMillis, exit, exitMillis, loop,
)

private fun DecorationRange.toDto() =
    DecorationRangeDto(start, end, DecorationDto(decoration.color, decoration.bold, decoration.outline, decoration.shadow, decoration.background))

private fun DecorationRangeDto.toDomain() =
    DecorationRange(start, end, Decoration(decoration.color, decoration.bold, decoration.outline, decoration.shadow, decoration.background))

private fun CaptionSegment.toDto() = CaptionDto(id, text, start, length, decorations.map { it.toDto() }, clipId, sourceStart, sourceEnd)

private fun CaptionDto.toDomain() = CaptionSegment(id, text, start, length, decorations.map { it.toDomain() }, clipId, sourceStart, sourceEnd)

private fun Dubbing.toDto() = DubbingDto(id, text, voiceId, speed, start, length, layer, volume, captionId, rate, audioUri)

private fun DubbingDto.toDomain() = Dubbing(id, text, voiceId, speed, start, length, layer, volume, captionId, rate, audioUri)

private fun SoundEffectClip.toDto() = SoundEffectDto(id, effectId, name, start, length, layer, volume)

private fun SoundEffectDto.toDomain() = SoundEffectClip(id, effectId, name, start, length, layer, volume)
