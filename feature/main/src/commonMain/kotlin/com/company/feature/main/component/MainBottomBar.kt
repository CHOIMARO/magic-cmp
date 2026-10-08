package com.company.feature.main.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.core.designsystem.component.MagicCreateButton
import com.company.core.designsystem.component.pressClickable
import com.company.core.designsystem.theme.MagicColors
import com.company.feature.main.MainTab

/**
 * Bottom navigation with four tabs and a center create button.
 *
 * @param selected Selected tab.
 * @param onSelect Called with the tapped tab.
 * @param onCreate Action of the center "+" button.
 */
@Composable
internal fun MainBottomBar(selected: MainTab, onSelect: (MainTab) -> Unit, onCreate: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MagicColors.NavigationBar)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
        Row(
            modifier = Modifier.fillMaxWidth().height(73.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(MainTab.HOME, "홈", Icons.Rounded.Home, Icons.Outlined.Home, selected, onSelect)
            NavItem(MainTab.TEMPLATES, "템플릿", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, selected, onSelect)
            Box(modifier = Modifier.weight(1f).height(64.dp), contentAlignment = Alignment.Center) {
                MagicCreateButton(onClick = onCreate)
            }
            NavItem(MainTab.LIBRARY, "내 영상", Icons.Rounded.VideoLibrary, Icons.Outlined.VideoLibrary, selected, onSelect)
            NavItem(MainTab.MY, "마이", Icons.Rounded.Person, Icons.Outlined.Person, selected, onSelect)
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun RowScope.NavItem(
    tab: MainTab,
    label: String,
    selectedIcon: ImageVector,
    icon: ImageVector,
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    val isSelected = tab == selected
    val pill by animateColorAsState(
        targetValue = if (isSelected) MagicColors.Accent.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(200),
    )
    Column(
        modifier = Modifier.weight(1f).height(64.dp).pressClickable { onSelect(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(width = 56.dp, height = 30.dp).clip(CircleShape).background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (isSelected) selectedIcon else icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MagicColors.OnSurface else MagicColors.OnSurface55,
        )
    }
}
