package com.company.feature.account.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicIconButton
import com.company.feature.account.resources.Res
import com.company.feature.account.resources.back
import org.jetbrains.compose.resources.stringResource

/** Alpha of the back button while the screen blocks the back action. */
private const val BLOCKED_BACK_ALPHA = 0.3f

/**
 * Top bar of the account screens: a back button and a title.
 *
 * @param title Title of the screen.
 * @param onBack Action of the back button.
 * @param backEnabled False to block the back button, for example during the withdrawal.
 */
@Composable
internal fun AccountTopBar(title: String, onBack: () -> Unit, backEnabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagicIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(Res.string.back),
            onClick = onBack,
            modifier = Modifier.alpha(if (backEnabled) 1f else BLOCKED_BACK_ALPHA),
            enabled = backEnabled,
            iconSize = 26.dp,
        )
        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
