package com.company.feature.account.di

import com.company.feature.account.login.LoginViewModel
import com.company.feature.account.manage.AccountViewModel
import com.company.feature.account.withdraw.WithdrawViewModel
import com.company.ui.account.LoginGate
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val accountModule = module {
    // 로그인 시트는 앱에 하나다. 서버 기능이 있는 화면들이 같은 LoginGate로 로그인을 요청한다.
    singleOf(::LoginGate)
    viewModelOf(::LoginViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::WithdrawViewModel)
}
