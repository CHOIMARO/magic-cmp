package com.company.data.di

import org.koin.core.module.Module

/** Data sources that use a platform API, for example the media metadata reader and the video export. */
internal expect val platformDataModule: Module
