package com.company.magiccmp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.company.core.designsystem.component.LocalMagicToast
import com.company.core.designsystem.component.MagicToastHost
import com.company.core.designsystem.component.rememberMagicToastState
import com.company.core.designsystem.theme.MagicColors
import com.company.magiccmp.navigation.AppNavHost

@Composable
fun MainApp() {
    val navHostController = rememberNavController()
    val toastState = rememberMagicToastState()

    CompositionLocalProvider(LocalMagicToast provides toastState) {
        // 위쪽 inset만 여기서 처리한다. 아래쪽은 각 화면이 배경색을 이어서 직접 처리한다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MagicColors.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        ) {
            AppNavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navHostController,
            )
            MagicToastHost(state = toastState)
        }
    }
}
