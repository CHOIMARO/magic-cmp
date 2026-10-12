package com.company.feature.editor.editor

import kotlin.math.floor
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilmstripFrameTimeTest {

    @Test
    fun frameTime_isOnPowerOfTwoGrid() {
        // 한 칸이 0.72초이면 격자는 0.5초다. 0번 칸의 가운데 0.36초는 0.5초에 맞춘다.
        assertEquals(0.5, filmstripFrameTime(index = 0, frameSeconds = 0.72))
        assertEquals(1.0, filmstripFrameTime(index = 1, frameSeconds = 0.72))
        // 최대로 확대한 30 fps 한 프레임(1/30초)은 1/32초 격자를 쓴다.
        val grid = 2.0.pow(-5)
        val steps = filmstripFrameTime(index = 10, frameSeconds = 1.0 / 30) / grid
        assertEquals(floor(steps), steps)
    }

    @Test
    fun neighborFrames_neverShareTime() {
        // 칸 길이가 격자 간격과 정확히 같은 경우(0.5초, 1초, 2초)도 확인한다.
        for (frameSeconds in listOf(1.0 / 30, 0.05, 0.3, 0.5, 0.72, 0.99, 1.0, 1.7, 2.0, 3.6)) {
            val times = (0 until 200).map { filmstripFrameTime(it, frameSeconds) }
            assertEquals(times.size, times.distinct().size, "frameSeconds=$frameSeconds")
        }
    }

    @Test
    fun nearZoomLevels_reuseMostFrames() {
        // 0.72초 칸과 0.8초 칸은 같은 0.5초 격자를 쓰므로, 0.8초 칸의 프레임 대부분은 이미 읽은 프레임이다.
        val loaded = (0 until 300).map { filmstripFrameTime(it, 0.72) }.toSet()
        val next = (0 until 260).map { filmstripFrameTime(it, 0.8) }
        val reused = next.count { it in loaded }.toDouble() / next.size
        assertTrue(reused >= 0.7, "reused=$reused")
    }
}
