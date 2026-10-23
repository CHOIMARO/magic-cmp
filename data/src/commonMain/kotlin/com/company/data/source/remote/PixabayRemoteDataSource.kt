package com.company.data.source.remote

import com.company.data.model.PixabayImageResponse

interface PixabayRemoteDataSource {
    suspend fun getImageById(id: String): PixabayImageResponse
}