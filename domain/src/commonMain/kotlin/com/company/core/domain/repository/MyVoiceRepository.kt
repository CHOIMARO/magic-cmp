package com.company.core.domain.repository

import com.company.core.domain.model.editor.MyVoiceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Makes and keeps the user's own voice for dubbings ("내 목소리", F06).
 *
 * A user has one voice at most. The production continues when the user leaves the screen.
 */
interface MyVoiceRepository {
    /** State of the voice. */
    val status: StateFlow<MyVoiceStatus>

    /** Sends a value each time a voice becomes ready. */
    val readyEvents: Flow<Unit>

    /** Starts to make the voice from the recordings. A new voice replaces the old voice. */
    fun startMaking()

    /** Removes the voice, its model, and its recordings. Dubbings in projects stay. */
    fun delete()
}
