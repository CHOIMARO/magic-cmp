package com.company.feature.editor.export

import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayTimelineTest {

    private fun clip(id: String, length: Double) =
        Clip(id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, thumbnailHue = 0, sourceUri = "file://$id")

    private val project = EditorProject(name = "p", clips = listOf(clip("a", 2.0), clip("b", 3.0)))

    @Test
    fun noOverlay_givesNoSpans() {
        assertTrue(project.overlaySpans(watermark = false).isEmpty())
    }

    @Test
    fun watermark_coversTheWholeTimeline() {
        val spans = project.overlaySpans(watermark = true)
        assertEquals(listOf(0.0 to 5.0), spans.map { it.start to it.end })
    }

    @Test
    fun text_givesOneSpanForItsTimeRange() {
        val text = TextOverlay(id = "t", text = "hi", start = 0.5, length = 2.0, style = TextStyleType.BOLD, x = 50.0, y = 50.0)
        val spans = project.copy(texts = listOf(text)).overlaySpans(watermark = false)
        assertEquals(listOf(0.5 to 2.5), spans.map { it.start to it.end })
        assertTrue(spans.single().time in 0.5..2.5)
    }

    @Test
    fun caption_givesOneSpanForEachSpokenWord() {
        val caption = CaptionSegment(id = "c", text = "one two", start = 1.0, length = 2.0)
        val spans = project.copy(captions = listOf(caption)).overlaySpans(watermark = false)
        assertEquals(listOf(1.0 to 2.0, 2.0 to 3.0), spans.map { it.start to it.end })
    }

    @Test
    fun splitLayout_givesOneSpanForEachClip() {
        val spans = project.copy(layout = LayoutType.SPLIT_VERTICAL).overlaySpans(watermark = false)
        assertEquals(listOf(0.0 to 2.0, 2.0 to 5.0), spans.map { it.start to it.end })
    }
}
