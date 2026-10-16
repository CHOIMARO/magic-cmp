package com.company.feature.capture.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicIconButton
import com.company.core.designsystem.component.MagicIconButtonStyle
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.PHONE_PREVIEW_HEIGHT
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.LayoutType
import com.company.core.domain.model.editor.MediaType
import com.company.core.domain.model.editor.SourceMedia
import com.company.feature.capture.resources.*
import com.company.ui.editor.GalleryMode
import com.company.ui.editor.displayName
import com.company.ui.editor.formatTime
import com.company.ui.media.MediaThumbnail
import org.jetbrains.compose.resources.stringResource

/** Text shadow on thumbnails, like CSS `text-shadow: 0 1px 3px rgba(0,0,0,.6)`. */
private val ThumbnailTextShadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 6f)

/**
 * Photo and video picker screen. It only draws [state] and sends user actions out.
 *
 * The screen has three areas from top to bottom: the app header, the picker area, and the app bottom bar.
 * The app UI does not go on top of the system picker, because the OS draws the picker above the app.
 *
 * @param state Screen state.
 * @param useEmbeddedPicker True to show the system picker inside the screen.
 *   False to show the items from the full-screen picker so that the user can set the order.
 * @param onIntent Called with each user action.
 * @param onOpenSystemPicker Opens the full-screen system picker.
 * @param onClose Action of the close button.
 */
@Composable
internal fun GalleryScreen(
    state: GalleryState,
    useEmbeddedPicker: Boolean,
    onIntent: (GalleryIntent) -> Unit,
    onOpenSystemPicker: () -> Unit,
    onClose: () -> Unit,
) {
    val pickerController = remember { EmbeddedMediaPickerController() }
    val append = state.mode == GalleryMode.APPEND
    Column(modifier = Modifier.fillMaxSize().background(MagicColors.Background)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MagicIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(Res.string.close),
                onClick = onClose,
                style = MagicIconButtonStyle.SURFACE,
            )
            Text(text = stringResource(if (append) Res.string.gallery_title_append else Res.string.gallery_title_pick), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(44.dp))
        }

        state.template?.let { template ->
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MagicColors.Accent.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Dashboard, contentDescription = null, tint = MagicColors.AccentPale, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(Res.string.gallery_template_banner, template.displayName),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MagicColors.AccentPale,
                )
            }
        }
        Text(
            text = galleryHint(state),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            fontSize = 13.sp,
            color = MagicColors.OnSurface60,
        )
        if (useEmbeddedPicker) {
            EmbeddedMediaPicker(
                controller = pickerController,
                onSelectionChange = { onIntent(GalleryIntent.SelectionChanged(it)) },
                onSelectionComplete = { onIntent(GalleryIntent.Next) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            PickedMediaOrderGrid(
                media = state.media,
                pickedIds = state.pickedIds,
                onToggle = { onIntent(GalleryIntent.TogglePick(it)) },
                onAddMore = onOpenSystemPicker,
                modifier = Modifier.weight(1f),
            )
        }

        GalleryBottomBar(
            state = state,
            onRemove = { uri ->
                // 임베디드 피커는 선택 상태를 OS가 갖고 있으므로 피커에서 빼야 한다. 피커가 바뀐 목록을 다시 알려 준다.
                if (useEmbeddedPicker) pickerController.deselect(uri) else onIntent(GalleryIntent.RemovePick(uri))
            },
            onNext = { onIntent(GalleryIntent.Next) },
        )
    }
}

/** Guide text under the header. */
@Composable
private fun galleryHint(state: GalleryState): String {
    val layout = (state.template as? EditorTemplate.Layout)?.layout
    return when {
        state.mode == GalleryMode.APPEND -> stringResource(Res.string.gallery_hint_append)
        layout != null && layout.clipCount > 1 -> {
            val name = layout.displayName
            stringResource(Res.string.gallery_hint_layout, name, name.topicParticle(), layout.clipCount)
        }
        else -> stringResource(Res.string.gallery_hint_default)
    }
}

/** Korean topic particle for the word: "은" after a final consonant, "는" after a vowel. */
private fun String.topicParticle(): String {
    val last = lastOrNull() ?: return "은"
    if (last !in '가'..'힣') return "은"
    return if ((last - '가') % 28 == 0) "는" else "은"
}

/**
 * Photos and videos that the full-screen system picker returned. The user taps them in the order to use.
 * The last cell opens the system picker again.
 *
 * @param media Photos and videos from the system picker.
 * @param pickedIds URIs of the picked items in pick order.
 * @param onToggle Called with the URI of the tapped item.
 * @param onAddMore Opens the system picker again to add more items.
 * @param modifier Modifier for the grid.
 */
@Composable
private fun PickedMediaOrderGrid(
    media: List<SourceMedia>,
    pickedIds: List<String>,
    onToggle: (String) -> Unit,
    onAddMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // null은 마지막 "더 고르기" 칸이다.
    FixedGrid(
        items = media + null,
        columns = 3,
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalSpacing = 6.dp,
        verticalSpacing = 6.dp,
    ) { item ->
        if (item == null) {
            AddMoreCell(onClick = onAddMore)
        } else {
            GalleryCell(item = item, order = pickedIds.indexOf(item.uri), onClick = { onToggle(item.uri) })
        }
    }
}

