package com.company.feature.editor.editor

import com.company.core.domain.model.editor.EditorProject
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
 *
 * @param target Dragged part.
 * @param originStart Start of a dragged text when the drag started. A clip drag does not use it.
 * @param originEnd End of a dragged text when the drag started. A clip drag does not use it.
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
    }
}

/** Start time of a clip on the timeline. */
private fun EditorProject.clipStartTime(clipId: String): Double = clips.takeWhile { it.id != clipId }.sumOf { it.duration }
