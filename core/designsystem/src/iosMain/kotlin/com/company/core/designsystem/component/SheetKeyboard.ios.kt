package com.company.core.designsystem.component

// Compose Multiplatform의 M3 시트 다이얼로그는 useSoftwareKeyboardInset 기본값(true)으로 열린다.
// 그래서 시트 레이어가 키보드 위로 옮겨지고, 시트 안의 WindowInsets.ime는 0이 된다.
internal actual val SheetWindowHasKeyboardInsets: Boolean = false
