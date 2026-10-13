package com.company.core.media

import com.company.core.domain.model.editor.TransitionWindow
import kotlin.math.abs
import kotlin.math.roundToLong

// =====================================================================
// Frame plan of the transitions for the Media3 renderer (F08).
// It uses only Kotlin types, so the host tests can check it.
// =====================================================================

/** Number of samples in each transition that [TransitionTimeline] uses to find the layers of the transition. */
private const val LAYER_SAMPLES = 48

/** Tolerance for the tests of a scene look. Float rounding makes a value that is almost exact. */
private const val LOOK_EPSILON = 1e-4f

/** Progress of a boundary transition at the clip boundary. */
private const val BOUNDARY_PROGRESS = 0.5f

/** Edge of a clip that a still frame shows. */
internal enum class StillEdge {
    /** FIRST : The first frame of the clip. */
    FIRST,

    /** LAST : The last frame of the clip. */
    LAST,
}

/**
 * Still frame of one clip edge.
 *
 * @property clipIndex Index of the clip.
 * @property edge Edge of the clip.
 */
internal data class StillRef(val clipIndex: Int, val edge: StillEdge)

/**
 * Still scene on top of the live frame.
 *
 * @property ref Still frame to show.
 * @property look Look of the still. Its alpha is the opacity of the layer. The renderer uses only the alpha, the scale,
 *   and the move.
 */
internal data class StillLayer(val ref: StillRef, val look: SceneLook)

/**
 * How to draw one output frame of a transition.
 *
 * Draw the live frame with [live] on black, then [still], then [cover] with [coverAlpha].
 *
 * @property live Look of the clip that plays now. Its alpha multiplies the colors. This is the same as opacity on black.
 * @property still Still scene on top of the live frame, or null.
 * @property cover Color on top of both scenes.
 * @property coverAlpha Opacity of the cover.
 */
internal data class TransitionFrame(
    val live: SceneLook,
    val still: StillLayer? = null,
    val cover: CoverColor = CoverColor.BLACK,
    val coverAlpha: Float = 0f,
)

/**
 * Layers that the transitions of a composition use. The renderer adds an effect only for a layer in use.
 *
 * @property blur True when the live frame blurs.
 * @property shape True when the live frame moves, scales, or skews.
 * @property color True when the live frame changes its opacity, hue, or saturation.
 * @property still True when a still scene shows.
 * @property cover True when a cover color shows.
 */
internal data class TransitionLayers(
    val blur: Boolean = false,
    val shape: Boolean = false,
    val color: Boolean = false,
    val still: Boolean = false,
    val cover: Boolean = false,
)

/**
 * Transitions of a composition on the Media3 timeline.
 *
 * Media3 plays one clip at a time, and the project length does not change. The clip that plays is the live scene. The
 * other scene of a boundary transition is a still frame. Before the boundary, the still is the first frame of the next
 * clip. After the boundary, the still is the last frame of the previous clip.
 *
 * Media3 can put the still only on top of the live frame. Before the boundary, this is the correct order. After the
 * boundary, the still scene must be under the live scene, so the plan changes the opacities:
 * - When the live scene covers the whole frame, the still alpha becomes `(1 - live) × still` and the live alpha becomes
 *   `live / (1 - still alpha)`. The result is the same as the formula.
 * - Otherwise, the still stays on top with its own alpha. The result is the same as the formula when the two scenes do
 *   not overlap, as in the push transition.
 *
 * @param windows Transitions in time order. When two transitions overlap, the later one shows.
 * @param clipStartsUs Start time of each clip in microseconds, as Media3 places the clip.
 */
internal class TransitionTimeline(windows: List<TransitionWindow>, private val clipStartsUs: List<Long>) {
    private val windows: List<TransitionWindow> = windows.filter { window ->
        window.end > window.start &&
            window.toClip in clipStartsUs.indices &&
            (window.fromClip == null || window.fromClip in clipStartsUs.indices)
    }
    private val startsUs = this.windows.map { (it.start * 1_000_000).roundToLong() }
    private val endsUs = this.windows.map { (it.end * 1_000_000).roundToLong() }

