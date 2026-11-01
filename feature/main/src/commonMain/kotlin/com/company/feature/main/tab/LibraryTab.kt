package com.company.feature.main.tab

import androidx.compose.foundation.layout.Column
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
import com.company.core.designsystem.theme.MagicColors
import com.company.domain.model.editor.VideoSummary
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TabTitle
import com.company.feature.main.component.WorkCard
import com.company.feature.main.component.rememberNow
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.library_empty
import com.company.feature.main.resources.tab_library
import org.jetbrains.compose.resources.stringResource

/**
 * Library tab ("내 영상"): all works in one list, the work with the most recent change first (D43, D44).
 *
 * Each card shows the day of the last change, and a mark when the user exported the work. An empty list shows a
 * short message.
 *
 * @param works Works of the user.
 * @param menuWorkId Work whose menu is open, or null.
 * @param onOpenWork Called with the ID of the tapped work.
 * @param onOpenWorkMenu Called with the ID of the work after a long press on its card.
 */
@Composable
internal fun LibraryTab(
    works: List<VideoSummary>,
    menuWorkId: String?,
    onOpenWork: (String) -> Unit,
    onOpenWorkMenu: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = TabContentBottomPadding),
    ) {
        TabTitle(text = stringResource(Res.string.tab_library), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 18.dp))
        if (works.isEmpty()) {
            Text(
                text = stringResource(Res.string.library_empty),
                modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
                fontSize = 14.sp,
                color = MagicColors.OnSurface55,
                textAlign = TextAlign.Center,
            )
        } else {
            val now = rememberNow()
            FixedGrid(items = works, columns = 2, horizontalSpacing = 8.dp, verticalSpacing = 8.dp) { work ->
                WorkCard(
                    work = work,
                    now = now,
                    onOpen = { onOpenWork(work.id) },
                    onOpenMenu = { onOpenWorkMenu(work.id) },
                    menuOpen = work.id == menuWorkId,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
