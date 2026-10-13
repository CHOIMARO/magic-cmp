package com.company.feature.editor.editor

import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.clipBoundaries
import com.company.core.domain.model.editor.itemsOf
import com.company.core.domain.model.editor.moveAudioItem
import com.company.core.domain.model.editor.moveTextTime
import com.company.core.domain.model.editor.placedCaptions
import com.company.core.domain.model.editor.trimCaption
import com.company.core.domain.model.editor.trimClipEnd
import com.company.core.domain.model.editor.trimClipStart
import com.company.core.domain.model.editor.trimTextEnd
import com.company.core.domain.model.editor.trimTextStart
import kotlin.math.abs

/**
 * One edge of a dragged item and the times that the edge snaps to.
 *
 * @property time Position of the edge in timeline seconds.
 * @property targets Times that the edge snaps to, for example the playhead or the start of another text.
 */
internal data class SnapEdge(val time: Double, val targets: List<Double>)

/**
 * Finds the move that puts one of the edges exactly on its nearest target, like a magnet.
 *
 * @param edges Dragged edges with their targets.
 * @param threshold Largest distance in seconds that snaps.
 * @return Seconds to add to the drag, or null when no target is nearer than [threshold].
 */
internal fun snapOffset(edges: List<SnapEdge>, threshold: Double): Double? =
    edges.flatMap { edge -> edge.targets.map { it - edge.time } }
        .filter { abs(it) < threshold }
        .minByOrNull { abs(it) }

/**
 * Edges of a dragged part of the timeline and the times that they snap to.
 *
 * Each edge snaps to the playhead. A text start also snaps to the start and the end of each other text in any row,
 * so that one text can start together with another text or right after it. A clip end does not snap.
 * A caption edge and a dubbing or sound effect also snap to the clip boundaries, and the start of a dubbing or a
 * sound effect also snaps to the start of each text.
 *
 * @param target Dragged part.
 * @param originStart Start of a dragged item when the drag started. A clip drag does not use it.
 * @param originEnd End of a dragged item when the drag started. A clip drag does not use it.
 * @param delta Requested movement in timeline seconds.
 * @param playhead Time that the playhead line points to.
 */
internal fun EditorProject.timelineSnapEdges(
    target: TimelineDragTarget,
    originStart: Double,
    originEnd: Double,
    delta: Double,
    playhead: Double,
): List<SnapEdge> {
    val otherEdges = { textId: String -> texts.filter { it.id != textId }.flatMap { listOf(it.start, it.end) } }
    return when (target) {
        is TimelineDragTarget.ClipEdge -> when (target.edge) {
            // 앞 가장자리가 재생선에 닿는 조건: 재생 위치(시작 시간 + 스크롤 - 자른 길이)가 클립 시작 시간과 같다.
            // 그래서 자른 길이(delta)를 재생선 시간에서 클립 시작 시간을 뺀 값에 붙인다.
            TrimEdge.START -> listOf(SnapEdge(time = delta, targets = listOf(playhead - clipStartTime(target.clipId))))
            TrimEdge.END -> emptyList()
        }
        is TimelineDragTarget.TextEdge -> when (target.edge) {
            TrimEdge.START -> listOf(SnapEdge(originStart + delta, listOf(playhead) + otherEdges(target.textId)))
            TrimEdge.END -> listOf(SnapEdge(originEnd + delta, listOf(playhead)))
        }
        is TimelineDragTarget.TextMove -> listOf(
            SnapEdge(originStart + delta, listOf(playhead) + otherEdges(target.textId)),
            SnapEdge(originEnd + delta, listOf(playhead)),
        )
        // 자막 가장자리는 재생선과 클립 경계에 붙는다.
        is TimelineDragTarget.CaptionEdge -> {
            val edge = if (target.edge == TrimEdge.START) originStart else originEnd
            listOf(SnapEdge(edge + delta, listOf(playhead) + clipBoundaries()))
        }
        // 더빙·효과음의 시작은 재생선, 클립 경계, 글자 시작에 붙고, 끝은 재생선과 클립 경계에 붙는다.
        is TimelineDragTarget.AudioMove -> listOf(
            SnapEdge(originStart + delta, listOf(playhead) + clipBoundaries() + texts.map { it.start }),
            SnapEdge(originEnd + delta, listOf(playhead) + clipBoundaries()),
        )
    }
}

