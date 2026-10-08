package com.company.core.data.di

import com.company.core.data.editor.FakeCaptionRepository
import com.company.core.data.editor.FakeMediaLibraryRepository
import com.company.core.data.editor.InMemoryEditorProjectRepository
import com.company.core.data.editor.InMemoryEditorSettingsRepository
import com.company.core.data.editor.InMemoryMyVideoRepository
import com.company.core.data.repository.PixabayRepositoryImpl
import com.company.core.data.source.remote.PixabayRemoteDataSource
import com.company.core.data.source.remote.PixabayRemoteDataSourceImpl
import com.company.core.domain.repository.CaptionRepository
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import com.company.core.domain.repository.MediaLibraryRepository
import com.company.core.domain.repository.MyVideoRepository
import com.company.core.domain.repository.PixabayRepository
import com.company.core.data.api.PixabayApi
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    singleOf(::PixabayApi)
    singleOf(::PixabayRemoteDataSourceImpl) { bind<PixabayRemoteDataSource>() }
    singleOf(::PixabayRepositoryImpl) { bind<PixabayRepository>() }

    // Editor (메모리 기반 mock 구현)
    singleOf(::InMemoryEditorProjectRepository) { bind<EditorProjectRepository>() }
    singleOf(::FakeMediaLibraryRepository) { bind<MediaLibraryRepository>() }
    singleOf(::InMemoryMyVideoRepository) { bind<MyVideoRepository>() }
    singleOf(::InMemoryEditorSettingsRepository) { bind<EditorSettingsRepository>() }
    singleOf(::FakeCaptionRepository) { bind<CaptionRepository>() }
}