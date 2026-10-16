package com.company.feature.editor.voice

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.MyVoiceStatus
import com.company.core.domain.repository.MyVoiceRepository
import com.company.feature.editor.resources.Res
import com.company.feature.editor.resources.toast_my_voice_deleted
import com.company.feature.editor.resources.toast_my_voice_notify
import com.company.ui.text.UiText
import com.company.ui.text.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Step of the screen of the user's own voice (F06). */
enum class MyVoiceStep {
    /** INTRO : What the app records and keeps, and the agreement. */
    INTRO,

    /** CONSENT : Recording of the consent sentence ("1 / 2"). */
    CONSENT,

    /** SAMPLE : Recording of the sample sentence ("2 / 2"). */
    SAMPLE,

    /** CHECK : Check of the noise, the length, and the speaker. */
    CHECK,

    /** MAKING : The voice is in production. The user can leave the screen. */
    MAKING,

    /** DONE : The voice is ready. */
    DONE,

    /** MANAGE : The voice exists. The user can listen to it, make it again, or delete it. */
    MANAGE,
}

/**
 * State of the screen of the user's own voice.
 *
 * @property step Current step.
 * @property agreed True when the user agreed in the intro step.
 * @property recording True while the microphone records.
 * @property recordedSeconds Length of the recording of the current step.
 * @property checkedItems Number of check items that are done, from 0 to 3.
 * @property voice State of the voice in the repository.
 * @property samplePlaying True while the sample of the voice plays.
 * @property confirmDelete True while the popup asks before the delete.
 */
data class MyVoiceState(
    val step: MyVoiceStep = MyVoiceStep.INTRO,
    val agreed: Boolean = false,
    val recording: Boolean = false,
    val recordedSeconds: Double = 0.0,
    val checkedItems: Int = 0,
    val voice: MyVoiceStatus = MyVoiceStatus.None,
    val samplePlaying: Boolean = false,
    val confirmDelete: Boolean = false,
) : State() {
    /** Shortest recording of the current step, in seconds. */
    val requiredSeconds: Double
        get() = if (step == MyVoiceStep.SAMPLE) SAMPLE_SECONDS else CONSENT_SECONDS

    /** True when the recording of the current step is long enough. */
    val recordedEnough: Boolean
        get() = !recording && recordedSeconds >= requiredSeconds
}

/** User actions on the screen of the user's own voice. */
sealed class MyVoiceIntent : Intent() {
    /** ToggleAgree : Checks or clears the agreement. */
    data object ToggleAgree : MyVoiceIntent()

    /** ToggleRecording : Starts or stops the recording of the current step. */
    data object ToggleRecording : MyVoiceIntent()

    /** Primary : Runs the main button of the current step. */
    data object Primary : MyVoiceIntent()

    /** Secondary : Runs the second button: record again, listen, or delete. */
    data object Secondary : MyVoiceIntent()

    /** PlaySample : Starts or stops the sample of the voice. */
    data object PlaySample : MyVoiceIntent()

    /** ConfirmDelete : Deletes the voice after the popup. */
    data object ConfirmDelete : MyVoiceIntent()

    /** DismissDelete : Closes the popup without a delete. */
    data object DismissDelete : MyVoiceIntent()

    /** Close : Leaves the screen. The production continues. */
    data object Close : MyVoiceIntent()
}

/** One-time events of the screen of the user's own voice. */
sealed class MyVoiceSideEffect : SideEffect() {
    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: UiText) : MyVoiceSideEffect()

    /** Close : Leaves the screen. */
    data object Close : MyVoiceSideEffect()
}

/**
 * ViewModel of the screen of the user's own voice (F06): intro and agreement, two recordings, a check, and the
 * production. The repository makes the voice, so the production continues after the user leaves the screen.
 *
 * The app has no recorder and no vendor yet, so the recordings only count the time, and the check always passes.
 */
