package com.company.feature.main

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.domain.model.account.SessionState
import com.company.domain.model.editor.EditorSettings
import com.company.domain.model.editor.MyVoiceStatus
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.TemplateKind
import com.company.domain.model.editor.VideoSummary
import com.company.domain.repository.AuthRepository
import com.company.domain.repository.EditorSettingsRepository
import com.company.domain.repository.MyVoiceRepository
import com.company.domain.repository.MyVideoRepository
import com.company.domain.usecase.account.SignOutUseCase
import com.company.domain.usecase.editor.OpenWorkUseCase
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.toast_work_delete_failed
import com.company.feature.main.resources.toast_work_deleted
import com.company.ui.account.LoginGate
import com.company.ui.account.LoginReason
import com.company.ui.text.UiText
import com.company.ui.text.uiText
import kotlinx.coroutines.CancellationException
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

    /** Work(workId) : Menu of a work: export and delete (S13). A long press on the work card opens it. */
    data class Work(val workId: String) : MainSheet
}

/**
 * State of the main tab screen.
 *
 * @property tab Selected tab.
 * @property sheet Open bottom sheet. Null means no sheet.
 * @property templateFilter Template kind filter in the template tab. Null shows all kinds.
 * @property works Works of the user, the work with the most recent change first (F15).
 * @property deleteWorkId Work that the delete popup asks about, or null when the popup is closed.
 * @property settings Editor settings.
 * @property myVoice State of the user's own voice for dubbings.
 * @property session Sign-in state. The "마이" tab shows the profile or the login card from it (F17).
 * @property logoutConfirm True while the logout popup asks the user.
 */
data class MainState(
    val tab: MainTab = MainTab.HOME,
    val sheet: MainSheet? = null,
    val templateFilter: TemplateKind? = null,
    val works: List<VideoSummary> = emptyList(),
    val deleteWorkId: String? = null,
    val settings: EditorSettings = EditorSettings(),
    val myVoice: MyVoiceStatus = MyVoiceStatus.None,
    val session: SessionState = SessionState.SignedOut,
    val logoutConfirm: Boolean = false,
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

    /** OpenWorkMenu(workId) : Opens the menu of the work after a long press on its card (S13). */
    data class OpenWorkMenu(val workId: String) : MainIntent()

    /** ExportWork(workId) : Opens the export screen of the work, without the editor. */
    data class ExportWork(val workId: String) : MainIntent()

    /** RequestDeleteWork(workId) : Asks the user before the work goes away. */
    data class RequestDeleteWork(val workId: String) : MainIntent()

    /** ConfirmDeleteWork : Removes the work that the popup asks about. The gallery files stay. */
    data object ConfirmDeleteWork : MainIntent()

    /** DismissDeleteWork : Closes the delete popup. The work stays. */
    data object DismissDeleteWork : MainIntent()

    /** OpenMyVoice : Opens the screen of the user's own voice. Without an account, the login sheet opens first. */
    data object OpenMyVoice : MainIntent()

    /** OpenLogin : Opens the login sheet from the login card or from a row that needs an account. */
    data object OpenLogin : MainIntent()

    /** OpenAccount : Opens the account management screen (S15). */
    data object OpenAccount : MainIntent()

    /** RequestLogout : Asks the user before the logout. */
    data object RequestLogout : MainIntent()

    /** ConfirmLogout : Signs out. The works on the device and the dubbings in them stay. */
    data object ConfirmLogout : MainIntent()

    /** DismissLogout : Closes the logout popup. The user stays signed in. */
    data object DismissLogout : MainIntent()
}

/** One-time events of the main screen. */
sealed class MainSideEffect : SideEffect() {
    /** OpenGallery(template) : Navigates to the photo and video picker for a new project with the template, or with no template. */
    data class OpenGallery(val template: EditorTemplate?) : MainSideEffect()

    /** OpenCamera : Navigates to the camera. */
    data object OpenCamera : MainSideEffect()

    /** OpenEditor : Navigates to the editor with the current project. */
    data object OpenEditor : MainSideEffect()

