package com.company.feature.main

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.domain.model.editor.EditorSettings
import com.company.domain.model.editor.MyVoiceStatus
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.TemplateKind
import com.company.domain.model.editor.VideoSummary
import com.company.domain.repository.EditorSettingsRepository
import com.company.domain.repository.MyVoiceRepository
import com.company.domain.repository.MyVideoRepository
import com.company.domain.usecase.editor.OpenWorkUseCase
import kotlinx.coroutines.launch

/** Tab of the main screen. */
enum class MainTab {
    /** Start cards and the recent works. */
    HOME,

    /** All templates with a kind filter. */
    TEMPLATES,

    /** All works of the user in one list (D43). */
    LIBRARY,

    /** Profile and settings. */
    MY,
}

/** Setting that the user can switch on and off in the "마이" tab. */
enum class SettingKey {
    /** News about new templates. */
    TEMPLATE_NEWS,

    /** "magic" watermark on the video. */
    WATERMARK,
}

/** Bottom sheet of the main screen. */
sealed interface MainSheet {
    /** Template(template) : Preview of one template with a start button. */
    data class Template(val template: EditorTemplate) : MainSheet

    /** AppInfo : App version and links. */
    data object AppInfo : MainSheet
}

/**
 * State of the main tab screen.
 *
 * @property tab Selected tab.
 * @property sheet Open bottom sheet. Null means no sheet.
 * @property templateFilter Template kind filter in the template tab. Null shows all kinds.
 * @property works Works of the user, the work with the most recent change first (F15).
 * @property settings Editor settings.
 * @property myVoice State of the user's own voice for dubbings.
 */
data class MainState(
    val tab: MainTab = MainTab.HOME,
    val sheet: MainSheet? = null,
    val templateFilter: TemplateKind? = null,
    val works: List<VideoSummary> = emptyList(),
    val settings: EditorSettings = EditorSettings(),
    val myVoice: MyVoiceStatus = MyVoiceStatus.None,
) : State()

/** User actions on the main screen. */
sealed class MainIntent : Intent() {
    /** SelectTab(tab) : Shows the tab and closes the open sheet. */
    data class SelectTab(val tab: MainTab) : MainIntent()

    /** OpenTemplate(template) : Opens the preview sheet of the template. */
    data class OpenTemplate(val template: EditorTemplate) : MainIntent()

    /** OpenAppInfo : Opens the app information sheet. */
    data object OpenAppInfo : MainIntent()

    /** CloseSheet : Closes the open sheet. */
    data object CloseSheet : MainIntent()

    /** SelectTemplateFilter(kind) : Filters the template grid. Null shows all kinds. */
    data class SelectTemplateFilter(val kind: TemplateKind?) : MainIntent()

    /** ShowAllWorks : Opens the library tab with all works. */
    data object ShowAllWorks : MainIntent()

    /** ToggleSetting(key) : Switches the setting on or off. */
    data class ToggleSetting(val key: SettingKey) : MainIntent()

    /** StartNewVideo : Opens the photo and video picker for a new project. */
    data object StartNewVideo : MainIntent()

    /** OpenCamera : Opens the camera. */
    data object OpenCamera : MainIntent()

    /** UseSelectedTemplate : Opens the video picker with the template of the open sheet. */
    data object UseSelectedTemplate : MainIntent()

    /** OpenWork(workId) : Opens the work in the editor, in its last saved state (F15). */
    data class OpenWork(val workId: String) : MainIntent()

    /** OpenMyVoice : Opens the screen of the user's own voice. */
    data object OpenMyVoice : MainIntent()
}

/** One-time events of the main screen. */
sealed class MainSideEffect : SideEffect() {
    /** OpenGallery(template) : Navigates to the photo and video picker for a new project with the template, or with no template. */
    data class OpenGallery(val template: EditorTemplate?) : MainSideEffect()

    /** OpenCamera : Navigates to the camera. */
    data object OpenCamera : MainSideEffect()

    /** OpenEditor : Navigates to the editor with the current project. */
    data object OpenEditor : MainSideEffect()

    /** OpenMyVoice : Navigates to the screen of the user's own voice. */
    data object OpenMyVoice : MainSideEffect()
}

/**
 * ViewModel of the main tab screen.
 *
 * It shows the tabs and the sheets, keeps the settings, and starts the create flows.
 */
class MainViewModel(
    myVideoRepository: MyVideoRepository,
    private val settingsRepository: EditorSettingsRepository,
    private val openWork: OpenWorkUseCase,
    myVoiceRepository: MyVoiceRepository,
) : BaseViewModel<MainState, MainIntent, MainSideEffect>(
    initialState = MainState()
) {

    init {
        viewModelScope.launch { myVideoRepository.works.collect { reduce { copy(works = it) } } }
        viewModelScope.launch { settingsRepository.settings.collect { reduce { copy(settings = it) } } }
        viewModelScope.launch { myVoiceRepository.status.collect { reduce { copy(myVoice = it) } } }
    }

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SelectTab -> reduce { copy(tab = intent.tab, sheet = null) }
            is MainIntent.OpenTemplate -> reduce { copy(sheet = MainSheet.Template(intent.template)) }
            MainIntent.OpenAppInfo -> reduce { copy(sheet = MainSheet.AppInfo) }
            MainIntent.CloseSheet -> reduce { copy(sheet = null) }
            is MainIntent.SelectTemplateFilter -> reduce { copy(templateFilter = intent.kind) }
            MainIntent.ShowAllWorks -> reduce { copy(tab = MainTab.LIBRARY) }
            is MainIntent.ToggleSetting -> toggleSetting(intent.key)
            MainIntent.StartNewVideo -> openGallery(template = null)
            MainIntent.OpenCamera -> {
                reduce { copy(sheet = null) }
                postSideEffect { MainSideEffect.OpenCamera }
            }
            MainIntent.UseSelectedTemplate -> useSelectedTemplate()
            is MainIntent.OpenWork -> openInEditor(intent.workId)
            MainIntent.OpenMyVoice -> postSideEffect { MainSideEffect.OpenMyVoice }
        }
    }

    // =====================================================================
    // Create flows
    // =====================================================================

    private fun openGallery(template: EditorTemplate?) {
        reduce { copy(sheet = null) }
        postSideEffect { MainSideEffect.OpenGallery(template) }
    }

    private fun useSelectedTemplate() {
        val template = (currentState.sheet as? MainSheet.Template)?.template ?: return
        openGallery(template)
    }

    /**
     * Opens a work in the editor. The undo history is empty.
     *
     * @param workId ID of the work.
     */
    private fun openInEditor(workId: String) {
        viewModelScope.launch {
            if (openWork(workId)) postSideEffect { MainSideEffect.OpenEditor }
        }
    }

    // =====================================================================
    // Settings
    // =====================================================================

    private fun toggleSetting(key: SettingKey) {
        settingsRepository.update { settings ->
            when (key) {
                SettingKey.TEMPLATE_NEWS -> settings.copy(templateNews = !settings.templateNews)
                SettingKey.WATERMARK -> settings.copy(watermark = !settings.watermark)
            }
        }
    }
}
