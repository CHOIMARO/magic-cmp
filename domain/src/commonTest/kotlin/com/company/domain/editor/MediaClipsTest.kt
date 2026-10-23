package com.company.domain.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.MediaType
import com.company.domain.model.editor.PHOTO_CLIP_DEFAULT_LENGTH
import com.company.domain.model.editor.PHOTO_CLIP_MAX_LENGTH
import com.company.domain.model.editor.PHOTO_CLIP_MIN_LENGTH
import com.company.domain.model.editor.SourceMedia
import com.company.domain.model.editor.TimelineRange
import com.company.domain.model.editor.setClipSpeed
import com.company.domain.model.editor.speechRanges
import com.company.domain.model.editor.toClip
import com.company.domain.model.editor.trimClipEnd
import com.company.domain.model.editor.trimClipStart
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaClipsTest {

    private val photo = SourceMedia(uri = "p", type = MediaType.PHOTO, name = "p", duration = null, thumbnailHue = 0)
    private val video = SourceMedia(uri = "v", type = MediaType.VIDEO, name = "v", duration = 5.0, thumbnailHue = 0)

    private fun videoClip(id: String, length: Double) = Clip(
        id = id, name = id, sourceDuration = length, inPoint = 0.0, outPoint = length, thumbnailHue = 0,
    )

    @Test
    fun toClip_photoShowsForDefaultLength() {
        val clip = photo.toClip()
        assertEquals(MediaType.PHOTO, clip.type)
        assertEquals(PHOTO_CLIP_DEFAULT_LENGTH, clip.duration)
        assertEquals("p", clip.sourceUri)
    }

    @Test
    fun toClip_videoUsesWholeVideo() {
        val clip = video.toClip()
        assertEquals(MediaType.VIDEO, clip.type)
        assertEquals(5.0, clip.duration)
    }

    @Test
    fun trim_photoKeepsLengthBetweenMinAndMax() {
        val clip = photo.toClip()
        val project = EditorProject(name = "p", clips = listOf(clip))

        assertEquals(PHOTO_CLIP_MIN_LENGTH, project.trimClipEnd(clip.id, 0.0).clips.single().duration, 1e-9)
        assertEquals(PHOTO_CLIP_MAX_LENGTH, project.trimClipEnd(clip.id, 10_000.0).clips.single().duration)
        assertEquals(PHOTO_CLIP_MIN_LENGTH, project.trimClipStart(clip.id, 10.0).clips.single().duration, 1e-9)
    }

    @Test
    fun trim_videoKeepsHalfSecondMinimum() {
        val clip = videoClip("a", 4.0)
        val project = EditorProject(name = "p", clips = listOf(clip))

        assertEquals(0.5, project.trimClipEnd(clip.id, 0.0).clips.single().duration)
    }

    @Test
    fun setClipSpeed_photoKeepsNormalSpeed() {
        val clip = photo.toClip()
        val project = EditorProject(name = "p", clips = listOf(clip))

        assertEquals(1.0, project.setClipSpeed(clip.id, 2.0).clips.single().speed)
    }

    @Test
    fun speechRanges_skipPhotosAndJoinTouchingVideos() {
        val clips = listOf(videoClip("a", 3.0), videoClip("b", 1.0), photo.toClip(), videoClip("c", 2.0))

        assertEquals(listOf(TimelineRange(0.0, 4.0), TimelineRange(6.0, 8.0)), clips.speechRanges())
    }
}
