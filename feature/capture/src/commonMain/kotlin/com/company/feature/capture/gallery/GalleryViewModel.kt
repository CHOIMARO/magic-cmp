package com.company.feature.capture.gallery

import androidx.lifecycle.viewModelScope
import com.company.core.base.BaseViewModel
import com.company.core.base.Intent
import com.company.core.base.SideEffect
import com.company.core.base.State
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.SourceMedia
import com.company.core.domain.model.editor.addClips
import com.company.core.domain.model.editor.toClip
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.MediaLibraryRepository
import com.company.core.domain.usecase.editor.ApplyTemplateUseCase
import com.company.feature.capture.resources.Res
import com.company.feature.capture.resources.new_project_name
import com.company.feature.capture.resources.toast_clips_added
import com.company.ui.editor.DefaultTitleText
import com.company.ui.editor.GalleryMode
import com.company.ui.text.StringResolver
import com.company.ui.text.UiText
import com.company.ui.text.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Arguments of the photo and video picker.
 *
 * @property mode Purpose of the picker.
 * @property template Template to apply to the new project, or null.
 */
data class GalleryArgs(val mode: GalleryMode, val template: EditorTemplate?)

/**
 * State of the photo and video picker.
 *
 * @property mode Purpose of the picker.
 * @property template Template to apply to the new project, or null.
 * @property media Photos and videos that the app read from the system picker.
 *   With the full-screen picker, the screen shows these items so that the user can set the order.
 * @property pickedIds URIs of the picked items in pick order.
 */
data class GalleryState(
    val mode: GalleryMode = GalleryMode.CREATE,
    val template: EditorTemplate? = null,
    val media: List<SourceMedia> = emptyList(),
    val pickedIds: List<String> = emptyList(),
) : State() {
    /** Picked photos and videos in pick order. */
    val pickedMedia: List<SourceMedia>
        get() = pickedIds.mapNotNull { uri -> media.find { it.uri == uri } }
}

/** User actions on the photo and video picker. */
sealed class GalleryIntent : Intent() {
    /** SelectionChanged(uris) : The embedded system picker changed the selection. [uris] are in pick order. */
    data class SelectionChanged(val uris: List<String>) : GalleryIntent()

    /**
     * MediaAdded(uris) : The full-screen system picker returned [uris]. New items go to the end of the picked list.
     * An empty list means the user closed it.
     */
    data class MediaAdded(val uris: List<String>) : GalleryIntent()

    /** TogglePick(uri) : Picks the item on the order screen, or removes it from the picked list. */
    data class TogglePick(val uri: String) : GalleryIntent()

    /** RemovePick(uri) : Removes the item from the picked list. */
    data class RemovePick(val uri: String) : GalleryIntent()

    /** Next : Continues with the picked items. */
    data object Next : GalleryIntent()
}

/** One-time events of the photo and video picker. */
sealed class GallerySideEffect : SideEffect() {
    /** OpenEditor : Navigates to the editor with the new project. */
    data object OpenEditor : GallerySideEffect()

    /** ReturnToEditor(message) : Goes back to the editor and shows the message. */
    data class ReturnToEditor(val message: UiText) : GallerySideEffect()

    /** Close : Leaves the picker because the user picked nothing in the first full-screen picker. */
    data object Close : GallerySideEffect()
}

/**
 * ViewModel of the photo and video picker.
 *
 * It reads the photos and videos that the system picker gave, keeps the pick order,
 * and starts a new project or adds clips to the current project.
 */
class GalleryViewModel(
    args: GalleryArgs,
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val projectRepository: EditorProjectRepository,
    private val applyTemplate: ApplyTemplateUseCase,
    private val strings: StringResolver,
) : BaseViewModel<GalleryState, GalleryIntent, GallerySideEffect>(
    initialState = GalleryState(mode = args.mode, template = args.template)
) {
    private var selectionJob: Job? = null

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
            is GalleryIntent.SelectionChanged -> changeSelection(intent.uris)
            is GalleryIntent.MediaAdded -> addMedia(intent.uris)
            is GalleryIntent.TogglePick -> reduce {
                copy(pickedIds = if (intent.uri in pickedIds) pickedIds - intent.uri else pickedIds + intent.uri)
            }
            is GalleryIntent.RemovePick -> reduce { copy(pickedIds = pickedIds - intent.uri) }
            GalleryIntent.Next -> next()
        }
    }

    // =====================================================================
    // Picked items
    // =====================================================================

    /**
     * Reads the new items and then replaces the picked list.
     *
     * A new selection cancels the previous read. Thus an old read cannot overwrite a newer selection.
     */
    private fun changeSelection(uris: List<String>) {
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            loadMissingMedia(uris)
            reduce { copy(pickedIds = uris.filter { uri -> media.any { it.uri == uri } }) }
        }
    }

    /**
     * Adds the items from the full-screen picker and picks the new ones in the returned order.
     * An empty result closes the screen if no item was added before.
     */
    private fun addMedia(uris: List<String>) {
        if (uris.isEmpty()) {
            if (currentState.media.isEmpty()) postSideEffect { GallerySideEffect.Close }
            return
        }
        val newUris = uris.filter { uri -> currentState.media.none { it.uri == uri } }
        viewModelScope.launch {
            loadMissingMedia(uris)
            reduce {
                val added = newUris.filter { uri -> uri !in pickedIds && media.any { it.uri == uri } }
                copy(pickedIds = pickedIds + added)
            }
        }
    }

    private suspend fun loadMissingMedia(uris: List<String>) {
        val missing = uris.filter { uri -> currentState.media.none { it.uri == uri } }
        if (missing.isEmpty()) return
        val loaded = mediaLibraryRepository.loadSourceMedia(missing)
        reduce { copy(media = media + loaded.filter { item -> media.none { it.uri == item.uri } }) }
    }

    // =====================================================================
    // Next step
    // =====================================================================

    private fun next() {
        val state = currentState
        val clips = state.pickedMedia.map { it.toClip() }
        if (clips.isEmpty()) return

        when (state.mode) {
            GalleryMode.APPEND -> {
                projectRepository.commit { it.addClips(clips) }
                postSideEffect { GallerySideEffect.ReturnToEditor(uiText(Res.string.toast_clips_added, clips.size)) }
            }
            GalleryMode.CREATE -> viewModelScope.launch {
                val name = strings.resolve(uiText(Res.string.new_project_name))
                val defaultText = strings.resolve(UiText.Resource(DefaultTitleText))
                val project = applyTemplate(EditorProject(name = name, clips = clips), state.template, defaultText)
                projectRepository.start(project)
                postSideEffect { GallerySideEffect.OpenEditor }
            }
        }
    }
}
