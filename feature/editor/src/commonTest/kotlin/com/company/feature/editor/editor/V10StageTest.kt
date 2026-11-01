package com.company.feature.editor.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class V10StageTest {

    private fun clip(id: String) = Clip(id = id, name = id, sourceDuration = 2.0, inPoint = 0.0, outPoint = 2.0, thumbnailHue = 0)

    private val project = EditorProject(name = "p", clips = listOf(clip("a"), clip("b"), clip("c")))

    @Test
    fun cellClip_showsTheNextClips_andLeavesTheCellsAfterTheLastClipEmpty() {
        assertEquals("a", project.cellClip(clipIndex = 0, cell = 0)?.id)
        assertEquals("c", project.cellClip(clipIndex = 1, cell = 1)?.id)
        // 마지막 클립 뒤의 칸은 처음 클립으로 돌아가지 않고 검게 둔다(F16).
        assertNull(project.cellClip(clipIndex = 2, cell = 1))
        // 클립이 재생되지 않는 시간에는 모든 칸이 검다.
        assertNull(project.cellClip(clipIndex = null, cell = 0))
    }
}
