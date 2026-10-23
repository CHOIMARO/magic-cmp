package com.company.domain.model

data class VideoItem(
    val id: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val type: String,
    val user: String,
    val isFavorited: Boolean = false,
    val tags: List<String>,
    val views: Int,
    val likes: Int,
    val downloads: Int
)