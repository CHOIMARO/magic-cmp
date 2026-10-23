package com.company.data.di

import com.company.data.editor.FakeCaptionRepository
import com.company.data.editor.FakeDubbingRepository
import com.company.data.editor.FakeMyVoiceRepository
import com.company.data.editor.InMemoryEditorProjectRepository
import com.company.data.editor.InMemoryEditorSettingsRepository
import com.company.data.editor.InMemoryMyVideoRepository
import com.company.data.font.RemoteFontRepository
import com.company.data.media.MediaLibraryRepositoryImpl
import com.company.data.repository.PixabayRepositoryImpl
import com.company.data.source.remote.PixabayRemoteDataSource
import com.company.data.source.remote.PixabayRemoteDataSourceImpl
import com.company.domain.repository.CaptionRepository
import com.company.domain.repository.DubbingRepository
import com.company.domain.repository.EditorProjectRepository
import com.company.domain.repository.EditorSettingsRepository
import com.company.domain.repository.FontRepository
import com.company.domain.repository.MediaLibraryRepository
import com.company.domain.repository.MyVideoRepository
import com.company.domain.repository.MyVoiceRepository
import com.company.domain.repository.PixabayRepository
import com.company.data.api.PixabayApi
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    includes(platformDataModule)

    singleOf(::PixabayApi)
    singleOf(::PixabayRemoteDataSourceImpl) { bind<PixabayRemoteDataSource>() }
    singleOf(::PixabayRepositoryImpl) { bind<PixabayRepository>() }

    // Media (시스템 피커로 고른 영상)
    singleOf(::MediaLibraryRepositoryImpl) { bind<MediaLibraryRepository>() }

    // Editor (메모리 기반 mock 구현)
    singleOf(::InMemoryEditorProjectRepository) { bind<EditorProjectRepository>() }
    singleOf(::InMemoryMyVideoRepository) { bind<MyVideoRepository>() }
    singleOf(::InMemoryEditorSettingsRepository) { bind<EditorSettingsRepository>() }
    singleOf(::FakeCaptionRepository) { bind<CaptionRepository>() }
    singleOf(::FakeDubbingRepository) { bind<DubbingRepository>() }
    singleOf(::FakeMyVoiceRepository) { bind<MyVoiceRepository>() }

    // 글꼴 (Google Fonts 저장소에서 내려받기)
    singleOf(::RemoteFontRepository) { bind<FontRepository>() }
}