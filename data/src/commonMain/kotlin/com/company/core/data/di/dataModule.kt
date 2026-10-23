package com.company.core.data.di

import com.company.core.data.editor.FakeCaptionRepository
import com.company.core.data.editor.FakeDubbingRepository
import com.company.core.data.editor.FakeMyVoiceRepository
import com.company.core.data.editor.InMemoryEditorProjectRepository
import com.company.core.data.editor.InMemoryEditorSettingsRepository
import com.company.core.data.editor.InMemoryMyVideoRepository
import com.company.core.data.font.RemoteFontRepository
import com.company.core.data.media.MediaLibraryRepositoryImpl
import com.company.core.data.repository.PixabayRepositoryImpl
import com.company.core.data.source.remote.PixabayRemoteDataSource
import com.company.core.data.source.remote.PixabayRemoteDataSourceImpl
import com.company.core.domain.repository.CaptionRepository
import com.company.core.domain.repository.DubbingRepository
import com.company.core.domain.repository.EditorProjectRepository
import com.company.core.domain.repository.EditorSettingsRepository
import com.company.core.domain.repository.FontRepository
import com.company.core.domain.repository.MediaLibraryRepository
import com.company.core.domain.repository.MyVideoRepository
import com.company.core.domain.repository.MyVoiceRepository
import com.company.core.domain.repository.PixabayRepository
import com.company.core.data.api.PixabayApi
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