package com.company.ui.editor

import com.company.core.domain.model.editor.CaptionStyleType
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.FilterType
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.TemplateKind
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe

/** Default text of a new title overlay. */
const val DEFAULT_TITLE_TEXT = "오늘의 기록"

/** Display name of the layout. */
val LayoutType.displayName: String
    get() = when (this) {
        LayoutType.FULL -> "전체 화면"
        LayoutType.SPLIT_VERTICAL -> "위아래 분할"
        LayoutType.SPLIT_HORIZONTAL -> "좌우 분할"
        LayoutType.PICTURE_IN_PICTURE -> "화면 속 화면"
        LayoutType.BEFORE_AFTER -> "전후 비교"
        LayoutType.GRID_THREE -> "3단 분할"
        LayoutType.POLAROID -> "폴라로이드"
        LayoutType.CINEMA -> "시네마"
    }

/** Short note under the layout name, for example "클립 2개". */
val LayoutType.subLabel: String
    get() = if (this == LayoutType.FULL) "기본" else "클립 ${clipCount}개"

/** Description of the layout in the template sheet. */
val LayoutType.description: String
    get() = when (this) {
        LayoutType.FULL -> "영상 하나를 화면 가득 채워요."
        LayoutType.SPLIT_VERTICAL -> "두 장면을 위아래로 보여줘요. 리액션이나 비교 영상에 좋아요."
        LayoutType.SPLIT_HORIZONTAL -> "두 장면을 나란히 놓아요. 같은 순간 다른 시점을 보여주기 좋아요."
        LayoutType.PICTURE_IN_PICTURE -> "큰 화면 위에 작은 화면을 띄워요. 설명하거나 리뷰할 때 좋아요."
        LayoutType.BEFORE_AFTER -> "전과 후를 나란히 보여줘요. 정리·메이크업·인테리어에 딱이에요."
        LayoutType.GRID_THREE -> "세 장면을 한 화면에 쌓아요. 하루 요약이나 과정 영상에 좋아요."
        LayoutType.POLAROID -> "사진 액자처럼 여백을 둬요. 아래쪽에 글자를 넣기 좋아요."
        LayoutType.CINEMA -> "위아래에 검은 띠를 둬서 영화처럼 보여요."
    }

/** Display name of the text style. */
val TextStyleType.displayName: String
    get() = when (this) {
        TextStyleType.BOLD -> "굵게"
        TextStyleType.BOX -> "박스"
        TextStyleType.OUTLINE -> "테두리"
        TextStyleType.SERIF -> "감성 세리프"
        TextStyleType.NEON -> "네온"
        TextStyleType.MARKER -> "형광펜"
        TextStyleType.TAG -> "라벨"
    }

/** Display name of the caption style. */
val CaptionStyleType.displayName: String
    get() = when (this) {
        CaptionStyleType.BASIC -> "기본"
        CaptionStyleType.POP -> "강조"
        CaptionStyleType.BOX -> "박스"
        CaptionStyleType.MINIMAL -> "미니멀"
        CaptionStyleType.SERIF -> "세리프"
        CaptionStyleType.BIG -> "크게"
    }

/** Display name of the filter. */
val FilterType.displayName: String
    get() = when (this) {
        FilterType.NONE -> "원본"
        FilterType.WARM -> "노을"
        FilterType.FADE -> "필름"
        FilterType.COOL -> "청량"
        FilterType.PUNCH -> "쨍하게"
        FilterType.CINE -> "영화처럼"
        FilterType.MONO -> "흑백"
    }

/** Display name of the vibe preset. */
val Vibe.displayName: String
    get() = when (this) {
        Vibe.WARM -> "따뜻하게"
        Vibe.DREAMY -> "감성"
        Vibe.HYPE -> "신나게"
        Vibe.CINEMATIC -> "시네마틱"
        Vibe.FRESH -> "청량하게"
        Vibe.NONE -> "원본"
    }

/** Label of the template kind in the template sheet. */
val TemplateKind.sheetLabel: String
    get() = when (this) {
        TemplateKind.LAYOUT -> "레이아웃 템플릿"
        TemplateKind.TEXT -> "글자 템플릿"
        TemplateKind.CAPTION -> "자막 템플릿"
    }

/** Display name of the template. */
val EditorTemplate.displayName: String
    get() = when (this) {
        is EditorTemplate.Layout -> layout.displayName
        is EditorTemplate.Text -> style.displayName
        is EditorTemplate.Caption -> style.displayName
    }

/** Short note under the template name in the template grid. */
val EditorTemplate.subLabel: String
    get() = when (this) {
        is EditorTemplate.Layout -> "레이아웃 · ${layout.subLabel}"
        is EditorTemplate.Text -> "글자"
        is EditorTemplate.Caption -> "자막"
    }

/** Description of the template in the template sheet. */
val EditorTemplate.description: String
    get() = when (this) {
        is EditorTemplate.Layout -> layout.description
        is EditorTemplate.Text -> "제목이나 강조 문구에 쓰기 좋아요. 넣은 뒤 화면에서 끌어서 옮길 수 있어요."
        is EditorTemplate.Caption -> "자동 자막이 이 스타일로 나와요. 말하는 영상에 잘 어울려요."
    }

/**
 * Formats seconds as "m:ss".
 *
 * @param seconds Time in seconds. Negative values show as 0:00.
 */
fun formatTime(seconds: Double): String {
    val total = seconds.coerceAtLeast(0.0).toInt()
    val minutes = total / 60
    val rest = total % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}
