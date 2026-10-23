package com.company.core.data.di

import com.company.core.data.export.IosVideoExportRepository
import com.company.core.data.font.FontFileStore
import com.company.core.data.font.IosFontFileStore
import com.company.core.data.media.IosMediaMetadataReader
import com.company.core.data.media.MediaMetadataReader
import com.company.core.domain.repository.VideoExportRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single<MediaMetadataReader> { IosMediaMetadataReader() }
    single<VideoExportRepository> { IosVideoExportRepository() }
    single<FontFileStore> { IosFontFileStore() }
}
