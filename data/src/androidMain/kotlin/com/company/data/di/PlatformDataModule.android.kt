package com.company.data.di

import com.company.data.export.AndroidVideoExportRepository
import com.company.data.font.AndroidFontFileStore
import com.company.data.font.FontFileStore
import com.company.data.media.AndroidMediaMetadataReader
import com.company.data.media.MediaMetadataReader
import com.company.domain.repository.VideoExportRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    // Context는 앱 시작 시 androidContext()로 등록된다.
    single<MediaMetadataReader> { AndroidMediaMetadataReader(get()) }
    single<VideoExportRepository> { AndroidVideoExportRepository(get()) }
    single<FontFileStore> { AndroidFontFileStore(get()) }
}
