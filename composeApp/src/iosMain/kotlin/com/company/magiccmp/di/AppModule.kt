package com.company.magiccmp.di

// Swift에서 호출할 함수
fun doInitKoin() {
    initKoin {
        // iOS는 Context가 필요 없으므로 비워둡니다.
        // 필요하다면 여기서 iOS 전용 모듈을 추가할 수도 있습니다.
        // modules(iosSpecificModule) 
    }
}