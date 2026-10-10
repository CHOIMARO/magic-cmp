package com.company.core.data.di

import com.company.core.data.media.IosMediaMetadataReader
import com.company.core.data.media.MediaMetadataReader
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    single<MediaMetadataReader> { IosMediaMetadataReader() }
}
