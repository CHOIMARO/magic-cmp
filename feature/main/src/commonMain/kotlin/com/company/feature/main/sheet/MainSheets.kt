package com.company.feature.main.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicPreview
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.designsystem.theme.PHONE_PREVIEW_WIDTH
import com.company.domain.model.editor.EditorTemplate
import com.company.domain.model.editor.LayoutType
import com.company.feature.main.resources.Res
import com.company.feature.main.resources.app_info_feedback
import com.company.feature.main.resources.app_info_latest
import com.company.feature.main.resources.app_info_licenses
import com.company.feature.main.resources.app_info_version
import com.company.feature.main.resources.app_info_whats_new
import com.company.feature.main.resources.template_use
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.description
import com.company.ui.editor.displayName
import com.company.ui.editor.sheetLabel
import org.jetbrains.compose.resources.stringResource

/**
 * Content of the template preview sheet.
 *
 * @param template Template to preview.
 * @param onUseTemplate Action of "이 템플릿으로 만들기".
 */
@Composable
internal fun ColumnScope.TemplateSheetContent(template: EditorTemplate, onUseTemplate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
        val previewShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .width(150.dp)
                .aspectRatio(9f / 16f)
                .shadow(elevation = 18.dp, shape = previewShape)
                .clip(previewShape),
        ) {
            TemplatePreview(
                template = template,
                scale = 0.57f,
                modifier = Modifier.matchParentSize(),
                labelPadding = 5.dp to 1.dp,
                captionRadius = 5f,
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = template.kind.sheetLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MagicColors.AccentSoft)
            Text(text = template.displayName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.25.em)
            Text(text = template.description, fontSize = 13.sp, lineHeight = 1.5.em, color = MagicColors.OnSurface60)
        }
    }
    MagicButton(
        text = stringResource(Res.string.template_use),
        onClick = onUseTemplate,
        modifier = Modifier.fillMaxWidth(),
        style = MagicButtonStyle.PRIMARY,
    )
}

/** Content of the app information sheet. */
@Composable
internal fun ColumnScope.AppInfoSheetContent() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val serif = MagicTheme.fonts.serif
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(MagicColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "m",
                modifier = Modifier.offset(y = (-3).dp),
                fontFamily = serif,
                fontStyle = FontStyle.Italic,
                fontSize = 40.sp,
                color = MagicColors.OnAccent,
            )
        }
        Text(
            text = "magic",
            modifier = Modifier.padding(top = 4.dp),
            fontFamily = serif,
            fontStyle = FontStyle.Italic,
            fontSize = 28.sp,
        )
        Text(text = stringResource(Res.string.app_info_version), fontSize = 13.sp, color = MagicColors.OnSurface55)
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MagicColors.SurfaceHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MagicColors.Success, modifier = Modifier.size(15.dp))
            Text(text = stringResource(Res.string.app_info_latest), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.Success)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MagicColors.Background)) {
        listOf(Res.string.app_info_whats_new, Res.string.app_info_licenses, Res.string.app_info_feedback).forEachIndexed { index, label ->
            if (index > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MagicColors.Divider))
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(label), fontSize = 15.sp)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MagicColors.OnSurface40, modifier = Modifier.size(20.dp))
            }
        }
    }
    Text(
        text = "© 2026 magic",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 11.5.sp,
        color = MagicColors.OnSurface40,
    )
}

// 시트는 별도 창(MagicModalBottomSheet)으로 열려 정적 프리뷰에 그려지지 않는다. 시트 표면과 콘텐츠만 보여 준다.
@Composable
private fun MainSheetPreviewFrame(content: @Composable ColumnScope.() -> Unit) {
    MagicPreview {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MagicColors.Surface)
                .padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Preview(name = "템플릿 상세 시트", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun TemplateSheetPreview() = MainSheetPreviewFrame {
    TemplateSheetContent(template = EditorTemplate.Layout(LayoutType.BEFORE_AFTER), onUseTemplate = {})
}

@Preview(name = "앱 정보 시트", widthDp = PHONE_PREVIEW_WIDTH)
@Composable
private fun AppInfoSheetPreview() = MainSheetPreviewFrame {
    AppInfoSheetContent()
}
