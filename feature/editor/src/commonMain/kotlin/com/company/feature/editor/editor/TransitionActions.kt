package com.company.feature.editor.editor

import com.company.domain.model.editor.ClipTransition
import com.company.domain.model.editor.PushDirection
import com.company.domain.model.editor.TransitionSlot
import com.company.domain.model.editor.TransitionType
import com.company.domain.model.editor.clipBoundaries
import com.company.domain.model.editor.nearestTransitionSlot
import com.company.domain.model.editor.setTransition
import com.company.domain.model.editor.setTransitionForAllBoundaries
import com.company.domain.model.editor.transitionAt
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.toast_transition_applied_all
import com.company.feature.editor.resources.toast_transition_removed_all
import com.company.ui.text.uiText

/**
 * Transition sheet (F08).
 *
 * @param host Editor state and project.
 */
internal class TransitionActions(private val host: EditorHost) {
    /**
     * Opens the transition sheet of a slot. For a boundary, the playhead moves to the boundary.
     *
     * @param slot Place of the transition.
     */
    fun open(slot: TransitionSlot) {
        host.pause()
        val project = host.state.project
        val boundaryTime = (slot as? TransitionSlot.Boundary)?.let { boundary ->
            val index = project.clips.indexOfFirst { it.id == boundary.clipId }
            if (index > 0) project.clipBoundaries()[index] else null
        }
        host.reduce {
            copy(
                sheet = EditorSheet.TRANSITION,
                transitionSlot = slot,
                selectedClipId = null,
                selectedTextId = null,
                selectedTrackItem = null,
                time = boundaryTime ?: time,
            )
        }
    }

    /** Opens the transition sheet of the boundary nearest to the playhead. A project with one clip opens the video start. */
    fun openNearest() {
        val slot = host.state.project.nearestTransitionSlot(host.state.time) ?: return
        open(slot)
    }

    /**
     * Sets the transition of the open slot. The same transition again changes nothing.
     *
     * @param type New type, or null to remove the transition.
     */
    fun pick(type: TransitionType?) {
        val slot = host.state.transitionSlot ?: return
        val current = host.state.project.transitionAt(slot)
        val next = type?.let { ClipTransition(it, current?.direction ?: PushDirection.RIGHT) }
        if (next != current) host.projects.commit { it.setTransition(slot, next) }
    }

    /**
     * Sets the side of the push transition of the open slot.
     *
     * @param direction Side that the next scene comes from.
     */
    fun pickDirection(direction: PushDirection) {
        val slot = host.state.transitionSlot ?: return
        val next = ClipTransition(TransitionType.PUSH, direction)
        if (host.state.project.transitionAt(slot) != next) host.projects.commit { it.setTransition(slot, next) }
    }

    /** Sets the transition of the open slot at all clip boundaries. */
    fun applyToAll() {
        val slot = host.state.transitionSlot ?: return
        val current = host.state.project.transitionAt(slot)
        host.projects.commit { it.setTransitionForAllBoundaries(current) }
        host.toast(uiText(if (current != null) Res.string.toast_transition_applied_all else Res.string.toast_transition_removed_all))
    }
}
