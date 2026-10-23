package com.company.core.media

import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.TransitionType
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

// =====================================================================
// Look of the transitions (F08). The editor sheet, the placeholder stage, and the platform renderers use it.
// The values come from the Magic Editor v7 prototype (transFx, startFx).
// =====================================================================

/** Blur radius of the blur transition, as a part of the frame width. It is 10 px on the 264 px wide prototype stage. */
private const val BLUR_RADIUS = 10f / 264.375f

/** Blur radius of the blur opening, as a part of the frame width. It is 12 px on the prototype stage. */
private const val OPENING_BLUR_RADIUS = 12f / 264.375f

/**
 * Look of one scene during a transition. The scene is a full frame, and it changes around the frame center.
 *
 * @property alpha Opacity from 0 to 1.
 * @property scale Size factor.
 * @property translateX Horizontal move, as a part of the frame width. A positive value moves right.
 * @property translateY Vertical move, as a part of the frame height. A positive value moves down.
 * @property blur Blur radius, as a part of the frame width.
 * @property skewXDegrees Horizontal skew in degrees.
 * @property hueDegrees Hue rotation in degrees.
 * @property saturation Saturation factor. 1 keeps the colors.
 */
data class SceneLook(
    val alpha: Float = 1f,
    val scale: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val blur: Float = 0f,
    val skewXDegrees: Float = 0f,
    val hueDegrees: Float = 0f,
    val saturation: Float = 1f,
)

/** Color that covers the frame during a transition. */
enum class CoverColor {
    /** BLACK : Black, for the fade. */
    BLACK,

    /** WHITE : White, for the flash. */
    WHITE,
}

/**
 * Look of the frame at one moment of a transition.
 *
 * Draw a black frame, then [from], then [to], then [cover] with [coverAlpha].
 *
 * @property from Scene before the boundary. For the opening transition, it is a black scene.
 * @property to Scene after the boundary.
 * @property cover Color on top of both scenes.
 * @property coverAlpha Opacity of the cover.
 * @property fromIsBlack True when [from] is not a clip but black: the opening transition.
 */
data class TransitionLook(
    val from: SceneLook,
    val to: SceneLook,
    val cover: CoverColor = CoverColor.BLACK,
    val coverAlpha: Float = 0f,
    val fromIsBlack: Boolean = false,
)

/**
 * Look of a boundary transition.
 *
 * @param type Kind of the transition.
 * @param progress Progress from 0 to 1.
 * @param direction Side that the next scene comes from. Only [TransitionType.PUSH] uses it.
 */
fun transitionLook(type: TransitionType, progress: Float, direction: PushDirection = PushDirection.RIGHT): TransitionLook {
    val p = progress.coerceIn(0f, 1f)
    val middle = 1f - abs(2f * p - 1f)
    val q = easeInOutCubic(p)
    val half = if (p >= 0.5f) 1f else 0f
    return when (type) {
        TransitionType.DISSOLVE -> TransitionLook(from = SceneLook(), to = SceneLook(alpha = q))
        TransitionType.FADE -> TransitionLook(from = SceneLook(), to = SceneLook(alpha = half), coverAlpha = middle)
        TransitionType.PUSH -> {
            val (vx, vy) = when (direction) {
                PushDirection.LEFT -> -1f to 0f
                PushDirection.RIGHT -> 1f to 0f
                PushDirection.UP -> 0f to -1f
                PushDirection.DOWN -> 0f to 1f
            }
            TransitionLook(
                from = SceneLook(translateX = -vx * q, translateY = -vy * q),
                to = SceneLook(translateX = vx * (1f - q), translateY = vy * (1f - q)),
            )
        }
        TransitionType.ZOOM -> TransitionLook(
            from = SceneLook(alpha = 1f - easeInCubic(p), scale = 1f + easeInCubic(p) * 0.9f),
            to = SceneLook(alpha = easeOutCubic(p), scale = 1.3f - 0.3f * easeOutCubic(p)),
        )
        TransitionType.BLUR -> TransitionLook(
            from = SceneLook(blur = easeInCubic((p * 2f).coerceIn(0f, 1f)) * BLUR_RADIUS),
            to = SceneLook(
                alpha = ((p - 0.4f) / 0.2f).coerceIn(0f, 1f),
                blur = (1f - easeOutCubic((p * 2f - 1f).coerceIn(0f, 1f))) * BLUR_RADIUS,
            ),
        )
        TransitionType.FLASH -> TransitionLook(from = SceneLook(), to = SceneLook(alpha = half), cover = CoverColor.WHITE, coverAlpha = middle)
        TransitionType.GLITCH -> {
            val shaking = p > 0f && p < 1f
            val shake = if (shaking) {
                SceneLook(
                    translateX = sin(p * 97f) * 0.07f * middle,
                    skewXDegrees = sin(p * 53f) * 8f * middle,
                    hueDegrees = p * 300f,
                    saturation = 1f + middle * 2f,
                )
            } else {
                SceneLook()
            }
            TransitionLook(from = shake, to = shake.copy(alpha = half))
        }
    }
}

/**
 * Look of the opening transition. The first scene comes out of black.
 *
 * @param type Kind of the transition. A type without an opening shows the scene at once.
 * @param progress Progress from 0 to 1.
 */
fun openingLook(type: TransitionType, progress: Float): TransitionLook {
    val p = progress.coerceIn(0f, 1f)
    val black = SceneLook()
    return when (type) {
        TransitionType.FADE -> TransitionLook(from = black, to = SceneLook(), coverAlpha = 1f - easeInOutCubic(p), fromIsBlack = true)
        TransitionType.ZOOM -> TransitionLook(
            from = black,
            to = SceneLook(alpha = easeOutCubic(p), scale = 1.35f - 0.35f * easeOutCubic(p)),
            fromIsBlack = true,
        )
        TransitionType.BLUR -> TransitionLook(from = black, to = SceneLook(blur = (1f - easeOutCubic(p)) * OPENING_BLUR_RADIUS), fromIsBlack = true)
        TransitionType.FLASH -> TransitionLook(
            from = black,
            to = SceneLook(),
            cover = CoverColor.WHITE,
            coverAlpha = 1f - easeOutCubic(p),
            fromIsBlack = true,
        )
        TransitionType.DISSOLVE, TransitionType.PUSH, TransitionType.GLITCH -> TransitionLook(from = black, to = SceneLook(), fromIsBlack = true)
    }
}

private fun easeOutCubic(p: Float): Float = 1f - (1f - p).pow(3)

private fun easeInCubic(p: Float): Float = p.pow(3)

private fun easeInOutCubic(p: Float): Float = if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).pow(3) / 2f
