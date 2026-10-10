package com.company.feature.editor.preview

import com.company.core.domain.model.editor.CaptionSegment
import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.MediaType
import com.company.core.domain.model.editor.MusicTrack
import com.company.core.domain.model.editor.PHOTO_CLIP_DEFAULT_LENGTH
import com.company.core.domain.model.editor.PHOTO_CLIP_MAX_LENGTH
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe

/** Sample data for the `@Preview` functions of the editor feature. */
internal object EditorPreviewData {

    /** Project with two video clips and one photo clip. */
    val project = EditorProject(
        name = "바닷가 주말",
        clips = listOf(
            Clip(id = "c1", name = "VID_4021", sourceDuration = 4.2, inPoint = 0.4, outPoint = 3.8, thumbnailHue = 35),
            Clip(id = "c2", name = "VID_4028", sourceDuration = 6.8, inPoint = 0.4, outPoint = 3.8, speed = 2.0, thumbnailHue = 200),
            Clip(
                id = "c3",
                name = "IMG_0012",
                sourceDuration = PHOTO_CLIP_MAX_LENGTH,
                inPoint = 0.0,
                outPoint = PHOTO_CLIP_DEFAULT_LENGTH,
                thumbnailHue = 150,
                type = MediaType.PHOTO,
            ),
        ),
        captionStyle = CaptionStyleType.POP,
        texts = listOf(
            TextOverlay(id = "t1", text = "바닷가 주말", start = 0.2, length = 2.8, style = TextStyleType.BOX, x = 50.0, y = 24.0),
        ),
        musicId = "m5",
        filter = FilterType.WARM,
        vibe = Vibe.WARM,
        captions = listOf(
            CaptionSegment(id = "k1", text = "자 오늘은", start = 0.0, length = 1.4),
            CaptionSegment(id = "k2", text = "바닷가로 떠나볼게요", start = 1.5, length = 1.6),
        ),
    )

    /** Background music tracks. */
    val musicTracks = listOf(
        MusicTrack(id = "m1", title = "한밤의 드라이브", mood = "몽환", durationSeconds = 42, thumbnailHue = 250),
        MusicTrack(id = "m2", title = "파도 소리", mood = "청량", durationSeconds = 30, thumbnailHue = 200),
        MusicTrack(id = "m3", title = "빅 룸", mood = "신나는", durationSeconds = 24, thumbnailHue = 35),
        MusicTrack(id = "m4", title = "종이 등불", mood = "감성 로파이", durationSeconds = 65, thumbnailHue = 300),
        MusicTrack(id = "m5", title = "일요일 버스", mood = "따뜻한 어쿠스틱", durationSeconds = 51, thumbnailHue = 80),
    )
}
