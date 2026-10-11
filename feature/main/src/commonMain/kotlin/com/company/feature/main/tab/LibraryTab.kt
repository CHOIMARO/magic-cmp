package com.company.feature.main.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicChip
import com.company.core.designsystem.component.MagicVideoCard
import com.company.core.domain.model.editor.VideoSummary
import com.company.feature.main.LibrarySegment
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TabTitle
import com.company.ui.editor.formatTime

private val SegmentChips = listOf(
    LibrarySegment.DRAFTS to "작업 중",
    LibrarySegment.COMPLETED to "완성",
)

/**
 * Library tab ("내 영상"): drafts and completed videos.
 *
 * @param segment Selected list.
 * @param drafts Draft projects.
 * @param completedVideos Saved videos.
 * @param onSelectSegment Called with the tapped list.
 * @param onOpenProject Called with the name of the tapped video.
 */
@Composable
internal fun LibraryTab(
    segment: LibrarySegment,
    drafts: List<VideoSummary>,
    completedVideos: List<VideoSummary>,
    onSelectSegment: (LibrarySegment) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = TabContentBottomPadding),
    ) {
        TabTitle(text = "내 영상", modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 18.dp))
        Row(modifier = Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SegmentChips.forEach { (chipSegment, label) ->
                MagicChip(label = label, selected = chipSegment == segment, onClick = { onSelectSegment(chipSegment) })
            }
        }
        FixedGrid(
            items = if (segment == LibrarySegment.COMPLETED) completedVideos else drafts,
            columns = 2,
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp,
        ) { video ->
            MagicVideoCard(
                title = video.name,
                duration = formatTime(video.duration),
                hue = video.thumbnailHue.toFloat(),
                onClick = { onOpenProject(video.name) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
