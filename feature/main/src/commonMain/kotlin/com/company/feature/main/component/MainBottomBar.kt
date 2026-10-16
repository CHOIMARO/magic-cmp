package com.company.feature.main.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.MagicTabPill
import com.company.core.designsystem.theme.MagicColors
import com.company.feature.main.MainTab
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.tab_home
import com.company.feature.main.resources.tab_library
import com.company.feature.main.resources.tab_my
import com.company.feature.main.resources.tab_templates
import org.jetbrains.compose.resources.stringResource

/** Label of the tab in the bottom tabs. */
private val MainTab.label: String
    @Composable get() = stringResource(
        when (this) {
            MainTab.HOME -> Res.string.tab_home
            MainTab.TEMPLATES -> Res.string.tab_templates
            MainTab.LIBRARY -> Res.string.tab_library
            MainTab.MY -> Res.string.tab_my
        }
    )

/**
 * Bottom tabs: four pills that float on top of the tab content.
 *
 * A fade from the background color keeps the pills readable over the content.
 *
 * @param selected Selected tab.
 * @param onSelect Called with the tapped tab.
 * @param modifier Modifier for the tabs.
 */
@Composable
internal fun MainBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to MagicColors.Background.copy(alpha = 0f), 0.48f to MagicColors.Background))
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        MainTab.entries.forEach { tab ->
            MagicTabPill(label = tab.label, selected = tab == selected, onClick = { onSelect(tab) })
        }
    }
}
