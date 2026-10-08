package com.company.feature.editor.di

import com.company.feature.editor.editor.EditorViewModel
import com.company.feature.editor.export.ExportViewModel
import com.company.feature.editor.magic.MagicViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val editorModule = module {
    viewModel { params -> MagicViewModel(params.get(), get(), get()) }
    viewModelOf(::EditorViewModel)
    viewModelOf(::ExportViewModel)
}
