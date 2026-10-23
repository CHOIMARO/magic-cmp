package com.company.core.domain.editor

import com.company.core.domain.model.editor.AudioTrack
import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.CaptionSplit
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.ClipTransition
import com.company.core.domain.model.editor.CopiedElement
import com.company.core.domain.model.editor.Decoration
import com.company.core.domain.model.editor.DecorationRange
import com.company.core.domain.model.editor.DubbingAudio
import com.company.core.domain.model.editor.DubbingContent
import com.company.core.domain.model.editor.DubbingSpeed
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.MusicRange
import com.company.core.domain.model.editor.PasteTarget
import com.company.core.domain.model.editor.SoundEffect
import com.company.core.domain.model.editor.SoundEffectCategory
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.TransitionSlot
import com.company.core.domain.model.editor.TransitionType
import com.company.core.domain.model.editor.addCaptionAt
import com.company.core.domain.model.editor.addDubbing
import com.company.core.domain.model.editor.addSoundEffect
import com.company.core.domain.model.editor.afterTextChange
import com.company.core.domain.model.editor.captionsFromScript
import com.company.core.domain.model.editor.copyElement
import com.company.core.domain.model.editor.currentWordIndex
import com.company.core.domain.model.editor.decorate
import com.company.core.domain.model.editor.dubbingOverflow
import com.company.core.domain.model.editor.duplicateAudioItem
import com.company.core.domain.model.editor.finishAudioMove
import com.company.core.domain.model.editor.fitDubbingToCaption
import com.company.core.domain.model.editor.moveAudioItem
import com.company.core.domain.model.editor.moveMusicRange
import com.company.core.domain.model.editor.paste
import com.company.core.domain.model.editor.pasteOptions
import com.company.core.domain.model.editor.placedCaptions
import com.company.core.domain.model.editor.removeAudioItem
import com.company.core.domain.model.editor.removeClip
import com.company.core.domain.model.editor.scriptLines
import com.company.core.domain.model.editor.setTransition
import com.company.core.domain.model.editor.setTransitionForAllBoundaries
import com.company.core.domain.model.editor.splitCaption
import com.company.core.domain.model.editor.transitionLength
import com.company.core.domain.model.editor.transitionWindows
import com.company.core.domain.model.editor.trimCaption
import com.company.core.domain.model.editor.trimClipEnd
import com.company.core.domain.model.editor.trimWith
import com.company.core.domain.model.editor.valuesIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorV7EditsTest {

    private fun clip(id: String, length: Double) = Clip(
        id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, thumbnailHue = 0,
    )

    private val project = EditorProject(name = "p", clips = listOf(clip("a", 4.0), clip("b", 2.0), clip("c", 3.0)))

    // ----- Decorations -----

    @Test
    fun decorate_changesOnlyTheRange_andMergesEqualNeighbors() {
        val ranges = emptyList<DecorationRange>()
            .decorate(length = 6, start = 0, end = 2) { it.copy(bold = true) }
            .decorate(length = 6, start = 2, end = 4) { it.copy(bold = true) }
        assertEquals(listOf(DecorationRange(0, 4, Decoration(bold = true))), ranges)
        val cleared = ranges.decorate(length = 6, start = 0, end = 4) { it.copy(bold = false) }
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun afterTextChange_newLettersTakeThePreviousDecoration_andRemovedLettersLoseIt() {
        val red = Decoration(color = 0xFF0000)
        val ranges = listOf(DecorationRange(0, 2, red))
        assertEquals(listOf(DecorationRange(0, 3, red)), ranges.afterTextChange("ab cd", "abX cd"))
        assertEquals(listOf(DecorationRange(0, 1, red)), ranges.afterTextChange("ab cd", "a cd"))
    }

    @Test
    fun trimWith_movesTheDecorationsWithTheText() {
        val red = Decoration(color = 0xFF0000)
        val (text, ranges) = listOf(DecorationRange(2, 4, red)).trimWith("  ab  ")
        assertEquals("ab", text)
        assertEquals(listOf(DecorationRange(0, 2, red)), ranges)
    }

    @Test
    fun valuesIn_reportsAMixedRange() {
        val ranges = listOf(DecorationRange(0, 1, Decoration(bold = true)))
        assertEquals(setOf(true, false), ranges.valuesIn(3, 0, 3) { it.bold })
        assertEquals(setOf(true), ranges.valuesIn(3, 0, 1) { it.bold })
    }

    // ----- Captions -----

    @Test
    fun captionsFromScript_bindsOneLinePerClip_andFollowsTheClip() {
        val lines = scriptLines(" 첫 줄 \n\n둘째 줄\n셋째\n넷째")
        assertEquals(listOf("첫 줄", "둘째 줄", "셋째", "넷째"), lines)
        val withCaptions = project.copy(captions = project.captionsFromScript(lines))
        assertEquals(3, withCaptions.captions.size)
        val placed = withCaptions.placedCaptions
        assertEquals(listOf(0.0, 4.0, 6.0), placed.map { it.start })
        assertEquals(listOf(4.0, 2.0, 3.0), placed.map { it.length })
        // 클립 길이를 바꾸면 자막도 따라간다.
        val trimmed = withCaptions.trimClipEnd("a", 3.0).placedCaptions
        assertEquals(listOf(0.0, 3.0, 5.0), trimmed.map { it.start })
        // 클립을 지우면 그 클립의 자막도 없어진다.
        assertEquals(2, withCaptions.removeClip("b").placedCaptions.size)
    }

    @Test
    fun currentWordIndex_usesTheCharacterCount() {
        val caption = CaptionSegment(id = "k", text = "가 나다라", start = 0.0, length = 4.0)
        assertEquals(0, caption.currentWordIndex(0.5))
        assertEquals(1, caption.currentWordIndex(1.1))
    }

    @Test
    fun addCaptionAt_stopsAtTheNextCaption_andRefusesAnOccupiedTime() {
        val withCaption = project.copy(captions = listOf(CaptionSegment("k", "자막", start = 2.0, length = 1.0)))
        val (added, id) = assertNotNull(withCaption.addCaptionAt(1.0, "새 자막"))
        assertEquals(1.0, added.captions.first { it.id == id }.length)
        assertNull(withCaption.addCaptionAt(2.5, "새 자막"))
        assertNull(withCaption.addCaptionAt(1.8, "새 자막"))
    }

    @Test
    fun splitCaption_splitsTheWordsByTime() {
        val withCaption = project.copy(captions = listOf(CaptionSegment("k", "하나 둘", start = 0.0, length = 4.0)))
        val result = assertIs<CaptionSplit.Done>(withCaption.splitCaption("k", 2.0))
        assertEquals(listOf("하나", "둘"), result.project.captions.map { it.text })
        assertEquals(listOf(2.0, 2.0), result.project.captions.map { it.length })
        assertIs<CaptionSplit.OutsideCaption>(withCaption.splitCaption("k", 0.1))
        val oneWord = project.copy(captions = listOf(CaptionSegment("k", "하나", start = 0.0, length = 4.0)))
        assertIs<CaptionSplit.OneWord>(oneWord.splitCaption("k", 2.0))
    }

    @Test
    fun trimCaption_stopsAtNeighbors_andEndsTheBinding() {
        val withCaptions = project.copy(captions = project.captionsFromScript(listOf("a", "b")))
        val firstId = withCaptions.captions[0].id
        val trimmed = withCaptions.trimCaption(firstId, start = false, time = 5.0)
        val first = trimmed.captions.first { it.id == firstId }
        assertNull(first.clipId)
        assertEquals(4.0, first.end)
        assertEquals(3.7, withCaptions.trimCaption(firstId, start = true, time = 3.9).captions[0].start, 1e-9)
    }

    @Test
    fun placedCaptions_cutAtTheProjectEnd_andKeepTheStoredTimes() {
        val withCaptions = project.copy(
            captions = listOf(
                CaptionSegment("k", "자막", start = 5.5, length = 3.0),
                CaptionSegment("m", "끝 뒤", start = 8.5, length = 0.5),
            ),
        )
        // 마지막 클립을 줄이면 프로젝트가 7.5초에서 끝난다.
        val shorter = withCaptions.trimClipEnd("c", 1.5)
        assertEquals(listOf("k"), shorter.placedCaptions.map { it.id })
        assertEquals(7.5, shorter.placedCaptions.single().end, 1e-9)
        assertEquals(withCaptions.captions, shorter.captions)
        // 영상을 다시 늘리면 자막 길이도 돌아온다.
        assertEquals(8.5, shorter.trimClipEnd("c", 3.0).placedCaptions.first().end, 1e-9)
        // 잘린 끝에서 끝을 당기면 바로 짧아진다.
        assertEquals(7.0, shorter.trimCaption("k", start = false, time = 7.0).captions.first { it.id == "k" }.end, 1e-9)
    }

    // ----- Transitions -----

    @Test
    fun transitionWindows_centerOnTheBoundary_andShortClipsShortenThem() {
        val dissolve = ClipTransition(TransitionType.DISSOLVE)
        val withTransition = project.setTransition(TransitionSlot.Boundary("b"), dissolve)
        val window = withTransition.transitionWindows().single()
        assertEquals(3.75, window.start)
        assertEquals(4.25, window.end)
        val short = EditorProject(name = "p", clips = listOf(clip("a", 0.4), clip("b", 2.0)))
        assertEquals(0.2, short.transitionLength(1))
        // 첫 클립 앞에는 경계가 없다. 영상 시작에는 시작 전환만 쓸 수 있다.
        assertEquals(project, project.setTransition(TransitionSlot.Boundary("a"), dissolve))
        assertNull(project.setTransition(TransitionSlot.Opening, dissolve).openingTransition)
    }

    @Test
    fun removeClip_removesTheTransitionsAtItsEdges() {
        val fade = ClipTransition(TransitionType.FADE)
        val all = project.setTransitionForAllBoundaries(fade)
        assertEquals(setOf("b", "c"), all.transitions.keys)
        assertTrue(all.removeClip("b").transitions.isEmpty())
    }

    // ----- Paste -----

    @Test
    fun paste_keepsTheOffsetAndTheLength_andSkipsShortClips() {
        val text = TextOverlay(id = "t", text = "제목", start = 0.5, length = 1.0, style = TextStyleType.BOLD, x = 50.0, y = 30.0)
        val withText = project.copy(texts = listOf(text))
        val clipboard = assertNotNull(withText.copyElement(CopiedElement.Text(text), text.start, text.length))
        val options = assertNotNull(withText.pasteOptions(clipboard, time = 0.0))
        assertEquals(listOf(1), options.indexesOf(PasteTarget.NEXT))
        assertEquals(listOf(1, 2), options.indexesOf(PasteTarget.ALL))
        val result = withText.paste(clipboard, options.indexesOf(PasteTarget.ALL), replaceCaptions = false)
        val pasted = result.project.texts.filter { it.id != "t" }
        assertEquals(listOf(4.5, 6.5), pasted.map { it.start })
        assertTrue(pasted.all { it.length == 1.0 && it.y == 30.0 })
        val short = EditorProject(name = "p", clips = listOf(clip("a", 4.0), clip("b", 0.8)), texts = listOf(text))
        assertEquals(1, short.paste(clipboard, listOf(1), replaceCaptions = false).shortClips)
    }

    @Test
    fun paste_captionCoversTheWholeClip_andSkipsClipsWithCaptions() {
        val withCaptions = project.copy(captions = project.captionsFromScript(listOf("첫", "둘")))
        val first = withCaptions.placedCaptions.first()
        val clipboard = assertNotNull(withCaptions.copyElement(CopiedElement.Caption(first.text, emptyList()), first.start, first.length))
        assertTrue(clipboard.coversClip)
        val result = withCaptions.paste(clipboard, listOf(1, 2), replaceCaptions = false)
        assertEquals(1, result.clipsWithCaptions)
        val third = result.project.placedCaptions.last()
        assertEquals(6.0, third.start)
        assertEquals(3.0, third.length)
    }

    // ----- Audio -----

    @Test
    fun dubbings_useFreeRows_andFitTheirCaption() {
        val withCaption = project.copy(captions = listOf(CaptionSegment("k", "안녕", start = 1.0, length = 1.0)))
        val content = DubbingContent("안녕하세요", voiceId = "v1", speed = DubbingSpeed.NORMAL)
        val (first, id) = withCaption.addDubbing(content, DubbingAudio(1.5, null), time = 0.0, captionId = "k")
        val dubbing = first.dubbings.single()
        assertEquals(1.0, dubbing.start)
        assertEquals(0.5, first.dubbingOverflow(dubbing), 1e-9)
        val (second, _) = first.addDubbing(content, DubbingAudio(1.0, null), time = 1.2, captionId = null)
        assertEquals(listOf(0, 1), second.dubbings.map { it.layer })
        val fitted = first.fitDubbingToCaption(id).dubbings.single()
        assertEquals(1.0, fitted.length)
        assertEquals(1.5, fitted.rate, 1e-9)
    }

    @Test
    fun soundEffects_move_duplicate_andRemove() {
        val effect = SoundEffect(id = "s1", category = SoundEffectCategory.HIGHLIGHT, name = "띠링", length = 0.6)
        val (added, id) = project.addSoundEffect(effect, time = 1.0)
        val (copied, copyId) = assertNotNull(added.duplicateAudioItem(AudioTrack.SOUND_EFFECT, id))
        assertEquals(1.6, copied.soundEffects.first { it.id == copyId }.start, 1e-9)
        val moved = copied.moveAudioItem(AudioTrack.SOUND_EFFECT, copyId, 1.2).finishAudioMove(AudioTrack.SOUND_EFFECT, copyId)
        assertEquals(1, moved.soundEffects.first { it.id == copyId }.layer)
        val removed = moved.removeAudioItem(AudioTrack.SOUND_EFFECT, id)
        assertEquals(0, removed.soundEffects.single().layer)
    }

    @Test
    fun moveMusicRange_keepsTheRangeInsideTheTrack() {
        val range = MusicRange(4.0, 10.0)
        assertEquals(MusicRange(24.0, 30.0), moveMusicRange(30.0, range, 100.0, moveStart = true, moveEnd = true))
        assertEquals(MusicRange(7.0, 10.0), moveMusicRange(30.0, range, 5.0, moveStart = true, moveEnd = false))
        assertEquals(MusicRange(4.0, 30.0), moveMusicRange(30.0, range, 50.0, moveStart = false, moveEnd = true))
    }
}
