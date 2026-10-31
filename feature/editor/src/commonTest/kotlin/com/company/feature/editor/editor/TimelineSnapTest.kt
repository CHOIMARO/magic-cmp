package com.company.feature.editor.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimelineSnapTest {

    @Test
    fun snapOffset_movesEdgeOntoNearTarget() {
        assertEquals(0.1, snapOffset(listOf(SnapEdge(time = 2.0, targets = listOf(2.1))), threshold = 0.2)!!, 1e-9)
        assertEquals(-0.15, snapOffset(listOf(SnapEdge(time = 2.0, targets = listOf(1.85))), threshold = 0.2)!!, 1e-9)
    }

    @Test
    fun snapOffset_ignoresFarTargets() {
        assertNull(snapOffset(listOf(SnapEdge(time = 2.0, targets = listOf(2.3, 1.5))), threshold = 0.2))
        assertNull(snapOffset(listOf(SnapEdge(time = 2.0, targets = emptyList())), threshold = 0.2))
    }

    @Test
    fun snapOffset_usesNearestTargetOfAllEdges() {
        val edges = listOf(
            // 시작점은 0.15초 떨어진 대상에, 끝점은 0.05초 떨어진 대상에 가깝다.
            SnapEdge(time = 1.0, targets = listOf(1.15)),
            SnapEdge(time = 3.0, targets = listOf(2.95)),
        )
        assertEquals(-0.05, snapOffset(edges, threshold = 0.2)!!, 1e-9)
    }

    /** Project of 10 seconds with text a at 0~3 seconds (row 0) and text b at 5~7 seconds (row 1). */
    private val project = EditorProject(
        name = "p",
        clips = listOf(Clip(id = "c", name = "c", sourceDuration = 10.0, inPoint = 0.0, outPoint = 10.0, thumbnailHue = 0)),
        texts = listOf(
            TextOverlay(id = "a", text = "a", start = 0.0, length = 3.0, style = TextStyleType.IMPACT, x = 50.0, y = 50.0, layer = 0),
            TextOverlay(id = "b", text = "b", start = 5.0, length = 2.0, style = TextStyleType.IMPACT, x = 50.0, y = 50.0, layer = 1),
        ),
    )

    /** Snap of text b, dragged [delta] seconds, with the playhead far away. */
    private fun snapOfB(target: TimelineDragTarget, delta: Double): Double? = snapOffset(
        project.timelineSnapEdges(target, originStart = 5.0, originEnd = 7.0, delta = delta, playhead = 9.5),
        threshold = 0.24,
    )

    @Test
    fun textMove_startSnapsToEndOfOtherText() {
        // b의 시작이 3.1초로 오면 a의 끝(3초)에 붙는다.
        assertEquals(-0.1, snapOfB(TimelineDragTarget.TextMove("b"), delta = -1.9)!!, 1e-9)
    }

    @Test
    fun textMove_startSnapsToStartOfOtherText() {
        // b의 시작이 0.2초로 오면 a의 시작(0초)에 붙는다.
        assertEquals(-0.2, snapOfB(TimelineDragTarget.TextMove("b"), delta = -4.8)!!, 1e-9)
    }

    @Test
    fun textStartTrim_snapsToEndOfOtherText() {
        // b의 앞 손잡이를 3.15초까지 끌면 a의 끝(3초)에 붙는다.
        assertEquals(-0.15, snapOfB(TimelineDragTarget.TextEdge("b", TrimEdge.START), delta = -1.85)!!, 1e-9)
    }

    @Test
    fun textEnd_doesNotSnapToOtherTexts() {
        // b의 끝이 3.1초로 와도 다른 글자에는 붙지 않는다. 끝은 재생선에만 붙는다.
        assertNull(snapOfB(TimelineDragTarget.TextEdge("b", TrimEdge.END), delta = -3.9))
    }
}
