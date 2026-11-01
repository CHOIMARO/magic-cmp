package com.company.domain.model.editor

import kotlin.math.round

// =====================================================================
// Thumbnail templates (the "릴스 썸네일 템플릿" of the design)
//
// A thumbnail is a cover title of several lines. Each line becomes one text overlay with its own look, text, place,
// and accents. The values come from the design, which uses the 1080 × 1920 canvas.
// =====================================================================

/**
 * One line of a thumbnail template.
 *
 * @property style Look of the line.
 * @property text Text of the line.
 * @property x Horizontal center in percent of the screen width.
 * @property y Vertical center in percent of the screen height.
 * @property accents Accents of character ranges: a color or a size factor. They are decorations of the new text.
 */
data class ThumbnailLine(val style: TextStyleType, val text: String, val x: Double, val y: Double, val accents: List<DecorationRange> = emptyList())

/**
 * Makes a line from the canvas position of its center.
 *
 * @param style Look of the line.
 * @param text Text of the line.
 * @param centerX Horizontal center on the 1080 canvas.
 * @param centerY Vertical center on the 1920 canvas.
 * @param accents Parts of [text] with their accent decorations.
 */
private fun line(style: TextStyleType, text: String, centerX: Int, centerY: Int, vararg accents: Pair<String, Decoration>) = ThumbnailLine(
    style = style,
    text = text,
    x = percent(centerX / 10.8),
    y = percent(centerY / 19.2),
    accents = accents.map { (part, decoration) ->
        val start = text.indexOf(part)
        DecorationRange(start, start + part.length, decoration)
    },
)

/** Rounds a percent value to 2 decimals, as the design does. */
private fun percent(value: Double): Double = round(value * 100) / 100

/**
 * Thumbnail template. The order is the order of the template lists.
 *
 * @property lines Lines of the thumbnail, from the top.
 */
enum class ThumbnailTemplate(val lines: List<ThumbnailLine>) {
    /** FIRE : "불꽃·핑크 입체". */
    FIRE(
        listOf(
            line(TextStyleType.THUMB_FIRE_1, "이제는 마지막...", 540, 500),
            line(TextStyleType.THUMB_FIRE_2, "서울", 540, 781),
            line(TextStyleType.THUMB_FIRE_3, "7-8억대 아파트", 540, 962),
            line(TextStyleType.THUMB_FIRE_4, "강서구 1편", 540, 1340),
        ),
    ),

    /** BUBBLE : "옐로 버블". */
    BUBBLE(
        listOf(
            line(TextStyleType.THUMB_BUBBLE_1, "월 260", 540, 490, "월 " to Decoration(sizeScale = 0.68f)),
            line(TextStyleType.THUMB_BUBBLE_2, "1억 모은", 540, 700, "모은" to Decoration(sizeScale = 0.65f)),
            line(TextStyleType.THUMB_BUBBLE_3, "가계부작성법", 540, 884),
        ),
    ),

    /** HAND : "손글씨 풀스크린". */
    HAND(
        listOf(
            line(TextStyleType.THUMB_HAND_1, "마케팅", 540, 310),
            line(TextStyleType.THUMB_HAND_2, "수업에서", 540, 900),
            line(TextStyleType.THUMB_HAND_3, "배운 것", 540, 1502),
        ),
    ),

    /** THIN : "얇은 고딕 미니멀". */
    THIN(
        listOf(
            line(TextStyleType.THUMB_THIN_1, "with. LH•SH", 800, 554),
            line(TextStyleType.THUMB_THIN_2, "무주택 신혼부부가", 540, 714),
            line(TextStyleType.THUMB_THIN_3, "(청약 4번 당첨된 방법)", 540, 829),
            line(TextStyleType.THUMB_THIN_4, "ep 02. 청약을 어떻게 알아봐야 하지?", 540, 1168),
            line(TextStyleType.THUMB_THIN_5, "What does the house look like?", 480, 1220),
        ),
    ),

    /** BOLD : "볼드 고딕 하이라이트". */
    BOLD(
        listOf(
            line(TextStyleType.THUMB_BOLD_1, "출근 전 1시간", 540, 515),
            line(TextStyleType.THUMB_BOLD_2, "일주일에 3번", 540, 682, "3번" to Decoration(color = 0xFFF09C)),
            line(TextStyleType.THUMB_BOLD_3, "협찬받는 직장인", 540, 826),
            line(TextStyleType.THUMB_BOLD_4, "블로그 루틴", 540, 970),
        ),
    ),

    /** PIXEL : "픽셀 손글씨". */
    PIXEL(
        listOf(
            line(TextStyleType.THUMB_PIXEL_1, "20대 중반의 시선에서", 540, 471),
            line(TextStyleType.THUMB_PIXEL_2, "멋있다고 느끼는", 388, 698),
            line(TextStyleType.THUMB_PIXEL_3, "어른의 공통점", 733, 848),
            line(TextStyleType.THUMB_PIXEL_4, "[부제: 나의 어른 취미]", 465, 1021),
        ),
    ),
    ;

    companion object {
        /**
         * Finds the line that uses a look.
         *
         * @param style Look of a thumbnail line.
         * @return The line, or null when [style] is not a thumbnail line.
         */
        fun lineOf(style: TextStyleType): ThumbnailLine? = entries.firstNotNullOfOrNull { template -> template.lines.find { it.style == style } }
    }
}

/**
 * Adds all lines of a thumbnail as texts, with their texts, places, and accents.
 *
 * The lines start at [start] and stay for 3 seconds inside the video. When the video has less than 1 second after
 * [start], they stay for 3 seconds. Each line goes to the first row of the text track without an overlap.
 *
 * @param template Thumbnail to add.
 * @param start Start time of the lines.
 * @param replaceTextId ID of a text that the lines replace, or null.
 */
fun EditorProject.addThumbnail(template: ThumbnailTemplate, start: Double, replaceTextId: String? = null): EditorProject {
    val room = totalDuration - start
    val length = if (room >= NEW_TEXT_MIN_ROOM) minOf(NEW_TEXT_LENGTH, room) else NEW_TEXT_LENGTH
    val result = texts.filter { it.id != replaceTextId }.toMutableList()
    template.lines.forEach { line ->
        result += TextOverlay(
            id = newEditorId(),
            text = line.text,
            start = start,
            length = length,
            style = line.style,
            x = line.x,
            y = line.y,
            layer = result.firstFreeLayer(start, start + length),
            decorations = line.accents,
        )
    }
    return copy(texts = result).compactTextLayers()
}
