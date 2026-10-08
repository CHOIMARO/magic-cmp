package com.company.feature.main.tab

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicChip
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.TemplateKind
import com.company.feature.main.TemplateCatalog
import com.company.feature.main.component.TabTitle
import com.company.feature.main.component.TemplateCard

private val FilterChips = listOf(
    null to "전체",
    TemplateKind.LAYOUT to "레이아웃",
    TemplateKind.TEXT to "글자",
    TemplateKind.CAPTION to "자막",
)

/**
 * Template tab: kind filter and template grid.
 *
 * @param filter Selected kind. Null shows all kinds.
 * @param onSelectFilter Called with the tapped kind.
 * @param onOpenTemplate Called with the tapped template.
 */
@Composable
internal fun TemplateTab(
    filter: TemplateKind?,
    onSelectFilter: (TemplateKind?) -> Unit,
    onOpenTemplate: (EditorTemplate) -> Unit,
) {
    val templates = TemplateCatalog.All.filter { filter == null || it.kind == filter }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        TabTitle(text = "템플릿", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp))
        Text(
            text = "고르면 내 영상에 바로 입혀져요",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            fontSize = 13.sp,
            color = MagicColors.OnSurface55,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChips.forEach { (kind, label) ->
                MagicChip(label = label, selected = kind == filter, onClick = { onSelectFilter(kind) })
            }
        }
        FixedGrid(
            items = templates,
            columns = 3,
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalSpacing = 8.dp,
            verticalSpacing = 16.dp,
        ) { template ->
            TemplateCard(
                template = template,
                scale = 0.46f,
                onClick = { onOpenTemplate(template) },
                isNew = template in TemplateCatalog.New,
            )
        }
    }
}
