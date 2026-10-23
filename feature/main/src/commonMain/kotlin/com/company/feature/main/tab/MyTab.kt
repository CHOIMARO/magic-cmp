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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.BrandingWatermark
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Hd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
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
import com.company.domain.model.editor.EditorSettings
import com.company.domain.model.editor.MyVoiceStatus
import com.company.feature.main.SettingKey
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TabTitle
import com.company.feature.main.resources.*
import org.jetbrains.compose.resources.stringResource

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
 * @param myVoice State of the user's own voice.
 * @param onToggleSetting Called with the tapped setting.
 * @param onOpenAppInfo Action of the "앱 정보" row.
 * @param onOpenMyVoice Action of the "내 목소리" row.
 */
@Composable
internal fun MyTab(
    settings: EditorSettings,
    completedCount: Int,
    myVoice: MyVoiceStatus,
    onToggleSetting: (SettingKey) -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenMyVoice: () -> Unit,
) {
    val groups = listOf(
        stringResource(Res.string.my_group_account) to listOf(
            SettingRow(Icons.Outlined.Person, stringResource(Res.string.my_profile_edit)),
            SettingRow(Icons.Outlined.ManageAccounts, stringResource(Res.string.my_account_manage), value = stringResource(Res.string.my_account_google)),
            SettingRow(Icons.Outlined.WorkspacePremium, stringResource(Res.string.my_subscription), value = stringResource(Res.string.my_plan_free)),
        ),
        stringResource(Res.string.my_group_notification) to listOf(
            SettingRow(Icons.Outlined.Campaign, stringResource(Res.string.my_template_news), toggle = SettingKey.TEMPLATE_NEWS),
        ),
        stringResource(Res.string.my_group_edit) to listOf(
            SettingRow(
                Icons.Outlined.RecordVoiceOver,
                stringResource(Res.string.my_voice),
                value = when (myVoice) {
                    MyVoiceStatus.Ready -> stringResource(Res.string.my_voice_ready)
                    is MyVoiceStatus.Making -> stringResource(Res.string.my_voice_making)
                    MyVoiceStatus.None -> stringResource(Res.string.my_voice_none)
                },
                onClick = onOpenMyVoice,
            ),
            SettingRow(Icons.AutoMirrored.Outlined.BrandingWatermark, stringResource(Res.string.my_watermark), toggle = SettingKey.WATERMARK),
            SettingRow(Icons.Outlined.Hd, stringResource(Res.string.my_default_quality), value = "1080p"),
        ),
        stringResource(Res.string.my_group_info) to listOf(
            SettingRow(Icons.Outlined.Info, stringResource(Res.string.my_app_info), value = "v1.0.0", onClick = onOpenAppInfo),
            SettingRow(Icons.AutoMirrored.Outlined.HelpOutline, stringResource(Res.string.my_help)),
            SettingRow(Icons.Outlined.Description, stringResource(Res.string.my_terms)),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = TabContentBottomPadding),
    ) {
        TabTitle(text = stringResource(Res.string.tab_my), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 22.dp))
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
            text = stringResource(Res.string.my_logout),
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
            Text(text = stringResource(Res.string.my_profile_name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = stringResource(Res.string.my_profile_summary, completedCount),
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 13.sp,
                color = MagicColors.OnSurface55,
            )
        }
        MagicButton(text = stringResource(Res.string.my_profile_modify), onClick = {}, style = MagicButtonStyle.OUTLINE)
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
    SettingKey.WATERMARK -> watermark
}
