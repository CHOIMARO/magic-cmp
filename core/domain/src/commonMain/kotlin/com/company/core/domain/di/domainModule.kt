package com.company.core.domain.di

import com.company.core.domain.usecase.GetImageDetailUseCase
import com.company.core.domain.usecase.SearchImagesUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf( ::SearchImagesUseCase )
    factoryOf( ::GetImageDetailUseCase )
}