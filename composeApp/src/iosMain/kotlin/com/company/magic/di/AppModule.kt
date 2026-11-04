package com.company.magic.di

import com.company.domain.repository.SocialLoginClient
import com.company.magic.login.IosLoginBridge
import com.company.magic.login.IosSocialLoginClient
import org.koin.dsl.module

/**
 * Starts Koin. The iOS app calls it one time when it starts.
 *
 * @param loginBridge Login SDKs of the Swift code: Kakao, Google, and Apple.
 */
fun doInitKoin(loginBridge: IosLoginBridge) {
    initKoin {
        // 로그인 SDK는 Swift 패키지라서 Swift가 만든 구현을 받아 등록한다.
        modules(module { single<SocialLoginClient> { IosSocialLoginClient(loginBridge) } })
    }
}
