package com.company.feature.main.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.FixedGrid
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.theme.MagicColors
import com.company.core.designsystem.theme.MagicTheme
import com.company.core.domain.model.editor.EditorTemplate
import com.company.core.domain.model.editor.VideoSummary
import com.company.feature.main.TemplateCatalog
import com.company.feature.main.component.MagicLogo
import com.company.feature.main.component.SectionHeader
import com.company.feature.main.component.TemplateCard
import com.company.feature.main.component.VideoCard

/**
 * Home tab: start card, template shortcuts, and recent drafts.
 *
 * @param drafts Draft projects.
 * @param onStartMagic Action of the "영상 고르기" button.
 * @param onShowAllTemplates Action of "전체 보기" in the template section.
 * @param onOpenTemplate Called with the tapped template.
 * @param onShowAllDrafts Action of "전체 보기" in the draft section.
 * @param onOpenProject Called with the name of the tapped draft.
 */
@Composable
internal fun HomeTab(
    drafts: List<VideoSummary>,
    onStartMagic: () -> Unit,
    onShowAllTemplates: () -> Unit,
    onOpenTemplate: (EditorTemplate) -> Unit,
    onShowAllDrafts: () -> Unit,
    onOpenProject: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
    ) {
        Row(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)) {
            MagicLogo(fontSize = 36.sp, iconSize = 18.dp)
        }

        HeroCard(onStartMagic = onStartMagic)

        SectionHeader(title = "템플릿으로 시작하기", onShowAll = onShowAllTemplates)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TemplateCatalog.Home.forEach { template ->
                TemplateCard(
                    template = template,
                    scale = 0.4f,
                    onClick = { onOpenTemplate(template) },
                    modifier = Modifier.width(104.dp),
                    showSubLabel = false,
                    nameSize = 12.5.sp,
                )
            }
        }

        SectionHeader(title = "최근 작업", onShowAll = onShowAllDrafts)
        FixedGrid(
            items = drafts,
            columns = 4,
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp,
        ) { draft ->
            VideoCard(video = draft, onClick = { onOpenProject(draft.name) }, compact = true)
        }
    }
}

@Composable
private fun HeroCard(onStartMagic: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MagicColors.Surface)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val cardShape = RoundedCornerShape(9.dp)
            StripeBox(hue = 35f, shape = cardShape, modifier = Modifier.size(46.dp, 74.dp).rotate(-6f))
            StripeBox(hue = 200f, shape = cardShape, modifier = Modifier.size(46.dp, 74.dp).offset(y = (-5).dp))
            StripeBox(hue = 150f, shape = cardShape, modifier = Modifier.size(46.dp, 74.dp).rotate(6f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val serif = MagicTheme.fonts.serif
            Text(
                text = buildAnnotatedString {
                    append("영상만 고르세요.\n나머지는 ")
                    withStyle(
                        SpanStyle(
                            fontFamily = serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            fontSize = 30.sp,
                            color = MagicColors.Accent,
                        )
                    ) { append("magic.") }
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 1.3.em,
                letterSpacing = (-0.02).em,
            )
            Text(
                text = "컷·음악·자막·필터를 알아서 맞춰드려요",
                fontSize = 14.sp,
                lineHeight = 1.5.em,
                color = MagicColors.OnSurface.copy(alpha = 0.65f),
            )
        }
        MagicButton(
            text = "영상 고르기",
            onClick = onStartMagic,
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.PRIMARY,
            leadingIcon = Icons.Rounded.AutoAwesome,
        )
    }
}