class MyVoiceViewModel(
    private val repository: MyVoiceRepository,
) : BaseViewModel<MyVoiceState, MyVoiceIntent, MyVoiceSideEffect>(
    initialState = MyVoiceState(
        step = when (repository.status.value) {
            MyVoiceStatus.Ready -> MyVoiceStep.MANAGE
            is MyVoiceStatus.Making -> MyVoiceStep.MAKING
            MyVoiceStatus.None -> MyVoiceStep.INTRO
        },
        voice = repository.status.value,
    ),
) {
    private var recordJob: Job? = null
    private var checkJob: Job? = null
    private var sampleJob: Job? = null

    init {
        viewModelScope.launch {
            repository.status.collect { status ->
                reduce {
                    // 만드는 중이던 화면은 다 되면 완료 단계로 간다.
                    val next = if (step == MyVoiceStep.MAKING && status == MyVoiceStatus.Ready) MyVoiceStep.DONE else step
                    copy(voice = status, step = next)
                }
            }
        }
    }

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: MyVoiceIntent) {
        when (intent) {
            MyVoiceIntent.ToggleAgree -> reduce { copy(agreed = !agreed) }
            MyVoiceIntent.ToggleRecording -> toggleRecording()
            MyVoiceIntent.Primary -> primary()
            MyVoiceIntent.Secondary -> secondary()
            MyVoiceIntent.PlaySample -> playSample()
            MyVoiceIntent.ConfirmDelete -> {
                reduce { copy(confirmDelete = false) }
                repository.delete()
                postSideEffect { MyVoiceSideEffect.ShowToast(uiText(Res.string.toast_my_voice_deleted)) }
                postSideEffect { MyVoiceSideEffect.Close }
            }
            MyVoiceIntent.DismissDelete -> reduce { copy(confirmDelete = false) }
            MyVoiceIntent.Close -> {
                if (currentState.step == MyVoiceStep.MAKING) postSideEffect { MyVoiceSideEffect.ShowToast(uiText(Res.string.toast_my_voice_notify)) }
                postSideEffect { MyVoiceSideEffect.Close }
            }
        }
    }

    // =====================================================================
    // Steps
    // =====================================================================

    private fun primary() {
        val state = currentState
        when (state.step) {
            MyVoiceStep.INTRO -> if (state.agreed) goTo(MyVoiceStep.CONSENT)
            MyVoiceStep.CONSENT -> if (state.recordedEnough) goTo(MyVoiceStep.SAMPLE)
            MyVoiceStep.SAMPLE -> if (state.recordedEnough) startCheck()
            MyVoiceStep.CHECK -> if (state.checkedItems >= CHECK_ITEMS) {
                repository.startMaking()
                reduce { copy(step = MyVoiceStep.MAKING) }
            }
            MyVoiceStep.MAKING, MyVoiceStep.DONE -> postSideEffect { MyVoiceSideEffect.Close }
            MyVoiceStep.MANAGE -> reduce { copy(step = MyVoiceStep.INTRO, agreed = false, recordedSeconds = 0.0) }
        }
    }

    private fun secondary() {
        when (currentState.step) {
            MyVoiceStep.MANAGE -> reduce { copy(confirmDelete = true) }
            MyVoiceStep.DONE -> playSample()
            MyVoiceStep.CONSENT, MyVoiceStep.SAMPLE -> reduce { copy(recordedSeconds = 0.0) }
            else -> Unit
        }
    }

    private fun goTo(step: MyVoiceStep) {
        recordJob?.cancel()
        reduce { copy(step = step, recording = false, recordedSeconds = 0.0) }
    }

    /** Counts the recording time while the microphone records. */
    private fun toggleRecording() {
        if (currentState.recording) {
            recordJob?.cancel()
            reduce { copy(recording = false) }
            return
        }
        reduce { copy(recording = true, recordedSeconds = 0.0) }
        recordJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MILLIS)
                reduce { copy(recordedSeconds = recordedSeconds + TICK_MILLIS / 1000.0) }
            }
        }
    }

    /** Shows the three check items one by one. */
    private fun startCheck() {
        reduce { copy(step = MyVoiceStep.CHECK, checkedItems = 0) }
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            repeat(CHECK_ITEMS) {
                delay(CHECK_ITEM_MILLIS)
                reduce { copy(checkedItems = checkedItems + 1) }
            }
        }
    }

    private fun playSample() {
        sampleJob?.cancel()
        if (currentState.samplePlaying) {
            reduce { copy(samplePlaying = false) }
            return
        }
        reduce { copy(samplePlaying = true) }
        sampleJob = viewModelScope.launch {
            delay(SAMPLE_PLAY_MILLIS)
            reduce { copy(samplePlaying = false) }
        }
    }
}

/** Shortest consent recording, in seconds. */
private const val CONSENT_SECONDS = 2.0

/** Shortest sample recording, in seconds. */
private const val SAMPLE_SECONDS = 5.0

/** Update interval of the recording time. */
private const val TICK_MILLIS = 100L

/** Number of check items. */
internal const val CHECK_ITEMS = 3

/** Time of one check item. */
private const val CHECK_ITEM_MILLIS = 500L

/** Length of the voice sample. */
private const val SAMPLE_PLAY_MILLIS = 1600L
