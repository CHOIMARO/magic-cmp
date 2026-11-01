package com.company.feature.main.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.company.core.designsystem.component.MagicVideoCard
import com.company.domain.model.editor.VideoSummary
import com.company.domain.model.editor.WorkThumbnail
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.work_exported
import com.company.ui.editor.formatTime
import com.company.ui.media.rememberMediaThumbnail
import org.jetbrains.compose.resources.stringResource

/** Time of the fade from the placeholder to the first frame of a work, in milliseconds (D58). */
private const val THUMBNAIL_FADE_MILLIS = 200

/**
 * Card of one work in "최근 작업" and "내 영상" (F15). It shows the first frame of the work, the length, the day of the
 * last change, and a mark when the user exported the work (D52). A work without clips has a black card.
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
        background = work.thumbnail?.let { thumbnail -> { WorkThumbnailImage(thumbnail) } },
    )
}

/**
 * First frame of a work over the striped placeholder (D52). It fades in when it is ready (D58). A frame from the
 * memory cache shows at once. When the app cannot read the file, the placeholder stays.
 */
@Composable
private fun BoxScope.WorkThumbnailImage(thumbnail: WorkThumbnail) {
    val image = rememberMediaThumbnail(thumbnail.uri, thumbnail.type, thumbnail.time)
    val alpha by animateFloatAsState(if (image != null) 1f else 0f, tween(THUMBNAIL_FADE_MILLIS))
    if (image == null) return
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.matchParentSize().graphicsLayer { this.alpha = alpha },
        contentScale = ContentScale.Crop,
    )
}
