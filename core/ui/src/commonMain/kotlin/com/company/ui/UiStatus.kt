package com.company.ui

sealed interface UiStatus {
    data object Idle : UiStatus
    data object Loading : UiStatus
    data object Content : UiStatus
    data class Error(val message: String? = null) : UiStatus // 에러 메시지도 같이 넘기면 좋습니다
}