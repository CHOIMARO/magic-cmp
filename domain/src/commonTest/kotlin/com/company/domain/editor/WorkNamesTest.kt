package com.company.domain.editor

import com.company.domain.model.editor.MAX_WORK_NAME_LENGTH
import com.company.domain.model.editor.nextWorkName
import com.company.domain.model.editor.workNameOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Automatic names of new works (D53). */
class WorkNamesTest {

    private val base = "새 작업"

    @Test
    fun firstWork_getsTheBaseName() {
        assertEquals("새 작업", nextWorkName(base, emptyList()))
        assertEquals("새 작업", nextWorkName(base, listOf("바닷가 주말", "새 작업 2")))
    }

    @Test
    fun aRepeatedName_getsTheSmallestFreeNumber() {
        assertEquals("새 작업 2", nextWorkName(base, listOf("새 작업")))
        assertEquals("새 작업 3", nextWorkName(base, listOf("새 작업", "새 작업 2", "새 작업 4")))
        // 지운 작업의 번호는 다시 쓴다.
        assertEquals("새 작업 2", nextWorkName(base, listOf("새 작업", "새 작업 3")))
    }

    @Test
    fun onlyTheBaseAndItsNumbersCount() {
        // 예전 이름, 다른 글자가 붙은 이름, 0으로 시작하는 번호, 1번은 자동 이름이 아니다.
        val names = listOf("새 작업", "새 영상", "새 촬영 2", "새 작업 2개", "새 작업 02", "새 작업 1", "새 작업  2", "새 작업 ٣")
        assertEquals("새 작업 2", nextWorkName(base, names))
    }

    @Test
    fun workNameOf_trimsTheInput_andRefusesAnEmptyName() {
        assertEquals("바닷가 주말", workNameOf("  바닷가 주말  "))
        // 붙여 넣은 줄바꿈은 띄어쓰기가 된다.
        assertEquals("바닷가 주말", workNameOf("바닷가\n주말"))
        assertNull(workNameOf("   "))
        assertNull(workNameOf("\n"))
        assertEquals(MAX_WORK_NAME_LENGTH, workNameOf("가".repeat(40))?.length)
    }

    @Test
    fun aHugeNumberThatTheUserTyped_hasNoEffect() {
        val names = listOf("새 작업", "새 작업 2147483647", "새 작업 99999999999999999999999999")
        assertEquals("새 작업 2", nextWorkName(base, names))
    }
}
