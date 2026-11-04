package com.company.core.media

import com.company.domain.model.editor.Dubbing
import com.company.domain.model.editor.DubbingSpeed
import kotlin.test.Test
import kotlin.test.assertEquals

/** Dubbings in the sound of the preview and the export (F05). */
class TimelineAudioTest {
    private fun dubbing(id: String, start: Double, layer: Int = 0, audioUri: String? = "file:///$id.mp3") = Dubbing(
        id = id, text = id, voiceId = "SEOHYEON", speed = DubbingSpeed.NORMAL, start = start, length = 2.0, layer = layer,
        volume = 50, rate = 1.25, audioUri = audioUri,
    )

    @Test
    fun dubbingWithoutFile_staysSilent() {
        val audio = listOf(dubbing("a", 0.0), dubbing("b", 3.0, audioUri = null)).timelineAudio()

        assertEquals(listOf("file:///a.mp3"), audio.map { it.uri })
    }

    @Test
    fun fittedDubbing_playsTheWholeFileFaster() {
        // 자막에 맞춘 더빙은 파일 전체(길이 × 배속)를 빠르게 재생한다.
        val item = listOf(dubbing("a", 1.0)).timelineAudio().single()

        assertEquals(2.5, item.sourceLength)
        assertEquals(0.5f, item.gain)
    }

    @Test
    fun rows_areInLayerOrder_andTimeOrder() {
        val rows = listOf(dubbing("c", 5.0, layer = 1), dubbing("b", 4.0), dubbing("a", 0.0)).timelineAudio().byLayer()

        assertEquals(listOf(listOf("file:///a.mp3", "file:///b.mp3"), listOf("file:///c.mp3")), rows.map { row -> row.map { it.uri } })
    }
}
