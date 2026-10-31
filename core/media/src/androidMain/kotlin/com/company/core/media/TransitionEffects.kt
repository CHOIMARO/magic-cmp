package com.company.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.Pair
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.ConvolutionFunction1D
import androidx.media3.effect.GaussianFunction
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.MatrixTransformation
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.SeparableConvolutionShaderProgram
import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.TransitionWindow
import kotlin.math.PI
import kotlin.math.tan

// =====================================================================
// Media3 effects of the transitions (F08)
// =====================================================================

/** Blur radius in pixels below which the live frame stays sharp. */
private const val MIN_BLUR_RADIUS = 0.5f

/** Half width of the blur kernel, in standard deviations. It is the default of the Media3 Gaussian blur. */
private const val BLUR_KERNEL_DEVIATIONS = 2f

/**
 * Builds the composition effects that draw the transitions in the video area.
 *
 * Put the effects before the effect that moves the area into the frame. The frame then has the area size, and the
 * layout and text overlays do not change. The effects run in this order:
 * 1. Blur of the live frame ([SeparableConvolutionShaderProgram] with a kernel that changes with the time).
 * 2. Move, scale, skew, and turn of the live frame ([MatrixTransformation]).
 * 3. Hue, saturation, and opacity on black of the live frame ([RgbMatrix]).
 * 4. Still scene of the other clip ([OverlayEffect]). See [TransitionTimeline] for the layer order.
 * 5. Cover color ([RgbMatrix]).
 *
 * The list has only the effects that the transitions use. Without transitions, it is empty, so the playback cost does
 * not change.
 *
 * Approximations:
 * - The other scene is a still frame, so it does not move during the transition.
 * - The still scene supports only the alpha, the scale, the move, and the turn. No transition skews or colors the still
 *   scene.
 * - After the boundary of the spin, the still of the previous scene is on top. It gets a transparent hole where the
 *   next scene turns, so a new bitmap loads in each frame of that part.
 * - A still frame has one blur: the blur of its scene at the boundary. In the blur transition, the still shows only
 *   near the boundary, where the blur does not change. A blurred still is a small bitmap that the overlay scales up.
 * - A still frame is not larger than [STILL_MAX_SIDE] pixels on its longest side. A 4K export scales it up.
 * - The blur repeats the edge pixels. The prototype fades the blurred edges to transparent.
 * - Without a still layer between them, Media3 joins the cover with the live effects into one shader. The cover then
 *   changes only the pixels of the live scene. No transition moves the live scene and shows a cover at the same time.
 * - A frame waits on the GL thread until its still is ready, but not longer than a few seconds. The export then gets
 *   the still in each frame. The preview can stop for a moment after a seek into a transition.
 * - With HDR video, the still scene does not show. Media3 can put a bitmap on an HDR frame only with a gain map.
 *
 * @param clips Clips of the composition.
 * @param transitions Transitions in time order.
 * @param clipStartsUs Start time of each clip in microseconds, as Media3 places the clip.
 * @param frameWidth Width of the output frame in pixels.
 * @param frameHeight Height of the output frame in pixels.
 * @param areaWidth Width of the video area in pixels.
 * @param areaHeight Height of the video area in pixels.
 */
@OptIn(UnstableApi::class)
internal fun transitionEffects(
    clips: List<Clip>,
    transitions: List<TransitionWindow>,
    clipStartsUs: List<Long>,
    frameWidth: Int,
    frameHeight: Int,
    areaWidth: Int,
    areaHeight: Int,
): List<Effect> {
    if (transitions.isEmpty()) return emptyList()
    val timeline = TransitionTimeline(transitions, clipStartsUs)
    val layers = timeline.layers
    return buildList {
        if (layers.blur) add(LiveBlur(timeline))
        if (layers.shape) add(LiveShape(timeline))
        if (layers.color) add(LiveColor(timeline))
        if (layers.still) {
            val stills = StillFrames(clips, timeline, frameWidth, frameHeight, areaWidth, areaHeight)
            add(StillEffect(StillOverlay(timeline, stills), stills))
        }
        if (layers.cover) add(CoverTint(timeline))
    }
}

/** Blurs the live frame with the blur of the transition at the frame time. */
@OptIn(UnstableApi::class)
private class LiveBlur(private val timeline: TransitionTimeline) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        SeparableConvolutionShaderProgram(context, useHdr, BlurKernels(timeline))
}