    /** OpenExport : Navigates to the export screen of the current project. */
    data object OpenExport : MainSideEffect()

    /** ShowToast(message) : Shows a short message. */
    data class ShowToast(val message: UiText) : MainSideEffect()

    /** OpenMyVoice : Navigates to the screen of the user's own voice. */
    data object OpenMyVoice : MainSideEffect()

    /** OpenAccount : Navigates to the account management screen (S15). */
    data object OpenAccount : MainSideEffect()
}

/**
 * ViewModel of the main tab screen.
 *
 * It shows the tabs and the sheets, keeps the settings, and starts the create flows.
 */
class MainViewModel(
    private val myVideoRepository: MyVideoRepository,
    private val settingsRepository: EditorSettingsRepository,
    private val openWork: OpenWorkUseCase,
    myVoiceRepository: MyVoiceRepository,
    private val authRepository: AuthRepository,
    private val loginGate: LoginGate,
    private val signOut: SignOutUseCase,
) : BaseViewModel<MainState, MainIntent, MainSideEffect>(
    initialState = MainState()
) {

    init {
        viewModelScope.launch { myVideoRepository.works.collect { reduce { copy(works = it) } } }
        viewModelScope.launch { settingsRepository.settings.collect { reduce { copy(settings = it) } } }
        viewModelScope.launch { myVoiceRepository.status.collect { reduce { copy(myVoice = it) } } }
        viewModelScope.launch { authRepository.session.collect { reduce { copy(session = it) } } }
        // 앱을 다시 시작하면 토큰만 남아 있다. 계정 정보는 서버에서 다시 불러온다.
        viewModelScope.launch {
            val session = authRepository.session.value
            if (session is SessionState.SignedIn && session.member == null) authRepository.refreshMember()
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
            is MainIntent.OpenWorkMenu -> reduce { copy(sheet = MainSheet.Work(intent.workId)) }
            is MainIntent.ExportWork -> exportWork(intent.workId)
            is MainIntent.RequestDeleteWork -> reduce { copy(sheet = null, deleteWorkId = intent.workId) }
            MainIntent.ConfirmDeleteWork -> deleteWork()
            MainIntent.DismissDeleteWork -> reduce { copy(deleteWorkId = null) }
            MainIntent.OpenMyVoice -> openMyVoice()
            MainIntent.OpenLogin -> loginGate.open(LoginReason.MY_TAB)
            MainIntent.OpenAccount -> postSideEffect { MainSideEffect.OpenAccount }
            MainIntent.RequestLogout -> reduce { copy(logoutConfirm = true) }
            MainIntent.ConfirmLogout -> logout()
            MainIntent.DismissLogout -> reduce { copy(logoutConfirm = false) }
        }
    }

    // =====================================================================
    // Account (F17)
    // =====================================================================

    /** Opens the screen of the user's own voice. The voice is on the server, so the user signs in first. */
    private fun openMyVoice() {
        viewModelScope.launch {
            if (loginGate.ensureSignedIn(LoginReason.MY_VOICE)) postSideEffect { MainSideEffect.OpenMyVoice }
        }
    }

    /** Signs out. The tab then shows the login card. */
    private fun logout() {
        reduce { copy(logoutConfirm = false) }
        viewModelScope.launch { signOut() }
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
    // Work menu (S13)
    // =====================================================================

    /**
     * Opens the export screen of a work without the editor. Back on the export screen comes back to the tab.
     *
     * @param workId ID of the work.
     */
    private fun exportWork(workId: String) {
        reduce { copy(sheet = null) }
        viewModelScope.launch {
            if (openWork(workId)) postSideEffect { MainSideEffect.OpenExport }
        }
    }

    /** Removes the work that the popup asks about, and tells the user. */
    private fun deleteWork() {
        val workId = currentState.deleteWorkId ?: return
        reduce { copy(deleteWorkId = null) }
        viewModelScope.launch {
            val message = try {
                myVideoRepository.deleteWork(workId)
                uiText(Res.string.toast_work_deleted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiText(Res.string.toast_work_delete_failed)
            }
            postSideEffect { MainSideEffect.ShowToast(message) }
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
