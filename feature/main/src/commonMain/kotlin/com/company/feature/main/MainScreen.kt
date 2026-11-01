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
import com.company.core.designsystem.component.MagicDialog
import com.company.core.designsystem.component.MagicModalBottomSheet
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.domain.model.editor.VideoSummary
import com.company.feature.main.component.MainBottomBar
import com.company.feature.main.sheet.AppInfoSheetContent
import com.company.feature.main.sheet.TemplateSheetContent
import com.company.feature.main.component.storageSizeLabel
import com.company.feature.main.sheet.WorkMenuSheetContent
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.work_delete_cancel
import com.company.feature.main.resources.work_delete_confirm
import com.company.feature.main.resources.work_delete_message
import com.company.feature.main.resources.work_delete_title
import org.jetbrains.compose.resources.stringResource
import com.company.feature.main.tab.HomeTab
import com.company.feature.main.tab.LibraryTab
import com.company.feature.main.tab.MyTab
import com.company.feature.main.tab.TemplateTab
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
    // 작업 메뉴가 열려 있는 동안 그 카드는 살짝 줄어든 채로 있다.
    val menuWorkId = (state.sheet as? MainSheet.Work)?.workId
    Box(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        when (state.tab) {
            MainTab.HOME -> HomeTab(
                works = state.works,
                menuWorkId = menuWorkId,
                onNewVideo = { onIntent(MainIntent.StartNewVideo) },
                onOpenCamera = { onIntent(MainIntent.OpenCamera) },
                onShowAllWorks = { onIntent(MainIntent.ShowAllWorks) },
                onOpenWork = { onIntent(MainIntent.OpenWork(it)) },
                onOpenWorkMenu = { onIntent(MainIntent.OpenWorkMenu(it)) },
            )
            MainTab.TEMPLATES -> TemplateTab(
                filter = state.templateFilter,
                onSelectFilter = { onIntent(MainIntent.SelectTemplateFilter(it)) },
                onOpenTemplate = { onIntent(MainIntent.OpenTemplate(it)) },
            )
            MainTab.LIBRARY -> LibraryTab(
                works = state.works,
                menuWorkId = menuWorkId,
                onOpenWork = { onIntent(MainIntent.OpenWork(it)) },
                onOpenWorkMenu = { onIntent(MainIntent.OpenWorkMenu(it)) },
            )
            MainTab.MY -> MyTab(
                settings = state.settings,
                exportedCount = state.works.count { it.exported },
                storageBytes = state.works.sumOf { it.storageBytes },
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
                    is MainSheet.Work -> WorkMenuSheetContent(
                        name = state.works.find { it.id == sheet.workId }?.name.orEmpty(),
                        size = storageSizeLabel(state.works.find { it.id == sheet.workId }?.storageBytes ?: 0),
                        onExport = { closeThen(MainIntent.ExportWork(sheet.workId)) },
                        onDelete = { closeThen(MainIntent.RequestDeleteWork(sheet.workId)) },
                    )
                }
            }
        }
        if (state.deleteWorkId != null) {
            MagicDialog(
                title = stringResource(Res.string.work_delete_title),
                message = stringResource(Res.string.work_delete_message),
                confirmLabel = stringResource(Res.string.work_delete_confirm),
                onConfirm = { onIntent(MainIntent.ConfirmDeleteWork) },
                cancelLabel = stringResource(Res.string.work_delete_cancel),
                onCancel = { onIntent(MainIntent.DismissDeleteWork) },
                onDismissRequest = { onIntent(MainIntent.DismissDeleteWork) },
                danger = true,
            )
        }
    }
}

/** Space between the handle and the content, and between the content items, of a main sheet. */
private val SHEET_SPACING = 14.dp

/** Preview work that the user changed [daysAgo] days before now. A null [hue] is a work without clips. */
@OptIn(ExperimentalTime::class)
private fun previewWork(name: String, duration: Double, hue: Int?, daysAgo: Int, exported: Boolean = false) = VideoSummary(
    id = name,
    name = name,
    duration = duration,
    thumbnailHue = hue,
    modifiedAt = Clock.System.now().toEpochMilliseconds() - daysAgo * 24L * 60 * 60 * 1000,
    exported = exported,
)

private val PreviewWorks = listOf(
    previewWork("바닷가 주말", 18.0, 35, daysAgo = 0),
    previewWork("말차 레시피", 27.0, 150, daysAgo = 1, exported = true),
    previewWork("다음 편 예고", 8.0, null, daysAgo = 3),
    previewWork("작업실 투어", 42.0, 200, daysAgo = 6),
    previewWork("첫 출근 브이로그", 34.0, 60, daysAgo = 9, exported = true),
)

@Composable
private fun MainScreenPreview(tab: MainTab, works: List<VideoSummary> = PreviewWorks) {
    MagicPreview {
        MainScreen(state = MainState(tab = tab, works = works), onIntent = {})
    }
}

@Preview(name = "홈", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainHomePreview() = MainScreenPreview(MainTab.HOME)

@Preview(name = "홈 · 최근 작업 없음", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainHomeEmptyPreview() = MainScreenPreview(MainTab.HOME, works = emptyList())

@Preview(name = "템플릿", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainTemplatesPreview() = MainScreenPreview(MainTab.TEMPLATES)

@Preview(name = "내 영상", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainLibraryPreview() = MainScreenPreview(MainTab.LIBRARY)

@Preview(name = "마이", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun MainMyPreview() = MainScreenPreview(MainTab.MY)
