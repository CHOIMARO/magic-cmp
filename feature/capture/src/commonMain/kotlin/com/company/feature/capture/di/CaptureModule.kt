package com.company.feature.capture.di

import com.company.feature.capture.camera.CameraViewModel
import com.company.feature.capture.gallery.GalleryViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val captureModule = module {
    viewModel { params -> GalleryViewModel(params.get(), get(), get(), get(), get()) }
    viewModelOf(::CameraViewModel)
}
