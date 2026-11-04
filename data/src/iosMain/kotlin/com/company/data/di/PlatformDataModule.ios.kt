package com.company.data.di

import com.company.data.editor.IosMediaPersister
import com.company.data.editor.IosWorkFileStore
import com.company.data.editor.MediaPersister
import com.company.data.editor.WorkFileStore
import com.company.data.export.IosVideoExportRepository
import com.company.data.font.FontFileStore
import com.company.data.font.IosFontFileStore
import com.company.data.media.IosMediaMetadataReader
import com.company.data.media.MediaMetadataReader
import com.company.data.remote.IosSecureStore
import com.company.data.remote.SecureStore
import com.company.domain.repository.VideoExportRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single<MediaMetadataReader> { IosMediaMetadataReader() }
    single<VideoExportRepository> { IosVideoExportRepository() }
    single<FontFileStore> { IosFontFileStore() }
    single<WorkFileStore> { IosWorkFileStore() }
    single<MediaPersister> { IosMediaPersister() }
    single<SecureStore> { IosSecureStore() }
}
