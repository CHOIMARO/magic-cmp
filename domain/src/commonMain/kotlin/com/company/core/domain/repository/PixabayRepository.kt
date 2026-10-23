package com.company.core.domain.repository

import androidx.paging.PagingData
import com.company.core.domain.model.ImageItem
import kotlinx.coroutines.flow.Flow

interface PixabayRepository {
    /**
     * 검색어를 기반으로 페이징된 이미지 스트림을 반환
     */
    fun getSearchImagesStream(query: String): Flow<PagingData<ImageItem>>

    /**
     * ID로 특정 이미지의 상세 정보를 조회
     */
    suspend fun getImageDetail(id: String): Result<ImageItem>
}