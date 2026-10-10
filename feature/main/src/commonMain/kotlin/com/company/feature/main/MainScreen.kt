package com.company.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.MagicModalBottomSheet
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
@OptIn(ExperimentalMaterial3Api::class)
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

        // 시트를 닫고 이동하는 버튼은 닫힘 애니메이션이 끝난 뒤에 해당 Intent를 보낸다.
        // 템플릿 사용(UseSelectedTemplate)은 열린 시트의 템플릿을 읽으므로 CloseSheet보다 먼저 보낸다.
        var actionAfterClose by remember { mutableStateOf<MainIntent?>(null) }
        MagicModalBottomSheet(
            target = state.sheet,
            onDismissRequest = {
                val action = actionAfterClose
                actionAfterClose = null
                onIntent(action ?: MainIntent.CloseSheet)
            },
            showDragHandle = true,
        ) { sheet, requestDismiss ->
            val closeThen: (MainIntent) -> Unit = { intent ->
                actionAfterClose = intent
                requestDismiss()
            }
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = SHEET_SPACING, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(SHEET_SPACING),
            ) {
                when (sheet) {
                    MainSheet.Create -> CreateSheetContent(
                        onPickVideos = { closeThen(MainIntent.PickVideosForMagic) },
                        onOpenCamera = { closeThen(MainIntent.OpenCamera) },
                        onStartFromTemplates = { closeThen(MainIntent.StartFromTemplates) },
                        onEditManually = { closeThen(MainIntent.PickVideosManually) },
                    )
                    is MainSheet.Template -> TemplateSheetContent(
                        template = sheet.template,
                        onUseTemplate = { closeThen(MainIntent.UseSelectedTemplate) },
                    )
                    MainSheet.AppInfo -> AppInfoSheetContent()
                }
            }
        }
    }
}

/** Space between the handle and the content, and between the content items, of a main sheet. */
private val SHEET_SPACING = 14.dp
