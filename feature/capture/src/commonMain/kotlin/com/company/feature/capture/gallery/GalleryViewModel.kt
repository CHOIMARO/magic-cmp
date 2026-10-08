package com.company.feature.capture.gallery

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.SourceVideo
import com.company.core.domain.model.editor.addClips
import com.company.core.domain.model.editor.newEditorId
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import com.company.core.domain.repository.MediaLibraryRepository
import com.company.core.domain.usecase.editor.ApplyTemplateUseCase
import com.company.ui.editor.DEFAULT_TITLE_TEXT
import com.company.ui.editor.GalleryMode
import kotlinx.coroutines.launch

/**
 * Arguments of the video picker.
 *
 * @property mode Purpose of the picker.
 * @property template Template to apply to the new project, or null.
 */
data class GalleryArgs(val mode: GalleryMode, val template: EditorTemplate?)

/**
 * State of the video picker.
 *
 * @property mode Purpose of the picker.
 * @property template Template to apply to the new project, or null.
 * @property videos Videos in the gallery.
 * @property pickedIds IDs of the picked videos in pick order.
 * @property autoMagic True when automatic decoration is on.
 */
data class GalleryState(
    val mode: GalleryMode = GalleryMode.MAGIC,
    val template: EditorTemplate? = null,
    val videos: List<SourceVideo> = emptyList(),
    val pickedIds: List<String> = emptyList(),
    val autoMagic: Boolean = true,
) : State() {
    /** True when the picked videos go to the magic screen. */
    val isMagicMode: Boolean
        get() = mode == GalleryMode.MAGIC && autoMagic
}

/** User actions on the video picker. */
sealed class GalleryIntent : Intent() {
    /** TogglePick(videoId) : Picks the video, or removes it from the picked list. */
    data class TogglePick(val videoId: String) : GalleryIntent()

    /** Next : Continues with the picked videos. */
    data object Next : GalleryIntent()
}

/** One-time events of the video picker. */
sealed class GallerySideEffect : SideEffect() {
    /** OpenMagic(template) : Navigates to the magic screen. */
    data class OpenMagic(val template: EditorTemplate?) : GallerySideEffect()

    /** OpenEditor : Navigates to the editor with the new project. */
    data object OpenEditor : GallerySideEffect()

    /** ReturnToEditor(message) : Goes back to the editor and shows the message. */
    data class ReturnToEditor(val message: String) : GallerySideEffect()
}

/**
 * ViewModel of the video picker.
 *
 * It keeps the pick order and starts a new project or adds clips to the current project.
 */
class GalleryViewModel(
    args: GalleryArgs,
    mediaLibraryRepository: MediaLibraryRepository,
    settingsRepository: EditorSettingsRepository,
    private val projectRepository: EditorProjectRepository,
    private val applyTemplate: ApplyTemplateUseCase,
) : BaseViewModel<GalleryState, GalleryIntent, GallerySideEffect>(
    initialState = GalleryState(
        mode = args.mode,
        template = args.template,
        videos = mediaLibraryRepository.getSourceVideos(),
        autoMagic = settingsRepository.settings.value.autoMagic,
    )
) {

    // =====================================================================
    // Intent
    // =====================================================================

    /**
     * Handles a user action.
     *
     * @param intent Action to handle.
     */
    override fun handleIntent(intent: GalleryIntent) {
        when (intent) {
            is GalleryIntent.TogglePick -> reduce {
                copy(pickedIds = if (intent.videoId in pickedIds) pickedIds - intent.videoId else pickedIds + intent.videoId)
            }
            GalleryIntent.Next -> next()
        }
    }

    // =====================================================================
    // Next step
    // =====================================================================

    private fun next() {
        val state = currentState
        val clips = state.pickedIds.mapNotNull { id -> state.videos.find { it.id == id }?.toClip() }
        if (clips.isEmpty()) return

        when {
            state.mode == GalleryMode.APPEND -> {
                projectRepository.commit { it.addClips(clips) }
                postSideEffect { GallerySideEffect.ReturnToEditor("${clips.size}개 추가했어요") }
            }
            state.isMagicMode -> {
                projectRepository.start(EditorProject(name = NEW_PROJECT_NAME, clips = clips))
                postSideEffect { GallerySideEffect.OpenMagic(state.template) }
            }
            else -> viewModelScope.launch {
                val project = applyTemplate(EditorProject(name = NEW_PROJECT_NAME, clips = clips), state.template, DEFAULT_TITLE_TEXT)
                projectRepository.start(project)
                postSideEffect { GallerySideEffect.OpenEditor }
            }
        }
    }

    private fun SourceVideo.toClip() = Clip(
        id = newEditorId(),
        name = name,
        sourceDuration = duration,
        inPoint = 0.0,
        outPoint = duration,
        thumbnailHue = thumbnailHue,
    )

    private companion object {
        /** Name of a project that starts from picked videos. */
        const val NEW_PROJECT_NAME = "새 영상"
    }
}
