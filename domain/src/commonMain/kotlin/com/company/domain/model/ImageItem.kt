package com.company.domain.model

data class ImageItem(
    val id: String,
    val imageUrl: String,
    val type: String,
    val user: String,
    val tags: List<String>,
    val isFavorited: Boolean = false,
    val views: Int,
    val likes: Int,
    val downloads: Int
)