package com.company.feature.editor.magic

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.usecase.editor.CreateMagicProjectUseCase
import com.company.ui.editor.DEFAULT_TITLE_TEXT
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Arguments of the magic screen.
 *
 * @property template Template that the user picked, or null.
 */
data class MagicArgs(val template: EditorTemplate?)

/**
 * State of the magic screen.
 *
 * @property step Index of the running step. A value equal to [MagicViewModel.STEPS] size means all steps are done.
 * @property firstHue Hue of the first clip for the front card.
 * @property secondHue Hue of the second clip for the back card.
 */
data class MagicState(
    val step: Int = 0,
    val firstHue: Int = 300,
    val secondHue: Int = 200,
) : State()

/** The magic screen has no user actions. It runs by itself. */
sealed class MagicIntent : Intent()

/** One-time events of the magic screen. */
sealed class MagicSideEffect : SideEffect() {
    /** OpenEditor(message) : Navigates to the editor and shows the message. */
    data class OpenEditor(val message: String) : MagicSideEffect()
}

/**
 * ViewModel of the magic screen.
 *
 * It shows the steps one by one, then decorates the clips of the current project.
 */
class MagicViewModel(
    args: MagicArgs,
    private val projectRepository: EditorProjectRepository,
    private val createMagicProject: CreateMagicProjectUseCase,
) : BaseViewModel<MagicState, MagicIntent, MagicSideEffect>(
    initialState = projectRepository.project.value.clips.let { clips ->
        MagicState(
            firstHue = clips.getOrNull(0)?.thumbnailHue ?: 300,
            secondHue = clips.getOrNull(1)?.thumbnailHue ?: 200,
        )
    }
) {

    init {
        viewModelScope.launch {
            repeat(STEPS.size) {
                delay(STEP_MILLIS)
                reduce { copy(step = step + 1) }
            }
            delay(STEP_MILLIS)
            val source = projectRepository.project.value
            val project = createMagicProject(
                name = source.name,
                clips = source.clips,
                template = args.template,
                titleText = DEFAULT_TITLE_TEXT,
            )
            projectRepository.start(project)
            postSideEffect { MagicSideEffect.OpenEditor("매직 완료 · 분위기는 ✦ 매직에서 바꿔요") }
        }
    }

    /**
     * The magic screen has no user actions.
     *
     * @param intent Not used.
     */
    override fun handleIntent(intent: MagicIntent) = Unit

    companion object {
        /** Labels of the magic steps. */
        val STEPS = listOf("흐름에 맞게 컷 정리", "어울리는 음악 고르기", "자막 만들기", "분위기 입히기")

        /** Time of one step. */
        private const val STEP_MILLIS = 650L
    }
}
