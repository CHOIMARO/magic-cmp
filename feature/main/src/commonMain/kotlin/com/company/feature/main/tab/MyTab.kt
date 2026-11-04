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
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.RecordVoiceOver
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicButton
import com.company.core.designsystem.component.MagicButtonStyle
import com.company.core.designsystem.component.MagicSwitch
import com.company.core.designsystem.component.StripeBox
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.domain.model.account.LoginProvider
import com.company.domain.model.account.SessionState
import com.company.domain.model.editor.EditorSettings
import com.company.domain.model.editor.MyVoiceStatus
import com.company.feature.main.SettingKey
import com.company.feature.main.component.TabContentBottomPadding
import com.company.feature.main.component.TabTitle
import com.company.feature.main.component.storageSizeLabel
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
 * Without an account, the tab shows a login card instead of the profile. The account group then has only the
 * subscription row, and the logout button is not there (F17).
 *
 * @param settings Current editor settings.
 * @param exportedCount Number of works that the user exported (D43).
 * @param storageBytes Bytes that all works use on the device (D51).
 * @param myVoice State of the user's own voice.
 * @param session Sign-in state.
 * @param onToggleSetting Called with the tapped setting.
 * @param onOpenAppInfo Action of the "앱 정보" row.
 * @param onOpenMyVoice Action of the "내 목소리" row.
 * @param onLogin Action of the login card and of the rows that need an account.
 * @param onOpenAccount Action of the "계정 관리" row.
 * @param onLogout Action of the logout button.
 */
@Composable
internal fun MyTab(
    settings: EditorSettings,
    exportedCount: Int,
    storageBytes: Long,
    myVoice: MyVoiceStatus,
    session: SessionState,
    onToggleSetting: (SettingKey) -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenMyVoice: () -> Unit,
    onLogin: () -> Unit,
    onOpenAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    val signedIn = session as? SessionState.SignedIn
    val member = signedIn?.member
    val groups = listOf(
        stringResource(Res.string.my_group_account) to if (signedIn != null) {
            listOf(
                SettingRow(Icons.Outlined.Person, stringResource(Res.string.my_profile_edit)),
                SettingRow(
                    Icons.Outlined.ManageAccounts,
                    stringResource(Res.string.my_account_manage),
                    value = member?.providers?.firstOrNull()?.let { stringResource(it.label) }.orEmpty(),
                    onClick = onOpenAccount,
                ),
                SettingRow(Icons.Outlined.WorkspacePremium, stringResource(Res.string.my_subscription), value = stringResource(Res.string.my_plan_free)),
            )
        } else {
            listOf(SettingRow(Icons.Outlined.WorkspacePremium, stringResource(Res.string.my_subscription), onClick = onLogin))
        },
        stringResource(Res.string.my_group_notification) to listOf(
            SettingRow(Icons.Outlined.Campaign, stringResource(Res.string.my_template_news), toggle = SettingKey.TEMPLATE_NEWS),
        ),
        stringResource(Res.string.my_group_edit) to listOf(
            SettingRow(
                Icons.Outlined.RecordVoiceOver,
                stringResource(Res.string.my_voice),
                // 로그인 전에는 상태를 보여 주지 않는다. 누르면 로그인부터 한다.
                value = when {
                    signedIn == null -> ""
                    myVoice == MyVoiceStatus.Ready -> stringResource(Res.string.my_voice_ready)
                    myVoice is MyVoiceStatus.Making -> stringResource(Res.string.my_voice_making)
                    else -> stringResource(Res.string.my_voice_none)
                },
                onClick = onOpenMyVoice,
            ),
            SettingRow(Icons.AutoMirrored.Outlined.BrandingWatermark, stringResource(Res.string.my_watermark), toggle = SettingKey.WATERMARK),
            SettingRow(Icons.Outlined.Hd, stringResource(Res.string.my_default_quality), value = "1080p"),
            SettingRow(Icons.Outlined.Storage, stringResource(Res.string.my_work_storage), value = storageSizeLabel(storageBytes)),
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
        if (signedIn != null) {
            ProfileRow(name = member?.nickname.orEmpty(), exportedCount = exportedCount)
        } else {
            LoginCard(onLogin = onLogin)
        }
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
        if (signedIn != null) {
            MagicButton(
                text = stringResource(Res.string.my_logout),
                onClick = onLogout,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                style = MagicButtonStyle.DANGER,
            )
        }
    }
}

/** Card that asks the user to sign in. It takes the place of the profile when the user has no account. */
@Composable
private fun LoginCard(onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MagicColors.Surface)
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MagicColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MagicColors.OnAccent, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(Res.string.my_login_title),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.02).em,
                )
                Text(text = stringResource(Res.string.my_login_body), fontSize = 13.sp, color = MagicColors.OnSurface55)
            }
        }
        MagicButton(
            text = stringResource(Res.string.my_login),
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            style = MagicButtonStyle.PRIMARY_SMALL,
            height = 48.dp,
        )
    }
}

@Composable
private fun ProfileRow(name: String, exportedCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StripeBox(hue = 300f, lightness = 0.55f, shape = CircleShape, modifier = Modifier.size(60.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(Res.string.my_profile_summary, exportedCount),
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

/** Name of the provider in the "계정 관리" row. */
private val LoginProvider.label
    get() = when (this) {
        LoginProvider.KAKAO -> Res.string.my_provider_kakao
        LoginProvider.GOOGLE -> Res.string.my_provider_google
        LoginProvider.APPLE -> Res.string.my_provider_apple
    }

private fun EditorSettings.isOn(key: SettingKey): Boolean = when (key) {
    SettingKey.TEMPLATE_NEWS -> templateNews
    SettingKey.WATERMARK -> watermark
}
