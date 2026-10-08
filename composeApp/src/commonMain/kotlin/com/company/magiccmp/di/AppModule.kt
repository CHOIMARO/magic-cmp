package com.company.magiccmp.di

import com.company.core.data.di.dataModule
import com.company.core.domain.di.domainModule
import com.company.core.network.di.networkModule
import com.company.feature.capture.di.captureModule
import com.company.feature.editor.di.editorModule
import com.company.feature.home.di.homeModule
import com.company.feature.main.di.mainModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

// 1. 앱 전체에서 사용할 모듈들을 한곳에 모읍니다.
val appModule = module {
    includes(
        homeModule,
        mainModule,
        captureModule,
        editorModule,
    ) // feature 모듈

    includes(
        domainModule,
        networkModule,
        dataModule,
    ) // core 모듈들
}

// 2. Koin을 시작하는 함수
// appDeclaration: 플랫폼별(안드로이드 Context 등) 설정을 받기 위한 파라미터
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration() // 안드로이드나 iOS에서 넘겨준 설정을 적용
        modules(appModule) // 위에서 모은 모듈 등록
    }
}