/** Gives the blur kernel of each frame. A frame without a blur gets a kernel that keeps the pixels. */
@OptIn(UnstableApi::class)
private class BlurKernels(private val timeline: TransitionTimeline) : ConvolutionFunction1D.Provider {
    private var frameWidth = 1

    override fun configure(inputSize: Size): Size {
        frameWidth = inputSize.width
        return inputSize
    }

    override fun getConvolution(presentationTimeUs: Long): ConvolutionFunction1D {
        val radius = (timeline.frameAt(presentationTimeUs)?.live?.blur ?: 0f) * frameWidth
        // 반지름은 CSS blur()와 같이 표준 편차로 쓴다.
        return if (radius < MIN_BLUR_RADIUS) SHARP_KERNEL else GaussianFunction(radius, BLUR_KERNEL_DEVIATIONS)
    }

    private companion object {
        /** Kernel narrower than one pixel. The shader then reads only the center pixel. */
        val SHARP_KERNEL = GaussianFunction(0.2f, BLUR_KERNEL_DEVIATIONS)
    }
}

/**
 * Moves, scales, skews, and turns the live frame around its center.
 *
 * The matrix works in normalized device coordinates: -1 to 1, with y up.
 */
@OptIn(UnstableApi::class)
private class LiveShape(private val timeline: TransitionTimeline) : MatrixTransformation {
    private val matrix = Matrix()

    /** Frame height divided by the frame width. The skew needs it, because the coordinates are not square. */
    private var aspect = 1f

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        aspect = inputHeight.toFloat() / inputWidth
        return Size(inputWidth, inputHeight)
    }

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        matrix.reset()
        val look = timeline.frameAt(presentationTimeUs)?.live ?: return matrix
        // CSS의 skewX는 아래쪽을 오른쪽으로 민다. 여기서는 y가 위쪽이므로 부호가 바뀐다.
        matrix.setSkew(-tan(look.skewXDegrees * PI.toFloat() / 180f) * aspect, 0f)
        if (look.rotationDegrees != 0f) {
            // 가로와 세로의 단위가 다르므로, 화면 비율로 바꾼 뒤 돌리고 되돌린다. y가 위쪽이므로 시계 방향은 음수다.
            matrix.postScale(1f, aspect)
            matrix.postRotate(-look.rotationDegrees)
            matrix.postScale(1f, 1f / aspect)
        }
        matrix.postScale(look.scale, look.scale)
        matrix.postTranslate(look.translateX * 2f, -look.translateY * 2f)
        return matrix
    }
}

/** Changes the hue, the saturation, and the opacity on black of the live frame. */
@OptIn(UnstableApi::class)
private class LiveColor(private val timeline: TransitionTimeline) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray =
        timeline.frameAt(presentationTimeUs)?.live?.let(::sceneColorMatrix) ?: IDENTITY_COLOR_MATRIX
}

/** Mixes the frame with the cover color of the transition. */
@OptIn(UnstableApi::class)
private class CoverTint(private val timeline: TransitionTimeline) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray =
        timeline.frameAt(presentationTimeUs)?.let { coverColorMatrix(it.cover, it.coverAlpha) } ?: IDENTITY_COLOR_MATRIX
}

/**
 * Overlay effect of the still scene. It gives the [Context] of Media3 to [StillFrames].
 *
 * Media3 can put a bitmap on an HDR frame only with a gain map. For HDR video, the effect keeps the frame as it is.
 */
@OptIn(UnstableApi::class)
private class StillEffect(private val overlay: StillOverlay, private val stills: StillFrames) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        stills.attach(context)
        val overlays = if (useHdr) emptyList() else listOf(overlay)
        return OverlayEffect(overlays).toGlShaderProgram(context, useHdr)
    }
}

/**
 * Shows the still scene of the transition at the frame time.
 *
 * Media3 draws a bitmap at its pixel size, so the overlay scales the still to the frame size. A time without a still
 * keeps the last bitmap with alpha 0, so that the texture does not load again. A still with a hole shows a copy with
 * the hole.
 */
@OptIn(UnstableApi::class)
private class StillOverlay(private val timeline: TransitionTimeline, private val stills: StillFrames) : BitmapOverlay() {
    private var bitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    /** Still frame that [bitmap] shows, or null. */
    private var bitmapRef: StillRef? = null
    private val holes = StillWithHole()
    private var frameWidth = 1
    private var frameHeight = 1

