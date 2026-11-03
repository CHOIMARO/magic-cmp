package com.company.magic

import android.app.Application
import com.company.magic.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class MagicApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // commonMain에 있는 initKoin 호출
        initKoin {
            androidLogger() // Koin 로그 보기
            androidContext(this@MagicApplication) // Context 주입
        }
    }
}