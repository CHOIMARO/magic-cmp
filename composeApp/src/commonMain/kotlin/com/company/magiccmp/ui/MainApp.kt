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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import com.company.core.designsystem.component.LocalMagicToast
import com.company.core.designsystem.component.MagicToastHost
import com.company.core.designsystem.component.rememberMagicToastState
import com.company.core.designsystem.theme.MagicColors
import com.company.core.domain.repository.FontRepository
import com.company.core.domain.repository.MyVoiceRepository
import com.company.feature.editor.navigation.MyVoiceRoute
import com.company.magiccmp.navigation.AppNavHost
import com.company.magiccmp.resources.Res
import com.company.magiccmp.resources.my_voice_ready
import com.company.ui.editor.LocalDownloadedFonts
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject

@Composable
fun MainApp() {
    val navHostController = rememberNavController()
    val toastState = rememberMagicToastState()
    // 내려받은 글꼴은 편집 무대와 저장 화면이 함께 쓴다.
    val fontRepository = koinInject<FontRepository>()
    val downloadedFonts by fontRepository.downloadedFonts.collectAsState()
    // 내 목소리가 다 만들어지면, 내 목소리 화면이 아닐 때 토스트로 알린다.
    val myVoiceRepository = koinInject<MyVoiceRepository>()
    LaunchedEffect(myVoiceRepository) {
        myVoiceRepository.readyEvents.collect {
            if (navHostController.currentDestination?.hasRoute<MyVoiceRoute>() != true) toastState.show(getString(Res.string.my_voice_ready))
        }
    }

    CompositionLocalProvider(LocalMagicToast provides toastState, LocalDownloadedFonts provides downloadedFonts) {
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
