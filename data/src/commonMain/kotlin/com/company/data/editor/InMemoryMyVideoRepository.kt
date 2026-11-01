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
 * Keeps the works and their projects in memory (F15). The work with the most recent change is first.
 *
 * A sample work opens the sample project, as in the prototype. The project is made when the user opens the work the
 * first time, and the later changes stay.
 */
class InMemoryMyVideoRepository(
    private val captionRepository: CaptionRepository,
) : MyVideoRepository {
    private val _works = MutableStateFlow(EditorSampleData.Works.toSummaries(now()))
    override val works: StateFlow<List<VideoSummary>> = _works.asStateFlow()

    /** Last saved project of each work. */
    private val projects = mutableMapOf<String, EditorProject>()

    override suspend fun addWork(project: EditorProject): VideoSummary {
        val id = newEditorId()
        val work = project.summary(id, now())
        projects[id] = project.copy(workId = id)
        _works.update { listOf(work) + it }
        return work
    }

    override suspend fun saveWork(id: String, project: EditorProject): Boolean {
        val work = _works.value.find { it.id == id } ?: return false
        val saved = project.copy(workId = id)
        if (projects[id] == saved) return false
        projects[id] = saved
        val changed = saved.summary(id, now()).copy(exported = work.exported)
        _works.update { list -> listOf(changed) + list.filterNot { it.id == id } }
        return true
    }

    override suspend fun loadWork(id: String): EditorProject? {
        val work = _works.value.find { it.id == id } ?: return null
        return projects[id] ?: sampleProject(work.name).copy(workId = id).also { projects[id] = it }
    }

    override suspend fun deleteWork(id: String) {
        projects.remove(id)
        _works.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun markExported(id: String) {
        _works.update { list -> list.map { if (it.id == id) it.copy(exported = true) else it } }
    }

    /** Sample project of a sample work. */
    private suspend fun sampleProject(name: String): EditorProject {
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

/** Summary of the project as the work [id], changed at [time]. A project without clips has a black card. */
private fun EditorProject.summary(id: String, time: Long): VideoSummary =
    VideoSummary(id = id, name = name, duration = totalDuration, thumbnailHue = clips.firstOrNull()?.thumbnailHue, modifiedAt = time)

/** Makes the works of the samples, with the most recent change first. */
private fun List<EditorSampleData.SampleSummary>.toSummaries(now: Long): List<VideoSummary> =
    mapIndexed { index, sample ->
        VideoSummary(
            id = newEditorId(),
            name = sample.name,
            duration = sample.duration,
            thumbnailHue = sample.thumbnailHue,
            modifiedAt = now - sample.daysAgo * DAY_MILLIS - index * SAMPLE_STEP_MILLIS,
            exported = sample.exported,
        )
    }.sortedByDescending { it.modifiedAt }
