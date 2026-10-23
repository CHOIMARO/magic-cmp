package com.company.core.data.source.remote

import com.company.core.data.model.PixabayImageResponse

interface PixabayRemoteDataSource {
    suspend fun getImageById(id: String): PixabayImageResponse
}