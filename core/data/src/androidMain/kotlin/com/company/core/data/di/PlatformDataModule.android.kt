package com.company.core.data.di

import com.company.core.data.media.AndroidMediaMetadataReader
import com.company.core.data.media.MediaMetadataReader
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    // Context는 앱 시작 시 androidContext()로 등록된다.
    single<MediaMetadataReader> { AndroidMediaMetadataReader(get()) }
}