    /** Still frames of each transition, in the order of [windows]. */
    private val stills: List<Set<StillRef>>

    /** Blur of each still frame, as a part of the frame width. */
    private val stillBlurs: Map<StillRef, Float>

    /** Layers that the transitions use. */
    val layers: TransitionLayers

    init {
        var layers = TransitionLayers()
        val stills = ArrayList<Set<StillRef>>(this.windows.size)
        val stillBlurs = HashMap<StillRef, Float>()
        this.windows.forEachIndexed { index, window ->
            val refs = HashSet<StillRef>()
            // 짧은 효과도 놓치지 않게 전환 구간을 고르게 나누어 확인한다.
            for (sample in 0 until LAYER_SAMPLES) {
                val timeUs = startsUs[index] + (endsUs[index] - startsUs[index]) * (2 * sample + 1) / (2 * LAYER_SAMPLES)
                val frame = frameOf(window, timeUs)
                frame.still?.let { refs += it.ref }
                layers = layers.copy(
                    blur = layers.blur || frame.live.blur > 0f,
                    shape = layers.shape || !frame.live.hasPlainShape,
                    color = layers.color || !frame.live.hasPlainColor,
                    still = layers.still || frame.still != null,
                    cover = layers.cover || frame.coverAlpha > 0f,
                )
            }
            refs.forEach { stillBlurs[it] = boundaryBlurOf(window, it.edge) }
            stills += refs
        }
        this.stills = stills
        this.stillBlurs = stillBlurs
        this.layers = layers
    }

    /** All still frames of the transitions. */
    val stillRefs: Set<StillRef>
        get() = stillBlurs.keys

    /**
     * Frame plan at [timeUs], or null when no transition runs.
     *
     * @param timeUs Time on the timeline in microseconds.
     */
    fun frameAt(timeUs: Long): TransitionFrame? {
        val index = windowIndexAt(timeUs)
        return if (index < 0) null else frameOf(windows[index], timeUs)
    }

    /**
     * Index of the transition that runs at [timeUs], or -1.
     *
     * @param timeUs Time on the timeline in microseconds.
     */
    fun windowIndexAt(timeUs: Long): Int {
        for (index in windows.indices.reversed()) {
            if (timeUs >= startsUs[index] && timeUs < endsUs[index]) return index
        }
        return -1
    }

    /**
     * Index of the first transition that starts after [timeUs], or -1.
     *
     * @param timeUs Time on the timeline in microseconds.
     */
    fun nextWindowIndexAfter(timeUs: Long): Int = startsUs.indexOfFirst { it > timeUs }

    /**
     * Still frames that the transition at [index] shows. It is empty for an index that is not valid.
     *
     * @param index Index from [windowIndexAt] or [nextWindowIndexAfter].
     */
    fun stillsOf(index: Int): Set<StillRef> = stills.getOrElse(index) { emptySet() }

    /**
     * Blur of the still frame [ref], as a part of the frame width.
     *
     * A still frame keeps one blur: the blur of its scene at the boundary.
     *
     * @param ref Still frame from [stillsOf].
     */
    fun stillBlur(ref: StillRef): Float = stillBlurs[ref] ?: 0f

