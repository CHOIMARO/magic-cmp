package com.company.core.domain.model.editor

// =====================================================================
// Copy and paste of texts and captions (F12)
// =====================================================================

/** A copied element covers its whole clip when both edges are this near to the clip edges, in seconds. */
private const val FULL_CLIP_TOLERANCE = 0.05

/** Shortest length of a pasted element, in seconds. A shorter element is not pasted. */
const val MIN_PASTE_LENGTH = 0.5

/** Element that the user copied. It keeps the values of the moment of the copy. */
sealed interface CopiedElement {
    /** Text(overlay) : A text overlay. Only the look, the place, and the animations of [overlay] are used. */
    data class Text(val overlay: TextOverlay) : CopiedElement

    /** Caption(text, decorations) : A caption. The caption style of the project gives its look. */
    data class Caption(val text: String, val decorations: List<DecorationRange>) : CopiedElement
}

/**
 * Clipboard of the editor. It holds one element.
 *
 * @property element The copied element.
 * @property offset Seconds from the start of its clip to the start of the element.
 * @property length Length of the element.
 * @property coversClip True when the element covers its whole clip.
 * @property sourceClipId ID of the clip that the element came from.
 * @property lastClipId ID of the clip of the last paste. The next "다음 구간" paste uses the clip after it.
 */
data class ElementClipboard(
    val element: CopiedElement,
    val offset: Double,
    val length: Double,
    val coversClip: Boolean,
    val sourceClipId: String,
    val lastClipId: String,
)

/**
 * Copies a text overlay or a caption.
 *
 * @param element The copied element.
 * @param start Start time of the element on the timeline.
 * @param length Length of the element.
 * @return The clipboard, or null when the project has no clips.
 */
fun EditorProject.copyElement(element: CopiedElement, start: Double, length: Double): ElementClipboard? {
    if (clips.isEmpty()) return null
    val boundaries = clipBoundaries()
    val index = clips.indices.firstOrNull { start >= boundaries[it] - TIME_EPSILON && start < boundaries[it + 1] - TIME_EPSILON }
        ?: clips.lastIndex
    val clipStart = boundaries[index]
    val clipEnd = boundaries[index + 1]
    val covers = kotlin.math.abs(start - clipStart) < FULL_CLIP_TOLERANCE && kotlin.math.abs(start + length - clipEnd) < FULL_CLIP_TOLERANCE
    return ElementClipboard(
        element = element,
        offset = start - clipStart,
        length = length,
        coversClip = covers,
        sourceClipId = clips[index].id,
        lastClipId = clips[index].id,
    )
}

/** Where a paste puts the element. */
enum class PasteTarget {
    /** NEXT : The clip after the clip of the last paste ("다음 구간"). */
    NEXT,

    /** CURRENT : The clip at the playhead ("지금 구간"). */
    CURRENT,

    /** ALL : All clips except the clip that the element came from ("모든 구간"). */
    ALL,
}

/**
 * Clips that each paste target uses.
 *
 * @property currentIndex Index of the clip at the playhead.
 * @property next Indexes for [PasteTarget.NEXT]. It is empty when no next clip exists.
 * @property current Indexes for [PasteTarget.CURRENT].
 * @property all Indexes for [PasteTarget.ALL]. It is empty when the project has one clip.
 */
data class PasteOptions(val currentIndex: Int, val next: List<Int>, val current: List<Int>, val all: List<Int>) {
    /** Indexes of a target. */
    fun indexesOf(target: PasteTarget): List<Int> = when (target) {
        PasteTarget.NEXT -> next
        PasteTarget.CURRENT -> current
        PasteTarget.ALL -> all
    }
}

/**
 * Finds the clips that each paste target uses.
 *
 * @param clipboard The clipboard.
 * @param time Playhead position.
 * @return The options, or null when the project has no clips.
 */
