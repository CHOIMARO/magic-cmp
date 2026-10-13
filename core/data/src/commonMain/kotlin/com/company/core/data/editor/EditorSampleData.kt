package com.company.core.data.editor

import com.company.core.domain.model.editor.DubbingVoice
import com.company.core.domain.model.editor.MusicTrack
import com.company.core.domain.model.editor.SoundEffect
import com.company.core.domain.model.editor.SoundEffectCategory
import com.company.core.domain.model.editor.VideoSummary

/**
 * Sample content of the prototype.
 *
 * The app has no real music service, speech recognition, or text to speech yet.
 * The fake repositories use this data instead.
 */
internal object EditorSampleData {
    private val Hues = listOf(35, 80, 150, 200, 250, 300, 20, 120, 180, 270, 330, 60, 220, 100, 0, 160, 45, 240)
    private val Durations = listOf(4.2, 6.8, 3.1, 8.4, 5.0, 2.6, 7.2, 4.8, 3.6, 9.1, 5.5, 3.9, 6.1, 4.4, 2.9, 5.7, 4.0, 6.4)

    /**
     * Video of the sample draft project. It has no real file.
     *
     * @property name File name of the video.
     * @property duration Length of the video in seconds.
     * @property hue Hue of the placeholder thumbnail.
     */
    data class SampleVideo(val name: String, val duration: Double, val hue: Int)

    /** Videos that the sample draft project uses. */
    val SampleVideos: List<SampleVideo> = Hues.mapIndexed { index, hue ->
        SampleVideo(name = "VID_${4021 + index * 7}", duration = Durations[index], hue = hue)
    }

    /** Background music tracks. They are free for commercial use. */
    val MusicTracks: List<MusicTrack> = listOf(
        MusicTrack(id = "m1", title = "한밤의 드라이브", mood = "몽환", durationSeconds = 42, thumbnailHue = 250),
        MusicTrack(id = "m2", title = "파도 소리", mood = "청량", durationSeconds = 30, thumbnailHue = 200),
        MusicTrack(id = "m3", title = "빅 룸", mood = "신나는", durationSeconds = 24, thumbnailHue = 35),
        MusicTrack(id = "m4", title = "종이 등불", mood = "로파이", durationSeconds = 65, thumbnailHue = 300),
        MusicTrack(id = "m5", title = "일요일 버스", mood = "어쿠스틱", durationSeconds = 51, thumbnailHue = 80),
        MusicTrack(id = "m6", title = "반짝이는 오후", mood = "신나는", durationSeconds = 38, thumbnailHue = 60),
        MusicTrack(id = "m7", title = "느린 산책", mood = "어쿠스틱", durationSeconds = 47, thumbnailHue = 140),
        MusicTrack(id = "m8", title = "푸른 새벽", mood = "몽환", durationSeconds = 56, thumbnailHue = 220),
    )

    /** Voices for dubbings: two women, two men, and one character (PRD D11). */
    val Voices: List<DubbingVoice> = listOf(
        DubbingVoice(id = "v1", name = "하늘", description = "여성 · 맑고 밝은 목소리", hue = 330),
        DubbingVoice(id = "v2", name = "서윤", description = "여성 · 차분한 내레이션", hue = 260),
        DubbingVoice(id = "v3", name = "도윤", description = "남성 · 친근한 브이로그", hue = 200),
        DubbingVoice(id = "v4", name = "준호", description = "남성 · 낮고 믿음직한 목소리", hue = 150),
        DubbingVoice(id = "v5", name = "뽀삐", description = "캐릭터 · 통통 튀는 말투", hue = 70),
    )

    /** Sound effects. */
    val SoundEffects: List<SoundEffect> = listOf(
        SoundEffect("s1", SoundEffectCategory.HIGHLIGHT, "띠링", 0.6),
        SoundEffect("s2", SoundEffectCategory.HIGHLIGHT, "반짝", 0.8),
        SoundEffect("s3", SoundEffectCategory.HIGHLIGHT, "뿅", 0.3),
        SoundEffect("s4", SoundEffectCategory.REACTION, "박수", 2.4),
        SoundEffect("s5", SoundEffectCategory.REACTION, "웃음", 1.8),
        SoundEffect("s6", SoundEffectCategory.REACTION, "환호", 3.2),
        SoundEffect("s7", SoundEffectCategory.TRANSITION, "휙", 0.4),
        SoundEffect("s8", SoundEffectCategory.TRANSITION, "슝", 0.7),
        SoundEffect("s9", SoundEffectCategory.TENSION, "두둥", 1.5),
        SoundEffect("s10", SoundEffectCategory.TENSION, "쿵", 0.5),
        SoundEffect("s11", SoundEffectCategory.TENSION, "심장 박동", 4.0),
        SoundEffect("s12", SoundEffectCategory.COMIC, "삐빅", 0.4),
        SoundEffect("s13", SoundEffectCategory.COMIC, "띠로리", 1.6),
        SoundEffect("s14", SoundEffectCategory.DAILY, "카메라 셔터", 0.2),
        SoundEffect("s15", SoundEffectCategory.DAILY, "타자", 2.0),
        SoundEffect("s16", SoundEffectCategory.DAILY, "알림음", 0.5),
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

    /** Music of the sample draft project. */
    const val SampleMusicId = "m5"

    /** Indexes in [SampleVideos] that make the sample draft project. */
    val SampleClipIndexes = listOf(0, 3, 6, 2)
}
