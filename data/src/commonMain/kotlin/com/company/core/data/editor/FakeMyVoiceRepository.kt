package com.company.core.data.editor

import com.company.core.domain.model.editor.MyVoiceStatus
import com.company.core.domain.repository.MyVoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Simulates the production of the user's voice. It keeps no recording and makes no model.
 *
 * The progress goes up 1.5% each 120 ms, as in the prototype. Replace this class when the vendor is decided (PRD D14).
 */
class FakeMyVoiceRepository : MyVoiceRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _status = MutableStateFlow<MyVoiceStatus>(MyVoiceStatus.None)
    override val status: StateFlow<MyVoiceStatus> = _status.asStateFlow()

    private val _readyEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val readyEvents: Flow<Unit> = _readyEvents.asSharedFlow()

    override fun startMaking() {
        job?.cancel()
        _status.value = MyVoiceStatus.Making(0)
        job = scope.launch {
            var progress = 0.0
            while (progress < 100.0) {
                delay(STEP_MILLIS)
                progress += STEP_PERCENT
                _status.value = if (progress < 100.0) MyVoiceStatus.Making(progress.toInt()) else MyVoiceStatus.Ready
            }
            _readyEvents.tryEmit(Unit)
        }
    }

    override fun delete() {
        job?.cancel()
        job = null
        _status.value = MyVoiceStatus.None
    }

    private companion object {
        const val STEP_MILLIS = 120L
        const val STEP_PERCENT = 1.5
    }
}
