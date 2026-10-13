package com.company.feature.editor.di

import com.company.feature.editor.editor.EditorViewModel
import com.company.feature.editor.export.ExportViewModel
import com.company.feature.editor.voice.MyVoiceViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val editorModule = module {
    viewModelOf(::EditorViewModel)
    viewModelOf(::ExportViewModel)
    viewModelOf(::MyVoiceViewModel)
}
