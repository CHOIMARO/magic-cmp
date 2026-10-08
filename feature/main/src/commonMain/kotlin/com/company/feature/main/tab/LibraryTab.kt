package com.company.feature.main.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.SegmentOption
import com.company.core.designsystem.component.SegmentedControl
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.VideoSummary
import com.company.feature.main.LibrarySegment
import com.company.feature.main.component.TabTitle
import com.company.feature.main.component.VideoCard

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
    val completed = segment == LibrarySegment.COMPLETED
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        TabTitle(text = "내 영상", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp))
        SegmentedControl(
            options = listOf(SegmentOption("작업 중"), SegmentOption("완성")),
            selectedIndex = segment.ordinal,
            onSelect = { onSelectSegment(LibrarySegment.entries[it]) },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            contentColor = MagicColors.OnSurface70,
        )
        FixedGrid(
            items = if (completed) completedVideos else drafts,
            columns = 3,
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalSpacing = 8.dp,
            verticalSpacing = 14.dp,
        ) { video ->
            VideoCard(video = video, onClick = { onOpenProject(video.name) }, saved = completed)
        }
    }
}
