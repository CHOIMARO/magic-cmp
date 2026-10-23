package com.company.data.di

import com.company.data.export.IosVideoExportRepository
import com.company.data.font.FontFileStore
import com.company.data.font.IosFontFileStore
import com.company.data.media.IosMediaMetadataReader
import com.company.data.media.MediaMetadataReader
import com.company.domain.repository.VideoExportRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single<MediaMetadataReader> { IosMediaMetadataReader() }
    single<VideoExportRepository> { IosVideoExportRepository() }
    single<FontFileStore> { IosFontFileStore() }
}
