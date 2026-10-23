package com.company.core.data.api

import com.company.core.data.model.PixabayImageResponse
import com.company.core.data.model.PixabayVideoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PixabayApi(private val client: HttpClient) {

    /**
     * 이미지 API 호출
     * Base URL(api/) 뒤에 아무것도 붙이지 않고 호출
     */
    suspend fun searchImages(
        query: String,
        imageType: String = "photo"
    ): PixabayImageResponse {
        return client.get {
            // URL 파라미터 추가
            parameter("q", query)
            parameter("image_type", imageType)
        }.body()
    }

    /**
     * 이미지 API 호출 (페이징 포함)
     */
    suspend fun searchImagesPagingData(
        query: String,
        imageType: String = "photo",
        page: Int,
        perPage: Int
    ): PixabayImageResponse {
        return client.get {
            parameter("q", query)
            parameter("image_type", imageType)
            parameter("page", page)
            parameter("per_page", perPage)
        }.body()
    }

    /**
     * 특정 이미지 ID로 조회
     */
    suspend fun getImageById(id: String): PixabayImageResponse {
        return client.get {
            parameter("id", id)
        }.body()
    }

    /**
     * 비디오 API 호출 (BASE_URL/videos/)
     */
    suspend fun searchVideos(query: String): PixabayVideoResponse {
        return client.get("videos/") {
            parameter("q", query)
        }.body()
    }

    /**
     * 특정 비디오 ID로 조회
     */
    suspend fun getVideoById(id: String): PixabayVideoResponse {
        return client.get("videos/") {
            parameter("id", id)
        }.body()
    }
}