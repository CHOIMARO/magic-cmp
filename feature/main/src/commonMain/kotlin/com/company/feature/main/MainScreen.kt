package com.company.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.company.core.designsystem.component.MagicModalBottomSheet
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.domain.model.editor.VideoSummary
import com.company.feature.main.component.MainBottomBar
import com.company.feature.main.sheet.AppInfoSheetContent
import com.company.feature.main.sheet.TemplateSheetContent
import com.company.feature.main.tab.HomeTab
import com.company.feature.main.tab.LibraryTab
import com.company.feature.main.tab.MyTab
import com.company.feature.main.tab.TemplateTab

/**
 * Main tab screen. It only draws [state] and sends user actions to [onIntent].
 *
 * The bottom tabs float on top of the tab content. Each tab leaves space at the bottom for them.
 *
 * @param state Screen state.
 * @param onIntent Called with each user action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(state: MainState, onIntent: (MainIntent) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        when (state.tab) {
            MainTab.HOME -> HomeTab(
                drafts = state.drafts,
                onNewVideo = { onIntent(MainIntent.StartNewVideo) },
                onOpenCamera = { onIntent(MainIntent.OpenCamera) },
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
                myVoice = state.myVoice,
                onToggleSetting = { onIntent(MainIntent.ToggleSetting(it)) },
                onOpenAppInfo = { onIntent(MainIntent.OpenAppInfo) },
                onOpenMyVoice = { onIntent(MainIntent.OpenMyVoice) },
            )
        }
        MainBottomBar(
            selected = state.tab,
            onSelect = { onIntent(MainIntent.SelectTab(it)) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

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

private val PreviewDrafts = listOf(
    VideoSummary(name = "바닷가 주말", duration = 18.0, thumbnailHue = 35),
    VideoSummary(name = "작업실 투어", duration = 42.0, thumbnailHue = 200),
    VideoSummary(name = "말차 레시피", duration = 27.0, thumbnailHue = 150),
    VideoSummary(name = "러닝 크루", duration = 15.0, thumbnailHue = 20),
)

private val PreviewCompletedVideos = listOf(
    VideoSummary(name = "벚꽃 산책", duration = 21.0, thumbnailHue = 330),
    VideoSummary(name = "첫 출근 브이로그", duration = 34.0, thumbnailHue = 60),
)

@Composable
private fun MainScreenPreview(tab: MainTab) {
    MagicPreview {
        MainScreen(
            state = MainState(tab = tab, drafts = PreviewDrafts, completedVideos = PreviewCompletedVideos),
            onIntent = {},
        )
    }
}

@Preview(name = "홈", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainHomePreview() = MainScreenPreview(MainTab.HOME)

@Preview(name = "템플릿", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainTemplatesPreview() = MainScreenPreview(MainTab.TEMPLATES)

@Preview(name = "내 영상", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainLibraryPreview() = MainScreenPreview(MainTab.LIBRARY)

@Preview(name = "마이", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainMyPreview() = MainScreenPreview(MainTab.MY)
