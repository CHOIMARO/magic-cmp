package com.company.feature.editor.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    @Test
    fun stageCenterGuide_snapsOnlyInsideTheDistance_onEachAxis() {
        // 무대 폭의 2%, 높이의 1% 안이면 그 축에 붙는다.
        assertEquals(TextSnapGuide(vertical = true, horizontal = false), stageCenterGuide(x = 51.9, y = 40.0, snapX = 2.0, snapY = 1.0))
        assertEquals(TextSnapGuide(vertical = false, horizontal = true), stageCenterGuide(x = 52.0, y = 49.1, snapX = 2.0, snapY = 1.0))
        assertEquals(TextSnapGuide(vertical = true, horizontal = true), stageCenterGuide(x = 50.0, y = 50.0, snapX = 2.0, snapY = 1.0))
        assertEquals(TextSnapGuide(), stageCenterGuide(x = 30.0, y = 70.0, snapX = 2.0, snapY = 1.0))
    }

    @Test
    fun textSnapGuide_vibratesOnlyForANewLine() {
        val vertical = TextSnapGuide(vertical = true)
        val both = TextSnapGuide(vertical = true, horizontal = true)
        assertTrue(vertical.hasNewLine(TextSnapGuide()))
        assertTrue(both.hasNewLine(vertical))
        // 이미 붙어 있던 축에 머물거나 축에서 떨어지면 진동하지 않는다.
        assertFalse(vertical.hasNewLine(vertical))
        assertFalse(vertical.hasNewLine(both))
        assertFalse(TextSnapGuide().hasNewLine(both))
    }
}
