package com.company.feature.editor.export

import com.company.domain.model.editor.CaptionSegment
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.LayoutType
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayTimelineTest {

    private fun clip(id: String, length: Double) =
        Clip(id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, thumbnailHue = 0, sourceUri = "file://$id")

    private val project = EditorProject(name = "p", clips = listOf(clip("a", 2.0), clip("b", 3.0)))

    private fun assertNear(expected: Double, actual: Double) = assertTrue(abs(expected - actual) < 1e-6, "expected $expected but was $actual")

    /** Checks that the spans touch each other from [start] to [end]. */
    private fun assertCovers(spans: List<OverlaySpan>, start: Double, end: Double) {
        assertNear(start, spans.first().start)
        assertNear(end, spans.last().end)
        spans.zipWithNext().forEach { (a, b) -> assertNear(a.end, b.start) }
        spans.forEach { assertTrue(it.time >= it.start && it.time < it.end) }
    }

    @Test
    fun noOverlay_givesNoSpans() {
        assertTrue(project.textSpans(watermark = false).isEmpty())
        assertTrue(project.layoutSpans().isEmpty())
    }

    @Test
    fun watermark_coversTheWholeTimelineWithOneSpan() {
        val spans = project.textSpans(watermark = true)
        assertEquals(1, spans.size)
        assertCovers(spans, 0.0, 5.0)
    }

    @Test
    fun text_givesOneSpanPerAnimationFrameAndOneStaticSpan() {
        // 굵게: 팝 350ms 등장, 페이드 200ms 퇴장.
        val text = TextOverlay(id = "t", text = "hi", start = 0.5, length = 2.0, style = TextStyleType.BOLD, x = 50.0, y = 50.0)
        val spans = project.copy(texts = listOf(text)).textSpans(watermark = false)
        assertCovers(spans, 0.5, 2.5)
        val static = spans.single { it.end - it.start > 0.5 }
        assertNear(0.85, static.start)
        assertNear(2.3, static.end)
        assertEquals(11, spans.count { it.end <= 0.85 + 1e-9 })
        assertEquals(6, spans.count { it.start >= 2.3 - 1e-9 })
    }

    @Test
    fun caption_changesAtTheNextWord() {
        val caption = CaptionSegment(id = "c", text = "one two", start = 1.0, length = 2.0)
        val spans = project.copy(captions = listOf(caption)).textSpans(watermark = false)
        assertCovers(spans, 1.0, 3.0)
        assertTrue(spans.any { abs(it.start - 2.0) < 1e-6 })
    }

    @Test
    fun splitLayout_givesOneLayoutSpanForEachClip() {
        val spans = project.copy(layout = LayoutType.SPLIT_VERTICAL).layoutSpans()
        assertEquals(listOf(0.0 to 2.0, 2.0 to 5.0), spans.map { it.start to it.end })
    }
}
