package com.company.feature.main.tab

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicChip
import com.company.core.designsystem.component.bleedHorizontal
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.TemplateKind
import com.company.feature.main.TemplateCatalog
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TabTitle
import com.company.feature.main.component.TemplateCard

/** Side padding of the template tab. */
private val TemplatePadding = 16.dp

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
            .navigationBarsPadding()
            .padding(start = TemplatePadding, end = TemplatePadding, top = 12.dp, bottom = TabContentBottomPadding),
    ) {
        TabTitle(text = "템플릿", modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bleedHorizontal(TemplatePadding)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = TemplatePadding),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChips.forEach { (kind, label) ->
                MagicChip(label = label, selected = kind == filter, onClick = { onSelectFilter(kind) })
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        FixedGrid(items = templates, columns = 2, horizontalSpacing = 8.dp, verticalSpacing = 20.dp) { template ->
            TemplateCard(
                template = template,
                onClick = { onOpenTemplate(template) },
                isNew = template in TemplateCatalog.New,
            )
        }
    }
}
