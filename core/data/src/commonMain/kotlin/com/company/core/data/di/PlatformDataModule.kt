package com.company.core.data.di

import org.koin.core.module.Module

/** Data sources that use a platform API, for example the media metadata reader. */
internal expect val platformDataModule: Module
