package com.company.feature.main.tab

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicActionCard
import com.company.core.designsystem.component.MagicActionCardStyle
import com.company.core.designsystem.component.MagicVideoCard
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.VideoSummary
import com.company.feature.main.TemplateCatalog
import com.company.feature.main.component.MagicLogo
import com.company.feature.main.component.SectionHeader
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TemplateCard
import com.company.feature.main.component.TemplateCardSize
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.home_camera
import com.company.feature.main.resources.home_new_video
import com.company.feature.main.resources.home_recent
import com.company.feature.main.resources.tab_templates
import com.company.ui.editor.formatTime
import org.jetbrains.compose.resources.stringResource

/** Side padding of the home tab. */
private val HomePadding = 16.dp

/**
 * Home tab: start cards, template shortcuts, and recent drafts.
 *
 * @param drafts Draft projects.
 * @param onNewVideo Action of the "새 영상 만들기" card.
 * @param onOpenCamera Action of the "촬영" card.
 * @param onShowAllTemplates Action of the arrow in the template section.
 * @param onOpenTemplate Called with the tapped template.
 * @param onShowAllDrafts Action of the arrow in the draft section.
 * @param onOpenProject Called with the name of the tapped draft.
 */
@Composable
internal fun HomeTab(
    drafts: List<VideoSummary>,
    onNewVideo: () -> Unit,
    onOpenCamera: () -> Unit,
    onShowAllTemplates: () -> Unit,
    onOpenTemplate: (EditorTemplate) -> Unit,
    onShowAllDrafts: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = HomePadding, end = HomePadding, top = 12.dp, bottom = TabContentBottomPadding),
    ) {
        Row(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 22.dp)) {
            MagicLogo()
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MagicActionCard(
                title = stringResource(Res.string.home_new_video),
                icon = Icons.Rounded.Add,
                onClick = onNewVideo,
                modifier = Modifier.weight(1.3f),
            )
            MagicActionCard(
                title = stringResource(Res.string.home_camera),
                icon = Icons.Rounded.PhotoCamera,
                onClick = onOpenCamera,
                modifier = Modifier.weight(1f),
                style = MagicActionCardStyle.SECONDARY,
            )
        }

        SectionHeader(title = stringResource(Res.string.tab_templates), onShowAll = onShowAllTemplates)
        // 가로 목록은 화면 끝까지 스크롤되고, 첫 카드는 다른 내용과 같은 왼쪽 선에서 시작한다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bleedHorizontal(HomePadding)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = HomePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TemplateCatalog.Home.forEach { template ->
                TemplateCard(
                    template = template,
                    onClick = { onOpenTemplate(template) },
                    modifier = Modifier.width(132.dp),
                    size = TemplateCardSize.SMALL,
                )
            }
        }

        SectionHeader(title = stringResource(Res.string.home_recent), onShowAll = onShowAllDrafts)
        FixedGrid(items = drafts, columns = 2, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { draft ->
            MagicVideoCard(
                title = draft.name,
                duration = formatTime(draft.duration),
                hue = draft.thumbnailHue.toFloat(),
                onClick = { onOpenProject(draft.name) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
