package com.company.data.di

import com.company.data.editor.AndroidMediaPersister
import com.company.data.editor.AndroidWorkFileStore
import com.company.data.editor.MediaPersister
import com.company.data.editor.WorkFileStore
import com.company.data.export.AndroidVideoExportRepository
import com.company.data.font.AndroidFontFileStore
import com.company.data.font.FontFileStore
import com.company.data.media.AndroidMediaMetadataReader
import com.company.data.media.MediaMetadataReader
import com.company.data.remote.AndroidSecureStore
import com.company.data.remote.AndroidSocialLoginClient
import com.company.data.remote.CurrentActivityTracker
import com.company.data.remote.SecureStore
import com.company.domain.repository.SocialLoginClient
import com.company.domain.repository.VideoExportRepository
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformDataModule: Module = module {
    // Context는 앱 시작 시 androidContext()로 등록된다.
    single<MediaMetadataReader> { AndroidMediaMetadataReader(get()) }
    single<VideoExportRepository> { AndroidVideoExportRepository(get()) }
    single<FontFileStore> { AndroidFontFileStore(get()) }
    single<WorkFileStore> { AndroidWorkFileStore(get()) }
    single<MediaPersister> { AndroidMediaPersister(get()) }
    single<SecureStore> { AndroidSecureStore(get()) }

    // 로그인 SDK는 화면의 Activity가 필요하다. 추적기는 첫 Activity보다 먼저 만든다.
    single(createdAtStart = true) { CurrentActivityTracker(get()) }
    single<SocialLoginClient> { AndroidSocialLoginClient(get(), get()) }
}
