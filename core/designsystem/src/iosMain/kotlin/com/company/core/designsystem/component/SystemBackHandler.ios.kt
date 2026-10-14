package com.company.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

// Compose Multiplatform 1.10은 BackHandler 대신 navigationevent-compose를 권장하지만, 그 라이브러리는 아직 의존성에 없다.
// 이 BackHandler도 같은 NavigationEventDispatcher에 등록되므로 화면 가장자리 스와이프를 받는다.
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