/** Start time of a clip on the timeline. */
private fun EditorProject.clipStartTime(clipId: String): Double = clips.takeWhile { it.id != clipId }.sumOf { it.duration }

/**
 * Moves the dragged part [delta] timeline seconds from its place at the drag start.
 *
 * @param target Dragged part.
 * @param originStart Start of the item at the drag start. For a clip, it is the source in-point.
 * @param originEnd End of the item at the drag start. For a clip, it is the source out-point.
 * @param delta Requested movement in timeline seconds.
 * @param layer New row of a moved text, or null to keep the row.
 */
internal fun EditorProject.dragTo(
    target: TimelineDragTarget,
    originStart: Double,
    originEnd: Double,
    delta: Double,
    layer: Int?,
): EditorProject = when (target) {
    is TimelineDragTarget.ClipEdge -> {
        // 클립 가장자리는 원본 시간으로 움직이므로 재생 속도를 곱한다.
        val speed = clips.find { it.id == target.clipId }?.speed ?: 1.0
        when (target.edge) {
            TrimEdge.START -> trimClipStart(target.clipId, originStart + delta * speed)
            TrimEdge.END -> trimClipEnd(target.clipId, originEnd + delta * speed)
        }
    }
    is TimelineDragTarget.TextEdge -> when (target.edge) {
        TrimEdge.START -> trimTextStart(target.textId, originStart + delta)
        TrimEdge.END -> trimTextEnd(target.textId, originEnd + delta)
    }
    is TimelineDragTarget.TextMove -> moveTextTime(target.textId, originStart + delta, layer)
    is TimelineDragTarget.CaptionEdge -> when (target.edge) {
        TrimEdge.START -> trimCaption(target.captionId, start = true, time = originStart + delta)
        TrimEdge.END -> trimCaption(target.captionId, start = false, time = originEnd + delta)
    }
    is TimelineDragTarget.AudioMove -> moveAudioItem(target.track, target.id, originStart + delta)
}

/**
 * Timeline seconds that the dragged part moved from its place at the drag start.
 *
 * @param target Dragged part.
 * @param originStart Start of the item at the drag start. For a clip, it is the source in-point.
 * @param originEnd End of the item at the drag start. For a clip, it is the source out-point.
 * @return The distance, or null when the dragged item does not exist.
 */
internal fun EditorProject.draggedDistance(target: TimelineDragTarget, originStart: Double, originEnd: Double): Double? =
    when (target) {
        is TimelineDragTarget.ClipEdge -> clips.find { it.id == target.clipId }?.let { clip ->
            when (target.edge) {
                TrimEdge.START -> (clip.inPoint - originStart) / clip.speed
                TrimEdge.END -> (clip.outPoint - originEnd) / clip.speed
            }
        }
        is TimelineDragTarget.TextEdge -> texts.find { it.id == target.textId }?.let { text ->
            when (target.edge) {
                TrimEdge.START -> text.start - originStart
                TrimEdge.END -> text.end - originEnd
            }
        }
        is TimelineDragTarget.TextMove -> texts.find { it.id == target.textId }?.let { it.start - originStart }
        is TimelineDragTarget.CaptionEdge -> placedCaptions.find { it.id == target.captionId }?.let { caption ->
            when (target.edge) {
                TrimEdge.START -> caption.start - originStart
                TrimEdge.END -> caption.end - originEnd
            }
        }
        is TimelineDragTarget.AudioMove -> itemsOf(target.track).find { it.id == target.id }?.let { it.start - originStart }
    }