    private fun frameOf(window: TransitionWindow, timeUs: Long): TransitionFrame {
        val progress = window.progressAt(timeUs / 1_000_000.0).toFloat()
        val transition = window.transition
        val fromClip = window.fromClip
        if (fromClip == null) {
            // 시작 전환에서 앞 장면은 검은 화면이므로, 재생 중인 첫 클립만 그린다.
            val look = openingLook(transition.type, progress)
            return TransitionFrame(live = look.to, cover = look.cover, coverAlpha = look.coverAlpha)
        }
        val look = transitionLook(transition.type, progress, transition.direction)
        return if (timeUs < clipStartsUs[window.toClip]) {
            // 경계 전: 앞 클립이 재생되고, 다음 장면(정지 화면)이 위에 온다. 그리는 순서가 공식과 같다.
            val ref = StillRef(window.toClip, StillEdge.FIRST)
            val still = if (look.to.isVisible) StillLayer(ref, look.to) else null
            TransitionFrame(live = look.from, still = still, cover = look.cover, coverAlpha = look.coverAlpha)
        } else {
            afterBoundary(look, StillRef(fromClip, StillEdge.LAST))
        }
    }

    /** Frame after the boundary: the next clip plays, and the previous scene is the still. */
    private fun afterBoundary(look: TransitionLook, ref: StillRef): TransitionFrame {
        val from = look.from
        val to = look.to
        val hidden = !from.isVisible || (to.alpha >= 1f && (to.coversFrame || to.hasSameShape(from)))
        return when {
            hidden -> TransitionFrame(live = to, cover = look.cover, coverAlpha = look.coverAlpha)
            to.coversFrame -> {
                // 아래 장면을 위로 올리고 두 투명도를 바꾸면 결과가 공식과 같다.
                val stillAlpha = (1f - to.alpha) * from.alpha
                val liveAlpha = if (stillAlpha < 1f) to.alpha / (1f - stillAlpha) else 0f
                TransitionFrame(
                    live = to.copy(alpha = liveAlpha),
                    still = StillLayer(ref, from.copy(alpha = stillAlpha)),
                    cover = look.cover,
                    coverAlpha = look.coverAlpha,
                )
            }
            // 두 장면이 겹치지 않으면(밀기) 어느 장면을 위에 그려도 결과가 같다.
            else -> TransitionFrame(
                live = to,
                still = StillLayer(ref, from),
                cover = look.cover,
                coverAlpha = look.coverAlpha,
            )
        }
    }

    /** Blur of the scene at [edge] at the boundary of [window]. */
    private fun boundaryBlurOf(window: TransitionWindow, edge: StillEdge): Float {
        val look = transitionLook(window.transition.type, BOUNDARY_PROGRESS, window.transition.direction)
        return when (edge) {
            StillEdge.FIRST -> look.to.blur
            StillEdge.LAST -> look.from.blur
        }
    }
}

/** True when the scene shows some part in the frame. It ignores the skew. */
private val SceneLook.isVisible: Boolean
    get() = alpha > 0f && abs(translateX) < (1f + scale) / 2f && abs(translateY) < (1f + scale) / 2f

/** True when the scene covers the whole frame. */
private val SceneLook.coversFrame: Boolean
    get() = abs(skewXDegrees) < LOOK_EPSILON &&
        abs(translateX) <= (scale - 1f) / 2f + LOOK_EPSILON &&
        abs(translateY) <= (scale - 1f) / 2f + LOOK_EPSILON

/** True when the scene has the same place and size as [other]. */
private fun SceneLook.hasSameShape(other: SceneLook): Boolean =
    abs(scale - other.scale) < LOOK_EPSILON &&
        abs(translateX - other.translateX) < LOOK_EPSILON &&
        abs(translateY - other.translateY) < LOOK_EPSILON &&
        abs(skewXDegrees - other.skewXDegrees) < LOOK_EPSILON

/** True when the scene does not move, scale, or skew. */
internal val SceneLook.hasPlainShape: Boolean
    get() = abs(scale - 1f) < LOOK_EPSILON &&
        abs(translateX) < LOOK_EPSILON &&
        abs(translateY) < LOOK_EPSILON &&
        abs(skewXDegrees) < LOOK_EPSILON

/** True when the scene does not change its colors. */
internal val SceneLook.hasPlainColor: Boolean
    get() = abs(alpha - 1f) < LOOK_EPSILON && abs(hueDegrees) < LOOK_EPSILON && abs(saturation - 1f) < LOOK_EPSILON
