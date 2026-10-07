package com.company.core.data.di

import com.company.core.data.repository.PixabayRepositoryImpl
import com.company.core.data.source.remote.PixabayRemoteDataSource
import com.company.core.data.source.remote.PixabayRemoteDataSourceImpl
import com.company.core.domain.repository.PixabayRepository
import com.company.core.data.api.PixabayApi
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    singleOf(::PixabayApi)
    singleOf(::PixabayRemoteDataSourceImpl) { bind<PixabayRemoteDataSource>() }
    singleOf(::PixabayRepositoryImpl) { bind<PixabayRepository>() }
}