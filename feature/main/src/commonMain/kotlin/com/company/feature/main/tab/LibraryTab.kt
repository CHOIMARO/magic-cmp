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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicChip
import com.company.core.designsystem.component.MagicVideoCard
import com.company.core.designsystem.theme.MagicColors
import com.company.domain.model.editor.VideoSummary
import com.company.feature.main.LibrarySegment
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TabTitle
import com.company.feature.main.component.createdLabel
import com.company.feature.main.component.rememberNow
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.library_completed
import com.company.feature.main.resources.library_drafts
import com.company.feature.main.resources.library_empty_completed
import com.company.feature.main.resources.library_empty_drafts
import com.company.feature.main.resources.tab_library
import com.company.ui.editor.formatTime
import org.jetbrains.compose.resources.stringResource

private val SegmentChips = listOf(
    LibrarySegment.DRAFTS to Res.string.library_drafts,
    LibrarySegment.COMPLETED to Res.string.library_completed,
)

/**
 * Library tab ("내 영상"): drafts and completed videos.
 *
 * Each card shows the day when the user made the project or saved the video. An empty list shows a short message.
 *
 * @param segment Selected list.
 * @param drafts Draft projects. The newest draft is first.
 * @param completedVideos Saved videos. The newest video is first.
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
        TabTitle(text = stringResource(Res.string.tab_library), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 18.dp))
        Row(modifier = Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SegmentChips.forEach { (chipSegment, label) ->
                MagicChip(label = stringResource(label), selected = chipSegment == segment, onClick = { onSelectSegment(chipSegment) })
            }
        }
        val videos = if (segment == LibrarySegment.COMPLETED) completedVideos else drafts
        if (videos.isEmpty()) {
            Text(
                text = stringResource(if (segment == LibrarySegment.COMPLETED) Res.string.library_empty_completed else Res.string.library_empty_drafts),
                modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                fontSize = 14.sp,
                color = MagicColors.OnSurface55,
                textAlign = TextAlign.Center,
            )
        } else {
            val now = rememberNow()
            FixedGrid(items = videos, columns = 2, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { video ->
                MagicVideoCard(
                    title = video.name,
                    duration = formatTime(video.duration),
                    hue = video.thumbnailHue.toFloat(),
                    onClick = { onOpenProject(video.name) },
                    modifier = Modifier.fillMaxWidth(),
                    caption = createdLabel(video.createdAt, now),
                )
            }
        }
    }
}