fun EditorProject.pasteOptions(clipboard: ElementClipboard, time: Double): PasteOptions? {
    if (clips.isEmpty()) return null
    val currentIndex = clipAt(time)?.index ?: 0
    val baseIndex = clips.indexOfFirst { it.id == clipboard.lastClipId }.takeIf { it >= 0 } ?: currentIndex
    val sourceIndex = clips.indexOfFirst { it.id == clipboard.sourceClipId }
    return PasteOptions(
        currentIndex = currentIndex,
        next = if (baseIndex + 1 < clips.size) listOf(baseIndex + 1) else emptyList(),
        current = listOf(currentIndex),
        all = if (clips.size < 2) emptyList() else clips.indices.filter { it != sourceIndex },
    )
}

/**
 * One pasted element.
 *
 * @property clipIndex Index of the clip that got the element.
 * @property id ID of the new text or caption.
 * @property start Start time on the timeline.
 */
data class PastedElement(val clipIndex: Int, val id: String, val start: Double)

/**
 * Result of a paste.
 *
 * @property project The changed project.
 * @property pasted Pasted elements in clip order.
 * @property shortClips Number of clips that were too short.
 * @property clipsWithCaptions Number of clips that kept their captions.
 */
data class PasteResult(val project: EditorProject, val pasted: List<PastedElement>, val shortClips: Int, val clipsWithCaptions: Int)

/**
 * Pastes the element of the clipboard into clips. All pastes of one call are one change.
 *
 * The element keeps its distance from the clip start and its length. An element that covered its whole clip covers
 * each whole target clip. The clip end cuts the element. When the distance is longer than the clip, the element
 * starts at the clip start. A clip that gives less than [MIN_PASTE_LENGTH] is skipped.
 *
 * A text keeps its row when the row is free, or else it goes to the first free row. A caption is bound to its clip.
 *
 * @param clipboard The clipboard.
 * @param clipIndexes Target clips.
 * @param replaceCaptions True to remove the captions of a target clip. False skips a clip that has captions.
 */
fun EditorProject.paste(clipboard: ElementClipboard, clipIndexes: List<Int>, replaceCaptions: Boolean): PasteResult {
    val boundaries = clipBoundaries()
    var texts = texts
    var captions = captions
    val pasted = mutableListOf<PastedElement>()
    var short = 0
    var busy = 0
    clipIndexes.filter { it in clips.indices }.forEach { index ->
        val clip = clips[index]
        val clipLength = clip.duration
        var offset = 0.0
        var length = clipLength
        if (!clipboard.coversClip) {
            offset = if (clipboard.offset >= clipLength) 0.0 else clipboard.offset
            length = minOf(clipboard.length, clipLength - offset)
        }
        if (length < MIN_PASTE_LENGTH - TIME_EPSILON) {
            short++
            return@forEach
        }
        val start = boundaries[index] + offset
        when (val element = clipboard.element) {
            is CopiedElement.Text -> {
                val wanted = element.overlay.layer
                val layer = if (!texts.overlapsLayer(wanted, start, start + length)) wanted else texts.firstFreeLayer(start, start + length)
                val overlay = element.overlay.copy(id = newEditorId(), start = start, length = length, layer = layer)
                texts = texts + overlay
                pasted += PastedElement(index, overlay.id, start)
            }
            is CopiedElement.Caption -> {
                val inClip = copy(captions = captions).captionsInClip(index).map { it.id }.toSet()
                if (inClip.isNotEmpty()) {
                    if (!replaceCaptions) {
                        busy++
                        return@forEach
                    }
                    captions = captions.filterNot { it.id in inClip }
                }
                val caption = CaptionSegment(
                    id = newEditorId(),
                    text = element.text,
                    start = 0.0,
                    length = 0.0,
                    decorations = element.decorations,
                    clipId = clip.id,
                    clipOffset = if (clipboard.coversClip) null else offset,
                    clipLength = if (clipboard.coversClip) null else length,
                )
                captions = captions + caption
                pasted += PastedElement(index, caption.id, start)
            }
        }
    }
    val project = copy(texts = texts, captions = captions).compactTextLayers()
    return PasteResult(project = project, pasted = pasted, shortClips = short, clipsWithCaptions = busy)
}
