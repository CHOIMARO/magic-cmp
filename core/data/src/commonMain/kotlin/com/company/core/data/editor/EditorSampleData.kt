package com.company.core.data.editor

import com.company.core.domain.model.editor.MusicTrack
import com.company.core.domain.model.editor.SourceVideo
import com.company.core.domain.model.editor.VideoSummary

/**
 * Sample content of the prototype.
 *
 * The app has no real gallery, music service, or speech recognition yet.
 * The fake repositories use this data instead.
 */
internal object EditorSampleData {
    private val Hues = listOf(35, 80, 150, 200, 250, 300, 20, 120, 180, 270, 330, 60, 220, 100, 0, 160, 45, 240)
    private val Durations = listOf(4.2, 6.8, 3.1, 8.4, 5.0, 2.6, 7.2, 4.8, 3.6, 9.1, 5.5, 3.9, 6.1, 4.4, 2.9, 5.7, 4.0, 6.4)

    /** Videos in the fake gallery. */
    val SourceVideos: List<SourceVideo> = Hues.mapIndexed { index, hue ->
        SourceVideo(
            id = "v$index",
            name = "VID_${4021 + index * 7}",
            duration = Durations[index],
            thumbnailHue = hue,
        )
    }

    /** Background music tracks. */
    val MusicTracks: List<MusicTrack> = listOf(
        MusicTrack(id = "m1", title = "한밤의 드라이브", mood = "몽환", durationSeconds = 42, thumbnailHue = 250),
        MusicTrack(id = "m2", title = "파도 소리", mood = "청량", durationSeconds = 30, thumbnailHue = 200),
        MusicTrack(id = "m3", title = "빅 룸", mood = "신나는", durationSeconds = 24, thumbnailHue = 35),
        MusicTrack(id = "m4", title = "종이 등불", mood = "감성 로파이", durationSeconds = 65, thumbnailHue = 300),
        MusicTrack(id = "m5", title = "일요일 버스", mood = "따뜻한 어쿠스틱", durationSeconds = 51, thumbnailHue = 80),
    )

    /** Draft projects. */
    val Drafts: List<VideoSummary> = listOf(
        VideoSummary(name = "바닷가 주말", duration = 18.0, thumbnailHue = 35),
        VideoSummary(name = "작업실 투어", duration = 42.0, thumbnailHue = 200),
        VideoSummary(name = "말차 레시피", duration = 27.0, thumbnailHue = 150),
        VideoSummary(name = "러닝 크루", duration = 15.0, thumbnailHue = 20),
    )

    /** Completed videos at app start. */
    val CompletedVideos: List<VideoSummary> = listOf(
        VideoSummary(name = "벚꽃 산책", duration = 21.0, thumbnailHue = 330),
        VideoSummary(name = "첫 출근 브이로그", duration = 34.0, thumbnailHue = 60),
        VideoSummary(name = "고양이 낮잠", duration = 12.0, thumbnailHue = 100),
    )

    /** Lines that the fake speech recognition "hears". */
    val CaptionPhrases: List<String> = listOf(
        "자 오늘은", "바닷가로 떠나볼게요", "노을이 진짜", "예술이었어요", "마무리는 타코", "당연하죠",
    )

    /** Title text of the sample draft project. */
    const val SampleTitle = "바닷가 주말"

    /** Indexes in [SourceVideos] that make the sample draft project. */
    val SampleClipIndexes = listOf(0, 3, 6, 2)
}