@Composable
private fun GalleryCell(item: SourceMedia, order: Int, onClick: () -> Unit) {
    val picked = order >= 0
    val shape = RoundedCornerShape(12.dp)
    MediaThumbnail(
        uri = item.uri,
        type = item.type,
        hue = item.thumbnailHue,
        shape = shape,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .pressClickable(pressedScale = 0.98f, onClick = onClick),
    ) {
        if (picked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MagicColors.Accent.copy(alpha = 0.15f))
                    .border(3.dp, MagicColors.Accent, shape)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (picked) MagicColors.Accent else Color.Black.copy(alpha = 0.25f))
                .border(1.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (picked) {
                Text(text = "${order + 1}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MagicColors.OnAccent)
            }
        }
        // 영상만 길이를 표시한다.
        item.duration?.let { duration ->
            Text(
                text = formatTime(duration),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 7.dp, bottom = 6.dp),
                style = LocalTextStyle.current.merge(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, shadow = ThumbnailTextShadow),
            )
        }
    }
}

/** Dashed "더 고르기" cell at the end of the grid. */
@Composable
private fun AddMoreCell(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val borderColor = MagicColors.OnSurface.copy(alpha = 0.25f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .pressClickable(interactionSource = interactionSource, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed) MagicColors.OnSurface.copy(alpha = 0.06f) else Color.Transparent)
            .drawBehind {
                val width = 1.5.dp.toPx()
                val dash = 4.dp.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(width / 2, width / 2),
                    size = size.copy(width = size.width - width, height = size.height - width),
                    cornerRadius = CornerRadius(12.dp.toPx() - width / 2),
                    style = Stroke(width = width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = MagicColors.OnSurface70, modifier = Modifier.size(24.dp))
        Text(text = stringResource(Res.string.gallery_pick_more), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.OnSurface70)
    }
}

@Composable
private fun GalleryBottomBar(
    state: GalleryState,
    onRemove: (String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickedMedia = state.pickedMedia
    val count = pickedMedia.size
    val hasPicked = count > 0
    val label = when {
        !hasPicked -> stringResource(Res.string.gallery_next_empty)
        state.mode == GalleryMode.APPEND -> stringResource(Res.string.gallery_next_append, count)
        else -> stringResource(Res.string.gallery_next_edit, count)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (hasPicked) {
            // 삭제 버튼이 썸네일 위·오른쪽으로 5dp 튀어나오므로 스크롤 영역 안쪽에 그만큼 여백을 둔다.
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp, end = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pickedMedia.forEachIndexed { index, item ->
                    PickedThumbnail(item = item, order = index + 1, onRemove = { onRemove(item.uri) })
                }
            }
        }
        // 고른 항목이 없으면 비활성 상태로 두고, 이유는 버튼 문구("영상이나 사진을 골라주세요")로 알려준다.
        MagicButton(
            text = label,
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.PRIMARY,
            enabled = hasPicked,
        )
    }
}

/** Small picked item with its order, its length, and a remove button. */
@Composable
private fun PickedThumbnail(item: SourceMedia, order: Int, onRemove: () -> Unit) {
    Box {
        MediaThumbnail(
            uri = item.uri,
            type = item.type,
            hue = item.thumbnailHue,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.size(width = 44.dp, height = 58.dp),
        ) {
            Text(
                text = "$order",
                modifier = Modifier.padding(start = 5.dp, top = 3.dp),
                style = LocalTextStyle.current.merge(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, shadow = ThumbnailTextShadow),
            )
            Text(
                text = item.duration?.let { formatTime(it) } ?: stringResource(Res.string.gallery_photo),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 5.dp, bottom = 3.dp),
                style = LocalTextStyle.current.merge(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, shadow = ThumbnailTextShadow),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 5.dp, y = (-5).dp)
                .size(20.dp)
                .pressClickable(onClick = onRemove)
                .clip(CircleShape)
                .background(MagicColors.OnSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.gallery_remove), tint = MagicColors.Background, modifier = Modifier.size(14.dp))
        }
    }
}

// 임베디드 시스템 피커는 OS가 그리므로 프리뷰에 나오지 않는다. 앱이 그리는 순서 확인 화면으로 보여 준다.
private val PreviewMedia = listOf(
    SourceMedia(uri = "preview://v1", type = MediaType.VIDEO, name = "VID_4021", duration = 4.2, thumbnailHue = 35),
    SourceMedia(uri = "preview://v2", type = MediaType.VIDEO, name = "VID_4028", duration = 6.8, thumbnailHue = 200),
    SourceMedia(uri = "preview://p1", type = MediaType.PHOTO, name = "IMG_0012", duration = null, thumbnailHue = 150),
    SourceMedia(uri = "preview://v3", type = MediaType.VIDEO, name = "VID_4035", duration = 3.1, thumbnailHue = 300),
)

@Composable
private fun GalleryScreenPreview(state: GalleryState) {
    MagicPreview {
        GalleryScreen(state = state, useEmbeddedPicker = false, onIntent = {}, onOpenSystemPicker = {}, onClose = {})
    }
}

@Preview(name = "사진·영상 선택", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun GalleryPickedPreview() = GalleryScreenPreview(
    GalleryState(media = PreviewMedia, pickedIds = listOf("preview://p1", "preview://v1")),
)

@Preview(name = "사진·영상 선택 - 템플릿", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun GalleryTemplatePreview() = GalleryScreenPreview(
    GalleryState(template = EditorTemplate.Layout(LayoutType.BEFORE_AFTER), media = PreviewMedia),
)

@Preview(name = "사진·영상 추가", widthDp = PHONE_PREVIEW_WIDTH, heightDp = PHONE_PREVIEW_HEIGHT)
@Composable
private fun GalleryAppendPreview() = GalleryScreenPreview(
    GalleryState(mode = GalleryMode.APPEND, media = PreviewMedia, pickedIds = listOf("preview://v2")),
)