    override fun configure(videoSize: Size) {
        frameWidth = videoSize.width
        frameHeight = videoSize.height
    }

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        stills.prefetch(presentationTimeUs)
        val layer = timeline.frameAt(presentationTimeUs)?.still
        val ref = layer?.ref
        if (ref != null && ref != bitmapRef) {
            stills.await(ref)?.let {
                bitmap = it
                bitmapRef = ref
            }
        }
        val hole = layer?.hole
        return if (hole != null && ref == bitmapRef) holes.draw(bitmap, hole) else bitmap
    }

    // Media3는 같은 시각의 getBitmap을 먼저 부른다. 그래서 bitmap은 이 프레임의 비트맵이다.
    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val layer = timeline.frameAt(presentationTimeUs)?.still
        if (layer == null || layer.ref != bitmapRef) return HIDDEN
        val look = layer.look
        return StillPlacement(
            alpha = look.alpha,
            scaleX = look.scale * frameWidth / bitmap.width,
            scaleY = look.scale * frameHeight / bitmap.height,
            anchorX = look.translateX * 2f,
            anchorY = -look.translateY * 2f,
            // 화면 좌표는 y가 위쪽이므로 시계 방향은 음수다.
            rotation = -look.rotationDegrees,
        )
    }

    private companion object {
        val HIDDEN = StillPlacement(alpha = 0f, scaleX = 1f, scaleY = 1f, anchorX = 0f, anchorY = 0f)
    }
}

/** Part of the live scene edge that the still with a hole covers, in still pixels. It hides the hard edge of the live scene. */
private const val HOLE_INSET_PX = 1f

/**
 * Copy of a still with a transparent hole. The cached still does not change, because other compositions can share it.
 *
 * Two copies take turns. Media3 loads the texture again when it gets a different bitmap object. A drawing on a canvas
 * possibly does not change the generation ID of the same bitmap.
 */
private class StillWithHole {
    private val copies = arrayOfNulls<Bitmap>(2)
    private var next = 0
    private val copyPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    /**
     * Draws [still] into the next copy and cuts the hole.
     *
     * @param still Still with a plain shape that fills the frame.
     * @param hole Place of the live scene in the frame.
     * @return The copy with the hole.
     */
    fun draw(still: Bitmap, hole: SceneLook): Bitmap {
        val index = next
        next = 1 - next
        val target = copies[index]?.takeIf { it.width == still.width && it.height == still.height }
            ?: Bitmap.createBitmap(still.width, still.height, Bitmap.Config.ARGB_8888).also { copies[index] = it }
        val canvas = Canvas(target)
        canvas.drawBitmap(still, 0f, 0f, copyPaint)
        val width = target.width.toFloat()
        val height = target.height.toFloat()
        val halfWidth = (width * hole.scale / 2f - HOLE_INSET_PX).coerceAtLeast(0f)
        val halfHeight = (height * hole.scale / 2f - HOLE_INSET_PX).coerceAtLeast(0f)
        // 비트맵 좌표는 y가 아래쪽이므로, 화면 기준 값을 그대로 쓴다.
        canvas.translate(width / 2f + hole.translateX * width, height / 2f + hole.translateY * height)
        canvas.rotate(hole.rotationDegrees)
        canvas.drawRect(-halfWidth, -halfHeight, halfWidth, halfHeight, clearPaint)
        return target
    }
}

/**
 * Place of the still overlay.
 *
 * [androidx.media3.effect.StaticOverlaySettings] does not accept an anchor outside the frame, but the push transition
 * moves the still out of the frame.
 *
 * @property alpha Opacity of the still.
 * @property scaleX Horizontal scale of the bitmap.
 * @property scaleY Vertical scale of the bitmap.
 * @property anchorX Horizontal center of the still in normalized device coordinates.
 * @property anchorY Vertical center of the still in normalized device coordinates. A positive value is up.
 * @property rotation Turn of the still in degrees. A positive value turns counterclockwise.
 */
@OptIn(UnstableApi::class)
private class StillPlacement(
    private val alpha: Float,
    private val scaleX: Float,
    private val scaleY: Float,
    private val anchorX: Float,
    private val anchorY: Float,
    private val rotation: Float = 0f,
) : OverlaySettings {
    override fun getAlphaScale(): Float = alpha

    override fun getScale(): Pair<Float, Float> = Pair.create(scaleX, scaleY)

    override fun getBackgroundFrameAnchor(): Pair<Float, Float> = Pair.create(anchorX, anchorY)

    override fun getRotationDegrees(): Float = rotation
}
