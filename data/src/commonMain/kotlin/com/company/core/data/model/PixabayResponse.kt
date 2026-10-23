package com.company.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- 이미지 Response ---

@Serializable
data class PixabayImageResponse(
    @SerialName("hits") val hits: List<PixabayImageHitResponse>
)

@Serializable
data class PixabayImageHitResponse(
    @SerialName("id") val id: Long,
    @SerialName("largeImageURL") val imageUrl: String,
    @SerialName("user") val user: String,
    @SerialName("type") val type: String,
    @SerialName("tags") val tags: String,
    @SerialName("views") val views: Int,
    @SerialName("likes") val likes: Int,
    @SerialName("downloads") val downloads: Int
)

// --- 비디오 Response ---

@Serializable
data class PixabayVideoResponse(
    @SerialName("hits") val hits: List<PixabayVideoHitResponse>
)

@Serializable
data class PixabayVideoHitResponse(
    @SerialName("id") val id: Long,
    @SerialName("user") val user: String,
    @SerialName("type") val type: String,
    @SerialName("tags") val tags: String,
    @SerialName("views") val views: Int,
    @SerialName("likes") val likes: Int,
    @SerialName("downloads") val downloads: Int,
    @SerialName("videos") val videos: PixabayVideoUrlResponse
)

@Serializable
data class PixabayVideoUrlResponse(
    @SerialName("medium") val medium: PixabayVideoDetailResponse
)

@Serializable
data class PixabayVideoDetailResponse(
    @SerialName("url") val url: String,
    @SerialName("thumbnail") val thumbnail: String
)