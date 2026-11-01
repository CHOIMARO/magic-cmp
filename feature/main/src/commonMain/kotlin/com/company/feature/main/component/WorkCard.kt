package com.company.feature.main.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.core.designsystem.component.MagicVideoCard
import com.company.domain.model.editor.VideoSummary
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.work_exported
import com.company.ui.editor.formatTime
import org.jetbrains.compose.resources.stringResource

/**
 * Card of one work in "최근 작업" and "내 영상" (F15). It shows the length, the day of the last change, and a mark when
 * the user exported the work. A work without clips has a black card.
 *
 * A tap opens the work in the editor. A long press opens the work menu (S13), and the card stays a little smaller
 * while the menu is open.
 *
 * @param work The work.
 * @param now Current time in milliseconds since 1970-01-01 UTC.
 * @param onOpen Opens the work in the editor.
 * @param onOpenMenu Opens the work menu.
 * @param menuOpen True while the menu of this work is open.
 * @param modifier Modifier for the card. Give it a width.
 */
@Composable
internal fun WorkCard(
    work: VideoSummary,
    now: Long,
    onOpen: () -> Unit,
    onOpenMenu: () -> Unit,
    menuOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    MagicVideoCard(
        title = work.name,
        duration = formatTime(work.duration),
        hue = work.thumbnailHue?.toFloat(),
        onClick = onOpen,
        modifier = modifier,
        caption = createdLabel(work.modifiedAt, now),
        exportedLabel = if (work.exported) stringResource(Res.string.work_exported) else null,
        onLongClick = onOpenMenu,
        held = menuOpen,
    )
}
