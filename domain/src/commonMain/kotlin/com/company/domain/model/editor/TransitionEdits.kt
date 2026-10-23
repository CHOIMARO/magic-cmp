package com.company.domain.model.editor

// =====================================================================
// Transitions (F08)
// =====================================================================

/** Length of a transition in seconds. The user cannot change it yet. */
const val TRANSITION_SECONDS = 0.5

/**
 * Place of a transition.
 *
 * Each clip after the first one has a boundary before it. The video start is a separate place.
 */
sealed interface TransitionSlot {
    /** Opening : The start of the video. */
    data object Opening : TransitionSlot

    /** Boundary(clipId) : The boundary before the clip [clipId]. */
    data class Boundary(val clipId: String) : TransitionSlot
}

/**
 * Transition at [slot], or null when the slot has no transition.
 *
 * @param slot Place of the transition.
 */
fun EditorProject.transitionAt(slot: TransitionSlot): ClipTransition? = when (slot) {
    TransitionSlot.Opening -> openingTransition
    is TransitionSlot.Boundary -> transitions[slot.clipId]
}

/**
 * Sets or removes the transition at [slot].
 *
 * @param slot Place of the transition. A boundary of the first clip or of a clip that does not exist is ignored.
 * @param transition New transition, or null to remove it. The opening slot ignores a type that it cannot use.
 */
fun EditorProject.setTransition(slot: TransitionSlot, transition: ClipTransition?): EditorProject = when (slot) {
    TransitionSlot.Opening -> copy(openingTransition = transition?.takeIf { it.type.canOpen })
    is TransitionSlot.Boundary -> {
        val index = clips.indexOfFirst { it.id == slot.clipId }
        when {
            index < 1 -> this
            transition == null -> copy(transitions = transitions - slot.clipId)
            else -> copy(transitions = transitions + (slot.clipId to transition))
        }
    }
}

/**
 * Sets the same transition at all clip boundaries. The video start does not change.
 *
 * @param transition Transition to set, or null to remove all boundary transitions.
 */
fun EditorProject.setTransitionForAllBoundaries(transition: ClipTransition?): EditorProject =
    copy(transitions = if (transition == null) emptyMap() else clips.drop(1).associate { it.id to transition })

/**
 * Length of the transition before the clip at [clipIndex]. It is [TRANSITION_SECONDS], but not more than half of the
 * shorter neighbor clip. For index 0, it is the length of the opening transition.
 *
 * @param clipIndex Index of the clip after the boundary.
 */
fun EditorProject.transitionLength(clipIndex: Int): Double {
    val clip = clips.getOrNull(clipIndex) ?: return 0.0
    val previous = clips.getOrNull(clipIndex - 1)
    return if (previous == null) {
        minOf(TRANSITION_SECONDS, clip.duration)
    } else {
        minOf(TRANSITION_SECONDS, previous.duration / 2, clip.duration / 2)
    }
}

/**
 * Boundary nearest to [time]. When the project has one clip, it is the video start.
 *
 * @param time Time on the timeline.
 * @return The slot, or null when the project has no clips.
 */
fun EditorProject.nearestTransitionSlot(time: Double): TransitionSlot? {
    if (clips.isEmpty()) return null
    if (clips.size < 2) return TransitionSlot.Opening
    val boundaries = clipBoundaries()
    val index = (1 until clips.size).minBy { kotlin.math.abs(boundaries[it] - time) }
    return TransitionSlot.Boundary(clips[index].id)
}

/**
 * Time range of a transition on the timeline.
 *
 * A boundary transition starts half of its length before the boundary and ends half of its length after it, so the
 * project length does not change. An opening transition starts at 0.
 *
 * @property transition The transition.
 * @property start Start time.
 * @property end End time.
 * @property boundary Time of the clip boundary. It is 0 for the opening transition.
 * @property fromClip Index of the clip before the boundary, or null for the opening transition.
 * @property toClip Index of the clip after the boundary.
 */
data class TransitionWindow(
    val transition: ClipTransition,
    val start: Double,
    val end: Double,
    val boundary: Double,
    val fromClip: Int?,
    val toClip: Int,
) {
    /**
     * Progress of the transition at [time], from 0 to 1.
     *
     * @param time Time on the timeline.
     */
    fun progressAt(time: Double): Double = if (end <= start) 1.0 else ((time - start) / (end - start)).coerceIn(0.0, 1.0)
}

/** Time ranges of all transitions, in time order. */
fun EditorProject.transitionWindows(): List<TransitionWindow> {
    if (clips.isEmpty()) return emptyList()
    val boundaries = clipBoundaries()
    val windows = mutableListOf<TransitionWindow>()
    openingTransition?.let { transition ->
        windows += TransitionWindow(transition, start = 0.0, end = transitionLength(0), boundary = 0.0, fromClip = null, toClip = 0)
    }
    for (index in 1 until clips.size) {
        val transition = transitions[clips[index].id] ?: continue
        val half = transitionLength(index) / 2
        val boundary = boundaries[index]
        windows += TransitionWindow(transition, start = boundary - half, end = boundary + half, boundary = boundary, fromClip = index - 1, toClip = index)
    }
    return windows
}
