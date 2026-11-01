package com.company.domain.di

import com.company.domain.usecase.GetImageDetailUseCase
import com.company.domain.usecase.SearchImagesUseCase
import com.company.domain.usecase.editor.ApplyTemplateUseCase
import com.company.domain.usecase.editor.GenerateCaptionsUseCase
import com.company.domain.usecase.editor.LeaveEditorUseCase
import com.company.domain.usecase.editor.OpenWorkUseCase
import com.company.domain.usecase.editor.SaveWorkUseCase
import com.company.domain.usecase.editor.StartNewProjectUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val domainModule = module {
    factoryOf( ::SearchImagesUseCase )
    factoryOf( ::GetImageDetailUseCase )

    // Editor
    factoryOf( ::ApplyTemplateUseCase )
    factoryOf( ::GenerateCaptionsUseCase )
    factoryOf( ::StartNewProjectUseCase )

    // Works (F15)
    factoryOf( ::SaveWorkUseCase )
    factoryOf( ::LeaveEditorUseCase )
    factoryOf( ::OpenWorkUseCase )
}
