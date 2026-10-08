package com.company.core.data.editor

import com.company.core.domain.model.editor.Clip
import com.company.core.domain.model.editor.EditorProject
import com.company.core.domain.model.editor.TextOverlay
import com.company.core.domain.model.editor.TextStyleType
import com.company.core.domain.model.editor.Vibe
import com.company.core.domain.model.editor.VideoSummary
import com.company.core.domain.model.editor.applyVibe
import com.company.core.domain.model.editor.newEditorId
import com.company.core.domain.repository.CaptionRepository
import com.company.core.domain.repository.MyVideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Keeps the drafts and the completed videos in memory.
 *
 * Every draft opens the same sample project, as in the prototype.
 */
class InMemoryMyVideoRepository(
    private val captionRepository: CaptionRepository,
) : MyVideoRepository {
    private val _drafts = MutableStateFlow(EditorSampleData.Drafts)
    override val drafts: StateFlow<List<VideoSummary>> = _drafts.asStateFlow()

    private val _completedVideos = MutableStateFlow(EditorSampleData.CompletedVideos)
    override val completedVideos: StateFlow<List<VideoSummary>> = _completedVideos.asStateFlow()

    override fun addCompletedVideo(video: VideoSummary) {
        _completedVideos.update { listOf(video) + it }
    }

    override suspend fun loadDraftProject(name: String): EditorProject {
        val clips = EditorSampleData.SampleClipIndexes.map { index ->
            val source = EditorSampleData.SourceVideos[index]
            val long = source.duration > 4
            Clip(
                id = newEditorId(),
                name = source.name,
                sourceDuration = source.duration,
                inPoint = if (long) 0.4 else 0.0,
                outPoint = if (long) minOf(source.duration, 3.8) else source.duration,
                thumbnailHue = source.thumbnailHue,
            )
        }
        val project = EditorProject(
            name = name,
            clips = clips,
            texts = listOf(
                TextOverlay(
                    id = newEditorId(),
                    text = EditorSampleData.SampleTitle,
                    start = 0.2,
                    length = 2.8,
                    style = TextStyleType.BOX,
                    x = 50.0,
                    y = 24.0,
                )
            ),
        ).applyVibe(Vibe.WARM)
        return project.copy(captions = captionRepository.generateCaptions(project.totalDuration))
    }
}
