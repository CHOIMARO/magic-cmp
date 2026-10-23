package com.company.core.data.source.remote

import com.company.core.data.model.PixabayImageResponse
import com.company.core.data.api.PixabayApi

class PixabayRemoteDataSourceImpl(
    private val api: PixabayApi
) : PixabayRemoteDataSource {

    override suspend fun getImageById(id: String): PixabayImageResponse {
        return api.getImageById(id)
    }
}