package com.company.feature.main.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicSwitch
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.model.editor.EditorSettings
import com.company.feature.main.SettingKey
import com.company.feature.main.component.TabTitle

/**
 * One row of a settings group.
 *
 * @property icon Leading icon.
 * @property label Main label.
 * @property subLabel Optional note under the label.
 * @property value Optional value text before the arrow. Rows without [toggle] show it.
 * @property toggle Setting that the row switches, or null for a link row.
 * @property onClick Optional action of a link row.
 */
private data class SettingRow(
    val icon: ImageVector,
    val label: String,
    val subLabel: String? = null,
    val value: String = "",
    val toggle: SettingKey? = null,
    val onClick: (() -> Unit)? = null,
)

/**
 * "마이" tab: profile, settings, and logout.
 *
 * @param settings Current editor settings.
 * @param completedCount Number of completed videos.
 * @param onToggleSetting Called with the tapped setting.
 * @param onOpenAppInfo Action of the "앱 정보" row.
 */
@Composable
internal fun MyTab(
    settings: EditorSettings,
    completedCount: Int,
    onToggleSetting: (SettingKey) -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val groups = listOf(
        "계정" to listOf(
            SettingRow(Icons.Outlined.Person, "프로필 편집"),
            SettingRow(Icons.Outlined.ManageAccounts, "계정 관리", value = "Google 계정"),
            SettingRow(Icons.Outlined.WorkspacePremium, "구독", value = "무료 플랜"),
        ),
        "알림" to listOf(
            SettingRow(Icons.Outlined.Campaign, "새 템플릿 소식", toggle = SettingKey.TEMPLATE_NEWS),
        ),
        "편집" to listOf(
            SettingRow(Icons.Outlined.AutoAwesome, "자동 매직", subLabel = "영상을 고르면 바로 꾸며줘요", toggle = SettingKey.AUTO_MAGIC),
            SettingRow(Icons.AutoMirrored.Outlined.BrandingWatermark, "magic 워터마크", toggle = SettingKey.WATERMARK),
            SettingRow(Icons.Outlined.Hd, "기본 저장 화질", value = "1080p"),
        ),
        "정보" to listOf(
            SettingRow(Icons.Outlined.Info, "앱 정보", value = "v1.0.0", onClick = onOpenAppInfo),
            SettingRow(Icons.AutoMirrored.Outlined.HelpOutline, "도움말 · 문의"),
            SettingRow(Icons.Outlined.Description, "약관 및 개인정보"),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 28.dp),
    ) {
        TabTitle(text = "마이", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp))
        ProfileRow(completedCount = completedCount)
        groups.forEach { (title, rows) ->
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MagicColors.OnSurface50,
                )
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MagicColors.Surface)) {
                    rows.forEachIndexed { index, row ->
                        if (index > 0) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MagicColors.Divider))
                        }
                        SettingRowItem(row = row, settings = settings, onToggleSetting = onToggleSetting)
                    }
                }
            }
        }
        MagicButton(
            text = "로그아웃",
            onClick = {},
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            style = MagicButtonStyle.DANGER,
        )
    }
}

@Composable
private fun ProfileRow(completedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StripeBox(hue = 300f, lightness = 0.55f, shape = CircleShape, modifier = Modifier.size(60.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "민지", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "@minji.daily · 완성한 영상 ${completedCount}개",
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 13.sp,
                color = MagicColors.OnSurface55,
            )
        }
        MagicButton(text = "프로필 수정", onClick = {}, style = MagicButtonStyle.OUTLINE)
    }
}

@Composable
private fun SettingRowItem(row: SettingRow, settings: EditorSettings, onToggleSetting: (SettingKey) -> Unit) {
    val toggle = row.toggle
    val action: (() -> Unit)? = when {
        toggle != null -> ({ onToggleSetting(toggle) })
        else -> row.onClick
    }
    // 행을 누르면 스위치도 눌림 상태(손잡이 확대, 트랙 어둡게)를 보여준다.
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .then(
                if (action != null) {
                    Modifier.pressClickable(pressedScale = 0.99f, interactionSource = interactionSource, onClick = action)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(row.icon, contentDescription = null, tint = MagicColors.OnSurface70, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (row.subLabel != null) {
                Text(
                    text = row.subLabel,
                    modifier = Modifier.padding(top = 2.dp),
                    fontSize = 12.sp,
                    color = MagicColors.OnSurface50,
                )
            }
        }
        if (toggle != null) {
            MagicSwitch(checked = settings.isOn(toggle), pressed = pressed)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = row.value, fontSize = 13.sp, color = MagicColors.OnSurface50)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MagicColors.OnSurface40, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun EditorSettings.isOn(key: SettingKey): Boolean = when (key) {
    SettingKey.TEMPLATE_NEWS -> templateNews
    SettingKey.AUTO_MAGIC -> autoMagic
    SettingKey.WATERMARK -> watermark
}
