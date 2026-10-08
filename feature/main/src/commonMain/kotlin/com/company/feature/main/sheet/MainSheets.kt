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
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.EditorTemplate
import com.company.ui.editor.TemplatePreview
import com.company.ui.editor.description
import com.company.ui.editor.displayName
import com.company.ui.editor.sheetLabel

/**
 * Content of the "무엇으로 시작할까요?" sheet.
 *
 * @param onPickVideos Action of "영상 고르기".
 * @param onOpenCamera Action of "촬영하기".
 * @param onStartFromTemplates Action of "템플릿으로 시작".
 * @param onEditManually Action of "직접 편집".
 */
@Composable
internal fun ColumnScope.CreateSheetContent(
    onPickVideos: () -> Unit,
    onOpenCamera: () -> Unit,
    onStartFromTemplates: () -> Unit,
    onEditManually: () -> Unit,
) {
    Text(
        text = "무엇으로 시작할까요?",
        modifier = Modifier.padding(2.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CreateOption(
            icon = Icons.Rounded.AutoAwesome,
            title = "영상 고르기",
            subtitle = "고르기만 하면 magic이 꾸며줘요",
            highlighted = true,
            onClick = onPickVideos,
        )
        CreateOption(Icons.Outlined.PhotoCamera, "촬영하기", "끊어서 여러 번 찍을 수 있어요", onClick = onOpenCamera)
        CreateOption(Icons.Outlined.Dashboard, "템플릿으로 시작", "레이아웃·글자·자막 스타일 고르기", onClick = onStartFromTemplates)
        CreateOption(Icons.Outlined.MovieCreation, "직접 편집", "꾸밈 없이 처음부터 만들어요", onClick = onEditManually)
    }
}

@Composable
private fun CreateOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressClickable(pressedScale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (highlighted) MagicColors.Accent else MagicColors.SurfaceHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) MagicColors.OnAccent else MagicColors.OnSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 13.sp,
                color = MagicColors.OnSurface55,
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MagicColors.OnSurface40, modifier = Modifier.size(22.dp))
    }
}

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
        text = "이 템플릿으로 만들기",
        onClick = onUseTemplate,
        modifier = Modifier.fillMaxWidth(),
        style = MagicButtonStyle.PRIMARY,
        leadingIcon = Icons.Rounded.AutoAwesome,
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
        Text(text = "버전 1.0.0 (100)", fontSize = 13.sp, color = MagicColors.OnSurface55)
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MagicColors.SurfaceHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MagicColors.Success, modifier = Modifier.size(15.dp))
            Text(text = "최신 버전이에요", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MagicColors.Success)
        }
    }
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MagicColors.Background)) {
        listOf("새로운 기능", "오픈소스 라이선스", "의견 보내기").forEachIndexed { index, label ->
            if (index > 0) Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MagicColors.Divider))
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label, fontSize = 15.sp)
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
