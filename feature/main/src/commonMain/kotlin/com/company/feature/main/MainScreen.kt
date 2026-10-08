package com.company.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.MagicBottomSheet
import com.company.core.designsystem.theme.MagicColors
import com.company.feature.main.component.MainBottomBar
import com.company.feature.main.sheet.AppInfoSheetContent
import com.company.feature.main.sheet.CreateSheetContent
import com.company.feature.main.sheet.TemplateSheetContent
import com.company.feature.main.tab.HomeTab
import com.company.feature.main.tab.LibraryTab
import com.company.feature.main.tab.MyTab
import com.company.feature.main.tab.TemplateTab

/**
 * Main tab screen. It only draws [state] and sends user actions to [onIntent].
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@Composable
internal fun MainScreen(state: MainState, onIntent: (MainIntent) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (state.tab) {
                    MainTab.HOME -> HomeTab(
                        drafts = state.drafts,
                        onStartMagic = { onIntent(MainIntent.PickVideosForMagic) },
                        onShowAllTemplates = { onIntent(MainIntent.ShowAllTemplates) },
                        onOpenTemplate = { onIntent(MainIntent.OpenTemplate(it)) },
                        onShowAllDrafts = { onIntent(MainIntent.ShowAllDrafts) },
                        onOpenProject = { onIntent(MainIntent.OpenProject(it)) },
                    )
                    MainTab.TEMPLATES -> TemplateTab(
                        filter = state.templateFilter,
                        onSelectFilter = { onIntent(MainIntent.SelectTemplateFilter(it)) },
                        onOpenTemplate = { onIntent(MainIntent.OpenTemplate(it)) },
                    )
                    MainTab.LIBRARY -> LibraryTab(
                        segment = state.librarySegment,
                        drafts = state.drafts,
                        completedVideos = state.completedVideos,
                        onSelectSegment = { onIntent(MainIntent.SelectLibrarySegment(it)) },
                        onOpenProject = { onIntent(MainIntent.OpenProject(it)) },
                    )
                    MainTab.MY -> MyTab(
                        settings = state.settings,
                        completedCount = state.completedVideos.size,
                        onToggleSetting = { onIntent(MainIntent.ToggleSetting(it)) },
                        onOpenAppInfo = { onIntent(MainIntent.OpenAppInfo) },
                    )
                }
            }
            MainBottomBar(
                selected = state.tab,
                onSelect = { onIntent(MainIntent.SelectTab(it)) },
                onCreate = { onIntent(MainIntent.OpenCreateSheet) },
            )
        }

        MagicBottomSheet(
            value = state.sheet,
            onDismiss = { onIntent(MainIntent.CloseSheet) },
            spacing = 14.dp,
        ) { sheet ->
            when (sheet) {
                MainSheet.Create -> CreateSheetContent(
                    onPickVideos = { onIntent(MainIntent.PickVideosForMagic) },
                    onOpenCamera = { onIntent(MainIntent.OpenCamera) },
                    onStartFromTemplates = { onIntent(MainIntent.StartFromTemplates) },
                    onEditManually = { onIntent(MainIntent.PickVideosManually) },
                )
                is MainSheet.Template -> TemplateSheetContent(
                    template = sheet.template,
                    onUseTemplate = { onIntent(MainIntent.UseSelectedTemplate) },
                )
                MainSheet.AppInfo -> AppInfoSheetContent()
            }
        }
    }
}
