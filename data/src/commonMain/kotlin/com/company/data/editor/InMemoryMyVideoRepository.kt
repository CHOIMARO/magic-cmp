package com.company.data.editor

import com.company.domain.model.editor.Clip
import com.company.domain.model.editor.EditorProject
import com.company.domain.model.editor.TextOverlay
import com.company.domain.model.editor.TextStyleType
import com.company.domain.model.editor.VideoSummary
import com.company.domain.model.editor.newEditorId
import com.company.domain.repository.CaptionRepository
import com.company.domain.repository.MyVideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** One day in milliseconds. */
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Time between two sample entries of the same day, in milliseconds. It keeps the sample order stable. */
private const val SAMPLE_STEP_MILLIS = 10L * 60 * 1000

/**
 * Keeps the drafts and the completed videos in memory. Both lists keep the newest entry first.
 *
 * Every draft opens the same sample project, as in the prototype.
 */
class InMemoryMyVideoRepository(
    private val captionRepository: CaptionRepository,
) : MyVideoRepository {
    private val _drafts = MutableStateFlow(EditorSampleData.Drafts.toSummaries(now()))
    override val drafts: StateFlow<List<VideoSummary>> = _drafts.asStateFlow()

    private val _completedVideos = MutableStateFlow(EditorSampleData.CompletedVideos.toSummaries(now()))
    override val completedVideos: StateFlow<List<VideoSummary>> = _completedVideos.asStateFlow()

    override fun addDraft(name: String, duration: Double, thumbnailHue: Int): VideoSummary {
        val draft = VideoSummary(newEditorId(), name, duration, thumbnailHue, createdAt = now())
        _drafts.update { listOf(draft) + it }
        return draft
    }

    override fun updateDraft(id: String, duration: Double, thumbnailHue: Int?) {
        _drafts.update { list ->
            list.map { draft ->
                if (draft.id == id) draft.copy(duration = duration, thumbnailHue = thumbnailHue ?: draft.thumbnailHue) else draft
            }
        }
    }

    override fun addCompletedVideo(name: String, duration: Double, thumbnailHue: Int) {
        val video = VideoSummary(newEditorId(), name, duration, thumbnailHue, createdAt = now())
        _completedVideos.update { listOf(video) + it }
    }

    override suspend fun loadDraftProject(name: String): EditorProject {
        val clips = EditorSampleData.SampleClipIndexes.map { index ->
            val source = EditorSampleData.SampleVideos[index]
            val long = source.duration > 4
            Clip(
                id = newEditorId(),
                name = source.name,
                sourceDuration = source.duration,
                inPoint = if (long) 0.4 else 0.0,
                outPoint = if (long) minOf(source.duration, 3.8) else source.duration,
                thumbnailHue = source.hue,
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
                    style = TextStyleType.IMPACT,
                    x = 50.0,
                    y = 24.0,
                )
            ),
            musicId = EditorSampleData.SampleMusicId,
        )
        return project.copy(captions = captionRepository.generateCaptions(project.totalDuration))
    }
}

/** Current time in milliseconds since 1970-01-01 UTC. */
@OptIn(ExperimentalTime::class)
private fun now(): Long = Clock.System.now().toEpochMilliseconds()

/** Makes the list entries of the samples, with the newest entry first. */
private fun List<EditorSampleData.SampleSummary>.toSummaries(now: Long): List<VideoSummary> =
    mapIndexed { index, sample ->
        VideoSummary(
            id = newEditorId(),
            name = sample.name,
            duration = sample.duration,
            thumbnailHue = sample.thumbnailHue,
            createdAt = now - sample.daysAgo * DAY_MILLIS - index * SAMPLE_STEP_MILLIS,
        )
    }.sortedByDescending { it.createdAt }
