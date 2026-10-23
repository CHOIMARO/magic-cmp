package com.company.data.source.remote

import com.company.data.model.PixabayImageResponse
import com.company.data.api.PixabayApi

class PixabayRemoteDataSourceImpl(
    private val api: PixabayApi
) : PixabayRemoteDataSource {

    override suspend fun getImageById(id: String): PixabayImageResponse {
        return api.getImageById(id)
    }
}