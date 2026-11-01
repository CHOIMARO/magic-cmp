package com.company.domain.editor

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.boundTo
import com.company.domain.model.editor.duplicateClip
import com.company.domain.model.editor.placedCaptions
import com.company.domain.model.editor.removeClip
import com.company.domain.model.editor.setClipSpeed
import com.company.domain.model.editor.splitAt
import com.company.domain.model.editor.trimClipStart
import com.company.domain.repository.CaptionRepository
import com.company.domain.usecase.editor.generateSpeechCaptions
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Speech captions that are bound to their clips (D47). */
class CaptionBindingTest {

    private fun clip(id: String, length: Double, type: MediaType = MediaType.VIDEO) = Clip(
        id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, thumbnailHue = 0, type = type,
    )

    /** Clip a (0~4 s) and clip b (4~6 s). The caption says words at 1~2 s of clip a. */
    private val project = EditorProject(name = "p", clips = listOf(clip("a", 4.0), clip("b", 2.0))).let {
        it.copy(captions = listOf(CaptionSegment("k", "안녕", start = 1.0, length = 1.0).boundTo(it.clips[0], clipStart = 0.0)))
    }

    private fun EditorProject.caption() = placedCaptions.single()

    /** Runs a suspend function that never suspends, without a coroutine library. */
    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return assertNotNull(result).getOrThrow()
    }

    @Test
    fun boundCaption_followsATrimOfTheClipStart() {
        // 앞을 0.5초 자르면 말소리가 0.5초 앞당겨지므로 자막도 따라간다.
        val trimmed = project.trimClipStart("a", 0.5).caption()
        assertEquals(0.5, trimmed.start, 1e-9)
        assertEquals(1.0, trimmed.length, 1e-9)
        // 말소리 부분을 모두 잘라 내면 자막도 보이지 않는다.
        assertTrue(project.trimClipStart("a", 2.5).placedCaptions.isEmpty())
    }

    @Test
    fun boundCaption_followsTheClipSpeed() {
        val faster = project.setClipSpeed("a", 2.0).caption()
        assertEquals(0.5, faster.start, 1e-9)
        assertEquals(0.5, faster.length, 1e-9)
    }

    @Test
    fun boundCaption_followsTheClipToANewPlace_andGoesAwayWithIt() {
        // 앞 클립 a를 지우면 b가 0초로 오지만, 자막은 a에 묶여 있으므로 함께 없어진다.
        assertTrue(project.removeClip("a").placedCaptions.isEmpty())
        val withB = project.copy(
            captions = listOf(CaptionSegment("m", "b", start = 4.5, length = 1.0).boundTo(project.clips[1], clipStart = 4.0)),
        )
        assertEquals(0.5, withB.removeClip("a").caption().start, 1e-9)
    }

    @Test
    fun splitAt_givesTheCaptionToThePartWithItsSpeech() {
        val split = assertNotNull(project.splitAt(0.5))
        val second = split.clips[1]
        assertEquals(second.id, split.captions.single().clipId)
        assertEquals(1.0, split.caption().start, 1e-9)
    }

    @Test
    fun duplicateClip_copiesTheCaptionsOfTheClip() {
        val (duplicated, copyId) = assertNotNull(project.duplicateClip("a"))
        assertEquals(2, duplicated.captions.size)
        assertEquals(copyId, duplicated.captions.last().clipId)
        // 복제본은 4~8초에 있으므로 자막은 5~6초다.
        assertEquals(listOf(1.0, 5.0), duplicated.placedCaptions.map { it.start })
    }

    @Test
    fun generateSpeechCaptions_bindsEachCaptionToItsVideoClip() {
        val repository = object : CaptionRepository {
            override suspend fun generateCaptions(totalDuration: Double): List<CaptionSegment> =
                listOf(CaptionSegment("x", "말", start = 0.0, length = totalDuration / 2))
        }
        val clips = listOf(clip("a", 4.0), clip("p", 2.0, MediaType.PHOTO), clip("c", 3.0))
        val captions = runSuspend { repository.generateSpeechCaptions(clips, from = 1.0) }
        // 사진 클립에는 말소리가 없다. 영상 클립마다 따로 만들고 그 클립에 묶는다.
        assertEquals(listOf("a", "c"), captions.map { it.clipId })
        assertEquals(listOf(1.0, 6.0), captions.map { it.start })
        assertEquals(1.0, assertNotNull(captions[0].sourceStart), 1e-9)
        // 앞 클립을 줄이면 뒤 클립의 자막도 그 클립을 따라 앞당겨진다.
        val trimmed = EditorProject(name = "p", clips = clips, captions = captions).trimClipStart("a", 1.0)
        assertEquals(listOf(0.0, 5.0), trimmed.placedCaptions.map { it.start })
    }
}
